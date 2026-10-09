# Arquitectura de VirtualPetKMP

Este documento explica **cómo está montado el proyecto y por qué**, para que alguien que
llegue nuevo (o tú mismo dentro de seis meses) pueda leer el código sin perderse. El README
cuenta *qué hace* la app; esto cuenta *cómo está hecha*.

---

## 1. Idea general

Es una app **offline-first**: todo vive en el dispositivo y no hay servidor. La consecuencia
de diseño más importante es que **la base de datos local es la fuente de verdad** y que
cualquier función que dependa de la red no existe. Si algún día se añade sincronización, será
una capa nueva por encima, no un requisito para que la app funcione.

**Stack:** Kotlin Multiplatform + Compose Multiplatform. Un solo código para Android y
escritorio; solo hay código específico de plataforma donde es inevitable (cámara, archivos,
notificaciones, abrir mapas...).

---

## 2. Capas

```
        ui/  (Compose: pantallas, hojas modales, tarjetas)
         │   observa StateFlow, llama a los ViewModels
         ▼
   viewmodel/  (estado + reglas que dependen de "hoy")
         │   habla con los repositorios, nunca con SQLDelight
         ▼
       data/  (repositorios: CRUD + conversión a modelos de dominio)
         │   único sitio que toca la base de datos
         ▼
    SQLDelight  (esquema .sq + migraciones .sqm)
```

La regla que se respeta en todo el proyecto: **la UI no conoce la base de datos y los
repositorios no conocen la UI**. Los repositorios devuelven modelos de dominio
(`Mascota`, `Peso`, `Vacuna`...), nunca las filas generadas por SQLDelight.

### Por qué las reglas de estado están en los ViewModels

En la base de datos se guardan **fechas**, no conclusiones. Que una vacuna esté "vencida" o
"próxima" depende de qué día es hoy, así que ese cálculo se hace al leer, en
`FichaMascotaViewModel.cargarResumen()`. Guardar un estado calculado obligaría a recalcularlo
todo cada día; calcularlo al vuelo cuesta nada y nunca queda desactualizado.

---

## 3. Estructura de carpetas

```
VirtualPetKMP/
├── androidApp/                     # Módulo Android: Activity, manifest, iconos, FileProvider
├── desktopApp/                     # Módulo escritorio
└── shared/
    ├── src/commonMain/
    │   ├── kotlin/com/example/virtualpetkmp/
    │   │   ├── App.kt              # Raíz: crea la BD, los repos y la navegación
    │   │   ├── Mascota.kt          # Modelos de dominio (uno por fichero)
    │   │   ├── data/               # Repositorios + DatabaseFactory (expect)
    │   │   ├── ui/                 # Pantallas, hojas y componentes Compose
    │   │   │   └── theme/          # Colores y tema Material 3
    │   │   ├── util/               # Utilidades y contratos expect/actual
    │   │   └── viewmodel/          # Estado y reglas de negocio
    │   └── sqldelight/com/example/virtualpetkmp/db/
    │       ├── *.sq                # Esquema (describe la versión MÁS RECIENTE)
    │       └── ../migrations/*.sqm # Migraciones versionadas
    ├── src/androidMain/            # actual de Android
    └── src/jvmMain/                # actual de escritorio (+ herramientas de desarrollo)
```

---

## 4. Base de datos y migraciones

Todo el esquema se declara en ficheros `.sq` y el acceso se genera en compilación. El módulo
`shared` usa SQLDelight con `schemaOutputDirectory` configurado.

### Reglas al tocar el esquema

1. Un `.sq` **siempre describe la versión más reciente**, no un histórico. Si añades una
   columna, se añade al `.sq`.
2. **Toda versión nueva necesita su migración** en `migrations/`. El fichero se llama
   `<versión de la que se viene>.sqm`: para pasar de la 11 a la 12 se crea `11.sqm`. Las
   migraciones se aplican en orden y son la única forma de no perder los datos del usuario.
3. La versión del esquema la determina el número de migraciones, no un número escrito a mano.

### Los drivers aplican las migraciones (importante)

Esto **estuvo roto** y es un fallo fácil de repetir: los drivers se construían con el esquema
pero sin ejecutar nunca `migrate()`, así que la primera migración real habría fallado en el
móvil o borrado datos en escritorio. Ahora se resuelve con `SqlSchemaDelegado`, que envuelve
el esquema y lo entrega al driver; Android además necesita
`AndroidSqliteDriver.Callback(esquema)` para que el `onUpgrade` de SQLite llegue a
`migrate()`. **Si tocas `DatabaseFactory`, no quites ese envoltorio.**

### Cómo saber si una migración funciona

Compilar no demuestra nada: un `ALTER TABLE` solo se prueba sobre una base de datos que ya
existe. La forma de verificarlo es abrir la app **con una base de datos de la versión
anterior** y comprobar que arranca sin excepciones y que los datos siguen ahí. En el README
hay un apartado con la herramienta que genera datos de ejemplo, que sirve también para esto.

---

## 5. Navegación

Es deliberadamente mínima. En `App.kt` solo hay tres estados:

| Estado | Pantalla |
|---|---|
| `list` | Lista de mascotas |
| `ficha` | Detalle de una mascota |
| `form` | Alta / edición de mascota |

**Todo lo demás son hojas modales dentro de la ficha** (`ActiveSheet`: vacunas, salud, notas,
peso), no pantallas. Esto es una decisión de arquitectura, no un detalle estético: cuando
había una pantalla por sección, la ficha y el detalle podían mostrar datos distintos porque
tenían ViewModels separados. Ahora hay **un único `FichaMascotaViewModel` por mascota**,
creado con `remember(mascotaId)` y compartido por la pantalla y todas sus hojas, así que
resumen, listas y avisos son siempre coherentes entre sí.

