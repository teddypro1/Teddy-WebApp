package com.teddy.bpositive.ui.screens.messaging.chat

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.teddy.bpositive.ui.theme.TeddyAppTheme

private val ChatHeaderIconColor = Color(0xFF171A1C)
private val ChatHeaderAvatarFallback = Color(0xFFE9D8CF)

/**
 * WhatsApp-inspired individual conversation header.
 *
 * UI only: the parent supplies navigation, call and overflow-menu callbacks.
 * No messaging, repository or backend behavior is included in this component.
 */
@Composable
fun ChatHeader(
    displayName: String,
    modifier: Modifier = Modifier,
    avatarUrl: String? = null,
    onBack: () -> Unit = {},
    onVideoCall: () -> Unit = {},
    onVoiceCall: () -> Unit = {},
    onMore: () -> Unit = {}
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(start = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(40.dp)
            ) {
                ChatHeaderBackIcon()
            }

            Box(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(ChatHeaderAvatarFallback),
                contentAlignment = Alignment.Center
            ) {
                if (!avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = "$displayName profile photo",
                        modifier = Modifier.matchParentSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = displayName.trim().firstOrNull()?.uppercase() ?: "?",
                        color = Color(0xFF493C36),
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Text(
                text = displayName,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 9.dp, end = 4.dp),
                color = Color(0xFF151719),
                fontFamily = FontFamily.Default,
                fontSize = 18.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            IconButton(onClick = onVideoCall, modifier = Modifier.size(40.dp)) {
                ChatHeaderVideoIcon()
            }

            Spacer(modifier = Modifier.width(11.dp))

            IconButton(onClick = onVoiceCall, modifier = Modifier.size(40.dp)) {
                ChatHeaderPhoneIcon()
            }

            Spacer(modifier = Modifier.width(9.dp))

            IconButton(onClick = onMore, modifier = Modifier.size(28.dp)) {
                ChatHeaderMoreIcon()
            }
        }
    }
}

@Composable
private fun ChatHeaderBackIcon() {
    Canvas(modifier = Modifier.size(24.dp)) {
        val path = Path().apply {
            moveTo(size.width * 0.78f, size.height * 0.5f)
            lineTo(size.width * 0.20f, size.height * 0.5f)
            moveTo(size.width * 0.48f, size.height * 0.20f)
            lineTo(size.width * 0.18f, size.height * 0.5f)
            lineTo(size.width * 0.48f, size.height * 0.80f)
        }
        drawPath(
            path = path,
            color = ChatHeaderIconColor,
            style = Stroke(
                width = 2.15.dp.toPx(),
                cap = StrokeCap.Square,
                join = StrokeJoin.Round
            )
        )
    }
}

@Composable
private fun ChatHeaderVideoIcon() {
    Canvas(modifier = Modifier.size(25.dp)) {
        val body = Path().apply {
            moveTo(size.width * 0.12f, size.height * 0.25f)
            quadraticTo(size.width * 0.12f, size.height * 0.17f, size.width * 0.22f, size.height * 0.17f)
            lineTo(size.width * 0.62f, size.height * 0.17f)
            quadraticTo(size.width * 0.72f, size.height * 0.17f, size.width * 0.72f, size.height * 0.27f)
            lineTo(size.width * 0.72f, size.height * 0.73f)
            quadraticTo(size.width * 0.72f, size.height * 0.83f, size.width * 0.62f, size.height * 0.83f)
            lineTo(size.width * 0.22f, size.height * 0.83f)
            quadraticTo(size.width * 0.12f, size.height * 0.83f, size.width * 0.12f, size.height * 0.73f)
            close()
        }
        val lens = Path().apply {
            moveTo(size.width * 0.75f, size.height * 0.39f)
            lineTo(size.width * 0.94f, size.height * 0.27f)
            lineTo(size.width * 0.94f, size.height * 0.73f)
            lineTo(size.width * 0.75f, size.height * 0.61f)
            close()
        }
        drawPath(
            body,
            ChatHeaderIconColor,
            style = Stroke(width = 2.05.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawPath(
            lens,
            ChatHeaderIconColor,
            style = Stroke(width = 2.05.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
private fun ChatHeaderPhoneIcon() {
    Canvas(modifier = Modifier.size(24.dp)) {
        val path = Path().apply {
            moveTo(size.width * 0.27f, size.height * 0.13f)
            quadraticTo(size.width * 0.18f, size.height * 0.10f, size.width * 0.14f, size.height * 0.22f)
            cubicTo(
                size.width * 0.10f, size.height * 0.40f,
                size.width * 0.29f, size.height * 0.72f,
                size.width * 0.53f, size.height * 0.86f
            )
            cubicTo(
                size.width * 0.67f, size.height * 0.94f,
                size.width * 0.84f, size.height * 0.89f,
                size.width * 0.88f, size.height * 0.76f
            )
            lineTo(size.width * 0.91f, size.height * 0.64f)
            quadraticTo(size.width * 0.92f, size.height * 0.58f, size.width * 0.85f, size.height * 0.55f)
            lineTo(size.width * 0.69f, size.height * 0.48f)
            quadraticTo(size.width * 0.63f, size.height * 0.45f, size.width * 0.59f, size.height * 0.51f)
            lineTo(size.width * 0.52f, size.height * 0.61f)
            cubicTo(
                size.width * 0.40f, size.height * 0.54f,
                size.width * 0.31f, size.height * 0.43f,
                size.width * 0.27f, size.height * 0.32f
            )
            lineTo(size.width * 0.36f, size.height * 0.24f)
            quadraticTo(size.width * 0.41f, size.height * 0.19f, size.width * 0.37f, size.height * 0.14f)
            close()
        }
        drawPath(
            path = path,
            color = ChatHeaderIconColor,
            style = Stroke(width = 2.05.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
private fun ChatHeaderMoreIcon() {
    Canvas(modifier = Modifier.size(24.dp)) {
        val radius = 1.8.dp.toPx()
        val centerX = size.width / 2f
        listOf(0.25f, 0.5f, 0.75f).forEach { fraction ->
            drawCircle(
                color = ChatHeaderIconColor,
                radius = radius,
                center = androidx.compose.ui.geometry.Offset(centerX, size.height * fraction)
            )
        }
    }
}

@Preview(
    name = "Chat Header - WhatsApp Style",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    widthDp = 360,
    heightDp = 120
)
@Composable
private fun ChatHeaderPreview() {
    TeddyAppTheme(darkMode = false) {
        ChatHeader(displayName = "Teddy Ahsan")
    }
}
