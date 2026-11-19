const assert = require("node:assert")

function buildActivityMessage(event) {
  const groupId = String(event.groupId || "")
  const body = String(event.body || "").trim()
  if (!groupId || !body) return null

  const authorUid = String(event.uid || event.authorUid || "")
  return {
    topic: `g_${groupId}`,
    notification: {
      title: "TwinWatch",
      body
    },
    data: {
      groupId,
      uid: authorUid,
      authorUid,
      authorName: String(event.authorName || ""),
      body,
      eventId: String(event.eventId || "")
    }
  }
}

function shouldDisplay(message, currentUid) {
  return Boolean(message) && (!currentUid || message.data.authorUid !== currentUid)
}

function createActivityHandler(send) {
  return async function handleActivityCreated(snap, context) {
    const data = snap.data ? snap.data() : snap
    const event = { ...data, ...(context.params || {}) }
    const message = buildActivityMessage(event)
    if (!message) return null
    await send(message)
    return message
  }
}

async function simulate() {
  const sent = []
  const handler = createActivityHandler(async message => { sent.push(message) })
  const params = { groupId: "circle-1", eventId: "event-1" }

  await handler(
    {
      data: () => ({
        uid: "member-2",
        authorName: "Maya",
        body: "Maya marked 3 episodes of The Show",
        createdAt: 1
      })
    },
    { params }
  )

  await handler(
    {
      data: () => ({
        uid: "member-2",
        authorName: "Maya",
        body: "Maya wrote a note about S01E04 of The Show",
        createdAt: 2
      })
    },
    { params: { ...params, eventId: "event-2" } }
  )

  assert.equal(sent.length, 2)
  assert.equal(sent[0].topic, "g_circle-1")
  assert.equal(sent[0].data.authorUid, "member-2")
  assert.equal(sent[0].notification.body, "Maya marked 3 episodes of The Show")
  assert.equal(sent[1].notification.body, "Maya wrote a note about S01E04 of The Show")
  assert.equal(shouldDisplay(sent[0], "member-1"), true)
  assert.equal(shouldDisplay(sent[0], "member-2"), false)

  return "SIMULATION_OK: 2 group notifications queued; author notification skipped"
}

module.exports = {
  buildActivityMessage,
  shouldDisplay,
  createActivityHandler,
  simulate
}
