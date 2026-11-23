package cl.payroutine.ui.pantallas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cl.payroutine.datos.local.dao.MovimientoConCategoria
import cl.payroutine.datos.local.entidad.TipoMovimiento
import cl.payroutine.ui.theme.*
import cl.payroutine.viewmodel.DatosInicio
import cl.payroutine.viewmodel.EstadoCategorias
import cl.payroutine.viewmodel.FiltroMovimientos
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PantallaMovimientos(
    datos: DatosInicio?,
    estadoCategorias: EstadoCategorias,
    verMovimiento: (Int) -> Unit,
) {
    var tipo by rememberSaveable { mutableStateOf<TipoMovimiento?>(null) }
    var categoriaId by rememberSaveable { mutableStateOf<Int?>(null) }
    var desdeMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    var hastaMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    var mostrarCalendario by rememberSaveable { mutableStateOf(false) }
    var mostrarCategorias by remember { mutableStateOf(false) }
    val categorias = remember(estadoCategorias.categorias, tipo) {
        estadoCategorias.categorias.map { it.categoria }.filter { tipo == null || it.tipo == tipo }
    }
    val categoria = categorias.find { it.id == categoriaId }
    val filtro = FiltroMovimientos(tipo, categoriaId, desdeMillis?.let(::diaDelCalendario), hastaMillis?.let(::diaDelCalendario))
    val filas = remember(datos?.movimientos, datos?.fecha?.zona, filtro) {
        datos?.let { filtro.aplicar(it.movimientos, it.fecha.zona) }.orEmpty()
    }

    fun seleccionarTipo(nuevoTipo: TipoMovimiento?) {
        if (nuevoTipo != null && categoria?.tipo != nuevoTipo) categoriaId = null
        tipo = nuevoTipo
    }

    Column {
        Cabecera("Movimientos")
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 72.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FlowRow(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Pildora("Todos", activa = tipo == null) { seleccionarTipo(null) }
                        Pildora("Ingresos", activa = tipo == TipoMovimiento.Ingreso) { seleccionarTipo(TipoMovimiento.Ingreso) }
                        Pildora("Gastos", activa = tipo == TipoMovimiento.Gasto) { seleccionarTipo(TipoMovimiento.Gasto) }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(0.4f)) {
                            Pildora(
                                texto = categoria?.nombre ?: "Categoría",
                                activa = categoriaId != null,
                                modifier = Modifier.fillMaxWidth(),
                                unaLinea = true,
                                rol = Role.Button,
                                habilitada = categorias.isNotEmpty() || categoriaId != null,
                            ) { mostrarCategorias = true }
                            DropdownMenu(
                                expanded = mostrarCategorias,
                                onDismissRequest = { mostrarCategorias = false },
                                modifier = Modifier.heightIn(max = 320.dp),
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Todas las categorías") },
                                    onClick = { categoriaId = null; mostrarCategorias = false },
                                )
                                categorias.forEach { opcion ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(if (tipo == null) "${opcion.nombre} · ${opcion.tipo.name}" else opcion.nombre)
                                        },
                                        onClick = {
                                            categoriaId = opcion.id
                                            mostrarCategorias = false
                                        },
                                    )
                                }
                            }
                        }
                        val desde = filtro.desde
                        val hasta = filtro.hasta
                        val rangoSeleccionado = desde != null && hasta != null
                        Pildora(
                            texto = if (desde != null && hasta != null) rangoEnPantalla(desde, hasta) else "Rango de fechas",
                            activa = rangoSeleccionado,
                            modifier = Modifier.weight(0.6f),
                            unaLinea = true,
                            rol = Role.Button,
                        ) { mostrarCalendario = true }
                    }
                    if (tipo != null || categoriaId != null || desdeMillis != null) {
                        TextButton(onClick = {
                            tipo = null
                            categoriaId = null
                            desdeMillis = null
                            hastaMillis = null
                        }) { Text("Limpiar filtros") }
                    }
                }
            }
            when {
                datos == null -> item { Etiqueta("Cargando movimientos…") }
                datos.movimientos.isEmpty() -> item { Etiqueta("Todavía no tienes movimientos registrados.") }
                filas.isEmpty() -> item { Etiqueta("No hay movimientos que coincidan con los filtros.") }
                else -> items(filas, key = { it.movimiento.id }) { fila ->
                    TarjetaMovimiento(fila, datos.fecha.zona) { verMovimiento(fila.movimiento.id) }
                }
            }
        }
    }

    if (mostrarCalendario) {
        SelectorRangoMovimientos(desdeMillis, hastaMillis, cerrar = { mostrarCalendario = false }) { desde, hasta ->
            desdeMillis = desde
            hastaMillis = hasta
            mostrarCalendario = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorRangoMovimientos(
    inicio: Long?,
    fin: Long?,
    cerrar: () -> Unit,
    aplicar: (Long?, Long?) -> Unit,
) {
    val calendario = rememberDateRangePickerState(initialSelectedStartDateMillis = inicio, initialSelectedEndDateMillis = fin)
    Dialog(onDismissRequest = cerrar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
                Cabecera(titulo = "Rango de fechas", volver = cerrar)
                DateRangePicker(state = calendario, modifier = Modifier.weight(1f), title = null)
                HorizontalDivider()
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                ) {
                    if (inicio != null) TextButton(onClick = { aplicar(null, null) }) { Text("Quitar rango") }
                    TextButton(
                        enabled = calendario.selectedStartDateMillis != null && calendario.selectedEndDateMillis != null,
                        onClick = { aplicar(calendario.selectedStartDateMillis, calendario.selectedEndDateMillis) },
                    ) { Text("Aplicar rango") }
                }
            }
        }
    }
}

private fun diaDelCalendario(utcMillis: Long): LocalDate =
    Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()

@Composable
internal fun TarjetaMovimiento(fila: MovimientoConCategoria, zona: ZoneId, alPulsar: () -> Unit) {
    val ingreso = fila.esIngreso()
    val color = if (ingreso) VerdeIngreso else RojoGasto
    val fondo = if (ingreso) FondoIngreso else FondoGasto
    FilaTarjeta(fila.titulo(), "${fila.categoria} · ${fila.fechaEnPantalla(zona)}", fila.montoConSigno(),
        if (ingreso) "down" else "up", color, fondo, color, alPulsar)
}
