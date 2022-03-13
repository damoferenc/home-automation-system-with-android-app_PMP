#include <Servo.h> 
#include <Wire.h>

#define CONFIG 0x02
#define TEMPERATURE 0x00

Servo srv; 

int input;
int intStat = 0;
int intPlace = 0;

//bathroom   //0
//bedroom    //1
//kitchen    //2
//livingroom //3
//outside    //4
//window     //5
//cooling    //6

int places[7];
int part = 0;

uint16_t read_register(uint8_t reg_addr)
{
  uint16_t result = 0;
  Wire.beginTransmission(0x40);
  Wire.write(reg_addr);
  Wire.endTransmission();
  
  delay(10);
  
  Wire.requestFrom(0x40, 2);
    while (Wire.available()) { 
    result = Wire.read(); 
    result = result << 8;
    result = result | Wire.read();
    }
  
  return result;  
  
}

void write_register(uint8_t reg_addr, uint16_t data)
{
  uint8_t buf = data >> 8;
   Wire.beginTransmission(0x40);
   Wire.write(reg_addr);
   Wire.write(buf);
   Wire.write((data & 0x00FF));
   Wire.endTransmission();    
}

void setup() {Serial.begin(9600);

  srv.attach(8);
  Wire.begin();
  write_register(CONFIG, 0x0000); //init
  Serial1.begin(9600);

 //init
 //set all leds to 0
 for (int i = 0 ; i < 6 ; i ++){
  places[i] = 0;
 }
  
  DDRA = 0b11111111;

  delay(20); //delay is needed for the sensor
}

double get_temp(void)
{  
  double  temp = 0;
  write_register(CONFIG, 0x0000);
  temp = ((float)((float)read_register(TEMPERATURE)/65536)*165)-40;
  return temp;
}

void window(){
  if(places[5] == 0){
    srv.write(0);
  }
  if(places[5] == 1){
    srv.write(45);
  }
  if(places[5] == 2){
    srv.write(90);
  }
  
}

void loop() {
  // put your main code here, to run repeatedly:

  window(); 

 if (Serial1.available()){
  input = Serial1.read();
  process(input);
 }
 //Serial1.print(get_temp());
 Serial.println(get_temp());
 PORTA = ((places[6]<<6) | (places[4]<<4) | (places[3]<<3) | (places[2]<<2) | (places[1]<<1) | places[0]);
  delay(1000);
  
}

void process(int command){
  if(part == 0){
    intPlace = command - '0';
    part = 1;
  }
  else{
    intStat = command - '0';
    places[intPlace] = intStat;
    
    part = 0;
  }
}