Los formularios de alta/edición de cada entidad también son diálogos dentro de la hoja
(`FormularioHoja`, `EdicionHoja`), no rutas.

---

## 6. Estado y flujos

- Cada ViewModel expone `StateFlow` de solo lectura (`asStateFlow()`), nunca `MutableStateFlow`
  público. La UI los consume con `collectAsState()`.
- Las escrituras de los repositorios devuelven `Result`, no lanzan. Un fallo al guardar se
  comunica al usuario mediante el `errorMessage` del ViewModel correspondiente, porque en una
  app de uso diario perder un registro en silencio es peor que mostrar un error.
- Los ViewModels recargan sus listas después de cada escritura, para que todo lo que observa
  ese mismo estado se actualice sin refrescos manuales.

---

## 7. Código específico de plataforma (`expect` / `actual`)

Los contratos están en `commonMain/util` y cada plataforma los implementa:

| Contrato | Android | Escritorio |
|---|---|---|
| `rememberSelectorFotoMascota` | galería / cámara | selector de archivos |
| `rememberSelectorArchivo` | selector de documentos | selector de archivos |
| `rememberGuardarArchivo` | crear documento | guardar en disco |
| `rememberAbridorArchivo` | intent de visualización | abrir con el SO |
| `rememberLlamador` | `ACTION_DIAL` | (sin efecto) |
| `rememberAbridorMapa` | Google Maps | navegador |
| `rememberAgregadorCalendario` | intent de calendario | (sin efecto) |
| `rememberProgramadorNotificaciones` | `AlarmManager` | (sin efecto) |
| `cargarImagenDesdeRuta` | `BitmapFactory` + EXIF | Skia |
| `DatabaseFactory` | `AndroidSqliteDriver` | `JdbcSqliteDriver` |

**Regla:** el `expect` lleva la documentación del contrato (qué garantiza, qué devuelve); el
`actual` explica solo lo específico de su plataforma. Si añades una función de plataforma,
documenta el contrato en el `expect`.

### Notificaciones

Se programan con `AlarmManager` y un `BroadcastReceiver`. El id de la notificación es el
**id de la fila**, con un detalle que hay que respetar: vacunas y preventivos son tablas
distintas y ambas numeran desde 1, así que los preventivos suman un desplazamiento
(`DESPLAZAMIENTO_PREVENTIVO`) para no pisar la alarma de una vacuna. Si añades otro tipo de
aviso, dale su propio rango.

Las notificaciones se **reprograman al abrir la ficha de una mascota y al arrancar la app**.
No hay servicio en segundo plano: es un equilibrio consciente entre simplicidad y precisión.

---

## 8. Interfaz: decisiones que conviene conocer

- **Ficha como panel.** La cabecera lleva foto, nombre, edad y un menú ⋮ con las acciones.
  Debajo, tarjetas por bloque. Cada tarjeta muestra **un solo dato relevante**, no un recuento:
  el objetivo es que un vistazo diga si hay algo que requiera atención.
- **Semántica del color.** El color no decora, informa:
  - **rojo** = algo va mal (vencido, y en el peso, por debajo del ideal);
  - **ámbar** = vence pronto;
  - **verde** = está bien / por encima del ideal / acciones;
  - **azul** = peso y su gráfico;
  - **lila** = tarjeta de "A tener en cuenta".
  Las tarjetas de la ficha y las de las listas van sobre **fondo claro y neutro**, con un filo
  sutil: los bloques de color saturado cansan y ensucian la lectura en pantallas pequeñas.
- **Accesibilidad**: el estado se marca con **pastilla de color + texto** ("1 vencida"), nunca
  solo con color.
- **Gráficos.** Hay dos y ambos comparten criterios: todo el histórico se ajusta al ancho
  disponible (sin desplazamiento, para que la línea no quede cortada), los datos se separan de
  los bordes con un tope igual a cada lado, y las etiquetas de fecha se van saltando solas
  cuando hay muchos meses para que no se solapen. El peso ideal se dibuja como referencia
  **discontinua**, para que se distinga de los datos medidos.
- **Orientación.** En horizontal la ficha se parte en dos columnas: identidad a la izquierda,
  tarjetas a la derecha. El hueco reservado al banner de publicidad va anclado **abajo**, fuera
  del área de desplazamiento.

---

## 9. Tests

Están en `shared/src/jvmTest` y usan una base de datos SQLite **en memoria** creada desde el
esquema, así que prueban los repositorios y los cálculos de verdad, sin mocks. Cubren el CRUD
de cada entidad, la regla del nombre único, el peso ideal (incluido que `null` no sea `0`) y
los cálculos del resumen de la ficha.

```bash
./gradlew :shared:jvmTest
```

Cuando arregles un fallo, añade el test que lo habría cazado: es la única red de seguridad que
tiene un proyecto sin backend.

---

## 10. Herramientas de desarrollo

En `shared/src/jvmMain/.../tools/` hay utilidades que **no forman parte de la app**:

- `GenerarDatosEjemplo.kt` — crea una base de datos con una mascota de ejemplo y datos
  coherentes (una vacuna vencida, una próxima, un tratamiento activo...) para preparar las
  capturas del README. Se ejecuta con `:shared:generarDatosEjemplo`. Las fechas se calculan a
  partir de hoy, así que el ejemplo siempre muestra el mismo aspecto con el tiempo.

Dos detalles que costaron un rato y conviene no repetir: hay que marcar `PRAGMA user_version`
a mano, o la app intentará crear las tablas que ya existen; y el `last_insert_rowid()` no es
fiable con el driver JDBC, así que el id se lee por nombre.
