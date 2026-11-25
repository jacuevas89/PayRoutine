package cl.payroutine.ui.pantallas

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.payroutine.datos.local.entidad.Movimiento
import cl.payroutine.datos.local.entidad.TipoMovimiento
import cl.payroutine.datos.repositorio.NuevoMovimiento
import cl.payroutine.datos.repositorio.validarMontoMovimiento
import cl.payroutine.ui.theme.*
import cl.payroutine.viewmodel.EstadoCategorias
import cl.payroutine.viewmodel.EstadoRegistroMovimiento
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

@Composable
internal fun PantallaNuevoMovimiento(
    estadoCategorias: EstadoCategorias,
    reintentarCategorias: () -> Unit,
    estadoRegistro: EstadoRegistroMovimiento = EstadoRegistroMovimiento(),
    guardar: (NuevoMovimiento) -> Unit = {},
    volver: () -> Unit,
) = FormularioMovimiento(estadoCategorias, reintentarCategorias, estadoRegistro, guardar, volver = volver)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun FormularioMovimiento(
    estadoCategorias: EstadoCategorias,
    reintentarCategorias: () -> Unit,
    estadoRegistro: EstadoRegistroMovimiento,
    guardar: (NuevoMovimiento) -> Unit,
    movimientoInicial: Movimiento? = null,
    soloFechaYNota: Boolean = false,
    volver: () -> Unit,
) {
    val idEdicion = movimientoInicial?.id
    val hoy = LocalDate.now()
    var ingreso by rememberSaveable(idEdicion) { mutableStateOf(movimientoInicial?.tipo == TipoMovimiento.Ingreso) }
    var categoriaGasto by rememberSaveable(idEdicion) {
        mutableStateOf(movimientoInicial?.takeIf { it.tipo == TipoMovimiento.Gasto }?.categoriaId)
    }
    var categoriaIngreso by rememberSaveable(idEdicion) {
        mutableStateOf(movimientoInicial?.takeIf { it.tipo == TipoMovimiento.Ingreso }?.categoriaId)
    }
    var nombre by rememberSaveable(idEdicion) { mutableStateOf(movimientoInicial?.nombre.orEmpty()) }
    var monto by rememberSaveable(idEdicion) {
        val texto = movimientoInicial?.monto?.toString()
        mutableStateOf(texto.orEmpty())
    }
    var nota by rememberSaveable(idEdicion) { mutableStateOf(movimientoInicial?.nota.orEmpty()) }
    var dia by rememberSaveable(idEdicion) {
        val fechaInicial = movimientoInicial?.fecha?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate() ?: hoy
        mutableLongStateOf(fechaInicial.toEpochDay())
    }
    var mostrarCalendario by rememberSaveable { mutableStateOf(false) }
    var intentoGuardar by rememberSaveable { mutableStateOf(false) }
    val fecha = LocalDate.ofEpochDay(dia)
    val montoValidado = validarMontoMovimiento(monto)
    val bloqueado = estadoRegistro.guardando || estadoRegistro.idGuardado != null
    val foco = LocalFocusManager.current
    val teclado = LocalSoftwareKeyboardController.current
    val tipo = if (ingreso) TipoMovimiento.Ingreso else TipoMovimiento.Gasto
    val categorias = estadoCategorias.categorias.map { it.categoria }.filter { it.tipo == tipo }
    val idSeleccionado = if (ingreso) categoriaIngreso else categoriaGasto
    val seleccionada = categorias.firstOrNull { it.id == idSeleccionado }
        ?: categorias.firstOrNull().takeIf { movimientoInicial == null }
    val color = if (ingreso) VerdeIngreso else RojoGasto

    fun cerrarTeclado() {
        foco.clearFocus()
        teclado?.hide()
    }

    BackHandler(enabled = estadoRegistro.guardando) {}
    Column(Modifier.fillMaxSize().imePadding()) {
        Cabecera(
            titulo = when {
                soloFechaYNota -> "Editar fecha y nota"
                movimientoInicial != null -> if (ingreso) "Editar ingreso" else "Editar gasto"
                ingreso -> "Nuevo ingreso"
                else -> "Nuevo gasto"
            },
            volver = {
                if (!bloqueado) {
                    cerrarTeclado()
                    volver()
                }
            },
        )
        Cuerpo(Modifier.weight(1f), espacio = 14) {
            if (movimientoInicial == null) {
                Segmentos(listOf("Gasto", "Ingreso"), if (ingreso) 1 else 0) {
                    if (!bloqueado) ingreso = it == 1
                }
            }
            OutlinedTextField(
                value = nombre, onValueChange = { nombre = it },
                modifier = Modifier.fillMaxWidth(), enabled = !bloqueado,
                readOnly = soloFechaYNota,
                label = { Text("Nombre del movimiento") }, singleLine = true,
                isError = intentoGuardar && nombre.isBlank(),
                supportingText = if (intentoGuardar && nombre.isBlank()) ({ Text("Ingresa el nombre del movimiento") }) else null,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                shape = RoundedCornerShape(12.dp),
            )
            Tarjeta(Modifier.fillMaxWidth(), espacio = 4) {
                CampoMonto(
                    value = monto, onValueChange = { monto = it },
                    modifier = Modifier.fillMaxWidth(), enabled = !bloqueado,
                    readOnly = soloFechaYNota,
                    label = { Text("Monto") }, prefix = { Text("$ ", color = color) },
                    singleLine = true, placeholder = { Text("0") },
                    isError = intentoGuardar && montoValidado.error != null,
                    supportingText = {
                        Text(if (intentoGuardar) montoValidado.error ?: "Pesos chilenos (CLP)" else "Pesos enteros. Ej.: 15000 o 15.000")
                    },
                    textStyle = TextStyle(color = color, fontSize = 30.sp, fontWeight = FontWeight.Bold),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    shape = RoundedCornerShape(12.dp),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Etiqueta("Fecha")
                Tarjeta(
                    Modifier.fillMaxWidth().semantics { contentDescription = "Seleccionar fecha" },
                    alPulsar = if (bloqueado) null else ({ cerrarTeclado(); mostrarCalendario = true }),
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Destacado(fechaEnPantalla(fecha))
                        Icono("calendar", TextoSecundario)
                    }
                }
                if (intentoGuardar && fecha > hoy) Etiqueta("La fecha no puede ser futura", RojoGasto)
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Etiqueta("Categoría")
                when {
                    estadoCategorias.cargando -> Etiqueta("Cargando categorías…")
                    estadoCategorias.errorCarga -> {
                        Etiqueta("No se pudieron cargar las categorías.")
                        BotonPrincipal("Reintentar", alPulsar = reintentarCategorias)
                    }
                    categorias.isEmpty() -> Etiqueta("No hay categorías disponibles.")
                    soloFechaYNota -> Etiqueta(seleccionada?.nombre.orEmpty(), Texto)
                    else -> FlowRow(
                        modifier = Modifier.selectableGroup(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        categorias.forEach { categoria ->
                            Pildora(categoria.nombre, activa = categoria.id == seleccionada?.id) {
                                if (!bloqueado) {
                                    if (ingreso) categoriaIngreso = categoria.id else categoriaGasto = categoria.id
                                }
                            }
                        }
                    }
                }
                if (!estadoCategorias.cargando && !estadoCategorias.errorCarga && categorias.isNotEmpty() && seleccionada == null) {
                    Etiqueta("Selecciona una categoría.", RojoGasto)
                }
            }
            OutlinedTextField(
                value = nota, onValueChange = { nota = it },
                modifier = Modifier.fillMaxWidth(), enabled = !bloqueado,
                label = { Text("Nota (opcional)") }, minLines = 2, maxLines = 4,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                shape = RoundedCornerShape(12.dp),
            )
        }
        estadoRegistro.error?.let { Etiqueta(it, RojoGasto, Modifier.padding(horizontal = 16.dp)) }
        BotonPrincipal(
            when {
                estadoRegistro.guardando -> "Guardando…"
                movimientoInicial != null -> "Guardar cambios"
                ingreso -> "Guardar ingreso"
                else -> "Guardar gasto"
            },
            modifier = Modifier.padding(16.dp),
            habilitado = !bloqueado && !estadoCategorias.cargando && !estadoCategorias.errorCarga && seleccionada != null,
        ) {
            intentoGuardar = true
            val valor = montoValidado.valor
            val categoria = seleccionada
            if (nombre.isNotBlank() && valor != null && fecha <= LocalDate.now() && categoria != null) {
                cerrarTeclado()
                guardar(NuevoMovimiento(nombre, tipo, valor, fecha, categoria.id, nota))
            }
        }
    }

    if (mostrarCalendario && !bloqueado) {
        val calendario = rememberDatePickerState(
            initialSelectedDateMillis = fecha.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate() <= hoy
                override fun isSelectableYear(year: Int): Boolean = year <= hoy.year
            },
        )
        DatePickerDialog(
            onDismissRequest = { mostrarCalendario = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        calendario.selectedDateMillis?.let {
                            dia = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()
                        }
                        mostrarCalendario = false
                    },
                    enabled = calendario.selectedDateMillis != null,
                ) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { mostrarCalendario = false }) { Text("Cancelar") } },
        ) { DatePicker(state = calendario) }
    }
}
