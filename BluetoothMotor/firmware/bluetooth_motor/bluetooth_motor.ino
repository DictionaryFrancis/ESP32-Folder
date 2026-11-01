#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <atomic>

// Nano ESP32 printed board labels.
const int enablePin = D5;
const int direction1 = D4;
const int direction2 = D3;

// Must match the Android app.
const char* serviceUUID =
  "d8f10001-6a73-4c31-9145-24e3c84f2100";

const char* commandUUID =
  "d8f10002-6a73-4c31-9145-24e3c84f2100";

std::atomic<bool> connected(false);
std::atomic<bool> requestedOn(false);
std::atomic<bool> restartAdvertising(false);

// Bluetooth connection events.
class ConnectionCallbacks : public BLEServerCallbacks {
  void onConnect(BLEServer* server) override {
    requestedOn.store(false);
    connected.store(true);

    Serial.println("Phone connected. Motor OFF.");
  }

  void onDisconnect(BLEServer* server) override {
    connected.store(false);
    requestedOn.store(false);
    restartAdvertising.store(true);

    Serial.println("Phone disconnected. Motor OFF.");
  }
};

// Commands received from the Android app.
class CommandCallbacks : public BLECharacteristicCallbacks {
  void onWrite(BLECharacteristic* characteristic) override {
    // Works with cores returning String or std::string.
    auto received = characteristic->getValue();
    String command(received.c_str());

    command.trim();
    command.toUpperCase();

    if (command == "ON" && connected.load()) {
      requestedOn.store(true);
      Serial.println("Received ON");
    } else if (command == "OFF") {
      requestedOn.store(false);
      Serial.println("Received OFF");
    } else {
      requestedOn.store(false);
      Serial.println("Unknown command. Motor OFF.");
    }
  }
};

void setup() {
  // Disable motor before configuring direction.
  digitalWrite(enablePin, LOW);
  pinMode(enablePin, OUTPUT);

  digitalWrite(direction1, LOW);
  digitalWrite(direction2, LOW);

  pinMode(direction1, OUTPUT);
  pinMode(direction2, OUTPUT);

  // Fixed direction. Swap HIGH/LOW to reverse.
  digitalWrite(direction1, LOW);
  digitalWrite(direction2, HIGH);

  Serial.begin(115200);

  BLEDevice::init("ESP32-Motor");

  BLEServer* server = BLEDevice::createServer();
  server->setCallbacks(new ConnectionCallbacks());

  BLEService* service =
    server->createService(serviceUUID);

  BLECharacteristic* command =
    service->createCharacteristic(
      commandUUID,
      BLECharacteristic::PROPERTY_WRITE
    );

  command->setCallbacks(new CommandCallbacks());

  service->start();

  BLEAdvertising* advertising =
    BLEDevice::getAdvertising();

  advertising->addServiceUUID(serviceUUID);
  advertising->setScanResponse(true);
  advertising->start();

  Serial.println("Ready. Connect to ESP32-Motor using BLE.");
}

void loop() {
  // Motor can run only while connected and commanded ON.
  bool runMotor =
    connected.load() && requestedOn.load();

  digitalWrite(
    enablePin,
    runMotor ? HIGH : LOW
  );

  // Make the ESP32 discoverable again after disconnecting.
  if (restartAdvertising.exchange(false)) {
    BLEDevice::startAdvertising();
    Serial.println("Waiting for a new connection...");
  }

  delay(5);
}