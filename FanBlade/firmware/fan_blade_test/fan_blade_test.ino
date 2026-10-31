// Arduino Nano ESP32: use printed board labels, not raw GPIO numbers.
const int buttonPin = D11;
const int enablePin = D5;
const int direction1 = D4;
const int direction2 = D3;

bool lastReading = false;
bool motorRunning = false;
unsigned long lastChange = 0;

void setup() {
  digitalWrite(enablePin, LOW);
  pinMode(enablePin, OUTPUT);
  digitalWrite(direction1, LOW);
  digitalWrite(direction2, LOW);
  pinMode(direction1, OUTPUT);
  pinMode(direction2, OUTPUT);
  pinMode(buttonPin, INPUT_PULLUP);
  Serial.begin(115200);

  // Swap HIGH and LOW here to reverse direction, with the motor stopped.
  digitalWrite(direction1, HIGH);
  digitalWrite(direction2, LOW);
}

void loop() {
  bool pressed = digitalRead(buttonPin) == LOW;
  if (pressed != lastReading) {
    lastReading = pressed;
    lastChange = millis();
  }
  if (millis() - lastChange >= 30 && pressed != motorRunning) {
    motorRunning = pressed;
    digitalWrite(enablePin, motorRunning ? HIGH : LOW);
    Serial.println(motorRunning ? "Motor on" : "Motor off");
  }
}
