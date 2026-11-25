package cl.payroutine

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider
import cl.payroutine.ui.navegacion.PayRoutineApp
import cl.payroutine.ui.theme.PayRoutineTheme
import cl.payroutine.recordatorios.ABRIR_NUEVO_MOVIMIENTO
import cl.payroutine.recordatorios.ABRIR_PRESUPUESTO
import cl.payroutine.recordatorios.ABRIR_PAGO
import cl.payroutine.recordatorios.EXTRA_USUARIO
import cl.payroutine.recordatorios.EXTRA_VENCIMIENTO
import cl.payroutine.viewmodel.FinanzasViewModel
import cl.payroutine.viewmodel.CategoriasViewModel
import cl.payroutine.viewmodel.PerfilViewModel
import cl.payroutine.viewmodel.PresupuestoViewModel
import cl.payroutine.viewmodel.RecordatoriosViewModel

class MainActivity : ComponentActivity() {
    private lateinit var recordatorios: RecordatoriosViewModel
    private var usuarioDelAviso by mutableStateOf<String?>(null)
    private var avisoDePresupuesto by mutableStateOf(false)
    private var vencimientoDelAviso by mutableStateOf<Int?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val perfil = ViewModelProvider(this)[PerfilViewModel::class.java]
        val finanzas = ViewModelProvider(this)[FinanzasViewModel::class.java]
        val categorias = ViewModelProvider(this)[CategoriasViewModel::class.java]
        val presupuesto = ViewModelProvider(this)[PresupuestoViewModel::class.java]
        recordatorios = ViewModelProvider(this)[RecordatoriosViewModel::class.java]
        if (savedInstanceState == null) leerAviso(intent)
        else {
            usuarioDelAviso = savedInstanceState.getString(EXTRA_USUARIO)
            avisoDePresupuesto = savedInstanceState.getBoolean(ABRIR_PRESUPUESTO)
            vencimientoDelAviso = savedInstanceState.getInt(EXTRA_VENCIMIENTO).takeIf { it > 0 }
        }
        setContent {
            PayRoutineTheme {
                PayRoutineApp(perfil, finanzas, categorias, recordatorios, presupuesto, usuarioDelAviso, avisoDePresupuesto, vencimientoDelAviso) {
                    usuarioDelAviso = null
                    avisoDePresupuesto = false
                    vencimientoDelAviso = null
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        recordatorios.restaurar()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        leerAviso(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(EXTRA_USUARIO, usuarioDelAviso)
        outState.putBoolean(ABRIR_PRESUPUESTO, avisoDePresupuesto)
        vencimientoDelAviso?.let { outState.putInt(EXTRA_VENCIMIENTO, it) }
        super.onSaveInstanceState(outState)
    }

    private fun leerAviso(intent: Intent) {
        if (intent.action in setOf(ABRIR_NUEVO_MOVIMIENTO, ABRIR_PRESUPUESTO, ABRIR_PAGO)) {
            avisoDePresupuesto = intent.action == ABRIR_PRESUPUESTO
            vencimientoDelAviso = intent.getIntExtra(EXTRA_VENCIMIENTO, 0).takeIf { intent.action == ABRIR_PAGO && it > 0 }
            if (intent.action == ABRIR_PAGO && vencimientoDelAviso == null) {
                usuarioDelAviso = null
                return
            }
            usuarioDelAviso = intent.getStringExtra(EXTRA_USUARIO)
        }
    }
}
