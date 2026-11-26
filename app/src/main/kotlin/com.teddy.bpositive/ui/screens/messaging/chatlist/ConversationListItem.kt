package com.teddy.bpositive.ui.screens.messaging.chatlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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

private val ConversationPrimaryText = Color(0xFF111418)
private val ConversationSecondaryText = Color(0xFF62686D)
private val ConversationDraftGreen = Color(0xFF159B58)

private data class ConversationSample(
    val name: String,
    val date: String,
    val message: String,
    val avatar: String,
    val avatarBackground: Color,
    val isDraft: Boolean = false
)

/**
 * Reusable WhatsApp-inspired conversation row.
 * Avatar content is illustrative placeholder data, not a real contact photo.
 */
@Composable
fun ConversationListItem(
    name: String,
    date: String,
    messagePreview: String,
    avatar: String,
    avatarBackground: Color,
    isDraft: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .height(76.dp)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(avatarBackground, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = avatar,
                fontSize = if (avatar.length == 1) 29.sp else 27.sp,
                lineHeight = 30.sp
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp),
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
                    color = ConversationPrimaryText,
                    fontSize = 18.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.1).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = date,
                    modifier = Modifier.padding(start = 8.dp),
                    color = ConversationSecondaryText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 0.sp,
                    maxLines = 1
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isDraft) {
                    Text(
                        text = "Draft: ",
                        color = ConversationDraftGreen,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }

                Text(
                    text = messagePreview,
                    color = ConversationSecondaryText,
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 0.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Sample conversation list used by the current Messages UI and component preview.
 * Replace the sample source with real conversation data in a later integration step.
 */
@Composable
fun ConversationList(modifier: Modifier = Modifier) {
    val samples = listOf(
        ConversationSample(
            "Riya Apu", "11/21/26", "Kora to ace",
            "🌻", Color(0xFFF4E6C8)
        ),
        ConversationSample(
            "Nusrat Jahan", "11/20/26", "Ajke class hobe?",
            "N", Color(0xFFE8DDF8)
        ),
        ConversationSample(
            "Shakila apu", "11/18/26", "Thik ache, pore kotha hobe",
            "🌿", Color(0xFFDCEBDD)
        ),
        ConversationSample(
            "Teddy Ahsan (You)", "11/17/26", "https://example.com/shared-file...",
            "T", Color(0xFFF8DFD8), isDraft = true
        ),
        ConversationSample(
            "Mim Akter", "11/15/26", "Photo pathiyechi dekho",
            "M", Color(0xFFF5E3D2)
        ),
        ConversationSample(
            "Study Group", "11/14/26", "Rahim: Notes gula peyecho?",
            "👥", Color(0xFFFFEBCB)
        ),
        ConversationSample(
            "Soma Bhabi", "11/13/26", "Jitla vlo hoto eta fir ki",
            "S", Color(0xFFE8E0FA)
        ),
        ConversationSample(
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
            ConversationListItem(
                name = sample.name,
                date = sample.date,
                messagePreview = sample.message,
                avatar = sample.avatar,
                avatarBackground = sample.avatarBackground,
                isDraft = sample.isDraft
            )
        }

        item {
            // WhatsApp-style separator directly below the final conversation row.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFFE9EDEF))
            )
        }

        item {
            EncryptionFooter(
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(
    name = "Conversation List - WhatsApp Style",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    widthDp = 360,
    heightDp = 640
)
@Composable
private fun ConversationListPreview() {
    TeddyAppTheme(darkMode = false) {
        ConversationList()
    }
}
