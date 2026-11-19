package com.teddy.bpositive.ui.screens

import com.teddy.bpositive.data.AdminRepository
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
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
    onSend: (String, List<String>) -> Unit,
    onRefresh: () -> Unit
) {
    val showBroadcastConfirmation = remember { mutableStateOf(false) }
    val historyFilter = remember { mutableStateOf("All") }
    val historySearch = remember { mutableStateOf("") }
    val audience = remember { mutableStateOf("all_users") }
    val recipientEmails = remember { mutableStateOf("") }
    val parsedEmails = recipientEmails.value.split(",", ";", "\n")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinctBy { it.lowercase() }

    val totalDevices = history.sumOf { it.totalDevices }
    val totalAccepted = history.sumOf { it.sentCount }
    val totalFailed = history.sumOf { it.failedCount }
    val deliveryRate = if (totalDevices > 0) totalAccepted * 100 / totalDevices else 0
    val filteredHistory = history.filter { item ->
        val matchesSearch = historySearch.value.isBlank() ||
            item.title.contains(historySearch.value, ignoreCase = true) ||
            item.body.contains(historySearch.value, ignoreCase = true)
        val matchesFilter = when (historyFilter.value) {
            "Successful" -> item.failedCount == 0 && item.sentCount > 0
            "Failed" -> item.failedCount > 0
            else -> true
        }
        matchesSearch && matchesFilter
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val horizontalPadding = if (maxWidth < 360.dp) 14.dp else 18.dp

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 760.dp)
                .align(androidx.compose.ui.Alignment.TopCenter)
                .verticalScroll(rememberScrollState())
                .padding(
                    start = horizontalPadding,
                    top = 20.dp,
                    end = horizontalPadding,
                    bottom = 120.dp
                ),
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

        Text("Notification analytics", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("Based on the latest ${history.size} records loaded from history (maximum 50).",
            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AnalyticsCard("Notifications", history.size.toString(), Modifier.weight(1f))
            AnalyticsCard("Devices targeted", totalDevices.toString(), Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AnalyticsCard("FCM accepted", totalAccepted.toString(), Modifier.weight(1f))
            AnalyticsCard("Failed", totalFailed.toString(), Modifier.weight(1f))
        }
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("FCM acceptance rate", fontWeight = FontWeight.SemiBold)
                    Text("$deliveryRate%", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                LinearProgressIndicator(
                    progress = (deliveryRate / 100f).coerceIn(0f, 1f),
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Accepted means FCM accepted the message request; it is not a device-open/read receipt.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
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
                Text("Choose recipients", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        RadioButton(
                            selected = audience.value == "all_users",
                            onClick = { audience.value = "all_users" }
                        )
                        Text("All users", fontSize = 13.sp)
                    }
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        RadioButton(
                            selected = audience.value == "selected_users",
                            onClick = { audience.value = "selected_users" }
                        )
                        Text("Specific users", fontSize = 13.sp)
                    }
                }
                if (audience.value == "selected_users") {
                    OutlinedTextField(
                        value = recipientEmails.value,
                        onValueChange = { recipientEmails.value = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Recipient email address(es)") },
                        placeholder = { Text("user1@email.com, user2@email.com") },
                        minLines = 2,
                        maxLines = 4,
                        supportingText = { Text("Separate multiple addresses with commas. Up to 100 accounts.") }
                    )
                    Text("Only selected accounts with registered Android devices will receive this notification.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                } else {
                    Text("All accounts with registered Android devices will be targeted.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
                Text("This sends a real notification. Review the audience and message before sending.",
                    color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                Button(
                    onClick = { showBroadcastConfirmation.value = true },
                    enabled = !isLoading && !isSending && title.isNotBlank() && message.isNotBlank() &&
                        (audience.value == "all_users" || parsedEmails.isNotEmpty()),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isSending) CircularProgressIndicator(
                        modifier = Modifier.size(18.dp), strokeWidth = 2.dp
                    ) else Text("Send notification")
                }
                if (statusMessage.isNotBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            statusMessage,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
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
        OutlinedTextField(
            value = historySearch.value,
            onValueChange = { historySearch.value = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Search notification history") },
            singleLine = true
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("All", "Successful", "Failed").forEach { filter ->
                FilterChip(
                    selected = historyFilter.value == filter,
                    onClick = { historyFilter.value = filter },
                    label = { Text(filter) }
                )
            }
        }
        if (isLoading) {
            Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (filteredHistory.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Text(
                    if (history.isEmpty()) "No notification history yet." else "No records match this filter.",
                    modifier = Modifier.padding(18.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            filteredHistory.forEach { item ->
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(item.title, fontWeight = FontWeight.SemiBold)
                        Text(item.body, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Audience: ${when (item.audience) { "all_users" -> "All users"; "selected_users" -> "Selected users"; else -> "Admin devices" }}",
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("FCM accepted: ${item.sentCount}/${item.totalDevices}  •  Failed: ${item.failedCount}",
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        if (item.deliveryDetails.isNotEmpty()) {
                            HorizontalDivider()
                            Text("Device delivery details", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            DeliveryDetails(item.deliveryDetails)
                        }
                        Text(item.createdAt.replace("T", " ").removeSuffix("Z"),
                            fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        Text("Security review", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SecurityLine("✓", "Admin permission checked on the server")
                SecurityLine("✓", "Edge Function requires a valid JWT")
                SecurityLine("✓", "Admin and notification history tables are not granted to anon/authenticated roles")
                SecurityLine("✓", "Service-role and Firebase credentials remain server-side")
                Text("Security controls reviewed against the live Supabase configuration. This is not a full penetration test.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
        }
        Text("Build 6: Admin Panel safe-area and bottom navigation spacing fix.",
            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)

        if (showBroadcastConfirmation.value) {
            AlertDialog(
                onDismissRequest = { showBroadcastConfirmation.value = false },
                title = { Text(if (audience.value == "all_users") "Send notification to all users?" else "Send to selected users?") },
                text = { Text(if (audience.value == "all_users") "Your notification will be sent to every Android device currently registered in Teddy-WebApp. This action cannot be undone." else "Recipients: ${parsedEmails.joinToString(", ")}. Only registered devices belonging to these accounts will be targeted.") },
                confirmButton = {
                    TextButton(onClick = {
                        showBroadcastConfirmation.value = false
                        onSend()
                    }) { Text("Send to all") }
                },
                dismissButton = {
                    TextButton(onClick = { showBroadcastConfirmation.value = false }) { Text("Cancel") }
                }
            )
        }
        }
    }
}

@Composable
private fun AnalyticsCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, fontSize = 23.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary)
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SecurityLine(symbol: String, label: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(symbol, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Text(label, fontSize = 12.sp)
    }
}

@Composable
private fun DeliveryDetails(details: List<AdminRepository.DeliveryDetail>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        details.forEach { detail ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                val statusColor = if (detail.success) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error
                Text(
                    (if (detail.success) "✓ FCM accepted" else "✕ FCM failed") +
                        " · Account …${detail.userId.takeLast(6)} · Token …${detail.tokenSuffix}",
                    color = statusColor,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp
                )
                if (!detail.success) {
                    val code = detail.errorCode ?: "UNKNOWN_ERROR"
                    Text(
                        "$code · HTTP ${detail.httpStatus} · ${AdminRepository.friendlyError(code, detail.staleTokenRemoved)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                    detail.errorMessage?.takeIf { it.isNotBlank() }?.let {
                        Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    }
                    Text(
                        "Attempts: ${detail.attempts}" +
                            if (detail.staleTokenRemoved) " · Invalid token removed" else "",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                } else {
                    Text(
                        "HTTP ${detail.httpStatus}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
