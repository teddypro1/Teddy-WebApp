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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.teddy.bpositive.ui.theme.TeddyAppTheme

private val NewChatFabGreen = Color(0xFF1FAF62)

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
                .padding(end = 18.dp, bottom = 16.dp)
                .size(56.dp),
            shape = RoundedCornerShape(16.dp),
            color = NewChatFabGreen,
            shadowElevation = 6.dp,
            tonalElevation = 0.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                NewChatChatPlusIcon()
            }
        }
    }
}

/**
 * Custom WhatsApp-style chat-bubble-plus glyph drawn with Compose Canvas.
 * The white bubble and its tail form one continuous silhouette; the green
 * plus is centered optically inside the bubble body.
 */
@Composable
private fun NewChatChatPlusIcon(
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.size(28.dp)
    ) {
        val sx = size.width / 22f
        val sy = size.height / 22f
        val unit = minOf(sx, sy)
        val white = Color.White

        val bubble = Path().apply {
            moveTo(6.0f * sx, 3.0f * sy)
            lineTo(17.0f * sx, 3.0f * sy)
            quadraticTo(19.0f * sx, 3.0f * sy, 19.0f * sx, 5.0f * sy)
            lineTo(19.0f * sx, 13.4f * sy)
            quadraticTo(19.0f * sx, 15.4f * sy, 17.0f * sx, 15.4f * sy)
            lineTo(10.0f * sx, 15.4f * sy)
            lineTo(5.0f * sx, 19.0f * sy)
            lineTo(6.2f * sx, 15.4f * sy)
            lineTo(5.8f * sx, 15.4f * sy)
            quadraticTo(4.0f * sx, 15.4f * sy, 4.0f * sx, 13.4f * sy)
            lineTo(4.0f * sx, 5.0f * sy)
            quadraticTo(4.0f * sx, 3.0f * sy, 6.0f * sx, 3.0f * sy)
            close()
        }
        drawPath(path = bubble, color = white)

        // Plus is optically centered within the bubble's usable inner area.
        val center = Offset(11.5f * sx, 9.2f * sy)
        val arm = 2.6f * unit
        val plusStroke = 1.55f * unit

        drawLine(
            color = NewChatFabGreen,
            start = Offset(center.x - arm, center.y),
            end = Offset(center.x + arm, center.y),
            strokeWidth = plusStroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = NewChatFabGreen,
            start = Offset(center.x, center.y - arm),
            end = Offset(center.x, center.y + arm),
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
