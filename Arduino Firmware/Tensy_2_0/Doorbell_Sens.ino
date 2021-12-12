/** 
 *  Dorbell Sensor
 *  
 *  Teensy 2.0 Version. 
 *  -------------------
 *  
 *  This is called a "door bell sensor" because I inted to use it for
 *  exactly this use case although it can be used for similar
 *  application like a light sensor or an alarm to warn if windows/ doors have
 *  been opened etc.....
 *  
 *  The associated hardware is a generic pcb featuring an ic2- bus, analog in 
 *  connections, a simple voltage check and input keys. 
 *  The pcb can easily be adjusted to any application you might think of  
 *  
 *  Although the Tensy is an quite expensive microcontroller, it proved
 *  to be very reliable to me.
 */
//
// Version info and lib's
//
#define FIRMWARE_VERSION "V1.0.0"
#define CHIPSET "Teensy 2.0 // Max310105 // HC05 BT"
//
//
//
#include <Wire.h>
#include "MAX30105.h"
#include <SoftwareSerial.h>
//
// OELD Display
//
#include <Adafruit_GFX.h>
#include <Adafruit_SSD1306.h>
#define SCREEN_WIDTH 128 // OLED display width, in pixels
#define SCREEN_HEIGHT 32 // OLED display height, in pixels
#define OLED_RESET     4  // Reset pin # (or -1 if sharing Arduino reset pin)
Adafruit_SSD1306 display(SCREEN_WIDTH, SCREEN_HEIGHT, &Wire, OLED_RESET);
//
// General info.
//
String fwVersion = FIRMWARE_VERSION;
String dataToSend; //Contains json- data structure of data optained.
String hc05Status;
String doorBellSensorStatus;
//
// Input buffer
//
char connectionStatus [255];
int index;
//
// Voltage check
//
#define VOLTAGE_CHECK_PIN PIN_B2
#define VOLTAGE_LOW_WARNING_PIN PIN_B1
String voltageStatus;
//
// Settings buttons
//
int sensSetButton=11;
//
// Sensitivity setting...
//
// "sens1" contains always the continious read value from the sensor
// "sens2" contains an arbitrary stored value.
//
// For example, if one wants to use the circuit to detect lights on or of.
// Uncover the sensor, the value read is stored in "sens1"
// Cover the sensor, press "sensButton", the value read is stored in "sens2"
// Every time the values read are grater then the value in "sens2" lights off is detected 
// and vice versa...
//
long sens1,sens2;
long lastSens1ValueRead;
int lastSensState;
//
// Doorbell rang xxx times...
//
int doorBellRang=0;
//
// Display logic
//
int displayToShow;
int displaySelectButton = 10;

#define NUMBER_OF_SCREENS 5
#define DOORBELL_SCREEN 0

#define TITLE_SCREEN 1
String titleScreen;

#define STATUS_SCREEN 2
String statusScreenDisplay;

#define SETTINGS_SCREEN 3
#define SENSOR_READINGS_SCREEN 4

#define MESSAGE_SCREEN 5
char incommingMessage[255];
int x, minX; // Parameters used to scroll large messages...
//
// Command mnomics
//
#define COMMAND_LENGTH 4 // Leading bytes of a received string contain the command to evaluate....
#define RESET_COUNTER   "rsct"
#define GET_COUNTER     "gtct" 
#define RECEIVE_MESSAGE "rmsg"  

//
// Max30102/05
//
MAX30105 doorBellSensor;
/*
//
// HC05   <->   Tensy
//  RX          TX (PIN 8)
//  TX          RX (Pin 7)
SoftwareSerial BT(7,8); 
*/
#define txPin 3
#define rxPin 4

//                RX  TX
SoftwareSerial BT(4,   3);

/**
 * The setup, running once per start.
 * 
 * 
 * 
 */
void setup() {
  //
  // Display
  //
  display.begin(SSD1306_SWITCHCAPVCC, 0x3C);
  display.clearDisplay();
  display.display();
  pinMode(displaySelectButton,INPUT_PULLUP);
  displayToShow=DOORBELL_SCREEN;
  //
  // Settings button
  //
  pinMode(sensSetButton,INPUT_PULLUP);
  //
  // Serial
  //
  Serial.begin(115200);
  //
  //Setup HC05:
  //
  pinMode(txPin, OUTPUT);
  pinMode(rxPin,INPUT);
  BT.begin(9600);
  strcpy(connectionStatus,"Not connected");
  //
  // Voltage check pin
  //
  pinMode(VOLTAGE_CHECK_PIN,INPUT_PULLUP);
  pinMode(VOLTAGE_LOW_WARNING_PIN,OUTPUT);
  digitalWrite(VOLTAGE_LOW_WARNING_PIN,LOW);
  //  
  // Configure Max30105
  //
  if (doorBellSensor.begin() == false)
  {
    Serial.println("MAX30105 was not found. Please check wiring/power. ");
    while (1);
  }
  doorBellSensor.setup();
}

/**
 *  Main loop.
 *  
 *  
 *  
 */
