#include<math.h>
#include <Wire.h>
#include <Adafruit_Sensor.h>
#include <Adafruit_ADXL345_U.h>

//SPEEDOMETER variables
//const int ledPin = 7; // the pin that the LED is attached to
const int veloPin = 8; // Speedometer Pin
double previouscycle = 0.0;
double cycleduration = 0.0;
double Speed = 0.0;
bool addcycle = true; 

//THERMISTOR variables
const int temperaturePin = A0; // the pin that the resistance is attached to
bool refresh_temp = true;
double tempRefreshTime = 0.0;

//ALARM variables
float  xi,yi,zi,ai;
float  xf,yf,zf,af;
float  dx,dy,dz,da;
float  sense = 0.7; // sensitivity index
int buzzerPin = 13;
int k = 0; // numero de vezes que o alarme ja tocou
boolean alarm_active = false;
boolean set_alarm_time = true;
double time_activated = 0.0;
boolean alarm_triggered = false;
Adafruit_ADXL345_Unified accel = Adafruit_ADXL345_Unified(12345);

//SPEEDOMETER FUNCTION
void speed_calc(bool switch_state)
{
  switch(switch_state)
  {
    case 1:   //em repouso
      addcycle = true;  
      break;
    case 0:   //sensor activado
      if(addcycle)
      {
        double perimeter = 2.055;
        addcycle = false;
        cycleduration = millis() - previouscycle;
        Speed = cycleduration/1000;
        Speed /= 3600;
        Speed = (perimeter/1000) / Speed;
        Serial.print('V');
        Serial.println(Speed,0);
        previouscycle = millis();
      }
      break;
   }
 // digitalWrite(ledPin, switch_state);    // sets the LED to the button's value 
}

//THERMISTOR FUNCTION
void thermistor(int RawADC) {
     double check_Temp;
     check_Temp = log(10000.0*((1024.0/RawADC-1))); 
     check_Temp = 1 / (0.001129148 + (0.000234125 + (0.0000000876741 * check_Temp * check_Temp ))* check_Temp );
     check_Temp = check_Temp - 273.15;            // Convert Kelvin to Celsius
     if(check_Temp - (int) check_Temp >= 0.5) check_Temp == (int) check_Temp + 1;
     Serial.print('T');
     Serial.println(check_Temp,0);
    
}

//ALARM FUNCTION
void alarm(double time_of_activation) {
      sensors_event_t event; 
      accel.getEvent(&event);

    if(millis() - time_of_activation < 1000) // delay of 10 seconds before starting the alarm
    {
      xi = event.acceleration.x;
      yi = event.acceleration.y;
      zi = event.acceleration.z;
      k = 0;
      
    } else {
      xf = event.acceleration.x;
      yf = event.acceleration.y;
      zf = event.acceleration.z;
      
      af = sqrt((xf*xf) + (yf*yf) + (zf*zf));
      dx = (xf - xi);
      dy = (yf - yi);
      dz = (zf - zi);
      ai = sqrt((xi*xi) + (yi*yi) + (zi*zi));
      da = (ai - af);
    
    
      if ((dx > sense) || (dy > sense) || (dz > sense) || (da > sense)){ alarm_triggered = true; ++k; }
    
      int reps = 0;
      
      if(k > 0 && k < 3) reps = 3;
      if( k >= 3) reps = 20;

      
      if(alarm_triggered) {
        for (int i=0; i < reps ; i++){
          
          if(i == 0 && reps == 20) Serial.println("MSG"); //Send coordinates only after first warnings
          
          digitalWrite(buzzerPin, HIGH);   
          delay(250);                       
          digitalWrite(buzzerPin, LOW);    
          delay(250);               
        }
        // reset initial position
        xi = event.acceleration.x;
        yi = event.acceleration.y;
        zi = event.acceleration.z;     

        alarm_triggered = false;

        delay(5000);
      }
      
     }
}

void setup() {
  // initialize serial communication:
  Serial.begin(9600);
  // initialize speedometer PIN;
  pinMode(veloPin, INPUT_PULLUP);
  //initialize the temperature pin 
  pinMode(temperaturePin, INPUT);
  // initialize the buzzer pin:
  pinMode(buzzerPin, OUTPUT);

    /* Initialise the sensor */
  if(!accel.begin())
  {
    /* There was a problem detecting the ADXL345 ... check your connections */
    //Serial.println("Ooops, no ADXL345 detected ... Check your wiring!");
  }

  previouscycle = millis();
}

void loop() {

 // if(!alarm_active) // normal main activity state
 // {

    //SPEEDOMETER  
    speed_calc(digitalRead(veloPin)); 
   
    //THERMISTOR each 5 seconds update
    if(millis() - tempRefreshTime >= 5000 || tempRefreshTime == 0)
    {
      thermistor(analogRead(temperaturePin));
      tempRefreshTime = millis();
    }
  //}

  //ALARM SWITCH
  if(alarm_active)
  {
    if(set_alarm_time) { time_activated = millis(); set_alarm_time = false; }
    alarm(time_activated);
  }

  //ALARM LISTENER
  if (Serial.available() > 0) {
          // read the incoming byte:
          byte incomingByte = Serial.read();
          //Serial.println(incomingByte);
          if(incomingByte == 1){ Serial.println("A1"); delay(500); set_alarm_time = true; alarm_active = true; }
          else if(incomingByte == 2){ Serial.println("A0"); alarm_active = false; }
  }
}


