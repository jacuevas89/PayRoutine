package cl.payroutine.recordatorios

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteException
import android.media.AudioAttributes
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import cl.payroutine.MainActivity
import cl.payroutine.R
import cl.payroutine.datos.local.entidad.RecordatorioMovimiento
import cl.payroutine.datos.local.entidad.PreferenciasPresupuesto
import cl.payroutine.datos.local.entidad.PreferenciasAvisosPago
import cl.payroutine.datos.local.entidad.PagoPendiente
import cl.payroutine.datos.local.entidad.fechaAviso
import cl.payroutine.datos.local.entidad.indice
import cl.payroutine.datos.repositorio.RepositorioFinanzas
import cl.payroutine.ui.pantallas.fechaEnPantalla
import cl.payroutine.ui.pantallas.montoEnPantalla
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZonedDateTime
import java.util.UUID
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal const val ABRIR_NUEVO_MOVIMIENTO = "cl.payroutine.recordatorios.ABRIR_NUEVO_MOVIMIENTO"
internal const val ABRIR_PRESUPUESTO = "cl.payroutine.recordatorios.ABRIR_PRESUPUESTO"
internal const val ABRIR_PAGO = "cl.payroutine.recordatorios.ABRIR_PAGO"
internal const val EXTRA_USUARIO = "usuario"
internal const val EXTRA_VENCIMIENTO = "vencimiento"
internal const val AVISO_MOVIMIENTO = "cl.payroutine.recordatorios.AVISO_MOVIMIENTO"
internal const val AVISO_PRESUPUESTO = "cl.payroutine.recordatorios.AVISO_PRESUPUESTO"
internal const val GENERAR_PRESUPUESTO = "cl.payroutine.recordatorios.GENERAR_PRESUPUESTO"
internal const val AVISO_PAGOS = "cl.payroutine.recordatorios.AVISO_PAGOS"
internal const val EXTRA_PROGRAMACION = "programacion"
internal const val EXTRA_DIA = "dia"
internal const val CANAL_MOVIMIENTOS = "recordatorio_movimientos"
private const val CANAL_PRESUPUESTO = "recordatorio_presupuesto"
internal const val CANAL_PAGOS = "recordatorios_pagos"
private const val ID_AVISO = 91
private const val ID_AVISO_PRESUPUESTO = 92
private const val ID_GENERAR_PRESUPUESTO = 93
private const val ID_ALARMA_PAGOS = 94
private const val FLAGS_ALARMA = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT

