package com.teddy.bpositive.ui.screens.messaging.chatlist

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
            shadowElevation = 5.dp,
            tonalElevation = 0.dp,
            border = androidx.compose.foundation.BorderStroke(0.7.dp, NavigationOutline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 7.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
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
        modifier = modifier.height(50.dp),
        shape = CircleShape,
        color = if (selected) TelegramSelectedBackground else Color.Transparent,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.height(26.dp),
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

                if (tab == ChatListNavigationTab.CHATS && unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 5.dp, y = (-2).dp)
                            .size(width = 25.dp, height = 19.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .padding(1.5.dp)
                            .clip(CircleShape)
                            .background(TelegramBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = unreadCount.coerceAtMost(99).toString(),
                            color = Color.White,
                            fontSize = 9.sp,
                            lineHeight = 10.sp,
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
                fontSize = 12.sp,
                lineHeight = 14.sp,
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
        val rear = Path().apply {
            moveTo(size.width * 0.04f, size.height * 0.28f)
            quadraticTo(size.width * 0.04f, size.height * 0.08f, size.width * 0.25f, size.height * 0.08f)
            lineTo(size.width * 0.58f, size.height * 0.08f)
            quadraticTo(size.width * 0.73f, size.height * 0.08f, size.width * 0.73f, size.height * 0.27f)
            lineTo(size.width * 0.73f, size.height * 0.48f)
            lineTo(size.width * 0.48f, size.height * 0.48f)
            lineTo(size.width * 0.29f, size.height * 0.66f)
            lineTo(size.width * 0.33f, size.height * 0.48f)
            lineTo(size.width * 0.23f, size.height * 0.48f)
            quadraticTo(size.width * 0.04f, size.height * 0.48f, size.width * 0.04f, size.height * 0.28f)
            close()
        }
        drawPath(rear, color.copy(alpha = 0.78f))

        val front = Path().apply {
            moveTo(size.width * 0.28f, size.height * 0.35f)
            quadraticTo(size.width * 0.28f, size.height * 0.13f, size.width * 0.49f, size.height * 0.13f)
            lineTo(size.width * 0.83f, size.height * 0.13f)
            quadraticTo(size.width * 0.98f, size.height * 0.13f, size.width * 0.98f, size.height * 0.33f)
            lineTo(size.width * 0.98f, size.height * 0.67f)
            quadraticTo(size.width * 0.98f, size.height * 0.87f, size.width * 0.79f, size.height * 0.87f)
            lineTo(size.width * 0.55f, size.height * 0.87f)
            lineTo(size.width * 0.34f, size.height)
            lineTo(size.width * 0.39f, size.height * 0.84f)
            quadraticTo(size.width * 0.28f, size.height * 0.78f, size.width * 0.28f, size.height * 0.63f)
            close()
        }
        drawPath(front, color)
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

        // Telegram's Settings glyph is an outlined gear, not a solid gear.
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
