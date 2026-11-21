package cl.payroutine.ui.pantallas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.payroutine.ui.theme.TextoSecundario
import cl.payroutine.datos.local.entidad.TipoMovimiento
import cl.payroutine.viewmodel.EstadoCategorias

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PantallaNuevoMovimiento(
    estadoCategorias: EstadoCategorias,
    reintentarCategorias: () -> Unit,
    volver: () -> Unit,
) {
    var ingreso by rememberSaveable { mutableStateOf(false) }
    var categoriaGasto by rememberSaveable { mutableStateOf<Int?>(null) }
    var categoriaIngreso by rememberSaveable { mutableStateOf<Int?>(null) }
    val tipo = if (ingreso) TipoMovimiento.Ingreso else TipoMovimiento.Gasto
    val categorias = estadoCategorias.categorias.map { it.categoria }.filter { it.tipo == tipo }
    val idSeleccionado = if (ingreso) categoriaIngreso else categoriaGasto
    val seleccionada = categorias.firstOrNull { it.id == idSeleccionado } ?: categorias.firstOrNull()
    val muestra = if (ingreso) DatosDeEjemplo.formularioIngreso else DatosDeEjemplo.formularioGasto
    val fecha = "29 de octubre de 2026"
    Column(Modifier.fillMaxSize()) {
        Cabecera(muestra.titulo, volver)
        Cuerpo(Modifier.weight(1f), espacio = 14) {
            Segmentos(listOf("Gasto", "Ingreso"), if (ingreso) 1 else 0) { ingreso = it == 1 }
            Tarjeta(Modifier.fillMaxWidth(), espacio = 4) {
                Etiqueta("Monto")
                Cifra(muestra.monto, muestra.color, 34)
                Etiqueta("El monto debe ser mayor que cero")
            }
            CampoDeEjemplo("Fecha", fecha, "calendar")
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Etiqueta("Categoría")
                when {
                    estadoCategorias.cargando -> Etiqueta("Cargando categorías…")
                    estadoCategorias.errorCarga -> {
                        Etiqueta("No se pudieron cargar las categorías.")
                        BotonPrincipal("Reintentar", alPulsar = reintentarCategorias)
                    }
                    categorias.isEmpty() -> Etiqueta("No hay categorías disponibles.")
                    else -> FlowRow(
                        modifier = Modifier.selectableGroup(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        categorias.forEach { categoria ->
                            Pildora(categoria.nombre, activa = categoria.id == seleccionada?.id) {
                                if (ingreso) categoriaIngreso = categoria.id else categoriaGasto = categoria.id
                            }
                        }
                    }
                }
            }
            CampoDeEjemplo("Nota (opcional)", muestra.nota, color = TextoSecundario)
        }
        PieFormulario(muestra.guardar)
    }
}
