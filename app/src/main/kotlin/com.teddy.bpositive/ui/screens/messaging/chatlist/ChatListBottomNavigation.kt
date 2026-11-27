package com.teddy.bpositive.ui.screens.messaging.chatlist

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teddy.bpositive.ui.theme.TeddyAppTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val TelegramBlue = Color(0xFF229ED9)
private val TelegramSelectedBackground = Color(0xFFE5F2FA)
private val NavigationText = Color(0xFF202124)
private val NavigationOutline = Color(0xFFE9EDF0)

/** Tabs displayed by the Telegram-inspired Messages navigation. */
enum class ChatListNavigationTab {
    CHATS,
    CONTACTS,
    SETTINGS,
    PROFILE
}

/**
 * Floating Telegram-style navigation used only by the Messages list screen.
 * The parent Messages Scaffold reserves a 76dp bottom slot for this 60dp pill.
 */
@Composable
fun ChatListBottomNavigation(
    modifier: Modifier = Modifier,
    selectedTab: ChatListNavigationTab = ChatListNavigationTab.CHATS,
    unreadChatsCount: Int = 18,
    onTabSelected: (ChatListNavigationTab) -> Unit = {}
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .padding(bottom = 8.dp)
                .width(320.dp)
                .height(60.dp),
            shape = CircleShape,
            color = Color.White,
            shadowElevation = 0.dp,
            tonalElevation = 0.dp,
            border = androidx.compose.foundation.BorderStroke(0.8.dp, NavigationOutline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(1.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavigationItem(
                    modifier = Modifier.weight(1f),
                    tab = ChatListNavigationTab.CHATS,
                    selected = selectedTab == ChatListNavigationTab.CHATS,
                    onClick = onTabSelected
                )
                NavigationItem(
                    modifier = Modifier.weight(1f),
                    tab = ChatListNavigationTab.CONTACTS,
                    selected = selectedTab == ChatListNavigationTab.CONTACTS,
                    onClick = onTabSelected
                )
                NavigationItem(
                    modifier = Modifier.weight(1f),
                    tab = ChatListNavigationTab.SETTINGS,
                    selected = selectedTab == ChatListNavigationTab.SETTINGS,
                    onClick = onTabSelected
                )
                NavigationItem(
                    modifier = Modifier.weight(1f),
                    tab = ChatListNavigationTab.PROFILE,
                    selected = selectedTab == ChatListNavigationTab.PROFILE,
                    onClick = onTabSelected
                )
            }
        }
    }
}

