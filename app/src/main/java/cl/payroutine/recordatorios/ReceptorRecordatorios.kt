package cl.payroutine.recordatorios

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteException
import android.util.Log
import cl.payroutine.PayRoutineApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReceptorRecordatorios : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendiente = goAsync()
        val recordatorios = (context.applicationContext as PayRoutineApplication).recordatorios
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (intent.action == AVISO_MOVIMIENTO) {
                    recordatorios.recibirAviso(
                        intent.getStringExtra(EXTRA_PROGRAMACION),
                        intent.getLongExtra(EXTRA_DIA, Long.MIN_VALUE),
                    )
                } else {
                    recordatorios.restaurar()
                }
            } catch (error: SQLiteException) {
                Log.e("Recordatorios", "No se pudo consultar la configuración", error)
            } catch (error: SecurityException) {
                Log.e("Recordatorios", "Android bloqueó el aviso", error)
            } finally {
                pendiente.finish()
            }
        }
    }
}
