package cl.payroutine.ui.navegacion

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat

@Composable
internal fun AjustarBarraDeEstado(fondoClaro: Boolean) {
    val actividad = LocalContext.current as? Activity
    SideEffect {
        actividad?.window?.let { ventana ->
            WindowCompat.getInsetsController(ventana, ventana.decorView)
                .isAppearanceLightStatusBars = fondoClaro
        }
    }
}
