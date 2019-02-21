# Smart Dashboard prototype #

## Description ##

<img src="https://user-images.githubusercontent.com/40466329/53058003-57e12100-34a9-11e9-8db8-c690f9557239.png" alt="alt text" width="400" height="250"> <img src="https://user-images.githubusercontent.com/40466329/53058049-7c3cfd80-34a9-11e9-9f2f-e5db4c32d7e3.jpg" alt="alt text" width="400" height="250">

The Smart Dashboard was a project developed with [AXIIS](https://www.axiis-ea.com/) that reached the finals on a national contest for Hardware Start-Ups. The prototype consisted of 2 main hardware parts: An Android-based dashboard device and an Arduino board with some electronics. In this repo, you will find the software that made the prototype work with features that included speedometer, temperature reading, GPS navigation and a smartphone-connected alarm. The Android code works with a trial version of HERE maps for the maps/navigation features.

In this repository, you will find the complete Arduino and Android code that made the prototype work and some testing footage to the code and electronics in the earlier stages of development of the dashboard.

## Electronics ##

The following electronics were mounted on an Arduino Uno board used for the prototype:
- An HC-05 Bluetooth Shield module (to communicate with the dashboard wirelessly)
- An electromagnetic sensor (working as a speedometer with a wheel-mounted magnet)
- A thermistor and a resistance (working as a temperature sensor)
- An LED that worked as a warning signal when the alarm is triggered.
- An IMU (compatible with Adafruit libraries) to sense movement when the alarm is active.

**First test of the electromagnetic sensor on a bicycle wheel**

<img src="https://user-images.githubusercontent.com/40466329/53059398-988f6900-34ae-11e9-80d3-a1dca53baf3f.gif" alt="alt text" width="800" height="400">

**Applying a heat source to the temperature sensor**

<img src="https://user-images.githubusercontent.com/40466329/53058619-b27b7c80-34ab-11e9-9edb-3385705ab0f7.gif" alt="alt text" width="400" height="250">

## Features ##

### Smartphone-connected alarm ###
The alarm was programmed to work with a SIM card used exclusively for the dashboard. When the alarm is active, the dashboard passively scans for movement (using the IMU). If it senses movement, it rings a small warning two times. If the dashboard senses the bike is being moved after those warnings, the alarm increases its ringing time and periodically sends the owner messages with the bike's location until being switched off. To switch off the alarm, the user sets his own password or code.

**Testing position broadcast mechanism and prototypical message**

<img src="https://user-images.githubusercontent.com/40466329/53130553-8a9b2000-3562-11e9-8744-6ea364eee414.gif" alt="alt text" width="500" height="200"> <img src="https://user-images.githubusercontent.com/40466329/53130547-88d15c80-3562-11e9-9fec-1a61e524e58f.png" alt="alt text" width="250" height="250">

### GPS and Navigation ###
The dashboard's navigation system was programmed using [HERE Maps SDK](https://developer.here.com/develop/mobile-sdks) trial version and [Google Location Services](https://developers.google.com/android/reference/com/google/android/gms/location/LocationServices). The user can choose his destination by writing it in a tab that constantly runs HERE Maps smart algorithm to try and complete the user's destination. Alternatively, the user can simply click on the map and get a route to the clicked location. The shortest and fastest way are calculated and can be previewed by a single box click. The user can also run a simulation of the route to be used.

<img src="https://user-images.githubusercontent.com/40466329/46422764-eccac400-c72c-11e8-941a-77b070a99162.png" alt="alt text" width="430" height="220"> <img src="https://user-images.githubusercontent.com/40466329/53207907-f1850b80-362c-11e9-89bf-210ee6191de0.gif" alt="alt text" width="430" height="220"> 

<img src="https://user-images.githubusercontent.com/40466329/53208260-031ae300-362e-11e9-80a2-96e2d09071c5.gif" alt="alt text" width="430" height="220"> <img src="https://user-images.githubusercontent.com/40466329/53208505-f1860b00-362e-11e9-8e03-3897af4d79e7.gif" alt="alt text" width="430" height="220"> 

