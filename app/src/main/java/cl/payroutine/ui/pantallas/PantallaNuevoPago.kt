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
import cl.payroutine.datos.repositorio.NuevoPagoUnico
import cl.payroutine.datos.repositorio.validarMontoMovimiento
import cl.payroutine.ui.theme.RojoGasto
import cl.payroutine.ui.theme.TextoSecundario
import cl.payroutine.viewmodel.EstadoCategorias
import cl.payroutine.viewmodel.EstadoRegistroPago
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun PantallaNuevoPago(
    estadoCategorias: EstadoCategorias,
    reintentarCategorias: () -> Unit,
    estadoRegistro: EstadoRegistroPago,
    guardar: (NuevoPagoUnico) -> Unit,
    volver: () -> Unit,
) {
    var nombre by rememberSaveable { mutableStateOf("") }
    var monto by rememberSaveable { mutableStateOf("") }
    var fechaMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    var categoriaId by rememberSaveable { mutableStateOf<Int?>(null) }
    var mostrarCalendario by rememberSaveable { mutableStateOf(false) }
    var intentoGuardar by rememberSaveable { mutableStateOf(false) }
    val categorias = estadoCategorias.categorias.map { it.categoria }.filter { it.tipo == TipoMovimiento.Gasto }
    val categoria = categorias.firstOrNull { it.id == categoriaId }
    val fecha = fechaMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
    val montoValidado = validarMontoMovimiento(monto)
    val bloqueado = estadoRegistro.guardando || estadoRegistro.idGuardado != null
    val foco = LocalFocusManager.current
    val teclado = LocalSoftwareKeyboardController.current

    fun cerrarTeclado() {
        foco.clearFocus()
        teclado?.hide()
    }

    BackHandler(enabled = estadoRegistro.guardando) {}
    Column(Modifier.fillMaxSize().imePadding()) {
        Cabecera("Nuevo pago pendiente", volver = {
            if (!bloqueado) {
                cerrarTeclado()
                volver()
            }
        })
        Cuerpo(Modifier.weight(1f), espacio = 14) {
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
                value = monto, onValueChange = { monto = it },
                modifier = Modifier.fillMaxWidth(), enabled = !bloqueado, singleLine = true,
                label = { Text("Monto") }, prefix = { Text("$ ") },
                isError = intentoGuardar && montoValidado.error != null,
                supportingText = { Text(if (intentoGuardar) montoValidado.error ?: "Pesos chilenos (CLP)" else "Pesos enteros (CLP)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                shape = RoundedCornerShape(12.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Etiqueta("Fecha de vencimiento")
                Tarjeta(
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Seleccionar fecha de vencimiento" },
                    alPulsar = if (bloqueado) null else ({ cerrarTeclado(); mostrarCalendario = true }),
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Destacado(fecha?.let(::fechaEnPantalla) ?: "Seleccionar fecha")
                        Icono("calendar", TextoSecundario)
                    }
                }
                if (intentoGuardar && fecha == null) Etiqueta("Selecciona la fecha de vencimiento", RojoGasto)
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
        estadoRegistro.error?.let { Etiqueta(it, RojoGasto, Modifier.padding(horizontal = 16.dp)) }
        BotonPrincipal(
            if (estadoRegistro.guardando) "Guardando…" else "Guardar pago pendiente",
            modifier = Modifier.padding(16.dp),
            habilitado = !bloqueado && !estadoCategorias.cargando && !estadoCategorias.errorCarga && categorias.isNotEmpty(),
        ) {
            intentoGuardar = true
            val valor = montoValidado.valor
            if (nombre.isNotBlank() && valor != null && fecha != null && categoria != null) {
                cerrarTeclado()
                guardar(NuevoPagoUnico(nombre, valor, fecha, categoria.id))
            }
        }
    }

    if (mostrarCalendario && !bloqueado) {
        val hoyUtc = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val calendario = rememberDatePickerState(
            initialSelectedDateMillis = fechaMillis,
            initialDisplayedMonthMillis = fechaMillis ?: hoyUtc,
        )
        DatePickerDialog(
            onDismissRequest = { mostrarCalendario = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        fechaMillis = calendario.selectedDateMillis
                        mostrarCalendario = false
                    },
                    enabled = calendario.selectedDateMillis != null,
                ) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { mostrarCalendario = false }) { Text("Cancelar") } },
        ) { DatePicker(state = calendario) }
    }
}
