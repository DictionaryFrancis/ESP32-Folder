# DFPlayer hardware notes

## Working connections — Arduino Nano ESP32 (ABX00083)

| From | To |
|---|---|
| Nano ESP32 VBUS | DFPlayer VCC, directly |
| Nano ESP32 GND | DFPlayer GND |
| Nano ESP32 D3 | 1 kohm resistor, then DFPlayer RX |
| Nano ESP32 D4 | DFPlayer TX |
| DFPlayer SPK1 | Speaker red lead |
| DFPlayer SPK2 | Speaker black lead |

Power the Nano ESP32 by USB-C. VBUS supplies the DFPlayer for the demonstrated low-volume test. Do not use VIN as a USB 5 V output. Do not join the external Elegoo 5 V output to VBUS. Disconnect the Elegoo supply from this experiment.

The resistor belongs in D3 -> RX, not in the VCC supply or between VCC and RX. Neither speaker lead connects to GND. No 330 ohm speaker resistor or L293D is used.

The speaker's impedance and rated power have not been verified. Successful low-volume playback does not establish the USB/board power budget for louder playback.

## Schematic and photos

- [Editable KiCad schematic](esp32-DFPlayer.kicad_sch).
- [Schematic screenshot](../docs/images/kicad-schematic.png).
- [Assembly photo](../docs/images/breadboard-assembly.jpeg).
- [Nano ESP32 VBUS detail](../docs/images/nano-esp32-vbus-connection.jpeg).
- [DFPlayer connector detail](../docs/images/dfplayer-connections.jpeg).

J3 represents the left DFPlayer header using local connector numbers. The unused right header is omitted. The adapted Nano template has VBUS corrected; unused board labels and PCB mapping are not fully verified. No KiCad electrical-rule check was run during documentation.

The [supplied Arduino Nano ESP32 manual](../docs/references/arduino-nano-esp32-user-manual.pdf), section 11.4 on page 15, documents USB-powered VBUS. The original failed external-supply photo remains historical evidence only.

Reference pinout: https://wiki.dfrobot.com/dfr0299
