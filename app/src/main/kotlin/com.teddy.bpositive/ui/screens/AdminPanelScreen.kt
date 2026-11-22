package com.teddy.bpositive.ui.screens
import androidx.compose.ui.tooling.preview.Preview
import com.teddy.bpositive.ui.theme.TeddyAppTheme

import com.teddy.bpositive.data.AdminRepository
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
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
    directory: List<AdminRepository.UserRecord>,
    isLoadingDirectory: Boolean,
    statusMessage: String,
    onTitleChange: (String) -> Unit,
    onMessageChange: (String) -> Unit,
    onSend: (String, List<String>) -> Unit,
    onSendTopic: (String) -> Unit,
    onRefresh: () -> Unit,
    onRefreshDirectory: () -> Unit
) {
    val showBroadcastConfirmation = remember { mutableStateOf(false) }
    val historyFilter = remember { mutableStateOf("All") }
    val historySearch = remember { mutableStateOf("") }
    val audience = remember { mutableStateOf("all_users") }
    val directorySearch = remember { mutableStateOf("") }
    val selectedEmails = remember { mutableStateListOf<String>() }
    val filteredDirectory = directory.filter { user ->
        directorySearch.value.isBlank() ||
            user.email.contains(directorySearch.value, ignoreCase = true) ||
            user.fullName.contains(directorySearch.value, ignoreCase = true) ||
            user.username.orEmpty().contains(directorySearch.value, ignoreCase = true)
    }
    val selectedRecipientEmails = directory.filter { it.email.lowercase() in selectedEmails }
        .map { it.email }

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
                    progress = { (deliveryRate / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Accepted means FCM accepted the message request; it is not a device-open/read receipt.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
        }

        var selectedTopic = remember { mutableStateOf("all_users") }
        Text("Send to FCM Topic", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Send the title and message above to devices subscribed to a topic.")
                listOf("all_users" to "All users", "announcements" to "Announcements", "updates" to "Updates").forEach { (value, label) ->
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        RadioButton(selected = selectedTopic.value == value, onClick = { selectedTopic.value = value })
                        Text(label)
                    }
                }
                Button(
                    onClick = { onSendTopic(selectedTopic.value) },
                    enabled = !isSending && title.isNotBlank() && message.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Send to topic") }
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text("User Directory", fontWeight = FontWeight.SemiBold)
                        TextButton(onClick = onRefreshDirectory, enabled = !isLoadingDirectory) {
                            Text("Refresh users")
                        }
                    }
                    Text("${selectedRecipientEmails.size} user(s) selected · Only selected accounts will be targeted.",
                        color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                    OutlinedTextField(
                        value = directorySearch.value,
                        onValueChange = { directorySearch.value = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Search by name, email or username") },
                        singleLine = true
                    )
                    if (isLoadingDirectory) {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (filteredDirectory.isEmpty()) {
                        Text("No matching user accounts found.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    } else {
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                filteredDirectory.forEachIndexed { index, user ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = user.email.lowercase() in selectedEmails,
                                            onCheckedChange = { checked ->
                                                val key = user.email.lowercase()
                                                if (checked && selectedEmails.size < 100 && key !in selectedEmails) selectedEmails.add(key)
                                                else if (!checked) selectedEmails.remove(key)
                                            }
                                        )
                                        Column(Modifier.weight(1f)) {
                                            Text(user.fullName, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                            Text(user.email, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                (user.username?.let { "@$it · " } ?: "") +
                                                    if (user.androidDeviceCount > 0) "Android device registered: ${user.androidDeviceCount}"
                                                    else "No Android device registered",
                                                fontSize = 11.sp,
                                                color = if (user.androidDeviceCount > 0) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    if (index < filteredDirectory.lastIndex) HorizontalDivider()
                                }
                            }
                        }
                    }
                    Text("Accounts without a registered Android device can be selected, but cannot receive Push Notifications.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                } else {
                    Text("All accounts with registered Android devices will be targeted.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
                Text("This sends a real notification. Review the audience and message before sending.",
                    color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                Button(
                    onClick = { showBroadcastConfirmation.value = true },
                    enabled = !isLoading && !isSending && title.isNotBlank() && message.isNotBlank() &&
                        (audience.value == "all_users" || selectedRecipientEmails.isNotEmpty()),
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
                        Text("Audience: ${when (item.audience) { "all_users" -> "All users"; "selected_users" -> "Selected users"; "topic_all_users" -> "Topic: All users"; "topic_announcements" -> "Topic: Announcements"; "topic_updates" -> "Topic: Updates"; else -> "Admin devices" }}",
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (item.audience.startsWith("topic_")) {
                            Text("FCM topic message accepted: ${item.sentCount}",
                                fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            Text("FCM does not provide per-device delivery receipts for topic broadcasts.",
                                fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Text("FCM accepted: ${item.sentCount}/${item.totalDevices}  •  Failed: ${item.failedCount}",
                                fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        if (item.deliveryDetails.isNotEmpty()) {
                            HorizontalDivider()
                            Text("Per-device FCM results", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
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
                text = { Text(if (audience.value == "all_users") "Your notification will be sent to every Android device currently registered in Teddy-WebApp. This action cannot be undone." else "Recipients: ${selectedRecipientEmails.joinToString(", ")}. Only registered devices belonging to these accounts will be targeted.") },
                confirmButton = {
                    TextButton(onClick = {
                        showBroadcastConfirmation.value = false
                        onSend(audience.value, selectedRecipientEmails)
                    }) { Text(if (audience.value == "all_users") "Send to all" else "Send to selected") }
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

@Preview(showBackground = true, name = "Admin Panel")
@Composable
private fun AdminPanelScreenPreview() {
    TeddyAppTheme(darkMode = false) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Admin Dashboard", fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text("Restricted administrator tools for Teddy WebApp.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Administrator account", fontWeight = FontWeight.SemiBold)
                    Text("admin@example.com", color = MaterialTheme.colorScheme.primary)
                    Text("Access verified by Supabase Edge Function", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
            Text("Notification analytics", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AnalyticsCard("Notifications", "12", Modifier.weight(1f))
                AnalyticsCard("Devices targeted", "28", Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AnalyticsCard("FCM accepted", "26", Modifier.weight(1f))
                AnalyticsCard("Failed", "2", Modifier.weight(1f))
            }
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Send notification", fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = "A sample announcement", onValueChange = {}, label = { Text("Notification title") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = "Preview content for the admin notification composer.", onValueChange = {}, label = { Text("Notification message") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                    Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Send notification") }
                }
            }
            Text("Notification history", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}
