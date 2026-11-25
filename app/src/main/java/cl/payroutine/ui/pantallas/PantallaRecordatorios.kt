package cl.payroutine.ui.pantallas

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import cl.payroutine.datos.local.entidad.RecordatorioMovimiento
import cl.payroutine.datos.local.entidad.RecurrenciaRecordatorio
import cl.payroutine.datos.local.entidad.PreferenciasPresupuesto
import cl.payroutine.datos.local.dao.ConfiguracionConProgramacion
import cl.payroutine.datos.local.entidad.OpcionesAvisoPago
import cl.payroutine.datos.local.entidad.claveAvisoPago
import cl.payroutine.datos.local.entidad.FrecuenciaAvisoPago
import cl.payroutine.recordatorios.CANAL_MOVIMIENTOS
import cl.payroutine.recordatorios.CANAL_PAGOS
import cl.payroutine.ui.theme.RojoGasto
import cl.payroutine.viewmodel.EstadoPagos
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

private val idiomaSelectores = Locale.forLanguageTag("es-ES")
private val formatoHora = DateTimeFormatter.ofPattern("HH:mm", idiomaSelectores)
private val formatoDia = DateTimeFormatter.ofPattern("EEEE", idiomaSelectores)
private val formatoProximo = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM, HH:mm", idiomaSelectores)
private enum class HorarioRecordatorio { Movimientos, Presupuesto }

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun PantallaRecordatorios(
    configuracion: RecordatorioMovimiento,
    error: Boolean,
    notificacionesHabilitadas: () -> Boolean,
    alarmasExactasHabilitadas: () -> Boolean,
    canalSilenciado: () -> Boolean,
    canalPagosSilenciado: () -> Boolean,
    actualizar: ((RecordatorioMovimiento) -> RecordatorioMovimiento) -> Unit,
    presupuesto: PreferenciasPresupuesto,
    actualizarPresupuesto: ((PreferenciasPresupuesto) -> PreferenciasPresupuesto) -> Unit,
    avisosPagos: List<ConfiguracionConProgramacion>,
    pagos: EstadoPagos,
    configurarPago: (Int, OpcionesAvisoPago?) -> Unit,
    notificacionesPagosHabilitadas: () -> Boolean,
    volver: () -> Unit,
) {
    var elegirHora by rememberSaveable { mutableStateOf<HorarioRecordatorio?>(null) }
    var elegirDia by rememberSaveable { mutableStateOf(false) }
    var repetirDesdeFecha by rememberSaveable { mutableStateOf<RecurrenciaRecordatorio?>(null) }
    val fecha = configuracion.fechaInicial?.let(LocalDate::ofEpochDay)
    val siguiente = if (configuracion.activo) configuracion.siguienteAviso(ZonedDateTime.now()) else null
    Column(Modifier.fillMaxSize()) {
        Cabecera("Recordatorios", volver)
        Cuerpo(Modifier.weight(1f)) {
            PermisosRecordatorios(notificacionesHabilitadas, notificacionesPagosHabilitadas, alarmasExactasHabilitadas,
                canalSilenciado, canalPagosSilenciado)
            Tarjeta(Modifier.fillMaxWidth(), espacio = 12) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Destacado("Activar recordatorios")
                        Etiqueta("Recibe un aviso para anotar los ingresos y gastos que tuviste desde la última vez que abriste PayRoutine.")
                    }
                    Switch(
                        checked = configuracion.activo,
                        modifier = Modifier.semantics { contentDescription = "Activar recordatorios" },
                        onCheckedChange = { activo ->
                            actualizar { it.copy(activo = activo) }
                        },
                    )
                }
                FilaConfiguracion("Horario", configuracion.hora.format(formatoHora)) { elegirHora = HorarioRecordatorio.Movimientos }
                Destacado("Repetir")
                FlowRow(
                    Modifier.selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Pildora("Cada día", configuracion.recurrencia == RecurrenciaRecordatorio.Diaria) {
                        actualizar { it.copy(recurrencia = RecurrenciaRecordatorio.Diaria, fechaInicial = null) }
                    }
                    Pildora("Cada semana", configuracion.recurrencia == RecurrenciaRecordatorio.Semanal) {
                        repetirDesdeFecha = RecurrenciaRecordatorio.Semanal
                    }
                    Pildora("Cada mes", configuracion.recurrencia == RecurrenciaRecordatorio.Mensual) {
                        repetirDesdeFecha = RecurrenciaRecordatorio.Mensual
                    }
                    Pildora("Fin de mes", configuracion.recurrencia == RecurrenciaRecordatorio.FinDeMes) {
                        actualizar { it.copy(recurrencia = RecurrenciaRecordatorio.FinDeMes, fechaInicial = null) }
                    }
                }
                val repeticion = configuracion.recurrencia
                if (repeticion == RecurrenciaRecordatorio.Semanal || repeticion == RecurrenciaRecordatorio.Mensual) {
                    val dia = fecha ?: LocalDate.ofEpochDay(configuracion.fechaReferencia)
                    val valor = if (repeticion == RecurrenciaRecordatorio.Semanal) dia.format(formatoDia) else "Día ${dia.dayOfMonth}"
                    FilaConfiguracion("Día de repetición", valor) { repetirDesdeFecha = repeticion }
                }
                if (siguiente != null) Etiqueta("Próximo aviso: ${siguiente.format(formatoProximo)}")
            }
            Tarjeta(Modifier.fillMaxWidth(), espacio = 12) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Destacado("Aviso mensual de presupuesto")
                        Etiqueta("Recibe un aviso si todavía no tienes un presupuesto para el mes.")
                    }
                    Switch(
                        checked = presupuesto.avisoActivo,
                        modifier = Modifier.semantics { contentDescription = "Activar aviso de presupuesto" },
                        onCheckedChange = { activo ->
                            actualizarPresupuesto { it.copy(avisoActivo = activo) }
                        },
                    )
                }
                FilaConfiguracion("Día del mes", presupuesto.diaAviso.toString()) { elegirDia = true }
                val hora = LocalTime.of(presupuesto.minutoAviso / 60, presupuesto.minutoAviso % 60)
                FilaConfiguracion("Horario del aviso", hora.format(formatoHora)) { elegirHora = HorarioRecordatorio.Presupuesto }
                Etiqueta("Si el día elegido no existe, recibirás el aviso el último día del mes.")
            }
            if (error) Etiqueta("No se pudo actualizar el aviso. Vuelve a intentarlo.", RojoGasto)
            ConfiguracionAvisosPagos(avisosPagos, pagos, configurarPago)
        }
    }

    elegirHora?.let { tipo -> SelectoresEnEspanol {
        val minuto = when (tipo) {
            HorarioRecordatorio.Movimientos -> configuracion.minutoDelDia
            HorarioRecordatorio.Presupuesto -> presupuesto.minutoAviso
        }
        val reloj = rememberTimePickerState(minuto / 60, minuto % 60, is24Hour = true)
        AlertDialog(
            onDismissRequest = { elegirHora = null },
            title = { Text("Elegir horario") },
            text = { SelectoresEnEspanol { TimeInput(reloj) } },
            confirmButton = {
                TextButton(onClick = {
                    val nuevoMinuto = reloj.hour * 60 + reloj.minute
                    when (tipo) {
                        HorarioRecordatorio.Movimientos -> actualizar { it.copy(minutoDelDia = nuevoMinuto) }
                        HorarioRecordatorio.Presupuesto -> actualizarPresupuesto { it.copy(minutoAviso = nuevoMinuto) }
                    }
                    elegirHora = null
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { elegirHora = null }) { Text("Cancelar") } },
        )
    } }

    if (elegirDia) AlertDialog(
        onDismissRequest = { elegirDia = false },
        title = { Text("Elegir día del mes") },
        text = {
            FlowRow(
                Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()).selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                (1..31).forEach { dia ->
                    Pildora(dia.toString(), presupuesto.diaAviso == dia) {
                        actualizarPresupuesto { it.copy(diaAviso = dia) }
                        elegirDia = false
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { elegirDia = false }) { Text("Cancelar") } },
    )

    repetirDesdeFecha?.let { repeticion -> SelectoresEnEspanol {
        val hoy = LocalDate.now()
        val hoyUtc = hoy.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val primera = if (repeticion == configuracion.recurrencia && fecha != null) maxOf(fecha, hoy) else hoy
        val calendario = rememberDatePickerState(
            initialSelectedDateMillis = primera.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = remember(hoyUtc) {
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= hoyUtc
                    override fun isSelectableYear(year: Int) = year >= hoy.year
                }
            },
        )
        DatePickerDialog(
            onDismissRequest = { repetirDesdeFecha = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        calendario.selectedDateMillis?.let { ms ->
                            val dia = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()
                            actualizar { it.copy(fechaInicial = dia, recurrencia = repeticion) }
                        }
                        repetirDesdeFecha = null
                    },
                    enabled = calendario.selectedDateMillis != null,
                ) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { repetirDesdeFecha = null }) { Text("Cancelar") } },
        ) {
            SelectoresEnEspanol {
                DatePicker(
                    state = calendario,
                    title = { Text("Primer aviso", Modifier.padding(start = 24.dp, top = 16.dp)) },
                    showModeToggle = false,
                )
            }
        }
    } }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun EditorAvisoPago(
    inicial: OpcionesAvisoPago,
    cerrar: () -> Unit,
    guardar: (OpcionesAvisoPago) -> Unit,
) {
    var dias by rememberSaveable { mutableIntStateOf(inicial.anticipacion) }
    var frecuencia by rememberSaveable { mutableStateOf(inicial.frecuencia) }
    val reloj = rememberTimePickerState(inicial.minuto / 60, inicial.minuto % 60, is24Hour = true)
    AlertDialog(
        onDismissRequest = cerrar,
        title = { Text("Configurar recordatorio") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Destacado("Hora")
                SelectoresEnEspanol { TimeInput(reloj) }
                Destacado("Anticipación")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (dia in listOf(0, 1, 3, 7)) Pildora(textoAnticipacion(dia), dias == dia) { dias = dia }
                }
                Destacado("Repetir")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (opcion in FrecuenciaAvisoPago.entries) Pildora(textoFrecuencia(opcion), frecuencia == opcion) {
                        frecuencia = opcion
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { guardar(OpcionesAvisoPago(dias, reloj.hour * 60 + reloj.minute, frecuencia)) }) {
                Text("Guardar recordatorio")
            }
        },
        dismissButton = { TextButton(onClick = cerrar) { Text("Cancelar") } },
    )
}

