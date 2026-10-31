const int speakerPin = D2;

void setup() {
  pinMode(speakerPin, OUTPUT);
}

void loop() {
  tone(speakerPin, 262);  // C4
  delay(400);

  tone(speakerPin, 330);  // E4
  delay(400);

  tone(speakerPin, 392);  // G4
  delay(400);

  noTone(speakerPin);
  delay(2000);
}
