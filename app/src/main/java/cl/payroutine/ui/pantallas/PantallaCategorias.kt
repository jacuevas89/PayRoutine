package cl.payroutine.ui.pantallas

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.payroutine.datos.local.entidad.TipoMovimiento
import cl.payroutine.viewmodel.EstadoCategorias

@Composable
internal fun PantallaCategorias(estado: EstadoCategorias, reintentar: () -> Unit, volver: () -> Unit) {
    var ingreso by rememberSaveable { mutableStateOf(false) }
    val tipo = if (ingreso) TipoMovimiento.Ingreso else TipoMovimiento.Gasto
    val categorias = estado.categorias.filter { it.categoria.tipo == tipo }
    Column(Modifier.fillMaxSize()) {
        Cabecera("Categorías", volver)
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
}
