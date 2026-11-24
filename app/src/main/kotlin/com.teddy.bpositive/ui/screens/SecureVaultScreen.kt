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
import com.teddy.bpositive.data.EncryptedLocalNotesStorage
import com.teddy.bpositive.data.EncryptedNoteSyncQueue
import com.teddy.bpositive.data.SupabaseVaultApi
import com.teddy.bpositive.security.NotesEncryption
import com.teddy.bpositive.security.NotesVaultStorage
import com.teddy.bpositive.security.VaultSessionManager

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
    var isMigratingNotes by remember(userId) { mutableStateOf(false) }
    var migrationMessage by remember(userId) { mutableStateOf("") }
    var isMigratingQueue by remember(userId) { mutableStateOf(false) }
    var queueMigrationMessage by remember(userId) { mutableStateOf("") }
    var isAuditingMigration by remember(userId) { mutableStateOf(false) }
    var auditMessage by remember(userId) { mutableStateOf("") }

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
                                try {
                                    VaultSessionManager.unlock(userId, unlocked)
                                } finally {
                                    unlocked.fill(0)
                                }
                                recoveryUnlockInput = ""
                                status = "Vault unlocked for this app session. Note Lock integration is not enabled yet."
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
                                try {
                                    VaultSessionManager.unlock(userId, unlocked)
                                } finally {
                                    unlocked.fill(0)
                                }
                                masterPassword = ""
                                status = "Vault unlocked for this app session. Note Lock integration is not enabled yet."
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
                    Text("Local Notes Encryption Migration", fontWeight = FontWeight.SemiBold)
                    Text("After unlocking the Vault, create and verify an encrypted local copy of this account's existing notes. The original notes remain untouched.")
                    Button(
                        onClick = {
                            if (!VaultSessionManager.isUnlocked(userId)) {
                                migrationMessage = "Unlock the Vault with your Master Password or Recovery Key first."
                                return@Button
                            }
                            isMigratingNotes = true
                            migrationMessage = "Encrypting and verifying local notes..."
                            try {
                                when (val result = EncryptedLocalNotesStorage.stageLegacyMigration(context, userId)) {
                                    is EncryptedLocalNotesStorage.MigrationResult.Staged ->
                                        migrationMessage = "Verified encrypted copy created for ${result.noteCount} notes. Original notes were preserved."
                                    is EncryptedLocalNotesStorage.MigrationResult.Refreshed ->
                                        migrationMessage = "Encrypted copy refreshed and verified for ${result.noteCount} updated notes. Original notes were preserved."
                                    EncryptedLocalNotesStorage.MigrationResult.AlreadyStaged ->
                                        migrationMessage = "Encrypted local snapshot already matches the current source, or was safely preserved because the plaintext source is empty."
                                    EncryptedLocalNotesStorage.MigrationResult.NoLegacyNotes ->
                                        migrationMessage = "No existing local notes were found to migrate."
                                }
                            } catch (error: Exception) {
                                migrationMessage = "Migration stopped safely: ${error.message ?: "Unknown error"}. Original notes were preserved."
                            } finally {
                                isMigratingNotes = false
                            }
                        },
                        enabled = !isMigratingNotes,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isMigratingNotes) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Text("Prepare Encrypted Local Copy")
                    }
                    if (migrationMessage.isNotBlank()) {
                        Text(migrationMessage, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    HorizontalDivider()
                    Text("Offline Sync Queue Encryption", fontWeight = FontWeight.SemiBold)
                    Text("Prepare an encrypted copy of pending offline sync operations. The existing queue remains untouched until all sync paths can be switched safely.")
                    Button(
                        onClick = {
                            if (!VaultSessionManager.isUnlocked(userId)) {
                                queueMigrationMessage = "Unlock the Vault with your Master Password or Recovery Key first."
                                return@Button
                            }
                            isMigratingQueue = true
                            queueMigrationMessage = "Encrypting and verifying pending sync records..."
                            try {
                                val result = EncryptedNoteSyncQueue.stageLegacyMigration(context, userId)
                                queueMigrationMessage = if (result.alreadyPresent) {
                                    "Encrypted queue already exists and was verified (${result.copiedCount} records)."
                                } else {
                                    "Verified encrypted copy created for ${result.copiedCount} pending records. Original queue was preserved."
                                }
                            } catch (error: Exception) {
                                queueMigrationMessage = "Queue migration stopped safely: ${error.message ?: "Unknown error"}. Original queue was preserved."
                            } finally {
                                isMigratingQueue = false
                            }
                        },
                        enabled = !isMigratingQueue,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isMigratingQueue) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Text("Prepare Encrypted Offline Queue")
                    }
                    if (queueMigrationMessage.isNotBlank()) {
                        Text(queueMigrationMessage, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    HorizontalDivider()
                    Text("Encrypted Migration Verification", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Run a read-only integrity check of both encrypted copies against their current legacy sources. This check does not modify either copy.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = {
                            if (!VaultSessionManager.isUnlocked(userId)) {
                                auditMessage = "Unlock the Vault first."
                                return@Button
                            }
                            isAuditingMigration = true
                            auditMessage = "Checking encrypted snapshots..."
                            val notesResult = try {
                                when (val result = EncryptedLocalNotesStorage.inspectMigration(context, userId)) {
                                    is EncryptedLocalNotesStorage.MigrationAuditResult.MatchesLegacy ->
                                        "Local notes: verified and match (${result.noteCount} notes)."
                                    is EncryptedLocalNotesStorage.MigrationAuditResult.LegacySourceEmpty ->
                                        "Local notes: legacy source is empty; encrypted copy is intact (${result.encryptedNoteCount} notes), but source comparison is unavailable."
                                    is EncryptedLocalNotesStorage.MigrationAuditResult.Mismatch ->
                                        "Local notes: mismatch detected (legacy ${result.legacyNoteCount}, encrypted ${result.encryptedNoteCount})."
                                    EncryptedLocalNotesStorage.MigrationAuditResult.NoEncryptedSnapshot ->
                                        "Local notes: no encrypted snapshot found."
                                }
                            } catch (error: Exception) {
                                "Local notes: verification failed (${error.message ?: "unknown error"})."
                            }
                            val queueResult = try {
                                when (val result = EncryptedNoteSyncQueue.inspectMigration(context, userId)) {
                                    is EncryptedNoteSyncQueue.MigrationAuditResult.MatchesLegacy ->
                                        "Offline queue: verified and match (${result.recordCount} records)."
                                    is EncryptedNoteSyncQueue.MigrationAuditResult.LegacyQueueEmpty ->
                                        "Offline queue: legacy queue is empty; encrypted copy is intact (${result.encryptedRecordCount} records), but source comparison is unavailable."
                                    is EncryptedNoteSyncQueue.MigrationAuditResult.Mismatch ->
                                        "Offline queue: mismatch detected (legacy ${result.legacyRecordCount}, encrypted ${result.encryptedRecordCount})."
                                    EncryptedNoteSyncQueue.MigrationAuditResult.NoEncryptedSnapshot ->
                                        "Offline queue: no encrypted snapshot found."
                                }
                            } catch (error: Exception) {
                                "Offline queue: verification failed (${error.message ?: "unknown error"})."
                            }
                            auditMessage = "$notesResult\n$queueResult"
                            isAuditingMigration = false
                        },
                        enabled = !isAuditingMigration,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isAuditingMigration) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Text("Verify Encrypted Copies")
                    }
                    if (auditMessage.isNotBlank()) {
                        Text(auditMessage, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
