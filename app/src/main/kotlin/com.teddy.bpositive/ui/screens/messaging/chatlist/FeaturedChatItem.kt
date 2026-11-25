package com.teddy.bpositive.ui.screens.messaging.chatlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
private val ChatItemDraftGreen = Color(0xFF159B58)

private data class FeaturedChatSample(
    val name: String,
    val date: String,
    val message: String,
    val avatar: String,
    val avatarBackground: Color,
    val isDraft: Boolean = false
)

/**
 * One compact personal conversation row, styled after the WhatsApp Chat List.
 * The preview below shows multiple sample rows to judge real list density.
 * Avatar artwork is illustrative placeholder content, not a real contact photo.
 */
@Composable
fun FeaturedChatItem(
    name: String = "Riya Apu",
    date: String = "11/21/26",
    messagePreview: String = "Kora to ace",
    avatar: String = "🌻",
    avatarBackground: Color = Color(0xFFF4E6C8),
    isDraft: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(53.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(avatarBackground, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = avatar,
                fontSize = 20.sp,
                lineHeight = 22.sp
            )
        }

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
                    modifier = Modifier.weight(1f),
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isDraft) {
                    Text(
                        text = "Draft: ",
                        color = ChatItemDraftGreen,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }

                Text(
                    text = messagePreview,
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
}

@Composable
fun FeaturedChatList(modifier: Modifier = Modifier) {
    val samples = listOf(
        FeaturedChatSample(
            "Riya Apu", "11/21/26", "Kora to ace",
            "🌻", Color(0xFFF4E6C8)
        ),
        FeaturedChatSample(
            "Nusrat Jahan", "11/20/26", "Ajke class hobe?",
            "N", Color(0xFFE8DDF8)
        ),
        FeaturedChatSample(
            "Shakila apu", "11/18/26", "Thik ache, pore kotha hobe",
            "🌿", Color(0xFFDCEBDD)
        ),
        FeaturedChatSample(
            "Teddy Ahsan (You)", "11/17/26", "https://example.com/shared-file...",
            "T", Color(0xFFF8DFD8), isDraft = true
        ),
        FeaturedChatSample(
            "Mim Akter", "11/15/26", "Photo pathiyechi dekho",
            "M", Color(0xFFF5E3D2)
        ),
        FeaturedChatSample(
            "Study Group", "11/14/26", "Rahim: Notes gula peyecho?",
            "👥", Color(0xFFFFEBCB)
        ),
        FeaturedChatSample(
            "Soma Bhabi", "11/13/26", "Jitla vlo hoto eta fir ki",
            "S", Color(0xFFE8E0FA)
        ),
        FeaturedChatSample(
            "Tasdit", "11/12/26", "You reacted 🙂 to a message",
            "T", Color(0xFFDCE8F5)
        )
    )

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White),
        contentPadding = PaddingValues(bottom = 84.dp)
    ) {
        items(samples) { sample ->
            FeaturedChatItem(
                name = sample.name,
                date = sample.date,
                messagePreview = sample.message,
                avatar = sample.avatar,
                avatarBackground = sample.avatarBackground,
                isDraft = sample.isDraft
            )
        }
    }
}

@Preview(
    name = "Featured Chat Items - WhatsApp List",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    widthDp = 360,
    heightDp = 480
)
@Composable
private fun FeaturedChatItemPreview() {
    TeddyAppTheme(darkMode = false) {
        FeaturedChatList()
    }
}