@Composable
private fun NavigationItem(
    modifier: Modifier,
    tab: ChatListNavigationTab,
    selected: Boolean,
    onClick: (ChatListNavigationTab) -> Unit
) {
    Surface(
        onClick = { onClick(tab) },
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(50),
        color = if (selected) TelegramSelectedBackground else Color.Transparent,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 1.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.height(25.dp),
                contentAlignment = Alignment.Center
            ) {
                when (tab) {
                    ChatListNavigationTab.CHATS -> ChatsNavigationIcon(
                        color = if (selected) TelegramBlue else NavigationText,
                        modifier = Modifier.size(width = 26.dp, height = 24.dp)
                    )
                    ChatListNavigationTab.CONTACTS -> ContactsNavigationIcon(
                        color = NavigationText,
                        modifier = Modifier.size(25.dp)
                    )
                    ChatListNavigationTab.SETTINGS -> SettingsNavigationIcon(
                        color = NavigationText,
                        modifier = Modifier.size(25.dp)
                    )
                    ChatListNavigationTab.PROFILE -> ProfileNavigationAvatar(
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Text(
                text = when (tab) {
                    ChatListNavigationTab.CHATS -> "Chats"
                    ChatListNavigationTab.CONTACTS -> "Contacts"
                    ChatListNavigationTab.SETTINGS -> "Settings"
                    ChatListNavigationTab.PROFILE -> "Profile"
                },
                color = if (selected) TelegramBlue else NavigationText,
                fontSize = 12.sp,
                lineHeight = 14.sp,
                letterSpacing = 0.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ChatsNavigationIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        // Two balanced, overlapping Telegram-style speech bubbles. No count badge.
        val backBubble = Path().apply {
            moveTo(size.width * 0.43f, size.height * 0.13f)
            cubicTo(size.width * 0.43f, size.height * 0.06f, size.width * 0.49f, size.height * 0.02f, size.width * 0.57f, size.height * 0.02f)
            lineTo(size.width * 0.86f, size.height * 0.02f)
            cubicTo(size.width * 0.94f, size.height * 0.02f, size.width * 0.99f, size.height * 0.08f, size.width * 0.99f, size.height * 0.17f)
            lineTo(size.width * 0.99f, size.height * 0.48f)
            cubicTo(size.width * 0.99f, size.height * 0.57f, size.width * 0.93f, size.height * 0.63f, size.width * 0.85f, size.height * 0.63f)
            lineTo(size.width * 0.72f, size.height * 0.63f)
            lineTo(size.width * 0.84f, size.height * 0.84f)
            lineTo(size.width * 0.54f, size.height * 0.63f)
            cubicTo(size.width * 0.47f, size.height * 0.62f, size.width * 0.43f, size.height * 0.56f, size.width * 0.43f, size.height * 0.48f)
            close()
        }
        drawPath(backBubble, color.copy(alpha = 0.82f))

        val frontBubble = Path().apply {
            moveTo(size.width * 0.04f, size.height * 0.29f)
            cubicTo(size.width * 0.04f, size.height * 0.16f, size.width * 0.13f, size.height * 0.08f, size.width * 0.27f, size.height * 0.08f)
            lineTo(size.width * 0.59f, size.height * 0.08f)
            cubicTo(size.width * 0.73f, size.height * 0.08f, size.width * 0.82f, size.height * 0.17f, size.width * 0.82f, size.height * 0.30f)
            lineTo(size.width * 0.82f, size.height * 0.55f)
            cubicTo(size.width * 0.82f, size.height * 0.68f, size.width * 0.73f, size.height * 0.77f, size.width * 0.59f, size.height * 0.77f)
            lineTo(size.width * 0.32f, size.height * 0.77f)
            lineTo(size.width * 0.13f, size.height * 0.98f)
            lineTo(size.width * 0.17f, size.height * 0.75f)
            cubicTo(size.width * 0.09f, size.height * 0.71f, size.width * 0.04f, size.height * 0.64f, size.width * 0.04f, size.height * 0.54f)
            close()
        }
        drawPath(frontBubble, color)
    }
}

@Composable
private fun ContactsNavigationIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val stroke = 2.dp.toPx()
        val center = Offset(size.width * 0.5f, size.height * 0.5f)
        drawCircle(color, radius = size.minDimension * 0.43f, center = center, style = Stroke(stroke))
        drawCircle(
            color,
            radius = size.minDimension * 0.13f,
            center = Offset(size.width * 0.5f, size.height * 0.36f),
            style = Stroke(stroke)
        )
        val shoulders = Path().apply {
            moveTo(size.width * 0.23f, size.height * 0.79f)
            quadraticTo(size.width * 0.25f, size.height * 0.59f, size.width * 0.5f, size.height * 0.59f)
            quadraticTo(size.width * 0.75f, size.height * 0.59f, size.width * 0.77f, size.height * 0.79f)
        }
        drawPath(
            shoulders,
            color,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
private fun SettingsNavigationIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val outerRadius = size.minDimension * 0.43f
        val rootRadius = size.minDimension * 0.34f
        val points = 64
        val gear = Path()
        for (index in 0 until points) {
            val angle = -PI / 2.0 + index * (2.0 * PI / points)
            val toothPhase = index % 8
            val radius = if (toothPhase <= 2) outerRadius else rootRadius
            val x = center.x + cos(angle).toFloat() * radius
            val y = center.y + sin(angle).toFloat() * radius
            if (index == 0) gear.moveTo(x, y) else gear.lineTo(x, y)
        }
        gear.close()

        drawPath(
            gear,
            color,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawCircle(
            color,
            radius = size.minDimension * 0.12f,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

@Composable
private fun ProfileNavigationAvatar(
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .clip(CircleShape)
            .background(Color(0xFFE7B0B7))
    ) {
        drawCircle(Color(0xFFB95F75), radius = size.minDimension * 0.5f)
        drawCircle(
            Color(0xFFF1C6A7),
            radius = size.minDimension * 0.21f,
            center = Offset(size.width * 0.5f, size.height * 0.38f)
        )
        val shoulders = Path().apply {
            moveTo(size.width * 0.12f, size.height)
            quadraticTo(size.width * 0.16f, size.height * 0.58f, size.width * 0.5f, size.height * 0.62f)
            quadraticTo(size.width * 0.84f, size.height * 0.58f, size.width * 0.88f, size.height)
            close()
        }
        drawPath(shoulders, Color(0xFF344B79))
        drawArc(
            color = Color(0xFF3A2930),
            startAngle = 195f,
            sweepAngle = 150f,
            useCenter = false,
            topLeft = Offset(size.width * 0.20f, size.height * 0.04f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.60f, size.height * 0.58f),
            style = Stroke(width = size.minDimension * 0.12f)
        )
    }
}

@Preview(
    name = "Telegram Bottom Navigation - Chats Selected",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    widthDp = 360,
    heightDp = 800
)
@Composable
private fun ChatListBottomNavigationPreview() {
    TeddyAppTheme(darkMode = false) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            ChatListBottomNavigation()
        }
    }
}
