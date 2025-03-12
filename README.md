# TaskManager

TaskManager es una aplicación de gestión de tareas para Android que permite a los usuarios organizar sus actividades mediante una lista interactiva y un calendario. La aplicación incorpora notificaciones, gestión de sesiones, preferencias personalizadas y un sistema multiidioma.

## Requisitos

- **Android:** Mínimo SDK 24, Target SDK 35
- **Lenguaje:** Java
- **Dependencias:**
  - `androidx.appcompat`
  - `com.google.android.material`
  - `androidx.activity`
  - `androidx.constraintlayout`
  - `androidx.recyclerview`
  - `androidx.drawerlayout`
  - `androidx.navigation.ui`
  - `androidx.cardview`
  - `org.mindrot.jbcrypt` (para el cifrado de contraseñas)

## Permisos requeridos

La aplicación requiere los siguientes permisos en Android:

- `POST_NOTIFICATIONS` para enviar notificaciones al usuario.
- `SCHEDULE_EXACT_ALARM` para programar recordatorios de tareas.
- `RECEIVE_BOOT_COMPLETED` para restaurar notificaciones después de reiniciar el dispositivo.

## Instalación y ejecución local

Para clonar y ejecutar la aplicación en **Android Studio**, sigue estos pasos:

1. Clonar el repositorio:
   ```bash
   git clone https://github.com/Markel15/TaskManager.git
