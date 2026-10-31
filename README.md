# Arduino Nano ESP32 — Learning Projects

A collection of practical electronics and programming projects using the **Arduino Nano ESP32 (ABX00083)**.

This repository records my progress learning embedded systems: building circuits on a breadboard, programming the board, testing its behavior and documenting the connections in KiCad. Each project includes its own code, schematic, available images and test results.

## Projects

| Project | What it explores | Current status |
|---|---|---|
| [01 — Speaker](Speaker/README.md) | Generate musical tones with a speaker and the tone() function | Initial audio test successful |
| [02 — Button](Button/README.md) | Read a push button using INPUT_PULLUP and report state changes | Button input tested successfully; Wi-Fi extension pending |
| [03 — FanBlade](FanBlade/README.md) | Control a DC motor with an L293D and a button | Motor rotation confirmed; additional tests pending |

## Learning goals

- Understand digital inputs and outputs.
- Build and test small electronic circuits.
- Write Arduino sketches for the Nano ESP32.
- Explore Wi-Fi and connected interfaces.
- Document circuits using KiCad schematics.
- Record observed results, limitations and possible improvements.

## Repository structure

```text
ESP32-Folder/
├── README.md
├── PROJECTS.md
├── LICENSE
├── Speaker/
│   ├── README.md
│   ├── firmware/speaker_test/speaker_test.ino
│   ├── hardware/esp32-Speaker.kicad_sch
│   └── docs/images/
├── Button/
│   ├── README.md
│   ├── firmware/button_test/button_test.ino
│   ├── hardware/esp32-Button.kicad_sch
│   └── docs/images/
└── FanBlade/
    ├── README.md
    ├── firmware/fan_blade_test/fan_blade_test.ino
    ├── hardware/esp32-FanBlade.kicad_sch
    └── docs/images/
```

Each new project gets its own folder following the same structure:

- **README.md:** purpose, components, wiring, instructions, test results and future improvements.
- **firmware/:** Arduino sketches, with each .ino file inside a folder of the same name.
- **hardware/:** editable KiCad schematics and other circuit design files.
- **docs/images/:** actual circuit screenshots and assembly photos, when available.

## Getting started

1. Choose a project from the table above and read its README.
2. Check the components and wire the circuit with USB disconnected.
3. Open the project's .ino file in Arduino IDE.
4. Select **Arduino Nano ESP32** and the connected port.
5. Upload and follow the project's test procedure.

Use the board labels such as **D2** in these projects; Arduino board labels and raw ESP32 GPIO numbers are not interchangeable.

## Documentation and test status

Successful results describe observations from the actual experiments. Features still being investigated are marked as pending or not validated.

Some schematics currently use an adapted classic Nano template. Their unused pin labels and complete physical pin mappings must be verified against the Nano ESP32 before PCB design.

The Button project's local Wi-Fi webpage is an unfinished experiment; the working button-only sketch is saved separately in its project folder.

## Next steps

- Improve button handling with software debouncing.
- Add more inputs and interactive behavior.
- Troubleshoot the local Wi-Fi webpage.
- Improve Nano ESP32 schematic symbols and pin documentation.
- Add new projects as experiments are built and tested.

## License

See [LICENSE](LICENSE) for the repository license.
