package cl.payroutine.ui.navegacion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import cl.payroutine.ui.pantallas.*
import cl.payroutine.ui.theme.*

private const val BIENVENIDA = "bienvenida"
private const val NUEVO_GASTO = "nuevo-gasto"
private const val NUEVO_INGRESO = "nuevo-ingreso"
private const val NUEVO_PAGO = "nuevo-pago"
private const val EDITAR_PRESUPUESTO = "editar-presupuesto"
private const val RECORDATORIOS = "recordatorios"
private const val CALENDARIO = "calendario"
private const val HISTORIAL_PAGOS = "historial-pagos"
private const val DETALLE_CUOTA = "detalle-cuota"
private const val DETALLE_MOVIMIENTO = "detalle-movimiento"

private enum class Seccion(val ruta: String, val titulo: String, val icono: String) {
    Inicio("inicio", "Inicio", "home"),
    Movimientos("movimientos", "Movimientos", "list"),
    Presupuesto("presupuesto", "Presupuesto", "pie"),
    Pagos("pagos", "Pagos", "calendar"),
    Resumen("resumen", "Resumen", "chart"),
}

@Composable
fun PayRoutineApp() {
    val navegador = rememberNavController()
    val entradaActual by navegador.currentBackStackEntryAsState()
    val rutaActual = entradaActual?.destination?.route ?: BIENVENIDA
    val seccionActual = Seccion.entries.firstOrNull { it.ruta == rutaActual }
    val fondoSuperior = if (rutaActual == BIENVENIDA || seccionActual == Seccion.Inicio) AzulNoche else Color.White

    fun abrir(ruta: String) = navegador.navigate(ruta) { launchSingleTop = true }
    fun volver() { navegador.popBackStack() }
    fun irA(seccion: Seccion) {
        navegador.navigate(seccion.ruta) {
            popUpTo(Seccion.Inicio.ruta) {
                saveState = true
            }
            restoreState = true
            launchSingleTop = true
        }
    }

    AjustarBarraDeEstado(fondoSuperior == Color.White)

    Column(Modifier.fillMaxSize().background(fondoSuperior)) {
        if (rutaActual != BIENVENIDA) Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        Scaffold(
            modifier = Modifier.weight(1f),
            containerColor = if (rutaActual == BIENVENIDA) AzulNoche else Fondo,
            contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
            bottomBar = {
                if (seccionActual != null) BarraInferior(seccionActual, ::irA)
            },
            floatingActionButton = {
                val destino = when (seccionActual) {
                    Seccion.Inicio, Seccion.Movimientos -> NUEVO_GASTO
                    Seccion.Pagos -> NUEVO_PAGO
                    else -> null
                }
                if (destino != null) {
                    FloatingActionButton(
                        onClick = { abrir(destino) },
                        shape = CircleShape,
                        containerColor = VerdeMarca,
                        contentColor = Color.White,
                    ) {
                        Icono("plus", Color.White, descripcion = if (destino == NUEVO_PAGO) "Nuevo pago pendiente" else "Nuevo movimiento")
                    }
                }
            },
        ) { espacioInterior ->
            NavHost(
                navController = navegador,
                startDestination = BIENVENIDA,
                modifier = Modifier.fillMaxSize().padding(espacioInterior),
            ) {
                composable(BIENVENIDA) {
                    PantallaBienvenida {
                        navegador.navigate(Seccion.Inicio.ruta) {
                            popUpTo(BIENVENIDA) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
                composable(Seccion.Inicio.ruta) {
                    PantallaInicio(
                        verMovimientos = { irA(Seccion.Movimientos) },
                        verPagos = { irA(Seccion.Pagos) },
                        verRecordatorios = { abrir(RECORDATORIOS) },
                        verMovimiento = { abrir("$DETALLE_MOVIMIENTO/$it") },
                    )
                }
                composable(Seccion.Movimientos.ruta) { PantallaMovimientos { abrir("$DETALLE_MOVIMIENTO/$it") } }
                composable(Seccion.Presupuesto.ruta) { PantallaPresupuesto { abrir(EDITAR_PRESUPUESTO) } }
                composable(Seccion.Pagos.ruta) {
                    PantallaPagos({ abrir(CALENDARIO) }, { abrir(HISTORIAL_PAGOS) }, { abrir(DETALLE_CUOTA) })
                }
                composable(Seccion.Resumen.ruta) { PantallaResumen() }
                composable(NUEVO_GASTO) { PantallaNuevoMovimiento(false, ::volver) { abrir(NUEVO_INGRESO) } }
                composable(NUEVO_INGRESO) { PantallaNuevoMovimiento(true, ::volver) { volver() } }
                composable(NUEVO_PAGO) { PantallaNuevoPago(::volver) }
                composable(EDITAR_PRESUPUESTO) { PantallaEditarPresupuesto(::volver) }
                composable(RECORDATORIOS) { PantallaRecordatorios(::volver) }
                composable(CALENDARIO) { PantallaCalendario(::volver) }
                composable(HISTORIAL_PAGOS) { PantallaHistorialPagos(::volver) }
                composable(DETALLE_CUOTA) { PantallaDetalleCuota(::volver) }
                composable("$DETALLE_MOVIMIENTO/{indice}") { entrada ->
                    val indice = entrada.arguments?.getString("indice")?.toIntOrNull() ?: 0
                    PantallaDetalleMovimiento(indice, ::volver)
                }
            }
        }
    }
}

@Composable
private fun BarraInferior(actual: Seccion, seleccionar: (Seccion) -> Unit) {
    Surface(color = Color.White) {
        Column(Modifier.navigationBarsPadding()) {
            HorizontalDivider(color = Borde)
            Row(Modifier.fillMaxWidth().selectableGroup(), verticalAlignment = Alignment.CenterVertically) {
                Seccion.entries.forEach { seccion ->
                    val elegida = seccion == actual
                    val color = if (elegida) VerdeMarca else TextoSecundario
                    Column(
                        Modifier.weight(1f).heightIn(min = 55.dp)
                            .selectable(elegida, role = Role.Tab, onClick = { seleccionar(seccion) })
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
                    ) {
                        Icono(seccion.icono, color)
                        Text(seccion.titulo, color = color, fontSize = 10.sp, lineHeight = 12.sp,
                            fontWeight = if (elegida) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1)
                    }
                }
            }
        }
    }
}
