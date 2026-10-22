# SLIMiot HRV

Android app that connects to SlimIoT wearables over Bluetooth LE, records beat-to-beat heart data and calculates **heart-rate variability (HRV)** and a stress indicator.

## Features

- Scan for and connect to SlimIoT BLE devices
- Live heart rate
- Time-domain HRV metrics: **RMSSD**, **SDNN**, **pNN50**
- Stress estimate derived from HRV
- User profile and cloud sync of results (Firebase)

## Build

1. Open the `SLIMiot HRV` folder in **Android Studio**.
2. Add your own Firebase config: download `google-services.json` from your Firebase project and place it in `SLIMiot HRV/app/`. (It is git-ignored and not included in this repo.)
3. Build and run on a phone with Bluetooth LE (Android 7.0 / API 24 or later).

## Tech stack

Java · Android SDK 33 · BLE via [FastBle](https://github.com/Jasonchenlijian/FastBle) and [RxAndroidBle](https://github.com/dariuszseweryn/RxAndroidBle) · [MPAndroidChart](https://github.com/PhilJay/MPAndroidChart) / [AndroidPlot](https://androidplot.com/) · Apache Commons Math · Firebase Auth & Firestore

---

Part of the [SlimIoT](https://www.slimiot.com) health-tech ecosystem. Questions or pilots: [info@slimiot.com](mailto:info@slimiot.com)
