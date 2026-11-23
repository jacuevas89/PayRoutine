package cl.payroutine.ui.pantallas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import cl.payroutine.datos.local.entidad.TipoMovimiento
import cl.payroutine.ui.theme.RojoGasto
import cl.payroutine.viewmodel.EstadoCategorias
import cl.payroutine.viewmodel.EstadoCreacionCategoria

@Composable
internal fun PantallaCategorias(
    estado: EstadoCategorias,
    reintentar: () -> Unit,
    crear: (String, TipoMovimiento) -> Unit,
    limpiarCreacion: () -> Unit,
    volver: () -> Unit,
) {
    var ingreso by rememberSaveable { mutableStateOf(false) }
    var mostrarFormulario by rememberSaveable { mutableStateOf(false) }
    var nuevoNombre by rememberSaveable { mutableStateOf("") }
    var nuevoIngreso by rememberSaveable { mutableStateOf(false) }
    val tipo = if (ingreso) TipoMovimiento.Ingreso else TipoMovimiento.Gasto
    val categorias = estado.categorias.filter { it.categoria.tipo == tipo }
    val guardando = estado.creacion == EstadoCreacionCategoria.Guardando

    LaunchedEffect(estado.creacion, mostrarFormulario) {
        if (mostrarFormulario && estado.creacion == EstadoCreacionCategoria.Guardada) {
            ingreso = nuevoIngreso
            mostrarFormulario = false
            limpiarCreacion()
        }
    }

    Column(Modifier.fillMaxSize()) {
        Cabecera(
            titulo = "Categorías",
            volver = volver,
            accion = if (estado.cargando || estado.errorCarga) null else "Nueva categoría",
            alAccionar = {
                limpiarCreacion()
                nuevoNombre = ""
                nuevoIngreso = ingreso
                mostrarFormulario = true
            },
        )
        Cuerpo(Modifier.weight(1f), espacio = 10) {
            Segmentos(listOf("Gastos", "Ingresos"), if (ingreso) 1 else 0) { ingreso = it == 1 }
            when {
                estado.cargando -> Etiqueta("Cargando categorías…")
                estado.errorCarga -> {
                    Etiqueta("No se pudieron cargar las categorías.")
                    BotonPrincipal("Reintentar", alPulsar = reintentar)
                }
                categorias.isEmpty() -> Etiqueta("No hay categorías disponibles.")
                else -> categorias.forEach { fila ->
                    val apariencia = aparienciaCategoria(fila.categoria)
                    val cantidad = if (fila.cantidadMovimientos == 1) "1 movimiento" else "${fila.cantidadMovimientos} movimientos"
                    Tarjeta(Modifier.fillMaxWidth(), relleno = 12) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Insignia(apariencia.icono, apariencia.color, apariencia.fondo)
                            Destacado(fila.categoria.nombre, modifier = Modifier.weight(1f))
                            Etiqueta(cantidad)
                        }
                    }
                }
            }
        }
    }

    if (mostrarFormulario) {
        fun cerrar() {
            if (guardando || estado.creacion == EstadoCreacionCategoria.Guardada) return
            mostrarFormulario = false
            limpiarCreacion()
        }
        val duplicada = estado.creacion == EstadoCreacionCategoria.Duplicada
        AlertDialog(
            onDismissRequest = ::cerrar,
            title = { Text("Nueva categoría") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Segmentos(listOf("Gasto", "Ingreso"), if (nuevoIngreso) 1 else 0) {
                        if (!guardando) {
                            nuevoIngreso = it == 1
                            limpiarCreacion()
                        }
                    }
                    OutlinedTextField(
                        value = nuevoNombre,
                        onValueChange = {
                            nuevoNombre = it
                            limpiarCreacion()
                        },
                        label = { Text("Nombre de la categoría") },
                        singleLine = true,
                        enabled = !guardando,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        isError = duplicada,
                        supportingText = if (duplicada) ({ Text("Ya existe una categoría con ese nombre para este tipo.") }) else null,
                    )
                    if (estado.creacion == EstadoCreacionCategoria.Error) {
                        Etiqueta("No se pudo guardar la categoría. Vuelve a intentarlo.", RojoGasto)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = nuevoNombre.isNotBlank() && !guardando && estado.creacion != EstadoCreacionCategoria.Guardada,
                    onClick = { crear(nuevoNombre, if (nuevoIngreso) TipoMovimiento.Ingreso else TipoMovimiento.Gasto) },
                ) { Text(if (guardando) "Guardando…" else "Guardar categoría") }
            },
            dismissButton = { TextButton(enabled = !guardando, onClick = ::cerrar) { Text("Cancelar") } },
        )
    }
}
