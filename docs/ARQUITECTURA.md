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
    │   ├── composeResources/       # Textos por idioma (values/, values-es/, ...)
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

### Identidad de cada fila (`uuid` y `actualizadoEn`)

Desde la versión **14** del esquema, todas las tablas llevan dos columnas más:

- `uuid`: identificador universal de la fila, 32 caracteres hexadecimales en minúscula. El `id`
  es un autonumérico **local**, así que la vacuna 3 de un móvil y la vacuna 3 de otro son cosas
  distintas; el `uuid`, en cambio, no cambia nunca y es lo que permitirá reconocer el mismo
  registro en otro dispositivo al exportar e importar.
- `actualizadoEn`: milisegundos desde epoch de la última modificación. Sirve para decidir qué
  versión es la más nueva cuando la misma fila existe en dos dispositivos.

Los dos los rellenan los repositorios (`util/Identificadores.kt`: `nuevoIdentificadorUniversal`
y `ahoraMillis`), y se pueden pasar a mano porque la importación tendrá que conservar los del
paquete original. La migración `13.sqm` los añade y rellena las filas que ya existían, sin tocar
ningún dato. Los detalles del traspaso están en `docs/TRASPASO_DE_DATOS.md`.

### Copia de seguridad (`.vpet`)

El menú de sistema (icono de ajustes junto a las pestañas) ofrece **Copia de seguridad** y
**Restaurar copia**. El paquete es un ZIP con `manifiesto.json`, `datos.json` y una carpeta
`archivos/` con las fotos y los PDFs; se puede abrir con cualquier descompresor y el JSON se lee a
mano, a propósito.

Tres reglas que no se pueden romper al tocar esto:

1. **Las relaciones van por `uuid`**, nunca por `id`: el id se recalcula al restaurar, así que una
   vacuna que apunte a `mascotaId = 1` podría acabar colgando de otra mascota.
2. **Los adjuntos se referencian por su nombre dentro del ZIP**, no por su ruta en el dispositivo:
   la ruta de un móvil no existe en el ordenador.
3. **La restauración reemplaza la base entera en una sola transacción**, y antes guarda sola una
   copia de lo que había (se conservan las 3 últimas). Los archivos se escriben antes de la
   transacción y se borran si la transacción falla.

En Android el destino que devuelve el selector del sistema es un `content://`, que **no es un
fichero**: por eso el paquete se escribe en la caché y se copia con `copiarArchivoA` (en
streaming). Y el MIME al guardar es `application/octet-stream`, no `application/zip`, porque el
sistema añadiría `.zip` al nombre y el archivo dejaría de reconocerse por su extensión.

### Los drivers aplican las migraciones (importante)

Esto **estuvo roto** y es un fallo fácil de repetir: los drivers se construían con el esquema
pero sin ejecutar nunca `migrate()`, así que la primera migración real habría fallado en el
móvil o borrado datos en escritorio. Ahora se resuelve con `SqlSchemaDelegado`, que envuelve
el esquema y lo entrega al driver; Android además necesita
`AndroidSqliteDriver.Callback(esquema)` para que el `onUpgrade` de SQLite llegue a
`migrate()`. **Si tocas `DatabaseFactory`, no quites ese envoltorio.**

### Cómo saber si una migración funciona

**Con `MigracionesTest`, que ya lo comprueba solo.** Antes había que hacerlo a mano —abrir la
app con una base de datos de la versión anterior— y eso significaba que nadie lo hacía. El test
cubre tres cosas:

1. **Los datos sobreviven desde cualquier versión anterior** (de la 1 a la 13, no solo desde la
   penúltima: quien no actualiza en meses se salta varias de golpe).
2. **El esquema migrado es idéntico al de una instalación nueva.** Es la comprobación que caza el
   despiste clásico: tocar el `.sq` y olvidar el `.sqm` deja dos rutas que acaban en bases de
   datos distintas, y el fallo no aparece hasta mucho después.
3. **Abrir el fichero dos veces seguidas**, que es el escenario real de arrancar la app dos
   veces.

Para probar la primera hace falta reconstruir la versión 1 a mano, porque la tabla `mascotas` es
anterior a los ficheros de migración y ninguno la crea. Está documentado en el propio test.

### `user_version`, el detalle que rompía el escritorio

