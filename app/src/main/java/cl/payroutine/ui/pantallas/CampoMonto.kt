package cl.payroutine.ui.pantallas

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

private val milesPegados = Regex("[0-9]{1,3}(?:\\.[0-9]{3})+")

internal fun normalizarMontoEditado(cambio: TextFieldValue): TextFieldValue {
    val texto = cambio.text
    if (!milesPegados.matches(texto)) return cambio
    fun mover(indice: Int) = texto.take(indice).count { it in '0'..'9' }
    return TextFieldValue(texto.replace(".", ""), TextRange(mover(cambio.selection.start), mover(cambio.selection.end)))
}

internal val SeparadoresPesos = VisualTransformation { original ->
    val valor = original.text
    if (valor.any { it !in '0'..'9' }) TransformedText(original, OffsetMapping.Identity)
    else {
        val posiciones = IntArray(valor.length + 1)
        val texto = buildString {
            for (i in valor.indices) {
                if (i > 0 && (valor.length - i) % 3 == 0) append('.')
                posiciones[i] = length
                append(valor[i])
            }
            posiciones[valor.length] = length
        }
        TransformedText(AnnotatedString(texto), object : OffsetMapping {
            override fun originalToTransformed(offset: Int) = posiciones[offset]
            override fun transformedToOriginal(offset: Int) = texto.take(offset).count { it in '0'..'9' }
        })
    }
}

@Composable
internal fun CampoMonto(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    label: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    textStyle: TextStyle = TextStyle.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    shape: Shape = OutlinedTextFieldDefaults.shape,
) {
    var campo by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(value, TextRange(value.length)))
    }
    LaunchedEffect(value) {
        if (campo.text != value) campo = normalizarMontoEditado(TextFieldValue(value, TextRange(value.length)))
    }
    OutlinedTextField(
        value = campo,
        onValueChange = {
            campo = normalizarMontoEditado(it)
            onValueChange(campo.text)
        },
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        singleLine = singleLine,
        label = label,
        prefix = prefix,
        placeholder = placeholder,
        supportingText = supportingText,
        isError = isError,
        textStyle = textStyle,
        keyboardOptions = keyboardOptions,
        visualTransformation = SeparadoresPesos,
        shape = shape,
    )
}
