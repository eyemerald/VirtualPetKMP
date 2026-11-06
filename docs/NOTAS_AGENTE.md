# Notas para el agente

Reglas de trabajo de este proyecto. Se cumplen siempre.

## Reglas

- Guarda siempre en UTF-8 sin BOM. **Excepción, y solo esta**: `packaging/windows/instalar.ps1`, `packaging/windows/desinstalar.ps1` y `packaging/windows/LEEME.txt` van en UTF-8 **con** BOM, porque los lee Windows PowerShell 5.1 y sin BOM se come las tildes. Nunca a `packaging/windows/INSTALAR.bat` (un BOM rompe su primera línea) ni a `packaging/windows/empaquetar.py`.
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
Queda en `androidApp\build\outputs\bundle\release\androidApp-release.aab`. La 1.14, `versionCode = 15` y `versionName = "1.14"` (que está dentro del manifiesto del `.aab`): 17.211.433 bytes, SHA-256 `6FEBC184E3F5BDC6A40163597B728FA4A26AA8CD35C9A24AE39272B6B030C6B2`.

**ZIP de Windows** — VERIFICADO: `createDistributable` compila y el script empaqueta, comprueba los cinco ficheros críticos del ZIP y no encuentra corrupción.
```
.\gradlew.bat :desktopApp:createDistributable
python packaging\windows\empaquetar.py
```
La app queda en `desktopApp\build\compose\binaries\main\app\Virtual Pet\`; el ZIP, en `dist\VirtualPet-<versión>-windows.zip`. Resultado real de la 1.14: **219 ficheros, 110,8 MB, SHA-256 `5F0D278CE512CCBFA22EAC28264172ECA7A3452E852178B03D0A914A48345DC7`**, con `INSTALAR.bat`, `instalar.ps1`, `desinstalar.ps1`, `LEEME.txt` y `Virtual Pet/Virtual Pet.exe` dentro.

## Trampas conocidas

- **La versión va a mano en dos sitios.** No sale sola del `versionName` de Android: al subir de versión hay que cambiarla en `packaging/windows/empaquetar.py` (**línea 18**, el nombre del ZIP) **y** en `packaging/windows/instalar.ps1` (`$version`, lo que se ve en "Aplicaciones instaladas"). Si se cambia solo una, el ZIP y la versión instalada dicen cosas distintas.
- **Un BOM rompe la primera línea de un `.bat`** (y el shebang de un `.py`): de ahí que `INSTALAR.bat` y `empaquetar.py` se guarden sin BOM aunque los `.ps1` y el `LEEME.txt` sí lo lleven.
- **Un aviso pospuesto vive en `posposiciones.txt`**, con el **id de la alarma** como clave (que ya incluye la toma: posponer la de la mañana no toca la de la noche). `sincronizarAvisos()` **no lo borra**: lo conserva mientras su tratamiento y su toma sigan existiendo y la hora sea futura, y retira solo las posposiciones de tratamientos borrados o de tomas que se quitaron al editar el horario.
- **Deshacer sobre una base vacía no puede hacer nada**: la copia previa de una fusión hecha en un dispositivo recién instalado no tiene datos y el lector la rechaza (`DATOS_INCOMPLETOS`). Desde la 1.14 se avisa con un texto propio (`deshacer_vacio`) en vez de dejar el botón sin respuesta; los datos fusionados no se tocan.
