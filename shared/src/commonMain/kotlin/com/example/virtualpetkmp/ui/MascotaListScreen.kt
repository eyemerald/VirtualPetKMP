package com.example.virtualpetkmp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.virtualpetkmp.Mascota
import com.example.virtualpetkmp.ui.theme.LocalExtrasColors
import com.example.virtualpetkmp.util.PreferenciaVista
import com.example.virtualpetkmp.util.TraduccionesDatos
import com.example.virtualpetkmp.util.localizacion
import com.example.virtualpetkmp.viewmodel.MascotaViewModel
import org.jetbrains.compose.resources.stringResource
import virtualpetkmp.shared.generated.resources.Res
import virtualpetkmp.shared.generated.resources.a11y_agregar_mascota
import virtualpetkmp.shared.generated.resources.a11y_ver_como_listado
import virtualpetkmp.shared.generated.resources.a11y_ver_como_tarjetas
import virtualpetkmp.shared.generated.resources.accion_cancelar
import virtualpetkmp.shared.generated.resources.accion_eliminar
import virtualpetkmp.shared.generated.resources.borrar_mascota_mensaje
import virtualpetkmp.shared.generated.resources.borrar_mascota_mensaje_sin_nombre
import virtualpetkmp.shared.generated.resources.borrar_mascota_titulo
import virtualpetkmp.shared.generated.resources.filtro_vacunas_pendientes
import virtualpetkmp.shared.generated.resources.filtro_preventivos_pendientes
import virtualpetkmp.shared.generated.resources.filtro_vencida
import virtualpetkmp.shared.generated.resources.filtro_proxima
import virtualpetkmp.shared.generated.resources.filtro_sin_resultados
import com.example.virtualpetkmp.util.DosisPendiente
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import virtualpetkmp.shared.generated.resources.buscar_mascota
import virtualpetkmp.shared.generated.resources.buscar_mascota_sin_resultados
import virtualpetkmp.shared.generated.resources.lista_mascotas_reintentar
import virtualpetkmp.shared.generated.resources.lista_mascotas_vacia
import virtualpetkmp.shared.generated.resources.lista_mascotas_vacia_ayuda
import virtualpetkmp.shared.generated.resources.lista_peso_ideal

/** Cómo se dibuja la lista de mascotas. */
enum class ModoLista {
    /** Tarjeta grande con foto: la vista de siempre, buena con pocas mascotas. */
    TARJETAS,

