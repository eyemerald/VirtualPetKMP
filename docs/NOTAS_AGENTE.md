# Notas para el agente

Reglas de trabajo de este proyecto. Se cumplen siempre.

## Reglas

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

**Tests** — VERIFICADO: 49 suites, 356 tests, 0 fallos. `:shared:allTests` cuelga solo de `jvmTest`.
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
Queda en `androidApp\build\outputs\bundle\release\androidApp-release.aab`.

**ZIP de Windows** — `createDistributable` VERIFICADO; el empaquetado con el script, SIN COMPROBAR.
```
.\gradlew.bat :desktopApp:createDistributable
python packaging\windows\empaquetar.py
```
La app queda en `desktopApp\build\compose\binaries\main\app\Virtual Pet\`; el ZIP, en `dist\VirtualPet-<versión>-windows.zip` (versión escrita en `packaging\windows\empaquetar.py`).
