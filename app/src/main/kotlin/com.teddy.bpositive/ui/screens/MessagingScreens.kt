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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.teddy.bpositive.data.UserSearchApi
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
fun MessagingUserSearchScreen(
    contentPadding: androidx.compose.foundation.layout.PaddingValues,
    profiles: List<UserSearchApi.PublicProfile>,
    isSearching: Boolean,
    status: String,
    onBack: () -> Unit,
    onSearch: (String) -> Unit,
    onStartChat: (UserSearchApi.PublicProfile) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selectedProfile by remember { mutableStateOf<UserSearchApi.PublicProfile?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(contentPadding)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("‹ Back") }
            Text("Find people", fontSize = 23.sp, fontWeight = FontWeight.Bold)
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it.filter { ch -> ch.isLetterOrDigit() || ch == '_' || ch == '.' || ch == '@' }.take(31) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            label = { Text("Username") },
            placeholder = { Text("Search by username") },
            leadingIcon = { Text("@", fontSize = 20.sp) }
        )
        Button(
            onClick = { onSearch(query) },
            enabled = !isSearching && query.trim().removePrefix("@").length >= 2,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) { Text(if (isSearching) "Searching..." else "Search users") }

        if (isSearching) {
            Box(Modifier.fillMaxWidth().padding(18.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        if (status.isNotBlank()) {
            Text(status, color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(profiles, key = { it.id }) { profile ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    onClick = { selectedProfile = profile }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(46.dp).background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(profile.fullName.take(1).uppercase(), fontWeight = FontWeight.Bold)
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(profile.fullName, fontWeight = FontWeight.SemiBold)
                            Text("@${profile.username}", color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodySmall)
                            if (profile.bio.isNotBlank()) {
                                Text(profile.bio, maxLines = 2,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }

    selectedProfile?.let { profile ->
        AlertDialog(
            onDismissRequest = { selectedProfile = null },
            title = { Text(profile.fullName) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("@${profile.username}", color = MaterialTheme.colorScheme.primary)
                    Text(profile.bio.ifBlank { "No bio added." })
                }
            },
            confirmButton = {
                Button(onClick = {
                    selectedProfile = null
                    onStartChat(profile)
                }) { Text("Start chat") }
            },
            dismissButton = {
                TextButton(onClick = { selectedProfile = null }) { Text("Close") }
            }
        )
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