    /** Fila compacta: mucha más información por pantalla. Es la que sirve con 40 animales. */
    LISTADO
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MascotaListScreen(
    viewModel: MascotaViewModel,
    onMascotaClick: (Long) -> Unit,
    onAddMascota: () -> Unit
) {
    val mascotas by viewModel.mascotas.collectAsState()
    val ultimoPesoPorMascota by viewModel.ultimoPesoPorMascota.collectAsState()
    val vacunasPendientes by viewModel.vacunasPendientes.collectAsState()
    val preventivosPendientes by viewModel.preventivosPendientes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    var showDeleteDialog by rememberSaveable { mutableStateOf<Long?>(null) }
    var nombreAEliminar by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        // Sin TopAppBar: el título se dibuja dentro de la lista, pegado a las pestañas.
        // El Scaffold se mantiene por el botón flotante y por el hueco del banner.
        //
        // contentWindowInsets = 0 a propósito: esta pantalla se dibuja DENTRO del Scaffold
        // de MainScreen, que ya aplica los insets del sistema. Si el Scaffold de aquí
        // volviera a aplicarlos, se sumarían y aparecería un hueco muerto arriba.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(onClick = onAddMascota) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(Res.string.a11y_agregar_mascota)
                )
            }
        },
        // Hueco FIJO del banner de publicidad: el Scaffold lo coloca fuera del área de
        // contenido, así que queda anclado al fondo y la lista se desplaza por encima.
        // El inset de la barra de navegación lo añade el propio Scaffold.
        bottomBar = {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ALTO_RESERVA_BANNER)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = {
                            viewModel.clearError()
                            viewModel.loadMascotas()
                        }) {
                            Text(stringResource(Res.string.lista_mascotas_reintentar))
                        }
                    }
                }
                mascotas.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(Res.string.lista_mascotas_vacia),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(Res.string.lista_mascotas_vacia_ayuda),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                else -> {
                    // Buscador y selector de vista, y debajo la lista. Van fuera del LazyColumn
                    // para que no se desplacen: con muchas mascotas, el buscador tiene que estar
                    // siempre a mano.
                    var consulta by rememberSaveable { mutableStateOf("") }
                    // Los dos filtros son independientes y se combinan con la busqueda: "las que
                    // tienen la vacuna pendiente y se llaman Luna" es una pregunta legitima.
                    var filtroVacunas by rememberSaveable { mutableStateOf(false) }
                    var filtroPreventivos by rememberSaveable { mutableStateOf(false) }
                    // La vista elegida se recuerda entre arranques (no solo al girar la pantalla):
                    // quien pone el listado con muchos animales no quiere volver a ponerlo cada vez.
                    var modo by rememberSaveable {
                        mutableStateOf(
                            if (PreferenciaVista.leerListado()) ModoLista.LISTADO else ModoLista.TARJETAS
                        )
                    }

                    // Las cuentas de cada filtro se hacen sobre la busqueda actual: si el
                    // usuario ha escrito "luna", "3 vacunas pendientes" no puede referirse a las
                    // mascotas que no se estan viendo.
                    val buscadas = remember(mascotas, consulta) { filtrarMascotas(mascotas, consulta) }
                    val cuantasVacunas = remember(buscadas, vacunasPendientes) {
                        buscadas.count { it.id != null && vacunasPendientes.containsKey(it.id) }
                    }
                    val cuantosPreventivos = remember(buscadas, preventivosPendientes) {
                        buscadas.count { it.id != null && preventivosPendientes.containsKey(it.id) }
                    }
                    val visibles = remember(buscadas, filtroVacunas, filtroPreventivos, vacunasPendientes, preventivosPendientes) {
                        buscadas.filter { mascota ->
                            val id = mascota.id
                            val vacunaOk = !filtroVacunas || (id != null && vacunasPendientes.containsKey(id))
                            val preventivoOk = !filtroPreventivos || (id != null && preventivosPendientes.containsKey(id))
                            vacunaOk && preventivoOk
                        }
                    }
                    val hayFiltro = filtroVacunas || filtroPreventivos

                    Column(modifier = Modifier.fillMaxSize()) {
                        BarraBusqueda(
                            consulta = consulta,
                            onConsulta = { consulta = it },
                            modo = modo,
                            onCambiarModo = { nuevo ->
                                modo = nuevo
                                PreferenciaVista.guardarListado(nuevo == ModoLista.LISTADO)
                            }
                        )

                        FiltrosPendientes(
                            filtroVacunas = filtroVacunas,
                            cuantasVacunas = cuantasVacunas,
                            filtroPreventivos = filtroPreventivos,
                            cuantosPreventivos = cuantosPreventivos,
                            onVacunas = { filtroVacunas = it },
                            onPreventivos = { filtroPreventivos = it }
                        )

                        if (visibles.isEmpty()) {
                            // Solo puede pasar buscando: sin búsqueda, la lista vacía la cubre el
                            // caso de arriba ("no hay mascotas registradas").
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    // Con filtros puestos, "no hay resultados" tiene que decir
                                    // que es por el filtro; sin ellos, la busqueda vacia se
                                    // explica sola.
                                    text = stringResource(
                                        if (hayFiltro) Res.string.filtro_sin_resultados
                                        else Res.string.buscar_mascota_sin_resultados
                                    ),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                // Sin título dentro: ya lo pone la barra superior global, así que
                                // la lista empieza directamente con las mascotas. El hueco del
                                // banner va aparte (bottomBar), para que no se desplace con el scroll.
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    top = 8.dp,
                                    end = 16.dp,
                                    bottom = 8.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(visibles, key = { it.id ?: 0 }) { mascota ->
                                    // Tipos explícitos: `mascota.id?.let { ... }` devuelve Unit? y
                                    // los parámetros que vienen de la tarjeta piden Unit.
                                    val alPulsar: () -> Unit = { mascota.id?.let { onMascotaClick(it) } }
                                    val alBorrar: () -> Unit = {
                                        nombreAEliminar = mascota.nombre
                                        mascota.id?.let { showDeleteDialog = it }
                                    }

                                    // Con un filtro puesto se dice QUÉ está pendiente y para
                                    // cuándo: "Rabia · vencida el 12/03/2026". Sin filtro la lista
                                    // se queda como estaba.
                                    val avisos = if (hayFiltro) {
                                        avisosDe(mascota, filtroVacunas, filtroPreventivos, vacunasPendientes, preventivosPendientes)
                                    } else {
                                        emptyList()
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    when (modo) {
                                        ModoLista.TARJETAS -> MascotaCard(
                                            mascota = mascota,
                                            ultimoPeso = mascota.id?.let { ultimoPesoPorMascota[it] },
                                            onClick = alPulsar,
                                            onEdit = alPulsar,
                                            onDelete = alBorrar
                                        )

                                        ModoLista.LISTADO -> MascotaFila(
                                            mascota = mascota,
                                            ultimoPeso = mascota.id?.let { ultimoPesoPorMascota[it] },
                                            onClick = alPulsar,
                                            onEdit = alPulsar,
                                            onDelete = alBorrar
                                        )
                                    }
                                    if (avisos.isNotEmpty()) {
                                        Text(
                                            text = avisos.joinToString(" · "),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                                            modifier = Modifier.padding(horizontal = 12.dp)
                                        )
                                    }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Diálogo de confirmación de eliminación
    showDeleteDialog?.let { id ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text(stringResource(Res.string.borrar_mascota_titulo)) },
            text = {
                Text(
                    nombreAEliminar?.let {
                        stringResource(Res.string.borrar_mascota_mensaje, it)
                    } ?: stringResource(Res.string.borrar_mascota_mensaje_sin_nombre)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteMascota(id) {
                            showDeleteDialog = null
                            nombreAEliminar = null
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(Res.string.accion_eliminar))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = null
                    nombreAEliminar = null
                }) {
                    Text(stringResource(Res.string.accion_cancelar))
                }
            }
        )
    }
}

/**
 * Buscador por nombre + selector de vista (tarjetas o listado).
 *
 * El filtro es **por nombre** y no distingue mayúsculas ni acentos: en una protectora se busca
 * "pepe" escribiendo rápido y esperando encontrar a "Pepé". Los acentos se quitan de los dos lados
 * de la comparación, así que también funciona al revés.
 */
@Composable
private fun BarraBusqueda(
    consulta: String,
    onConsulta: (String) -> Unit,
    modo: ModoLista,
    onCambiarModo: (ModoLista) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = consulta,
            onValueChange = onConsulta,
            modifier = Modifier.weight(1f),
            placeholder = { Text(stringResource(Res.string.buscar_mascota)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (consulta.isNotEmpty()) {
                    IconButton(onClick = { onConsulta("") }) {
                        Icon(Icons.Default.Clear, contentDescription = null)
                    }
                }
            },
            singleLine = true,
            // En escritorio no hay teclado que cerrar; en el móvil, "buscar" lo esconde.
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { })
        )

        // Un solo botón que alterna: muestra el icono de **la otra** vista, que es lo que se va a
        // conseguir al pulsarlo.
        IconButton(
            onClick = {
                onCambiarModo(
                    if (modo == ModoLista.TARJETAS) ModoLista.LISTADO else ModoLista.TARJETAS
                )
            }
        ) {
            Icon(
                imageVector = if (modo == ModoLista.TARJETAS) Icons.Default.List else Icons.Default.GridView,
                contentDescription = stringResource(
                    if (modo == ModoLista.TARJETAS) {
                        Res.string.a11y_ver_como_listado
                    } else {
                        Res.string.a11y_ver_como_tarjetas
                    }
                )
            )
        }
    }
}

