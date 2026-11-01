# Arduino Nano ESP32 — Bluetooth Parking Sensor

**Project:** 06 — Distance and Buzzer  
**Date:** 1 November 2026  
**Status:** Standalone sensor/buzzer and Android Bluetooth operation confirmed by the user  
**Board:** Arduino Nano ESP32 (ABX00083)  
**Android project:** Distance_Buzzer  
**Application ID:** com.matiascompany.distance_buzzer  
**On-phone label:** ESP32 Parking Sensor

## 1. Overview

An HC-SR04 measures distance while an active buzzer provides increasingly frequent warnings as an object approaches. The Nano ESP32 also sends measurements to a custom Android app through Bluetooth Low Energy notifications.

The fixed sensor measures proximity in front of it. The app's coloured arc is a proximity indicator, not a directional map. The buzzer continues to work without the phone connected.

## 2. Project files

- [ESP32 sketch](firmware/parking_sensor_ble/parking_sensor_ble.ino).
- [Actual Android Studio project](android/Distance_Buzzer/).
- [MainActivity.kt](android/Distance_Buzzer/app/src/main/java/com/matiascompany/distance_buzzer/MainActivity.kt): interface, scanning, subscription and measurement display.
- [AndroidManifest.xml](android/Distance_Buzzer/app/src/main/AndroidManifest.xml): permissions and launcher.
- [Hardware notes](hardware/README.md).

The Android source is copied from the user's Distance_Buzzer project, not the generic starter. Machine-specific SDK paths, generated builds, IDE caches and signing keys are excluded. The supplied ESP32 sketch is the compiled BLE version from this session; an independently saved final sketch has not yet been supplied for comparison.

KiCad schematic, physical assembly photos and screenshots of the working app are pending. No placeholder photos or fabricated measurement screenshots are included. No APK binary is committed; build it from the source.

## 3. Components and wiring

| Component | Quantity |
|---|---|
| Arduino Nano ESP32 | 1 |
| HC-SR04 ultrasonic sensor | 1 |
| Active buzzer | 1 |
| 1 kohm resistors | 3 (or 1 kohm + 2 kohm) |
| Breadboard and jumpers | As needed |
| USB-C cable and Android 12+ BLE phone | 1 each |

| Connection | Destination |
|---|---|
| Sensor VCC | Nano ESP32 VBUS, USB 5 V |
| Sensor GND | Common GND |
| Sensor TRIG | D4 |
| Sensor ECHO | 1 kohm resistor, then divider junction |
| Divider junction | D3 |
| Divider junction | 2 kohm to GND (two 1 kohm in series are equivalent) |
| Active buzzer control | D6, active HIGH |

```text
HC-SR04 ECHO --[1 kohm]--+---- D3
                        |
                     [2 kohm]
                        |
                       GND
```

The divider reduces a nominal 5 V Echo signal to approximately 3.33 V. The 5 V VBUS supply does not make ESP32 GPIO inputs 5 V tolerant. Keep the divider in place.

The user confirmed a two-legged active buzzer worked after correcting its setup, but the exact correction, rating, current draw and presence of a driver were not recorded. D6 denotes the control output; this documentation does not establish that any two-legged buzzer is suitable for direct GPIO power. Use a transistor driver if its electrical requirements exceed the GPIO limits. Verify the actual buzzer arrangement before reproducing it.

Disconnect USB before rewiring. Use board labels D3/D4/D6, not raw GPIO numbers. The previous motor and DFPlayer code are not used in this experiment.

## 4. Firmware

Open [parking_sensor_ble.ino](firmware/parking_sensor_ble/parking_sensor_ble.ino), select Arduino Nano ESP32 and upload. BLE libraries are supplied by the board package. Serial Monitor: 115200 baud.

Every approximately 150 ms, the firmware triggers a measurement, waits up to 25 ms for Echo and computes centimetres from the pulse duration. Valid distances are 2–400 cm. Timeouts and out-of-range results are sent as -1. The local buzzer uses timed pulses without long delay calls.

