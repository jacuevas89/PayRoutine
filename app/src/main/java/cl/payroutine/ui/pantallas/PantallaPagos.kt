package cl.payroutine.ui.pantallas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cl.payroutine.ui.theme.*

@Composable
internal fun PantallaPagos(verCalendario: () -> Unit, verHistorial: () -> Unit, verCuota: () -> Unit) {
    val datos = DatosDeEjemplo
    Column {
        Cabecera("Pagos pendientes", accion = "Calendario", alAccionar = verCalendario)
        Cuerpo {
            Segmentos(listOf("Pendientes (3)", "Pagados"), 0) { if (it == 1) verHistorial() }
            Tarjeta(Modifier.fillMaxWidth(), color = AzulNoche) {
                Etiqueta("Por pagar este mes", TextoSobreAzul)
                Cifra(datos.porPagar, Color.White)
            }
            datos.pagos.forEachIndexed { indice, pago ->
                Tarjeta(Modifier.fillMaxWidth(), relleno = 13, espacio = 10, alPulsar = if (indice == 1) verCuota else null) {
                    FilaDato(pago.nombre, pago.fecha, pago.monto, pago.icono, pago.color, pago.fondo)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        EstadoPago(pago.plazo, pago.color, pago.fondo)
                        Surface(shape = RoundedCornerShape(10.dp), color = Color.White, border = BorderStroke(1.dp, VerdeMarca)) {
                            Destacado("Marcar pagado", VerdeMarca, Modifier.padding(horizontal = 12.dp, vertical = 7.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun PantallaNuevoPago(volver: () -> Unit) {
    val pago = DatosDeEjemplo.pagos[1]
    Column(Modifier.fillMaxSize()) {
        Cabecera("Nuevo pago pendiente", volver)
        Cuerpo(Modifier.weight(1f), espacio = 14) {
            CampoDeEjemplo("Nombre del pago", "Cuota notebook")
            CampoDeEjemplo("Monto", pago.monto)
            CampoDeEjemplo("Fecha de vencimiento", "05 de noviembre de 2026", "calendar")
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Etiqueta("Tipo de obligación")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pildora("Pago único")
                    Pildora("Cuota", activa = true)
                    Pildora("Tarjeta")
                }
            }
            CampoDeEjemplo("Cuotas", "4 de 12")
            Tarjeta(Modifier.fillMaxWidth(), relleno = 14) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Destacado("Recordarme")
                        Etiqueta("1 día antes del vencimiento")
                    }
                    Switch(checked = true, onCheckedChange = null)
                }
            }
            Etiqueta("Un pago pendiente no se cuenta como gasto hasta que lo marques como pagado.")
        }
        PieFormulario("Guardar pago pendiente")
    }
}

@Composable
internal fun PantallaHistorialPagos(volver: () -> Unit) {
    val pagos = DatosDeEjemplo.pagos
    val historial = listOf(
        Triple(pagos[0].nombre, "Pagado 02 oct", pagos[0].monto),
        Triple("Cuota notebook (3 de 12)", "Pagado 05 oct", pagos[1].monto),
        Triple(pagos[2].nombre, "Pagado 10 oct", pagos[2].monto),
        Triple("Cuenta de luz", "Pagado 15 oct", "$45.200"),
    )
    Column {
        Cabecera("Historial de pagos", volver)
        Cuerpo {
            Segmentos(listOf("Pendientes", "Pagados"), 1) { if (it == 0) volver() }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pildora("Octubre", activa = true)
                Pildora("Septiembre")
                Pildora("Agosto")
            }
            Tarjeta(Modifier.fillMaxWidth()) {
                Etiqueta("Pagado en octubre")
                Cifra("$255.190", VerdeIngreso, 24)
                Etiqueta("4 pagos realizados")
            }
            historial.forEach { (nombre, fecha, monto) ->
                FilaTarjeta(nombre, fecha, monto, "check", VerdeIngreso, FondoIngreso)
            }
        }
    }
}

@Composable
internal fun PantallaDetalleCuota(volver: () -> Unit) {
    val fechas = listOf("05 oct", "05 nov", "05 dic", "05 ene")
    Column(Modifier.fillMaxSize()) {
        Cabecera("Cuota notebook", volver)
        Cuerpo(Modifier.weight(1f)) {
            Tarjeta(Modifier.fillMaxWidth(), AzulNoche, espacio = 6) {
                Etiqueta("Cuota 4 de 12 · vence 05 nov", TextoSobreAzul)
                Cifra(DatosDeEjemplo.pagos[1].monto, Color.White)
                Progreso(25, VerdeMarca)
                Etiqueta("3 de 12 cuotas pagadas", TextoSobreAzul)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Tarjeta(Modifier.weight(1f)) {
                    Etiqueta("Pagado")
                    Cifra("$195.000", VerdeIngreso, 18)
                }
                Tarjeta(Modifier.weight(1f)) {
                    Etiqueta("Falta pagar")
                    Cifra("$585.000", tamano = 18)
                }
            }
            Destacado("Calendario de cuotas")
            fechas.forEachIndexed { indice, fecha ->
                val pagada = indice == 0
                val color = if (pagada) VerdeIngreso else NaranjaAlerta
                val fondo = if (pagada) FondoIngreso else FondoAlerta
                Tarjeta(Modifier.fillMaxWidth(), relleno = 11) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Insignia(if (pagada) "check" else "calendar", color, fondo, tamano = 29)
                        Column(Modifier.weight(1f)) {
                            Destacado("Cuota ${indice + 3} de 12")
                            Etiqueta("Vence $fecha")
                        }
                        Destacado(DatosDeEjemplo.pagos[1].monto)
                        EstadoPago(if (pagada) "Pagada" else "Pendiente", color, fondo)
                    }
                }
            }
        }
        PieFormulario("Marcar cuota como pagada")
    }
}

@Composable
private fun EstadoPago(texto: String, color: Color, fondo: Color) {
    Surface(color = fondo, shape = RoundedCornerShape(12.dp)) {
        Etiqueta(texto, color, Modifier.padding(horizontal = 10.dp, vertical = 3.dp))
    }
}
