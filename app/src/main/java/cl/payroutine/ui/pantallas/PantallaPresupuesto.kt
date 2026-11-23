package cl.payroutine.ui.pantallas

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cl.payroutine.datos.local.dao.MovimientoConCategoria
import cl.payroutine.datos.local.entidad.Categoria
import cl.payroutine.datos.local.entidad.ModalidadPresupuesto
import cl.payroutine.datos.local.entidad.TipoMovimiento
import cl.payroutine.ui.theme.*
import cl.payroutine.viewmodel.DatosInicio
import cl.payroutine.viewmodel.OperacionPresupuesto
import cl.payroutine.viewmodel.PresupuestoDelMes
import cl.payroutine.viewmodel.sumarImportes
import java.time.YearMonth
import java.time.ZoneId

internal fun movimientosDelPeriodo(datos: DatosInicio?, mes: YearMonth): List<MovimientoConCategoria> {
    val inicio = datos ?: return emptyList()
    return inicio.movimientos.filter {
        YearMonth.from(it.movimiento.fecha.toInstant().atZone(inicio.fecha.zona)) == mes
    }
}

@Composable
internal fun PantallaPresupuesto(
    mes: YearMonth,
    cargando: Boolean,
    presupuesto: PresupuestoDelMes?,
    anterior: PresupuestoDelMes?,
    categorias: List<Categoria>,
    gastos: List<MovimientoConCategoria>,
    automatico: Boolean,
    elegirPeriodo: (YearMonth) -> Unit,
    editar: () -> Unit,
    copiar: () -> Unit,
    cambiarAutomatico: () -> Unit,
    estado: OperacionPresupuesto,
    verExcedido: (Int) -> Unit,
) {
    var selector by rememberSaveable { mutableStateOf(false) }
    val gastado = gastos.totalGastado()
    val porCategoria = gastos.groupBy { it.movimiento.categoriaId }
    Column(Modifier.fillMaxSize()) {
        Cabecera("Presupuesto mensual", accion = if (presupuesto == null) null else "Editar", alAccionar = editar)
        Cuerpo(Modifier.weight(1f)) {
            Tarjeta(Modifier.fillMaxWidth()) {
                FilaConfiguracion("Período", mesEnPantalla(mes.atDay(1))) { selector = true }
            }
            when {
                cargando -> Etiqueta("Cargando presupuesto…")
                presupuesto == null -> {
                    Tarjeta(Modifier.fillMaxWidth()) {
                        Destacado("Sin presupuesto")
                        Etiqueta("Todavía no hay un presupuesto para este mes.")
                        BotonPrincipal("Definir presupuesto", alPulsar = editar)
                        if (anterior != null) Enlace("Copiar mes anterior", copiar)
                    }
                }
                else -> {
                    val general = presupuesto.datos.modalidad == ModalidadPresupuesto.General
                    Tarjeta(Modifier.fillMaxWidth()) {
                        Destacado(if (general) "Presupuesto general" else "Presupuesto por categorías")
                        if (presupuesto.datos.generado) Etiqueta("Generado automáticamente")
                        ResumenLimite(gastado, presupuesto.total)
                        AdvertirIngresos(presupuesto.total, presupuesto.datos.ingresoReferencia)
                        if (general && gastado > presupuesto.total) Enlace("Ver exceso") { verExcedido(0) }
                    }
                    if (!general) categorias.forEach { categoria ->
                        val limite = presupuesto.limites[categoria.id] ?: return@forEach
                        val consumo = porCategoria[categoria.id].orEmpty().totalGastado()
                        Tarjeta(Modifier.fillMaxWidth()) {
                            Destacado(categoria.nombre)
                            ResumenLimite(consumo, limite)
                            if (consumo > limite) Enlace("Ver exceso") { verExcedido(categoria.id) }
                        }
                    }
                    if (anterior != null) Enlace("Copiar mes anterior", copiar)
                }
            }
            Tarjeta(Modifier.fillMaxWidth()) {
                Destacado("Presupuesto automático")
                Etiqueta(if (automatico) "Activado para los próximos meses." else "Genera límites a partir de tus movimientos.")
                Enlace(if (automatico) "Desactivar presupuesto automático" else "Activar presupuesto automático", cambiarAutomatico)
                ErrorPresupuesto(estado)
            }
        }
    }
    if (selector) SelectorPeriodo(mes, { selector = false }) {
        elegirPeriodo(it)
        selector = false
    }
}

