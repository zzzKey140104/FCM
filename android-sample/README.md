# Android Sample to fetch FCM token and upload to your server

## Steps
1. Open `android-sample/` in Android Studio.
2. In Firebase Console, create a project and add an Android app with package `com.example.fcmtest`. Download `google-services.json` and place it in `android-sample/app/`.
3. Ensure your desktop server runs on port 8080. If using Emulator, keep `SERVER_BASE` as `http://10.0.2.2:8080`. If using a physical device, replace with your PC LAN IP, e.g. `http://192.168.1.10:8080` in `TokenUploader.java`.
4. Build and run the app. Watch Logcat with filter `FCM` to see the token and upload result. The token is uploaded to `/register`.
5. Check MySQL table `ltm.device` for the saved token. Then use the Swing client to send notifications.

## Notes
- Requires Google Play Services (emulator image with Play Store or physical device).
- If you target Android 13+, you may also request POST_NOTIFICATIONS permission for heads-up notifications.
