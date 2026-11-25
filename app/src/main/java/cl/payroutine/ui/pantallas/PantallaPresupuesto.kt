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
import cl.payroutine.viewmodel.movimientosDelMes
import cl.payroutine.datos.repositorio.validarMontoMovimiento
import java.math.BigDecimal
import java.math.MathContext
import java.time.YearMonth
import java.time.ZoneId

internal fun movimientosDelPeriodo(datos: DatosInicio?, mes: YearMonth): List<MovimientoConCategoria> {
    val inicio = datos ?: return emptyList()
    return movimientosDelMes(inicio.movimientos, mes, inicio.fecha.zona)
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
                    val (conLimite, sinLimite) = gastos.partition {
                        general || it.movimiento.categoriaId in presupuesto.limites ||
                            (it.movimiento.categoriaId == null && presupuesto.sinCategoria.signum() > 0)
                    }
                    val gastado = conLimite.totalGastado()
                    Tarjeta(Modifier.fillMaxWidth()) {
                        Destacado(if (general) "Presupuesto general" else "Presupuesto total mensual")
                        if (!general) Etiqueta("Presupuesto por categorías")
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
                            ResumenLimite(consumo, limite.toBigDecimal())
                            if (consumo > limite.toBigDecimal()) Enlace("Ver exceso") { verExcedido(categoria.id) }
                        }
                    }
                    if (!general && presupuesto.sinCategoria.signum() > 0) Tarjeta(Modifier.fillMaxWidth()) {
                        Destacado("Sin categoría")
                        ResumenLimite(porCategoria[null].orEmpty().totalGastado(), presupuesto.sinCategoria)
                    }
                    if (sinLimite.isNotEmpty()) Tarjeta(Modifier.fillMaxWidth()) {
                        Destacado("Gastos sin presupuesto")
                        Etiqueta("Total: ${montoEnPantalla(sinLimite.totalGastado())}")
                        sinLimite.groupBy { it.movimiento.categoriaId }.values.forEach { filas ->
                            Etiqueta("${filas.first().categoria}: ${montoEnPantalla(filas.totalGastado())}")
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
    limite: Long?,
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
                Destacado(if (total > limite.toBigDecimal()) "Superaste tu presupuesto" else "Consumo del presupuesto")
                ResumenLimite(total, limite.toBigDecimal())
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
private fun ResumenLimite(gastado: BigDecimal, limite: BigDecimal) {
    val proporcion = if (limite.signum() > 0) gastado.divide(limite, MathContext.DECIMAL64) else BigDecimal.ZERO
    val color = if (gastado > limite) RojoGasto else VerdeMarca
    val diferencia = (limite - gastado).abs()
    Cifra("${montoEnPantalla(gastado)} de ${montoEnPantalla(limite)}", tamano = 21)
    Progreso((proporcion * 100.toBigDecimal()).min(100.toBigDecimal()).toInt(), color)
    val porcentaje = porcentajeEnPantalla(proporcion)
    val limiteExacto = porcentajeEnPantalla(1)
    Etiqueta(when {
        porcentaje == limiteExacto && gastado > limite -> "Más de $limiteExacto usado"
        porcentaje == limiteExacto && gastado < limite -> "Menos de $limiteExacto usado"
        else -> "$porcentaje usado"
    }, color)
    Etiqueta("Disponible presupuesto ${montoEnPantalla(if (gastado > limite) BigDecimal.ZERO else diferencia)}", color)
    if (gastado > limite) Etiqueta("Excedido por ${montoEnPantalla(diferencia)}", RojoGasto)
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
    guardar: (Long?, Map<Int, Long>) -> Unit,
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
    guardar: (Long?, Map<Int, Long>) -> Unit,
    copiado: Boolean = false,
) {
    var modalidad by rememberSaveable { mutableStateOf(modalidadInicial ?: origen?.datos?.modalidad ?: ModalidadPresupuesto.General) }
    var importe by rememberSaveable { mutableStateOf(origen?.datos?.limiteGeneral?.comoEntrada() ?: "") }
    var limites by rememberSaveable {
        mutableStateOf(HashMap(origen?.limites.orEmpty().mapValues { (_, monto) -> monto.comoEntrada() }))
    }
    var distribuir by rememberSaveable { mutableStateOf<Long?>(null) }
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
    val total = if (general) leerLimite(importe)?.toBigDecimal() ?: BigDecimal.ZERO else montos.values.sumarImportes()
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
                    importe = suma.takeIf { it.signum() > 0 }?.toPlainString() ?: ""
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
private fun CampoLimite(nombre: String, texto: String, gastado: BigDecimal, habilitado: Boolean, cambiar: (String) -> Unit) {
    val monto = leerLimite(texto)
    val invalido = texto.isNotBlank() && monto == null
    CampoMonto(
        value = texto,
        onValueChange = cambiar,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(nombre) },
        placeholder = { Text("Sin presupuesto") },
        singleLine = true,
        isError = invalido,
        enabled = habilitado,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        supportingText = {
            val menor = monto != null && monto.toBigDecimal() < gastado
            Etiqueta(when {
                invalido -> "Ingresa un importe positivo en pesos enteros."
                menor -> "El límite está por debajo de lo gastado: ${montoEnPantalla(gastado)}."
                else -> "Gastado: ${montoEnPantalla(gastado)}"
            }, if (menor || invalido) RojoGasto else TextoSecundario)
        },
    )
}

@Composable
private fun AdvertirIngresos(limite: BigDecimal, ingresos: Long?) {
    if (ingresos != null && limite > ingresos.toBigDecimal()) {
        Etiqueta("El presupuesto supera los ingresos de referencia (${montoEnPantalla(ingresos)}).", RojoGasto)
    }
}

@Composable
internal fun ErrorPresupuesto(estado: OperacionPresupuesto) {
    if (estado == OperacionPresupuesto.Error) Etiqueta("No se pudo guardar. Vuelve a intentarlo.", RojoGasto)
}

private fun leerLimite(texto: String) = validarMontoMovimiento(texto).valor

private fun Long.comoEntrada() = toString()

private fun List<MovimientoConCategoria> .totalGastado() = map { it.movimiento.monto }.sumarImportes()
