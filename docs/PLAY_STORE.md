# Publicar Virtual Pet en Google Play

Este documento reúne todo lo que hace falta para subir la app a Google Play y las respuestas ya
preparadas para los formularios de Play Console. Está escrito para que lo pueda seguir alguien que
no haya participado en el desarrollo.

Marca de cada dato:

- **VERIFICADO**: comprobado en este repositorio o en el dispositivo, con el comando o la captura
  que se indica.
- **SUPUESTO**: deducido de la documentación de Google o de cómo funciona Play, pero **no**
  comprobado aquí (no hay cuenta de Play Console en este entorno).
- **NO VERIFICADO**: sin comprobar todavía.

> **Nada de este documento se ha subido a Play.** No hay cuenta de desarrollador, no se ha creado
> ninguna clave de subida y no se ha enviado ninguna versión. Todo lo que se describe aquí está
> preparado en local.

---

## 1. Qué identifica a la app

| Dato | Valor | Estado |
| --- | --- | --- |
| `applicationId` (id del paquete en Play) | `io.github.eyemerald.virtualpet` | VERIFICADO (`androidApp/build.gradle.kts`) |
| `namespace` del código | `com.example.virtualpetkmp` | VERIFICADO |
| Nombre visible | `Virtual Pet` | VERIFICADO (`application-label` del APK de release) |
| `versionCode` | `14` | VERIFICADO |
| `versionName` | `1.13` | VERIFICADO |
| `minSdk` | `26` (Android 8.0) | VERIFICADO |
| `targetSdk` | `36` (Android 16) | VERIFICADO |
| `compileSdk` | `37` | VERIFICADO |

Comprobado con:

```
aapt2 dump badging build\apks\extraidos\base-master.apk
package: name='io.github.eyemerald.virtualpet' versionCode='14' versionName='1.13'
minSdkVersion:'26'
targetSdkVersion:'36'
application-label:'Virtual Pet'
```

**El `namespace` no cambia a propósito.** Es el paquete del código (el del `R` generado y el de
las clases) y no tiene nada que ver con la identidad de la app. Cambiarlo obligaría a mover todos
los ficheros de código y no aporta nada de cara a Play.

### Por qué `targetSdk` 36 y no menos

Play **exige** API 36 para apps y actualizaciones nuevas desde el 31 de agosto de 2026 (con
posibilidad de prórroga hasta el 1 de noviembre de 2026). Hoy esa fecha ya ha pasado, así que una
versión con `targetSdk` 34 no se puede enviar. Fuente:
<https://support.google.com/googleplay/android-developer/answer/11926878>.

---

## 2. Qué se construye y cómo se comprueba

### 2.1. Generar el App Bundle (.aab)

```
$env:GRADLE_USER_HOME="$PWD\.gradle-home"
.\gradlew.bat :androidApp:bundleRelease --console=plain
```

Sale en `androidApp/build/outputs/bundle/release/androidApp-release.aab`
(**17.157.058 bytes**, VERIFICADO; se comprueba que dentro va `versionCode 14` / `versionName 1.13`
con `java -jar bundletool.jar dump manifest --bundle=…`).

### 2.2. Comprobar que va firmado

La firma se lee de `keystore.properties` (que **no** se sube al repositorio). Si ese fichero falta,
Gradle deja el bundle **sin firmar** y el build no falla: Play lo rechazaría, así que hay que
comprobarlo siempre.

```
jarsigner -verify androidApp\build\outputs\bundle\release\androidApp-release.aab
-> jar verified.
```

Salida literal (el aviso de certificado autofirmado es lo normal y no es un problema para Play):

```
jar verified.

Warning:
This jar contains entries whose certificate chain is invalid. Reason: PKIX path building failed:
sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification
path to requested target
This jar contains entries whose signer certificate is self-signed.
```

Huella del certificado (VERIFICADO con `keytool -list -v`):

