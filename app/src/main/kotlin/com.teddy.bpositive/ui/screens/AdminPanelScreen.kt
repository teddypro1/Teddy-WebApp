package com.teddy.bpositive.ui.screens

import com.teddy.bpositive.data.AdminRepository
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AdminPanelScreen(
    accountEmail: String,
    isLoading: Boolean,
    isSending: Boolean,
    title: String,
    message: String,
    history: List<AdminRepository.NotificationRecord>,
    statusMessage: String,
    onTitleChange: (String) -> Unit,
    onMessageChange: (String) -> Unit,
    onSend: () -> Unit,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Admin Dashboard", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("Restricted administrator tools for Teddy WebApp.",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Administrator account", fontWeight = FontWeight.SemiBold)
                Text(accountEmail, color = MaterialTheme.colorScheme.primary)
                Text("Access verified by Supabase Edge Function",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        }

        Text("Send notification", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Notification title") },
                    singleLine = true,
                    supportingText = { Text("${title.length}/100 characters") }
                )
                OutlinedTextField(
                    value = message,
                    onValueChange = onMessageChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Notification message") },
                    minLines = 3,
                    maxLines = 5,
                    supportingText = { Text("${message.length}/500 characters") }
                )
                Text("Audience: only this Admin account's registered Android devices.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Button(
                    onClick = onSend,
                    enabled = !isLoading && !isSending && title.isNotBlank() && message.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isSending) CircularProgressIndicator(
                        modifier = Modifier.size(18.dp), strokeWidth = 2.dp
                    ) else Text("Send notification")
                }
                if (statusMessage.isNotBlank()) {
                    Text(statusMessage, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text("Notification history", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = onRefresh, enabled = !isLoading) { Text("Refresh") }
        }
        if (isLoading) {
            Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (history.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Text("No notification history yet.", modifier = Modifier.padding(18.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            history.forEach { item ->
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(item.title, fontWeight = FontWeight.SemiBold)
                        Text(item.body, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Delivered: ${item.sentCount}/${item.totalDevices}  •  Failed: ${item.failedCount}",
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        Text(item.createdAt.replace("T", " ").removeSuffix("Z"),
                            fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Text("Broadcast to all users is not enabled in this build.",
            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
    }
}
