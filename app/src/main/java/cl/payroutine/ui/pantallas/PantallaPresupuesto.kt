package cl.payroutine.ui.pantallas

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cl.payroutine.ui.theme.*

@Composable
internal fun PantallaPresupuesto(editar: () -> Unit) {
    val datos = DatosDeEjemplo
    val porcentajeTotal = 93
    Column {
        Cabecera("Presupuesto mensual", accion = "Editar", alAccionar = editar)
        Cuerpo {
            Tarjeta(Modifier.fillMaxWidth(), espacio = 7) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Etiqueta(datos.mes)
                    Etiqueta("$porcentajeTotal% usado", NaranjaAlerta)
                }
                Cifra("${datos.gastos} de ${datos.presupuestoTotal}", tamano = 21)
                Progreso(porcentajeTotal, NaranjaAlerta)
                Etiqueta("Te quedan ${datos.presupuestoDisponible} este mes")
            }
            datos.presupuestos.forEach { presupuesto ->
                val porcentaje = "${presupuesto.porcentaje}%"
                val excedido = presupuesto.porcentaje > 100
                Tarjeta(Modifier.fillMaxWidth(), relleno = 13, espacio = 8) {
                    FilaDato(presupuesto.categoria, "${presupuesto.gastado} de ${presupuesto.limite}",
                        if (excedido) "$porcentaje · Excedido" else porcentaje, presupuesto.icono,
                        presupuesto.color, presupuesto.fondo, if (presupuesto.porcentaje >= 90) presupuesto.color else Texto)
                    Progreso(presupuesto.porcentaje, presupuesto.color)
                }
            }
        }
    }
}

@Composable
internal fun PantallaEditarPresupuesto(volver: () -> Unit) {
    val datos = DatosDeEjemplo
    Column(Modifier.fillMaxSize()) {
        Cabecera("Editar presupuesto", volver)
        Cuerpo(Modifier.weight(1f), espacio = 10) {
            Tarjeta(Modifier.fillMaxWidth(), color = AzulNoche) {
                Etiqueta("Presupuesto total · ${datos.mes}", TextoSobreAzul)
                Cifra(datos.presupuestoTotal, Color.White)
            }
            datos.presupuestos.forEach { presupuesto ->
                Tarjeta(Modifier.fillMaxWidth(), relleno = 12) {
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Insignia(presupuesto.icono, presupuesto.color, presupuesto.fondo)
                        Column(Modifier.weight(1f)) {
                            Destacado(presupuesto.categoria)
                            Etiqueta("Gastado ${presupuesto.gastado}")
                        }
                        Destacado("−")
                        Destacado(presupuesto.limite)
                        Destacado("+")
                    }
                }
            }
        }
        PieFormulario("Guardar presupuesto")
    }
}
