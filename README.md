# SmartDashboard-prototype
Software of a motorcyle dashboard prototype that includes speedometer, temperature reading, GPS navigation and smartphone-connected alarm. This repository contains code for an Arduino microcontroller (that deals with sensor data) and for an Android-based dashboard device (for UI, GPS navigation and alarm messaging service).
The following electronics were mounted in the Arduino board used for the prototype:
- An HC-05 Bluetooth Shield module (to communicate with the dashboard wirelessly)
- An electromagnetic sensor (working as a speedometer with a wheel-mounted magnet)
- A thermistor and a resistance (working as a temperature sensor)
- An LED that worked as a warning signal when the alarm is triggered.
- An IMU (compatible with Adafruit libraries) to sense movement when the alarm is active.

The Android code works with a trial version of HERE maps for the maps/navigation features.

Screenshots:
![screenshot_20170504-000557](https://user-images.githubusercontent.com/40466329/46422676-c442ca00-c72c-11e8-9074-7901a323c2c7.png)

![screenshot_20170504-001616](https://user-images.githubusercontent.com/40466329/46422764-eccac400-c72c-11e8-941a-77b070a99162.png)

![screenshot_20170504-001801](https://user-images.githubusercontent.com/40466329/46422778-f18f7800-c72c-11e8-8015-a19fd103981d.png)