Ni `create()` ni `migrate()` escriben `PRAGMA user_version` por su cuenta: eso lo hace el driver
de Android, pero **no** el de JDBC. Como `DatabaseFactory` (escritorio) decide qué hacer leyendo
esa marca, el fichero se quedaba en `0` y **cada arranque intentaba crear las tablas otra vez**:
la app de escritorio moría al abrirse por segunda vez con un "table ... already exists". Ahora
`abrirBaseDeDatos()` marca la versión al terminar, tanto al crear como al migrar, y hay un test
que abre el fichero dos veces con conexiones distintas.

Si algún día se toca esto: **marcar la versión no es opcional.**

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
| `rememberCompartidorArchivo` | caché + `FileProvider` + `ACTION_SEND` | guarda en `~/.virtualpet/exportaciones` y lo abre |
| `rememberLlamador` | `ACTION_DIAL` | (sin efecto) |
| `rememberAbridorMapa` | Google Maps | navegador |
| `rememberAgregadorCalendario` | intent de calendario | (sin efecto) |
| `rememberProgramadorNotificaciones` | `AlarmManager` | (sin efecto) |
| `cargarImagenDesdeRuta` | `BitmapFactory` + EXIF | Skia |
| `DatabaseFactory` | `AndroidSqliteDriver` | `JdbcSqliteDriver` |

**Regla:** el `expect` lleva la documentación del contrato (qué garantiza, qué devuelve); el
`actual` explica solo lo específico de su plataforma. Si añades una función de plataforma,
documenta el contrato en el `expect`.

### Compartir el PDF

Compartir **no** es un menú propio: se le entrega el archivo al sistema. En Android el PDF se
escribe en `cacheDir/exportaciones` (una de las rutas declaradas en `file_paths.xml`, o el
`FileProvider` lo deniega) y se lanza un `ACTION_SEND` con `application/pdf`, así que el
selector lo ponen WhatsApp, el correo o Drive. En escritorio no existe esa hoja de compartir: se
guarda en una carpeta propia y se abre con la aplicación por defecto.

El icono que ve el usuario es también cosa de cada plataforma: Android usa el logo **sin texto**
(`mipmap-*/ic_launcher*`, con variante adaptativa) y el instalable de Windows el `.ico`
equivalente, porque en los dos casos el nombre lo pone el sistema al lado. En el PDF, en cambio,
no hay ninguna etiqueta del sistema que diga cómo se llama la app, así que la cabecera usa el
logo completo (`drawable/logo_virtualpet.png`).

### Notificaciones

Se programan con `AlarmManager` y un `BroadcastReceiver`. El id de la notificación es el
**id de la fila**, con un detalle que hay que respetar: vacunas, preventivos y tratamientos son
tablas distintas y todas numeran desde 1, así que cada tipo suma su propio desplazamiento
(`DESPLAZAMIENTO_PREVENTIVO` y el de `RecordatoriosTratamiento`) para no pisar la alarma de otro.
Si añades un cuarto tipo de aviso, dale su rango.

Hay **dos clases de aviso**, y no se comportan igual:

- **Vacunas y preventivos**: un aviso **único**, con días de antelación (15 y 1) a las 9 de la
  mañana. Lo calcula `ReprogramadorNotificaciones` y se programa una sola vez.
- **Tratamientos**: un recordatorio **diario** a la hora que elija el usuario, mientras dure el
  tratamiento. Aquí una alarma no basta: cuando salta, `NotificacionReceiver` **programa la del
  día siguiente** con los datos que lleva el propio intent (hora y fecha de fin), porque en ese
  momento la app puede estar cerrada. La regla de "cuándo toca" está en
  `util/RecordatoriosTratamiento.kt`, con tests, y la usan los dos extremos: quien programa y
  quien encadena.

Las notificaciones se **reprograman al abrir la ficha de una mascota** (cancelar y volver a
programar: así un tratamiento borrado o terminado pierde su aviso sin código aparte), **al
arrancar la app** y, **al reiniciar el móvil o actualizar la app**: Android borra las alarmas al
apagar, y sin el receptor de `BOOT_COMPLETED` (`ReinicioReceiver`) los recordatorios dejarían de
sonar para siempre. Ese receptor reprograma **los tres tipos** de aviso
(`util/RecordatoriosAndroid.kt`); hasta la 1.3 solo reprogramaba tratamientos, y el hueco se
encontró con una prueba real: cinco alarmas antes de reiniciar, una sola después.