@Composable
internal fun PantallaPresupuestoExcedido(
    mes: YearMonth,
    nombre: String,
    limite: Double?,
    gastos: List<MovimientoConCategoria>,
    zona: ZoneId,
    volver: () -> Unit,
    editar: () -> Unit,
) {
    val total = gastos.totalGastado()
    Column(Modifier.fillMaxSize()) {
        Cabecera(nombre, volver)
        Cuerpo(Modifier.weight(1f)) {
            Etiqueta(mesEnPantalla(mes.atDay(1)))
            if (limite != null) Tarjeta(Modifier.fillMaxWidth()) {
                Destacado(if (total > limite) "Superaste tu presupuesto" else "Consumo del presupuesto")
                ResumenLimite(total, limite)
            }
            if (gastos.isEmpty()) Etiqueta("No hay gastos en este período.")
            gastos.forEach { gasto ->
                val icono = aparienciaCategoria(Categoria(nombre = gasto.categoria, tipo = TipoMovimiento.Gasto))
                FilaTarjeta(gasto.titulo(), gasto.fechaEnPantalla(zona), gasto.montoConSigno(),
                    icono.icono, icono.color, icono.fondo)
            }
            BotonPrincipal("Ajustar presupuesto", alPulsar = editar)
        }
    }
}

@Composable
private fun ResumenLimite(gastado: Double, limite: Double) {
    val proporcion = if (limite > 0) gastado / limite else 0.0
    val color = if (gastado > limite) RojoGasto else VerdeMarca
    val diferencia = (limite.toBigDecimal() - gastado.toBigDecimal()).abs()
    Cifra("${montoEnPantalla(gastado)} de ${montoEnPantalla(limite)}", tamano = 21)
    Progreso((proporcion * 100).toInt(), color)
    Etiqueta(
        if (gastado > limite) "Excedido por ${montoEnPantalla(diferencia)}"
        else "Disponible: ${montoEnPantalla(diferencia)}",
        color,
    )
}

@Composable
internal fun PantallaConfigurarPresupuesto(
    mes: YearMonth,
    categorias: List<Categoria>,
    gastos: List<MovimientoConCategoria>,
    movimientosCargados: Boolean,
    hayMovimientosAnteriores: Boolean,
    estado: OperacionPresupuesto,
    elegirPeriodo: (YearMonth) -> Unit,
    guardar: (Double?, Map<Int, Double>) -> Unit,
    automatico: (Boolean) -> Unit,
    omitir: () -> Unit,
) {
    var modo by rememberSaveable { mutableStateOf<ModalidadPresupuesto?>(null) }
    var confirmar by rememberSaveable { mutableStateOf(false) }
    var confirmarOmitir by rememberSaveable { mutableStateOf(false) }
    var selector by rememberSaveable { mutableStateOf(false) }
    if (modo != null) {
        PantallaEditarPresupuesto(
            mes, null, modo, categorias, gastos, estado,
            volver = { modo = null }, guardar = guardar,
        )
    } else {
        Column(Modifier.fillMaxSize()) {
            Cabecera("Configura tu presupuesto")
            Cuerpo(Modifier.weight(1f)) {
                Tarjeta(Modifier.fillMaxWidth()) {
                    FilaConfiguracion("Período", mesEnPantalla(mes.atDay(1))) { selector = true }
                }
                Tarjeta(Modifier.fillMaxWidth()) {
                    BotonPrincipal("Presupuesto general", habilitado = estado != OperacionPresupuesto.Guardando) {
                        modo = ModalidadPresupuesto.General
                    }
                    BotonPrincipal("Presupuesto por categorías", habilitado = estado != OperacionPresupuesto.Guardando) {
                        modo = ModalidadPresupuesto.Categorias
                    }
                    BotonPrincipal("Presupuesto automático", habilitado = estado != OperacionPresupuesto.Guardando &&
                        movimientosCargados) { confirmar = true }
                    Enlace("Continuar sin presupuesto") {
                        if (estado != OperacionPresupuesto.Guardando) confirmarOmitir = true
                    }
                }
                ErrorPresupuesto(estado)
            }
        }
    }
    if (selector) SelectorPeriodo(mes, { selector = false }) {
        elegirPeriodo(it)
        selector = false
    }
    if (confirmar) ConfirmarPresupuestoAutomatico(YearMonth.now(), hayMovimientosAnteriores, { confirmar = false }) {
        confirmar = false
        automatico(it)
    }
    if (confirmarOmitir) AlertDialog(
        onDismissRequest = { confirmarOmitir = false },
        text = { Text("Definir un presupuesto te ayudará a ordenar tus finanzas. Puedes configurarlo después desde Presupuesto.") },
        confirmButton = {
            TextButton(onClick = {
                confirmarOmitir = false
                omitir()
            }) { Text("Continuar sin presupuesto") }
        },
        dismissButton = {
            TextButton(onClick = { confirmarOmitir = false }) { Text("Definir presupuesto") }
        },
    )
}