@Composable
private fun ConfiguracionAvisosPagos(
    avisos: List<ConfiguracionConProgramacion>,
    estado: EstadoPagos,
    configurar: (Int, OpcionesAvisoPago?) -> Unit,
) {
    var idEditor by rememberSaveable { mutableStateOf<Int?>(null) }
    var idEliminar by rememberSaveable { mutableStateOf<Int?>(null) }
    var opciones by rememberSaveable { mutableStateOf(OpcionesAvisoPago()) }
    val configuraciones = avisos.associateBy { it.configuracion.clave }
    val grupos = estado.pagos.groupBy { claveAvisoPago(it.pago.obligacionId, it.pago.id) }
        .filter { (clave, filas) -> clave in configuraciones || filas.any { !it.pago.pagado } }
    Tarjeta(Modifier.fillMaxWidth(), espacio = 12) {
        Destacado("Avisos de pagos")
        Etiqueta("Cada recordatorio se aplica a los vencimientos pendientes de su obligación.")
        when {
            estado.cargando -> Etiqueta("Cargando obligaciones…")
            estado.errorCarga -> Etiqueta("No se pudieron cargar las obligaciones.", RojoGasto)
            grupos.isEmpty() -> Etiqueta("No tienes pagos pendientes.")
            else -> grupos.forEach { (clave, filas) ->
                val fila = filas.firstOrNull { !it.pago.pagado } ?: filas.first()
                val programacion = configuraciones[clave]
                val aviso = programacion?.configuracion
                HorizontalDivider()
                Destacado(fila.nombreObligacion ?: fila.pago.descripcion)
                Etiqueta(if (aviso?.activo == true) {
                    val config = aviso.opciones
                    val hora = LocalTime.of(config.minuto / 60, config.minuto % 60).format(formatoHora)
                    "${textoAnticipacion(config.anticipacion)} · $hora · ${textoFrecuencia(config.frecuencia)}"
                } else "Sin aviso activo")
                if (aviso?.activo == true) {
                    val proximo = programacion.proximoDia?.let(LocalDate::ofEpochDay)
                    Etiqueta(if (proximo == null) "Sin próximos avisos programados."
                    else "Próximo aviso: ${fechaEnPantalla(proximo)}")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Enlace(if (aviso == null) "Configurar" else "Editar") {
                        opciones = aviso?.opciones ?: OpcionesAvisoPago()
                        idEditor = fila.pago.id
                    }
                    if (aviso != null) Enlace("Eliminar aviso") { idEliminar = fila.pago.id }
                }
            }
        }
    }
    idEditor?.let { id ->
        EditorAvisoPago(opciones, { idEditor = null }) {
            configurar(id, it)
            idEditor = null
        }
    }
    idEliminar?.let { id ->
        AlertDialog(
            onDismissRequest = { idEliminar = null },
            title = { Text("¿Eliminar el recordatorio?") },
            confirmButton = { TextButton(onClick = { configurar(id, null); idEliminar = null }) { Text("Eliminar aviso") } },
            dismissButton = { TextButton(onClick = { idEliminar = null }) { Text("Cancelar") } },
        )
    }
}

