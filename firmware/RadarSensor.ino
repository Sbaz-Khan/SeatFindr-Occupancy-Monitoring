 #include <HardwareSerial.h>

#include <WiFi.h>

#include <Firebase_ESP_Client.h>


#include "addons/TokenHelper.h"

#include "addons/RTDBHelper.h"


// --- Pin Definitions ---

#define RXD2 14 // ESP32 RX <- Sensor TX

#define TXD2 13 // ESP32 TX -> Sensor RX


HardwareSerial RadarSerial(2);


// --- Wi-Fi Setup ---

const char* ssid = "YOUR_WIFI_SSID";

const char* password = "YOUR_WIFI_PASSWORD";


// --- Firebase Setup ---


#define API_KEY ""        //Empty for safety Reasons (not leaking the keys!)

#define DATABASE_URL ""

#define USER_EMAIL ""

#define USER_PASSWORD ""
//dummy user and email so esp connects to firebase

FirebaseData fbdo;

FirebaseAuth auth;

FirebaseConfig config;

// --- Advanced Polling Variables ---
const unsigned long POLLING_WINDOW_MS = 10000; // 10-second voting window
const unsigned long SAMPLE_RATE_MS = 100;      // Take a snapshot every 100ms

unsigned long windowStartTime = 0;
unsigned long lastSampleTime = 0;

// a buckets to hold votes for 0, 1, 2, or 3 people
int targetBuckets[4] = {0, 0, 0, 0}; 
int lowVoteStreak = 0; // Tracks consecutive windows where radar thinks someone left

// Memry bank to hold the last known radar state
int currentRadarTargets = 0; 

// --- Database Variables ---
int roomCapacity = 3;
int occupiedSeats = 0;

// 30-byte buffer to hold exactly one radar frame
uint8_t frameBuffer[30];

void setup() {
  Serial.begin(115200);
  RadarSerial.begin(256000, SERIAL_8N1, RXD2, TXD2);
  
  Serial.println("Occupancy Tracker Initialized.");
  
  // Connect to Wi-Fi
  Serial.print("Connecting to Wi-Fi: ");
  Serial.println(ssid);
  WiFi.begin(ssid, password);

  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("\nWi-Fi Connected!");

  // Connect to Firebase
  Serial.println("Connecting to Firebase...");
  config.api_key = API_KEY;
  config.database_url = DATABASE_URL;
  auth.user.email = USER_EMAIL;
  auth.user.password = USER_PASSWORD;
  config.token_status_callback = tokenStatusCallback; 

  Firebase.begin(&config, &auth);
  Firebase.reconnectWiFi(true);
  
fbdo.setResponseSize(4096);
  config.timeout.serverResponse = 1000 * 15; // 15 seconds
  Serial.println("Firebase Initialized!");
  Serial.println("Testing target: Room A (Detection Area: 1.5m to 5.0m)");
  
  windowStartTime = millis();
}

