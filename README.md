# Smart Dashboard prototype #

## Description ##

<img src="https://user-images.githubusercontent.com/40466329/53058003-57e12100-34a9-11e9-8db8-c690f9557239.png" alt="alt text" width="400" height="250"> <img src="https://user-images.githubusercontent.com/40466329/53058049-7c3cfd80-34a9-11e9-9f2f-e5db4c32d7e3.jpg" alt="alt text" width="400" height="250">

The Smart Dashboard was a project developed with [AXIIS](https://www.axiis-ea.com/) that reached the finals on a national contest for Hardware Start-Ups. The prototype consisted of 2 main hardware parts: An an Android-based dashboard device and an Arduino board with some electronics. In this repo, you will find the software that made the prototype work with features that included speedometer, temperature reading, GPS navigation and a smartphone-connected alarm. The Android code works with a trial version of HERE maps for the maps/navigation features.

## Electronics ##

The following electronics were mounted on an Arduino Uno board used for the prototype:
- An HC-05 Bluetooth Shield module (to communicate with the dashboard wirelessly)
- An electromagnetic sensor (working as a speedometer with a wheel-mounted magnet)
- A thermistor and a resistance (working as a temperature sensor)
- An LED that worked as a warning signal when the alarm is triggered.
- An IMU (compatible with Adafruit libraries) to sense movement when the alarm is active.

### Testing footage ###

First test of the electromagnetic sensor on a bicycle wheel

<img src="https://user-images.githubusercontent.com/40466329/53059398-988f6900-34ae-11e9-80d3-a1dca53baf3f.gif" alt="alt text" width="800" height="400">

Applying a heat source to the temperature sensor

<img src="https://user-images.githubusercontent.com/40466329/53058619-b27b7c80-34ab-11e9-9edb-3385705ab0f7.gif" alt="alt text" width="400" height="250">

## Features ##

### Smartphone-connected alarm ###

### GPS and Navigation ###





<!---
Screenshots:
![screenshot_20170504-000557](https://user-images.githubusercontent.com/40466329/46422676-c442ca00-c72c-11e8-9074-7901a323c2c7.png)
-->
<!---
![screenshot_20170504-001616](https://user-images.githubusercontent.com/40466329/46422764-eccac400-c72c-11e8-941a-77b070a99162.png)
-->
<!---
![screenshot_20170504-001801](https://user-images.githubusercontent.com/40466329/46422778-f18f7800-c72c-11e8-8015-a19fd103981d.png)
-->

