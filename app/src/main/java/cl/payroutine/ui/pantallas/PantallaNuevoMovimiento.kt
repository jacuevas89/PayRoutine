package cl.payroutine.ui.pantallas

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.payroutine.ui.theme.TextoSecundario

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PantallaNuevoMovimiento(ingreso: Boolean, volver: () -> Unit, cambiarTipo: (Boolean) -> Unit) {
    val muestra = if (ingreso) DatosDeEjemplo.formularioIngreso else DatosDeEjemplo.formularioGasto
    val fecha = "29 de octubre de 2026"
    Column(Modifier.fillMaxSize()) {
        Cabecera(muestra.titulo, volver)
        Cuerpo(Modifier.weight(1f), espacio = 14) {
            Segmentos(listOf("Gasto", "Ingreso"), if (ingreso) 1 else 0) { cambiarTipo(it == 1) }
            Tarjeta(Modifier.fillMaxWidth(), espacio = 4) {
                Etiqueta("Monto")
                Cifra(muestra.monto, muestra.color, 34)
                Etiqueta("El monto debe ser mayor que cero")
            }
            CampoDeEjemplo("Fecha", fecha, "calendar")
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Etiqueta("Categoría")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    muestra.categorias.forEachIndexed { indice, categoria -> Pildora(categoria, activa = indice == 0) }
                }
            }
            CampoDeEjemplo("Nota (opcional)", muestra.nota, color = TextoSecundario)
        }
        PieFormulario(muestra.guardar)
    }
}
