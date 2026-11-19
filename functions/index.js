const { createActivityHandler } = require("./notification")

if (process.env.WATCH_TOGETHER_LOCAL_SIMULATION !== "1") {
  const { onDocumentCreated } = require("firebase-functions/v2/firestore")
  const admin = require("firebase-admin")
  admin.initializeApp()

  const handler = createActivityHandler(message => admin.messaging().send(message))
  module.exports.sendGroupActivity = onDocumentCreated(
    "groups/{groupId}/events/{eventId}",
    event => event.data ? handler(event.data, event) : null
  )
}

module.exports.simulate = require("./notification").simulate
