package cl.payroutine.ui.pantallas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
            Etiqueta("PayRoutine · Versión 1.0 (prototipo)")
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
