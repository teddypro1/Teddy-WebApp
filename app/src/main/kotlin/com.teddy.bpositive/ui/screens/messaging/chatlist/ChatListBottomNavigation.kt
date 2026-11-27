package com.teddy.bpositive.ui.screens.messaging.chatlist

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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

private val TelegramBlue = Color(0xFF229ED9)
private val TelegramSelectedBackground = Color(0xFFE8F4FC)
private val NavigationText = Color(0xFF17191C)
private val NavigationBorder = Color(0xFFF0F1F2)

/** Tabs available in the Telegram-inspired bottom navigation. */
enum class ChatListNavigationTab {
    CHATS,
    CONTACTS,
    SETTINGS,
    PROFILE
}

/**
 * Telegram-inspired floating bottom navigation pill.
 *
 * This is a standalone UI component. It does not change the app's existing
 * navigation destinations or integrate itself into the Messages screen.
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
                .padding(bottom = 10.dp)
                .width(228.dp)
                .height(40.dp)
                .border(0.6.dp, NavigationBorder, RoundedCornerShape(50)),
            shape = RoundedCornerShape(50),
            color = Color.White,
            shadowElevation = 3.dp,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 3.dp, vertical = 3.dp),
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
        modifier = modifier.height(34.dp),
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
                modifier = Modifier.height(19.dp),
                contentAlignment = Alignment.Center
            ) {
                when (tab) {
                    ChatListNavigationTab.CHATS -> ChatBubbleNavigationIcon(
                        color = if (selected) TelegramBlue else NavigationText,
                        modifier = Modifier.size(width = 19.dp, height = 17.dp)
                    )
                    ChatListNavigationTab.CONTACTS -> ContactNavigationIcon(
                        color = NavigationText,
                        modifier = Modifier.size(17.dp)
                    )
                    ChatListNavigationTab.SETTINGS -> SettingsNavigationIcon(
                        color = NavigationText,
                        modifier = Modifier.size(17.dp)
                    )
                    ChatListNavigationTab.PROFILE -> ProfileNavigationAvatar(
                        modifier = Modifier.size(16.dp)
                    )
                }

                if (tab == ChatListNavigationTab.CHATS && unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 0.dp)
                            .size(width = 15.dp, height = 12.dp)
                            .clip(CircleShape)
                            .background(TelegramBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = unreadCount.coerceAtMost(99).toString(),
                            color = Color.White,
                            fontSize = 7.sp,
                            lineHeight = 8.sp,
                            fontWeight = FontWeight.SemiBold,
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
                fontSize = 9.5.sp,
                lineHeight = 11.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ChatBubbleNavigationIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val stroke = 1.8.dp.toPx()
        val main = Path().apply {
            moveTo(size.width * 0.12f, size.height * 0.18f)
            quadraticTo(size.width * 0.12f, 0f, size.width * 0.34f, 0f)
            lineTo(size.width * 0.78f, 0f)
            quadraticTo(size.width, 0f, size.width, size.height * 0.25f)
            lineTo(size.width, size.height * 0.62f)
            quadraticTo(size.width, size.height * 0.86f, size.width * 0.76f, size.height * 0.86f)
            lineTo(size.width * 0.43f, size.height * 0.86f)
            lineTo(size.width * 0.20f, size.height)
            lineTo(size.width * 0.25f, size.height * 0.82f)
            quadraticTo(size.width * 0.12f, size.height * 0.76f, size.width * 0.12f, size.height * 0.58f)
            close()
        }
        drawPath(main, color = color)
        val second = Path().apply {
            moveTo(size.width * 0.38f, size.height * 0.34f)
            quadraticTo(size.width * 0.38f, size.height * 0.22f, size.width * 0.53f, size.height * 0.22f)
            lineTo(size.width * 0.86f, size.height * 0.22f)
            quadraticTo(size.width, size.height * 0.22f, size.width, size.height * 0.36f)
        }
        drawPath(second, color = Color.White.copy(alpha = 0.9f), style = Stroke(stroke * 0.55f, cap = StrokeCap.Round))
    }
}

@Composable
private fun ContactNavigationIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val stroke = 1.8.dp.toPx()
        drawCircle(color, style = Stroke(stroke))
        drawCircle(
            color,
            radius = size.minDimension * 0.16f,
            center = Offset(size.width * 0.5f, size.height * 0.38f),
            style = Stroke(stroke)
        )
        val shoulders = Path().apply {
            moveTo(size.width * 0.22f, size.height * 0.82f)
            quadraticTo(size.width * 0.24f, size.height * 0.60f, size.width * 0.50f, size.height * 0.60f)
            quadraticTo(size.width * 0.76f, size.height * 0.60f, size.width * 0.78f, size.height * 0.82f)
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
        val outer = size.minDimension * 0.43f
        val stroke = 1.8.dp.toPx()
        drawCircle(color, radius = outer, center = center, style = Stroke(stroke))
        drawCircle(color, radius = outer * 0.32f, center = center, style = Stroke(stroke))
        repeat(8) { index ->
            val angle = Math.toRadians(index * 45.0)
            val innerRadius = outer * 1.02f
            val outerRadius = outer * 1.28f
            val start = Offset(
                center.x + kotlin.math.cos(angle).toFloat() * innerRadius,
                center.y + kotlin.math.sin(angle).toFloat() * innerRadius
            )
            val end = Offset(
                center.x + kotlin.math.cos(angle).toFloat() * outerRadius,
                center.y + kotlin.math.sin(angle).toFloat() * outerRadius
            )
            drawLine(color, start, end, strokeWidth = stroke, cap = StrokeCap.Round)
        }
    }
}

@Composable
private fun ProfileNavigationAvatar(
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .clip(CircleShape)
            .background(Color(0xFFE8A4B2))
    ) {
        drawCircle(Color(0xFFB94D69), radius = size.minDimension * 0.49f)
        drawCircle(
            Color(0xFFF2C6A7),
            radius = size.minDimension * 0.22f,
            center = Offset(size.width * 0.5f, size.height * 0.39f)
        )
        val shoulders = Path().apply {
            moveTo(size.width * 0.14f, size.height)
            quadraticTo(size.width * 0.17f, size.height * 0.58f, size.width * 0.50f, size.height * 0.62f)
            quadraticTo(size.width * 0.83f, size.height * 0.58f, size.width * 0.86f, size.height)
            close()
        }
        drawPath(shoulders, Color(0xFF263B68))
        drawArc(
            color = Color(0xFF3A2530),
            startAngle = 190f,
            sweepAngle = 160f,
            useCenter = false,
            topLeft = Offset(size.width * 0.22f, size.height * 0.06f),
            size = Size(size.width * 0.56f, size.height * 0.57f),
            style = Stroke(width = size.minDimension * 0.13f)
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
