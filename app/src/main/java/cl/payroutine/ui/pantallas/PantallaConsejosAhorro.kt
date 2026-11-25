package cl.payroutine.ui.pantallas

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.payroutine.datos.local.entidad.ConsejoAhorro
import cl.payroutine.ui.theme.FondoMarca
import cl.payroutine.ui.theme.VerdeMarca

@Composable
internal fun PantallaConsejosAhorro(consejos: List<ConsejoAhorro>, volver: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Cabecera("Consejos de ahorro", volver)
        Cuerpo(Modifier.weight(1f)) {
            Etiqueta("Ideas simples para ordenar tu mes.")
            consejos.forEach { consejo ->
                FilaTarjeta(consejo.titulo, consejo.detalle, "", "bulb", VerdeMarca, FondoMarca)
            }
        }
        Etiqueta(
            "Consejos generales, no son recomendaciones personalizadas.",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}
