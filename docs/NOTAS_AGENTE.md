# Notas para el agente

Reglas de trabajo de este proyecto. Se cumplen siempre.

## Reglas

- **Responde siempre en español**, aunque el encargo o un fichero lleguen en otro idioma.
- **No añadas herramientas, lenguajes ni ficheros de otro tipo sin preguntarme antes.** Si hace falta algo, dilo con lo que es y para qué, y espera.
- Guarda siempre en UTF-8 sin BOM.
- Android solo en el emulador: nunca toques móviles físicos ni la app `com.example.virtualpetkmp` sin que se te diga.
- Nunca hagas push ni fusiones sin que se te diga. Un commit por tema.
- Los textos para el usuario, solo en los recursos de los 6 idiomas; cada traducción nueva es NO VERIFICADA.
- Los mensajes de las excepciones son para los logs, no para el usuario.
- Informa siempre VERIFICADO / SUPUESTO / NO VERIFICADO. No adaptes los tests para que pasen.
- Las migraciones, solo aditivas. Las pruebas de base de datos, sobre copias: nunca sobre `~/.virtualpet`.
- Salidas largas, nunca: solo totales y errores.

## Cómo se hace
Todas las órdenes se lanzan desde la raíz del proyecto, con la convención del proyecto (VERIFICADO): `$env:GRADLE_USER_HOME="$PWD\.gradle-home"` antes de `.\gradlew.bat`.

**Tests** — VERIFICADO: 51 suites, 367 tests, 0 fallos. `:shared:allTests` cuelga solo de `jvmTest`.
```
.\gradlew.bat :shared:jvmTest --console=plain
```
**APK de release** — VERIFICADO: compila y queda firmada (`CN=Virtual Pet`).
```
.\gradlew.bat :androidApp:assembleRelease
```
Queda en `androidApp\build\outputs\apk\release\androidApp-release.apk`; sin `keystore.properties` sale `...-unsigned.apk`.

**App Bundle (.aab)** — VERIFICADO: compila y `jarsigner -verify` responde `jar verified`.
```
.\gradlew.bat :androidApp:bundleRelease
```
Queda en `androidApp\build\outputs\bundle\release\androidApp-release.aab`. La 1.14, `versionCode = 15` y `versionName = "1.14"` (que está dentro del manifiesto del `.aab`): 17.210.898 bytes, SHA-256 `3BE09E7A0F23F8A0DF87BA323D45173296A136EC7A8856CF5654E5BB614B7F74`.

**Instalador de Windows (Inno Setup)** — VERIFICADO: instala por usuario sin administrador, se instala encima de la versión anterior, se desinstala sin dejar rastro y no toca `%USERPROFILE%\.virtualpet`.
```
.\gradlew.bat :desktopApp:createDistributable
python packaging\windows\empaquetar.py
```
La app la sigue montando jpackage en `desktopApp\build\compose\binaries\main\app\Virtual Pet\`, y `empaquetar.py` llama a `ISCC.exe` (`packaging\windows\VirtualPet.iss`) y deja `dist\VirtualPet-<versión>-setup.exe`. Hace falta **Inno Setup 6** instalado: si no está, el script lo dice y se para (no instala nada por su cuenta). La 1.14: 97,5 MB, SHA-256 `1597164EC0A5A6C837A5AFC57FEEB5FA7E8664597F4851A19DB16595554ABE0E`.

El `LEEME.txt` ya no va dentro del paquete: es el texto para la web y vive en `docs\LEEME.txt`.

## Trampas conocidas

- **La versión vive en un solo sitio**: `packageVersion` de `desktopApp/build.gradle.kts`. Ni el `.iss` ni `empaquetar.py` la llevan escrita: el script la lee de ahí y se la pasa a Inno con `/DVersion`. Si algún día hay que cambiarla, se cambia ahí y en el `versionName` de Android (que es de otra app), y nada más.
- **El `AppId` del `.iss` no se toca nunca** (`D6C352D6-FAD5-4D6F-B64C-54A5344E2639`): es lo que hace que una entrega nueva se reconozca como actualización de la misma app. Si cambia, Windows instala otra app distinta y deja la anterior al lado.
- **Hace falta Inno Setup 6 para empaquetar** (`ISCC.exe`; se instala con `winget install --id JRSoftware.InnoSetup -e`). Ojo: la versión 6.7 imprime "Non-commercial use only" al compilar aunque su `license.txt` permita uso comercial; si esto se vende algún día, revisarlo.
- **Dentro de `[Code]` ninguna línea puede empezar por `[`**: el compilador la toma como una sección y falla con "Invalid section tag". Si hay que pasar un array, se mete en la misma línea o en una variable.
- **El icono `.ico` va con transparencia y sale de `logo.png`.** Son siete PNG de 32 bits (de 16 a 256 px) y el canal alfa se respeta: si se aplana sobre blanco, el ejecutable y los accesos directos salen con un cuadro detrás (fue el fallo de la 1.14, ya corregido). Se usa el logo del **corazón solo** (`desktopApp\src\main\resources\logo.png`), sin el nombre: el nombre ya lo pone la etiqueta del acceso directo.
- **Un aviso pospuesto vive en `posposiciones.txt`**, con el **id de la alarma** como clave (que ya incluye la toma: posponer la de la mañana no toca la de la noche). `sincronizarAvisos()` **no lo borra**: lo conserva mientras su tratamiento y su toma sigan existiendo y la hora sea futura, y retira solo las posposiciones de tratamientos borrados o de tomas que se quitaron al editar el horario.
- **Deshacer sobre una base vacía no puede hacer nada**: la copia previa de una fusión hecha en un dispositivo recién instalado no tiene datos y el lector la rechaza (`DATOS_INCOMPLETOS`). Desde la 1.14 se avisa con un texto propio (`deshacer_vacio`) en vez de dejar el botón sin respuesta; los datos fusionados no se tocan.