#### El aviso de un tratamiento se puede aceptar o posponer

Un aviso de dosis es información; un aviso de medicina es una **tarea pendiente**. Por eso el de
tratamiento trae tres cosas que el de una vacuna no tiene:

- **Tocar la notificación** abre la app en la ficha de su mascota y en la hoja de **Salud y
  seguimiento → Tratamientos** (`SolicitudNavegacion` hasta `App`, con `MainActivity` en
  `singleTop` para no apilar pantallas: si la app ya estaba abierta, el aviso entra por
  `onNewIntent` y se navega sin reiniciar).
- **Aceptar**: cierra el aviso. No guarda nada, porque la dosis no se registra en ningún sitio; el
  del día siguiente ya está programado desde que sonó este.
- **Posponer 30 min**: cierra el aviso y lo vuelve a programar. No hay límite de veces, y
  posponer **no puede perder** el aviso de mañana: la posposición usa el mismo id de alarma y, al
  sonar, el receptor vuelve a encadenar el día siguiente.
- **La posposición se guarda en disco** (`util/Posposiciones.kt`, un fichero de texto en la carpeta
  de datos). Sin eso solo vivía en `AlarmManager`, y como Android vacía las alarmas al reiniciar,
  el aviso pospuesto se perdía hasta el día siguiente —lo encontró una prueba del usuario: aviso
  pospuesto, reinicio, y nada hasta mañana—. Ahora, al reprogramar un tratamiento se mira primero
  si hay una posposición viva y se usa esa hora; se borra cuando el aviso vuelve a salir o cuando
  se acepta. Si el tratamiento ya se acabó (la
  posposición caería después de su fecha de fin), no se programa nada.

Los dos botones los atiende `AccionRecordatorioReceiver`, aparte del que pinta el aviso, para que
un fallo al posponer no pueda impedir el encadenado de mañana. Todo lo que hace falta para
pintarlo y reprogramarlo viaja dentro del intent de la alarma (`AvisoTratamiento`), porque cuando
salta la app puede estar cerrada: incluidos los dos textos de los botones, que se traducen al
programar (ahí sí hay corrutina y recursos) y no al mostrar.

No es un despertador: se descartó a propósito pedir `USE_FULL_SCREEN_INTENT` (en Android 14+ está
reservado a apps de alarma y llamadas) y sonar en bucle con un servicio en primer plano. El aviso
es un **banner con sonido y vibración** del canal `recordatorios_tratamientos`
(`IMPORTANCE_HIGH`), y su categoría es `reminder`, no `alarm`.

#### El permiso de alarmas exactas: el fallo más silencioso

`AlarmManager.setExactAndAllowWhileIdle` necesita `SCHEDULE_EXACT_ALARM`. Si no está concedido, el
código cae a `set()`, que **no falla**: programa la alarma con una ventana (hasta una hora) y con la
app cerrada y el móvil en reposo puede aplazarla mucho más. El aviso acaba llegando tarde o, desde
el punto de vista del usuario, **no llegando**.

Ese permiso **se pierde al reinstalar la app** (los *appops* se borran con la instalación) y
`AlarmManager` no avisa a la app de que ha cambiado. De ahí tres cosas que no se pueden quitar:

1. Al encender *Recordarme* en un tratamiento se pide (`ComprobadorPermisoExacto.pedirPermiso`),
   junto al de notificaciones.
2. Si falta, el menú de sistema lo avisa con "Los avisos pueden llegar tarde".
3. **El menú reprograma los avisos al abrirse**: es lo que hace que, al volver de concederlo en
   Ajustes, las alarmas pasen a ser exactas sin reiniciar la app. Se hizo así porque
   `WindowInfo.isWindowFocused` no resultó fiable para detectar la vuelta de otra app.

No hay servicio en segundo plano: es un equilibrio consciente entre simplicidad y precisión.

---

## 8. Interfaz: decisiones que conviene conocer

- **Ficha como panel.** La cabecera lleva foto, nombre, edad y un menú ⋮ con las acciones.
  Debajo, tarjetas por bloque. Cada tarjeta muestra **un solo dato relevante**, no un recuento:
  el objetivo es que un vistazo diga si hay algo que requiera atención.
