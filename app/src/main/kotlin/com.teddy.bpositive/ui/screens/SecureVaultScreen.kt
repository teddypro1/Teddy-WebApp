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
import com.teddy.bpositive.data.SupabaseSessionManager
import com.teddy.bpositive.data.SupabaseVaultApi
import com.teddy.bpositive.security.NotesEncryption
import com.teddy.bpositive.security.NotesVaultStorage

@Composable
fun SecureVaultScreen(
    userId: String,
    session: SupabaseSessionManager.Session,
    contentPadding: PaddingValues,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var envelope by remember(userId) { mutableStateOf<NotesEncryption.VaultEnvelope?>(null) }
    var storageMessage by remember(userId) { mutableStateOf("Checking this account's Vault...") }
    var page by remember(userId) { mutableStateOf("loading") }
    var masterPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var recoveryKey by remember { mutableStateOf("") }
    var recoveryConfirmation by remember { mutableStateOf("") }
    var recoveryKeyToConfirm by remember { mutableStateOf("") }
    var recoveryUnlockInput by remember { mutableStateOf("") }
    var useRecoveryKey by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var isSyncing by remember { mutableStateOf(false) }
    var cloudRetryCount by remember(userId) { mutableIntStateOf(0) }

    LaunchedEffect(userId, cloudRetryCount) {
        if (session.userId != userId || userId.isBlank()) {
            page = "cloud_error"
            storageMessage = "The signed-in account does not match this Vault screen."
            return@LaunchedEffect
        }

        when (val local = NotesVaultStorage.load(context, userId)) {
            is NotesVaultStorage.LoadResult.Found -> {
                envelope = local.envelope
                page = "unlock"
                storageMessage = "Vault is saved on this device. Checking account sync automatically..."
                isSyncing = true
                SupabaseVaultApi.syncEnvelope(session, local.envelope) { result ->
                    isSyncing = false
                    storageMessage = result.message
                }
            }
            NotesVaultStorage.LoadResult.Missing -> {
                storageMessage = "Checking whether this account already has a synced Vault..."
                SupabaseVaultApi.fetchEnvelope(session) { remote ->
                    if (!remote.success) {
                        page = "cloud_error"
                        storageMessage = "Could not check the account Vault: ${remote.message}"
                    } else if (remote.data == null) {
                        page = "setup"
                        storageMessage = "No Vault exists on this device or in the account yet."
                    } else {
                        val saved = NotesVaultStorage.save(context, userId, remote.data)
                        if (saved) {
                            envelope = remote.data
                            page = "unlock"
                            storageMessage = "Encrypted Vault envelope restored from your account and saved on this device."
                        } else {
                            page = "cloud_error"
                            storageMessage = "The account Vault was found, but could not be saved securely on this device."
                        }
                    }
                }
            }
            is NotesVaultStorage.LoadResult.Error -> {
                page = "error"
                storageMessage = local.message
            }
        }
    }

    fun syncCurrentVault() {
        val currentEnvelope = envelope
        if (currentEnvelope == null) {
            status = "Vault envelope is not available."
            return
        }
        if (session.userId != userId) {
            status = "Signed-in account changed. Reopen Secure Vault."
            return
        }
        isSyncing = true
        status = "Checking account Vault..."
        SupabaseVaultApi.syncEnvelope(session, currentEnvelope) { result ->
            isSyncing = false
            status = result.message
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
            "The account stores only the encrypted Vault envelope. Existing notes are not encrypted and Note Lock is not enabled.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(storageMessage, color = MaterialTheme.colorScheme.onSurfaceVariant)

        when (page) {
            "loading" -> Text("Loading...")
            "cloud_error" -> Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Account Vault check failed", fontWeight = FontWeight.SemiBold)
                    Text("A new Vault cannot safely be created until the account is checked. This prevents accidentally creating a different Vault while an existing cloud Vault is unavailable.")
                    OutlinedButton(
                        onClick = { cloudRetryCount++ },
                        enabled = !isSyncing,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Retry Account Check") }
                }
            }
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
                            val passwordChars = masterPassword.toCharArray()
                            val setup = try {
                                NotesEncryption.createVault(passwordChars)
                            } finally {
                                passwordChars.fill(0.toChar())
                            }
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
                        val currentEnvelope = envelope
                        if (recoveryConfirmation == recoveryKeyToConfirm && currentEnvelope != null) {
                            val saved = NotesVaultStorage.save(context, userId, currentEnvelope)
                            if (saved) {
                                page = "complete"
                                recoveryKey = ""
                                recoveryKeyToConfirm = ""
                                recoveryConfirmation = ""
                                status = "Vault saved locally. Syncing encrypted envelope to your account..."
                                isSyncing = true
                                SupabaseVaultApi.syncEnvelope(session, currentEnvelope) { result ->
                                    isSyncing = false
                                    status = result.message
                                }
                            } else status = "Could not save Vault data. Try again."
                        } else status = "Recovery Key does not match."
                    }, modifier = Modifier.fillMaxWidth()) { Text("Confirm and Save Vault") }
                }
            }
            "complete" -> {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Vault Setup Complete", fontWeight = FontWeight.SemiBold)
                        Text("The encrypted Vault envelope is saved locally. Account sync stores only the encrypted envelope, not your password or Recovery Key.")
                        if (isSyncing) {
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                            Text("Syncing account Vault...")
                        }
                    }
                }
                Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Done") }
            }
            "unlock" -> Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Vault configured", fontWeight = FontWeight.SemiBold)
                    if (useRecoveryKey) {
                        Text("Enter the Recovery Key you saved during Vault setup.")
                        OutlinedTextField(
                            value = recoveryUnlockInput,
                            onValueChange = { recoveryUnlockInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Recovery Key") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true
                        )
                        Button(onClick = {
                            try {
                                val unlocked = NotesEncryption.unlockWithRecoveryKey(
                                    envelope ?: throw IllegalStateException("Vault envelope is unavailable."),
                                    recoveryUnlockInput
                                )
                                unlocked.fill(0)
                                recoveryUnlockInput = ""
                                status = "Recovery Key verified. Temporary key cleared from memory. Note Lock is not enabled."
                            } catch (_: Exception) {
                                recoveryUnlockInput = ""
                                status = "Recovery Key verification failed. Check the key and try again."
                            }
                        }, modifier = Modifier.fillMaxWidth()) { Text("Unlock with Recovery Key") }
                        TextButton(onClick = {
                            useRecoveryKey = false
                            recoveryUnlockInput = ""
                            status = ""
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text("Use Master Password instead")
                        }
                    } else {
                        Text("Verify the Master Password to check that it can unlock the wrapped key.")
                        OutlinedTextField(
                            value = masterPassword, onValueChange = { masterPassword = it },
                            modifier = Modifier.fillMaxWidth(), label = { Text("Master Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), singleLine = true
                        )
                        Button(onClick = {
                            try {
                                val passwordChars = masterPassword.toCharArray()
                                val unlocked = try {
                                    NotesEncryption.unlockWithPassword(
                                        envelope ?: throw IllegalStateException("Vault envelope is unavailable."),
                                        passwordChars
                                    )
                                } finally {
                                    passwordChars.fill(0.toChar())
                                }
                                unlocked.fill(0)
                                masterPassword = ""
                                status = "Password verified. Temporary key cleared from memory. Note Lock is not enabled."
                            } catch (_: Exception) {
                                masterPassword = ""
                                status = "Password verification failed."
                            }
                        }, modifier = Modifier.fillMaxWidth()) { Text("Verify Password") }
                        TextButton(onClick = {
                            useRecoveryKey = true
                            masterPassword = ""
                            status = ""
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text("Use Recovery Key instead")
                        }
                    }

                    HorizontalDivider()
                    Text("Account Sync", fontWeight = FontWeight.SemiBold)
                    if (isSyncing) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                        Text("Checking and syncing this Vault with your account automatically...")
                    } else {
                        Text("Account sync is automatic. The app checks the account whenever Secure Vault opens and sends the encrypted envelope if it is not already synced. A different account Vault is never overwritten.")
                    }
                }
            }
            "error" -> {
                Text("Local Vault data could not be opened. Do not uninstall the app or clear app data before reviewing recovery options.")
            }
        }
        if (status.isNotBlank()) Text(status, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
    }
}