@Composable
internal fun ConfirmarPresupuestoAutomatico(
    mes: YearMonth,
    hayMovimientosAnteriores: Boolean,
    cerrar: () -> Unit,
    confirmar: (Boolean) -> Unit,
) {
    var completo by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = cerrar,
        title = { Text("Activar presupuesto automático") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("PayRoutine usará tus ingresos y gastos para generar los presupuestos de los meses siguientes. Podrás editarlos y los presupuestos que ya guardaste se conservarán.")
                Text("El cálculo empieza después del primer mes calendario completo de uso.")
                if (hayMovimientosAnteriores) {
                    val anterior = mesEnPantalla(mes.minusMonths(1).atDay(1))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(completo, { completo = it })
                        Text("Ya registré todos mis ingresos y gastos de $anterior. Usar ese mes para calcular el presupuesto actual.")
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { confirmar(completo) }) { Text("Activar") } },
        dismissButton = { TextButton(onClick = cerrar) { Text("Cancelar") } },
    )
}

@Composable
internal fun PantallaEditarPresupuesto(
    mes: YearMonth,
    origen: PresupuestoDelMes?,
    modalidadInicial: ModalidadPresupuesto?,
    categorias: List<Categoria>,
    gastos: List<MovimientoConCategoria>,
    estado: OperacionPresupuesto,
    volver: () -> Unit,
    guardar: (Double?, Map<Int, Double>) -> Unit,
    copiado: Boolean = false,
) {
    var modalidad by rememberSaveable { mutableStateOf(modalidadInicial ?: origen?.datos?.modalidad ?: ModalidadPresupuesto.General) }
    var importe by rememberSaveable { mutableStateOf(origen?.datos?.limiteGeneral?.comoEntrada() ?: "") }
    var limites by rememberSaveable {
        mutableStateOf(HashMap(origen?.limites.orEmpty().mapValues { (_, monto) -> monto.comoEntrada() }))
    }
    var distribuir by rememberSaveable { mutableStateOf<Double?>(null) }
    val suma = limites.values.mapNotNull(::leerLimite).sumarImportes()
    val general = modalidad == ModalidadPresupuesto.General
    val montos = categorias.mapNotNull { categoria ->
        leerLimite(limites[categoria.id].orEmpty())?.let { categoria.id to it }
    }.toMap()
    val guardando = estado == OperacionPresupuesto.Guardando
    val valido = if (general) leerLimite(importe) != null else montos.isNotEmpty() && categorias.none {
        val texto = limites[it.id].orEmpty()
        texto.isNotBlank() && leerLimite(texto) == null
    }
    val total = if (general) leerLimite(importe) ?: 0.0 else montos.values.sumarImportes()
    val porCategoria = gastos.groupBy { it.movimiento.categoriaId }

    BackHandler(enabled = guardando) {}
    Column(Modifier.fillMaxSize()) {
        Cabecera("Editar presupuesto", volver = { if (!guardando) volver() })
        Cuerpo(Modifier.weight(1f)) {
            Etiqueta(mesEnPantalla(mes.atDay(1)))
            if (copiado) Etiqueta("Revisa los límites del mes anterior antes de guardar.")
            Segmentos(listOf("General", "Por categorías"), if (general) 0 else 1) { indice ->
                if (guardando || (indice == 0) == general) return@Segmentos
                if (indice == 0) {
                    importe = suma.takeIf { it > 0 }?.comoEntrada() ?: ""
                    modalidad = ModalidadPresupuesto.General
                } else {
                    distribuir = leerLimite(importe)
                    modalidad = ModalidadPresupuesto.Categorias
                }
            }
            if (general) {
                CampoLimite("Límite mensual", importe, gastos.totalGastado(), !guardando) { importe = it }
            } else {
                distribuir?.let { Etiqueta("Distribuye el límite de ${montoEnPantalla(it)} entre tus categorías.") }
                Etiqueta("Deja vacío el límite de las categorías que quedarán sin presupuesto.")
                categorias.forEach { categoria ->
                    CampoLimite(
                        categoria.nombre, limites[categoria.id].orEmpty(),
                        porCategoria[categoria.id].orEmpty().totalGastado(), !guardando,
                    ) { limites = HashMap(limites).apply { put(categoria.id, it) } }
                }
            }
            Destacado("Total: ${montoEnPantalla(total)}")
            AdvertirIngresos(total, origen?.datos?.ingresoReferencia?.takeUnless { copiado })
            ErrorPresupuesto(estado)
            BotonPrincipal("Guardar presupuesto", habilitado = valido && !guardando) {
                guardar(if (general) leerLimite(importe) else null, if (general) emptyMap() else montos)
            }
            Enlace("Cancelar") { if (!guardando) volver() }
        }
    }
}

