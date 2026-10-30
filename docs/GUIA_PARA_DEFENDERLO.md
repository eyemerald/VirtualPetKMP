# Guía del proyecto para defenderlo

> Documento **personal**: no es para los testers ni para el README. Explica cómo está hecho
> VirtualPetKMP, **por qué** cada pieza está así, qué alternativas se descartaron y qué responder
> cuando te pregunten lo incómodo. Los datos son los del repositorio en la entrega **1.12**
> (tag `entrega-1.12`): si algo no cuadra con el código, manda el código.

---

## Índice

1. [La respuesta de 30 segundos](#1-la-respuesta-de-30-segundos)
2. [El problema y las tres decisiones que definen el proyecto](#2-el-problema-y-las-tres-decisiones-que-definen-el-proyecto)
3. [Recorrido del código: de `main` a SQLite](#3-recorrido-del-código-de-main-a-sqlite)
4. [Cómo está hecho, paso a paso (orden real de construcción)](#4-cómo-está-hecho-paso-a-paso-orden-real-de-construcción)
5. [Por qué Kotlin Multiplatform y no otra cosa](#5-por-qué-kotlin-multiplatform-y-no-otra-cosa)
6. [Por qué Android y escritorio, y no iOS](#6-por-qué-android-y-escritorio-y-no-ios)
7. [Las versiones, una por una: por qué esas y no otras](#7-las-versiones-una-por-una-por-qué-esas-y-no-otras)
8. [Arquitectura por capas y las reglas que la sostienen](#8-arquitectura-por-capas-y-las-reglas-que-la-sostienen)
9. [La base de datos: SQLDelight, 9 tablas y 14 migraciones](#9-la-base-de-datos-sqldelight-9-tablas-y-14-migraciones)
10. [Identidad universal: `uuid` y `actualizadoEn`](#10-identidad-universal-uuid-y-actualizadoen)
11. [El traspaso de datos: el formato `.vpet`](#11-el-traspaso-de-datos-el-formato-vpet)
12. [Los avisos: alarmas exactas, no "notificaciones"](#12-los-avisos-alarmas-exactas-no-notificaciones)
13. [La interfaz: decisiones que se notan al usarla](#13-la-interfaz-decisiones-que-se-notan-al-usarla)
14. [La app de escritorio y su instalador](#14-la-app-de-escritorio-y-su-instalador)
15. [Firma y distribución: por qué el keystore es sagrado](#15-firma-y-distribución-por-qué-el-keystore-es-sagrado)
16. [Las 280 pruebas: qué se prueba, qué no y por qué](#16-las-280-pruebas-qué-se-prueba-qué-no-y-por-qué)
17. [El flujo de entrega, comando a comando](#17-el-flujo-de-entrega-comando-a-comando)
18. [Preguntas difíciles y su respuesta](#18-preguntas-difíciles-y-su-respuesta)
19. [Debilidades conocidas (dilas tú antes de que las digan)](#19-debilidades-conocidas-dilas-tú-antes-de-que-las-digan)
20. [Qué haría con dos semanas más](#20-qué-haría-con-dos-semanas-más)
21. [Mapa: "si te preguntan por X, está en Y"](#21-mapa-si-te-preguntan-por-x-está-en-y)
22. [Chuleta de comandos](#22-chuleta-de-comandos)
23. [Glosario](#23-glosario)
24. [Apéndice: cómo he comprobado cada cifra](#24-apéndice-cómo-he-comprobado-cada-cifra)

---

## 1. La respuesta de 30 segundos

> "Es un diario de salud para mascotas que funciona **sin servidores y sin cuenta**: los datos son
> de quien los tiene en el móvil. Lo escribí **una sola vez en Kotlin Multiplatform** y sale para
> **Android y Windows**, con la interfaz en Compose. La base de datos es SQLite con migraciones
> versionadas y **280 pruebas automáticas**; el traspaso entre dispositivos es un archivo `.vpet`
> (un ZIP con JSON y los adjuntos) que se puede restaurar, fusionar o deshacer, y **nunca borra
> nada sin haber hecho antes una copia**. Son **26.320 líneas de Kotlin en 167 ficheros**, y el
> **83,0 % del código de producción es común** a las dos plataformas (16.042 de 19.322 líneas)."

> Las cifras se miden, no se recuerdan: el apéndice del final dice con qué comando sale cada una.

Si te dejan 3 minutos más, añade:

> "Lo que mejor enseña el proyecto no es la app: es que **cada decisión está tomada por un fallo
> real**. Las alarmas exactas existen porque un tester no recibió el aviso del tratamiento; el
> `uuid` existe porque las fusiones duplicaban mascotas; el `java.sql` en el instalador existe
> porque la app empaquetada reventaba al abrirse. No hay nada 'porque se suele hacer así'."

### Guion de demo de 5 minutos (si te dejan una pantalla)

En este orden, porque cada paso "vende" algo distinto y no hace falta preparar nada:

1. **Lista con 6 animales** → escribe `lo` en el buscador: filtra a *Nilo* y *Lola*, sin acentos ni
   mayúsculas. Borra con la X.
2. **Botón de vista** → listado compacto: "6 fichas sin desplazar; con 40 esto es la diferencia".
   Cierra la app y vuelve a abrirla: **sigue en listado**.
3. **Ficha de una mascota** → peso con gráfico y peso ideal, vacunas con su estado en color,
   tratamientos con dosis. Todo con **un solo aviso por tarjeta**.
4. **Menú ⋮ de la ficha → Exportar la ficha a PDF** → compártelo al correo. "El PDF se genera en
   código común y tiene 36 pruebas que comprueban su contenido".
5. **Menú de la barra superior → Copia de seguridad** → *Compartir*: "se manda por WhatsApp, Drive o
   Quick Share sin cables; no hay servidor".
6. **Importar y fusionar** ese mismo paquete → sale la vista previa y, si choca un nombre,
   **pregunta**. Termina con **Deshacer** y enseña que todo vuelve a estar igual.
7. **La app de escritorio** (acceso directo del escritorio) con los mismos datos en
   `%USERPROFILE%\.virtualpet`: "misma UI, mismo código; y el instalable lleva su propio Java".

Si solo hay 60 segundos: pasos 1, 2 y 7.

---

## 2. El problema y las tres decisiones que definen el proyecto

El problema real: alguien con varios animales (o una protectora) lleva vacunas, desparasitaciones,
tratamientos con dosis, peso, visitas, informes en PDF y fotos **en el móvil**, y quiere además
poder mirarlo y editarlo en el ordenador sin meter sus datos en el servidor de nadie.

De ahí salen las tres decisiones que explican el resto del proyecto:

| Decisión | Qué implica en el código |
| --- | --- |
| **Offline-first, sin backend** | Una base SQLite local y nada más. No hay login, no hay API, no hay sincronización. Todo lo que "parece de red" (compartir, Drive) se hace con el compartir del sistema. |
| **Un solo código para dos plataformas** | `shared/` contiene UI + lógica + base de datos; `androidApp/` y `desktopApp/` son envoltorios de ~40 líneas cada uno más su configuración. |
| **Los datos del usuario no se tocan sin red de seguridad** | Antes de cualquier importación: copia automática. Todo en una transacción. Botón **Deshacer**. Migraciones aditivas y probadas desde todas las versiones anteriores. |

Si un entrevistador te pregunta "¿por qué no hay nube?", la respuesta buena no es "no me dio
tiempo": es **"datos de salud de un animal doméstico, en un dispositivo, sin cuentas: menos coste,
menos riesgo legal y funciona en el campo sin cobertura"**. Y el traspaso —que es lo único que
pedía red— se resuelve con un fichero que el usuario manda por donde quiera.

---

## 3. Recorrido del código: de `main` a SQLite

Esta es la traza que puedes seguir en vivo, en orden, si te piden "enséñame cómo funciona".

```
Android                                     Escritorio
androidApp/.../MainActivity.kt              desktopApp/.../main.kt
   · enableEdgeToEdge()                        · Window(icon, 430x900 dp)
   · notificación → SolicitudNavegacion        · DatabaseFactory()
   · App(DatabaseFactory(context))             · App(DatabaseFactory())
                          │
                          ▼
             shared/.../App.kt   ← LA RAÍZ (309 líneas, no hay más)
                · localizacionDelSistema()  → una sola vez
                · remember { databaseFactory.createDatabase() }
                · 9 repositorios + 3 ViewModels creados aquí (DI a mano)
                · LaunchedEffect → reprogramarAvisosDeTodo()
                · estado de navegación con rememberSaveable y un when
                          │
        ┌─────────────────┼───────────────────────────────┐
        ▼                 ▼                               ▼
  MascotaListScreen   FichaMascotaScreen            MainScreen (pestañas,
  + buscador/vistas   + hojas modales               barra, menú del sistema,
        │                 │                         diálogos de copia)
        └─────────────────┴───────────┬───────────────┘
                                      ▼
                              ViewModels (StateFlow)
                     MascotaViewModel · FichaMascotaViewModel
                     VeterinarioViewModel
                                      ▼
                          9 Repositorios (data/)
        MascotaRepository · VacunaRepository · PreventivoRepository
        RevisionRepository · TratamientoRepository · PesoRepository
        InformeRepository · NotaRepository · VeterinarioRepository
                                      ▼
                     SQLDelight: VirtualPetDatabase (generado de los .sq)
                                      ▼
        AndroidSqliteDriver("virtualpet.db")   JdbcSqliteDriver("~/.virtualpet/virtualpet.db")
```

**Las tres reglas que hacen que esto no se desmorone:**

1. **La UI no habla con la base de datos.** Ni un `SELECT` en un Composable: siempre
   `ViewModel → Repository`.
2. **El estado vive en el ViewModel, no en la pantalla.** `StateFlow<...>` + `collectAsState()`.
   Así las hojas de la ficha comparten el mismo resumen sin recalcularlo cinco veces.
3. **La navegación no es una librería.** Son tres pantallas de verdad (`list`, `ficha`, `form`
   para mascotas; `lista`/`form` para veterinarios) y todo lo demás son **hojas modales dentro de
   la ficha**. Meter Compose Navigation para esto sería una dependencia más para escribir lo
   mismo con `when`.

---

## 4. Cómo está hecho, paso a paso (orden real de construcción)

Si tuvieras que rehacerlo (o explicarlo como si lo hicieras), este es el orden que hace que cada
paso se pueda probar antes de seguir:

1. **Esquema primero.** Los 9 ficheros `.sq` (`mascotas`, `vacunas`, `preventivos`, `revisiones`,
   `tratamientos`, `pesos`, `informes`, `notas`, `veterinarios`); las **7 que cuelgan de `mascotas`
   con `FOREIGN KEY ... ON DELETE CASCADE`**. Sin tablas no hay nada que mostrar.
2. **Migraciones.** `1.sqm` … `14.sqm`. Aquí conviene ser exacto porque es una lección, no un
   mérito: **el primer `.sqm` no llegó el día 1**. El repositorio empieza el **2026-09-04** y
   `1.sqm` se añade el **2026-09-16** (commit `5aa7c2a`), **12 días después**. Durante esos días la
   base de datos no tenía marcador de versión (`user_version`), y de ahí viene una limitación que
   sigue en el código: un fichero con `user_version = 0` y tablas ya creadas entra por la rama
   "base nueva" y falla al crear. Lo correcto habría sido **el marcador y el primer `.sqm` en el
   mismo commit que el esquema**.
3. **Repositorios** (9): uno por tabla, con las consultas que la app necesita de verdad, más
   `actualizadoEn` en cada escritura (esto se convierte en obligatorio después, con el traspaso).
4. **ViewModels** (3): el estado de la pantalla y las reglas de presentación (resumen de la ficha,
   peso ideal, formateo).
5. **UI en Compose**: primero la lista y la ficha, después las hojas de salud. Material 3 con un
   tema propio y colores con significado.
6. **Notificaciones de vacunas/preventivos** y, más tarde, **tratamientos con Aceptar/Posponer**:
   `AlarmManager` + `BroadcastReceiver` + canal propio.
7. **PDF** con `compose-pdf` (código común, se puede recorrer en un test sin pintarlo) y compartir
   con `FileProvider`.
8. **Copia de seguridad** (`.vpet`), luego **restaurar**, luego **fusionar**, luego **exportar una
   mascota** y **deshacer**: en ese orden, porque cada uno se apoya en el anterior.
9. **`uuid` + `actualizadoEn` en las nueve tablas** (esquema 14 → 15): sin identidad estable no hay
   fusión posible.
10. **Escritorio**: añadir el target `jvm()`, `main.kt` y `jpackage`; descubrir allí los dos fallos
    que solo aparecen empaquetado (`java.sql`, icono).
11. **Instalador de Windows** propio (`packaging/windows/`), porque WiX no está instalado.
12. **Buscador y dos vistas de la lista** (1.12): lo último, porque se pidió al ver que con 40
    fichas las tarjetas grandes no se encuentran.

---

## 5. Por qué Kotlin Multiplatform y no otra cosa

El requisito de verdad era: **la misma interfaz y la misma lógica en el móvil y en el ordenador, y
la base de datos en ambos**. Con eso, las opciones eran:

| Opción | Por qué se descartó |
| --- | --- |
| **Nativo Android + nativo Windows** (Kotlin + C#) | Dos bases de código, dos modelos de datos, dos migraciones y dos juegos de tests. Es el doble de trabajo y la mitad de coherencia. |
| **Flutter** | Comparte interfaz sí, pero la lógica y la base de datos quedan al otro lado del puente (Dart ↔ nativo). Las migraciones SQL manuales no se pueden verificar como aquí (`MigracionesTest`), y el escritorio es el patrón menos cuidado de Flutter. |
| **React Native** | Escritorio es un ciudadano de segunda; puente JS; acceso a ficheros y notificaciones siempre vía módulo nativo. Para una app que hace **mucho** con ficheros y alarmas, el peor encaje. |
| **Tauri / Electron** | La app de escritorio sería web; la de móvil, otra cosa. Y las notificaciones exactas de Android seguirían siendo nativas. |
| **Kotlin Multiplatform + Compose Multiplatform** | Un lenguaje, un modelo de datos, un esquema SQL y **una sola UI**: las mismas pantallas se pintan en Android y en escritorio. `expect`/`actual` queda reducido a lo que de verdad es distinto (ficheros, notificaciones, selector de archivos). |

Lo que se demuestra con datos: `shared/` tiene **22 ficheros de UI** (20 pantallas/componentes + 2 del
tema) **y 33 de utilidades compartidas** (más 6 del paquete de copia), y los dos envoltorios son **un
fichero Kotlin cada uno** (`MainActivity.kt` y `main.kt`) más su configuración. El producto está
escrito prácticamente una sola vez (16.042 de las 19.322 líneas de producción son comunes: **83,0 %**).
Ahí está la respuesta a "¿esto cuánto trabajo tiene?": escribir una pantalla cuesta una pantalla, no
dos.

---

## 6. Por qué Android y escritorio, y no iOS

Es una decisión de **alcance**, no técnica, y conviene decirla así:

- iOS no se puede compilar ni probar aquí (hace falta un Mac y un dispositivo): **[NO VERIFICADO]**. Entregar iOS sin
  haberlo ejecutado nunca sería mentir.
- **Lo que está comprobado:** en `shared/build.gradle.kts` solo hay dos targets, `androidLibrary`
  y `jvm()` — **no existe `iosMain`**. Lo específico de plataforma son **44 `expect` en 15
  ficheros** (`Archivos.kt` 18, `AccionesSistema.kt` 6, `SelectorArchivo.kt` 4, y el resto repartido):
  ficheros, selector, compartir, notificaciones, versión de la app.
- **Lo que NO está listo, y antes se decía en esta guía:** `commonMain` usa
  `java.io.File` (`util/ImagenMascota.kt:4`), así que ese fichero no compila para iOS tal cual; y la
  librería del PDF, `io.github.rikoappdev:compose-pdf:0.9.0`, **solo publica variantes `-android` y
  `-jvm`** (no hay artefactos iOS). Añadir el target iOS hoy **no compila**: hay que sacar ese
  `java.io.File` de común, elegir otra librería de PDF (o aislarla) y escribir los `actual` de iOS,
  incluida la generación del PDF.
- Y falta además lo que no se puede probar sin dispositivo: notificaciones locales equivalentes a
  `AlarmManager`, selector de documentos (`UIDocumentPicker`) y un `.ipa` firmado. Sigue siendo
  trabajo de días, no de meses, pero **no es "poner un target"**.

Frase corregida para usar: **"la plataforma que falta es un target y tres piezas de plataforma;
el 83 % del código ya es común, pero hay dos dependencias que hoy lo impiden"**.

---

## 7. Las versiones, una por una: por qué esas y no otras

Todo está centralizado en `gradle/libs.versions.toml` (version catalog). **Una versión, un sitio**:
los tres módulos leen de ahí. Eso es en sí mismo una decisión defendible: no hay números sueltos en
los `build.gradle.kts`, salvo dos excepciones documentadas abajo.

### Toolchain

| Pieza | Versión | Por qué esa | Qué pasa si la cambias |
| --- | --- | --- | --- |
| **Gradle** | 9.2.1 (`gradle-wrapper.properties`) | Es la que exige AGP 9. Va fijada en el wrapper, así que cualquiera que clone compila con la misma. | Subir antes que AGP rompe el build; bajar impide usar AGP 9. |
| **JDK** | 21 (Temurin 21.0.11) | Es la que corre Gradle, jpackage y los tests. | Con 17 compila igual; el runtime que se empaqueta en Windows también es 21. |
| **AGP** | 9.0.1 | Necesaria por `com.android.kotlin.multiplatform.library` (el plugin de librería KMP nuevo) y por compilar contra API 37. | Con AGP 8.x desaparece `androidLibrary { }` dentro de `kotlin { }` y hay que volver al viejo `androidTarget()`. |
| **Kotlin** | 2.2.10 | Trae el compilador de Compose integrado (`org.jetbrains.kotlin.plugin.compose` con la misma versión). Ya no hay que casar dos versiones. | Kotlin mayor → hay que subir CMP a la par. |
| **compileSdk** | 37 | Es el SDK instalado y el que pide la librería de PDF/Compose nueva. | El proyecto **rechaza** versiones menores distintas de cero a propósito: el DSL de librería KMP no expone `compileSdkMinor`, y los dos módulos compilarían contra plataformas distintas. |
| **minSdk** | 26 (Android 8) | **Canales de notificación** (`NotificationChannel`) son API 26: por debajo habría que meter `if (SDK_INT >= 26)` en todo el código de avisos. Cubre la práctica totalidad de móviles en uso. | Bajar a 24 obliga a ramas por versión en notificaciones; subir deja fuera móviles viejos sin ganar nada. |
| **targetSdk** | 34 | Comportamiento estable. Aquí **no hay Play Store** que exija 35, y cada subida de target cambia reglas (Android 15 fuerza edge-to-edge, Android 14 restringe receptores, etc.). | Subir a 35 es trabajo real (revisar insets y receptores), no un cambio de número. |

### Librerías

| Librería | Versión | Por qué esa | Nota para defenderla |
| --- | --- | --- | --- |
| **Compose Multiplatform** | 1.9.3 | Es el par estable con el que está construido: CMP 1.9.3 + Material 3 1.9.0 + Kotlin 2.2.10. Subir uno obliga a revisar los otros dos y el generador de recursos. | Es lo que permite que la UI de Android y la de escritorio sean **el mismo código**. |
| **Material 3** | 1.9.0 | Artefacto `org.jetbrains.compose.material3`, que se versiona aparte de CMP. | M3 no es capricho: `Scaffold`, `NavigationBar`, `AlertDialog` y el sistema de color ya vienen con accesibilidad y tema oscuro. |
| **material-icons-extended** | 1.7.3 | **Fijado a mano a propósito.** JetBrains congeló ese artefacto en la rama 1.7 (el atajo `compose.materialIconsExtended` ya ni se expone y el proyecto está *deprecado*), así que su número no acompaña al resto de Compose. | Es la única entrada del catálogo con una versión "vieja": no es un descuido, es la última que se publicó. Si algún día se quita, hay que sustituir los iconos por `Icons.Default.*` de core o por vectoriales propios. |
| **SQLDelight** | 2.3.2 | Genera clases Kotlin **tipadas** desde el SQL y sus migraciones son ficheros `.sqm` que se pueden leer y probar. | Ver §9: es la razón de no usar Room. **Escritorio y Android usan ya la misma versión** (antes el driver JDBC estaba fijado a 2.0.0 en el `build.gradle.kts`). |
| **kotlinx-coroutines** | 1.8.0 | La que arrastra CMP 1.9 de forma transitiva. | Subirla sola no aporta y puede chocar con lo que espera CMP. |
| **kotlinx-datetime** | `0.7.1-0.6.x-compat` | La 0.7 movió `Instant` a `kotlin.time`. El artefacto **de compatibilidad** mantiene `kotlinx.datetime.Instant` y `toInstant()`, que es lo que usa el código. | Reescribir 30 ficheros antes de una entrega no compensa; cuando toque, se migra de una vez y se quita el sufijo. |
| **kotlinx-serialization-json** | 1.9.0 | El `.vpet` guarda `datos.json` y `manifiesto.json`. | Escribir y parsear JSON a mano es exactamente lo que no puede fallar cuando están en juego los datos de alguien. El lector es **tolerante** (`ignoreUnknownKeys`): un campo nuevo de una versión futura no rompe la lectura. |
| **compose-pdf** (rikoappdev) | 0.9.0 | Genera PDF **desde código común**, así que el PDF es el mismo en Android y en escritorio, y se puede **recorrer en un test** (`previewPages`) para comprobar que el texto está y en qué orden. | Hay 36 pruebas de PDF (`ContenidoPdfTest` 28 + `ExportadorPdfTest` 8): el PDF no se "mira", se verifica. **Ojo:** solo publica variantes `-android` y `-jvm`, así que hoy bloquea el target iOS. |

### Lo que antes eran "dos excepciones" ya está arreglado (revisión 1.13)

1. **El driver de escritorio estaba fijado a mano a `sqlite-driver:2.0.0`** mientras el catálogo
   decía 2.3.2 (así desde el primer commit). Ahora usa `libs.sqldelight.sqlite.driver`, o sea la
   2.3.2, y eso sube también el motor SQLite de escritorio de **3.42.0.0 a 3.51.3.0**. Los 267
   tests pasan; el ejecutable empaquetado se arrancó a mano contra una copia de la base real y
   abrió bien. **Lo que queda por probar es el `.exe` instalado en Windows** (ver lista de pruebas
   manuales al final).
2. **Las 9 entradas muertas del catálogo** (`pdfkmp`, `androidx-appcompat`, `androidx-espresso-core`,
   `androidx-testExt-junit`, `kotlin-testJunit`, `junit`, `sqldelight-native-driver`, y las dos de
   lifecycle) y las 5 versiones que solo usaban ellas: **borradas**. El build no cambia porque
   ningún `build.gradle.kts` las referenciaba.

---

## 8. Arquitectura por capas y las reglas que la sostienen

```
UI (Compose)          ui/            20 ficheros + theme/
   ↓  estados y eventos
ViewModel             viewmodel/      3 ficheros, StateFlow
   ↓  llamadas suspend
Repositorio           data/           9 ficheros, uno por tabla
   ↓  consultas generadas
SQLDelight            db/             VirtualPetDatabase (generado)
   ↓
SQLite                virtualpet.db
```

**Cómo se inyecta todo:** no hay framework de DI. En `App.kt` se crean todas las dependencias con
`remember`: **14 piezas** — 9 repositorios + 3 ViewModels + el programador de notificaciones + la
base de datos — y caben en 20 líneas. (En esta guía ponía "12" y luego "13": el criterio correcto es
contar la base de datos y el programador, que también se construyen ahí; el comando del apéndice lo
saca de `App.kt`.) Hilt o Koin aquí añadirían un plugin de compilación, anotaciones y un tiempo de
build mayor para resolver algo que `remember` ya resuelve. Si mañana hay 40 dependencias, se cambia;
hoy sería ceremonia.

**Lo que sí está separado de verdad y por qué:**

- **`util/` (33 ficheros comunes + 6 del paquete de copia)**: todo lo que tiene lógica propia y se
  puede probar sin pantalla: `EscalaPeso`, `SeriesPeso`, `FiltrosPeso`, `TraduccionesDatos`,
  `Localizacion`, `Posposiciones`, `PreferenciaVista`, `RedimensionFoto`, etc. **Regla del
  proyecto: si tiene un `if` que puede fallar, va aquí y lleva test.**
- **`expect`/`actual` (lo único distinto por plataforma)**: leer/escribir bytes, listar carpeta,
  SHA-256, ZIP, `DatabaseFactory`, selector de archivo, compartir, programador de notificaciones,
  reducción de fotos, versión de la app. Son **44 declaraciones `expect` en 15 ficheros**, con sus
  `actual` repartidos en **21 ficheros de `androidMain` y 19 de `jvmMain`**.
  **Nada de lógica de negocio ahí.**
- **`Localizacion` y `TraduccionesDatos`**: se decide **una sola vez** en `App.kt` y se publica con
  un `CompositionLocal`, para que no haya dos pantallas que opinen distinto sobre kilos/libras o
  sobre el orden de la fecha.

---

## 9. La base de datos: SQLDelight, 9 tablas y 14 migraciones

**Por qué SQLDelight y no Room.** Room es de Android (y su soporte KMP es reciente y atado a
AndroidX). SQLDelight es multiplataforma de verdad, y —lo que más importa aquí— **las migraciones
son ficheros SQL que se leen y se prueban**:

- Cada tabla tiene su `.sq` con las consultas nombradas y tipadas. Si un `SELECT` no cuadra con el
  esquema, **no compila**: no hay error en tiempo de ejecución por un nombre de columna.
- Las migraciones son `migrations/N.sqm`. La convención es: **`N.sqm` migra *desde* la versión
  N**. Con 14 ficheros, el esquema está en **versión 15**.
- `MigracionesTest` abre una base en la versión de partida, la migra y comprueba **columnas e
  índices** de las 9 tablas: no solo "no revienta", sino "el `uuid` está, el índice está y las filas
  siguen ahí".

**Los tres detalles que casi nadie tiene y que aquí sí:**

1. **`SqlSchemaDelegado` ya no existe** (revisión 1.13). Era un envoltorio vacío: reenviaba
   `version`, `create` y `migrate` sin añadir nada, y su KDoc decía que **sin él** las migraciones
   no se aplicarían. Se comprobó borrándolo y pasando `VirtualPetDatabase.Schema` directo: los 246
   tests de entonces (eran los de la 1.12) seguían en verde. Lo que de verdad aplica las migraciones es
   `abrirBaseDeDatos()` llamando a `esquema.migrate(...)`, y en Android el
   `AndroidSqliteDriver.Callback`. **Si alguien encuentra `SqlSchemaDelegado` en un tutorial o en
   una versión antigua: no hacía nada.**
2. **`PRAGMA foreign_keys` es de cada conexión, no de la base de datos.** Esto fue el fallo grave:
   en escritorio, `JdbcSqliteDriver` abre **una conexión nueva por sentencia** (su
   `ThreadedConnectionManager` cierra y olvida la conexión cuando la sentencia no va en una
   transacción, verificado en el bytecode), así que el `PRAGMA` ejecutado una vez al abrir no
   llegaba a los borrados de `Dispatchers.IO`: el `ON DELETE CASCADE` de las **7 tablas hijas** no
   se aplicaba y cada mascota borrada dejaba huérfanas sus vacunas, pesos, informes… **Reproducido
   con 19 filas** sobre una copia de la base real y arreglado de dos formas:
   - Escritorio: `Properties(foreign_keys=true)` al `DriverManager`, así **todas** las conexiones
     nacen con las claves foráneas activas. Test con base **en fichero** que borra por el
     repositorio y comprueba 0 huérfanas en las 7 tablas (antes fallaba).
   - Android: `setForeignKeyConstraintsEnabled(true)` desde `onConfigure` del `Callback`, que es
     configuración del **pool** de conexiones (antes había un `PRAGMA` suelto que funcionaba solo
     por cómo ordena Android sus escrituras). Verificado en el emulador borrando una mascota con 19
     filas hijas desde la interfaz: 0 huérfanas.
   - Y como ya había ficheros con huérfanas, al abrir se hace **una limpieza única**: si
     `PRAGMA foreign_key_check` devuelve filas, se copia el `.db` a
     `virtualpet-antes-de-limpiar.db`, se borran en una transacción y se deja constancia en consola.
3. **En escritorio, `PRAGMA user_version` se marca a mano.** El driver de Android lo actualiza; el
   de JDBC **no**. Sin marcarlo, el fichero se quedaba en versión 0 y cada arranque intentaba crear
   las tablas otra vez. Por eso `abrirBaseDeDatos()` es una función aparte y **probable con un
   driver en memoria**. Ahora ese marcador **solo sube**: si el fichero es más nuevo que la app, no
   se toca nada y se lanza `BaseDeDatosMasNuevaException` (mensaje en español); y antes de migrar se
   copia el fichero a `virtualpet-antes-de-migrar-vN.db` y la migración va **en una transacción**
   (`migrate` + `user_version`), así que o entra entera o no cambia nada.

4. **El `id` de un alta se lee en la misma transacción que el `insert`.** `SELECT
   last_insert_rowid()` devuelve el último id insertado **en esa conexión**, y sqldelight sobre
   fichero abre una conexión por sentencia fuera de transacción: el alta devolvía **0**. Estaba en
   los 4 repositorios que devuelven id (mascota, vacuna, preventivo, veterinario) y ningún test lo
   veía porque usaban base en memoria (una sola conexión). Consecuencia real: el aviso de una vacuna
   se programaba con id 0 (`FichaMascotaViewModel.kt:416`). Ahora `insert` + `getLastInsertedId` van
   dentro de `db.transactionWithResult { }`, con `IdInsertadoTest` (base en fichero) e
   `ImportacionFicheroTest` (restaurar y fusionar dejan los hijos colgados de su mascota y 0
   huérfanas).

**Migraciones aditivas siempre.** Ninguna migración borra ni renombra datos del usuario: se añaden
columnas (con valor por defecto o relleno) y se crean índices con `IF NOT EXISTS`. La 13 rellenó
`uuid` con `lower(hex(randomblob(16)))` para las filas existentes; la 14 creó los 9 índices por
`uuid`.

---

## 10. Identidad universal: `uuid` y `actualizadoEn`

Este es el capítulo que más te van a preguntar porque suena a "detalle de base de datos" y en
realidad es el corazón del traspaso.

- **El `id` es un autonumérico local.** En el móvil, Lola es `id = 3`. En el ordenador, Lola también
  podría ser `id = 1` (o `3` siendo otra mascota). Si exportas con ids y restauras, **las vacunas
  apuntan a la mascota equivocada**. Es el fallo clásico y silencioso.
- **`uuid`** (32 caracteres hex, `util/Identificadores.kt`) es estable y único **entre
  dispositivos**. Es lo que viaja en el `.vpet` y por lo que se relacionan las filas
  (`mascotaUuid`).
- **`actualizadoEn`** (epoch millis) en las 9 tablas es la regla de la fusión: **gana el más
  reciente**, fila a fila. Sin esa columna habría que preguntar por cada vacuna.
- **Índice por `uuid`** en las 9 tablas (migración 14): las fusiones hacen búsqueda por `uuid` miles
  de veces y sin índice son un escaneo completo. Con 40 animales y años de historial se nota.

Frase: **"el `id` sirve para la pantalla; el `uuid` sirve para la verdad"**.

---

## 11. El traspaso de datos: el formato `.vpet`

**Qué es:** un ZIP con tres cosas.

```
manifiesto.json   → formato, versionFormato (1), versionApp, esquema, creadoEn, plataforma
datos.json        → las 9 tablas: mascotas, vacunas, preventivos, revisiones,
                     tratamientos, pesos, informes, notas, veterinarios
archivos/         → fotos e informes PDF, referenciados por su nombre dentro del ZIP
```

`manifiesto.json` lleva además el **SHA-256 de `datos.json`**: si el fichero se corrompe al
mandarlo por WhatsApp, se detecta antes de tocar nada.

**Por qué ZIP + JSON y no copiar el `.db`:**

| Copiar `virtualpet.db` | Paquete `.vpet` |
| --- | --- |
| Puede ir con WAL a medias → fichero inconsistente. | Se escribe leyendo por consultas: siempre coherente. |
| Formato del motor, no legible. | JSON: se abre con un editor de texto y se puede recuperar a mano. |
| No permite fusionar: es "todo o nada". | Permite fusionar por `uuid` y exportar **una sola** mascota. |
| Las rutas de adjuntos son del dispositivo. | Los adjuntos van dentro, con nombre, y se reubican al restaurar. |

**Las garantías que ofrece la importación** (esto es lo que hay que defender):

1. **Vista previa antes de tocar**: cuántas mascotas, cuántas filas, cuántos archivos, qué falta.
2. **Copia automática previa** con nombre `...-copia-automatica-<millis>.vpet`, y se conservan las
   **3 últimas**.
3. **Una sola transacción** para la base de datos; los archivos se extraen antes, y los que falten
   se cuentan en el resultado en vez de romper.
4. **Deshacer** desde el propio diálogo: restaura la copia previa.
5. **Decisiones de conflicto explícitas**, no silenciosas:
   - **Nombre de mascota repetido** → se pregunta: **renombrar** u **sobrescribir** (nunca se
     decide solo).
   - **Misma fila en los dos lados** → gana `actualizadoEn` más reciente.
   - **Sobrescribir** no tira el historial del que había: **une** las historias.
   - **Nunca borra**: la fusión no elimina nada que exista en el dispositivo.

**Los tres modos y cuándo usar cada uno** (esto lo preguntan): *Restaurar* = "este móvil pasa a ser
una copia exacta del paquete" (reemplaza); *Importar y fusionar* = "añade lo del paquete a lo que
tengo" (une); *Exportar una mascota* = "mándale a la veterinaria solo a Lola" (la agenda de
veterinarios es común y no viaja en ese formato).

---

## 12. Los avisos: alarmas exactas, no "notificaciones"

Cuando un tester dijo "no me avisó del tratamiento", el diagnóstico no fue "Android es raro": fue
**`window=+1h0m0s0ms`** en `dumpsys alarm`. Es decir, la alarma era **inexacta**: se programó con
`setAndAllowWhileIdle` sin el permiso de alarmas exactas, y Android la agrupó una hora más tarde.
Ese es el hallazgo que justifica medio capítulo.

- **Permiso `SCHEDULE_EXACT_ALARM`** (Android 12+): sin él, `canScheduleExactAlarms()` es `false` y
  se usa `setAndAllowWhileIdle`. La app **pide el permiso** desde el formulario del tratamiento y
  **avisa en el menú del sistema** si falta; al abrir el menú, **reprograma** para que las alarmas
  pasen a exactas sin reiniciar.
- **`POST_NOTIFICATIONS`** (Android 13+) se pide al activar el recordatorio.
- **Canal propio** (`IMPORTANCE_HIGH`) para tratamientos: se pueden silenciar sin silenciar las
  vacunas.
- **Aceptar / Posponer 30 min** en la notificación (dos `PendingIntent` a un
  `BroadcastReceiver`). Posponer **no** es "volver a programar mañana": se guarda en
  `posposiciones.txt` y sobrevive al reinicio y a que la app reprograme todo. Ese fichero existe
  porque Android **borra las alarmas al reiniciar** y el receptor de arranque reprogramaba la hora
  diaria (o sea, al día siguiente): el aviso pospuesto se perdía.
- **Reinicio del móvil**: `RECEIVE_BOOT_COMPLETED` + `ReinicioReceiver` reprograma todo. Es la misma
  función que usa el arranque de la app (`reprogramarAvisosDeTodo`), a propósito: antes había dos
  copias y una se olvidó de las dosis.
- **Tocar la notificación** abre **la ficha de esa mascota** (y su hoja de salud), no la pantalla
  principal. Se hace con un `contentIntent` que lleva `virtualpet://aviso/<id>` y una
  `SolicitudNavegacion` que `MainActivity` empuja a Compose en caliente (la activity es `singleTop`:
  si la app ya estaba abierta, llega por `onNewIntent` y se navega igual).

**Por qué no WorkManager:** no garantiza hora exacta (es su diseño). **Por qué no FCM:** exige
servidor y cuenta de Google; aquí no hay backend por decisión.

---

## 13. La interfaz: decisiones que se notan al usarla

- **Regla de la casa: ningún texto que vea el usuario está escrito en el código.** Todos salen de
  `composeResources/values*/strings.xml` (**359 cadenas por idioma**, 6 idiomas, con un test de
  paridad). Los mensajes de las **excepciones son para el registro y la depuración**, no para el
  usuario: cuando hay que avisar, se captura la excepción y se pinta una cadena de los recursos (así
  funciona el aviso de "los datos son de una versión más nueva"). Queda texto fijo pendiente de
  revisar en sitios concretos (diálogos del sistema del escritorio, errores de foto, nombres de los
  canales de notificación); está inventariado en la revisión 1.14 y **no se ha tocado todavía**.
- **La foto de la mascota se guarda ya reducida a 640 px de lado mayor** (`util/RedimensionFoto.kt`,
  JPEG calidad 82; PNG si la imagen tiene transparencia). El punto de partida es la pantalla: la foto
  se pinta como mucho a **84 dp**, así que a densidad 4,0 hacen falta 336 px, y se guardan 640 para
  tener margen (pantallas más densas, el PDF de la ficha —46 dp impresos— y futuros tamaños). Medido
  con dos fotos reales de 3072×4096: **3.469 KB → 60 KB** y **3.588 KB → 53 KB** (98,2 % y 98,5 %), y
  el `.vpet` con las dos pasa de **7,27 MB a 115 KB**. En Android se decodifica con `inSampleSize`
  (no se cargan los 12 MP enteros) y la **rotación EXIF se aplica a los píxeles antes de
  recomprimir**. Las fotos **ya guardadas no se tocan**, y las de los informes tampoco: ahí la
  calidad legible es el requisito.
- **Un solo menú de acciones (⋮) en toda la app.** Antes cada lista tenía su lápiz y su papelera.
  Con el menú, la misma acción se ve igual en todas partes, y **la papelera deja de estar a un
  toque por accidente** (borrar es lo que menos se hace).
- **El color no decora, informa**: rojo = vencido / por debajo del ideal; ámbar = vence pronto;
  verde = bien / por encima; azul = peso; lila = "a tener en cuenta". Y siempre con **texto al
  lado** ("1 vencida"), nunca solo color: eso es accesibilidad real, no una nota al pie.
- **La ficha es un panel**, no un formulario: cada tarjeta da **un solo aviso**.
- **Buscador sin acentos ni mayúsculas.** Compara con una tabla de sustituciones y no con
  `java.text.Normalizer`, que no se comporta igual en todas las versiones de Android. `"lo"`
  encuentra *Nilo* y *Lola*.
- **Dos vistas de la lista**: tarjetas (pocas mascotas) y listado compacto (40 animales). La vista
  elegida se guarda en `vista.txt` porque `rememberSaveable` **sobrevive a girar la pantalla pero no
  a cerrar la app**, y quien pone el listado no quiere volver a ponerlo.
- **Gráficos propios**: escala con margen **relativo** (10 %) para que un conejo de 1,6 kg no salga
  como una raya y un perro de 26 kg no se aplaste; granularidad según filtro (cada pesaje en rangos
  cortos, media mensual en un año); peso ideal como línea discontinua; se arrastra **solo** cuando
  la serie no cabe.
- **Multiidioma con paridad comprobada**: 6 ficheros `strings.xml` (es, en, pt, fr, de, it) con las
  **mismas claves y el mismo orden**; un test (`ParidadTraduccionesTest`) lo verifica, porque
  olvidar un texto en un idioma **no rompe la compilación** (cae al inglés en silencio).
- **Los datos se guardan en español y se traducen al mostrarlos** (`TraduccionesDatos`). Traducir lo
  guardado rompería las mascotas que ya existen (el formulario compara la raza con la lista de su
  especie).
- **Reglas de formato**: peso **siempre en kilos** en la base de datos (libras solo si la región es
  `en-US` y solo al mostrar); fecha mes/día solo en inglés; coma decimal solo en español y
  portugués. Se decide **una vez** (`localizacionDelSistema`) y se publica a toda la interfaz.

---

## 14. La app de escritorio y su instalador

- **Compose Desktop** con el **mismo** `App()`: la ventana es 430×900 dp (proporción de móvil) y
  dentro va la app compartida. Lo específico son dos ficheros.
- **`jpackage`** produce un ejecutable con **su propio Java**: el runtime que llevan los usuarios es
  un **JRE 21 recortado** con 10 módulos (`java.base`, `java.desktop`, `java.sql`,
  `jdk.unsupported`, …). Instalado ocupa ~175 MB; el ZIP comprimido, 109 MB.
- **Los dos fallos que solo aparecen empaquetado** (y que hay que contar porque enseñan mucho):
  1. `NoClassDefFoundError: java/sql/DriverManager`: `jpackage` solo mete `java.base`. Se arregla
     con `modules("java.sql", "jdk.unsupported")`. **Desde Gradle (`run`) nunca fallaba**, porque
     ahí se usa la JDK completa: por eso hay que probar **el instalable**, no el `run`.
  2. El icono de la ventana **no se hereda del `.exe`**: se pasa en `Window(icon = ...)`. Los
     recursos de Compose de `shared` son `internal`, así que el PNG se carga con `ImageIO` desde los
     recursos de `desktopApp`.
- **Por qué un instalador propio y no un `.msi`**: `jpackage` genera MSI **a través de WiX Toolset**,
  que no está instalado en este equipo. El ZIP lleva `INSTALAR.bat` + `instalar.ps1`, que copian a
  `%LOCALAPPDATA%\Programs\Virtual Pet`, crean accesos directos, registran la entrada en
  "Aplicaciones instaladas" (HKCU) y dejan un desinstalador. **Instalación por usuario: no pide
  administrador.** Los datos viven en `%USERPROFILE%\.virtualpet`, así que desinstalar **no los
  borra**.

---

## 15. Firma y distribución: por qué el keystore es sagrado

- La APK de release se firma con `firma/virtualpet-release.jks` + `keystore.properties`. **Los dos
  están en `.gitignore` y no se suben nunca.** Quien los tenga puede firmar una actualización que se
  instale **encima** de la app de los testers.
- Certificado: `CN=Virtual Pet`, SHA-256 `d2c421e2…9ab6cb`. **Antes de cada entrega**:
  `apksigner verify --print-certs dist/VirtualPet-X.apk` y comprobar que coincide.
- **Cada entrega sube `versionCode`** (hoy 13 / `versionName` 1.12). Con el mismo número Android
  instala igual, pero el móvil ya no distingue cuál tiene.
- **Aviso obligatorio al tester**: "no desinstales para actualizar". Un cambio de firma obliga a
  desinstalar y eso **borra los datos** — pasó de verdad, con la 1.3 firmada en debug: hubo que
  rescatar los datos con `run-as` y un `.vpet` construido a mano. Está documentado en el README.
- **`isMinifyEnabled = false`**: la razón escrita en el `build.gradle.kts` ("la app no tiene
  reflexión propia") **es incorrecta**. Sí hay reflexión: `Class.forName("android.app.ActivityThread")`
  en 4 ficheros de `androidMain` (`Archivos`, `CarpetaDatos`, `GuardarArchivo`, `VersionApp`) para
  obtener el `Application` sin pasar el `Context`. Es reflexión sobre una clase del framework (no se
  ofusca) y la serialización usa serializers explícitos, así que **R8 probablemente no rompería
  nada — pero no se ha probado**. Hoy está desactivado por eso: por no haberlo probado, no porque
  sea imposible.
- **Por qué no está en Play Store**: no hay cuenta, ni ficha, ni revisión de traducciones, ni
  política de datos. Técnicamente podría publicarse; la decisión de repartir a mano es coherencia
  con el resto del proyecto, no una limitación técnica.
- **Ojo con el argumento de la privacidad, porque tiene matices**: `allowBackup="true"`, así que
  **Android sí copia los datos** —base de datos, fotos e informes— a la copia de seguridad del
  usuario (Google Drive). Lo que se hace es **dejar fuera las copias automáticas internas**
  (`files/copias`) para no duplicar el tamaño, con `data_extraction_rules.xml` (Android 12+) y
  `backup_rules.xml` (Android 8–11). Verificado en el emulador con el transporte de backup local:
  20.009.984 B con 12,5 MB metidos en `files/copias` → **7.490.048 B** al aplicar las reglas (medido con el transporte **local** del emulador; **[NO VERIFICADO]** contra una cuenta de Google real). La
  frase honesta es "los datos son tuyos y se quedan en tu móvil salvo la copia que el propio
  Android haga, que puedes desactivar"; **"no pasan por nadie" no es cierto**.
- **La copia de Google es una red extra, no la protección principal.** Tiene un **tope de 25 MB por
  aplicación**: con 40 mascotas y sus informes (que no se reducen porque tienen que seguir legibles)
  la cuenta se queda cerca de ese límite (cifras en la sección de debilidades). La protección
  **fiable** es el `.vpet`: se guarda donde el usuario quiera, no tiene tope, lleva todo (fotos y PDF
  dentro) y se puede restaurar en cualquier dispositivo. Lo que da la copia del sistema es comodidad
  al cambiar de móvil.
- **Lo que la app NO pide** (defensa de privacidad, y se puede demostrar en el manifiesto): nada de
  almacenamiento —`WRITE_EXTERNAL_STORAGE` y `READ_EXTERNAL_STORAGE` están **eliminados
  explícitamente** con `tools:node="remove"` porque alguna librería los arrastraba—, nada de
  contactos, nada de ubicación, nada de `READ_PHONE_STATE`. Solo tres permisos:
  `POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM` y `RECEIVE_BOOT_COMPLETED`. Y **cero** conexiones de
  red: la app no tiene el permiso `INTERNET` ni una sola llamada de red.

---

## 16. Las 280 pruebas: qué se prueba, qué no y por qué

**Números reales:** 280 pruebas en 39 clases. Se ejecutan con `./gradlew :shared:jvmTest` (unas 2
minutos) sobre **SQLite en memoria creado desde el esquema**: no son mocks, es el motor de verdad.

| Bloque | Clases | Qué demuestra |
| --- | --- | --- |
| Migraciones | `MigracionesTest` (7) | Cada migración, desde cada versión anterior, deja las 9 tablas con sus columnas e índices y los datos intactos. |
| Apertura de la base | `AperturaBaseDatosTest` (4) | Base más nueva que la app → rechazada y **sin tocar**; migración que falla a mitad → el marcador no sube y la base se puede reabrir; copia `virtualpet-antes-de-migrar-vN.db`. |
| Claves foráneas | `CascadaEscritorioTest` (5) | Con base **en fichero** y borrado por el repositorio: 0 huérfanas en las 7 tablas hijas; claves foráneas activas en otro hilo; limpieza de huérfanas con copia previa. |
| Repositorios | 7 clases | Altas, bajas, `ON DELETE CASCADE`, cálculos de peso ideal, errores. |
| Cálculos de peso | `EscalaPesoTest` (11), `SeriesPesoTest` (7), `FiltrosPesoTest` (19), `GraficoPesoTest` (8) | La escala, la granularidad, los filtros y la geometría del gráfico. |
| Traspaso | `CopiaDatosTest` (9), `FusionTest` (10), `IdentificadorUniversalTest` (7) | Ida y vuelta sin pérdidas, fusión por `uuid`, conflictos, retención de copias. |
| Avisos | `RecordatoriosTratamientoTest` (18), `ReprogramadorNotificacionesTest` (9), `PosposicionesTest` (6) | Qué hora se programa, que la posposición gane, que sobreviva al reinicio simulado. |
| PDF | `ContenidoPdfTest` (28), `ExportadorPdfTest` (8) | Que el PDF **contiene** lo que debe y en el orden correcto. |
| Idioma y formato | `LocalizacionTest` (10), `TraduccionesDatosTest` (8), `ParidadTraduccionesTest` (8), `IdiomaEnTiempoDeEjecucionTest` (2) | Kilos/libras, orden de fecha, decimal, paridad de los 6 idiomas. |
| Fotos | `RedimensionFotoTest` (12) | Cálculo del tamaño (horizontal, vertical, cuadrada, ya pequeña, límite, tamaño 0) y la reducción real de un fichero, incluido PNG con transparencia. |
| Id de un alta | `IdInsertadoTest` (4) | Con base **en fichero**: los 4 repositorios que devuelven id devuelven el de la fila creada (antes, 0). |
| Importación en fichero | `ImportacionFicheroTest` (3) | Restaurar y fusionar con base en fichero dejan los hijos colgados de su mascota y 0 huérfanas; fusionar dos veces no duplica. |
| Fusión con descartes | `FusionFilasSinMascotaTest` (2) | Una fila sin mascota se descarta, se cuenta en `filasSinMascota` y no deja huérfanas. |
| Apertura y versión | `RevisarVersionBaseTest` (4) | La revisión que hace el escritorio antes de abrir la ventana: detecta base más nueva, devuelve las dos versiones y no toca el fichero. |
| UI con lógica | `BuscadorMascotasTest` (8), `FichaMascotaResumenTest`, `FichaMascotaErroresTest`, `ErroresAppTest`, `PreferenciaVistaTest` (5) | Lo que se puede probar sin pintar: filtrado, resumen de la ficha, errores visibles. |

**Filosofía del proyecto:** *"al arreglar un fallo, añade el test que lo habría cazado"*. Ejemplos
reales de tests que nacieron de un fallo: `PosposicionesTest` (aviso pospuesto perdido tras
reinicio), `MigracionesTest` (mascotas sin `uuid` tras migrar), `FichaMascotaErroresTest` (una
operación fallida parecía haber ido bien), `CascadaEscritorioTest` (19 huérfanas al borrar en
escritorio) y `AperturaBaseDatosTest` (el marcador de versión bajando).

**Qué NO se prueba y cómo se compensa:**
- **No hay tests instrumentados de Compose** (ni Espresso): requieren emulador y son lentos y
  frágiles. Lo que tiene lógica se saca a `util/` y se prueba ahí; lo puramente visual se ha
  verificado **a mano en dispositivo real** (capturas del README) y en emulador.
- **No hay tests de la app de escritorio empaquetada**: se prueba abriéndola (y así aparecieron
  `java.sql` y el icono).
- Los tests fijan `user.language=es` y `user.country=ES` para que no dependan del idioma del
  ordenador de quien los ejecute (si no, la misma prueba pasa o falla según la máquina).

---

## 17. El flujo de entrega, comando a comando

```powershell
# 1. Subir versión en androidApp/build.gradle.kts (versionCode +1, versionName)
#    y en desktopApp (packageVersion), packaging/windows/LEEME.txt e instalar.ps1

# 2. Pruebas
$env:GRADLE_USER_HOME="$PWD\.gradle-home"; .\gradlew.bat :shared:jvmTest --console=plain

# 3. APK de release
.\gradlew.bat :androidApp:assembleRelease --console=plain
Copy-Item androidApp\build\outputs\apk\release\androidApp-release.apk dist\VirtualPet-X.Y.apk

# 4. Comprobar la firma ANTES de mandarla
apksigner verify --print-certs dist\VirtualPet-X.Y.apk   # debe decir CN=Virtual Pet

# 5. Instalar encima en el móvil (sin desinstalar) y dejar la APK en Descargas
adb -s <serie> install -r dist\VirtualPet-X.Y.apk
adb -s <serie> push dist\VirtualPet-X.Y.apk /sdcard/Download/VirtualPet-X.Y.apk

# 6. Escritorio + instalador
.\gradlew.bat :desktopApp:createDistributable --console=plain
python packaging\windows\empaquetar.py     # deja dist/VirtualPet-X.Y-windows.zip

# 7. Etiquetar y subir
git add -A; git commit -m "..." ; git tag -a entrega-X.Y -m "..."
git -c http.sslBackend=openssl push origin main
git -c http.sslBackend=openssl push origin entrega-X.Y
```

**Checklist de entrega** (si te lo preguntan, esto es "mi proceso"):

- [ ] `BUILD SUCCESSFUL` en tests y en release.
- [ ] `versionCode` subido y `versionName` correcto en Ajustes del móvil.
- [ ] Firma comprobada con `apksigner` (`CN=Virtual Pet`).
- [ ] Instalada **encima** de la anterior en un dispositivo con datos (no un móvil limpio): es la
      única forma de validar la migración.
- [ ] APK en `dist/` + copia en el móvil + mensaje para testers actualizado con los pasos.
- [ ] ZIP de Windows regenerado y probado **desde el ZIP descomprimido**, no desde Gradle.
- [ ] README y `docs/` al día (versión, funciones nuevas, estado del proyecto).
- [ ] Commit + tag `entrega-X.Y` + push.

---

## 18. Preguntas difíciles y su respuesta

**"¿Esto es una app de verdad o un proyecto de clase?"**
> 26.320 líneas de Kotlin, 167 ficheros, 280 pruebas, dos plataformas, un instalador para Windows,
> migraciones de esquema desde la versión 1, y hay usuarios reales probándola con datos reales. La
> parte de "clase" sería el tamaño: es un producto pequeño hecho con cuidado.

**"¿Por qué no hay servidor? ¿Y si se rompe el móvil?"**
> Porque son datos de salud y el usuario no tiene por qué tener cuenta. La protección es una copia
> que él controla: la app hace copia automática antes de importar, se puede mandar por WhatsApp,
> correo o Drive con el compartir del sistema, y restaurar es un botón. Un servidor habría añadido
> coste mensual, RGPD y un punto único de fallo a cambio de comodidad.

**"¿Qué pasa si dos dispositivos editan la misma mascota?"**
> Se fusiona por `uuid`, fila a fila, y **gana la más reciente** (`actualizadoEn`). Si el **nombre**
> de una mascota choca, la app **pregunta**: renombrar o sobrescribir. Nunca decide sola y nunca
> borra.

**"¿Cómo sé que una actualización no me va a borrar los datos?"**
> Tres capas: las migraciones solo **añaden** (probadas desde cada versión anterior comparando
> columnas e índices); antes de cualquier importación se guarda una copia automática (se conservan 3)
> y hay botón Deshacer; y la importación va en **una transacción** con los adjuntos ya extraídos.

**"¿Por qué SQLDelight y no Room, que es lo estándar en Android?"**
> Porque esto no es solo Android. SQLDelight genera el mismo código para Android y para el JVM, y
> sus migraciones son ficheros SQL revisables y ejecutables en un test. Room me habría atado a
> AndroidX y el escritorio habría necesitado otra capa de datos distinta: justo el problema que KMP
> viene a resolver.

**"¿Por qué no usas Hilt/Koin?"**
> Porque son 14 piezas (9 repositorios, 3 ViewModels, la base de datos y el programador de avisos) y se cablean en 20 líneas con `remember`. Una librería de DI añadiría
> procesado de anotaciones y tiempo de build para resolver un problema que aquí no existe.

**"¿Y los avisos, por qué fallaban?"**
> Porque la alarma era **inexacta**: sin el permiso de alarmas exactas, Android la agrupa y puede
> llegar una hora tarde (lo vi en `dumpsys alarm`: `window=+1h0m0s0ms`). Ahora la app pide el
> permiso, avisa en el menú si falta, usa `setExactAndAllowWhileIdle` cuando lo tiene, y —clave—
> **se reprograma al abrir el menú**, así que conceder el permiso surte efecto sin reiniciar.

**"¿Por qué no usas FCM para las notificaciones?"**
> FCM exige servidor, cuenta de Google y que el aviso viaje por internet. Aquí la notificación nace
> en el propio móvil: no hay nada que pueda caerse fuera del dispositivo.

**"¿Por qué no está en la Play Store?"**
> Porque no hay cuenta de desarrollador, ni ficha, ni revisión de traducciones, ni política de
> datos. Técnicamente sí podría publicarse (targetSdk 34, firma propia). Repartir la APK a mano es
> coherente con el resto: sin intermediarios.

**"¿Se sostiene con 40 animales?"**
> Sí, y por eso existe la 1.12: índice por `uuid` en las 9 tablas, consultas siempre filtradas por
> mascota, y un listado compacto en el que caben 6 fichas por pantalla sin desplazar. Las tarjetas
> grandes son para 5 animales, no para 40.

**"¿Cuánto tardarías en hacerlo para iPhone?"**
> El 83,0 % del código de producción es común, pero hoy **no compila para iOS tal cual**: hay un
> `java.io.File` en un fichero de `commonMain` y la librería del PDF no publica artefactos iOS.
> Habría que sacar esos dos, escribir los `actual` de iOS (ficheros, notificaciones locales,
> selector de documentos, generación del PDF) y firmar un `.ipa` con un Mac. Días de trabajo, no un
> proyecto nuevo — pero no es "poner un target".

**"¿Qué es lo que más te costó?"**
> Admitir que dos fallos no eran de Android sino de mi código: el `ON DELETE CASCADE` que no
> borraba nada (el `PRAGMA` era de la conexión, no de la base; dejaba 19 filas huérfanas) y el aviso
> que llegaba tarde (alarma inexacta). Y el que más miedo dio: la pérdida de firma con la 1.3, que
> se resolvió rescatando los datos con `run-as` y construyendo un `.vpet` a mano.

**"¿Qué harías distinto?"**
> Haber metido `uuid` desde el primer esquema, en vez de añadirlo en la 14 cuando el traspaso lo
> pidió: habría ahorrado una migración con relleno. Y haber probado el instalador de Windows desde
> el principio, no cuando lo pidió el usuario.

---

## 19. Debilidades conocidas (dilas tú antes de que las digan)

Están en el README como "Pendiente", pero tenlas en la cabeza con su explicación:

| Debilidad | Cómo la defiendes |
| --- | --- |
| **Los borrados no se propagan entre dispositivos.** Borras una mascota en el móvil y sigue en el PC hasta que la borras allí. | Es una decisión, no un olvido: propagar borrados pide "lápidas" (`borradoEn`) y una regla de fusión nueva; hacerlo mal **borra datos de más**, que es el peor fallo posible. Prefiero que sobre una ficha un rato a perder historial. |
| **Traducciones de de/pt/fr/it sin revisar por un nativo.** | Están con aviso explícito en el README; la paridad de claves la garantiza un test. Lo que más varía son los nombres de raza. Contratar una revisión es de las primeras cosas antes de publicar. |
| **Las fotos que ya estaban guardadas NO se reducen.** La reducción es al guardar, así que las antiguas siguen con sus MB. Medido en el emulador: siguen a 3072×4096 y 3,55 / 3,67 MB. | Es a propósito: tocar ficheros que el usuario ya tiene sin que lo pida es justo lo que no se hace. Un proceso de un solo uso (con copia previa) bajaría cada una a ~20 KB; está propuesto, **no implementado**, a la espera de decidirlo. |
| **La copia de Google tiene un tope de 25 MB y el peso lo ponen los informes**, no las fotos. | Fotos a 640 px: 40 mascotas × 2 × ~60 KB ≈ **4,8 MB**. Base: del orden de **1 MB**. Informes (no se reducen, tienen que seguir legibles): 80 ficheros × ~250 KB ≈ **20 MB** → total ≈ **26 MB**, es decir, **por encima del tope**. Está decidido no excluir los informes de la nube, así que la copia del sistema puede quedarse corta con muchas mascotas; el `.vpet` es la copia que no falla. |
| **Sin tests de UI instrumentados.** | Los requiere un emulador estable y son frágiles; la lógica está extraída a `util/` y probada, y lo visual se ha verificado en dispositivo real. Con más tiempo, `Compose UI test` para los flujos críticos. |
| **Un solo desarrollador y un solo Android de pruebas** (POCO con HyperOS). | Las peculiaridades de HyperOS (batería, inicio automático) están documentadas en el mensaje para testers, y hay un emulador aparte. Más marcas = más confianza; por eso existen los testers. |
| **Un `.vpet` recibido no se abre desde WhatsApp con doble toque.** | Requiere un `intent-filter` para la extensión. Hoy: guardar y elegirlo en "Importar y fusionar" (3 toques). Está en la lista. |
| **Queda texto visible escrito en el código** en sitios concretos: los títulos de los diálogos del sistema del escritorio, los mensajes de error al elegir foto y los nombres/descripciones de los canales de notificación. | Está inventariado (revisión 1.14) y todavía sin arreglar; la regla es que todo texto de cara al usuario salga de `strings.xml`, y estos son los que faltan. No es un problema de funcionamiento: es de idioma. |
| **[NO VERIFICADO]** El ejecutable de escritorio con SQLite 3.51 solo se ha arrancado a mano, no instalado desde el ZIP como usuario final. | Los 280 tests y el arranque contra una **copia** de la base real cubren el riesgo funcional; queda la prueba manual del instalador (está en la lista de pruebas antes de fusionar). |

---

## 20. Qué haría con dos semanas más

En este orden (el orden también demuestra criterio):

1. **Propagar borrados con lápidas** (`borradoEn` + regla de fusión) y su test. Es lo único que hoy
   puede confundir de verdad a quien use dos dispositivos.
2. **Abrir `.vpet` desde el gestor de archivos / WhatsApp** (`intent-filter` + `ACTION_VIEW`).
3. **Revisión nativa de las traducciones** (alemán primero: peor caso de longitud de texto).
4. **Subir `targetSdk` a 35** con revisión de insets y receptores.
5. **Tests de UI de los flujos críticos** (crear mascota, importar, posponer un aviso).
6. **`.msi`** si se instala WiX (mejor integración con "Aplicaciones instaladas" y despliegue por
   GPO en una protectora).
7. **Cartilla veterinaria** (tabla nueva de páginas con fotos/PDF) y **dosis vencidas** en el
   resumen del cuidador: las dos están ya identificadas como mejoras de producto, no como deuda.

---

## 21. Mapa: "si te preguntan por X, está en Y"

| Pregunta | Fichero |
| --- | --- |
| Punto de entrada, DI y navegación | `shared/src/commonMain/kotlin/com/example/virtualpetkmp/App.kt` |
| Esquema y consultas | `shared/src/commonMain/sqldelight/com/example/virtualpetkmp/db/*.sq` |
| Migraciones | `shared/src/commonMain/sqldelight/migrations/1.sqm … 14.sqm` |
| Apertura/migración de la base | `shared/.../data/DatabaseFactory.kt` (+ `.android.kt`, `.jvm.kt`) |
| `uuid` y `actualizadoEn` | `shared/.../util/Identificadores.kt` |
| Paquete `.vpet` (modelo y formato) | `shared/.../util/copia/PaqueteDatos.kt` |
| Exportar / importar / fusionar | `util/copia/ExportadorCopia.kt`, `ImportadorCopia.kt`, `ImportadorFusion.kt` |
| Diálogos de copia y deshacer | `shared/.../ui/CopiaSeguridad.kt`, `ui/FusionDatos.kt` |
| Avisos (Android) | `shared/src/androidMain/.../NotificacionReceiver.kt`, `AccionRecordatorioReceiver.kt`, `ReinicioReceiver.kt`, `util/AlarmasAndroid.kt` |
| Posposiciones | `shared/.../util/Posposiciones.kt` + `util/RecordatoriosTratamiento.kt` |
| PDF | `shared/.../util/ExportadorPdf.kt` + `util/ContenidoPdf.kt` |
| Buscador y vistas | `shared/.../ui/MascotaListScreen.kt`, `util/PreferenciaVista.kt` |
| Aviso de base más nueva | `ui/BaseMasNueva.kt` (Android), `data/RevisarVersionBase.kt` (escritorio) |
| Reducción de fotos | `util/RedimensionFoto.kt` (+ `.android.kt`, `.jvm.kt`) |
| Idioma y unidades | `shared/.../util/Localizacion.kt`, `TraduccionesDatos.kt`, `composeResources/values*/strings.xml` |
| Instalador de Windows | `packaging/windows/instalar.ps1`, `empaquetar.py`, `LEEME.txt` |
| Firma | `keystore.properties` (local) + `androidApp/build.gradle.kts` |
| Decisiones técnicas por escrito | `README.md`, `docs/ARQUITECTURA.md`, `docs/TRASPASO_DE_DATOS.md` |

---

## 22. Chuleta de comandos

```powershell
$env:GRADLE_USER_HOME="$PWD\.gradle-home"      # caché de Gradle dentro del proyecto

.\gradlew.bat :shared:jvmTest --console=plain           # 280 pruebas (~2 min)
.\gradlew.bat :androidApp:assembleDebug --console=plain  # APK de pruebas
.\gradlew.bat :androidApp:assembleRelease --console=plain# APK firmada de release
.\gradlew.bat :desktopApp:run --console=plain            # app de escritorio
.\gradlew.bat :desktopApp:hotRun --auto                  # escritorio con recarga en caliente
.\gradlew.bat :desktopApp:createDistributable            # .exe para empaquetar
python packaging\windows\empaquetar.py                   # ZIP del instalador

# Firma
apksigner verify --print-certs dist\VirtualPet-1.12.apk

# Dispositivo
adb devices
adb -s <serie> install -r dist\VirtualPet-1.12.apk
adb -s <serie> push dist\VirtualPet-1.12.apk /sdcard/Download/
adb -s <serie> shell "dumpsys package com.example.virtualpetkmp | grep versionName"
adb -s <serie> shell "dumpsys alarm | grep -i exact"      # comprobar alarmas exactas
adb -s <serie> shell "screencap -p /sdcard/p.png"         # captura de pantalla

# Git
git status --short
git tag -a entrega-1.13 -m "..."
git -c http.sslBackend=openssl push origin main
```

**Datos de la app en cada plataforma:**
- Android: `/data/data/com.example.virtualpetkmp/` (`virtualpet.db`, fotos, informes, copias).
- Windows: `%USERPROFILE%\.virtualpet\` (la misma carpeta tanto desde Gradle como instalado).

---

## 23. Glosario

| Término | Qué es, en una línea |
| --- | --- |
| **KMP** (Kotlin Multiplatform) | Compilar el mismo código Kotlin para varias plataformas; aquí Android y JVM. |
| **Compose Multiplatform** | La UI de Jetpack Compose llevada a escritorio y más: `commonMain/ui` se pinta igual en las dos. |
| **`expect` / `actual`** | Declarar en común algo que cada plataforma implementa (ficheros, alarmas, compartir). |
| **SQLDelight** | Genera clases Kotlin tipadas a partir de ficheros `.sq`, y aplica migraciones `.sqm`. |
| **`.sqm`** | Fichero de migración; `N.sqm` migra **desde** la versión N. Hay 14 → esquema 15. |
| **`uuid`** | Identificador de 32 hex estable entre dispositivos; lo que permite fusionar sin duplicar. |
| **`actualizadoEn`** | Epoch millis de la última escritura; decide quién gana en una fusión. |
| **`.vpet`** | Paquete de datos: ZIP con `manifiesto.json` + `datos.json` + `archivos/`. |
| **`FOREIGN KEY ... ON DELETE CASCADE`** | Borrar la mascota borra sus filas hijas. **Requiere que las claves foráneas estén activas en esa conexión**: en escritorio van por `Properties(foreign_keys=true)`, en Android por `setForeignKeyConstraintsEnabled(true)` en `onConfigure`. |
| **WAL** | Modo de diario de SQLite; por eso no se copia el `.db` a pelo. |
| **Alarma exacta** | `setExactAndAllowWhileIdle` con permiso `SCHEDULE_EXACT_ALARM`; sin él, Android agrupa. |
| **`PendingIntent`** | Acción que otra app (o el sistema) puede disparar en tu nombre: los botones Aceptar/Posponer. |
| **`FileProvider`** | Compartir un fichero privado sin dar permisos de almacenamiento a la otra app. |
| **SAF** (Storage Access Framework) | El selector de archivos de Android: "guardar copia" o "elegir copia" sin permisos. |
| **`jpackage`** | Empaqueta una app Java con su propio runtime en `.exe`/`.msi`/`.dmg`/`.deb`. |
| **`versionCode`** | Número de versión que Android compara para permitir actualizar encima. |
| **R8/ProGuard** | Ofuscador/minificador de Android; aquí **desactivado** a propósito. |
| **`StateFlow`** | Flujo de estado observable; la UI se suscribe y se redibuja. |
| **`rememberSaveable`** | Estado que sobrevive a girar la pantalla (no a cerrar la app). |
| **CompositionLocal** | Pasar un valor (idioma/región) a todo el árbol de UI sin pasarlo a mano. |

---

## 24. Apéndice: cómo he comprobado cada cifra

Todas las cifras de esta guía salen de un comando, no de la memoria. Estos son, para que puedas
repetirlos (y para que nadie te pille improvisando):

| Cifra | Comando |
| --- | --- |
| Líneas y ficheros por sourceSet | `foreach ($ss in 'commonMain','androidMain','jvmMain','jvmTest') { $f = Get-ChildItem "shared/src/$ss" -Recurse -Include *.kt; "$ss $($f.Count) ficheros $((($f | % { (Get-Content $_).Count }) | Measure-Object -Sum).Sum) lineas" }` |
| Producción y % común | Producción = commonMain + androidMain + jvmMain + los dos envoltorios = **19.322** líneas; común = **16.042** → 16.042 / 19.322 = **83,0 %** (revisión 1.14) |
| Total de líneas | 19.322 producción + 6.998 de test = **26.320** |
| Nº de pruebas y de clases | `Get-ChildItem shared/build/test-results/jvmTest/*.xml \| % { ([xml](Get-Content $_)).testsuite } \| Measure-Object tests -Sum` → **280**, y `.Count` de ficheros = **39** clases |
| `expect`/`actual` | `Select-String -Path (gci shared/src/commonMain -Recurse -Include *.kt).FullName -Pattern '\bexpect\b' \| Measure-Object` → 44 en 14 ficheros |
| Targets de la app | `Select-String shared/build.gradle.kts -Pattern 'androidLibrary|jvm\(\)\|ios'` → solo `androidLibrary` y `jvm()` |
| `java.io.File` en común | `Select-String -Path (gci shared/src/commonMain -Recurse -Include *.kt).FullName -Pattern '^import java\.'` → `ImagenMascota.kt:4` |
| compose-pdf sin iOS | listar el grupo en la caché de Gradle: solo `compose-pdf-android` y `compose-pdf-jvm` |
| Versión del esquema y `.sqm` | `(Get-ChildItem shared/src/commonMain/sqldelight/migrations/*.sqm).Count` = 14 → esquema 15 |
| Fecha del primer `.sqm` | `git log --format='%h %ad %s' --date=short --diff-filter=A -- shared/src/commonMain/sqldelight/migrations/1.sqm` → `5aa7c2a 2026-09-16`; el commit inicial es `1b9a8d6 2026-09-04` |
| Huérfanas del escritorio | programa JDBC aparte sobre una **copia** de la base real: conexión A con `foreign_keys=1`, conexión B sin él, `DELETE` en B → **19 huérfanas** en la mascota 3 |
| Limpieza al abrir | `CascadaEscritorioTest` (5 tests) con base en fichero y copia `virtualpet-antes-de-limpiar.db` |
| Android y claves foráneas | emulador: `logcat` del `onConfigure` (temperal, ya quitado) + borrado de una mascota con 19 filas hijas desde la interfaz → `PRAGMA foreign_key_check` = 0 |
| `PreviewActivity` fuera del release | abrir la APK como ZIP y buscar `compose/ui/tooling/PreviewActivity` en los `classes*.dex`: aparece en `dist/VirtualPet-1.12.apk` y **no** en la recién compilada |
| Tamaño de la copia de Google | `adb shell bmgr backupnow com.example.virtualpetkmp` con transporte local: 20.009.984 B con 12,5 MB en `files/copias` → 7.490.048 B con las reglas |
| Fotos antes/después (640 px) | test temporal con las dos fotos reales del `.vpet` de rescate: 3.469 KB → **60 KB** y 3.588 KB → **53 KB**; `.vpet` con las dos **115 KB** (original 7,27 MB) |
| Tamaño de una foto ya guardada | `run-as ... ls -l files/fotos` en el emulador: la nueva **5.657 B (480×640)**; antiguas 3.552.909 y 3.674.882 B (3072×4096, sin tocar) |
| Objetos cableados en `App.kt` | leer `App.kt:58-71`: 9 repositorios + 3 ViewModels + `programador` + `database` = **14** |
| Cadenas por idioma | `Get-ChildItem shared/src/commonMain/composeResources -Recurse -Filter strings.xml \| % { ((Select-String -Path $_.FullName -Pattern ''<string name='' \| Measure-Object).Count) }` → **359** en cada uno de los 6 |
| Coste de ir en transacción | test temporal midiendo 300 `insert` sobre fichero: **6.283 ms** sin transacción frente a **36 ms** en una sola (×174) |
| Id del alta | `IdInsertadoTest` con base en fichero: antes del arreglo los 4 tests fallaban (`id = 0` y, con claves foráneas activas, `FOREIGN KEY constraint failed` al insertar el hijo con `mascotaId = 0`) |
| Aviso de base más nueva | Android: emulador con `user_version = 99` → pantalla "Los datos son de una versión más nueva … versión 99 … hasta la 15". Escritorio: `.exe` empaquetado con una copia de la base a 99 → diálogo del sistema con el mismo texto; con la base al día abre normal |
| Warnings de compilación | `.\gradlew.bat :shared:compileKotlinJvm --rerun-tasks --no-build-cache` y contar líneas `^w:` → **116** (casi todas por el artefacto *compat* de `kotlinx-datetime`) |

### Lo que NO he podido verificar (marcado también como `[NO VERIFICADO]`)

- **La copia de Google real**: aquí se usa el transporte **local** del emulador. Que Google acepte el
  respaldo y respete las reglas en una cuenta real no se ha probado (mismo formato de reglas, pero
  es otra implementación).
- **iOS**: no hay Mac ni dispositivo; todo lo de iOS es análisis del build, no ejecución.
- **El `.msi`**: no existe, falta WiX.
- **El `.exe` de la 1.13 instalado con el instalador de Windows**: se arrancó el binario empaquetado
  a mano contra una copia de la base; la instalación desde el ZIP no se ha repetido tras los cambios.
- **El móvil real con la 1.13**: los cambios de esta revisión están probados en el emulador (que es
  un Android distinto del HyperOS del usuario).
- **Traducciones de de/pt/fr/it**: siguen sin revisión nativa.
