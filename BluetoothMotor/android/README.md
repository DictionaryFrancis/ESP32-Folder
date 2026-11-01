# Android Studio — ESP32 Motor

Open the MotorDC_via_BLT directory beside this file as a project in Android Studio. This is a source snapshot of the user's actual working app, not the earlier generic example.

## Important files

- [MainActivity.kt](MotorDC_via_BLT/app/src/main/java/com/matiascompany/motordc_via_blt/MainActivity.kt)
- [AndroidManifest.xml](MotorDC_via_BLT/app/src/main/AndroidManifest.xml)
- [App configuration](MotorDC_via_BLT/app/build.gradle.kts)
- [Dependency versions](MotorDC_via_BLT/gradle/libs.versions.toml)

Do not rename MainActivity.kt or change package names merely to rename the APK. App ID/namespace: com.matiascompany.motordc_via_blt. On-phone label: ESP32 Motor. The Kotlin and manifest files are the two primary implementation files, but the full project is provided so resources and dependencies remain available.

## Build

1. Open MotorDC_via_BLT in Android Studio.
2. Let Android Studio configure your local SDK path and sync Gradle.
3. Use the tool versions declared by the supplied project: compile/target SDK 37, Android Gradle Plugin 9.3.3, Gradle 9.5.0 and the Java 25 daemon toolchain specified by the wrapper configuration. The app's Java source/target compatibility setting is 11. These describe the inspected project, not a newly verified build environment.
4. Although minSdk is declared as 24, use an Android 12+ phone for this implementation. Older Android BLE permissions and API fallbacks are not implemented. This mismatch is documented rather than silently changed.
5. Build a debug APK through Android Studio's Build menu, or run gradlew.bat assembleDebug from this project directory.
6. Find app/build/outputs/apk/debug/app-debug.apk. You may rename a distributable copy to ESP32-Motor-Bluetooth-v1.0.apk; keep the .apk extension. This does not change the app label/package.
7. Transfer it to the phone's Download folder and install, or use Android Studio Run on the connected phone.
8. Allow Nearby devices permission and connect inside the app to ESP32-Motor.

The original project and its generated APK remain unchanged. Build output, local.properties, IDE caches and signing keys are not copied here. No APK binary is committed by this update. A locally built debug APK uses that machine's debug signing key, so another machine's debug build may require uninstalling the previous app before installation.

For ESP32 firmware, wiring and test results, see the [project README](../README.md).
