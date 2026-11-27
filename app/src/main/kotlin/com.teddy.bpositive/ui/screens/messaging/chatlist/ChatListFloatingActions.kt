package com.teddy.bpositive.ui.screens.messaging.chatlist

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.teddy.bpositive.ui.theme.TeddyAppTheme

private val NewChatFabGreen = Color(0xFF1FAF62)
private val NewChatFabIconGreen = NewChatFabGreen

/**
 * WhatsApp-inspired New Chat floating action button.
 *
 * This component intentionally contains only the lower New Chat FAB.
 * The Meta AI floating action button is not part of this component.
 */
@Composable
fun ChatListFloatingActions(
    modifier: Modifier = Modifier,
    onNewChatClick: () -> Unit = {}
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomEnd
    ) {
        Surface(
            onClick = onNewChatClick,
            modifier = Modifier
                .padding(end = 16.dp, bottom = 16.dp)
                .size(56.dp),
            shape = RoundedCornerShape(17.dp),
            color = NewChatFabGreen,
            shadowElevation = 5.dp,
            tonalElevation = 0.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                NewChatChatPlusIcon()
            }
        }
    }
}

/**
 * Custom chat-bubble-plus glyph drawn with Canvas to avoid a generic
 * plus icon and keep the mark visually close to WhatsApp's New Chat symbol.
 */
@Composable
private fun NewChatChatPlusIcon(
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.size(28.dp)
    ) {
        val sx = size.width / 28f
        val sy = size.height / 28f
        val unit = minOf(sx, sy)
        val white = Color.White

        // Main speech-bubble body.
        drawRoundRect(
            color = white,
            topLeft = Offset(5.2f * sx, 5.0f * sy),
            size = Size(18.0f * sx, 14.2f * sy),
            cornerRadius = CornerRadius(3.0f * unit, 3.0f * unit)
        )

        // Small lower-left speech tail.
        val tail = Path().apply {
            moveTo(8.0f * sx, 18.0f * sy)
            lineTo(6.2f * sx, 22.0f * sy)
            lineTo(12.0f * sx, 19.0f * sy)
            close()
        }
        drawPath(path = tail, color = white)

        // Green plus sign inset into the white chat bubble.
        val plusStroke = 1.9f * unit
        val centerX = 14.2f * sx
        val centerY = 12.1f * sy
        drawLine(
            color = NewChatFabIconGreen,
            start = Offset(centerX - 3.2f * sx, centerY),
            end = Offset(centerX + 3.2f * sx, centerY),
            strokeWidth = plusStroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = NewChatFabIconGreen,
            start = Offset(centerX, centerY - 3.2f * sy),
            end = Offset(centerX, centerY + 3.2f * sy),
            strokeWidth = plusStroke,
            cap = StrokeCap.Round
        )
    }
}

@Preview(
    name = "WhatsApp New Chat FAB",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    widthDp = 360,
    heightDp = 640
)
@Composable
private fun ChatListFloatingActionsPreview() {
    TeddyAppTheme(darkMode = false) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            ChatListFloatingActions()
        }
    }
}
