package cl.payroutine

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import cl.payroutine.ui.navegacion.PayRoutineApp
import cl.payroutine.ui.theme.PayRoutineTheme
import cl.payroutine.viewmodel.PerfilViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val perfil = ViewModelProvider(this)[PerfilViewModel::class.java]
        setContent {
            PayRoutineTheme {
                PayRoutineApp(perfil)
            }
        }
    }
}
