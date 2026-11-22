package com.teddy.bpositive.ui.screens
import androidx.compose.ui.tooling.preview.Preview
import com.teddy.bpositive.data.TopicRepository
import com.teddy.bpositive.ui.theme.TeddyAppTheme

import com.teddy.bpositive.data.SupabaseNotesApi
import com.teddy.bpositive.ui.components.EmptyNotes
import com.teddy.bpositive.ui.components.TrashNoteCard
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TrashScreen(
    deletedNotes: List<SupabaseNotesApi.CloudNote>,
    contentPadding: PaddingValues,
    onRestore: (SupabaseNotesApi.CloudNote) -> Unit,
    onDeletePermanently: (SupabaseNotesApi.CloudNote) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(contentPadding)
            .verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 20.dp)
    ) {
        Text("Recently Deleted", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text("Restore notes or permanently remove them.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(18.dp))
        if (deletedNotes.isEmpty()) EmptyNotes() else deletedNotes.forEach { cloudNote ->
            TrashNoteCard(
                note = cloudNote,
                onRestore = { onRestore(cloudNote) },
                onDeletePermanently = { onDeletePermanently(cloudNote) }
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
fun SettingsScreen(
    darkMode: Boolean,
    accountEmail: String,
    isAdmin: Boolean,
    onOpenAdminPanel: () -> Unit,
    onDarkModeChange: (Boolean) -> Unit,
    onSignOut: () -> Unit,
    onSendTestNotification: () -> Unit,
    topicPreferences: com.teddy.bpositive.data.TopicRepository.Preferences,
    topicStatus: String,
    installedVersionName: String,
    installedVersionCode: Long,
    latestVersionName: String?,
    latestVersionCode: Long?,
    updateStatus: String,
    isCheckingUpdate: Boolean,
    onCheckForUpdates: () -> Unit,
    onTopicChange: (String, Boolean) -> Unit,
    contentPadding: PaddingValues
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(contentPadding).verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Settings", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("Customize your Teddy WebApp experience.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Appearance", fontWeight = FontWeight.SemiBold)
                    Text(if (darkMode) "Dark mode is enabled" else "Light mode is enabled",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
                Switch(checked = darkMode, onCheckedChange = onDarkModeChange)
            }
        }
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Account", fontWeight = FontWeight.SemiBold)
                Text(accountEmail, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) { Text("Sign Out") }
            }
        }
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Notification topics", fontWeight = FontWeight.SemiBold)
                Text("Choose the announcement categories this account should receive on its registered Android devices.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) { Text("All users"); Text("General announcements", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Switch(checked = topicPreferences.allUsers, onCheckedChange = { onTopicChange("all_users", it) })
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) { Text("Announcements"); Text("Important notices", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Switch(checked = topicPreferences.announcements, onCheckedChange = { onTopicChange("announcements", it) })
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) { Text("Updates"); Text("App and feature updates", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Switch(checked = topicPreferences.updates, onCheckedChange = { onTopicChange("updates", it) })
                }
                if (topicStatus.isNotBlank()) Text(topicStatus, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        }
        if (isAdmin) {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Notifications", fontWeight = FontWeight.SemiBold)
                Text("Check whether push notifications can reach this device.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    OutlinedButton(onClick = onSendTestNotification, modifier = Modifier.fillMaxWidth()) {
                        Text("Send test notification")
                    }
                }
            }
        }
        if (isAdmin) {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Administrator", fontWeight = FontWeight.SemiBold)
                    Text("Verified admin access is enabled for this account.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    Button(onClick = onOpenAdminPanel, modifier = Modifier.fillMaxWidth()) {
                        Text("Open Admin Panel")
                    }
                }
            }
        }
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Update", fontWeight = FontWeight.SemiBold)
                Text(
                    "Installed version: $installedVersionName ($installedVersionCode)",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
                Text(
                    if (latestVersionName != null && latestVersionCode != null)
                        "Latest version: $latestVersionName ($latestVersionCode)"
                    else "Latest version: Not available",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
                Text(
                    updateStatus,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                OutlinedButton(
                    onClick = onCheckForUpdates,
                    enabled = !isCheckingUpdate,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AnimatedContent(
                        targetState = isCheckingUpdate,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(180)) togetherWith
                                fadeOut(animationSpec = tween(180))
                        },
                        label = "update-check-button"
                    ) { checking ->
                        if (checking) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Checking...")
                            }
                        } else {
                            Text("Check for Updates")
                        }
                    }
                }
            }
        }
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text("Teddy WebApp", fontWeight = FontWeight.SemiBold)
                Text("Your thoughts, organized beautifully.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                Text("Version 1.0", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        }
    }
}

@Preview(showBackground = true, name = "Trash Screen")
@Composable
private fun TrashScreenPreview() {
    TeddyAppTheme(darkMode = false) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Recently Deleted", fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text("Restore notes or permanently remove them.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Deleted sample", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                    Text("A deleted note in the Trash preview.")
                    Text("Deleted 21 Nov 2026", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = {}) { Text("Restore") }
                        TextButton(onClick = {}) { Text("Delete permanently") }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Settings Screen")
@Composable
private fun SettingsScreenPreview() {
    TeddyAppTheme(darkMode = false) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Settings", fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text("Customize your Teddy WebApp experience.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text("Appearance", fontWeight = FontWeight.SemiBold)
                        Text("Light mode is enabled", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    }
                    Switch(checked = false, onCheckedChange = {})
                }
            }
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Account", fontWeight = FontWeight.SemiBold)
                    Text("preview@example.com", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(onClick = {}, Modifier.fillMaxWidth()) { Text("Sign Out") }
                }
            }
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Notification topics", fontWeight = FontWeight.SemiBold)
                    Text("Choose the announcement categories this account should receive on its registered Android devices.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    listOf("All users", "Announcements", "Updates").forEach { topic ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(topic)
                            Switch(checked = topic == "All users", onCheckedChange = {})
                        }
                    }
                }
            }
            Text("Update", fontWeight = FontWeight.SemiBold)
            Text("Installed version: 1.0.1 (2)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            Text("Latest version: 1.0.2 (3)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }
    }
}