- **La cabecera tiene un solo botón flotante: volver.** Todo lo demás que se puede hacer con la
  mascota (editar sus datos, compartir el perfil) vive en el menú ⋮, junto al nombre. Antes
  compartir era un segundo icono suelto en la esquina opuesta y no se leía como una acción sobre
  la mascota, sino como un adorno de la pantalla. El botón de volver es un círculo con fondo,
  filo y sombra: con el icono solo se perdía entre la marca de agua y las tarjetas.
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
- **El filtro del historial y la serie del gráfico viven en `util` (`FiltrosPeso.kt` y
  `SeriesPeso.kt`), con test.** Filtros: **6 meses** (el de por defecto, para no cargar todo el
  historial de golpe), año, **mes actual** y rango de fechas. Que "mes actual" compare **año y
  mes** —y no solo el mes— es justo lo que más fácil se rompe en silencio: si no, octubre de este
  año arrastraría el octubre del anterior. Y que "6 meses" cuente **desde hoy** y no desde el
  último pesaje también: estirar la ventana hasta el último dato escondería que hace meses que no
  se pesa.
- **La granularidad la decide el filtro, no los datos.** Ver `presentacionDe`. Agrupar por meses
  (media de cada mes, con los meses sin datos como huecos) es lo correcto para un año entero, que
  con un punto por pesaje sería un amasijo; el resto de filtros enseñan **cada pesaje**, que es el
  detalle que se quiere ver. El eje se rotula con meses o con días según el caso: días cuando todo
  cae en un mes (el número sitúa el punto) y meses cuando el rango cruza.
- **El gráfico se arrastra en horizontal solo cuando no cabe.** `anchoDelGrafico` mide lo que
  necesita la serie con `SEPARACION_MINIMA_PUNTO` (14 dp) de aire y **nunca menos que la
  pantalla**: con pocos puntos se ajusta al ancho como siempre y no hay desplazamiento; con
  muchos el lienzo se ensancha y se puede arrastrar, arrancando por el final (el pesaje más
  reciente) para no tener que buscarlo.
- **El eje del peso tiene un único cálculo, en `util/EscalaPeso.kt`.** El rango incluye **el
  peso ideal además de los pesajes** y deja un **10 % de margen** (`MARGEN_RELATIVO_ESCALA`) por
  encima del máximo y por debajo del mínimo. El margen es *relativo* a propósito: con una
  cantidad fija (medio kilo, que es lo que hubo antes), un perro de 26 kg se veía bien pero un
  conejo de 1,6 kg salía como una raya aplastada en mitad del gráfico. Con el 10 %, los dos
  ocupan el mismo 83 % del alto. `EscalaPeso` recibe y devuelve kilos, y es ella la que convierte
  a la unidad mostrada: fuera de ahí no se manejan valores "en libras", que es como un punto
  acaba en el sitio equivocado sin que nada avise.
- **Los dos gráficos comparten márgenes y adornos (`ui/GraficoPeso.kt`).** `MARGENES_GRAFICO_PESO`
  son los 16 dp laterales y 10 dp de arriba y abajo **del área de dibujo** (no del contenedor):
  la línea flota dentro del recuadro. El degradado bajo la curva y la curva suave salen de ahí
  también, para que la tarjeta de la ficha y la hoja de peso no se vean distintas.
- **El suavizado es un bezier de puntos medios, no un Catmull-Rom.** Los puntos de control
  comparten la `y` de los extremos, así que la curva **no puede sobrepasar** el valor máximo ni
  el mínimo del tramo. Un suavizado que sobrepase se saldría justo por el margen que estamos
  protegiendo.
- **`posicionesX` reparte los puntos por igual dentro del área de dibujo, y tiene test.** Antes
  el sparkline los separaba a un sexto del ancho y "recortaba" por la izquierda lo que sobraba:
  con siete pesajes no se notaba, pero a partir de ocho la línea arrancaba fuera de la pantalla.
- **Orientación.** En horizontal la ficha se parte en dos columnas: identidad a la izquierda,
  tarjetas a la derecha. El hueco reservado al banner de publicidad va anclado **abajo**, fuera
  del área de desplazamiento.
- **Textos largos (alemán).** Las filas de pastillas de estado y de chips de filtro usan
  `FlowRow`, no `Row`: en alemán las etiquetas son bastante más largas y en una sola línea se
  salían de la pantalla. Antes de dar por buena una fila nueva, comprueba que aguanta el alemán.
