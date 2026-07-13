# 🌡️ CpuTemp - Root Governor Controller

Una herramienta avanzada de optimización de hardware para dispositivos Android con acceso Root. CpuTemp permite gestionar de forma directa las frecuencias y políticas de los gobernadores del procesador a través de perfiles de rendimiento preconfigurados, rompiendo las restricciones del sistema para mantener un control térmico absoluto.

---

## ✨ Características Principales

* **⚡ Selector de Gobernador Avanzado (Root):** Ajusta el comportamiento de la CPU en tiempo real mediante tres perfiles de energía dedicados:
    * **🔴 Boost:** Activa el gobernador de alto rendimiento (*Performance*) para exprimir los hercios del procesador en tareas pesadas o juegos.
    * **🟢 Normal:** Configura el gobernador equilibrado (*Interactive/Schedutil*) para optimizar el ratio consumo-potencia en el uso diario.
    * **🔵 Cool:** Fuerza un estado de ahorro de energía (*Powersave*) reduciendo frecuencias para mitigar el calentamiento del terminal.
* **📈 Monitor de Estado Integrado:** Control visual inmediato del modo activo y monitorización directa de las variaciones térmicas al conmutar entre los distintos perfiles de hardware.
* **🛡️ Integración con Superusuario:** Diseñada para trabajar de forma segura con gestores de permisos Root modernos como Magisk y KernelSU.

---

## 🛠️ Stack Técnico y Arquitectura

* **Requisitos:** Acceso Root obligatorio para la manipulación de los nodos del kernel en `/sys/devices/system/cpu/`.
* **Seguridad:** Código de producción optimizado y protegido contra ingeniería inversa mediante técnicas de ofuscación avanzada (**R8 / ProGuard**).

---

## 📄 Licencia

Copyright © 2026. Todos los derechos reservados.
Software de utilidad de sistema protegido bajo propiedad intelectual privada. Queda prohibida su copia o descompilación sin autorización previa.


<p align="center">
  <img src="https://github.com/user-attachments/assets/a61784bf-9482-4783-933a-a0c7ea3e42e1" width="30%" />
  <img src="https://github.com/user-attachments/assets/f721ad57-c3e9-48e4-99b9-088af9d3e80a" width="30%" />
</p>

![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=flat&logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Android-3DDC84?style=flat&logo=android&logoColor=white)
