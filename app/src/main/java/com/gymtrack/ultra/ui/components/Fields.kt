package com.gymtrack.ultra.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymtrack.ultra.ui.theme.GtColors

enum class FieldKind { TEXT, NUMBER, DECIMAL }

/** Płaskie pole tekstowe w stylu aplikacji: grafitowe tło, lime kursor i obramowanie przy edycji. */
@Composable
fun GtTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    kind: FieldKind = FieldKind.TEXT,
    maxLength: Int = 40,
    suffix: String? = null,
    label: String = placeholder,
    imeAction: ImeAction = ImeAction.Done,
    onDone: () -> Unit = {},
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    val big = kind != FieldKind.TEXT
    val style = TextStyle(
        color = GtColors.TextPrimary,
        fontSize = if (big) 18.sp else 16.sp,
        fontWeight = if (big) FontWeight.SemiBold else FontWeight.Normal,
        fontFeatureSettings = "tnum",
    )
    BasicTextField(
        value = value,
        onValueChange = { input ->
            val filtered = when (kind) {
                FieldKind.TEXT -> input.replace("\n", "")
                FieldKind.NUMBER -> input.filter { it.isDigit() }
                FieldKind.DECIMAL -> input.filter { it.isDigit() || it == ',' || it == '.' }
            }
            onValueChange(filtered.take(maxLength))
        },
        singleLine = true,
        textStyle = style,
        cursorBrush = SolidColor(GtColors.Lime),
        keyboardOptions = KeyboardOptions(
            keyboardType = when (kind) {
                FieldKind.TEXT -> KeyboardType.Text
                FieldKind.NUMBER -> KeyboardType.Number
                FieldKind.DECIMAL -> KeyboardType.Decimal
            },
            capitalization = if (kind == FieldKind.TEXT) KeyboardCapitalization.Sentences else KeyboardCapitalization.None,
            imeAction = imeAction,
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() }, onNext = { onDone() }),
        modifier = modifier
            .height(52.dp)
            .clip(shape)
            .background(GtColors.Elevated)
            .border(1.dp, if (focused) GtColors.Lime.copy(alpha = 0.6f) else GtColors.Elevated, shape)
            .onFocusChanged { focused = it.isFocused }
            .semantics { contentDescription = label },
        decorationBox = { inner ->
            Box(Modifier.padding(horizontal = 14.dp), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(placeholder, style = style.copy(color = GtColors.TextMuted, fontWeight = FontWeight.Normal))
                }
                inner()
                if (suffix != null && value.isNotEmpty()) {
                    Text(
                        suffix,
                        style = MaterialTheme.typography.labelLarge,
                        color = GtColors.TextSecondary,
                        modifier = Modifier.align(Alignment.CenterEnd),
                    )
                }
            }
        },
    )
}
