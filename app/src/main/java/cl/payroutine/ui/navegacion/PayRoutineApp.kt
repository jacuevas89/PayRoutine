package cl.payroutine.ui.navegacion

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import cl.payroutine.datos.local.entidad.Usuario
import cl.payroutine.ui.pantallas.*
import cl.payroutine.ui.theme.*
import cl.payroutine.viewmodel.DatosInicio
import cl.payroutine.viewmodel.EstadoBorrado
import cl.payroutine.viewmodel.EstadoEdicionPerfil
import cl.payroutine.viewmodel.EstadoPerfil
import cl.payroutine.viewmodel.FinanzasViewModel
import cl.payroutine.viewmodel.PerfilViewModel

private const val NUEVO_MOVIMIENTO = "nuevo-movimiento"
private const val NUEVO_PAGO = "nuevo-pago"
private const val EDITAR_PRESUPUESTO = "editar-presupuesto"
private const val PRESUPUESTO_EXCEDIDO = "presupuesto-excedido"
private const val AJUSTES = "ajustes"
private const val EDITAR_PERFIL = "editar-perfil"
private const val DATOS_Y_PRIVACIDAD = "datos-y-privacidad"
private const val INFORMACION_PRIVACIDAD = "informacion-privacidad"
private const val CATEGORIAS = "categorias"
private const val CONSEJOS_AHORRO = "consejos-ahorro"
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
internal fun PayRoutineApp(perfil: PerfilViewModel, finanzas: FinanzasViewModel) {
    val estado by perfil.estado.collectAsState()
    val inicio = if (estado.usuario != null && !estado.errorCarga) {
        finanzas.inicio.collectAsState().value
    } else {
        null
    }
    NavegacionPayRoutine(
        estado = estado,
        inicio = inicio,
        reintentarCarga = perfil::reintentarCarga,
        prepararEdicion = perfil::prepararEdicion,
        guardarEdicion = perfil::guardarEdicion,
        eliminarDatos = perfil::eliminarTodosLosDatos,
        limpiarErrorBorrado = perfil::limpiarErrorBorrado,
        registrar = perfil::registrar,
    )
}

@Composable
internal fun NavegacionPayRoutine(
    estado: EstadoPerfil,
    inicio: DatosInicio?,
    reintentarCarga: () -> Unit,
    prepararEdicion: () -> Unit = {},
    guardarEdicion: (String, String) -> Unit = { _, _ -> },
    eliminarDatos: () -> Unit = {},
    limpiarErrorBorrado: () -> Unit = {},
    registrar: (String, String) -> Unit,
) {
    val usuario = estado.usuario
    if (estado.cargando || estado.errorCarga || usuario == null) {
        PrimerUso(estado, reintentarCarga, registrar)
        return
    }

    key(usuario.id) {
        NavegacionPrincipal(
            usuario, inicio, estado.edicion, estado.borrado,
            prepararEdicion, guardarEdicion, eliminarDatos, limpiarErrorBorrado,
        )
    }
}

