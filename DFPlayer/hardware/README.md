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

A final KiCad schematic and a photo of the successful VBUS wiring have not yet been supplied. Do not use the historical troubleshooting photo as a final wiring reference.

Reference pinout: https://wiki.dfrobot.com/dfr0299
