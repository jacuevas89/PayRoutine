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
import cl.payroutine.recordatorios.ReceptorRecordatorios
import cl.payroutine.recordatorios.RecordatoriosMovimientos
import java.time.ZonedDateTime
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
