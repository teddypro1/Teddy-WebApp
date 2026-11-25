package com.teddy.bpositive.ui.screens.messaging.chatlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teddy.bpositive.ui.theme.TeddyAppTheme

private val ChatItemPrimaryText = Color(0xFF111418)
private val ChatItemSecondaryText = Color(0xFF62686D)

/**
 * Static personal conversation row based on the first "Riya Apu" item
 * in the supplied WhatsApp Chat List screenshot.
 *
 * The avatar is an illustrative placeholder until the real image asset
 * is supplied. This component has no click, navigation or data behavior.
 */
@Composable
fun FeaturedChatItem(
    name: String = "Riya Apu",
    date: String = "11/21/26",
    messagePreview: String = "Kora to ace",
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RiyaApuAvatar()

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = name,
                    color = ChatItemPrimaryText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = date,
                    modifier = Modifier.padding(start = 8.dp),
                    color = ChatItemSecondaryText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 0.sp,
                    maxLines = 1
                )
            }

            Text(
                text = messagePreview,
                modifier = Modifier.padding(top = 2.dp),
                color = ChatItemSecondaryText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RiyaApuAvatar() {
    Box(
        modifier = Modifier
            .size(34.dp)
            .background(
                color = Color(0xFFF4E6C8),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "🌻",
            fontSize = 21.sp,
            lineHeight = 21.sp
        )
    }
}

@Preview(
    name = "Featured Chat Item - Riya Apu",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    widthDp = 360,
    heightDp = 120
)
@Composable
private fun FeaturedChatItemPreview() {
    TeddyAppTheme(darkMode = false) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(top = 8.dp)
        ) {
            FeaturedChatItem()
        }
    }
}