@Composable
private fun CampoLimite(nombre: String, texto: String, gastado: Double, habilitado: Boolean, cambiar: (String) -> Unit) {
    val monto = leerLimite(texto)
    val invalido = texto.isNotBlank() && monto == null
    OutlinedTextField(
        value = texto,
        onValueChange = cambiar,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(nombre) },
        placeholder = { Text("Sin presupuesto") },
        singleLine = true,
        isError = invalido,
        enabled = habilitado,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        supportingText = {
            val menor = monto != null && monto < gastado
            Etiqueta(when {
                invalido -> "Ingresa un importe positivo con hasta dos decimales."
                menor -> "El límite está por debajo de lo gastado: ${montoEnPantalla(gastado)}."
                else -> "Gastado: ${montoEnPantalla(gastado)}"
            }, if (menor || invalido) RojoGasto else TextoSecundario)
        },
    )
}

@Composable
private fun AdvertirIngresos(limite: Double, ingresos: Double?) {
    if (ingresos != null && limite > ingresos) {
        Etiqueta("El presupuesto supera los ingresos de referencia (${montoEnPantalla(ingresos)}).", RojoGasto)
    }
}

@Composable
internal fun ErrorPresupuesto(estado: OperacionPresupuesto) {
    if (estado == OperacionPresupuesto.Error) Etiqueta("No se pudo guardar. Vuelve a intentarlo.", RojoGasto)
}

private val formatoImporte = Regex("[0-9]{1,15}([.,][0-9]{1,2})?")
private fun leerLimite(texto: String): Double? = texto.trim().takeIf(formatoImporte::matches)
    ?.replace(',', '.')?.toDoubleOrNull()?.takeIf { it > 0 }

private fun Double.comoEntrada() = toBigDecimal().stripTrailingZeros().toPlainString()

private fun List<MovimientoConCategoria>.totalGastado(): Double = map { it.movimiento.monto }.sumarImportes()
