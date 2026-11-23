package cl.payroutine.ui.pantallas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import cl.payroutine.ui.theme.*

@Composable
internal fun PantallaAjustes(
    apodo: String,
    nombreCompleto: String,
    volver: () -> Unit,
    editarPerfil: () -> Unit,
    verCategorias: () -> Unit,
    verRecordatorios: () -> Unit,
    verConsejos: () -> Unit,
    verDatosYPrivacidad: () -> Unit,
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
                        Etiqueta(nombreCompleto)
                        Etiqueta("Tus datos se guardan en este teléfono")
                        Enlace("Editar perfil", editarPerfil)
                    }
                }
            }
            FilaAjustes("Categorías", "tag", VerdeMarca, FondoMarca, alPulsar = verCategorias)
            FilaAjustes("Recordatorios", "bell", NaranjaAlerta, FondoAlerta, alPulsar = verRecordatorios)
            FilaAjustes("Consejos de ahorro", "bulb", AzulNoche, FondoSelector, alPulsar = verConsejos)
            FilaAjustes("Moneda", "coin", VerdeMarca, FondoMarca, "CLP $")
            FilaAjustes("Tema", "moon", AzulNoche, FondoSelector, "Claro")
            FilaAjustes("Datos y privacidad", "list", AzulNoche, FondoSelector, alPulsar = verDatosYPrivacidad)
            Etiqueta("PayRoutine · Versión 1.0 (prototipo)")
        }
    }
}

@Composable
internal fun FilaAjustes(
    titulo: String,
    icono: String,
    color: Color,
    fondo: Color,
    detalle: String? = null,
    habilitada: Boolean = true,
    colorTitulo: Color = Texto,
    alPulsar: (() -> Unit)? = null,
) {
    val estado = if (habilitada) Modifier else Modifier.semantics(mergeDescendants = true) { disabled() }
    Tarjeta(Modifier.fillMaxWidth().then(estado), relleno = 12, alPulsar = alPulsar.takeIf { habilitada }) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Insignia(icono, color, fondo)
            Column(Modifier.weight(1f)) {
                Destacado(titulo, if (habilitada) colorTitulo else TextoSecundario)
                if (!habilitada && detalle != null) Etiqueta(detalle)
            }
            if (habilitada && detalle != null) Etiqueta(detalle)
            if (habilitada) Icono("next", TextoSecundario, Modifier.size(16.dp))
        }
    }
}
