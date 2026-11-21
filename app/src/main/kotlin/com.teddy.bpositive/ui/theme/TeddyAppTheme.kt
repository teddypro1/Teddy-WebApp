package com.teddy.bpositive.ui.theme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun TeddyAppTheme(
    darkMode: Boolean,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkMode) {
            darkColorScheme(
                primary = Color(0xFF8BA9FF),
                secondary = Color(0xFFB8C7FF),
                background = Color(0xFF10131C),
                surface = Color(0xFF1A1E29)
            )
        } else {
            lightColorScheme(
                primary = Color(0xFF3459D6),
                secondary = Color(0xFF607CE5),
                background = Color(0xFFF3F5FB),
                surface = Color.White
            )
        },
        content = content
    )
}

@Preview(showBackground = true, name = "Teddy App Theme - Light")
@Composable
private fun TeddyAppThemeLightPreview() {
    TeddyAppTheme(darkMode = false) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Teddy WebApp")
            Text("Light theme preview", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
@Preview(showBackground = true, name = "Teddy App Theme - Dark")
@Composable
private fun TeddyAppThemeDarkPreview() {
    TeddyAppTheme(darkMode = true) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Teddy WebApp")
            Text("Dark theme preview", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