/**
 * Filtra por nombre, sin distinguir mayúsculas ni acentos.
 *
 * Devuelve la lista tal cual si la búsqueda está vacía: así escribir y borrar no reordena ni
 * parpadea nada.
 */
internal fun filtrarMascotas(mascotas: List<Mascota>, consulta: String): List<Mascota> {
    val buscado = sinAcentos(consulta.trim())
    if (buscado.isEmpty()) return mascotas
    return mascotas.filter { sinAcentos(it.nombre).contains(buscado) }
}

/**
 * Texto en minúsculas y sin acentos, para comparar.
 *
 * Se hace a mano y no con `java.text.Normalizer` porque este código es compartido con Android,
 * donde `Normalizer` existe pero se comporta distinto según la versión de la API. Para nombres de
 * mascotas basta con las vocales y la eñe del español más las vocales acentuadas del resto de
 * idiomas de la app.
 */
internal fun sinAcentos(texto: String): String = buildString(texto.length) {
    texto.lowercase().forEach { letra ->
        append(
            when (letra) {
                'á', 'à', 'ä', 'â', 'ã', 'å' -> 'a'
                'é', 'è', 'ë', 'ê' -> 'e'
                'í', 'ì', 'ï', 'î' -> 'i'
                'ó', 'ò', 'ö', 'ô', 'õ' -> 'o'
                'ú', 'ù', 'ü', 'û' -> 'u'
                'ñ' -> 'n'
                'ç' -> 'c'
                else -> letra
            }
        )
    }
}

