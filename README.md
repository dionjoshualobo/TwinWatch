# TwinWatch

TwinWatch is an Android app for watching shows with a small group. Create a circle, share a code, add shows, mark episodes as watched, leave notes, and see everyone's progress in one place.

Show metadata comes from the public TVMaze API. Group data, profiles, progress, notes, and activity live in Firebase/Firestore. Sign-in is anonymous, so there is no separate password flow.

## Features

- Create or join a circle with a short invite code.
- Add shows from TVMaze and browse episodes by season.
- Mark single episodes or a range of episodes as watched.
- See per-member progress for each show and group-wide averages.
- Add public or private notes on a show, season, or episode.
- Browse group activity and drill into any member's progress and public notes.
- Light/dark theme and editable display name.

## Requirements

- Android Studio with Android SDK 35 installed.
- JDK 17.
- Gradle 8.14 if building outside Android Studio. This repository does not currently include a Gradle wrapper.
- A Firebase project if you want to self-host instead of using the bundled configuration.

## Build and run

1. Open this repository in Android Studio.
2. Let Gradle sync.
3. Run the `app` configuration on a device or emulator running Android 8.0 / API 26 or newer.

From the command line with Gradle 8.14 installed:

```bash
gradle assembleDebug --no-daemon
```

The APK is written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

The app package id is `com.dionlobo.watchtogether`. Keep this in mind when registering an Android app in Firebase.

## Important: the bundled Firebase key

This repository currently includes `app/google-services.json` pointing at the maintainer's Firebase project.

If you build and install the app without changing that file:

- The app will use the maintainer's Firebase project.
- Your circles, names, watched episodes, notes, and activity will be stored in that project.
- The maintainer, as owner of that Firebase project, can read that data using the Firebase console or Admin tooling.
- Restricting the API key does not hide Firestore data from the project owner. It only limits which apps and APIs can call the project.

Mobile API keys are not secret. The real access controls are Firebase Authentication and Firestore Security Rules. Do not put private data in a Firebase project controlled by someone else.

If privacy matters, replace the Firebase configuration and deploy your own rules before sharing or installing builds.

## Self-host with your own Firebase project

1. Create a Firebase project at <https://console.firebase.google.com/>.
2. Add an Android app to that project with package name `com.dionlobo.watchtogether`.
3. Register your debug and release SHA-1 fingerprints in Firebase project settings.
   - For the default Android debug keystore:

     ```bash
     keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
     ```

4. Download the generated `google-services.json`.
5. Replace `app/google-services.json` with your downloaded file. Replace the whole file, not just the key string.
6. In Firebase, enable:
   - Authentication → Sign-in method → Anonymous.
   - Cloud Firestore.
   - Cloud Messaging, if you want push-message support.
7. Deploy the included security rules:

   ```bash
   firebase login
   firebase deploy --only firestore:rules --project <your-firebase-project-id>
   ```

8. Optional: restrict your Firebase API key in Google Cloud Console.
   - Application restriction: Android app.
   - Add your package name and SHA-1 fingerprint.
   - API restriction: allow only the APIs your project uses, such as Identity Toolkit, Firebase Installations, FCM Registration, and Cloud Firestore APIs.

## Changing the Firebase key or project

The Firebase key/configuration lives in:

```text
app/google-services.json
```

To switch projects:

1. Get the new project's `google-services.json` from Firebase console → Project settings → Your apps → Android app.
2. Overwrite `app/google-services.json`.
3. Make sure the new Firebase project has Anonymous Auth and Firestore enabled.
4. Deploy `firestore.rules` to the new project.
5. Rebuild and reinstall the app.

If you change the application id in `app/build.gradle.kts`, you must also register that new package name in Firebase and download a matching `google-services.json`.

## Notifications

The app registers for Firebase Cloud Messaging topics and can display incoming FCM messages. It does not include a backend that sends group notifications by itself. Activity is always written to Firestore and shown in the circle's Activity tab.

If you self-host and want push notifications, you need your own server or Admin SDK process to send FCM messages to the topic names used by the app.

## Project layout

```text
app/src/main/java/com/dionlobo/watchtogether/
  App.kt            Navigation, splash screen, circle list
  Screens.kt        UI screens and Compose components
  MainViewModel.kt  App state and safe coroutine launch helper
  Repository.kt     Firestore, Auth, FCM, and local reminder logic
  Model.kt          Firestore models and mappers
  TvmazeApi.kt      TVMaze search and episode cache
  Design.kt         Theme, palette, buttons, cards, tabs
  Notifications.kt  FCM handling
  Reminders.kt      WorkManager local reminders

app/google-services.json  Firebase configuration and API key
firestore.rules           Firestore access control rules
```

## Ground rules for forks

- Do not commit someone else's private Firebase configuration.
- Keep `firestore.rules` with your fork and deploy it to your own project.
- Test with at least two devices/users before sharing, because progress and notes are group-wide.
- The bundled Firebase project is for the original author's deployment; treat it as a demo, not a neutral backend.
