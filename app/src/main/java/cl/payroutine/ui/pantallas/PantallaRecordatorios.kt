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
import cl.payroutine.recordatorios.CANAL_MOVIMIENTOS
import cl.payroutine.ui.theme.RojoGasto
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

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
    actualizar: ((RecordatorioMovimiento) -> RecordatorioMovimiento) -> Unit,
    presupuesto: PreferenciasPresupuesto,
    actualizarPresupuesto: ((PreferenciasPresupuesto) -> PreferenciasPresupuesto) -> Unit,
    volver: () -> Unit,
) {
    val contexto = LocalContext.current
    val ciclo = LocalLifecycleOwner.current.lifecycle
    val comprobarPermiso by rememberUpdatedState(notificacionesHabilitadas)
    val comprobarPrecision by rememberUpdatedState(alarmasExactasHabilitadas)
    val comprobarSonido by rememberUpdatedState(canalSilenciado)
    var permitido by remember { mutableStateOf(comprobarPermiso()) }
    var preciso by remember { mutableStateOf(comprobarPrecision()) }
    var silenciado by remember { mutableStateOf(comprobarSonido()) }
    val solicitud = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permitido = comprobarPermiso()
    }
    DisposableEffect(ciclo) {
        val observador = LifecycleEventObserver { _, evento ->
            if (evento == Lifecycle.Event.ON_RESUME) {
                permitido = comprobarPermiso()
                preciso = comprobarPrecision()
                silenciado = comprobarSonido()
            }
        }
        ciclo.addObserver(observador)
        onDispose { ciclo.removeObserver(observador) }
    }

    var elegirHora by rememberSaveable { mutableStateOf<HorarioRecordatorio?>(null) }
    var elegirDia by rememberSaveable { mutableStateOf(false) }
    var repetirDesdeFecha by rememberSaveable { mutableStateOf<RecurrenciaRecordatorio?>(null) }
    val fecha = configuracion.fechaInicial?.let(LocalDate::ofEpochDay)
    val siguiente = if (configuracion.activo) configuracion.siguienteAviso(ZonedDateTime.now()) else null
    fun pedirNotificaciones() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) solicitud.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    Column(Modifier.fillMaxSize()) {
        Cabecera("Recordatorios", volver)
        Cuerpo(Modifier.weight(1f)) {
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
                            if (activo) pedirNotificaciones()
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
            if (permitido && silenciado) {
                Tarjeta(Modifier.fillMaxWidth(), espacio = 8) {
                    Destacado("Recordatorio sin sonido")
                    Etiqueta("Puedes volver a activar el sonido en el canal de recordatorios de Android.")
                    Enlace("Abrir ajustes de sonido") {
                        contexto.startActivity(
                            Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, contexto.packageName)
                                .putExtra(Settings.EXTRA_CHANNEL_ID, CANAL_MOVIMIENTOS),
                        )
                    }
                }
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
                            if (activo) pedirNotificaciones()
                        },
                    )
                }
                FilaConfiguracion("Día del mes", presupuesto.diaAviso.toString()) { elegirDia = true }
                val hora = LocalTime.of(presupuesto.minutoAviso / 60, presupuesto.minutoAviso % 60)
                FilaConfiguracion("Horario del aviso", hora.format(formatoHora)) { elegirHora = HorarioRecordatorio.Presupuesto }
                Etiqueta("Si el día elegido no existe, recibirás el aviso el último día del mes.")
            }
            if ((configuracion.activo || presupuesto.avisoActivo) && !preciso && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Tarjeta(Modifier.fillMaxWidth(), espacio = 8) {
                    Destacado("Horario aproximado")
                    Etiqueta("Para recibir el aviso a la hora elegida, permite que PayRoutine programe alarmas exactas.")
                    Enlace("Permitir alarmas exactas") {
                        contexto.startActivity(
                            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.fromParts("package", contexto.packageName, null)),
                        )
                    }
                }
            }
            if (!permitido) {
                Tarjeta(Modifier.fillMaxWidth(), espacio = 8) {
                    Destacado("Notificaciones bloqueadas", RojoGasto)
                    Etiqueta("Permite las notificaciones de PayRoutine en los ajustes del sistema para recibir avisos.")
                    Enlace("Abrir ajustes de notificaciones") {
                        contexto.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, contexto.packageName),
                        )
                    }
                }
            }
            if (error) Etiqueta("No se pudo actualizar el aviso. Vuelve a intentarlo.", RojoGasto)
            Tarjeta(Modifier.fillMaxWidth()) {
                Destacado("Avisos de pagos")
                Etiqueta("Estarán disponibles cuando se complete la gestión de pagos.")
            }
        }
    }

    elegirHora?.let { tipo -> SelectoresEnEspanol {
        val minuto = if (tipo == HorarioRecordatorio.Presupuesto) presupuesto.minutoAviso else configuracion.minutoDelDia
        val reloj = rememberTimePickerState(minuto / 60, minuto % 60, is24Hour = true)
        AlertDialog(
            onDismissRequest = { elegirHora = null },
            title = { Text("Elegir horario") },
            text = { SelectoresEnEspanol { TimeInput(reloj) } },
            confirmButton = {
                TextButton(onClick = {
                    val nuevoMinuto = reloj.hour * 60 + reloj.minute
                    if (tipo == HorarioRecordatorio.Presupuesto) actualizarPresupuesto { it.copy(minutoAviso = nuevoMinuto) }
                    else actualizar { it.copy(minutoDelDia = nuevoMinuto) }
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
