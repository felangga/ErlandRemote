// Created by Felix Angga
// 2021. All Rights Reserved.

#include <ArduinoJson.h>
#include <IRsend.h>
#include <IRrecv.h>
#include <IRremoteESP8266.h>
#include <IRutils.h>

#include "BluetoothSerial.h"
#define CODENAME "MOB"

// The GPIO an IR detector/demodulator is connected to. Recommended: 14 (D5)
// Note: GPIO 16 won't work on the ESP8266 as it does not have interrupts.
const uint16_t kRecvPin = 14;

// GPIO to use to control the IR LED circuit. Recommended: 4 (D2).
const uint16_t kIrLedPin = 4;

// The Serial connection baud rate.
// NOTE: Make sure you set your Serial Monitor to the same speed.
const uint32_t kBaudRate = 115200;

// As this program is a special purpose capture/resender, let's use a larger
// than expected buffer so we can handle very large IR messages.
const uint16_t kCaptureBufferSize = 1024;  // 1024 == ~511 bits

// kTimeout is the Nr. of milli-Seconds of no-more-data before we consider a
// message ended.
const uint8_t kTimeout = 50;  // Milli-Seconds

// kFrequency is the modulation frequency all UNKNOWN messages will be sent at.
const uint16_t kFrequency = 38000;  // in Hz. e.g. 38kHz.

BluetoothSerial ESP_BT;

IRsend irsend(kIrLedPin);
// The IR receiver.
IRrecv irrecv(kRecvPin, kCaptureBufferSize, kTimeout, false);
// Somewhere to store the captured message.
decode_results results;
bool modeTambah = false;


void setup() {
  irrecv.enableIRIn();  // Start up the IR receiver.
  irsend.begin();       // Start up the IR sender.

  Serial.begin(115200);
  Serial.println("RUNNING...");
  ESP_BT.begin("ErlandRemote");
}

void loop() {
  if (irrecv.decode(&results) && modeTambah) {  // We have captured something.
    uint16_t *raw_array = resultToRawArray(&results);
    uint16_t size = getCorrectedRawLength(&results);

    StaticJsonDocument<4092> doc;
    doc["cmd"] = "add";
    doc["size"] = size;
    JsonArray data = doc.createNestedArray("data");
    for (int i = 0; i < size; i++)
    {
      data.add(raw_array[i]);
    }

    String output;
    serializeJson(doc, output);

    Serial.println(output);
    ESP_BT.println(output);

    irrecv.resume();

    modeTambah = false;
  }
  if (ESP_BT.available()) {
    String command;
    while (ESP_BT.available()) {
      char kar = ESP_BT.read();

      if (kar != '\n') command += String(kar);
    }
    command.trim();

   

    StaticJsonDocument<4092> doc;
    DeserializationError error = deserializeJson(doc, command);
    if (error) {
      Serial.print(F("deserializeJson() failed: "));
      Serial.println(error.f_str());
      return;
    }
    if (doc["cmd"] == "send") {
      uint16_t panjang = doc["size"];
      uint16_t buffer[panjang];
      for (int i = 0; i < panjang; i++) {
        buffer[i] = (unsigned int)doc["data"][i];
      }
      irsend.sendRaw(buffer, panjang, kFrequency);
      irrecv.resume();
    } else if (doc["cmd"] == "tambah") {
      modeTambah = true;
      Serial.println("OnAdd ...");
      delay(500);
    } else if (doc["cmd"] == "cancelTambah") {
      modeTambah = false;      
      Serial.println("OnCancel ...");
    }
  }
  yield();
}