internal class RecordatoriosMovimientos(
    private val contexto: Context,
    private val repositorio: RepositorioFinanzas,
) {
    private val alarmas = requireNotNull(contexto.getSystemService(AlarmManager::class.java))
    private val notificaciones = requireNotNull(contexto.getSystemService(NotificationManager::class.java))
    private val mutex = Mutex()

    fun observar(usuarioId: String) = repositorio.observarRecordatorio(usuarioId)
    fun observarPreferenciasPagos(usuarioId: String) = repositorio.observarPreferenciasAvisosPago(usuarioId)
    fun observarSeleccionPagos() = repositorio.observarSeleccionAvisosPago()

    fun alarmasExactasHabilitadas(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmas.canScheduleExactAlarms()

    fun canalSilenciado(): Boolean = notificaciones.getNotificationChannel(CANAL_MOVIMIENTOS)
        ?.let { it.importance < NotificationManager.IMPORTANCE_DEFAULT || it.sound == null } ?: false

    fun notificacionesHabilitadas(): Boolean = notificacionesHabilitadas(CANAL_MOVIMIENTOS)
    fun notificacionesPagosHabilitadas(): Boolean = notificacionesHabilitadas(CANAL_PAGOS)

    private fun notificacionesHabilitadas(canal: String): Boolean {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(contexto).areNotificationsEnabled() &&
            notificaciones.getNotificationChannel(canal)?.importance != NotificationManager.IMPORTANCE_NONE
    }

    suspend fun actualizar(usuarioId: String, cambiar: (RecordatorioMovimiento) -> RecordatorioMovimiento) = mutex.withLock {
        val anterior = repositorio.obtenerRecordatorio()?.takeIf { it.usuarioId == usuarioId }
            ?: RecordatorioMovimiento(usuarioId)
        val ahora = ZonedDateTime.now()
        val nuevo = cambiar(anterior)
        val referencia = if (nuevo.activo && !anterior.activo) {
            val hoy = ahora.toLocalDate()
            val fecha = if (hoy.atTime(nuevo.hora).atZone(ahora.zone).isAfter(ahora)) hoy else hoy.plusDays(1)
            fecha.toEpochDay()
        } else {
            anterior.fechaReferencia
        }
        val guardado = nuevo.copy(fechaReferencia = referencia, programacionId = UUID.randomUUID().toString())
        repositorio.guardarRecordatorio(guardado)
        programar(guardado, ahora)
        if (!guardado.activo) notificaciones.cancel(ID_AVISO)
    }

    suspend fun restaurar(siFalta: Boolean = false) = mutex.withLock {
        val ahora = ZonedDateTime.now()
        if (!siFalta || alarmaExistente() == null) programar(repositorio.obtenerRecordatorio(), ahora)
        prepararPresupuesto(ahora, siFalta)
        prepararAvisosPagos(ahora, conservarHora = siFalta && alarmaExistente(AVISO_PAGOS, ID_ALARMA_PAGOS) != null)
    }

    suspend fun actualizarPreferenciasPagos(usuarioId: String, cambiar: (PreferenciasAvisosPago) -> PreferenciasAvisosPago) =
        mutex.withLock {
            repositorio.guardarPreferenciasAvisosPago(usuarioId, cambiar)
            prepararAvisosPagos(ZonedDateTime.now())
        }

    suspend fun seleccionarPagos(usuarioId: String, ids: List<Int>, seleccionar: Boolean) = mutex.withLock {
        repositorio.seleccionarAvisosPago(usuarioId, ids, seleccionar)
        val conservar = alarmaExistente(AVISO_PAGOS, ID_ALARMA_PAGOS) != null
        prepararAvisosPagos(ZonedDateTime.now(), conservarHora = conservar)
    }

    suspend fun restaurarPagos() = mutex.withLock {
        val conservar = alarmaExistente(AVISO_PAGOS, ID_ALARMA_PAGOS) != null
        prepararAvisosPagos(ZonedDateTime.now(), conservarHora = conservar)
    }

    suspend fun recibirAvisosPagos(instante: Long, ahora: ZonedDateTime = ZonedDateTime.now()) = mutex.withLock {
        if (ahora.toInstant().toEpochMilli() >= instante) prepararAvisosPagos(ahora, entregar = notificacionesPagosHabilitadas())
    }

    private suspend fun prepararAvisosPagos(
        ahora: ZonedDateTime,
        entregar: Boolean = false,
        conservarHora: Boolean = false,
    ) {
        val plan = repositorio.planificarAvisosPago(ahora, entregar, conservarHora)
        val pendientes = plan.pendientes.associateBy { it.id }
        notificaciones.activeNotifications.filter { it.tag == CANAL_PAGOS }.forEach { aviso ->
            val pago = pendientes[aviso.id]
            if (pago == null || aviso.notification.extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString() != pago.descripcion ||
                aviso.notification.extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString() != textoPago(pago, ahora)
            ) notificaciones.cancel(CANAL_PAGOS, aviso.id)
        }
        val siguiente = plan.siguiente
        if (siguiente == null) cancelarAlarma(AVISO_PAGOS, ID_ALARMA_PAGOS)
        else {
            val intent = intencionAlarma(AVISO_PAGOS).putExtra(EXTRA_DIA, siguiente.toInstant().toEpochMilli())
            agendar(generarAlarma(AVISO_PAGOS, ID_ALARMA_PAGOS, intent), siguiente)
        }
        val usuarioId = plan.usuarioId ?: return
        plan.entregar.forEach { pago ->
            publicar(
                usuarioId, pago.id, CANAL_PAGOS, "Vencimientos de pagos",
                ABRIR_PAGO, pago.descripcion, textoPago(pago, ahora), pago.id,
            )
        }
    }

    private fun textoPago(pago: PagoPendiente, ahora: ZonedDateTime): String {
        val fecha = pago.fechaVencimiento.toInstant().atZone(ahora.zone).toLocalDate()
        return "${montoEnPantalla(pago.monto)} · Vence el ${fechaEnPantalla(fecha)}"
    }

    suspend fun restaurarPresupuesto() = mutex.withLock { prepararPresupuesto(ZonedDateTime.now()) }

    suspend fun actualizarAvisoPresupuesto(usuarioId: String, cambiar: (PreferenciasPresupuesto) -> PreferenciasPresupuesto) =
        mutex.withLock {
            repositorio.actualizarPreferenciasPresupuesto(usuarioId, cambiar)
            prepararPresupuesto(ZonedDateTime.now())
        }

    suspend fun recibirAvisoPresupuesto(dia: Long) = mutex.withLock {
        val ahora = ZonedDateTime.now()
        val mes = YearMonth.from(ahora)
        repositorio.generarPresupuestoAutomatico(mes, ahora.zone)
        val preferencias = repositorio.preferenciasPresupuesto()
        if (preferencias?.avisoActivo == true &&
            preferencias.ultimoMesAvisado != mes.indice() &&
            preferencias.fechaAviso(mes, ahora).toLocalDate().toEpochDay() == dia &&
            !ahora.isBefore(preferencias.fechaAviso(mes, ahora)) &&
            !repositorio.existePresupuesto(mes) &&
            notificacionesHabilitadas(CANAL_PRESUPUESTO)
        ) {
            publicar(
                preferencias.usuarioId, ID_AVISO_PRESUPUESTO, CANAL_PRESUPUESTO, "Presupuesto mensual",
                ABRIR_PRESUPUESTO, "Configura tu presupuesto",
                "Crea el presupuesto de este mes o copia los límites del mes anterior.",
            )
            repositorio.actualizarPreferenciasPresupuesto(preferencias.usuarioId) { it.copy(ultimoMesAvisado = mes.indice()) }
        }
        prepararPresupuesto(ahora)
    }

    private suspend fun prepararPresupuesto(ahora: ZonedDateTime, conservarAviso: Boolean = false) {
        val mes = YearMonth.from(ahora)
        repositorio.generarPresupuestoAutomatico(mes, ahora.zone)
        val preferencias = repositorio.preferenciasPresupuesto()
        val existe = repositorio.existePresupuesto(mes)
        if (preferencias?.primerMesAutomatico != null) {
            val comienzo = mes.plusMonths(1).atDay(1).atTime(0, 5).atZone(ahora.zone)
            agendar(generarAlarma(accion = GENERAR_PRESUPUESTO, id = ID_GENERAR_PRESUPUESTO), comienzo)
        } else cancelarAlarma(GENERAR_PRESUPUESTO, ID_GENERAR_PRESUPUESTO)
        if (preferencias?.avisoActivo != true) {
            cancelarAlarma(AVISO_PRESUPUESTO, ID_AVISO_PRESUPUESTO)
            notificaciones.cancel(ID_AVISO_PRESUPUESTO)
            return
        }
        if (existe) notificaciones.cancel(ID_AVISO_PRESUPUESTO)
        if (conservarAviso && !existe && alarmaExistente(AVISO_PRESUPUESTO, ID_AVISO_PRESUPUESTO) != null) return
        val fecha = preferencias.fechaAviso(mes, ahora)
        val siguiente = if (existe || preferencias.ultimoMesAvisado == mes.indice() || !fecha.isAfter(ahora)) {
            preferencias.fechaAviso(mes.plusMonths(1), ahora)
        } else fecha
        val intencion = intencionAlarma(AVISO_PRESUPUESTO).putExtra(EXTRA_DIA, siguiente.toLocalDate().toEpochDay())
        agendar(generarAlarma(AVISO_PRESUPUESTO, ID_AVISO_PRESUPUESTO, intencion), siguiente)
    }

    suspend fun recibirAviso(idProgramacion: String?, dia: Long) = mutex.withLock {
        val configuracion = repositorio.obtenerRecordatorio() ?: return@withLock
        if (!configuracion.activo || configuracion.programacionId != idProgramacion) return@withLock
        val ahora = ZonedDateTime.now()
        val hoy = ahora.toLocalDate()
        val diaActual = hoy.toEpochDay()
        if (dia in (diaActual - 1)..diaActual &&
            (configuracion.ultimoAviso == null || configuracion.ultimoAviso < diaActual) &&
            !ahora.isBefore(LocalDate.ofEpochDay(dia).atTime(configuracion.hora).atZone(ahora.zone)) &&
            notificacionesHabilitadas()
        ) {
            val actualizado = configuracion.copy(ultimoAviso = diaActual)
            try {
                publicar(actualizado.usuarioId)
                repositorio.guardarRecordatorio(actualizado)
            } finally {
                programar(actualizado, ahora)
            }
        } else {
            programar(configuracion, ahora)
        }
    }

    suspend fun eliminarDatosLocales() = mutex.withLock {
        cancelarAlarma()
        cancelarAlarma(AVISO_PRESUPUESTO, ID_AVISO_PRESUPUESTO)
        cancelarAlarma(GENERAR_PRESUPUESTO, ID_GENERAR_PRESUPUESTO)
        cancelarAlarma(AVISO_PAGOS, ID_ALARMA_PAGOS)
        notificaciones.cancelAll()
        try {
            repositorio.eliminarTodosLosDatos()
        } catch (error: SQLiteException) {
            programar(repositorio.obtenerRecordatorio(), ZonedDateTime.now())
            prepararPresupuesto(ZonedDateTime.now())
            prepararAvisosPagos(ZonedDateTime.now())
            throw error
        }
    }

    private fun intencionAlarma(accion: String = AVISO_MOVIMIENTO) =
        Intent(contexto, ReceptorRecordatorios::class.java).setAction(accion)

    private fun alarmaExistente(accion: String = AVISO_MOVIMIENTO, id: Int = ID_AVISO): PendingIntent? =
        PendingIntent.getBroadcast(contexto, id, intencionAlarma(accion), FLAGS_ALARMA or PendingIntent.FLAG_NO_CREATE)

    private fun cancelarAlarma(accion: String = AVISO_MOVIMIENTO, id: Int = ID_AVISO) {
        alarmaExistente(accion, id)?.let {
            alarmas.cancel(it)
            it.cancel()
        }
    }

    private fun programar(configuracion: RecordatorioMovimiento?, ahora: ZonedDateTime) {
        if (configuracion?.activo != true) {
            cancelarAlarma()
            return
        }
        val siguiente = configuracion.siguienteAviso(ahora)
        val intent = intencionAlarma()
            .putExtra(EXTRA_PROGRAMACION, configuracion.programacionId)
            .putExtra(EXTRA_DIA, siguiente.toLocalDate().toEpochDay())
        agendar(generarAlarma(AVISO_MOVIMIENTO, ID_AVISO, intent), siguiente)
    }

    private fun generarAlarma(accion: String, id: Int, intent: Intent = intencionAlarma(accion)): PendingIntent =
        PendingIntent.getBroadcast(contexto, id, intent, FLAGS_ALARMA or PendingIntent.FLAG_UPDATE_CURRENT)

    private fun agendar(alarma: PendingIntent, siguiente: ZonedDateTime) {
        val instante = siguiente.toInstant().toEpochMilli()
        if (alarmasExactasHabilitadas()) {
            try {
                alarmas.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, instante, alarma)
                return
            } catch (_: SecurityException) {
                // El permiso puede cambiar entre la consulta y la programación.
            }
        }
        try {
            alarmas.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, instante, alarma)
        } catch (error: SecurityException) {
            alarma.cancel()
            throw error
        }
    }

    private fun publicar(
        usuarioId: String,
        id: Int = ID_AVISO,
        canal: String = CANAL_MOVIMIENTOS,
        nombreCanal: String = "Recordatorio de movimientos",
        destino: String = ABRIR_NUEVO_MOVIMIENTO,
        titulo: String = "Registra tus movimientos",
        mensaje: String = "Toca para anotar un gasto o un ingreso en PayRoutine.",
        vencimientoId: Int? = null,
    ) {
        notificaciones.createNotificationChannel(
            NotificationChannel(canal, nombreCanal, NotificationManager.IMPORTANCE_DEFAULT).apply {
                setSound(
                    Settings.System.DEFAULT_NOTIFICATION_URI,
                    AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build(),
                )
            },
        )
        val intent = Intent(contexto, MainActivity::class.java)
            .setAction(destino)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(EXTRA_USUARIO, usuarioId)
        vencimientoId?.let { intent.putExtra(EXTRA_VENCIMIENTO, it) }
        val abrir = PendingIntent.getActivity(
            contexto, id, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT,
        )
        val notificacion = NotificationCompat.Builder(contexto, canal)
            .setSmallIcon(R.drawable.icono_bell)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setContentIntent(abrir)
            .setAutoCancel(true)
            .build()
        if (vencimientoId == null) notificaciones.notify(id, notificacion)
        else notificaciones.notify(CANAL_PAGOS, id, notificacion)
    }
}