```
SHA256: D2:C4:21:E2:E8:93:EE:24:5D:83:BC:4E:1A:6A:5B:54:92:FF:9E:C4:CA:BA:18:8A:BE:AD:C2:8D:4B:9A:B6:CB
Alias: virtualpet
```

> **Esta clave no se puede perder.** Si se pierde, Play no acepta la primera subida (o, si ya está
> publicada, no se pueden subir actualizaciones firmadas por Google con esa identidad). Guardar
> `firma/virtualpet-release.jks` y las contraseñas en un sitio seguro y con copia. **Nunca** subir
> esos dos ficheros al repositorio: están en `.gitignore` (VERIFICADO con `git check-ignore -v`).

Comprobación de que no se cuelan en git:

```
git check-ignore -v keystore.properties firma/virtualpet-release.jks
.gitignore:30:keystore.properties    keystore.properties
.gitignore:28:*.jks                  firma/virtualpet-release.jks
```

### 2.3. Generar APKs a partir del bundle (lo que hace Play)

Play no entrega el `.aab`: lo parte en APKs según el dispositivo. Eso se reproduce con
`bundletool` (<https://github.com/google/bundletool>), que **no** viene con el SDK: hay que
descargarlo aparte (aquí se usó la 1.18.3).

```
java -jar bundletool.jar build-apks --bundle=androidApp-release.aab --output=virtualpet.apks
     --ks=firma\virtualpet-release.jks --ks-key-alias=virtualpet --ks-pass=pass:... --key-pass=pass:...

java -jar bundletool.jar extract-apks --apks=virtualpet.apks --output-dir=apks --device-spec=device-spec.json
-> base-master.apk (17.005.341 bytes)
   base-en.apk     (57.491)
   base-x86_64.apk (37.036)
   base-xxhdpi.apk (12.941)

java -jar bundletool.jar install-apks --apks=virtualpet.apks --device-id=<emulador>
-> Success
```

El dispositivo de prueba reconocido, con `bundletool get-device-spec`, fue un Android 8.0
(`sdkVersion: 26`, `x86_64`, densidad 420). Con esos APKs la app **arranca y funciona**
(VERIFICADO: lista, alta de mascota, ficha y copia de seguridad).

### 2.4. Revisar el manifiesto del release

```
aapt2 dump xmltree --file AndroidManifest.xml base-master.apk
```

Lo que tiene que verse (VERIFICADO):

| Comprobación | Resultado |
| --- | --- |
| `debuggable` | **no aparece** → la app de release no es depurable |
| `versionCode` / `versionName` | `14` / `1.13` |
| `minSdkVersion` / `targetSdkVersion` | `26` / `36` |
| Permisos | `POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED` |
| Permiso propio | `io.github.eyemerald.virtualpet.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` (nivel `signature`, lo añade AndroidX) |
| `allowBackup` | `true`, con `dataExtractionRules` y `fullBackupContent` |
| `extractNativeLibs` | `false` |
| `MainActivity` | `exported="true"`, `launchMode="singleTop"` |
| `FileProvider` | autoridad `io.github.eyemerald.virtualpet.fileprovider`, `exported="false"` |
| Receptores propios | los tres con `exported="false"` |
| Componentes de depuración (`PreviewActivity`, etc.) | **ninguno** |

Que la app no es depurable también se nota al intentar leer sus datos con `run-as`:

```
adb shell run-as io.github.eyemerald.virtualpet ls
run-as: package not debuggable: io.github.eyemerald.virtualpet
```

### 2.5. Páginas de 16 KB

Android 15 y 16 pueden usar páginas de memoria de 16 KB. Si la app trae librerías nativas
alineadas a 4 KB, el sistema las abre en "modo compatibilidad" y avisa al usuario. Esta app solo
trae **una** librería nativa (`libandroidx.graphics.path.so`, de AndroidX), y está bien alineada
(VERIFICADO):

```
zipalign -c -P 16 -v 4 base-x86_64.apk
16384 lib/x86_64/libandroidx.graphics.path.so (OK)
Verification successful
```

```
python build\comprobar-elf.py lib\x86_64\libandroidx.graphics.path.so
PT_LOAD offset=0x0 p_align=0x4000 (16384)
PT_LOAD offset=0x1ec0 p_align=0x4000 (16384)
PT_LOAD offset=0x2268 p_align=0x4000 (16384)
=> 16 KB: SI
```

Además, la app se instaló y se usó en un emulador con páginas de 16 KB
(`adb shell getconf PAGE_SIZE` → `16384`) sin ningún aviso de compatibilidad.

---

## 3. Permisos y justificación

Solo tres permisos, y uno de ellos lo añade AndroidX:

| Permiso | Para qué | Cuándo se pide |
| --- | --- | --- |
| `POST_NOTIFICATIONS` | Mostrar recordatorios de vacunas y tratamientos | Al abrir la app, en Android 13+ |
| `SCHEDULE_EXACT_ALARM` | Que el recordatorio salte a la hora exacta | El sistema lo concede desde Ajustes; la app detecta si falta, avisa en su menú y lleva allí |
| `RECEIVE_BOOT_COMPLETED` | Volver a programar los avisos tras reiniciar el móvil (Android borra las alarmas al apagar) | Lo concede el sistema al instalar |

Texto para el formulario de Play, si lo pide:

- **Alarmas exactas.** La app programa recordatorios de medicación y de vacunas que el usuario
  elige **a una hora concreta**; con una alarma inexacta el aviso puede llegar con hasta una hora
  de retraso y la dosis se da tarde. Se usa `SCHEDULE_EXACT_ALARM` (no `USE_EXACT_ALARM`, que es
  para apps de reloj y calendario) y, si el usuario no concede el permiso, la app **sigue
  funcionando** con avisos inexactos y avisa de ello en el menú.
- **No hay permiso de INTERNET.** La app no se conecta a ningún sitio.
- **No se quitan permisos heredados de AndroidX**: el manifiesto borra explícitamente
  `WRITE_EXTERNAL_STORAGE`, `READ_EXTERNAL_STORAGE` y `READ_PHONE_STATE` para que quien instale la
  app no vea permisos que no usa (VERIFICADO: no aparecen en el manifiesto del release).

---

## 4. Seguridad de los datos (formulario "Data safety")

Borrador de respuestas (SUPUESTO en cuanto a cómo las presenta Play; los hechos son VERIFICADOS):

| Pregunta | Respuesta |
| --- | --- |
| ¿La app recoge o comparte datos del usuario? | **No** |
| ¿Recoge datos de forma cifrada en tránsito? | No aplica: no hay tránsito |
| ¿Se pueden pedir los datos para borrarlos? | No aplica: los datos no salen del dispositivo |
| ¿Recoge ubicación, contactos, salud, fotos…? | **No**. Las fotos se eligen con el selector del sistema y se copian a la carpeta privada de la app; no se leen del almacenamiento externo |
| ¿Se usa publicidad o analítica? | No |
| ¿Hay SDKs de terceros que recojan datos? | No |
| ¿Cuenta con un proveedor de datos independiente? | No |

Detalle para quien revise:

- La base de datos (SQLDelight) y las fotos viven en `/data/data/io.github.eyemerald.virtualpet/`.
- La **copia de seguridad del sistema** está activada (`allowBackup=true`): si el usuario cambia de
  móvil, Android le devuelve los datos. Las copias internas de la app (`files/copias`) se excluyen
  del respaldo porque son una copia *de* los datos y harían que se pasara del límite de 25 MB.
- El **traspaso entre dispositivos** es un fichero `.vpet` que el usuario guarda donde quiera y
  mueve a mano. La app no lo envía a ninguna parte; si el usuario pulsa "Compartir", se lo entrega
  a la app que elija (correo, Drive…), que es una acción explícita suya.

---

## 5. Ficha de la tienda (borradores)

Categoría sugerida: **Estilo de vida** (o *Salud y bienestar*, si se prefiere; la app no es
dispositivo médico). Clasificación de contenido: la de declarar en Play Console sin contenido
sensible (**SUPUESTO**: no hay violencia, apuestas, compras ni anuncios).

### 5.1. Español

- **Título (≤ 30)**: `Virtual Pet: ficha de mascotas`
- **Descripción corta (≤ 80)**: `Vacunas, pesos, tratamientos y notas de tus mascotas, sin cuenta y sin internet.`
- **Descripción larga**:

```
Virtual Pet es una libreta completa para las mascotas de casa: quiénes son, qué vacunas llevan,
cuánto pesan, qué medicación toman y qué hay que recordar.

Funciona sin cuenta y sin conexión: todos los datos se quedan en tu teléfono.

Lo que puedes hacer:
• Ficha de cada mascota con foto, especie, raza, sexo, color, microchip y peso ideal.
• Vacunas y desparasitaciones con fecha de la próxima dosis.
• Revisiones veterinarias, tratamientos con sus dosis y notas.
• Peso con gráfico y comparación con el peso ideal.
• Informes: guarda el PDF del vete o la foto del análisis.
• Avisos a la hora que elijas para las próximas vacunas y las dosis de cada tratamiento, con
  botones para aceptar o posponer.
• Copia de seguridad en un solo archivo y traspaso a otro móvil cuando quieras.

Sin anuncios, sin analítica y sin permisos que no necesite.
```

### 5.2. English

- **Title**: `Virtual Pet: pet records`
- **Short description**: `Vaccines, weights, treatments and notes for your pets. No account, no internet.`
- **Full description**:

```
Virtual Pet is a complete record book for the pets at home: who they are, which vaccines they have
had, what they weigh, what medication they take and what needs remembering.

It works with no account and no connection: everything stays on your phone.

What you can do:
• A page per pet with photo, species, breed, sex, colour, microchip and ideal weight.
• Vaccines and deworming with the date of the next dose.
• Vet visits, treatments with their doses and notes.
• Weight with a chart and a comparison against the ideal weight.
• Reports: keep the vet's PDF or a photo of the blood test.
• Reminders at the time you choose for upcoming vaccines and for every treatment dose, with
  buttons to accept or postpone.
• Whole-app backup in a single file, and a hand-over file to move to a new phone.

No ads, no analytics and no permissions it does not need.
```

### 5.3. Português

- **Título**: `Virtual Pet: ficha dos pets`
- **Descrição curta**: `Vacinas, pesos, tratamentos e notas dos seus pets. Sem conta e sem internet.`
- **Descrição longa**:

```
O Virtual Pet é um caderno completo para os pets de casa: quem são, que vacinas têm, quanto pesam,
que medicação tomam e o que é preciso lembrar.

Funciona sem conta e sem ligação: todos os dados ficam no seu telemóvel.

O que pode fazer:
• Uma ficha por pet com foto, espécie, raça, sexo, cor, microchip e peso ideal.
• Vacinas e desparasitações com a data da próxima dose.
• Consultas veterinárias, tratamentos com as suas doses e notas.
• Peso com gráfico e comparação com o peso ideal.
• Relatórios: guarde o PDF do veterinário ou a foto da análise.
• Avisos à hora que escolher para as próximas vacinas e para cada dose, com botões para aceitar ou
  adiar.
• Cópia de segurança num único ficheiro e transferência para outro telemóvel.

Sem anúncios, sem analítica e sem permissões de que não precise.
```

> Los textos son borradores: si quieres decir algo distinto (por ejemplo, que solo es para perros y
> gatos, o mencionar que no sustituye al veterinario), se cambian aquí y en la ficha.

**Idiomas de la ficha**: se preparan en español, inglés y portugués. La **app** está traducida a
seis (español, inglés, portugués, francés, alemán e italiano); las traducciones de francés, alemán
e italiano están marcadas como NO VERIFICADAS en el repositorio, así que **no** conviene anunciar
la app en esos idiomas en la ficha hasta que alguien las revise.

---

## 6. Recursos gráficos que pide Play

| Recurso | Requisito | Estado |
| --- | --- | --- |
| Icono de la tienda | 512×512 PNG, 32 bits | NO VERIFICADO (hay que exportarlo del icono de la app) |
| Gráfico destacado | 1024×500 PNG o JPG | NO VERIFICADO |
| Capturas de teléfono | 2–8, lado corto ≥ 320 px | **Hechas** (1280×2772): `docs/capturas-play/01-lista.png`, `02-ficha.png`, `03-vacunas.png`, `04-aviso.png`; 1,86 MB en total |
| Capturas de tablet 7" y 10" | Obligatorias si se declara compatibilidad con tablet | Pendiente: las de `build/b3-tablet-*.png` son de emulador y a resolución forzada; hay que repetirlas en un tablet real o restringir la app a teléfonos |
| Vídeo | Opcional | — |

La app sí funciona en tablet (VERIFICADO en una ventana de 800×1280 dp y en 1280×800 dp). Si no se
quieren preparar capturas de tablet, en Play Console se puede **restringir a teléfonos**, pero no
se recomienda: el layout aguanta bien.

---

## 7. Pasos en Play Console

1. Crear la cuenta de desarrollador (25 USD, pago único). **No se ha hecho y no se puede hacer
   desde aquí.**
2. Crear la app con el nombre `Virtual Pet` y el idioma predeterminado (español o inglés).
   El `applicationId` lo pone el primer `.aab` que se suba: `io.github.eyemerald.virtualpet`
   **no se podrá cambiar después**.
3. Rellenar: ficha (sección 5), clasificación de contenido, público objetivo, seguridad de datos
   (sección 4) y política de privacidad (sección 8).
4. Subir el `.aab` a **Prueba interna** primero. Comprobar que instala y que la copia de seguridad
   del móvil viejo se puede restaurar (el traspaso con `.vpet` ya está probado entre dos
   `applicationId` distintos).
5. Si todo va bien, promover a producción.

Cosas que **solo puede hacer el dueño de la cuenta**: aceptar los acuerdos, pagar, decidir el
nombre definitivo y —si algún día se quiere— activar "Play App Signing". Si se activa, Google
guarda la clave de firma de las subidas y la nuestra pasa a ser la de **subida**; no cambia el
`.aab` que se genera aquí.

---

## 8. Política de privacidad (borradores)

Play exige una URL pública con la política de privacidad. Vale cualquier página (por ejemplo un
`README` en el repositorio, una página de GitHub Pages o un texto en un blog). Los textos de abajo
se pueden pegar tal cual.

### 8.1. Español

```
Política de privacidad de Virtual Pet

Virtual Pet no recoge ningún dato personal. La app no tiene permiso de internet y no envía
información a ningún servidor.

Todos los datos que introduces (mascotas, vacunas, pesos, tratamientos, informes, fotos y notas) se
guardan únicamente en el almacenamiento privado de la app en tu dispositivo.

Copias de seguridad: si tienes activada la copia de seguridad de Android, el sistema puede incluir
los datos de la app en la copia de tu cuenta de Google. Eso lo hace Android, no la app, y se puede
desactivar en los ajustes del teléfono.

Traspaso entre dispositivos: la app puede generar un archivo con todos tus datos para que lo muevas
tú mismo a otro teléfono. Ese archivo solo sale del dispositivo si tú lo compartes.

La app no muestra anuncios, no usa analítica y no comparte datos con terceros. No se recogen datos
de menores porque no se recoge ningún dato.

Contacto: <correo de contacto>
Última actualización: <fecha>
```

### 8.2. English

```
Virtual Pet privacy policy

Virtual Pet collects no personal data. The app has no internet permission and sends no information
to any server.

Everything you enter (pets, vaccines, weights, treatments, reports, photos and notes) is stored only
in the app's private storage on your device.

Backups: if you have Android backup enabled, the system may include the app's data in your Google
account backup. That is done by Android, not by the app, and can be turned off in your phone's
settings.

Moving to a new phone: the app can create a file with all your data so you can move it yourself to
another phone. That file only leaves the device if you share it.

The app shows no ads, uses no analytics and shares no data with third parties. No data from
children is collected because no data is collected at all.

Contact: <contact email>
Last updated: <date>
```

### 8.3. Português

```
Política de privacidade do Virtual Pet

O Virtual Pet não recolhe quaisquer dados pessoais. A app não tem permissão de internet e não envia
informação para nenhum servidor.

Todos os dados que introduz (pets, vacinas, pesos, tratamentos, relatórios, fotos e notas) ficam
guardados apenas no armazenamento privado da app no seu dispositivo.

Cópias de segurança: se tiver a cópia de segurança do Android ativa, o sistema pode incluir os dados
da app na cópia da sua conta Google. Isso é feito pelo Android, não pela app, e pode ser desativado
nas definições do telemóvel.

Transferência entre dispositivos: a app pode gerar um ficheiro com todos os seus dados para que o
mova para outro telemóvel. Esse ficheiro só sai do dispositivo se o partilhar.

A app não mostra anúncios, não usa analítica e não partilha dados com terceiros. Não são recolhidos
dados de menores porque não é recolhido nenhum dado.

Contacto: <correio de contacto>
Última atualização: <data>
```

---

## 9. Decisiones tomadas y lo que sigue pendiente

**Ya decidido** (no hace falta volver a discutirlo):

- **Versión**: `versionCode 14` / `versionName 1.13` en Android y `1.13.0` en el paquete de
  escritorio.
- **Vuelta atrás del sistema**: implementada (`BotonAtrasSistema`, expect/actual). Ficha → lista y
  formulario → pantalla anterior; en la lista, atrás cierra la app.
- **Alarmas exactas**: se mantiene **`SCHEDULE_EXACT_ALARM`**. `USE_EXACT_ALARM` queda descartado
  porque está reservado a apps de reloj y calendario (SUPUESTO: Play lo revisa en la revisión
  manual; la documentación de Android lo dice explícitamente). La app funciona sin el permiso, con
  avisos inexactos y un aviso en su menú.
- **Ofuscación (R8)**: **no entra ahora**. Probado en una rama aparte y revertido: la APK de
  release baja de 17.531.219 a 6.324.218 bytes (−64 %) y la app funciona, pero un fallo de R8 es
  más difícil de diagnosticar que el espacio que ahorra. Queda como mejora futura, y si se activa
  hay que repetir la batería de pruebas manuales sobre el APK de release.

**Pendiente** (fuera de este repositorio, sin prisa):

- Cuenta de desarrollador de Play, ficha, clasificación de contenido y seguridad de datos.
- Icono de tienda (512×512) y gráfico destacado (1024×500).
- Capturas de tablet (las de móvil ya están en `docs/capturas-play/`).
- Política de privacidad publicada en una URL y correo de contacto.

---

## 10. Lo que hay que tener a mano el día de la subida

- `firma/virtualpet-release.jks` y `keystore.properties` (fuera de git).
- `androidApp-release.aab` recién generado y verificado con `jarsigner`.
- Icono de la tienda (512×512) y gráfico destacado (1024×500).
- Capturas de teléfono (ya están en `docs/capturas-play/`) y **de tablet**, que siguen pendientes.
- Política de privacidad publicada en una URL.
- Correo de contacto para la ficha.
