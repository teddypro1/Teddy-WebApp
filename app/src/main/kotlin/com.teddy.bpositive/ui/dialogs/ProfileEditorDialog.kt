package com.teddy.bpositive.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun ProfileEditorDialog(
    fullName: String,
    username: String,
    bio: String,
    avatarUrl: String,
    email: String,
    createdAt: String,
    isSaving: Boolean,
    canSave: Boolean,
    onFullNameChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onBioChange: (String) -> Unit,
    onAvatarUrlChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("My Profile", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier.size(76.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                        .align(Alignment.CenterHorizontally),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        fullName.trim().firstOrNull()?.uppercase() ?: "👤",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    if (avatarUrl.isNotBlank()) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = "Profile picture",
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                Text(email, modifier = Modifier.align(Alignment.CenterHorizontally),
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                OutlinedTextField(value = fullName, onValueChange = onFullNameChange,
                    label = { Text("Full Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = username, onValueChange = onUsernameChange,
                    label = { Text("Username (without @)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = bio, onValueChange = onBioChange,
                    label = { Text("Bio") }, supportingText = { Text("${bio.length}/160") },
                    minLines = 2, maxLines = 4, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = avatarUrl, onValueChange = onAvatarUrlChange,
                    label = { Text("Avatar Image URL (optional)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())
                Text("Account created: $createdAt",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
        },
        confirmButton = {
            TextButton(enabled = !isSaving && fullName.isNotBlank() && canSave, onClick = onSave) {
                Text(if (isSaving) "Saving..." else "Save Profile")
            }
        },
        dismissButton = {
            TextButton(enabled = !isSaving, onClick = onDismiss) { Text("Cancel") }
        }
    )
}
