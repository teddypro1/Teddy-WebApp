package com.teddy.bpositive.ui.screens.messaging.chatlist

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teddy.bpositive.ui.theme.TeddyAppTheme
import kotlin.math.cos
import kotlin.math.sin

private val TelegramBlue = Color(0xFF229ED9)
private val TelegramSelectedBackground = Color(0xFFE6F3FB)
private val NavigationText = Color(0xFF202124)
private val NavigationOutline = Color(0xFFEDEFF1)

/** Tabs displayed by the Telegram-inspired Messages navigation. */
enum class ChatListNavigationTab {
    CHATS,
    CONTACTS,
    SETTINGS,
    PROFILE
}

/**
 * Floating Telegram-style navigation used only by the Messages list screen.
 * Its parent reserves a 65dp Scaffold bottom slot.
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
                .width(228.dp)
                .height(54.dp),
            shape = RoundedCornerShape(50),
            color = Color.White,
            shadowElevation = 5.dp,
            tonalElevation = 0.dp,
            border = androidx.compose.foundation.BorderStroke(0.7.dp, NavigationOutline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(1.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavigationItem(
                    modifier = Modifier.weight(1f),
                    tab = ChatListNavigationTab.CHATS,
                    selected = selectedTab == ChatListNavigationTab.CHATS,
                    unreadCount = unreadChatsCount,
                    onClick = onTabSelected
                )
                NavigationItem(
                    modifier = Modifier.weight(1f),
                    tab = ChatListNavigationTab.CONTACTS,
                    selected = selectedTab == ChatListNavigationTab.CONTACTS,
                    unreadCount = 0,
                    onClick = onTabSelected
                )
                NavigationItem(
                    modifier = Modifier.weight(1f),
                    tab = ChatListNavigationTab.SETTINGS,
                    selected = selectedTab == ChatListNavigationTab.SETTINGS,
                    unreadCount = 0,
                    onClick = onTabSelected
                )
                NavigationItem(
                    modifier = Modifier.weight(1f),
                    tab = ChatListNavigationTab.PROFILE,
                    selected = selectedTab == ChatListNavigationTab.PROFILE,
                    unreadCount = 0,
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
    unreadCount: Int,
    onClick: (ChatListNavigationTab) -> Unit
) {
    Surface(
        onClick = { onClick(tab) },
        modifier = modifier.height(46.dp),
        shape = RoundedCornerShape(50),
        color = if (selected) TelegramSelectedBackground else Color.Transparent,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
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
                        modifier = Modifier.size(width = 25.dp, height = 23.dp)
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
                        modifier = Modifier.size(23.dp)
                    )
                }

                if (tab == ChatListNavigationTab.CHATS && unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 5.dp, y = (-2).dp)
                            .size(width = 21.dp, height = 16.dp)
                            .clip(CircleShape)
                            .background(TelegramBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = unreadCount.coerceAtMost(99).toString(),
                            color = Color.White,
                            fontSize = 9.sp,
                            lineHeight = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
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
                fontSize = 11.sp,
                lineHeight = 13.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
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
        val bubble = Path().apply {
            moveTo(size.width * 0.12f, size.height * 0.18f)
            quadraticTo(size.width * 0.12f, 0f, size.width * 0.34f, 0f)
            lineTo(size.width * 0.78f, 0f)
            quadraticTo(size.width, 0f, size.width, size.height * 0.24f)
            lineTo(size.width, size.height * 0.61f)
            quadraticTo(size.width, size.height * 0.84f, size.width * 0.76f, size.height * 0.84f)
            lineTo(size.width * 0.43f, size.height * 0.84f)
            lineTo(size.width * 0.19f, size.height)
            lineTo(size.width * 0.24f, size.height * 0.79f)
            quadraticTo(size.width * 0.12f, size.height * 0.72f, size.width * 0.12f, size.height * 0.57f)
            close()
        }
        drawPath(bubble, color)
        val messageLine = color == TelegramBlue
        val lineColor = if (messageLine) Color.White else Color.White
        drawLine(
            lineColor.copy(alpha = 0.96f),
            Offset(size.width * 0.34f, size.height * 0.34f),
            Offset(size.width * 0.75f, size.height * 0.34f),
            strokeWidth = size.height * 0.075f,
            cap = StrokeCap.Round
        )
        drawLine(
            lineColor.copy(alpha = 0.96f),
            Offset(size.width * 0.34f, size.height * 0.51f),
            Offset(size.width * 0.66f, size.height * 0.51f),
            strokeWidth = size.height * 0.075f,
            cap = StrokeCap.Round
        )
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
            center = Offset(size.width * 0.5f, size.height * 0.37f),
            style = Stroke(stroke)
        )
        val shoulders = Path().apply {
            moveTo(size.width * 0.23f, size.height * 0.79f)
            quadraticTo(size.width * 0.25f, size.height * 0.59f, size.width * 0.5f, size.height * 0.59f)
            quadraticTo(size.width * 0.75f, size.height * 0.59f, size.width * 0.77f, size.height * 0.79f)
        }
        drawPath(shoulders, color, style = Stroke(stroke, cap = StrokeCap.Round))
    }
}

@Composable
private fun SettingsNavigationIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * 0.29f
        val stroke = 2.dp.toPx()
        repeat(8) { index ->
            val angle = Math.toRadians(index * 45.0)
            val inner = radius * 1.12f
            val outer = radius * 1.55f
            drawLine(
                color,
                Offset(center.x + cos(angle).toFloat() * inner, center.y + sin(angle).toFloat() * inner),
                Offset(center.x + cos(angle).toFloat() * outer, center.y + sin(angle).toFloat() * outer),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }
        drawCircle(color, radius = radius, center = center, style = Stroke(stroke))
        drawCircle(color, radius = radius * 0.34f, center = center, style = Stroke(stroke))
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
            size = Size(size.width * 0.60f, size.height * 0.58f),
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
