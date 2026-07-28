# 🌡️ CpuTemp - Root Governor Controller

An advanced hardware optimization tool for rooted Android devices. CpuTemp enables direct management of processor frequencies and governor policies via pre-configured performance profiles, bypassing system restrictions to maintain absolute thermal control.

---

## ✨ Key Features

* **⚡ Advanced Governor Selector (Root):** Adjusts CPU behavior in real-time using three dedicated power profiles:
    * **🔴 Boost:** Activates the high-performance governor (*Performance*) to maximize processor clock speeds during heavy tasks or gaming.
    * **🟢 Normal:** Sets the balanced governor (*Interactive/Schedutil*) to optimize the power-to-performance ratio for daily use.
    * **🔵 Cool:** Forces a power-saving state (*Powersave*) by lowering frequencies to mitigate device overheating.
* **📈 Integrated Status Monitor:** Provides an immediate visual overview of the active mode and direct monitoring of thermal fluctuations when switching between hardware profiles.
* **🛡️ Superuser Integration:** Designed to work securely with modern root permission managers such as Magisk and KernelSU.

---

## 🛠️ Technical Stack and Architecture

* **Requirements:** Root access is mandatory for manipulating kernel nodes located at `/sys/devices/system/cpu/`.
* **Security:** Production-grade code optimized and protected against reverse engineering using advanced obfuscation techniques (**R8 / ProGuard**).

---

## 📄 License

Copyright © 2026. All rights reserved.
System utility software protected under private intellectual property rights. Copying or decompilation without prior authorization is prohibited.


<p align="center">
  <img src="https://github.com/Jaypsmall/CpuTemp/blob/master/assets/cputem2.png" width="100%" />
</p>

![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=flat&logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Android-3DDC84?style=flat&logo=android&logoColor=white)