void loop() {
  // Reads the radar as fast as possible and update the memory bank
  while (RadarSerial.available()) {
    // Shift buffer left
    for (int i = 0; i < 29; i++) {
      frameBuffer[i] = frameBuffer[i + 1];
    }
    frameBuffer[29] = RadarSerial.read();

    // Check Header
    if (frameBuffer[0] == 0xAA && frameBuffer[1] == 0xFF && 
        frameBuffer[2] == 0x03 && frameBuffer[3] == 0x00) {
      
      int parsedTargets = 0; 

      // --- PARSE TARGET 1 ---
      int t1_Y = (frameBuffer[6] | (frameBuffer[7] << 8)) & 0x7FFF;
      if (t1_Y >= 1500 && t1_Y <= 6000) parsedTargets++;

      // --- PARSE TARGET 2 ---
      int t2_Y = (frameBuffer[14] | (frameBuffer[15] << 8)) & 0x7FFF;
      if (t2_Y >= 1500 && t2_Y <= 6000) parsedTargets++;

      // --- PARSE TARGET 3 ---
      int t3_Y = (frameBuffer[22] | (frameBuffer[23] << 8)) & 0x7FFF;
      if (t3_Y >= 1500 && t3_Y <= 6000) parsedTargets++;

      currentRadarTargets = parsedTargets;
      frameBuffer[0] = 0x00; 
    }
  }

  // --- Statistical Polling Logic ---
  
  // 1. we take a sample every 100ms
  if (millis() - lastSampleTime >= SAMPLE_RATE_MS) {
    lastSampleTime = millis();
    
    int vote = currentRadarTargets;
    
    // safety check cap at 3 targets (just in case) 
    if (vote > 3) vote = 3; 
    if (vote < 0) vote = 0;
    
    targetBuckets[vote]++;
  }

  // 2. When the 10-second window is over, tally the votes
  if (millis() - windowStartTime >= POLLING_WINDOW_MS) {
    
    int winningSeatCount = 0;
    int totalValidVotes = targetBuckets[0] + targetBuckets[1] + targetBuckets[2] + targetBuckets[3];

    Serial.print("[Window Closed] Votes -> 0P:"); Serial.print(targetBuckets[0]);
    Serial.print("  1P:"); Serial.print(targetBuckets[1]);
    Serial.print("  2P:"); Serial.print(targetBuckets[2]);
    Serial.print("  3P:"); Serial.println(targetBuckets[3]);

    // --- THE 20% SENSITIVITY THRESHOLD ---
    // 20% is very low, Ideally is around 40-50 (20% for demo only)
    int threshold = totalValidVotes * 0.20; 

    if (targetBuckets[3] >= threshold) {
      winningSeatCount = 3;
    } else if (targetBuckets[2] >= threshold) {
      winningSeatCount = 2;
    } else if (targetBuckets[1] >= threshold) {
      winningSeatCount = 1;
    } else {
      winningSeatCount = 0; 
    }

    // 3. Evaluate the Vote and Update Firebase 
    if (winningSeatCount > occupiedSeats) {
      occupiedSeats = winningSeatCount;
      lowVoteStreak = 0; 
      
      Serial.print("\n[Firebase Push: ++] Seats INCREASED to: ");
      Serial.println(occupiedSeats);
      updateFirebase();
      
    } else if (winningSeatCount < occupiedSeats) {
      lowVoteStreak++;
      Serial.print("[Potential Drop] Waiting to confirm... Streak: ");
      Serial.print(lowVoteStreak);
      Serial.println("/3");

      if (lowVoteStreak >= 1) {
        occupiedSeats = winningSeatCount;
        lowVoteStreak = 0; 
        
        Serial.print("\n[Firebase Push: --] Seats DECREASED to: ");
        Serial.println(occupiedSeats);
        updateFirebase();
      }
    } else {
      lowVoteStreak = 0; 
    }

    // 4. Empty the buckets and restart the timer
    windowStartTime = millis();
    for (int i = 0; i <= 3; i++) {
      targetBuckets[i] = 0;
    }
  }
}

// Function to update Firebase
void updateFirebase() {
  if (Firebase.ready()) {
    String basePath = "/buildings/Hall/floor4/RoomA";

    if (Firebase.RTDB.setInt(&fbdo, basePath + "/occupiedSeats", occupiedSeats)) {
      Serial.println("  -> PASSED: occupiedSeats synced.");
    } else {
      Serial.print("  -> FAILED: ");
      Serial.println(fbdo.errorReason());
    }

    if (Firebase.RTDB.setInt(&fbdo, basePath + "/capacity", roomCapacity)) {
      Serial.println("  -> PASSED: capacity synced.");
    } else {
      Serial.print("  -> FAILED: ");
      Serial.println(fbdo.errorReason());
    }
    Serial.println("-----------------------------");
  } else {
    Serial.println("Firebase not ready. Check Wi-Fi or API Key.");
  }
}