- **Un fallo nunca se calla.** Toda operación de escritura de la ficha pasa por
  `cerrarOperacion(...)`: si el repositorio devuelve `Result.failure`, se publica el error en
  `errorMessage` y **no** se recarga como si hubiera ido bien. Antes se tiraba el `Result`, así
  que guardar una vacuna podía fallar y la pantalla quedaba igual: el usuario creía que se había
  guardado. Además, los scopes de los ViewModels usan `SupervisorJob` y las lecturas van en
  `try`/`catch`: con un `Job` normal, una excepción sin capturar cancelaba el scope y **todas**
  las operaciones siguientes se volvían invisibles —la ficha dejaba de guardar nada y no lo
  decía—. `FichaMascotaErroresTest` reproduce justo eso provocando el fallo con una base de
  datos cerrada.
- **Los recálculos del resumen van de uno en uno.** `FichaMascotaViewModel.cargarResumen`
  encadena una recarga tras cada escritura, y sin serializar se cruzaban: una recarga leía la
  base de datos, otra leía y escribía ya con el dato nuevo, y la primera terminaba después
  pisando el resultado bueno con el viejo. Se veía como una nota recién guardada que no aparecía
  en la ficha, y hacía fallar un test una de cada cuatro veces. El cerrojo (`mutexResumen`) cubre
  **lectura y escritura**, no solo la escritura: así el último recálculo en ejecutarse es también
  el que ha leído los datos más recientes.

### El PDF: el contenido se decide aparte de cómo se pinta

La ficha exportada son **dos documentos distintos**, no el mismo recortado:

- **Completa**, para el veterinario: identificación, tratamientos en curso **con la dosis**,
  historial de tratamientos, vacunas y preventivos (con lote, veterinario y **estado en color**),
  visitas con diagnóstico y notas, y un **resumen de peso** (actual, primero, mínimo, máximo e
  ideal) con el recuento de pesajes.
- **Resumida**, para quien cuida a la mascota: peso actual e ideal, qué hay que darle con su
  dosis, cada cuánto y hasta cuándo, las próximas dosis (vacunas y preventivos mezclados y
  ordenados por fecha) y las notas a tener en cuenta.

Tres decisiones que conviene no deshacer sin querer:

1. **Una sección sin datos no se pinta** (ni el título ni un "no hay datos"). Un apartado vacío
   gasta papel y hace dudar de si el dato no está o se ha perdido. La única excepción es la hoja
   del cuidador cuando no hay nada que dar ni nada programado: ahí "no hay nada" **es** la
   respuesta, y se dice una vez en lugar de dejar la hoja en blanco.
