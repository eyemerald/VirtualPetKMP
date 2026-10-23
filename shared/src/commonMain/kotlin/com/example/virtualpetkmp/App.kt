package com.example.virtualpetkmp

import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.virtualpetkmp.data.DatabaseFactory
import com.example.virtualpetkmp.data.InformeRepository
import com.example.virtualpetkmp.data.MascotaRepository
import com.example.virtualpetkmp.data.NotaRepository
import com.example.virtualpetkmp.data.PesoRepository
import com.example.virtualpetkmp.data.PreventivoRepository
import com.example.virtualpetkmp.data.RevisionRepository
import com.example.virtualpetkmp.data.TratamientoRepository
import com.example.virtualpetkmp.data.VacunaRepository
import com.example.virtualpetkmp.data.VeterinarioRepository
import com.example.virtualpetkmp.ui.DialogoCopia
import com.example.virtualpetkmp.ui.DialogoGuardarCopia
import com.example.virtualpetkmp.ui.DialogoImportarFusion
import com.example.virtualpetkmp.ui.DialogoRestaurarCopia
import com.example.virtualpetkmp.util.rememberCarpetasApp
import com.example.virtualpetkmp.util.reprogramarAvisosDeTodo
import com.example.virtualpetkmp.ui.FichaMascotaScreen
import com.example.virtualpetkmp.ui.MainScreen
import com.example.virtualpetkmp.ui.MascotaFormScreen
import com.example.virtualpetkmp.ui.MascotaListScreen
import com.example.virtualpetkmp.ui.VeterinarioFormScreen
import com.example.virtualpetkmp.ui.VeterinariosListScreen
import com.example.virtualpetkmp.ui.theme.VirtualPetTheme
import com.example.virtualpetkmp.util.ConLocalizacion
import com.example.virtualpetkmp.util.ReprogramadorNotificaciones
import com.example.virtualpetkmp.util.localizacionDelSistema
import com.example.virtualpetkmp.util.rememberProgramadorNotificaciones
import com.example.virtualpetkmp.viewmodel.FichaMascotaViewModel
import com.example.virtualpetkmp.viewmodel.MascotaViewModel
import com.example.virtualpetkmp.viewmodel.VeterinarioViewModel
import kotlinx.coroutines.launch

/**
 * Raíz de la app.
 *
 * La navegación es mínima: lista de mascotas, ficha y formulario. Todo lo que antes eran
 * pantallas de detalle (vacunas, preventivos, revisiones, tratamientos, informes, notas y
 * peso) se abre ahora en hojas modales dentro de la ficha, así que sus rutas y sus
 * ViewModels ya no existen.
 *
 * @param solicitud petición de navegación que llega de fuera (hoy, tocar el aviso de un
 *   tratamiento). Puede cambiar en caliente: si la app ya estaba abierta y el usuario toca otra
 *   notificación, se navega otra vez sin reiniciar nada.
 */
