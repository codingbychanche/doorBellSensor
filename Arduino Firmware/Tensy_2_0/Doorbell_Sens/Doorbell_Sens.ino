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
 #include "Gfx.h"
//
// Version info and lib's
//
#define FIRMWARE_VERSION "V1.0.0"
#define CHIPSET "Teensy 2.0 // Max310105 // HC05 BT"
//
//
//
int TRUE=0;
int FALSE=1;
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
#define SCREEN_WIDTH    128   // OLED display width, in pixels
#define SCREEN_HEIGHT   32    // OLED display height, in pixels
#define OLED_RESET      4     // Reset pin # (or -1 if sharing Arduino reset pin)
#define BMP_WIDTH       16
#define BMP_HEIGHT      8
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
int sensSetButton=PIN_D6;
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
String sensSetState;
int numberOfTimesNotSend;
#define WAIT_TIMES_UNTIL_SEND 55
//
// Doorbell rang xxx times...
//
int doorBellRang=0;
//
// Other sensor readings
//
float temperatureC,temperatureF;
//
// Display logic
//
int displayToShow;
int displaySelectButton = PIN_C7;
int displayIsLocked=FALSE;

#define NUMBER_OF_SCREENS 4
#define DOORBELL_SCREEN 0

#define TITLE_SCREEN 1
String titleScreen;

#define SETTINGS_SCREEN 2
#define SENSOR_READINGS_SCREEN 3

