package com.teddy.bpositive.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.teddy.bpositive.security.NotesEncryption
import com.teddy.bpositive.security.NotesVaultStorage

@Composable
fun SecureVaultScreen(
    userId: String,
    contentPadding: PaddingValues,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var envelope by remember(userId) { mutableStateOf<NotesEncryption.VaultEnvelope?>(null) }
    var storageMessage by remember(userId) { mutableStateOf("Checking this account's local Vault...") }
    var page by remember(userId) { mutableStateOf("loading") }
    var masterPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var recoveryKey by remember { mutableStateOf("") }
    var recoveryConfirmation by remember { mutableStateOf("") }
    var recoveryKeyToConfirm by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }

    LaunchedEffect(userId) {
        when (val result = NotesVaultStorage.load(context, userId)) {
            is NotesVaultStorage.LoadResult.Found -> {
                envelope = result.envelope
                page = "unlock"
                storageMessage = "Vault setup is saved on this device."
            }
            NotesVaultStorage.LoadResult.Missing -> {
                page = "setup"
                storageMessage = "No Vault has been configured for this account on this device."
            }
            is NotesVaultStorage.LoadResult.Error -> {
                page = "error"
                storageMessage = result.message
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(contentPadding)
            .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Secure Vault", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            OutlinedButton(onClick = onBack) { Text("Back") }
        }
        Text(
            "This first stage configures the Vault locally. It does not encrypt existing notes or enable Note Lock.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(storageMessage, color = MaterialTheme.colorScheme.onSurfaceVariant)

        when (page) {
            "loading" -> Text("Loading...")
            "setup" -> Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Create Master Password", fontWeight = FontWeight.SemiBold)
                    Text("Use at least 12 characters. The password is not uploaded or saved by the app.")
                    OutlinedTextField(
                        value = masterPassword, onValueChange = { masterPassword = it },
                        modifier = Modifier.fillMaxWidth(), label = { Text("Master Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), singleLine = true
                    )
                    OutlinedTextField(
                        value = confirmPassword, onValueChange = { confirmPassword = it },
                        modifier = Modifier.fillMaxWidth(), label = { Text("Confirm Master Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), singleLine = true
                    )
                    Button(onClick = {
                        status = ""
                        if (masterPassword.length < 12) status = "Password must contain at least 12 characters."
                        else if (masterPassword != confirmPassword) status = "Passwords do not match."
                        else try {
                            val setup = NotesEncryption.createVault(masterPassword.toCharArray())
                            setup.masterKey.fill(0)
                            envelope = setup.envelope
                            recoveryKeyToConfirm = setup.recoveryKey
                            recoveryKey = setup.recoveryKey
                            masterPassword = ""
                            confirmPassword = ""
                            page = "recovery"
                        } catch (error: Exception) {
                            status = error.message ?: "Could not create the Vault."
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text("Create Vault") }
                }
            }
            "recovery" -> Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Save Your Recovery Key", fontWeight = FontWeight.SemiBold)
                    Text("This key is shown only during setup. Store it privately. Anyone with it may be able to unlock the Vault.")
                    Text(recoveryKey, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    OutlinedButton(onClick = {
                        clipboard.setText(AnnotatedString(recoveryKey))
                        status = "Recovery Key copied. Store it safely."
                    }, modifier = Modifier.fillMaxWidth()) { Text("Copy Recovery Key") }
                    OutlinedTextField(
                        value = recoveryConfirmation, onValueChange = { recoveryConfirmation = it },
                        modifier = Modifier.fillMaxWidth(), label = { Text("Enter Recovery Key to confirm") },
                        singleLine = true
                    )
                    Button(onClick = {
                        if (recoveryConfirmation == recoveryKeyToConfirm && envelope != null) {
                            val saved = NotesVaultStorage.save(context, userId, envelope!!)
                            if (saved) {
                                page = "complete"
                                recoveryKey = ""
                                recoveryKeyToConfirm = ""
                                recoveryConfirmation = ""
                                status = "Vault setup saved on this device."
                            } else status = "Could not save Vault data. Try again."
                        } else status = "Recovery Key does not match."
                    }, modifier = Modifier.fillMaxWidth()) { Text("Confirm and Save Vault") }
                }
            }
            "complete" -> {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Vault Setup Complete", fontWeight = FontWeight.SemiBold)
                        Text("The Vault envelope is saved locally and protected with an Android Keystore key.")
                        Text("Existing notes are not encrypted yet. Note Lock remains unavailable until encryption and sync migration are implemented.")
                    }
                }
                Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Done") }
            }
            "unlock" -> Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Vault configured", fontWeight = FontWeight.SemiBold)
                    Text("Verify the Master Password to check that it can unlock the wrapped key.")
                    OutlinedTextField(
                        value = masterPassword, onValueChange = { masterPassword = it },
                        modifier = Modifier.fillMaxWidth(), label = { Text("Master Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), singleLine = true
                    )
                    Button(onClick = {
                        try {
                            val unlocked = NotesEncryption.unlockWithPassword(
                                envelope ?: throw IllegalStateException("Vault envelope is unavailable."),
                                masterPassword.toCharArray()
                            )
                            unlocked.fill(0)
                            masterPassword = ""
                            status = "Password verified. Temporary key cleared from memory. Note Lock is not enabled."
                        } catch (_: Exception) {
                            masterPassword = ""
                            status = "Password verification failed."
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text("Verify Password") }
                }
            }
            "error" -> {
                Text("Local Vault data could not be opened. Do not uninstall the app or clear app data before reviewing recovery options.")
                OutlinedButton(onClick = {
                    page = "setup"
                    status = "Creating a new Vault will not recover the previous Vault."
                }) { Text("Back to Setup") }
            }
        }
        if (status.isNotBlank()) Text(status, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
    }
}
