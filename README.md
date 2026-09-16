Este es un proyecto Kotlin Multiplatform dirigido a Android y Escritorio (JVM).

* [/shared](https://github.com/eyemerald/VirtualPetKMP/blob/main/shared/src) contiene el código compartido entre las aplicaciones de Compose Multiplatform. Incluye varias subcarpetas:
   * [commonMain](https://github.com/eyemerald/VirtualPetKMP/blob/main/shared/src/commonMain/kotlin) contiene el código común para todas las plataformas.
   * Las demás carpetas contienen código Kotlin que se compila únicamente para la plataforma indicada en el nombre de la carpeta. Por ejemplo, si quisieras usar CoreCrypto de Apple para la parte de iOS de tu app Kotlin, la carpeta [iosMain](https://github.com/eyemerald/VirtualPetKMP/blob/main/shared/src/iosMain/kotlin) sería el lugar adecuado para esas llamadas. Del mismo modo, si quieres editar la parte específica de Escritorio (JVM), la carpeta [jvmMain](https://github.com/eyemerald/VirtualPetKMP/blob/main/shared/src/jvmMain/kotlin) es la ubicación apropiada.

## Ejecutar las aplicaciones

Usa las configuraciones de ejecución que ofrece el widget de "run" en la barra de herramientas de tu IDE. También puedes usar estos comandos:

* App de Android: `./gradlew :androidApp:assembleDebug`
* App de Escritorio:
   * Hot reload: `./gradlew :desktopApp:hotRun --auto`
   * Ejecución estándar: `./gradlew :desktopApp:run`

## Ejecutar los tests

Usa el botón de ejecución en el margen del editor de tu IDE, o ejecuta los tests mediante tareas de Gradle:

* Tests de Android: `./gradlew :shared:testAndroidHostTest`
* Tests de Escritorio: `./gradlew :shared:jvmTest`

---

Más información sobre [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…