private fun textoAnticipacion(dias: Int) = when (dias) {
    0 -> "El mismo día"
    1 -> "1 día antes"
    else -> "$dias días antes"
}

private fun textoFrecuencia(frecuencia: FrecuenciaAvisoPago) = when (frecuencia) {
    FrecuenciaAvisoPago.UnaVez -> "Una vez"
    FrecuenciaAvisoPago.Diaria -> "Cada día"
    FrecuenciaAvisoPago.CadaTresDias -> "Cada 3 días"
    FrecuenciaAvisoPago.Semanal -> "Cada semana"
}


@Composable
private fun PermisosRecordatorios(
    notificacionesHabilitadas: () -> Boolean,
    avisosPagoHabilitados: () -> Boolean,
    alarmasExactasHabilitadas: () -> Boolean,
    canalSilenciado: () -> Boolean,
    canalPagosSilenciado: () -> Boolean,
) {
    val contexto = LocalContext.current
    val ciclo = LocalLifecycleOwner.current.lifecycle
    val fase by ciclo.currentStateFlow.collectAsState()
    val consultarNotificaciones by rememberUpdatedState(notificacionesHabilitadas)
    val consultarPagos by rememberUpdatedState(avisosPagoHabilitados)
    val consultarExactitud by rememberUpdatedState(alarmasExactasHabilitadas)
    val consultarSilencio by rememberUpdatedState(canalSilenciado)
    val consultarSilencioPagos by rememberUpdatedState(canalPagosSilenciado)
    var notificaciones by remember { mutableStateOf(consultarNotificaciones()) }
    var pagos by remember { mutableStateOf(consultarPagos()) }
    var exactas by remember { mutableStateOf(consultarExactitud()) }
    var silenciado by remember { mutableStateOf(consultarSilencio()) }
    var pagosSilenciados by remember { mutableStateOf(consultarSilencioPagos()) }
    var continuarConAlarmas by rememberSaveable { mutableStateOf(false) }
    var seSolicito by rememberSaveable { mutableStateOf(false) }

    fun refrescar() {
        notificaciones = consultarNotificaciones()
        pagos = consultarPagos()
        exactas = consultarExactitud()
        silenciado = consultarSilencio()
        pagosSilenciados = consultarSilencioPagos()
    }

    val solicitud = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        refrescar()
        seSolicito = true
        if (concedido) continuarConAlarmas = true
    }

    val ajustes = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        refrescar()
        if (consultarNotificaciones() && consultarPagos()) continuarConAlarmas = true
    }
    val ajustesAlarmas = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { refrescar() }
    LaunchedEffect(continuarConAlarmas, fase) {
        if (!continuarConAlarmas || fase != Lifecycle.State.RESUMED) return@LaunchedEffect
        // Deja que Android termine de cerrar el diálogo antes de abrir otro ajuste.
        delay(250)
        continuarConAlarmas = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !consultarExactitud()) {
            ajustesAlarmas.launch(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                Uri.fromParts("package", contexto.packageName, null)))
        }
    }
    val actualizar by rememberUpdatedState({ refrescar() })
    DisposableEffect(ciclo) {
        val observador = LifecycleEventObserver { _, evento ->
            if (evento == Lifecycle.Event.ON_RESUME) actualizar()
        }
        ciclo.addObserver(observador)
        onDispose { ciclo.removeObserver(observador) }
    }

    if (!notificaciones || !pagos || !exactas || silenciado || pagosSilenciados) Tarjeta(Modifier.fillMaxWidth(), espacio = 8) {
        if (!notificaciones || !pagos) {
            Destacado(if (!pagos && notificaciones) "Notificaciones de pagos bloqueadas" else "Notificaciones bloqueadas", RojoGasto)
            Etiqueta("Permite las notificaciones de PayRoutine y activa sus categorías de avisos en los ajustes de Android.")
        }
        if (!exactas) {
            Destacado("Horario aproximado")
            Etiqueta("En Alarmas y recordatorios, permite programar alarmas exactas para recibir el aviso a la hora elegida.")
        }
        if (silenciado) {
            Etiqueta("El aviso de movimientos está silenciado en Android.")
            Enlace("Abrir ajustes de sonido") {
                contexto.startActivity(Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, contexto.packageName)
                    .putExtra(Settings.EXTRA_CHANNEL_ID, CANAL_MOVIMIENTOS))
            }
        }
        if (pagosSilenciados) {
            Etiqueta("Los avisos de pagos están silenciados en Android.")
            Enlace("Abrir ajustes de sonido de pagos") {
                contexto.startActivity(Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, contexto.packageName)
                    .putExtra(Settings.EXTRA_CHANNEL_ID, CANAL_PAGOS))
            }
        }
        if (!notificaciones || !pagos || !exactas) BotonPrincipal("Habilitar avisos paso a paso") {
            if (!consultarNotificaciones() || !consultarPagos()) {
                if (Build.VERSION.SDK_INT >= 33 && !seSolicito &&
                    ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                ) solicitud.launch(Manifest.permission.POST_NOTIFICATIONS)
                else {
                    ajustes.launch(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, contexto.packageName))
                }
            } else continuarConAlarmas = true
        }
    }
}

@Composable
private fun SelectoresEnEspanol(contenido: @Composable () -> Unit) {
    val contexto = LocalContext.current
    val configuracion = LocalConfiguration.current
    val configuracionEnEspanol = remember(configuracion) {
        Configuration(configuracion).apply { setLocale(idiomaSelectores) }
    }
    val contextoEnEspanol = remember(contexto, configuracionEnEspanol) {
        contexto.createConfigurationContext(configuracionEnEspanol)
    }
    CompositionLocalProvider(
        LocalContext provides contextoEnEspanol,
        LocalConfiguration provides configuracionEnEspanol,
        content = contenido,
    )
}

@Composable
internal fun FilaConfiguracion(etiqueta: String, valor: String, abrir: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Etiqueta(etiqueta)
        Enlace(valor, abrir)
    }
}
