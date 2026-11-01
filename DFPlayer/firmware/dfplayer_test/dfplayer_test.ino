#include <DFRobotDFPlayerMini.h>

HardwareSerial musicSerial(1);
DFRobotDFPlayerMini player;

void setup() {
  Serial.begin(115200);

  // RX = D4, TX = D3, usando os nomes da placa.
  musicSerial.begin(9600, SERIAL_8N1, D4, D3);

  // Tempo para o DFPlayer arrancar.
  delay(3000);

  Serial.println("Connecting to DFPlayer...");

  if (!player.begin(musicSerial)) {
    Serial.println("DFPlayer not detected.");
    Serial.println("Check power, common GND, RX/TX and SD card.");

    while (true) {
      delay(1000);
    }
  }

  player.volume(5);  // Volume baixo para o primeiro teste.
  delay(300);

  player.playMp3Folder(1);  // /mp3/0001.mp3
  Serial.println("Play command sent: /mp3/0001.mp3");
}

void loop() {
  if (player.available()) {
    uint8_t event = player.readType();
    int value = player.read();

    if (event == DFPlayerError) {
      Serial.print("DFPlayer error: ");
      Serial.println(value);
    } else if (event == DFPlayerPlayFinished) {
      Serial.println("Playback finished.");
    }
  }
}