2. **El histórico de peso no se vuelca.** Estaba, y el PDF salía de varias páginas por unos
   números que en papel nadie lee. Queda el resumen y el recuento ("Pesajes registrados: N.
   Consulte la aplicación…"), que es lo que se consulta de un vistazo. Por lo mismo, los informes
   adjuntos ya no se listan: el PDF no puede enseñar su contenido, así que solo podía enseñar el
   nombre del archivo. Las notas de la app se quedan en la hoja del cuidador, que es quien las usa.
3. **El estado de una dosis se ve, no se lee.** Vacunas y preventivos no son tablas sino una línea
   por registro con una pastilla de color (verde al día, ámbar vence pronto, rojo vencida), porque
   una celda de tabla es solo texto con un estilo común para todas. Los plazos y los colores salen
   de `util/EstadoDosis.kt` y del tema, **los mismos que usa la pantalla**: si el papel dijera "al
   día" donde la app dice "vence pronto", uno de los dos mentiría.

`ContenidoPdf.kt` decide **qué se escribe** (etiqueta y valor de cada fila) y devuelve cadenas;
`ExportadorPdf.kt` solo pinta tablas y recuadros con la librería `compose-pdf`. Esa separación
existe porque el motor del PDF corre en una corrutina, donde no hay composición ni
`stringResource`: los rótulos se leen antes, con `getString`, y se pasan ya resueltos. Gracias a
ella, lo que de verdad puede estar mal —que falte la dosis, que un tratamiento terminado salga
como activo, que una vacuna vencida se pinte como al día— se comprueba con tests
(`ContenidoPdfTest`) sin abrir un PDF, y `ExportadorPdfTest` va un paso más allá: recorre el
documento con `previewPages` y comprueba los textos y los colores de las pastillas.

---

## 9. Idiomas, unidades y formato de fecha

Está todo en `shared/src/commonMain/composeResources/` y en `util/Localizacion.kt`. La idea es
que **el idioma de los textos lo resuelve Compose solo** y que las decisiones que dependen de la
región (unidades, orden de fecha, separador decimal) estén en **un único sitio**.

### Textos

`values/` es el idioma **de respaldo** de Compose y contiene el inglés; el español, que es el
idioma original de la app, está en `values-es/`. Los seis ficheros tienen las mismas claves en
el mismo orden, y `ParidadTraduccionesTest` lo verifica, porque un idioma al que le falte una
clave no falla al compilar: se ve en inglés y nadie se entera.

Dos comportamientos de la librería que conviene tener presentes (están fijados con tests en
`ComportamientoRecursosTest`, no son suposiciones):

- `stringResource(recurso, args…)` **sí** sustituye los marcadores `%1$s`.
- `pluralStringResource(recurso, cantidad, args…)` **no** sustituye nada: solo elige la forma.
  Por eso los plurales **no** llevan el número dentro y el número se compone en Kotlin
  (`"$n " + pluralStringResource(...)`).

`stringResource` solo se puede llamar desde un contexto composable, así que **todo texto que se
use dentro de un `onClick`, un `onCalendar` o un `DrawScope` se resuelve antes**, en el cuerpo
del composable, y se captura en la lambda. Es el error que más veces ha aparecido al traducir.

### Región

`Localizacion` es la única fuente de verdad y se publica por composición con `ConLocalizacion`
desde `App.kt`. Sus reglas:

| Qué | Regla |
| --- | --- |
| Peso | Se guarda **siempre en kilos**; se muestra en libras solo en `en-US`. `UnidadPeso.aKilos` / `desdeKilos` convierten al leer y al escribir el campo. |
| Fecha | Orden mes/día solo en inglés; el resto, día/mes. `parseFechaLocalizada` acepta el orden de la región y, como respaldo, el otro. |
| Decimal | Coma en español y portugués; punto en el resto. |

### Valores guardados en la base de datos

Especie, raza, sexo y tipo de preventivo se guardan **en español** y se traducen **solo al
mostrarlos**, con `TraduccionesDatos`. Es deliberado: el formulario de mascota comprueba
`mascota.raza in razasDeEstaEspecie` para rellenar el desplegable, así que traducir lo guardado
dejaría sin raza a las mascotas que ya existen. **Nunca traduzcas un valor antes de escribirlo.**

Los motivos de error que devuelve la capa de datos también viajan sin texto
(`ErrorAppException` con un `ErrorApp`), y el ViewModel los convierte a texto con
`textoErrorApp`, que es `suspend` porque lee recursos fuera de la composición.

⚠️ **Alemán, francés, portugués e italiano están sin revisión nativa.** Repásalos antes de
publicar, sobre todo los nombres de raza.

---

## 10. Tests

Están en `shared/src/jvmTest` y usan una base de datos SQLite **en memoria** creada desde el
esquema, así que prueban los repositorios y los cálculos de verdad, sin mocks. Cubren el CRUD
de cada entidad, la regla del nombre único, el peso ideal (incluido que `null` no sea `0`), los
cálculos del resumen de la ficha, las **migraciones de esquema**, los **errores de la ficha**
(provocados con una base de datos cerrada) y la internacionalización (paridad entre los seis
idiomas, resolución real del idioma en ejecución y comportamiento de los plurales).

```bash
./gradlew :shared:jvmTest
```

Cuando arregles un fallo, añade el test que lo habría cazado: es la única red de seguridad que
tiene un proyecto sin backend.

---

## 11. Herramientas de desarrollo

En `shared/src/jvmMain/.../tools/` hay utilidades que **no forman parte de la app**:

- `GenerarDatosEjemplo.kt` — crea una base de datos con una mascota de ejemplo y datos
  coherentes (una vacuna vencida, una próxima, un tratamiento activo...) para preparar las
  capturas del README. Se ejecuta con `:shared:generarDatosEjemplo`. Las fechas se calculan a
  partir de hoy, así que el ejemplo siempre muestra el mismo aspecto con el tiempo.

Dos detalles que costaron un rato y conviene no repetir: hay que marcar `PRAGMA user_version`
a mano, o la app intentará crear las tablas que ya existen; y el `last_insert_rowid()` no es
fiable con el driver JDBC, así que el id se lee por nombre.
