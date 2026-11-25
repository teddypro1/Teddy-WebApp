package com.teddy.bpositive.ui.screens.messaging.chatlist

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teddy.bpositive.ui.theme.TeddyAppTheme

/**
 * Static, UI-only Chat List header.
 *
 * Positioned at the top of the app content area, immediately below
 * the Android system status bar. No navigation, menu action, state,
 * repository, API or backend integration is included.
 */
@Composable
fun ChatListHeader(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(start = 12.dp, end = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "WhatsApp",
                color = Color(0xFF159B58),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.35).sp,
                maxLines = 1
            )

            Canvas(
                modifier = Modifier.size(width = 12.dp, height = 24.dp)
            ) {
                val dotRadius = 1.9.dp.toPx()
                val centerX = size.width / 2f
                val dotCenterYPositions = listOf(6.6.dp, 12.dp, 17.4.dp)

                dotCenterYPositions.forEach { position ->
                    drawCircle(
                        color = Color(0xFF161616),
                        radius = dotRadius,
                        center = Offset(
                            x = centerX,
                            y = position.toPx()
                        )
                    )
                }
            }
        }
    }
}

@Preview(
    name = "Chat List Header - Top Position",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    widthDp = 360,
    heightDp = 800
)
@Composable
private fun ChatListHeaderPreview() {
    TeddyAppTheme(darkMode = false) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 24.dp)
        ) {
            ChatListHeader()
        }
    }
}


/**
 * Temporary in-app inspection screen for the completed Header component.
 * The normal Android Studio/CodeAssist @Preview remains available.
 */
@Composable
fun ChatListHeaderAppPreview(
    contentPadding: PaddingValues,
    onContinueToMessages: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(contentPadding)
    ) {
        ChatListHeader()

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Temporary Header Preview",
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.titleSmall,
            color = Color(0xFF555555)
        )

        Text(
            text = "The original Messages screen is unchanged. Continue when you finish checking the header.",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF777777)
        )

        TextButton(
            onClick = onContinueToMessages,
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Text("Continue to Messages")
        }
    }
}