/**
 * Una mascota en la vista de listado: una fila de ~56 dp en vez de una tarjeta de ~120.
 *
 * Con 40 animales metidos, la tarjeta grande obliga a desplazarse mucho; aquí caben el doble y la
 * foto sigue estando, más pequeña, para reconocer de un vistazo.
 */
@Composable
private fun MascotaFila(
    mascota: Mascota,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    ultimoPeso: Double? = null
) {
    val formato = localizacion

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = LocalExtrasColors.current.tarjetaFondo,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarMascota(nombre = mascota.nombre, rutaFoto = mascota.foto, tamano = 36.dp)

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mascota.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        append(
                            TraduccionesDatos.especieYRaza(
                                mascota.especie,
                                mascota.raza,
                                localizacion.idioma
                            )
                        )
                        val ideal = mascota.pesoIdeal
                        if (ideal != null && ultimoPeso != null) {
                            append(" · ")
                            append(formato.formatearPeso(ultimoPeso))
                            append(' ')
                            append(formato.unidadPesoSufijo)
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            MenuAccionesItem(nombre = mascota.nombre, onEditar = onEdit, onBorrar = onDelete)
        }
    }
}

/**
 * Tarjeta de una mascota en la lista: avatar, identidad y, si la mascota tiene peso ideal y
 * algún pesaje, el último peso con un indicador ▲/▼ de si está por encima o por debajo.
 *
 * Las acciones (editar / eliminar) van en un único menú de tres puntos para que la tarjeta
 * quede limpia y el nombre tenga más sitio.
 */
@Composable
fun MascotaCard(
    mascota: Mascota,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    ultimoPeso: Double? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = LocalExtrasColors.current.tarjetaFondo
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Foto de la mascota (o su inicial si aún no tiene): el mismo avatar que en la
            // ficha, en pequeño.
            AvatarMascota(
                nombre = mascota.nombre,
                rutaFoto = mascota.foto,
                tamano = 56.dp
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Un punto más grande que antes: al quitar los dos iconos de acción sobra
                // sitio horizontal y el nombre gana protagonismo.
                Text(
                    text = mascota.nombre,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    // Especie y raza se guardan en español en la base de datos; se traducen
                    // solo aquí, al mostrarlas.
                    text = TraduccionesDatos.especieYRaza(
                        mascota.especie,
                        mascota.raza,
                        localizacion.idioma
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = TraduccionesDatos.etiqueta(mascota.sexo, localizacion.idioma),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )

                // Solo se muestra si hay peso ideal configurado Y algún pesaje: sin uno de
                // los dos datos, un triángulo no significaría nada.
                val ideal = mascota.pesoIdeal
                if (ideal != null && ultimoPeso != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    IndicadorPeso(
                        ultimoPeso = ultimoPeso,
                        pesoIdeal = ideal
                    )
                }
            }

            // Mismo menú que en el resto de la app (ver MenuAcciones.kt): este fue el primero y
            // ahora todos comparten implementación, así no se separan con el tiempo.
            MenuAccionesItem(
                nombre = mascota.nombre,
                onEditar = onEdit,
                onBorrar = onDelete
            )
        }
    }
}

