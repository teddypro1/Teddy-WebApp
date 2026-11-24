package com.teddy.bpositive.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MessagingHomeScreen(
    contentPadding: androidx.compose.foundation.layout.PaddingValues,
    onFindPeople: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                text = "Messages",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Private conversations, all in one place.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        OutlinedTextField(
            value = "",
            onValueChange = {},
            enabled = false,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            placeholder = { Text("Search conversations") },
            leadingIcon = { Text("⌕", fontSize = 23.sp) },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                disabledContainerColor = MaterialTheme.colorScheme.surface,
                disabledIndicatorColor = MaterialTheme.colorScheme.outlineVariant,
                disabledTextColor = MaterialTheme.colorScheme.onSurface
            )
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 34.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✉", fontSize = 34.sp, color = MaterialTheme.colorScheme.primary)
                }
                Text(
                    text = "No conversations yet",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Find a person by username and start a private conversation. Your chats will appear here.",
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = onFindPeople,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Find people")
                }
            }
        }
    }
}

@Composable
fun MessagingChatScreen(
    displayName: String,
    username: String,
    onBack: () -> Unit,
    onSendMessage: (String) -> Unit
) {
    var messageText = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.textButtonColors()
                    ) { Text("‹") }
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(displayName.take(1).uppercase(), fontWeight = FontWeight.Bold)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(displayName, fontWeight = FontWeight.SemiBold)
                        Text("@$username", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Your conversation will appear here.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = messageText.value,
                onValueChange = { messageText.value = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Write a message") },
                shape = RoundedCornerShape(24.dp),
                maxLines = 4
            )
            Button(
                onClick = {
                    val text = messageText.value.trim()
                    if (text.isNotEmpty()) {
                        onSendMessage(text)
                        messageText.value = ""
                    }
                },
                shape = CircleShape
            ) { Text("➤") }
        }
    }
}
