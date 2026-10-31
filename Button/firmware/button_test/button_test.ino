const int buttonPin = D2;

void setup() {
  pinMode(buttonPin, INPUT_PULLUP);
  pinMode(LED_BUILTIN, OUTPUT);
  Serial.begin(115200);
}

void loop() {
  bool pressed = digitalRead(buttonPin) == LOW;
  digitalWrite(LED_BUILTIN, pressed ? HIGH : LOW);
  static bool previouslyPressed = false;
  if (pressed != previouslyPressed) {
    Serial.println(pressed ? "Button pressed" : "Button released");
    previouslyPressed = pressed;
  }
  delay(20);
}