/**
 * Último peso junto a su peso ideal: ▲ verde si está por encima, ▼ rojo si está por debajo.
 * Si coinciden (margen de 10 gramos), no se dibuja flecha: no hay nada que indicar.
 *
 * El peso se muestra en la unidad de la región (kg o lb), pero la comparación con el ideal se
 * hace **siempre en kilos**, que es como está guardado: convertir los dos valores daría el
 * mismo resultado pero acumularía error de redondeo.
 */
@Composable
private fun IndicadorPeso(
    ultimoPeso: Double,
    pesoIdeal: Double
) {
    val formato = localizacion
    val diferencia = ultimoPeso - pesoIdeal
    val hayDiferencia = kotlin.math.abs(diferencia) > 0.01

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "${formato.formatearPeso(ultimoPeso)} ${formato.unidadPesoSufijo}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
        )
        if (hayDiferencia) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (diferencia > 0) "▲" else "▼",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (diferencia > 0) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = stringResource(
                Res.string.lista_peso_ideal,
                formato.formatearPeso(pesoIdeal),
                formato.unidadPesoSufijo
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Los dos filtros de pendientes de la lista, con cuántas mascotas hay en cada uno.
 *
 * Van como chips y no como un desplegable porque son dos estados que se ven de un vistazo: quien
 * entra a revisar quiere saber si hay algo pendiente **sin** tener que abrir nada.
 *
 * Los dos chips **se reparten la fila a partes iguales** (`weight`) y su texto va en una sola línea y
 * con la letra un punto más pequeña. Antes cada chip medía lo que pedía su texto, así que, al
 * traducirlo, el de la derecha se partía en dos líneas y quedaba más estrecho y más alto que el de
 * la izquierda: la fila se descuadraba. Con el reparto y una sola línea, los dos cuadros son siempre
 * iguales, y la etiqueta corta (sin "vencidas o en 30 días") cabe en los seis idiomas.
 */
@Composable
private fun FiltrosPendientes(
    filtroVacunas: Boolean,
    cuantasVacunas: Int,
    filtroPreventivos: Boolean,
    cuantosPreventivos: Int,
    onVacunas: (Boolean) -> Unit,
    onPreventivos: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        FilterChip(
            selected = filtroVacunas,
            onClick = { onVacunas(!filtroVacunas) },
            modifier = Modifier.weight(1f),
            label = {
                Text(
                    text = "${stringResource(Res.string.filtro_vacunas_pendientes)} ($cuantasVacunas)",
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        )
        FilterChip(
            selected = filtroPreventivos,
            onClick = { onPreventivos(!filtroPreventivos) },
            modifier = Modifier.weight(1f),
            label = {
                Text(
                    text = "${stringResource(Res.string.filtro_preventivos_pendientes)} ($cuantosPreventivos)",
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        )
    }
}

/**
 * Lo que hay que enseñar bajo una mascota cuando hay un filtro puesto: **qué** dosis está pendiente
 * y **para cuándo** ("Rabia · vencida el 12/03/2026").
 *
 * Devuelve los textos ya montados porque es la pantalla la que tiene los recursos de idioma; así la
 * lógica de qué está pendiente vive en `Pendientes`/`Dosis`, que sí se prueba sin interfaz.
 */
@Composable
private fun avisosDe(
    mascota: com.example.virtualpetkmp.Mascota,
    filtroVacunas: Boolean,
    filtroPreventivos: Boolean,
    vacunasPendientes: Map<Long, List<DosisPendiente>>,
    preventivosPendientes: Map<Long, List<DosisPendiente>>
): List<String> {
    val id = mascota.id ?: return emptyList()
    val hoy = Clock.System.todayIn(TimeZone.currentSystemDefault())
    val formato = localizacion

    val dosis = buildList {
        if (filtroVacunas) addAll(vacunasPendientes[id].orEmpty())
        if (filtroPreventivos) addAll(preventivosPendientes[id].orEmpty())
    }

    return dosis.map { pendiente ->
        val cuando = formato.formatearFecha(pendiente.fecha)
        val texto = if (pendiente.vencida(hoy)) {
            stringResource(Res.string.filtro_vencida, cuando)
        } else {
            stringResource(Res.string.filtro_proxima, cuando)
        }
        "${pendiente.nombre} · $texto"
    }
}