#define MESSAGE_SCREEN 4
char incommingMessage[255];
int x, minX; // Parameters used to scroll large messages...
//
// Command mnemoics
//
#define COMMAND_LENGTH 4        // # of leading bytes of a received string contain the command to evaluate....
#define RESET_COUNTER   "rsct"  // Reset doorbell counter
#define GET_COUNTER     "gtct"  // Get the present value and send to connected device
#define RECEIVE_MESSAGE "rmsg"  // A message was send from the connected device, receive and display  
#define LOCK_KEYS       "lock"  // Locks the keys connected to the sensor
#define UNLOCK_KEYS     "ulck"  // Unlock..
#define SET_SENSITIVITY "ssns"  // The current value from the sensor is saved.
#define NEXT_SCREEN     "incs"  // Next screen on display.
#define LAST_SCREEN     "decs"  // Previous screen on display
//
// Max30102/05
//
MAX30105 doorBellSensor;
/*
//
// HC05   <->   Tensy
//  RX          TX (PIN 8)
//  TX          RX (Pin 7)
SoftwareBT BT(7,8); 
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
    BT.println("MAX30105 was not found. Please check wiring/power. ");
     display.setTextSize(1);      
    display.setCursor(0,10);             
    display.println("RetroZock 2021");
    display.println(FIRMWARE_VERSION);
    display.display();

    while (1);
  }
  doorBellSensor.setup();
  sensSetState="Not Set";
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
  // Receive data via the BT connection and evaluate if and which commands are received...
  //
  //
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
    // Lock keys
    //
    if (strcmp(connectionStatus,LOCK_KEYS)==0)
      displayIsLocked=TRUE;
    //
    // Unlock keys
    //
      if (strcmp(connectionStatus,UNLOCK_KEYS)==0)
      displayIsLocked=FALSE;
    //   
    // Resets the doorbell counter to zero....
    //
    if (strcmp(connectionStatus,RESET_COUNTER)==0){
      displayToShow=DOORBELL_SCREEN;
      doorBellRang=0;
    }
    //
    // Sends the doorbell counter status to the
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
    //
    // Set sensitivity
    //
    if (strcmp(connectionStatus,SET_SENSITIVITY)==0){
      sens2=sens1;
      sensSetState="Set";
    }
    //
    // Display next screen
    //
    if (strcmp(connectionStatus,NEXT_SCREEN)==0){
      if (displayToShow<NUMBER_OF_SCREENS){
        displayToShow++;
      }else{ 
        displayToShow=DOORBELL_SCREEN;
      }
    }
    //
    // Display previous screen
    //
    if (strcmp(connectionStatus,LAST_SCREEN)==0){
      if (displayToShow>0){
        displayToShow--;
      }else{ 
        displayToShow=NUMBER_OF_SCREENS;
      }
    }
  index=0;
  }
  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // Sensitivity settings.
  //
  // Store value read from Max30105 in "sens2" whenever the
  // approbiate button is pressed.Precondition: The settings screen must be displayed at the time the
  // button is pressed.
  //
  int sb=digitalRead(sensSetButton);
  if (sb==LOW && displayToShow==SETTINGS_SCREEN){
    sens2=sens1;
    sensSetState="Set";
  }
 
  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // Read Sensor Max30105
  //
  sens1=doorBellSensor.getIR();  
  temperatureC=doorBellSensor.readTemperature();
  temperatureF=doorBellSensor.readTemperatureF();
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
      //BT.println("on");
      doorBellRang++;
      displayToShow=DOORBELL_SCREEN;

      // Anything sens to the connected app will result in an alarm...
      BT.print("rang");
      }
    //
    // Detect if state has changed from uncovered to covered sensor
    //
    if (lastSensState==1 && sensState==0); // 1 to means sensor has been covered...
      // 
      // Inform connected device...
      //
      //BT.println("off");
  }
  //
  // Send all data read from the sensor, continiously via the BT connection...
  //

  if (displayToShow != SETTINGS_SCREEN && displayToShow != SENSOR_READINGS_SCREEN){
    
      // Short data chunks seem to be working :-)

      String d=d+"{\"doorbell_rang\":"+doorBellRang+",\"sens_set_to\":"+sens2+"}";
      BT.println(d);
      d="";
  }
      
      
  //
  // Repeat....
  //
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

  initScreen();
  //
  // Switch screens
  //
  switch (displayToShow){

    case DOORBELL_SCREEN:  
              
    display.drawBitmap(1,14,bell_bmp,32,19,1);
    display.setCursor(40,16); 
    display.setTextSize(2);
    display.println(doorBellRang);
    display.display();
    break;
    
    case TITLE_SCREEN:     
    display.setTextSize(1);      
    display.setCursor(0,10);             
    display.println("RetroZock 2021");
    display.println(FIRMWARE_VERSION);
    display.display();
    break;

    case SETTINGS_SCREEN:
    display.setTextSize(1);               
    display.setCursor(0,10);    
    display.println("Set sensitivity:");         
    display.print("R:");
    display.print(sens1);
    display.print(" S:");
    display.println(sens2);
    display.display();
    break;

    case SENSOR_READINGS_SCREEN:
    display.setTextSize(1);                   
    display.setCursor(0,10);             
    display.println("Incomming:");
    display.setCursor(0,20); 
    display.setTextSize(2);
    display.println(sens1);
    display.display();
    break;

    case MESSAGE_SCREEN:
    display.setTextSize(1);              
    display.setCursor(x,17); 
    display.setTextSize(2);
    display.println(incommingMessage);
    //
    // If message received is larger than the screen width, scroll horizontaly. 
    //
    if (x<minX)
      x=display.width();    
    display.display();
    x=x-8;
    break;
  }
  //
  // Display select key...
  //
  int displaySelectState=digitalRead(displaySelectButton);
  if (displaySelectState==LOW){
    if (displayToShow<=NUMBER_OF_SCREENS){
      displayToShow++;
    }else{ 
      displayToShow=DOORBELL_SCREEN;
    }
  }
}

/**
 * Inits the screen.
 * 
 * Should be called before anything on the screen is 
 * changed. 
 * 
 * Clears the screen and draws the status line. Performs various checks
 * (e.g. voltage status) and displays the result.
 */
 void initScreen (){
  display.clearDisplay();
  display.setTextSize(1);            
  display.setTextColor(SSD1306_WHITE);   
  display.setCursor(0,0);
  
  display.drawLine(0,9, display.width(), 9, SSD1306_WHITE);
  //
  // Voltage check
  //
  int voltageCheck=digitalRead(VOLTAGE_CHECK_PIN);
  if (voltageCheck==HIGH){
    display.drawBitmap(0,0,bat_full_bmp,BMP_WIDTH,BMP_HEIGHT,1);
    digitalWrite(VOLTAGE_LOW_WARNING_PIN,LOW);
    voltageStatus="OK";
  } else {
    display.drawBitmap(0,0,bat_low_bmp,BMP_WIDTH,BMP_HEIGHT,1);
    digitalWrite(VOLTAGE_LOW_WARNING_PIN,HIGH);
    voltageStatus="Low";
  }
  //
  // Display locked?
  //
  // If so, lock and show the message screen. When unlocked, the
  // last screen shown before the screen was locked will be shown again...
  //
  if (displayIsLocked==TRUE){
      display.drawBitmap(BMP_WIDTH+4,0,key_bmp,BMP_WIDTH,BMP_HEIGHT,1);
      displayToShow=MESSAGE_SCREEN;
  }
  if (displayIsLocked==FALSE){
     display.drawBitmap(BMP_WIDTH+4,0,unlocked_bmp,16,8,1);
  }
  //
  // Sensor was set?
  //
  display.setCursor(40,0);
  display.print (sensSetState);
  //
  // A little feature :-)
  //
  display.setCursor(100,0);
  display.print ((int)temperatureC);
  display.print("'C");
 }
 /**
  * Send all data aquired via the BT connection.
  * 
  * 
  */
  void sendData(){

    if (numberOfTimesNotSend>WAIT_TIMES_UNTIL_SEND){

      numberOfTimesNotSend=0;
    }
    numberOfTimesNotSend++;
  }
