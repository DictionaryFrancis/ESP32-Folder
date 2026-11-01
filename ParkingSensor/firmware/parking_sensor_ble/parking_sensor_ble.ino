#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#include <atomic>

const int trigPin = D4;
const int echoPin = D3; // HC-SR04 ECHO MUST pass through the 5 V -> 3.3 V divider.
const int buzzerPin = D6; // Active-high buzzer control; use a suitable driver if required.
const char* serviceUUID = "4fa10001-6a73-4c31-9145-24e3c84f2100";
const char* distanceUUID = "4fa10002-6a73-4c31-9145-24e3c84f2100";
BLECharacteristic* distanceCharacteristic;
std::atomic<bool> connected(false);
std::atomic<bool> restartAdvertising(false);
unsigned long lastMeasurement = 0;
unsigned long lastBeep = 0;
bool buzzerOn = false;
int beepInterval = 0;
bool continuousBeep = false;

class ConnectionCallbacks : public BLEServerCallbacks {
  void onConnect(BLEServer* server) override {
    connected.store(true);
    Serial.println("Phone connected.");
  }
  void onDisconnect(BLEServer* server) override {
    connected.store(false);
    restartAdvertising.store(true);
    Serial.println("Phone disconnected. Local sensor/buzzer continue.");
  }
};

void measure() {
  digitalWrite(trigPin, LOW);
  delayMicroseconds(2);
  digitalWrite(trigPin, HIGH);
  delayMicroseconds(10);
  digitalWrite(trigPin, LOW);
  unsigned long duration = pulseIn(echoPin, HIGH, 25000UL);
  float cm = duration * 0.0343f / 2.0f;
  bool valid = duration != 0 && cm >= 2 && cm <= 400;
  continuousBeep = valid && cm < 10;
  beepInterval = !valid || cm > 60 ? 0 : (cm < 30 ? 200 : 700);
  if (!valid) Serial.println("No valid reading");
  else { Serial.print("Distance: "); Serial.print(cm, 1); Serial.println(" cm"); }
  char payload[16];
  if (valid) snprintf(payload, sizeof(payload), "%.1f", cm);
  else snprintf(payload, sizeof(payload), "-1");
  distanceCharacteristic->setValue(payload);
  if (connected.load()) distanceCharacteristic->notify();
}

void setup() {
  digitalWrite(buzzerPin, LOW);
  pinMode(buzzerPin, OUTPUT);
  pinMode(trigPin, OUTPUT);
  digitalWrite(trigPin, LOW);
  pinMode(echoPin, INPUT);
  Serial.begin(115200);
  BLEDevice::init("ESP32-Parking");
  BLEServer* server = BLEDevice::createServer();
  server->setCallbacks(new ConnectionCallbacks());
  BLEService* service = server->createService(serviceUUID);
  distanceCharacteristic = service->createCharacteristic(distanceUUID,
    BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_NOTIFY);
  distanceCharacteristic->addDescriptor(new BLE2902());
  distanceCharacteristic->setValue("-1");
  service->start();
  BLEAdvertising* advertising = BLEDevice::getAdvertising();
  advertising->addServiceUUID(serviceUUID);
  advertising->setScanResponse(true);
  advertising->start();
  Serial.println("Ready. Connect to ESP32-Parking using the parking app.");
}

void loop() {
  unsigned long now = millis();
  if (now - lastMeasurement >= 150) {
    lastMeasurement = now;
    measure();
    now = millis();
  }
  if (continuousBeep) buzzerOn = true;
  else if (beepInterval == 0) buzzerOn = false;
  else {
    if (buzzerOn && now - lastBeep >= 70) buzzerOn = false;
    if (!buzzerOn && now - lastBeep >= (unsigned long)beepInterval) {
      buzzerOn = true; lastBeep = now;
    }
  }
  digitalWrite(buzzerPin, buzzerOn ? HIGH : LOW);
  if (restartAdvertising.exchange(false)) BLEDevice::startAdvertising();
  delay(2);
}
