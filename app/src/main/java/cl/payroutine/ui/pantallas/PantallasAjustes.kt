package cl.payroutine.ui.pantallas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cl.payroutine.ui.theme.*

@Composable
internal fun PantallaAjustes(
    apodo: String,
    volver: () -> Unit,
    verCategorias: () -> Unit,
    verRecordatorios: () -> Unit,
    verConsejos: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Cabecera("Ajustes", volver)
        Cuerpo(Modifier.weight(1f), espacio = 10) {
            Tarjeta(Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(56.dp).background(VerdeMarca, CircleShape), contentAlignment = Alignment.Center) {
                        Cifra(apodo.take(1).uppercase(), Color.White, 24)
                    }
                    Column(Modifier.weight(1f)) {
                        Cifra(apodo, tamano = 17)
                        Etiqueta("Tus datos se guardan en este teléfono")
                    }
                }
            }
            FilaAjustes("Categorías", "tag", VerdeMarca, FondoMarca, alPulsar = verCategorias)
            FilaAjustes("Recordatorios", "bell", NaranjaAlerta, FondoAlerta, alPulsar = verRecordatorios)
            FilaAjustes("Consejos de ahorro", "bulb", AzulNoche, FondoSelector, alPulsar = verConsejos)
            FilaAjustes("Moneda", "coin", VerdeMarca, FondoMarca, "CLP $")
            FilaAjustes("Tema", "moon", AzulNoche, FondoSelector, "Claro")
            FilaAjustes("Respaldo de datos", "download", AzulNoche, FondoSelector, "Exportar")
            Etiqueta("Gestor de vida · versión 1.0 (prototipo)")
        }
    }
}

@Composable
internal fun PantallaCategorias(volver: () -> Unit) {
    val gastos = DatosDeEjemplo.formularioGasto.categorias
    Column(Modifier.fillMaxSize()) {
        Cabecera("Categorías", volver, accion = "Nueva")
        Cuerpo(Modifier.weight(1f), espacio = 10) {
            Segmentos(listOf("Gastos", "Ingresos"), activa = 0)
            FilaAjustes(gastos[0], "cart", VerdeMarca, FondoMarca, "12 mov.")
            FilaAjustes(gastos[1], "bus", AzulNoche, FondoSelector, "9 mov.")
            FilaAjustes(gastos[2], "house", NaranjaAlerta, FondoAlerta, "3 mov.")
            FilaAjustes(gastos[3], "bolt", VerdeMarca, FondoMarca, "4 mov.")
            FilaAjustes(gastos[4], "play", RojoGasto, FondoGasto, "6 mov.")
            FilaAjustes(gastos[5], "heart", AzulNoche, FondoSelector, "1 mov.")
            Surface(
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                border = BorderStroke(1.dp, VerdeMarca),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icono("plus", VerdeMarca, Modifier.size(16.dp))
                    Destacado("Nueva categoría", VerdeMarca)
                }
            }
        }
    }
}

@Composable
internal fun PantallaConsejosAhorro(volver: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Cabecera("Consejos de ahorro", volver)
        Cuerpo(Modifier.weight(1f)) {
            Etiqueta("Ideas simples para ordenar tu mes.")
            DatosDeEjemplo.consejosAhorro.forEach { (titulo, detalle) ->
                FilaTarjeta(titulo, detalle, "", "bulb", VerdeMarca, FondoMarca)
            }
        }
        Etiqueta(
            "Consejos generales, no son recomendaciones personalizadas.",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun FilaAjustes(
    titulo: String,
    icono: String,
    color: Color,
    fondo: Color,
    detalle: String? = null,
    alPulsar: (() -> Unit)? = null,
) {
    Tarjeta(Modifier.fillMaxWidth(), relleno = 12, alPulsar = alPulsar) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Insignia(icono, color, fondo)
            Destacado(titulo, modifier = Modifier.weight(1f))
            if (detalle != null) Etiqueta(detalle)
            Icono("next", TextoSecundario, Modifier.size(16.dp))
        }
    }
}
