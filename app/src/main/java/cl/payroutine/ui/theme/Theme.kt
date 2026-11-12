package cl.payroutine.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ColoresPayRoutine = lightColorScheme(
    primary = VerdeMarca,
    onPrimary = Color.White,
    secondary = AzulNoche,
    onSecondary = Color.White,
    background = Fondo,
    onBackground = Texto,
    surface = Color.White,
    onSurface = Texto,
    onSurfaceVariant = TextoSecundario,
    outline = Borde,
    error = RojoGasto,
)

@Composable
fun PayRoutineTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ColoresPayRoutine,
        typography = Typography,
        content = content,
    )
}