@Composable
fun App(databaseFactory: DatabaseFactory, solicitud: SolicitudNavegacion? = null) {
    // La configuración regional se decide UNA vez aquí y se publica al resto de la interfaz:
    // unidades, orden de fecha y separador decimal salen siempre de este valor, así que no
    // puede haber dos pantallas que opinen distinto. Se calcula antes que nada porque también
    // la necesitan las notificaciones de arranque y el ViewModel de la ficha.
    val localizacion = localizacionDelSistema()

    val database = remember { databaseFactory.createDatabase() }
    val repository = remember { MascotaRepository(database) }
    val notaRepository = remember { NotaRepository(database) }
    val pesoRepository = remember { PesoRepository(database) }
    val vacunaRepository = remember { VacunaRepository(database) }
    val revisionRepository = remember { RevisionRepository(database) }
    val tratamientoRepository = remember { TratamientoRepository(database) }
    val informeRepository = remember { InformeRepository(database) }
    val veterinarioRepository = remember { VeterinarioRepository(database) }
    val preventivoRepository = remember { PreventivoRepository(database) }
    val viewModel = remember { MascotaViewModel(repository, pesoRepository) }
    val veterinarioViewModel = remember { VeterinarioViewModel(veterinarioRepository) }
    val programador = rememberProgramadorNotificaciones()
    val alcance = rememberCoroutineScope()

    // Al arrancar se reprograman los avisos de todas las mascotas. Abrir la ficha de una
    // mascota también los reprograma, así que esto es la red de seguridad para el caso de
    // que el usuario no entre en ninguna ficha. Es la misma función que usa el receptor de
    // reinicio del móvil: antes había dos copias y una se olvidó de las dosis.
    LaunchedEffect(Unit) {
        try {
            reprogramarAvisosDeTodo(programador, database, localizacion)
        } catch (e: Exception) {
            println("Error reprogramando notificaciones al arrancar: ${e.message}")
        }
    }

    var currentScreen by rememberSaveable { mutableStateOf("list") }
    var selectedMascotaId by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedVeterinarioId by rememberSaveable { mutableStateOf<Long?>(null) }
    var pantallaVeterinarios by rememberSaveable { mutableStateOf("lista") }
    var dialogoExportarFicha by rememberSaveable { mutableStateOf(false) }

    // Si el usuario toca el aviso de un tratamiento, se abre la ficha de ESA mascota (y su hoja de
    // Salud y seguimiento). Se consume una sola vez: el flag se apaga al abrirse la hoja, para que
    // volver a la ficha a mano no la abra otra vez.
    var abrirTratamientos by remember { mutableStateOf(false) }
    LaunchedEffect(solicitud) {
        if (solicitud != null) {
            selectedMascotaId = solicitud.mascotaId
            currentScreen = "ficha"
            abrirTratamientos = solicitud.abrirTratamientos
        }
    }

    val mascotas by viewModel.mascotas.collectAsState()
    val mascotaActual = mascotas.find { it.id == selectedMascotaId }

    // Carpetas privadas de la app (fotos, informes y copias) y qué diálogo de copia está abierto.
    val carpetas = rememberCarpetasApp()
    var dialogoCopia by remember { mutableStateOf<DialogoCopia?>(null) }
    // Mascota elegida para exportar solo sus datos (menú de su ficha).
    var mascotaParaExportar by remember { mutableStateOf<Mascota?>(null) }

    // Un único ViewModel de ficha por mascota, compartido por todas las hojas: así el
    // resumen, las listas y los avisos son los mismos en toda la pantalla.
    val fichaViewModel = selectedMascotaId?.let { mascotaId ->
        // La clave incluye el nombre: el ViewModel lo necesita para los recordatorios de
        // tratamiento, así que si se renombra la mascota se recrea con el nombre nuevo.
        remember(mascotaId, mascotaActual?.nombre) {
            FichaMascotaViewModel(
                mascotaId = mascotaId,
                vacunaRepository = vacunaRepository,
                revisionRepository = revisionRepository,
                tratamientoRepository = tratamientoRepository,
                pesoRepository = pesoRepository,
                informeRepository = informeRepository,
                notaRepository = notaRepository,
                preventivoRepository = preventivoRepository,
                programador = programador,
                nombreMascota = mascotaActual?.nombre.orEmpty(),
                localizacion = localizacion
            )
        }
    }

    // Iconos flotantes: solo en el detalle de una mascota. En la lista y en el formulario
    // no hacen falta (el formulario tiene su propio botón de guardar), y con la pestaña de
    // Veterinarios se ocultan solos porque MainScreen mira la pestaña activa.
    val enDetalleMascota = currentScreen == "ficha" && mascotaActual != null

    val volverAtras: (() -> Unit)? = when {
        currentScreen == "list" -> null
        currentScreen == "form" -> fun() {
            currentScreen = if (selectedMascotaId != null) "ficha" else "list"
        }
        else -> fun() {
            currentScreen = "list"
            selectedMascotaId = null
        }
    }

    // La configuración regional se decide UNA vez aquí y se publica al resto de la interfaz:
    // unidades, orden de fecha y separador decimal salen siempre de este valor, así que no
    // puede haber dos pantallas que opinen distinto.
    VirtualPetTheme {
        ConLocalizacion(localizacion) {
            MainScreen(
            mostrarAcciones = enDetalleMascota,
            onVolver = volverAtras,
            onGuardarCopia = { dialogoCopia = DialogoCopia.GUARDAR },
            onRestaurarCopia = { dialogoCopia = DialogoCopia.RESTAURAR },
            onImportarFusion = { dialogoCopia = DialogoCopia.IMPORTAR },
            // Al abrir el menú se reprograman los avisos: es lo que hace que, al volver de
            // conceder el permiso de alarmas exactas en Ajustes, pasen a ser exactas sin
            // reiniciar la app.
            onAbrirMenuSistema = {
                alcance.launch {
                    try {
                        reprogramarAvisosDeTodo(programador, database, localizacion)
                    } catch (e: Exception) {
                        println("Error reprogramando avisos: ${e.message}")
                    }
                }
            },
            contenidoMascotas = {
                when (currentScreen) {
                    "list" -> {
                        MascotaListScreen(
                            viewModel = viewModel,
                            onMascotaClick = { id ->
                                selectedMascotaId = id
                                currentScreen = "ficha"
                            },
                            onAddMascota = {
                                selectedMascotaId = null
                                currentScreen = "form"
                            }
                        )
                    }

                    "ficha" -> {
                        val mascota = mascotaActual
                        val ficha = fichaViewModel
                        if (mascota != null && ficha != null) {
                            FichaMascotaScreen(
                                mascota = mascota,
                                viewModel = ficha,
                                mascotaViewModel = viewModel,
                                mostrarDialogoExportar = dialogoExportarFicha,
                                onCambiarDialogoExportar = { visible -> dialogoExportarFicha = visible },
                                abrirTratamientosAlEntrar = abrirTratamientos,
                                onTratamientosAbiertos = { abrirTratamientos = false },
                                onExportarDatos = {
                                    mascotaParaExportar = mascota
                                    dialogoCopia = DialogoCopia.EXPORTAR_MASCOTA
                                },
                                onModificar = { currentScreen = "form" }
                            )
                        }
                    }

                    "form" -> {
                        MascotaFormScreen(
                            viewModel = viewModel,
                            mascotaId = selectedMascotaId,
                            onBack = {
                                currentScreen = if (selectedMascotaId != null) "ficha" else "list"
                            }
                        )
                    }
                }
            },
            contenidoVeterinarios = {
                when (pantallaVeterinarios) {
                    "lista" -> {
                        VeterinariosListScreen(
                            viewModel = veterinarioViewModel,
                            onVeterinarioClick = { id ->
                                selectedVeterinarioId = id
                                pantallaVeterinarios = "form"
                            },
                            onAddVeterinario = {
                                selectedVeterinarioId = null
                                pantallaVeterinarios = "form"
                            }
                        )
                    }
                    "form" -> {
                        VeterinarioFormScreen(
                            viewModel = veterinarioViewModel,
                            veterinarioId = selectedVeterinarioId,
                            onBack = {
                                pantallaVeterinarios = "lista"
                                selectedVeterinarioId = null
                            }
                        )
                    }
                }
            }
        )

        // Los diálogos de copia van aquí, dentro del tema y de la configuración regional, para que
        // sus textos salgan en el idioma activo aunque se abran desde la barra superior.
        when (dialogoCopia) {
            DialogoCopia.GUARDAR -> {
                DialogoGuardarCopia(
                    db = database,
                    carpetas = carpetas,
                    onCerrar = { dialogoCopia = null }
                )
            }

            DialogoCopia.RESTAURAR -> {
                DialogoRestaurarCopia(
                    db = database,
                    carpetas = carpetas,
                    alRestaurar = {
                        // Los datos han cambiado por completo: se reprograman los avisos para que
                        // no queden alarmas de mascotas que ya no existen ni falten las nuevas.
                        reprogramarAvisosDeTodo(programador, database, localizacion)
                    },
                    onCerrar = {
                        dialogoCopia = null
                        // La lista que se veía ya no es la de la base de datos.
                        selectedMascotaId = null
                        currentScreen = "list"
                        viewModel.loadMascotas()
                    }
                )
            }

            DialogoCopia.EXPORTAR_MASCOTA -> {
                DialogoGuardarCopia(
                    db = database,
                    carpetas = carpetas,
                    soloMascota = mascotaParaExportar,
                    onCerrar = {
                        dialogoCopia = null
                        mascotaParaExportar = null
                    }
                )
            }

            DialogoCopia.IMPORTAR -> {
                DialogoImportarFusion(
                    db = database,
                    carpetas = carpetas,
                    alFusionar = { reprogramarAvisosDeTodo(programador, database, localizacion) },
                    onCerrar = {
                        dialogoCopia = null
                        // La lista que se veía puede haber cambiado: llegan mascotas del paquete.
                        viewModel.loadMascotas()
                    }
                )
            }

            null -> Unit
        }
        }
    }
}
