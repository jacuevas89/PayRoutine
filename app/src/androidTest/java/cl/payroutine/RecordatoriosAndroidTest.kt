package cl.payroutine

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.provider.Settings
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import cl.payroutine.datos.local.BaseDatosPayRoutine
import cl.payroutine.datos.local.entidad.RecurrenciaRecordatorio
import cl.payroutine.datos.repositorio.RepositorioFinanzas
import cl.payroutine.recordatorios.AVISO_MOVIMIENTO
import cl.payroutine.recordatorios.AVISO_PRESUPUESTO
import cl.payroutine.recordatorios.ReceptorRecordatorios
import cl.payroutine.recordatorios.RecordatoriosMovimientos
import java.time.ZonedDateTime
import java.time.YearMonth
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 33)
class RecordatoriosAndroidTest {
    @Test
    fun avisoMensualOmiteMesConfiguradoYSeBorraConElPerfil() = runBlocking {
        val instrumentacion = InstrumentationRegistry.getInstrumentation()
        val contexto = instrumentacion.targetContext
        val base = Room.inMemoryDatabaseBuilder(contexto, BaseDatosPayRoutine::class.java).build()
        val repo = RepositorioFinanzas(base)
        val servicio = RecordatoriosMovimientos(contexto, repo)
        val sistema = requireNotNull(contexto.getSystemService(NotificationManager::class.java))
        val intencion = Intent(contexto, ReceptorRecordatorios::class.java).setAction(AVISO_PRESUPUESTO)
        val flags = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
        try {
            instrumentacion.uiAutomation.grantRuntimePermission(contexto.packageName, Manifest.permission.POST_NOTIFICATIONS)
            val ahora = ZonedDateTime.now()
            val dia = ahora.toLocalDate().toEpochDay()
            val mes = YearMonth.from(ahora)
            repo.registrarUsuario("Perfil con presupuesto", "Prueba")
            val perfil = requireNotNull(repo.observarUsuario().first()).id
            repo.guardarPresupuestoMensual(perfil, mes, 30000L, emptyMap())
            servicio.actualizarAvisoPresupuesto(perfil) {
                it.copy(avisoActivo = true, diaAviso = ahora.dayOfMonth, minutoAviso = 0)
            }
            assertNotNull(PendingIntent.getBroadcast(contexto, 92, intencion, flags))
            servicio.recibirAvisoPresupuesto(dia)
            assertNull(requireNotNull(repo.preferenciasPresupuesto()).ultimoMesAvisado)
            assertTrue(sistema.activeNotifications.none { it.id == 92 })

            servicio.eliminarDatosLocales()
            repo.registrarUsuario("Perfil sin presupuesto", "Otro")
            val otro = requireNotNull(repo.observarUsuario().first()).id
            servicio.actualizarAvisoPresupuesto(otro) {
                it.copy(avisoActivo = true, diaAviso = ahora.dayOfMonth, minutoAviso = 0)
            }
            servicio.recibirAvisoPresupuesto(dia)
            withTimeout(5_000) {
                while (sistema.activeNotifications.none { it.id == 92 }) delay(50)
            }
            val aviso = sistema.activeNotifications.single { it.id == 92 }.notification
            assertEquals("Configura tu presupuesto", aviso.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString())
            assertNotNull(aviso.contentIntent)
            servicio.recibirAvisoPresupuesto(dia)
            assertEquals(1, sistema.activeNotifications.count { it.id == 92 })
            servicio.eliminarDatosLocales()
            assertNull(repo.preferenciasPresupuesto())
            assertNull(PendingIntent.getBroadcast(contexto, 92, intencion, flags))
            withTimeout(5_000) {
                while (sistema.activeNotifications.any { it.id == 92 }) delay(50)
            }
        } finally {
            servicio.eliminarDatosLocales()
            base.close()
        }
    }

    @Test
    fun persisteConfigPublicaUnaVezYElReinicioEliminaElAviso() = runBlocking {
        val instrumentacion = InstrumentationRegistry.getInstrumentation()
        val contexto = instrumentacion.targetContext
        val base = Room.inMemoryDatabaseBuilder(contexto, BaseDatosPayRoutine::class.java).build()
        val repositorio = RepositorioFinanzas(base)
        val servicio = RecordatoriosMovimientos(contexto, repositorio)
        val sistema = requireNotNull(contexto.getSystemService(NotificationManager::class.java))
        try {
            instrumentacion.uiAutomation.grantRuntimePermission(contexto.packageName, Manifest.permission.POST_NOTIFICATIONS)
            repositorio.registrarUsuario("Perfil de prueba", "Prueba")
            val usuario = requireNotNull(repositorio.observarUsuario().first())
            val ahora = ZonedDateTime.now()
            val hoy = ahora.toLocalDate().toEpochDay()
            servicio.actualizar(usuario.id) {
                it.copy(activo = true, minutoDelDia = ahora.hour * 60 + ahora.minute, fechaInicial = hoy)
            }
            val anterior = requireNotNull(servicio.observar(usuario.id).first())
            servicio.actualizar(usuario.id) { it.copy(recurrencia = RecurrenciaRecordatorio.Mensual) }
            val guardado = requireNotNull(servicio.observar(usuario.id).first())
            val intencionAlarma = Intent(contexto, ReceptorRecordatorios::class.java).setAction(AVISO_MOVIMIENTO)
            val flagsAlarma = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
            assertEquals(hoy, guardado.fechaInicial)
            assertEquals(RecurrenciaRecordatorio.Mensual, guardado.recurrencia)
            assertNotEquals(anterior.programacionId, guardado.programacionId)
            assertNotNull(PendingIntent.getBroadcast(contexto, 91, intencionAlarma, flagsAlarma))
            servicio.recibirAviso(anterior.programacionId, hoy)
            assertNull(repositorio.obtenerRecordatorio()?.ultimoAviso)

            servicio.recibirAviso(guardado.programacionId, hoy)
            val notificacion = withTimeout(5000L) {
                var aviso = sistema.activeNotifications.singleOrNull()
                while (aviso == null) {
                    delay(100)
                    aviso = sistema.activeNotifications.singleOrNull()
                }
                aviso.notification
            }
            assertEquals("Registra tus movimientos", notificacion.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString())
            assertEquals(
                Settings.System.DEFAULT_NOTIFICATION_URI,
                sistema.getNotificationChannel(notificacion.channelId).sound,
            )
            assertNotNull(notificacion.contentIntent)
            assertEquals(hoy, repositorio.obtenerRecordatorio()?.ultimoAviso)
            servicio.recibirAviso(guardado.programacionId, hoy)
            assertEquals(1, sistema.activeNotifications.size)

            servicio.actualizar(usuario.id) { it.copy(activo = false) }
            assertFalse(requireNotNull(repositorio.obtenerRecordatorio()).activo)
            assertNull(PendingIntent.getBroadcast(contexto, 91, intencionAlarma, flagsAlarma))
            withTimeout(5000L) { while (sistema.activeNotifications.isNotEmpty()) delay(100) }
            servicio.eliminarDatosLocales()
            assertNull(repositorio.obtenerRecordatorio())
            assertNull(repositorio.observarUsuario().first())
            servicio.recibirAviso(guardado.programacionId, hoy)
            assertTrue(sistema.activeNotifications.isEmpty())
        } finally {
            servicio.eliminarDatosLocales()
            base.close()
        }
    }
}
