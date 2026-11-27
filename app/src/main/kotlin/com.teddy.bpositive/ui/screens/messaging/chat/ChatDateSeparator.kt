package com.teddy.bpositive.ui.screens.messaging.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teddy.bpositive.ui.theme.TeddyAppTheme

private val ChatDateTextColor = Color(0xFF667781)
private val ChatDatePillColor = Color(0xFFFFFFFF)

/**
 * Compact centered date marker for the individual conversation timeline.
 *
 * This component only renders the date label. The parent owns message ordering,
 * date formatting and any wallpaper/background behind the separator.
 */
@Composable
fun ChatDateSeparator(
    date: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = ChatDatePillColor,
            contentColor = ChatDateTextColor,
            shape = RoundedCornerShape(8.dp),
            shadowElevation = 1.dp
        ) {
            Text(
                text = date,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                color = ChatDateTextColor,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.sp,
                maxLines = 1
            )
        }
    }
}

@Preview(
    name = "Chat Date Separator",
    showBackground = true,
    backgroundColor = 0xFFF7F2EA,
    widthDp = 360
)
@Composable
private fun ChatDateSeparatorPreview() {
    TeddyAppTheme(darkMode = false) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF7F2EA))
        ) {
            ChatDateSeparator(date = "Today")
        }
    }
}
