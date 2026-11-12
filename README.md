# PayRoutine

PayRoutine es una aplicación Android de finanzas personales desarrollada para el Taller de Proyecto de Especialidad de la carrera Técnico en Programación y Análisis de Sistemas de AIEP.

Su objetivo es ayudar a organizar ingresos, gastos, presupuestos y pagos pendientes en un solo lugar. Usa Kotlin, Jetpack Compose y Material 3. Los datos financieros se almacenan en el dispositivo mediante Room y SQLite.

## Funciones del MVP

El alcance del MVP incluye las siguientes funciones:

- Registrar y consultar ingresos y gastos con monto, fecha, categoría y una nota opcional.
- Definir presupuestos mensuales por categoría y consultar cuánto se ha utilizado.
- Organizar pagos pendientes y cuotas, consultar sus vencimientos y programar recordatorios.
- Consultar el saldo mensual, los totales de ingresos y gastos, y la distribución de gastos por categoría.

## Cómo ejecutar el proyecto

Necesitas Android Studio, Android SDK 37 y JDK 21 para ejecutar Gradle. El código del módulo usa compatibilidad con Java 17. Para ejecutar la app, usa un emulador o un dispositivo con Android 8.0 (API 26) o superior.

1. Abre la carpeta raíz de PayRoutine en Android Studio.
2. Instala Android SDK 37 desde **SDK Manager** si aún no lo tienes y sincroniza el proyecto con Gradle.
3. Selecciona la configuración `app`, elige un emulador o dispositivo y pulsa **Run**.

En Windows, puedes compilar la app y ejecutar las pruebas unitarias desde la raíz del proyecto:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
```

## Estructura del código

La aplicación usa la arquitectura MVVM. El código Kotlin se encuentra en `app/src/main/java/cl/payroutine`.

| Ruta | Contenido |
| --- | --- |
| `ui/pantallas` | Pantallas de la aplicación hechas con Jetpack Compose y Material 3. |
| `ui/navegacion` | Rutas y navegación entre pantallas. |
| `ui/theme` | Colores, tema y tipografía. |
| `viewmodel` | ViewModels que exponen los datos a la interfaz. |
| `datos/local` | Base Room, entidades y DAO con las operaciones CRUD. |
| `datos/repositorio` | Repositorio que da acceso a los datos locales desde los ViewModels. |

El esquema de Room se encuentra en [`app/schemas`](app/schemas).
El monograma y los iconos de la interfaz están en [`app/src/main/res/drawable-nodpi`](app/src/main/res/drawable-nodpi).
