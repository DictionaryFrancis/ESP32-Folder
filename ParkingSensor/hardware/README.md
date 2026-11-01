# Parking sensor hardware notes

TRIG: D4. ECHO: D3 through a voltage divider. Active buzzer control: D6. Sensor supply: USB VBUS (5 V). Common GND.

The divider is ECHO -> 1 kohm -> D3 junction -> 2 kohm -> GND. Use two 1 kohm resistors in series for the 2 kohm branch if needed. Do not connect 5 V ECHO directly to the ESP32.

The user reported a working two-leg active buzzer; current/voltage ratings and the exact correction have not been recorded. Verify GPIO suitability or fit a transistor driver before reproducing the build.

KiCad schematic and photographs are pending. See the [project README](../README.md) for the connection table and test history.
