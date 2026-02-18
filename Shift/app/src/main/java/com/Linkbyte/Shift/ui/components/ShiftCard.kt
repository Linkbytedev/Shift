package com.Linkbyte.Shift.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.Linkbyte.Shift.ui.theme.*

@Composable
fun ShiftCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color? = MaterialTheme.colorScheme.outlineVariant,
    elevation: Dp = 0.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = if (borderColor != null) BorderStroke(1.dp, borderColor) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        content = content
    )
}

@Composable
fun ShiftGlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    ShiftCard(
        modifier = modifier,
        backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        borderColor = MaterialTheme.colorScheme.outline,
        content = content
    )
}

@Composable
fun ShiftPaddedCard(
    modifier: Modifier = Modifier,
    padding: Dp = 24.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    ShiftCard(modifier = modifier) {
        Column(modifier = Modifier.padding(padding)) {
            content()
        }
    }
}
