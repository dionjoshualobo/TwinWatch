const assert = require("node:assert")
const { buildActivityMessage } = require("./notification")

function createRunner({ db, messaging, maxPerRun = 20, now = () => Date.now() }) {
  return async function runOnce() {
    const snapshot = await db.collectionGroup("events")
      .where("fcmPending", "==", true)
      .limit(maxPerRun)
      .get()

    let sent = 0
    let skipped = 0
    let failed = 0

    for (const doc of snapshot.docs) {
      const data = doc.data()
      const groupId = doc.ref.parent.parent && doc.ref.parent.parent.id
      const message = groupId ? buildActivityMessage({ ...data, groupId, eventId: doc.id }) : null

      if (!message) {
        await doc.ref.update({ fcmPending: false, fcmSkippedAt: now() })
        skipped += 1
        continue
      }

      try {
        await messaging.send(message)
        await doc.ref.update({ fcmPending: false, fcmSentAt: now() })
        sent += 1
      } catch (error) {
        failed += 1
        console.error(`Notification failed for ${doc.id}: ${error && error.message ? error.message : error}`)
      }
    }

    return { scanned: snapshot.docs.length, sent, skipped, failed }
  }
}

function fakeDoc(id, data, groupId) {
  const updates = []
  return {
    id,
    data: () => data,
    updates,
    ref: {
      parent: { parent: { id: groupId } },
      update: async value => { updates.push(value) }
    }
  }
}

async function simulatePending() {
  const messages = []
  const docs = [
    fakeDoc("event-1", { uid: "member-2", body: "Maya marked S01E01 of The Show" }, "circle-1"),
    fakeDoc("event-2", { uid: "member-2", body: "" }, "circle-1"),
    fakeDoc("event-3", { uid: "member-3", body: "FAIL" }, "circle-2")
  ]
  const db = {
    collectionGroup: () => ({
      where: () => ({
        limit: () => ({
          get: async () => ({ docs })
        })
      })
    })
  }
  const messaging = {
    send: async message => {
      if (message.notification.body === "FAIL") throw new Error("simulated FCM failure")
      messages.push(message)
    }
  }

  const result = await createRunner({ db, messaging, maxPerRun: 20, now: () => 1 })()

  assert.equal(result.scanned, 3)
  assert.equal(result.sent, 1)
  assert.equal(result.skipped, 1)
  assert.equal(result.failed, 1)
  assert.equal(messages.length, 1)
  assert.equal(messages[0].topic, "g_circle-1")
  assert.deepEqual(docs[0].updates, [{ fcmPending: false, fcmSentAt: 1 }])
  assert.deepEqual(docs[1].updates, [{ fcmPending: false, fcmSkippedAt: 1 }])
  assert.equal(docs[2].updates.length, 0)

  return "PENDING_SIMULATION_OK: 1 sent, 1 skipped, 1 failed event remains pending"
}

if (require.main === module) {
  const admin = require("firebase-admin")
  const raw = process.env.FIREBASE_SERVICE_ACCOUNT
  if (!raw) throw new Error("FIREBASE_SERVICE_ACCOUNT is not set")

  admin.initializeApp({ credential: admin.credential.cert(JSON.parse(raw)) })
  const parsedMax = Number(process.env.MAX_PER_RUN || "20")
  const maxPerRun = Number.isFinite(parsedMax) && parsedMax > 0 ? parsedMax : 20

  createRunner({
    db: admin.firestore(),
    messaging: admin.messaging(),
    maxPerRun
  })().then(result => {
    console.log(JSON.stringify(result))
    if (result.failed > 0) process.exitCode = 1
  }).catch(error => {
    console.error(error)
    process.exit(1)
  })
}

module.exports = { createRunner, simulatePending }
