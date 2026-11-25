package cl.payroutine

import android.app.Application
import android.database.sqlite.SQLiteException
import android.util.Log
import cl.payroutine.datos.local.BaseDatosPayRoutine
import cl.payroutine.datos.repositorio.RepositorioFinanzas
import cl.payroutine.recordatorios.RecordatoriosMovimientos
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class PayRoutineApplication : Application() {
    val repositorio by lazy {
        RepositorioFinanzas(BaseDatosPayRoutine.crear(this))
    }
    internal val recordatorios by lazy { RecordatoriosMovimientos(this, repositorio) }

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            repositorio.observarPagos().catch { error ->
                if (error !is SQLiteException) throw error
                Log.e("Recordatorios", "No se pudieron observar los vencimientos", error)
            }.collect {
                try {
                    recordatorios.restaurarPagos()
                } catch (error: SQLiteException) {
                    Log.e("Recordatorios", "No se pudieron consultar los avisos de pagos", error)
                } catch (error: SecurityException) {
                    Log.e("Recordatorios", "No se pudieron programar los avisos de pagos", error)
                }
            }
        }
    }
}