@Composable
private fun NavegacionPrincipal(
    usuario: Usuario,
    inicio: DatosInicio?,
    edicion: EstadoEdicionPerfil,
    borrado: EstadoBorrado,
    prepararEdicion: () -> Unit,
    guardarEdicion: (String, String) -> Unit,
    eliminarDatos: () -> Unit,
    limpiarErrorBorrado: () -> Unit,
) {
    val navegador = rememberNavController()
    val entradaActual = navegador.currentBackStackEntryAsState().value
    val usuarioActual by rememberUpdatedState(usuario)
    val edicionActual by rememberUpdatedState(edicion)
    val borradoActual by rememberUpdatedState(borrado)
    val datosActuales by rememberUpdatedState(inicio)
    val rutaActual = entradaActual?.destination?.route ?: Seccion.Inicio.ruta
    val seccionActual = Seccion.entries.firstOrNull { it.ruta == rutaActual }
    val fondoSuperior = if (seccionActual == Seccion.Inicio) AzulNoche else Color.White

    // Los destinos salientes pueden seguir visibles durante la transición.
    fun abrir(origen: NavBackStackEntry?, ruta: String) {
        if (origen == null || navegador.currentBackStackEntry !== origen) return
        navegador.navigate(ruta) { launchSingleTop = true }
    }

    fun volver(origen: NavBackStackEntry) {
        if (navegador.currentBackStackEntry === origen) navegador.popBackStack()
    }

    fun irA(origen: NavBackStackEntry?, seccion: Seccion) {
        if (origen == null || navegador.currentBackStackEntry !== origen || origen.destination.route == seccion.ruta) return
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
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        Scaffold(
            modifier = Modifier.weight(1f),
            containerColor = Fondo,
            contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
            bottomBar = {
                if (seccionActual != null) BarraInferior(seccionActual) { irA(entradaActual, it) }
            },
            floatingActionButton = {
                val destino = when (seccionActual) {
                    Seccion.Inicio, Seccion.Movimientos -> NUEVO_MOVIMIENTO
                    Seccion.Pagos -> NUEVO_PAGO
                    else -> null
                }
                if (destino != null) {
                    FloatingActionButton(
                        onClick = { abrir(entradaActual, destino) },
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
                startDestination = Seccion.Inicio.ruta,
                modifier = Modifier.fillMaxSize().padding(espacioInterior).consumeWindowInsets(espacioInterior),
            ) {
                composable(Seccion.Inicio.ruta) { origen ->
                    PantallaInicio(
                        apodo = usuarioActual.apodo,
                        datos = datosActuales,
                        verMovimientos = { irA(origen, Seccion.Movimientos) },
                        verPagos = { irA(origen, Seccion.Pagos) },
                        verRecordatorios = { abrir(origen, RECORDATORIOS) },
                        verAjustes = { abrir(origen, AJUSTES) },
                        verMovimiento = { abrir(origen, "$DETALLE_MOVIMIENTO/$it") },
                    )
                }
                composable(Seccion.Movimientos.ruta) { origen ->
                    PantallaMovimientos(datosActuales) { abrir(origen, "$DETALLE_MOVIMIENTO/$it") }
                }
                composable(Seccion.Presupuesto.ruta) { origen ->
                    PantallaPresupuesto(
                        editar = { abrir(origen, EDITAR_PRESUPUESTO) },
                        verExcedido = { abrir(origen, PRESUPUESTO_EXCEDIDO) },
                    )
                }
                composable(Seccion.Pagos.ruta) { origen ->
                    PantallaPagos(
                        verCalendario = { abrir(origen, CALENDARIO) },
                        verHistorial = { abrir(origen, HISTORIAL_PAGOS) },
                        verCuota = { abrir(origen, DETALLE_CUOTA) },
                    )
                }
                composable(Seccion.Resumen.ruta) { PantallaResumen() }
                composable(NUEVO_MOVIMIENTO) { origen -> PantallaNuevoMovimiento { volver(origen) } }
                composable(NUEVO_PAGO) { origen -> PantallaNuevoPago { volver(origen) } }
                composable(PRESUPUESTO_EXCEDIDO) { origen ->
                    PantallaPresupuestoExcedido(volver = { volver(origen) }, editar = { abrir(origen, EDITAR_PRESUPUESTO) })
                }
                composable(EDITAR_PRESUPUESTO) { origen -> PantallaEditarPresupuesto { volver(origen) } }
                composable(AJUSTES) { origen ->
                    PantallaAjustes(
                        apodo = usuarioActual.apodo,
                        nombreCompleto = usuarioActual.nombreCompleto,
                        volver = { volver(origen) },
                        editarPerfil = {
                            if (navegador.currentBackStackEntry === origen) {
                                prepararEdicion()
                                abrir(origen, EDITAR_PERFIL)
                            }
                        },
                        verCategorias = { abrir(origen, CATEGORIAS) },
                        verRecordatorios = { abrir(origen, RECORDATORIOS) },
                        verConsejos = { abrir(origen, CONSEJOS_AHORRO) },
                        verDatosYPrivacidad = { abrir(origen, DATOS_Y_PRIVACIDAD) },
                    )
                }
                composable(DATOS_Y_PRIVACIDAD) { origen ->
                    PantallaDatosYPrivacidad(
                        estado = borradoActual,
                        volver = { volver(origen) },
                        verInformacion = { abrir(origen, INFORMACION_PRIVACIDAD) },
                        eliminarDatos = eliminarDatos,
                        limpiarError = limpiarErrorBorrado,
                    )
                }
                composable(INFORMACION_PRIVACIDAD) { origen ->
                    PantallaInformacionPrivacidad { volver(origen) }
                }
                composable(EDITAR_PERFIL) { origen ->
                    val estadoEdicion = edicionActual
                    val guardando = estadoEdicion == EstadoEdicionPerfil.Guardando
                    BackHandler(enabled = guardando) {}
                    LaunchedEffect(estadoEdicion) {
                        if (estadoEdicion == EstadoEdicionPerfil.Guardada) volver(origen)
                    }
                    FormularioPerfil(
                        titulo = "Editar perfil",
                        accionGuardar = "Guardar cambios",
                        guardando = guardando,
                        errorGuardado = estadoEdicion == EstadoEdicionPerfil.Error,
                        volver = { if (!guardando) volver(origen) },
                        nombreInicial = usuarioActual.nombreCompleto,
                        apodoInicial = usuarioActual.apodo,
                        mostrarCancelar = true,
                        guardar = guardarEdicion,
                    )
                }
                composable(CATEGORIAS) { origen -> PantallaCategorias { volver(origen) } }
                composable(CONSEJOS_AHORRO) { origen -> PantallaConsejosAhorro { volver(origen) } }
                composable(RECORDATORIOS) { origen -> PantallaRecordatorios { volver(origen) } }
                composable(CALENDARIO) { origen -> PantallaCalendario { volver(origen) } }
                composable(HISTORIAL_PAGOS) { origen -> PantallaHistorialPagos { volver(origen) } }
                composable(DETALLE_CUOTA) { origen -> PantallaDetalleCuota { volver(origen) } }
                composable(
                    "$DETALLE_MOVIMIENTO/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.IntType }),
                ) { entrada ->
                    val id = requireNotNull(entrada.arguments).getInt("id")
                    val datos = datosActuales
                    val fila = datos?.movimientos?.find { it.movimiento.id == id }
                    PantallaDetalleMovimiento(fila, datos?.fecha?.zona) { volver(entrada) }
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
