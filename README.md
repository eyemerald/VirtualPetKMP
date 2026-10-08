# 🐾 VirtualPetKMP

**VirtualPetKMP** es una aplicación multiplataforma (Android y Escritorio) para llevar un registro completo de la salud y los cuidados de tus mascotas: vacunas, desparasitaciones, pesos, revisiones veterinarias, tratamientos e informes, todo en un solo lugar.

Desarrollada con **Kotlin Multiplatform** y **Compose Multiplatform**, compartiendo toda la lógica y la interfaz de usuario entre Android y escritorio. **El código es privado**; si estás interesado en él, puedes contactar con el autor.

---

## ✨ Características

- 🐶 **Gestión de mascotas**: fichas con datos básicos (nombre, especie, raza, fecha de nacimiento, sexo, color, microchip).
- 📷 **Foto de la mascota**: se puede elegir de los archivos o **hacer con la cámara** (Android); se ve en la ficha y en la lista de mascotas.
- 🏠 **Ficha como panel**: cabecera con la foto, el nombre y la edad, y tarjetas por bloque (peso, vacunas y preventivos, salud y seguimiento, notas) con el estado de un vistazo: en ámbar lo que vence pronto y en rojo lo vencido.
- 💉 **Vacunas**: registro con próxima dosis, **notificaciones locales** 15 días antes y **añadir el recordatorio al calendario** del teléfono.
- 💊 **Preventivos**: pipetas, desparasitaciones y otros tratamientos preventivos, con aviso 1 día antes y también añadibles al calendario.
- ⚖️ **Pesos**: registro y evolución en **gráfico de barras** en la ficha, y una vista detallada por meses con filtro (todo, último año o un rango de fechas).
- 🩺 **Revisiones veterinarias**: historial con motivo, diagnóstico, notas y veterinario.
- 💊 **Tratamientos**: medicamentos con dosis, frecuencia y fechas.
- 📄 **Informes**: adjuntar informes veterinarios con tipo, descripción y archivo.
- 📝 **Notas**: apuntes rápidos de interés sobre cada mascota.
- 🏥 **Veterinarios**: agenda con urgencias (botón directo de llamada), llamada normal y navegación con Google Maps.
- 📤 **Exportar ficha a PDF**: genera un PDF completo o resumido de la ficha de la mascota.
- 🔔 **Notificaciones locales**: para no olvidar las próximas dosis.
- 🌓 **Tema claro / oscuro**: se adapta a la configuración del sistema.
- 📱 **Multiplataforma**: misma experiencia en Android y Escritorio.

---

## 📸 Capturas

## 📸 Capturas

### Gestión de mascotas

| Lista de mascotas | Ficha de mascota | Nueva mascota |
|-------------------|------------------|---------------|
| ![Mascotas](screenshots/01-mascotas.jpg) | ![Ficha](screenshots/02-ficha-mascota.jpg) | ![Nueva mascota](screenshots/03-nueva-mascota.jpg) |

### Salud y cuidados

| Vacunas | Preventivos | Evolución del peso |
|---------|-------------|-------------------|
| ![Vacunas](screenshots/04-vacunas.jpg) | ![Preventivos](screenshots/05-preventivos.jpg) | ![Peso](screenshots/07-registro-peso.jpg) |

### Veterinarios

| Urgencias 24h | Veterinarios cercanos |
|---------------|----------------------|
| ![Urgencias](screenshots/06-veterinarios-urgencias.jpg) | ![Mapa](screenshots/08-mapa-veterinarios.jpg) |

---

## 🛠️ Tecnologías

- **Kotlin Multiplatform** — código compartido entre Android y Desktop.
- **Compose Multiplatform** — UI compartida.
- **SQLDelight** — base de datos SQLite multiplataforma con migraciones versionadas.
- **Material 3** — sistema de diseño.
- **kotlinx-datetime** — manejo de fechas multiplataforma.
- **kotlinx-coroutines** — programación asíncrona.
- **AlarmManager + BroadcastReceiver** — notificaciones locales en Android.

---

## 📂 Estructura del proyecto

```
VirtualPetKMP/
├── androidApp/          # Módulo Android (Activity, manifest, recursos)
├── desktopApp/          # Módulo Desktop (main.kt, empaquetado)
├── shared/              # Código compartido KMP
│   ├── commonMain/      # Código común (UI, lógica, BD)
│   ├── androidMain/     # Implementaciones específicas de Android
│   ├── jvmMain/         # Implementaciones específicas de JVM/Desktop
│   └── sqldelight/      # Esquema y migraciones de la BD
└── build.gradle.kts
```

---

## 🚀 Cómo ejecutar el proyecto

**Requisitos previos:**
- JDK 11 o superior.
- Android Studio o IntelliJ IDEA con plugin de Kotlin.
- Android SDK (para la parte Android).

**App de Android:**
```bash
./gradlew :androidApp:assembleDebug
```

**App de Escritorio:**
```bash
./gradlew :desktopApp:run
```

Con **hot reload** durante el desarrollo:
```bash
./gradlew :desktopApp:hotRun --auto
```

---

## ✅ Tests

```bash
./gradlew :shared:jvmTest         # Tests de JVM
./gradlew :shared:allTests        # Todos los tests
```

---

## 📌 Estado del proyecto

🚧 **En desarrollo activo.** Actualmente en fase beta con testers.

**Pendiente antes de la v1.0:**

- [ ] **Compartir el PDF de la ficha** en lugar de solo guardarlo: el botón de exportar debe
      ofrecer enviarlo por correo, WhatsApp, guardarlo en una carpeta, etc. Enfoque acordado:
      generar el PDF en la caché y lanzar el "compartir" del sistema (`ACTION_SEND` con
      `application/pdf`), que ya muestra esas opciones, en vez de construir un menú propio.
      Hace falta un `expect/actual` de compartir, como los que ya existen para llamar o abrir
      el mapa. Desbloquea también el punto de backup.
- [ ] **Cartilla veterinaria**: poder guardarla y consultarla dentro de la app, con fotos
      (una foto por página, reutilizando el selector de foto actual) o en PDF/imágenes.
      Requiere decidir antes: tabla nueva de páginas de cartilla (`mascotaId`, `ruta`,
      `fecha`, `nota`), su migración, y cómo mostrar un PDF dentro de la app (hoy los PDF se
      abren con una app externa).
- [ ] **Backup y paso de datos entre PC y móvil**: en una primera fase, exportar e importar
      la base de datos completa a un archivo (es un único fichero SQLite, así que es copiarlo;
      con el compartir del sistema ya se puede subir a Drive o enviarlo por correo). La
      sincronización automática entre dispositivos queda para más adelante: necesita backend
      o cuenta de Google y resolver conflictos.
- [ ] Publicación en Google Play Store.

---

## 📜 Licencia

**Todos los derechos reservados.** El código fuente de este proyecto es privado y no se permite su uso, copia, modificación ni distribución sin autorización expresa del autor.

Si estás interesado en el código, puedes contactar con el autor.

---

## 👤 Autor

**Enrique Robles** — [@eyemerald](https://github.com/eyemerald)

---

## 📚 Más información

- [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)
- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)
- [SQLDelight](https://sqldelight.github.io/sqldelight/)