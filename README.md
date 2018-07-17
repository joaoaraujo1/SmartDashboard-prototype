# SmartDashboard-prototype
Software of a motorcyle dashboard prototype that includes speedometer, temperature reading, GPS navigation and smartphone-connected alarm. This repository contains code for an Arduino microcontroller (that deals with sensor data) and for an Android-based dashboard device (for UI, GPS navigation and alarm messaging service).
The following electronics were mounted in the Arduino board used for the prototype:
- An HC-05 Bluetooth Shield module (to communicate with the dashboard wirelessly)
- An electromagnetic sensor (working as a speedometer with a wheel-mounted magnet)
- A thermistor and a resistance (working as a temperature sensor)
- An LED that worked as a warning signal when the alarm is triggered.
- An IMU (compatible with Adafruit libraries) to sense movement when the alarm is active.

The Android code works with a trial version of HERE maps for the maps/navigation features.