| Distance | Phone display | Buzzer |
|---|---|---|
| More than 60 cm, up to 400 cm | Green — beyond warning range | Silent |
| 30–60 cm | Yellow — approaching | Slow beeps (700 ms start interval) |
| 10 to less than 30 cm | Orange — close | Fast beeps (200 ms start interval) |
| 2 to less than 10 cm | Red — very close | Continuous |
| Invalid / no echo | Grey — no valid reading | Silent; not an all-clear indication |

Short beeps target a 70 ms on period; blocking measurement time can affect exact timing. No echo does not prove that the path is clear, especially for very close, angled or sound-absorbing objects.

## 5. Bluetooth protocol

| Item | Value |
|---|---|
| Device name | ESP32-Parking |
| Service UUID | 4fa10001-6a73-4c31-9145-24e3c84f2100 |
| Distance characteristic | 4fa10002-6a73-4c31-9145-24e3c84f2100 |
| Properties | READ and NOTIFY |
| Subscription descriptor | Standard CCCD, 0x2902 |
| Encoding | UTF-8 decimal centimetres, e.g. 24.5 |
| Invalid measurement | -1 |

The Android app discovers this service and enables notifications. Its UUIDs differ from the motor app. A phone running the motor app cannot display these measurements.

## 6. Build and install Android app

1. Open android/Distance_Buzzer in Android Studio and sync Gradle.
2. Keep namespace/applicationId com.matiascompany.distance_buzzer.
3. Minimum Android version is API 31 (Android 12). This was corrected from the template's API 24 setting before the successful build.
4. Build a debug APK or Run on the connected phone.
5. Output: app/build/outputs/apk/debug/app-debug.apk. A distributable copy may be named Distance-Buzzer.apk without changing its package or app label.
6. Copy the APK to the phone's Download folder, install and allow Nearby devices.
7. Enable Bluetooth and tap Connect to sensor inside the app. No Android settings pairing step is required.

The source preserves the user's build versions: Android Gradle Plugin 9.3.3, Gradle 9.5.0 and compile/target SDK 37. See the checked-in version catalog and wrapper configuration. Machine-local SDK paths are recreated by Android Studio.

## 7. Interface and connection handling

The screen displays live centimetres, a coloured proximity arc, status text and Connect/Disconnect buttons. Measurements older than 2 seconds are cleared and marked unavailable. Invalid measurements are grey rather than green. Disconnecting clears the display.

This is a foreground-only Activity: leaving the screen, locking the phone or recreating the Activity disconnects the BLE link. Local distance measurement and buzzer operation continue on the ESP32. The prototype does not add pairing/encryption or device ownership authorization.

## 8. Validation and results

- User confirmed standalone sensor and buzzer operation.
- Android project package, manifest, permissions and service UUIDs were checked.
- minSdk was corrected to 31.
- The user's actual Distance_Buzzer project passed assembleDebug and lintDebug. Lint reported non-blocking items, including unused template resources, dependency updates and UI/resource suggestions.
- Matching ESP32 BLE firmware compiled for arduino:esp32:nano_nora using installed board package 2.0.18-arduino.5: 923973 bytes flash (29%), 56132 bytes global memory (17%).
- After installation, the user reported that it worked, confirming the Bluetooth integration at a functional level.

No controlled distance-accuracy study, full threshold sweep, radio range test or measured reconnect/stale-data timing was reported. Do not treat those as completed tests.

## 9. Reproduction checklist

1. Verify divider and buzzer ratings/driver with power disconnected.
2. Upload the matching parking BLE sketch.
3. Compare Serial Monitor readings against a flat object at known distances.
4. Install the app, connect and compare phone readings with Serial Monitor.
5. Test warning thresholds, missing Echo and disconnect/reconnect behavior.
6. Add dated assembly photos, app screenshots and the final KiCad schematic when available.

## 10. Next steps

- Document the actual buzzer electrical details and correction.
- Add final photos and KiCad schematic.
- Record measured distances and edge-case behavior.
- Optionally mount the sensor on a servo for angle-tagged scans; the present app is not a map.

## References

- [Android BLE data transfer](https://developer.android.com/develop/connectivity/bluetooth/ble/transfer-ble-data).
- [Espressif BLE notification example](https://github.com/espressif/arduino-esp32/tree/master/libraries/BLE/examples/Notify).
- [Repository license](../LICENSE).
