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
import cl.payroutine.recordatorios.EXTRA_USUARIO
import cl.payroutine.viewmodel.FinanzasViewModel
import cl.payroutine.viewmodel.CategoriasViewModel
import cl.payroutine.viewmodel.PerfilViewModel
import cl.payroutine.viewmodel.RecordatoriosViewModel

class MainActivity : ComponentActivity() {
    private lateinit var recordatorios: RecordatoriosViewModel
    private var usuarioDelAviso by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val perfil = ViewModelProvider(this)[PerfilViewModel::class.java]
        val finanzas = ViewModelProvider(this)[FinanzasViewModel::class.java]
        val categorias = ViewModelProvider(this)[CategoriasViewModel::class.java]
        recordatorios = ViewModelProvider(this)[RecordatoriosViewModel::class.java]
        usuarioDelAviso = if (savedInstanceState == null) leerAviso(intent)
        else savedInstanceState.getString(EXTRA_USUARIO)
        setContent {
            PayRoutineTheme {
                PayRoutineApp(perfil, finanzas, categorias, recordatorios, usuarioDelAviso) { usuarioDelAviso = null }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        recordatorios.restaurar()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        leerAviso(intent)?.let { usuarioDelAviso = it }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(EXTRA_USUARIO, usuarioDelAviso)
        super.onSaveInstanceState(outState)
    }

    private fun leerAviso(intent: Intent): String? =
        intent.takeIf { it.action == ABRIR_NUEVO_MOVIMIENTO }?.getStringExtra(EXTRA_USUARIO)
}
