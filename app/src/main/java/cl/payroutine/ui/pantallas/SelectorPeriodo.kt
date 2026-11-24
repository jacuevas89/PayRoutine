package cl.payroutine.ui.pantallas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import cl.payroutine.datos.repositorio.aniosCronograma
import cl.payroutine.ui.theme.FondoMarca
import java.time.Month
import java.time.YearMonth

@Composable
internal fun SelectorPeriodo(periodo: YearMonth, cerrar: () -> Unit, elegir: (YearMonth) -> Unit) {
    var anio by rememberSaveable(periodo) { mutableIntStateOf(periodo.year) }
    var mes by rememberSaveable(periodo) { mutableStateOf(periodo.month) }
    val anios = remember(periodo.year) { (aniosCronograma + periodo.year).distinct().sortedDescending() }

    AlertDialog(
        onDismissRequest = cerrar,
        title = { Text("Elegir mes y año") },
        containerColor = Color.White,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CampoPeriodo("Año", anio, anios, Int::toString) { anio = it }
                CampoPeriodo("Mes", mes, Month.entries, ::nombreMesEnPantalla) { mes = it }
            }
        },
        confirmButton = { TextButton(onClick = { elegir(YearMonth.of(anio, mes)) }) { Text("Aplicar") } },
        dismissButton = { TextButton(onClick = cerrar) { Text("Cancelar") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> CampoPeriodo(
    etiqueta: String,
    seleccionado: T,
    opciones: List<T>,
    formato: (T) -> String,
    elegir: (T) -> Unit,
) {
    var abierto by rememberSaveable { mutableStateOf(false) }
    val alturaOpcion = 48.dp
    val alturaOpcionPx = with(LocalDensity.current) { alturaOpcion.roundToPx() }
    val posicion = (opciones.indexOf(seleccionado) - 1).coerceAtLeast(0) * alturaOpcionPx
    val desplazamiento = rememberScrollState(posicion)
    LaunchedEffect(abierto, posicion) {
        if (abierto) desplazamiento.scrollTo(posicion)
    }

    ExposedDropdownMenuBox(expanded = abierto, onExpandedChange = { abierto = it }) {
        OutlinedTextField(
            value = formato(seleccionado),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(etiqueta) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = abierto) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .semantics { contentDescription = "Elegir ${etiqueta.lowercase()}" },
        )
        ExposedDropdownMenu(
            expanded = abierto,
            onDismissRequest = { abierto = false },
            modifier = Modifier.heightIn(max = 256.dp),
            scrollState = desplazamiento,
            shape = RoundedCornerShape(12.dp),
            containerColor = Color.White,
        ) {
            opciones.forEach { opcion ->
                DropdownMenuItem(
                    text = { Text(formato(opcion)) },
                    onClick = {
                        abierto = false
                        elegir(opcion)
                    },
                    modifier = Modifier.height(alturaOpcion)
                        .background(if (opcion == seleccionado) FondoMarca else Color.Transparent),
                )
            }
        }
    }
}
