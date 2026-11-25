package cl.payroutine

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import cl.payroutine.datos.local.BaseDatosPayRoutine
import cl.payroutine.datos.local.entidad.FrecuenciaAvisoPago
import cl.payroutine.datos.local.entidad.TipoMovimiento
import cl.payroutine.datos.repositorio.CuotaProgramada
import cl.payroutine.datos.repositorio.NuevaDeudaEnCuotas
import cl.payroutine.datos.repositorio.RepositorioFinanzas
import cl.payroutine.recordatorios.AVISO_PAGOS
import cl.payroutine.recordatorios.CANAL_PAGOS
import cl.payroutine.recordatorios.ReceptorRecordatorios
import cl.payroutine.recordatorios.RecordatoriosMovimientos
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 33)
class AvisosPagosTest {
    @Test
    fun programaCuotasIndependientesCancelaAlPagarYReanudaAlRevertir() = runBlocking {
        val prueba = InstrumentationRegistry.getInstrumentation()
        val contexto = prueba.targetContext
        val sistema = requireNotNull(contexto.getSystemService(NotificationManager::class.java))
        val base = Room.inMemoryDatabaseBuilder(contexto, BaseDatosPayRoutine::class.java).build()
        val repo = RepositorioFinanzas(base)
        val servicio = RecordatoriosMovimientos(contexto, repo)
        val zona = ZoneId.systemDefault()
        val hoy = LocalDate.now(zona)
        val aviso = hoy.atTime(20, 0).atZone(zona)
        val antes = aviso.minusMinutes(5)
        val intencion = Intent(contexto, ReceptorRecordatorios::class.java).setAction(AVISO_PAGOS)
        val flags = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
        try {
            prueba.uiAutomation.grantRuntimePermission(contexto.packageName, Manifest.permission.POST_NOTIFICATIONS)
            repo.registrarUsuario("Perfil de prueba", "Prueba")
            val usuario = requireNotNull(repo.observarUsuario().first()).id
            val categoria = repo.observarCategorias().first().first { it.tipo == TipoMovimiento.Gasto }
            repo.registrarDeuda(NuevaDeudaEnCuotas("Crédito", 3000, categoria.id, 0,
                listOf(CuotaProgramada(hoy, 1000), CuotaProgramada(hoy.plusDays(1), 2000))))
            val (primera, segunda) = repo.observarPagos().first()
            servicio.seleccionarPagos(usuario, listOf(primera.id, segunda.id), true)
            assertNull(PendingIntent.getBroadcast(contexto, 94, intencion, flags))
            repo.guardarPreferenciasAvisosPago(usuario) { it.copy(activo = true, frecuencia = FrecuenciaAvisoPago.CadaTresDias) }
            assertEquals(aviso, repo.planificarAvisosPago(antes, false, false).siguiente)
            val recibido = aviso.toInstant().toEpochMilli()
            servicio.recibirAvisosPagos(recibido, aviso)
            servicio.recibirAvisosPagos(recibido, aviso)
            val notificacion = withTimeout(5_000) {
                while (sistema.activeNotifications.none { it.tag == CANAL_PAGOS }) delay(50)
                sistema.activeNotifications.single { it.tag == CANAL_PAGOS }
            }
            assertEquals(primera.id, notificacion.id)
            assertTrue(notificacion.notification.extras.getString(Notification.EXTRA_TITLE).orEmpty().contains("Cuota 1"))
            assertTrue(notificacion.notification.extras.getString(Notification.EXTRA_TEXT).orEmpty().contains("Vence el"))
            assertNotNull(notificacion.notification.contentIntent)
            assertEquals(aviso.plusDays(1), repo.planificarAvisosPago(aviso, false, false).siguiente)

            repo.pagarVencimientos(listOf(primera.id), hoy, "")
            servicio.restaurarPagos()
            withTimeout(5_000) {
                while (sistema.activeNotifications.any { it.tag == CANAL_PAGOS && it.id == primera.id }) delay(50)
            }
            val movimiento = requireNotNull(repo.observarPagos().first().first().movimientoId)
            repo.deshacerPago(primera.id, movimiento)
            val restaurado = repo.planificarAvisosPago(aviso.plusMinutes(5), false, false)
            assertTrue(restaurado.pendientes.any { it.id == primera.id })
            assertEquals(aviso.plusDays(1), restaurado.siguiente)
            servicio.seleccionarPagos(usuario, listOf(primera.id), false)
            assertEquals(listOf(segunda.id), repo.observarSeleccionAvisosPago().first())
            repo.eliminar(segunda)
            servicio.restaurarPagos()
            assertNull(PendingIntent.getBroadcast(contexto, 94, intencion, flags))
            servicio.eliminarDatosLocales()
            assertNull(repo.observarPreferenciasAvisosPago(usuario).first())
        } finally {
            servicio.eliminarDatosLocales()
            base.close()
        }
    }
}