void loop() {
  
  displayLogic();
 
  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // Receive data via the serial connection and evaluate if and which commands are received...
  //
  char receivedData[255];
  while (BT.available()>0){
      //received=BT.read();
      receivedData[index]=BT.read();
      if (index<=255) index++;
  }
  if (index>0){
    receivedData[index]='\0';
    strcpy(connectionStatus,receivedData);
   
    //   
    // Resets the doorbell counter to zero....
    //
    if (strcmp(connectionStatus,RESET_COUNTER)==0){
      displayToShow=DOORBELL_SCREEN;
      doorBellRang=0;
    }
    //
    // Sents the doorbell counter status to the
    // device requesting it     
    //
    if (strcmp(connectionStatus,GET_COUNTER)==0)
      BT.println(doorBellRang);
      
    //
    // Changes to the message screen
    //
    if (strncmp(connectionStatus,RECEIVE_MESSAGE,COMMAND_LENGTH)==0)
      displayToShow=MESSAGE_SCREEN;
    
    //
    // Sets the message on the message screen.
    // Precondition: The message screen must be displayed at the time the
    // command is given.
    //
    if (displayToShow==MESSAGE_SCREEN && strncmp(connectionStatus,RECEIVE_MESSAGE,COMMAND_LENGTH)==0){
      strcpy(incommingMessage,&receivedData[COMMAND_LENGTH]);
      x=display.width();                  // This info is needed to scroll lang message strings accros the screen horizontaly...
      minX=-12*strlen(incommingMessage);  // 12 pixels per char for textsize=2
    }
  index=0;
  }
  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //
  // Sensitivity settings.
  // Store value read from Max30105 in "sens2" whenever the
  // approbiate button is pressed.
  //
  int sb=digitalRead(sensSetButton);
  if (sb==LOW && displayToShow==SETTINGS_SCREEN)
    sens2=sens1;
 
  //
  // Voltage check
  //
  int voltageCheck=digitalRead(VOLTAGE_CHECK_PIN);
  if (voltageCheck==HIGH){
    voltageStatus="ok";
    digitalWrite(VOLTAGE_LOW_WARNING_PIN,LOW);
  } else {
    voltageStatus="Low";
    digitalWrite(VOLTAGE_LOW_WARNING_PIN,HIGH);
  }
  //
  // Read Sensor Max30105
  //
  sens1=doorBellSensor.getIR();  
  //
  // TRIGGER ALARM, IF CONDITIONS APPLY.....
  //
  // Check if values stored inside of "sens1" and "sens2" change 
  // from sens1<sens2 to sens1>sens2 and vice versa.
  // A change from one to the other condition means lights have
  // been turned on or of. 
  //
  int sensState;
  if (lastSens1ValueRead<sens2)
   sensState=1; // on
  else
    sensState=0; // of

  if (displayToShow!=SETTINGS_SCREEN){ // Do not trigger an alarm while settings are changed.....
    //
    // Detect if state has changed from covered to uncovered sensor
    //
    if (lastSensState==0 && sensState==1){ // 0 to 1 means sensor has been uncovered.
      //
      // Notify the connected device that the sensor has been uncovered
      // and show "alarm screen" immediadly.
      //
      BT.println("on");
      doorBellRang++;
      displayToShow=DOORBELL_SCREEN;
    }
    //
    // Detect if state has changed from uncovered to covered sensor
    //
    if (lastSensState==1 && sensState==0) // 1 to means sensor has been covered...
      // 
      // Inform connected device...
      //
      BT.println("off");
  }
  lastSens1ValueRead=sens1; 
  lastSensState=sensState;
}

/**
 * Display logic.
 * 
 * Polls the display select key and switches
 * the screens accordingly.
 */
void displayLogic(){
  //
  // Switch screens
  //
  switch (displayToShow){

    case DOORBELL_SCREEN:
    display.clearDisplay();
    display.setTextSize(1);            
    display.setTextColor(SSD1306_WHITE);       
    display.setCursor(0,4);             
    display.println("Doorbell rang:");
    display.setCursor(0,12); 
    display.setTextSize(2);
    display.println(doorBellRang);
    display.display();
    break;
    
    case TITLE_SCREEN:
    display.clearDisplay();
    display.setTextSize(1);            
    display.setTextColor(SSD1306_WHITE);       
    display.setCursor(0,4);             
    display.println("Doorbell Sensor");
    display.println("RetroZock 2021");
    display.println(FIRMWARE_VERSION);
    display.display();
    break;
    
    case STATUS_SCREEN:
    display.clearDisplay();
    display.setTextSize(1);            
    display.setTextColor(SSD1306_WHITE);       
    display.setCursor(0,4);             
    display.print("Bat:");
    display.println(voltageStatus);
    display.print("BT:");
    display.println("-");
    display.display();
    break;

    case SETTINGS_SCREEN:
    display.clearDisplay();
    display.setTextSize(1);            
    display.setTextColor(SSD1306_WHITE);       
    display.setCursor(0,4);    
    display.println("Set sensitivity:");         
    display.print("Read:");
    display.println(sens1);
    display.print("Stored:");
    display.println(sens2);
    display.display();
    break;

    case SENSOR_READINGS_SCREEN:
    display.clearDisplay();
    display.setTextSize(1);            
    display.setTextColor(SSD1306_WHITE);       
    display.setCursor(0,4);             
    display.println("Incomming:");
    display.setCursor(0,12); 
    display.setTextSize(2);
    display.println(sens1);
    display.display();
    break;

    case MESSAGE_SCREEN:
    display.clearDisplay();
    display.setTextSize(1);            
    display.setTextColor(SSD1306_WHITE);       
    display.setCursor(0,4);             
    display.println("Message:");

    display.setCursor(x,16); 
    display.setTextSize(2);
    display.println(incommingMessage);
    //
    // If message received is larger than the screen width, scroll horizontaly. 
    if (x<minX)
      x=display.width();    
    display.display();
    x=x-2;
 
    break;
  }
  //
  // Display select key...
  //
  int displaySelectState=digitalRead(displaySelectButton);
  if (displaySelectState==LOW){
    if (displayToShow<=NUMBER_OF_SCREENS){
      displayToShow++;
      delay(250);
    } else {
      delay(250);
      displayToShow=DOORBELL_SCREEN;
    }
  }
}
