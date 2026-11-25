package cl.payroutine.ui.pantallas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import cl.payroutine.datos.local.entidad.AvisoEmitido
import cl.payroutine.ui.theme.RojoGasto
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val formatoAviso = DateTimeFormatter.ofPattern("d MMM uuuu, HH:mm", Locale.forLanguageTag("es-CL"))

@Composable
internal fun PantallaNotificaciones(
    historial: List<AvisoEmitido>?,
    marcarLeidos: (List<String>) -> Unit,
    eliminar: (String) -> Unit,
    eliminarTodos: () -> Unit,
    volver: () -> Unit,
) {
    var idsMostrados by rememberSaveable { mutableStateOf<ArrayList<String>?>(null) }
    var noLeidosAlMostrar by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
    var confirmar by rememberSaveable { mutableStateOf(false) }

    fun mostrar(avisos: List<AvisoEmitido>) {
        idsMostrados = ArrayList(avisos.map { it.id })
        noLeidosAlMostrar = ArrayList(avisos.filterNot { it.leido }.map { it.id })
        marcarLeidos(noLeidosAlMostrar)
    }
    LaunchedEffect(historial) {
        if (historial != null && idsMostrados == null) mostrar(historial)
    }
    val ids = idsMostrados.orEmpty().toSet()
    val visibles = historial.orEmpty().filter { it.id in ids }
    val nuevas = historial.orEmpty().size - visibles.size

    Column(Modifier.fillMaxSize()) {
        Cabecera("Notificaciones", volver, accion = if (historial.isNullOrEmpty()) null else "Borrar todas",
            alAccionar = { confirmar = true })
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Etiqueta("Aquí aparecen los avisos que ya se activaron. Consulta los futuros en Recordatorios.") }
            if (nuevas > 0 && idsMostrados != null) item {
                BotonPrincipal("Ver avisos nuevos ($nuevas)") { mostrar(historial.orEmpty()) }
            }
            if (historial?.isEmpty() == true) item { Etiqueta("Todavía no tienes avisos.") }
            items(visibles, key = { it.id }) { aviso ->
                Tarjeta(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (aviso.id in noLeidosAlMostrar) Box(
                            Modifier.size(8.dp).background(RojoGasto, CircleShape)
                                .semantics { contentDescription = "Sin leer" },
                        )
                        Column(Modifier.weight(1f)) { Destacado(aviso.titulo) }
                    }
                    Etiqueta(aviso.mensaje)
                    Etiqueta(Instant.ofEpochMilli(aviso.fecha).atZone(ZoneId.systemDefault()).format(formatoAviso))
                    if (!aviso.mostradoEnAndroid) Etiqueta("No se pudo mostrar en Android", RojoGasto)
                    Enlace("Borrar aviso") { eliminar(aviso.id) }
                }
            }
        }
    }
    if (confirmar) AlertDialog(
        onDismissRequest = { confirmar = false },
        title = { Text("¿Borrar todo el historial?") },
        text = { Text("Se borrarán los avisos de esta bandeja. Tus recordatorios seguirán programados.") },
        confirmButton = { TextButton(onClick = { confirmar = false; eliminarTodos() }) { Text("Borrar todas", color = RojoGasto) } },
        dismissButton = { TextButton(onClick = { confirmar = false }) { Text("Cancelar") } },
    )
}
