package com.salco.dragonfly.core.designsystem.component

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

/** Shows a formatted amount, coloured red when negative. */
@Composable
fun AmountText(
    formatted: String,
    negative: Boolean,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
) {
    Text(
        text = formatted,
        modifier = modifier,
        style = style,
        fontWeight = FontWeight.SemiBold,
        color = if (negative) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
    )
}
