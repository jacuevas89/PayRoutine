package cl.payroutine.ui.pantallas

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cl.payroutine.datos.local.entidad.TipoMovimiento
import cl.payroutine.datos.repositorio.AjusteCuota
import cl.payroutine.datos.repositorio.NuevaDeudaEnCuotas
import cl.payroutine.datos.repositorio.NuevoPagoUnico
import cl.payroutine.datos.repositorio.PeriodicidadCuotas
import cl.payroutine.datos.repositorio.aniosCronograma
import cl.payroutine.datos.repositorio.generarCronogramaCuotas
import cl.payroutine.datos.repositorio.validarMontoMovimiento
import cl.payroutine.ui.theme.RojoGasto
import cl.payroutine.ui.theme.TextoSecundario
import cl.payroutine.viewmodel.EstadoCategorias
import cl.payroutine.viewmodel.EstadoRegistroPago
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PantallaNuevoPago(
    estadoCategorias: EstadoCategorias,
    reintentarCategorias: () -> Unit,
    estadoRegistro: EstadoRegistroPago,
    guardar: (NuevoPagoUnico) -> Unit,
    guardarDeuda: (NuevaDeudaEnCuotas) -> Unit,
    volver: () -> Unit,
) {
    var nombre by rememberSaveable { mutableStateOf("") }
    var monto by rememberSaveable { mutableStateOf("") }
    var fechaDia by rememberSaveable { mutableStateOf<Long?>(null) }
    var categoriaId by rememberSaveable { mutableStateOf<Int?>(null) }
    var mostrarCalendario by rememberSaveable { mutableStateOf(false) }
    var intentoGuardar by rememberSaveable { mutableStateOf(false) }
    var enCuotas by rememberSaveable { mutableStateOf(false) }
    var cantidadTexto by rememberSaveable { mutableStateOf("") }
    var pagadasTexto by rememberSaveable { mutableStateOf("0") }
    var periodicidad by rememberSaveable { mutableStateOf(PeriodicidadCuotas.Mensual) }
    var ajuste by rememberSaveable { mutableStateOf(AjusteCuota.Ultima) }
    var mostrarCronograma by rememberSaveable { mutableStateOf(false) }
    var cronograma by rememberSaveable(stateSaver = guardadorCronograma) { mutableStateOf(emptyList<CuotaEditable>()) }
    var claveCronograma by rememberSaveable { mutableStateOf("") }
    var errorCronograma by rememberSaveable { mutableStateOf<String?>(null) }
    val categorias = estadoCategorias.categorias.map { it.categoria }.filter { it.tipo == TipoMovimiento.Gasto }
    val categoria = categorias.firstOrNull { it.id == categoriaId }
    val fecha = fechaDia?.let(LocalDate::ofEpochDay)
    val montoValidado = validarMontoMovimiento(monto)
    val montoTotal = montoValidado.valor
    val cantidad = cantidadTexto.trim().toIntOrNull()
    val pagadas = pagadasTexto.trim().toIntOrNull()
    val categoriaDisponible = !estadoCategorias.cargando && !estadoCategorias.errorCarga && categoria != null
    val bloqueado = estadoRegistro.guardando || estadoRegistro.idGuardado != null
    val foco = LocalFocusManager.current
    val teclado = LocalSoftwareKeyboardController.current

    fun cerrarTeclado() {
        foco.clearFocus()
        teclado?.hide()
    }

    BackHandler(enabled = bloqueado || mostrarCronograma) {
        if (!bloqueado) mostrarCronograma = false
    }
    if (mostrarCronograma && montoTotal != null && pagadas != null) {
        PantallaCronogramaCuotas(
            cuotas = cronograma,
            montoTotal = montoTotal,
            cuotasPagadas = pagadas,
            categoriaDisponible = categoriaDisponible,
            estado = estadoRegistro,
            cambiarCuota = { i, cuota -> cronograma = cronograma.toMutableList().apply { this[i] = cuota } },
            guardar = { cuotas ->
                if (categoriaDisponible) {
                    guardarDeuda(NuevaDeudaEnCuotas(nombre, montoTotal, categoria.id, pagadas, cuotas))
                }
            },
            volver = { mostrarCronograma = false },
        )
        return
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        Cabecera("Nuevo pago pendiente", volver = {
            if (!bloqueado) {
                cerrarTeclado()
                volver()
            }
        })
        Cuerpo(Modifier.weight(1f), espacio = 14) {
            Segmentos(listOf("Pago único", "En cuotas"), if (enCuotas) 1 else 0) {
                if (!bloqueado) {
                    enCuotas = it == 1
                    intentoGuardar = false
                    errorCronograma = null
                }
            }
            OutlinedTextField(
                value = nombre, onValueChange = { nombre = it },
                modifier = Modifier.fillMaxWidth(), enabled = !bloqueado, singleLine = true,
                label = { Text("Nombre del pago") },
                isError = intentoGuardar && nombre.isBlank(),
                supportingText = if (intentoGuardar && nombre.isBlank()) ({ Text("Ingresa el nombre del pago") }) else null,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                shape = RoundedCornerShape(12.dp),
            )
            OutlinedTextField(
                value = monto, onValueChange = { monto = it; errorCronograma = null },
                modifier = Modifier.fillMaxWidth(), enabled = !bloqueado, singleLine = true,
                label = { Text(if (enCuotas) "Monto total" else "Monto") }, prefix = { Text("$ ") },
                isError = intentoGuardar && montoValidado.error != null,
                supportingText = { Text(if (intentoGuardar) montoValidado.error ?: "Pesos chilenos (CLP)" else "Pesos enteros (CLP)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                shape = RoundedCornerShape(12.dp),
            )
            CampoVencimientoPago(
                etiqueta = if (enCuotas) "Vencimiento de la primera cuota" else "Fecha de vencimiento",
                descripcion = "Seleccionar fecha de vencimiento",
                fecha = fecha,
                alPulsar = if (bloqueado) null else ({ cerrarTeclado(); mostrarCalendario = true }),
            )
            if (intentoGuardar && fecha == null) Etiqueta("Selecciona la fecha de vencimiento", RojoGasto)
            if (enCuotas) {
                CampoCantidadCuotas(
                    "Cantidad de cuotas", cantidadTexto, { cantidadTexto = it; errorCronograma = null },
                    error = if (intentoGuardar && (cantidad == null || cantidad < 2)) "Ingresa al menos dos cuotas." else null,
                )
                CampoCantidadCuotas(
                    "Cuotas ya pagadas", pagadasTexto, { pagadasTexto = it; errorCronograma = null },
                    error = if (intentoGuardar && (pagadas == null || cantidad == null || pagadas !in 0..cantidad)) {
                        "Ingresa cuántas cuotas ya pagaste, desde 0 hasta la cantidad total."
                    } else null,
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Etiqueta("Periodicidad")
                    FlowRow(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PeriodicidadCuotas.entries.forEach { opcion ->
                            Pildora(opcion.etiqueta, activa = opcion == periodicidad) {
                                periodicidad = opcion
                                errorCronograma = null
                            }
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Etiqueta("Ajuste de redondeo")
                    Segmentos(listOf("Primera cuota", "Última cuota"), if (ajuste == AjusteCuota.Primera) 0 else 1) {
                        ajuste = AjusteCuota.entries[it]
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Etiqueta("Categoría de gasto")
                when {
                    estadoCategorias.cargando -> Etiqueta("Cargando categorías…")
                    estadoCategorias.errorCarga -> {
                        Etiqueta("No se pudieron cargar las categorías.")
                        BotonPrincipal("Reintentar", alPulsar = reintentarCategorias)
                    }
                    categorias.isEmpty() -> Etiqueta("No hay categorías de gasto disponibles.")
                    else -> FlowRow(
                        modifier = Modifier.selectableGroup(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        categorias.forEach { opcion ->
                            Pildora(opcion.nombre, activa = opcion.id == categoriaId, habilitada = !bloqueado) {
                                categoriaId = opcion.id
                            }
                        }
                    }
                }
                if (intentoGuardar && categoria == null && categorias.isNotEmpty()) {
                    Etiqueta("Selecciona una categoría de gasto", RojoGasto)
                }
            }
        }
        (errorCronograma ?: estadoRegistro.error)?.let { Etiqueta(it, RojoGasto, Modifier.padding(horizontal = 16.dp)) }
        BotonPrincipal(
            when {
                estadoRegistro.guardando -> "Guardando…"
                enCuotas -> "Previsualizar cuotas"
                else -> "Guardar pago pendiente"
            },
            modifier = Modifier.padding(16.dp),
            habilitado = !bloqueado && !estadoCategorias.cargando && !estadoCategorias.errorCarga && categorias.isNotEmpty(),
        ) {
            intentoGuardar = true
            if (nombre.isBlank() || montoTotal == null || fecha == null || categoria == null) return@BotonPrincipal
            cerrarTeclado()
            if (!enCuotas) {
                guardar(NuevoPagoUnico(nombre, montoTotal, fecha, categoria.id))
            } else if (cantidad != null && cantidad >= 2 && pagadas != null && pagadas in 0..cantidad) {
                val clave = listOf(montoTotal, cantidad, fecha, periodicidad, ajuste).joinToString("|")
                try {
                    if (cronograma.isEmpty() || clave != claveCronograma) {
                        cronograma = generarCronogramaCuotas(montoTotal, cantidad, fecha, periodicidad, ajuste)
                            .map { CuotaEditable(it.vencimiento, it.monto.toString()) }
                        claveCronograma = clave
                    }
                    errorCronograma = null
                    mostrarCronograma = true
                } catch (error: IllegalArgumentException) {
                    errorCronograma = error.message
                }
            }
        }
    }

    if (mostrarCalendario && !bloqueado) {
        SelectorVencimientoPago(fecha, cerrar = { mostrarCalendario = false }) {
            fechaDia = it.toEpochDay()
            errorCronograma = null
            mostrarCalendario = false
        }
    }
}

@Composable
private fun CampoCantidadCuotas(texto: String, valor: String, cambiar: (String) -> Unit, error: String?) {
    OutlinedTextField(
        value = valor, onValueChange = cambiar,
        modifier = Modifier.fillMaxWidth(), singleLine = true,
        label = { Text(texto) },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        shape = RoundedCornerShape(12.dp),
    )
}

@Composable
internal fun CampoVencimientoPago(etiqueta: String, descripcion: String, fecha: LocalDate?, alPulsar: (() -> Unit)?) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Etiqueta(etiqueta)
        Tarjeta(
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = descripcion },
            alPulsar = alPulsar,
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Destacado(fecha?.let(::fechaEnPantalla) ?: "Seleccionar fecha")
                Icono("calendar", TextoSecundario)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SelectorVencimientoPago(fecha: LocalDate?, cerrar: () -> Unit, elegir: (LocalDate) -> Unit) {
    val seleccionUtc = fecha?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
    val hoyUtc = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val calendario = rememberDatePickerState(
        initialSelectedDateMillis = seleccionUtc,
        initialDisplayedMonthMillis = seleccionUtc ?: hoyUtc,
        yearRange = aniosCronograma,
    )
    DatePickerDialog(
        onDismissRequest = cerrar,
        confirmButton = {
            TextButton(
                onClick = {
                    calendario.selectedDateMillis?.let { elegir(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                },
                enabled = calendario.selectedDateMillis != null,
            ) { Text("Aceptar") }
        },
        dismissButton = { TextButton(onClick = cerrar) { Text("Cancelar") } },
    ) { DatePicker(state = calendario) }
}
