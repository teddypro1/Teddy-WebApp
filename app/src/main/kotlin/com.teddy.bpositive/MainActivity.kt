package com.teddy.bpositive

import android.os.Bundle
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.teddy.bpositive.data.UpdateRepository
import com.teddy.bpositive.model.UpdateMetadata
import com.teddy.bpositive.data.FcmTokenManager
import com.teddy.bpositive.data.FcmTokenRepository
import com.teddy.bpositive.data.AdminRepository
import com.teddy.bpositive.data.InboxRepository
import com.teddy.bpositive.data.TopicRepository
import com.teddy.bpositive.data.SupabaseAuthRepository
import com.teddy.bpositive.data.SupabaseSessionManager
import com.teddy.bpositive.data.NoteSyncStore
import com.teddy.bpositive.data.NoteSyncRepository
import com.teddy.bpositive.data.SupabaseNotesApi
import com.teddy.bpositive.data.SupabaseRealtimeSubscription
import com.teddy.bpositive.data.SupabaseProfilesApi
import com.teddy.bpositive.data.UserSearchApi
import com.teddy.bpositive.data.DirectConversationApi
import com.teddy.bpositive.model.Note
import com.teddy.bpositive.security.NoteEncryptionCodec
import com.teddy.bpositive.security.VaultSessionManager
import com.teddy.bpositive.data.LocalNotesStorage.loadNotes
import com.teddy.bpositive.data.LocalNotesStorage.saveNotes
import com.teddy.bpositive.utils.parseCloudTimestampMillis
import com.teddy.bpositive.utils.hasValidatedInternet
import com.teddy.bpositive.utils.filterAndSortNotes
import com.teddy.bpositive.utils.parseCloudTimeMillis
import com.teddy.bpositive.utils.toggleNotePinned
import com.teddy.bpositive.ui.components.ProfileInfoRow
import com.teddy.bpositive.ui.components.TrashNoteCard
import com.teddy.bpositive.ui.components.ConflictCopyCard
import com.teddy.bpositive.ui.components.DashboardHeader
import com.teddy.bpositive.ui.components.StatsCard
import com.teddy.bpositive.ui.components.NoteEditor
import com.teddy.bpositive.ui.components.EmptyNotes
import com.teddy.bpositive.ui.components.NoteCard
import com.teddy.bpositive.ui.screens.ProfilePage
import com.teddy.bpositive.ui.screens.AuthScreen
import com.teddy.bpositive.ui.screens.TrashScreen
import com.teddy.bpositive.ui.screens.SettingsScreen
import com.teddy.bpositive.ui.screens.SecureVaultScreen
import com.teddy.bpositive.ui.screens.AdminPanelScreen
import com.teddy.bpositive.ui.screens.HomeScreen
import com.teddy.bpositive.ui.screens.MessagingHomeScreen
import com.teddy.bpositive.ui.screens.MessagingUserSearchScreen
import com.teddy.bpositive.ui.screens.MessagingChatScreen
import com.teddy.bpositive.ui.screens.NotesScreen
import com.teddy.bpositive.ui.screens.NoteDetailScreen
import com.teddy.bpositive.ui.screens.NotificationInboxScreen
import com.teddy.bpositive.ui.dialogs.ProfileEditorDialog
import com.teddy.bpositive.ui.dialogs.DeleteConfirmationDialog
import com.teddy.bpositive.ui.dialogs.PermanentDeleteConfirmationDialog
import com.teddy.bpositive.ui.components.AppBottomNavigationBar
import com.teddy.bpositive.ui.components.WorkspaceTopBar
import com.teddy.bpositive.ui.theme.TeddyAppTheme
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class MainActivity : ComponentActivity() {
    private var suppressInitialSyncSuccessToast = false

    override fun onStop() {
        // Never keep an unlocked Vault master key in memory while the app is backgrounded.
        com.teddy.bpositive.security.VaultSessionManager.lock()
        super.onStop()
    }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                android.util.Log.i("TeddyFCM", "Notification permission was not granted")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FcmTokenManager.initialize(this)
        if (
            android.os.Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        val previousOrientation = savedInstanceState?.getInt(KEY_LAST_ORIENTATION)
            ?: android.content.res.Configuration.ORIENTATION_UNDEFINED
        suppressInitialSyncSuccessToast =
            previousOrientation != android.content.res.Configuration.ORIENTATION_UNDEFINED &&
            previousOrientation != resources.configuration.orientation

        setContent {
            TeddyNotesApp(
                suppressInitialSyncSuccessToast = suppressInitialSyncSuccessToast
            )
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(KEY_LAST_ORIENTATION, resources.configuration.orientation)
        super.onSaveInstanceState(outState)
    }

    companion object {
        private const val KEY_LAST_ORIENTATION = "teddy_last_saved_orientation"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeddyNotesApp(suppressInitialSyncSuccessToast: Boolean = false) {

    val context = androidx.compose.ui.platform.LocalContext.current

    var isCheckingSession by remember { mutableStateOf(true) }
    var activeSession by remember { mutableStateOf<SupabaseSessionManager.Session?>(null) }
    var isAdmin by remember { mutableStateOf(false) }
    var adminVerifiedUserId by remember { mutableStateOf<String?>(null) }
    var isCheckingAdmin by remember { mutableStateOf(false) }
    var isSendingAdminNotification by remember { mutableStateOf(false) }
    var isLoadingAdminHistory by remember { mutableStateOf(false) }
    var isLoadingAdminDirectory by remember { mutableStateOf(false) }
    var adminTitle by rememberSaveable { mutableStateOf("") }
    var adminMessage by rememberSaveable { mutableStateOf("") }
    var adminStatusMessage by remember { mutableStateOf("") }
    var adminHistory by remember { mutableStateOf<List<AdminRepository.NotificationRecord>>(emptyList()) }
    var adminDirectory by remember { mutableStateOf<List<AdminRepository.UserRecord>>(emptyList()) }
    var inboxItems by remember { mutableStateOf<List<InboxRepository.Item>>(emptyList()) }
    var isLoadingInbox by remember { mutableStateOf(false) }
    var inboxStatus by remember { mutableStateOf("") }
    var topicPreferences by remember { mutableStateOf(TopicRepository.Preferences(false, false, false)) }
    var topicStatus by remember { mutableStateOf("") }
    var updateMetadata by remember { mutableStateOf<UpdateMetadata?>(null) }
    var updateStatus by remember { mutableStateOf("Not checked yet") }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var installedVersionName by remember { mutableStateOf("Unknown") }
    var installedVersionCode by remember { mutableStateOf(0L) }
    var showUpdateDialog by rememberSaveable { mutableStateOf(false) }
    var isMandatoryUpdate by rememberSaveable { mutableStateOf(false) }
    var isDownloadingUpdate by remember { mutableStateOf(false) }
    var updateDownloadProgress by remember { mutableStateOf(0f) }
    var updateDownloadBytes by remember { mutableStateOf(0L) }
    var updateTotalBytes by remember { mutableStateOf(0L) }
    var updateDownloadMessage by remember { mutableStateOf("") }
    var verifiedUpdateApk by remember { mutableStateOf<File?>(null) }

    fun checkForAppUpdates(manualCheck: Boolean = false) {
        val checkStartedAt = android.os.SystemClock.elapsedRealtime()
        isCheckingUpdate = true
        updateStatus = "Checking for updates..."

        UpdateRepository.check(context) { result ->
            val minimumLoadingTime = if (manualCheck) 1_200L else 0L
            val elapsed = android.os.SystemClock.elapsedRealtime() - checkStartedAt
            val remainingDelay = (minimumLoadingTime - elapsed).coerceAtLeast(0L)

            Handler(Looper.getMainLooper()).postDelayed({
                isCheckingUpdate = false
                installedVersionName = result.installedVersionName
                installedVersionCode = result.installedVersionCode
                updateMetadata = result.metadata
                updateStatus = result.message

                val metadata = result.metadata
                val mandatory = metadata != null && metadata.enabled &&
                    result.installedVersionCode < metadata.minimumSupportedVersionCode
                val available = metadata != null && metadata.enabled &&
                    (result.installedVersionCode < metadata.latestVersionCode || mandatory)
                isMandatoryUpdate = mandatory

                if (available) {
                    val preferences = context.getSharedPreferences("teddy_settings", Context.MODE_PRIVATE)
                    val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                    val laterVersion = preferences.getLong("update_later_version_code", -1L)
                    val laterDate = preferences.getString("update_later_date", "")
                    showUpdateDialog = mandatory || manualCheck ||
                        laterVersion != metadata.latestVersionCode || laterDate != today
                } else {
                    showUpdateDialog = false
                }
            }, remainingDelay)
        }
    }

    fun dismissOptionalUpdateForToday() {
        val versionCode = updateMetadata?.latestVersionCode ?: return
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        context.getSharedPreferences("teddy_settings", Context.MODE_PRIVATE).edit()
            .putLong("update_later_version_code", versionCode)
            .putString("update_later_date", today)
            .apply()
        showUpdateDialog = false
    }

    fun installVerifiedUpdate(file: File) {
        if (!file.exists()) {
            verifiedUpdateApk = null
            updateDownloadMessage = "Verified APK is no longer available. Download it again."
            return
        }
        if (android.os.Build.VERSION.SDK_INT >= 26 &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            val settingsIntent = android.content.Intent(
                android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                android.net.Uri.parse("package:${context.packageName}")
            ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(settingsIntent)
            updateDownloadMessage = "Allow app installs from this source, then tap Install update again."
            return
        }
        try {
            val apkUri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val installIntent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
            if (!isMandatoryUpdate) dismissOptionalUpdateForToday()
        } catch (error: Exception) {
            updateDownloadMessage = error.message ?: "Could not start Android Package Installer."
        }
    }

    fun openUpdateDownload() {
        val metadata = updateMetadata ?: return
        val cachedApk = verifiedUpdateApk
        if (cachedApk != null && cachedApk.exists()) {
            installVerifiedUpdate(cachedApk)
            return
        }
        if (isDownloadingUpdate) return
        isDownloadingUpdate = true
        updateDownloadProgress = 0f
        updateDownloadBytes = 0L
        updateTotalBytes = 0L
        updateDownloadMessage = "Preparing secure download..."
        UpdateRepository.downloadAndVerify(
            context = context,
            metadata = metadata,
            onProgress = { progress ->
                updateDownloadBytes = progress.downloadedBytes
                updateTotalBytes = progress.totalBytes
                updateDownloadProgress = progress.fraction
                updateDownloadMessage = "Downloading update..."
            },
            onComplete = { result ->
                isDownloadingUpdate = false
                result.onSuccess { file ->
                    verifiedUpdateApk = file
                    updateDownloadProgress = 1f
                    updateDownloadMessage = "Download complete. SHA-256 verified."
                    installVerifiedUpdate(file)
                }.onFailure { error ->
                    verifiedUpdateApk = null
                    updateDownloadMessage = error.message ?: "Update download failed."
                    Toast.makeText(context, updateDownloadMessage, Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    val isAuthenticated = activeSession != null

    // Wipe the previous account's in-memory Vault key whenever the authenticated
    // account changes or signs out. The key is never carried across account sessions.
    DisposableEffect(activeSession?.userId) {
        onDispose {
            com.teddy.bpositive.security.VaultSessionManager.lock()
        }
    }

    LaunchedEffect(Unit) {
        checkForAppUpdates()
    }

    LaunchedEffect(Unit) {
        SupabaseAuthRepository.restoreSession(context) { result ->
            activeSession = if (result.success) result.session else null
            isCheckingSession = false
        }
    }

    if (isCheckingSession) {
        MaterialTheme {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        return
    }

    if (!isAuthenticated) {
        AuthScreen(
            onAuthenticated = { session ->
                isAdmin = false
                adminVerifiedUserId = null
                isCheckingAdmin = true
                isSendingAdminNotification = false
                isLoadingAdminHistory = false
                isLoadingAdminDirectory = false
                adminHistory = emptyList()
                adminDirectory = emptyList()
                adminStatusMessage = ""
                activeSession = session
            }
        )
        if (showUpdateDialog && updateMetadata != null) {
            UpdatePromptDialog(
                metadata = updateMetadata!!,
                mandatory = isMandatoryUpdate,
                isDownloading = isDownloadingUpdate,
                progress = updateDownloadProgress,
                downloadedBytes = updateDownloadBytes,
                totalBytes = updateTotalBytes,
                downloadMessage = updateDownloadMessage,
                isVerified = verifiedUpdateApk?.exists() == true,
                onLater = { dismissOptionalUpdateForToday() },
                onDownload = { openUpdateDownload() }
            )
        }
        return
    }

    val userId = activeSession?.userId.orEmpty()
    val hasVerifiedAdminAccess =
        isAdmin && !isCheckingAdmin && adminVerifiedUserId == userId
    val notes = remember(userId) {
        mutableStateListOf<Note>().apply { addAll(loadNotes(context, userId)) }
    }

    val deletedNotes = remember(userId) { mutableStateListOf<SupabaseNotesApi.CloudNote>() }
    val conflictCopies = remember(userId) { mutableStateListOf<SupabaseNotesApi.ConflictCopy>() }
    var currentProfile by remember(userId) { mutableStateOf<SupabaseProfilesApi.Profile?>(null) }
    var userSearchResults by remember(userId) { mutableStateOf<List<UserSearchApi.PublicProfile>>(emptyList()) }
    var isSearchingUsers by remember { mutableStateOf(false) }
    var userSearchStatus by remember { mutableStateOf("") }
    var showProfileDialog by rememberSaveable { mutableStateOf(false) }
    var profileFullName by remember(userId) { mutableStateOf("") }
    var profileUsername by remember(userId) { mutableStateOf("") }
    var profileBio by remember(userId) { mutableStateOf("") }
    var profileAvatarUrl by remember(userId) { mutableStateOf("") }
    var isSavingProfile by remember { mutableStateOf(false) }

    LaunchedEffect(activeSession) {
        val session = activeSession ?: return@LaunchedEffect
        topicStatus = "Loading notification preferences..."
        TopicRepository.load(session) { result ->
            if (activeSession?.userId != session.userId) return@load
            if (result.success && result.preferences != null) {
                topicPreferences = result.preferences
                topicStatus = ""
            } else topicStatus = result.message
        }
    }

    LaunchedEffect(activeSession) {
        val session = activeSession
        inboxItems = emptyList()
        inboxStatus = ""
        if (session == null) {
            isLoadingInbox = false
            return@LaunchedEffect
        }
        isLoadingInbox = true
        InboxRepository.load(session) { result ->
            if (activeSession?.userId != session.userId) return@load
            isLoadingInbox = false
            if (result.success) { inboxItems = result.items; inboxStatus = "" }
            else inboxStatus = result.message
        }
    }

    LaunchedEffect(activeSession) {
        val session = activeSession
        isAdmin = false
        adminVerifiedUserId = null
        isSendingAdminNotification = false
        isLoadingAdminHistory = false
        isLoadingAdminDirectory = false
        adminHistory = emptyList()
        adminDirectory = emptyList()
        adminStatusMessage = ""
        if (session == null) {
            isCheckingAdmin = false
            return@LaunchedEffect
        }
        isCheckingAdmin = true
        AdminRepository.checkAdmin(session) { result ->
            if (activeSession?.userId != session.userId) return@checkAdmin
            val verifiedAdmin = result.success && result.data == true
            isCheckingAdmin = false
            isAdmin = verifiedAdmin
            adminVerifiedUserId = if (verifiedAdmin) session.userId else null
            if (verifiedAdmin) {
                isLoadingAdminHistory = true
                isLoadingAdminDirectory = true
                AdminRepository.loadHistory(session) { historyResult ->
                    if (activeSession?.userId != session.userId) return@loadHistory
                    isLoadingAdminHistory = false
                    if (historyResult.success) adminHistory = historyResult.data.orEmpty()
                    else adminStatusMessage = historyResult.message
                }
                AdminRepository.loadUserDirectory(session) { directoryResult ->
                    if (activeSession?.userId != session.userId) return@loadUserDirectory
                    isLoadingAdminDirectory = false
                    if (directoryResult.success) adminDirectory = directoryResult.data.orEmpty()
                    else adminStatusMessage = directoryResult.message
                }
            } else {
                adminHistory = emptyList()
                if (!result.success) adminStatusMessage = result.message
            }
        }
    }

    LaunchedEffect(activeSession) {
        val session = activeSession ?: return@LaunchedEffect
        android.util.Log.i("TeddyFCM", "Authenticated session detected; registering this device")
        FcmTokenRepository.registerCurrentDevice(context, session) { success, message ->
            if (success) {
                android.util.Log.i("TeddyFCM", message)
            } else {
                android.util.Log.e("TeddyFCM", message)
            }
        }
    }

    LaunchedEffect(activeSession) {
        val session = activeSession ?: return@LaunchedEffect
        SupabaseProfilesApi.fetchOrCreateProfile(context, session) { result ->
            if (activeSession?.userId != session.userId) return@fetchOrCreateProfile
            if (result.success && result.data != null && result.data.id == session.userId) {
                currentProfile = result.data
                profileFullName = result.data.fullName
                profileUsername = result.data.username.orEmpty()
                profileBio = result.data.bio
                profileAvatarUrl = result.data.avatarUrl.orEmpty()
            } else {
                Toast.makeText(context, "Could not load profile: " + result.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    LaunchedEffect(activeSession) {
        val session = activeSession ?: return@LaunchedEffect
        val userId = session.userId.orEmpty()
        if (userId.isBlank()) return@LaunchedEffect

        // Only this account's namespaced local notes and queue may be synchronized.
        // Unscoped legacy notes are deliberately never imported into a newly signed-in account.
        NoteSyncRepository.syncPendingChanges(context, session) { result ->
            if (activeSession?.userId != session.userId) return@syncPendingChanges
            if (!result.success) {
                Toast.makeText(
                    context,
                    "Cloud sync pending: ${result.message}",
                    Toast.LENGTH_LONG
                ).show()
                return@syncPendingChanges
            }

            SupabaseNotesApi.fetchNotes(session) { fetchResult ->
                if (!fetchResult.success || fetchResult.data == null) {
                    Toast.makeText(
                        context,
                        "Upload completed, but cloud notes could not be downloaded: ${fetchResult.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    return@fetchNotes
                }

                deletedNotes.clear()
                fetchResult.data.filter { it.userId == session.userId && it.deletedAt != null }.forEach { deletedNotes.add(it) }

                SupabaseNotesApi.fetchConflictCopies(session) { conflictResult ->
                    if (conflictResult.success && conflictResult.data != null) {
                        conflictCopies.clear()
                        conflictCopies.addAll(conflictResult.data)
                    }
                }

                fetchResult.data.filter { it.userId == session.userId }.forEach { cloudNote ->
                    val existingRecord = cloudNote.clientId?.let { clientId ->
                        NoteSyncStore.findByClientId(context, userId, clientId)
                    }

                    // Never let an older cloud snapshot overwrite a local change
                    // that is still waiting to be synchronized.
                    if (existingRecord?.operation != null) return@forEach

                    if (cloudNote.deletedAt != null) {
                        existingRecord?.let { record ->
                            notes.removeAll { it.id == record.localNoteId }
                        }
                    } else {
                        val clientId = cloudNote.clientId
                        if (clientId.isNullOrBlank()) return@forEach

                        val localId = existingRecord?.localNoteId ?: run {
                            var candidate = System.currentTimeMillis()
                            while (notes.any { it.id == candidate }) candidate++
                            candidate
                        }

                        NoteSyncStore.registerCloudNote(
                            context = context,
                            userId = userId,
                            localNoteId = localId,
                            clientId = clientId,
                            cloudNoteId = cloudNote.id,
                            title = cloudNote.title,
                            content = cloudNote.content,
                            isPinned = cloudNote.isPinned,
                            cloudUpdatedAt = cloudNote.updatedAt
                        )

                        val previousLocalNote = notes.firstOrNull { it.id == localId }
                        val cloudPayload = NoteEncryptionCodec.extractCloudPayload(cloudNote.content)
                        val localNote = if (cloudPayload != null) Note(
                            id = localId, title = "Locked note",
                            content = "Unlock Secure Vault to view this note.", isPinned = false,
                            createdAt = 0L, updatedAt = 0L, isLocked = true,
                            encryptedPayload = cloudPayload, isMasked = true
                        ) else Note(
                            id = localId, title = cloudNote.title, content = cloudNote.content,
                            isPinned = cloudNote.isPinned,
                            createdAt = parseCloudTimestampMillis(cloudNote.createdAt)
                                ?: previousLocalNote?.createdAt ?: System.currentTimeMillis(),
                            updatedAt = parseCloudTimestampMillis(cloudNote.updatedAt)
                                ?: previousLocalNote?.updatedAt ?: System.currentTimeMillis()
                        )

                        val localIndex = notes.indexOfFirst { it.id == localId }
                        if (localIndex >= 0) notes[localIndex] = localNote
                        else notes.add(localNote)
                    }
                }

                saveNotes(context, userId, notes)
                if (
                    (result.processed > 0 || fetchResult.data.isNotEmpty()) &&
                    !suppressInitialSyncSuccessToast
                ) {
                    Toast.makeText(
                        context,
                        "Cloud notes synchronized successfully.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    DisposableEffect(activeSession) {
        val session = activeSession
        if (session == null) {
            onDispose { }
        } else {
            val subscription = SupabaseRealtimeSubscription(session) {
                // A Realtime event is only a signal to refresh. Merge rules below
                // protect any local note that still has a queued operation.
                SupabaseNotesApi.fetchNotes(session) { fetchResult ->
                    if (activeSession?.userId != session.userId) return@fetchNotes
                    if (!fetchResult.success || fetchResult.data == null) return@fetchNotes

                    val userId = session.userId.orEmpty()
                    deletedNotes.clear()
                    fetchResult.data.filter { it.userId == session.userId && it.deletedAt != null }.forEach { deletedNotes.add(it) }

                    fetchResult.data.filter { it.userId == session.userId }.forEach { cloudNote ->
                        val existingRecord = cloudNote.clientId?.let { clientId ->
                            NoteSyncStore.findByClientId(context, userId, clientId)
                        }
                        if (existingRecord?.operation != null) return@forEach

                        if (cloudNote.deletedAt != null) {
                            existingRecord?.let { record ->
                                notes.removeAll { it.id == record.localNoteId }
                            }
                        } else {
                            val clientId = cloudNote.clientId
                            if (clientId.isNullOrBlank()) return@forEach

                            val localId = existingRecord?.localNoteId ?: run {
                                var candidate = System.currentTimeMillis()
                                while (notes.any { it.id == candidate }) candidate++
                                candidate
                            }

                            NoteSyncStore.registerCloudNote(
                                context = context,
                                userId = userId,
                                localNoteId = localId,
                                clientId = clientId,
                                cloudNoteId = cloudNote.id,
                                title = cloudNote.title,
                                content = cloudNote.content,
                                isPinned = cloudNote.isPinned,
                                cloudUpdatedAt = cloudNote.updatedAt
                            )

                            val previousLocalNote = notes.firstOrNull { it.id == localId }
                            val cloudPayload = NoteEncryptionCodec.extractCloudPayload(cloudNote.content)
                            val refreshedNote = if (cloudPayload != null) Note(
                                id = localId, title = "Locked note",
                                content = "Unlock Secure Vault to view this note.", isPinned = false,
                                createdAt = 0L, updatedAt = 0L, isLocked = true,
                                encryptedPayload = cloudPayload, isMasked = true
                            ) else Note(
                                id = localId, title = cloudNote.title, content = cloudNote.content,
                                isPinned = cloudNote.isPinned,
                                createdAt = parseCloudTimestampMillis(cloudNote.createdAt)
                                    ?: previousLocalNote?.createdAt ?: System.currentTimeMillis(),
                                updatedAt = parseCloudTimestampMillis(cloudNote.updatedAt)
                                    ?: previousLocalNote?.updatedAt ?: System.currentTimeMillis()
                            )
                            val localIndex = notes.indexOfFirst { it.id == localId }
                            if (localIndex >= 0) notes[localIndex] = refreshedNote
                            else notes.add(refreshedNote)
                        }
                    }

                    saveNotes(context, userId, notes)
                    SupabaseNotesApi.fetchConflictCopies(session) { conflictResult ->
                        if (conflictResult.success && conflictResult.data != null) {
                            conflictCopies.clear()
                            conflictCopies.addAll(conflictResult.data)
                        }
                    }

                    // Profile changes are delivered through the same user-scoped
                    // Realtime channel and reloaded with the current authenticated session.
                    SupabaseProfilesApi.fetchOrCreateProfile(context, session) { profileResult ->
                        val profile = profileResult.data ?: return@fetchOrCreateProfile
                        if (activeSession?.userId == session.userId &&
                            profileResult.success && profile.id == session.userId) {
                            currentProfile = profile
                            if (!showProfileDialog) {
                                profileFullName = profile.fullName
                                profileUsername = profile.username.orEmpty()
                                profileBio = profile.bio
                                profileAvatarUrl = profile.avatarUrl.orEmpty()
                            }
                        }
                    }
                }
            }
            subscription.start()
            onDispose { subscription.stop() }
        }
    }

    // Resume queued offline note changes as soon as Android reports validated internet.
    DisposableEffect(activeSession) {
        val session = activeSession
        val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val mainHandler = Handler(Looper.getMainLooper())
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                val hasWorkingInternet =
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                if (!hasWorkingInternet || session == null) return

                mainHandler.post {
                    if (activeSession?.userId != session.userId) return@post
                    NoteSyncRepository.syncPendingChanges(context, session) { syncResult ->
                        if (activeSession?.userId != session.userId || !syncResult.success) return@syncPendingChanges
                        // Realtime subscription also refreshes on reconnect; this fetch
                        // guarantees the foreground UI reconciles immediately after queue drain.
                        SupabaseNotesApi.fetchNotes(session) { cloudResult ->
                            if (activeSession?.userId != session.userId ||
                                !cloudResult.success || cloudResult.data == null) return@fetchNotes
                            cloudResult.data.filter { it.userId == session.userId }.forEach { cloudNote ->
                                val record = cloudNote.clientId?.let {
                                    NoteSyncStore.findByClientId(context, session.userId.orEmpty(), it)
                                }
                                if (record?.operation != null) return@forEach
                                if (cloudNote.deletedAt != null) {
                                    record?.let { notes.removeAll { note -> note.id == it.localNoteId } }
                                } else {
                                    val clientId = cloudNote.clientId ?: return@forEach
                                    val localId = record?.localNoteId ?: System.currentTimeMillis()
                                    val previousLocalNote = notes.firstOrNull { it.id == localId }
                                    val refreshed = Note(
                                        id = localId,
                                        title = cloudNote.title,
                                        content = cloudNote.content,
                                        isPinned = cloudNote.isPinned,
                                        createdAt = parseCloudTimestampMillis(cloudNote.createdAt)
                                            ?: previousLocalNote?.createdAt
                                            ?: System.currentTimeMillis(),
                                        updatedAt = parseCloudTimestampMillis(cloudNote.updatedAt)
                                            ?: previousLocalNote?.updatedAt
                                            ?: System.currentTimeMillis(),
                                        colorHex = previousLocalNote?.colorHex ?: "#FFFFFF"
                                    )
                                    val index = notes.indexOfFirst { it.id == localId }
                                    if (index >= 0) notes[index] = refreshed else notes.add(refreshed)
                                    NoteSyncStore.registerCloudNote(
                                        context, session.userId.orEmpty(), localId, clientId,
                                        cloudNote.id, cloudNote.title, cloudNote.content,
                                        cloudNote.isPinned, cloudNote.updatedAt
                                    )
                                }
                            }
                            saveNotes(context, session.userId.orEmpty(), notes)
                        }
                    }
                }
            }
        }

        try {
            connectivity.registerDefaultNetworkCallback(callback)
        } catch (_: Exception) {
            // Realtime reconnect remains a secondary recovery path on older devices.
        }
        onDispose {
            try { connectivity.unregisterNetworkCallback(callback) } catch (_: Exception) { }
        }
    }

    var darkMode by remember {
        mutableStateOf(
            context.getSharedPreferences(
                "teddy_settings",
                Context.MODE_PRIVATE
            ).getBoolean("dark_mode", false)
        )
    }

    var searchQuery by rememberSaveable {
        mutableStateOf("")
    }

    var showPinnedOnly by rememberSaveable {
        mutableStateOf(false)
    }

    var showTrash by rememberSaveable { mutableStateOf(false) }
    var showConflicts by rememberSaveable { mutableStateOf(false) }
    var selectedTab by rememberSaveable { mutableStateOf("Home") }
    var showUserSearch by rememberSaveable { mutableStateOf(false) }
    var showChatScreen by rememberSaveable { mutableStateOf(false) }
    var isCreatingConversation by remember { mutableStateOf(false) }
    var activeConversationId by rememberSaveable { mutableStateOf<String?>(null) }
    var activeChatProfile by remember { mutableStateOf<UserSearchApi.PublicProfile?>(null) }
    var showVaultScreen by rememberSaveable { mutableStateOf(false) }
    var pendingVaultNoteAction by remember { mutableStateOf<Pair<Long, Boolean>?>(null) }
    LaunchedEffect(hasVerifiedAdminAccess) {
        if (!hasVerifiedAdminAccess && selectedTab == "Admin") selectedTab = "Settings"
    }
    var permanentDeleteTarget by remember { mutableStateOf<Long?>(null) }

    var sortNewest by rememberSaveable {
        mutableStateOf(true)
    }

    var openedNoteId by rememberSaveable { mutableStateOf<Long?>(null) }
    var draftNoteId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showNotesFeature by rememberSaveable { mutableStateOf(false) }
    // Hoisted above the Notes/Reader route so list position and header collapse survive opening a note.
    val notesListState = rememberLazyListState()
    val notesHeaderCollapseProgress = remember { mutableFloatStateOf(0f) }

    var showEditor by rememberSaveable {
        mutableStateOf(false)
    }

    var editingId by remember {
        mutableStateOf<Long?>(null)
    }

    var title by rememberSaveable {
        mutableStateOf("")
    }

    var content by rememberSaveable {
        mutableStateOf("")
    }

    // Account changes must not carry a pending note action or plaintext editor
    // state into another account, even when both accounts have colliding local IDs.
    LaunchedEffect(userId) {
        com.teddy.bpositive.security.VaultSessionManager.lock()
        pendingVaultNoteAction = null
        openedNoteId = null
        draftNoteId = null
        showEditor = false
        editingId = null
        title = ""
        content = ""
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, userId) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                val openedNoteWasProtected = openedNoteId?.let { openId ->
                    notes.firstOrNull { it.id == openId }?.isLocked == true
                } == true
                notes.indices.toList().forEach { index ->
                    val note = notes[index]
                    if (note.isLocked && !note.isMasked) {
                        notes[index] = note.copy(
                            title = "Locked note",
                            content = "Unlock Secure Vault to view this note.",
                            isPinned = false,
                            isMasked = true
                        )
                    }
                }
                // Do not leave a protected editor holding plaintext after backgrounding.
                if (openedNoteWasProtected) {
                    openedNoteId = null
                    draftNoteId = null
                    showEditor = false
                    editingId = null
                    title = ""
                    content = ""
                }
                saveNotes(context, userId, notes)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var deleteTarget by remember {
        mutableStateOf<Long?>(null)
    }

    val totalNotes = notes.size
    val pinnedNotes = notes.count { it.isPinned }

    val filteredNotes = filterAndSortNotes(
        notes = notes,
        searchQuery = searchQuery,
        showPinnedOnly = showPinnedOnly,
        sortNewest = sortNewest
    )

    fun saveChanges() {
        saveNotes(context, userId, notes)
    }

    fun syncNoteUpsert(note: Note) {
        val session = activeSession ?: return
        val userId = session.userId ?: return

        val syncTitle = if (note.isLocked) "Locked note" else note.title
        val syncContent = if (note.isLocked) NoteEncryptionCodec.toCloudContent(
            note.encryptedPayload ?: error("Locked note payload missing.")
        ) else note.content
        NoteSyncStore.enqueueUpsert(
            context = context, userId = userId, localNoteId = note.id,
            title = syncTitle, content = syncContent,
            isPinned = if (note.isLocked) false else note.isPinned
        )

        NoteSyncRepository.syncPendingChanges(context, session) { result ->
            if (!result.success) {
                Toast.makeText(
                    context,
                    "Saved locally. Cloud sync pending: ${result.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    fun applyVaultNoteAction(noteId: Long, encrypt: Boolean) {
        val index = notes.indexOfFirst { it.id == noteId }
        if (index < 0) return
        val current = notes[index]
        try {
            if (encrypt) {
                if (current.isLocked) return
                val payload = NoteEncryptionCodec.encodeNote(userId, current)
                val protected = current.copy(isLocked = true, encryptedPayload = payload, isMasked = false)
                notes[index] = protected
                saveChanges()
                syncNoteUpsert(protected)
                Toast.makeText(context, "Note encrypted and locked.", Toast.LENGTH_SHORT).show()
            } else {
                if (!current.isLocked) return
                val decoded = NoteEncryptionCodec.decodeNote(
                    userId, current.encryptedPayload ?: error("Encrypted payload missing.")
                )
                notes[index] = current.copy(
                    title = decoded.title, content = decoded.content, isPinned = decoded.isPinned,
                    createdAt = decoded.createdAt, updatedAt = decoded.updatedAt,
                    colorHex = decoded.colorHex, isMasked = false
                )
                saveChanges()
                Toast.makeText(context, "Protected note unlocked for this session.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Secure Vault action failed: ${e.message ?: "Unknown error"}", Toast.LENGTH_LONG).show()
        }
    }

    fun requestVaultNoteAction(note: Note, encrypt: Boolean) {
        if (VaultSessionManager.isUnlocked(userId)) applyVaultNoteAction(note.id, encrypt)
        else {
            pendingVaultNoteAction = note.id to encrypt
            selectedTab = "Settings"
            showVaultScreen = true
            Toast.makeText(context, "Unlock Secure Vault, then return to Notes.", Toast.LENGTH_LONG).show()
        }
    }

    fun maskProtectedNote(note: Note) {
        if (!note.isLocked || note.isMasked) return
        val index = notes.indexOfFirst { it.id == note.id }
        if (index < 0) return
        notes[index] = note.copy(
            title = "Locked note", content = "Unlock Secure Vault to view this note.",
            isPinned = false, isMasked = true
        )
        saveChanges()
    }

    LaunchedEffect(showVaultScreen) {
        if (!showVaultScreen) {
            val pending = pendingVaultNoteAction
            if (pending != null) {
                pendingVaultNoteAction = null
                if (VaultSessionManager.isUnlocked(userId)) {
                    applyVaultNoteAction(pending.first, pending.second)
                } else {
                    Toast.makeText(context, "Vault was not unlocked. Note was not changed.", Toast.LENGTH_LONG).show()
                }
                selectedTab = "Home"
                showNotesFeature = true
            }
        }
    }

    fun syncNoteDelete(note: Note) {
        val session = activeSession ?: return
        val userId = session.userId ?: return

        val syncTitle = if (note.isLocked) "Locked note" else note.title
        val syncContent = if (note.isLocked) NoteEncryptionCodec.toCloudContent(
            note.encryptedPayload ?: error("Locked note payload missing.")
        ) else note.content
        NoteSyncStore.enqueueDelete(
            context = context, userId = userId, localNoteId = note.id,
            title = syncTitle, content = syncContent,
            isPinned = if (note.isLocked) false else note.isPinned
        )

        NoteSyncRepository.syncPendingChanges(context, session) { result ->
            if (!result.success) {
                Toast.makeText(
                    context,
                    "Deletion saved locally. Cloud sync pending: ${result.message}",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                SupabaseNotesApi.fetchNotes(session) { fetchResult ->
                    if (fetchResult.success && fetchResult.data != null) {
                        deletedNotes.clear()
                        deletedNotes.addAll(fetchResult.data.filter { it.deletedAt != null })
                    }
                }
            }
        }
    }

    fun restoreDeletedNote(cloudNote: SupabaseNotesApi.CloudNote) {
        val session = activeSession ?: return
        val clientId = cloudNote.clientId
        if (clientId.isNullOrBlank()) {
            Toast.makeText(context, "This note cannot be restored because its sync ID is missing.", Toast.LENGTH_LONG).show()
            return
        }

        SupabaseNotesApi.upsertNote(
            session = session,
            clientId = clientId,
            title = cloudNote.title,
            content = cloudNote.content,
            isPinned = cloudNote.isPinned,
            deletedAt = null
        ) { result ->
            val restored = result.data
            if (!result.success || restored == null) {
                Toast.makeText(context, "Could not restore note: ${result.message}", Toast.LENGTH_LONG).show()
                return@upsertNote
            }

            var localId = System.currentTimeMillis()
            while (notes.any { it.id == localId }) localId++
            val restoredPayload = NoteEncryptionCodec.extractCloudPayload(restored.content)
            val localNote = if (restoredPayload != null) {
                Note(
                    id = localId,
                    title = "Locked note",
                    content = "Unlock Secure Vault to view this note.",
                    isPinned = false,
                    createdAt = 0L,
                    updatedAt = 0L,
                    isLocked = true,
                    encryptedPayload = restoredPayload,
                    isMasked = true
                )
            } else {
                Note(
                    id = localId,
                    title = restored.title,
                    content = restored.content,
                    isPinned = restored.isPinned,
                    createdAt = parseCloudTimeMillis(restored.createdAt),
                    updatedAt = parseCloudTimeMillis(restored.updatedAt)
                )
            }
            notes.add(0, localNote)
            NoteSyncStore.registerCloudNote(
                context = context,
                userId = session.userId.orEmpty(),
                localNoteId = localId,
                clientId = clientId,
                cloudNoteId = restored.id,
                title = restored.title,
                content = restored.content,
                isPinned = restored.isPinned,
                cloudUpdatedAt = restored.updatedAt
            )
            deletedNotes.removeAll { it.id == cloudNote.id }
            saveChanges()
            showTrash = false
            Toast.makeText(context, "Note restored successfully.", Toast.LENGTH_SHORT).show()
        }
    }

    fun permanentlyDeleteNote(cloudNote: SupabaseNotesApi.CloudNote) {
        val session = activeSession ?: return
        SupabaseNotesApi.permanentlyDeleteNote(session, cloudNote.id) { result ->
            if (result.success && result.data == true) {
                deletedNotes.removeAll { it.id == cloudNote.id }
                Toast.makeText(context, "Note permanently deleted.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(
                    context,
                    "Permanent deletion failed: ${result.message}. Check the Supabase DELETE policy.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    fun refreshConflictCopies() {
        val session = activeSession ?: return
        SupabaseNotesApi.fetchConflictCopies(session) { result ->
            if (result.success && result.data != null) {
                conflictCopies.clear()
                conflictCopies.addAll(result.data)
            } else {
                Toast.makeText(context, "Could not load conflict copies: ${result.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun restoreConflictCopy(copy: SupabaseNotesApi.ConflictCopy) {
        val session = activeSession ?: return
        val userId = session.userId.orEmpty()
        val restoredClientId = "conflict-${copy.id}"
        val existingRecord = NoteSyncStore.findByClientId(context, userId, restoredClientId)
        val localId = existingRecord?.localNoteId ?: run {
            var candidate = System.currentTimeMillis()
            while (notes.any { it.id == candidate }) candidate++
            candidate
        }

        SupabaseNotesApi.upsertNote(
            session = session,
            clientId = restoredClientId,
            title = copy.title,
            content = copy.content,
            isPinned = copy.isPinned,
            deletedAt = null
        ) { result ->
            val restored = result.data
            if (!result.success || restored == null) {
                Toast.makeText(context, "Could not restore conflict copy: ${result.message}", Toast.LENGTH_LONG).show()
                return@upsertNote
            }

            val restoredPayload = NoteEncryptionCodec.extractCloudPayload(restored.content)
            val localNote = if (restoredPayload != null) {
                Note(
                    id = localId,
                    title = "Locked note",
                    content = "Unlock Secure Vault to view this note.",
                    isPinned = false,
                    createdAt = 0L,
                    updatedAt = 0L,
                    isLocked = true,
                    encryptedPayload = restoredPayload,
                    isMasked = true
                )
            } else {
                Note(
                    id = localId,
                    title = restored.title,
                    content = restored.content,
                    isPinned = restored.isPinned,
                    createdAt = parseCloudTimeMillis(restored.createdAt),
                    updatedAt = parseCloudTimeMillis(restored.updatedAt)
                )
            }
            val localIndex = notes.indexOfFirst { it.id == localId }
            if (localIndex >= 0) notes[localIndex] = localNote else notes.add(0, localNote)

            NoteSyncStore.registerCloudNote(
                context = context,
                userId = userId,
                localNoteId = localId,
                clientId = restoredClientId,
                cloudNoteId = restored.id,
                title = restored.title,
                content = restored.content,
                isPinned = restored.isPinned,
                cloudUpdatedAt = restored.updatedAt
            )
            saveChanges()

            SupabaseNotesApi.resolveConflictCopy(session, copy.id) { resolveResult ->
                if (resolveResult.success && resolveResult.data == true) {
                    conflictCopies.removeAll { it.id == copy.id }
                    Toast.makeText(context, "Conflict copy restored as a separate note.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Note restored, but conflict status could not be updated: ${resolveResult.message}", Toast.LENGTH_LONG).show()
                    refreshConflictCopies()
                }
            }
        }
    }

    fun dismissConflictCopy(copy: SupabaseNotesApi.ConflictCopy) {
        val session = activeSession ?: return
        SupabaseNotesApi.resolveConflictCopy(session, copy.id) { result ->
            if (result.success && result.data == true) {
                conflictCopies.removeAll { it.id == copy.id }
                Toast.makeText(context, "Conflict copy dismissed.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Could not dismiss conflict copy: ${result.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun clearEditor() {
        title = ""
        content = ""
        editingId = null
        showEditor = false
    }

    fun saveNote() {

        if (title.isBlank() || content.isBlank()) {
            Toast.makeText(
                context,
                "Please enter both title and content.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val currentTime = System.currentTimeMillis()

        if (editingId != null) {

            val index = notes.indexOfFirst {
                it.id == editingId
            }

            if (index != -1) {
                val oldNote = notes[index]

                notes[index] = oldNote.copy(
                    title = title.trim(),
                    content = content.trim(),
                    updatedAt = currentTime
                )
            }

            Toast.makeText(
                context,
                "Note updated successfully!",
                Toast.LENGTH_SHORT
            ).show()

        } else {

            notes.add(
                0,
                Note(
                    id = currentTime,
                    title = title.trim(),
                    content = content.trim(),
                    isPinned = false,
                    createdAt = currentTime,
                    updatedAt = currentTime
                )
            )

            Toast.makeText(
                context,
                "Note added successfully!",
                Toast.LENGTH_SHORT
            ).show()
        }

        saveChanges()
        notes.firstOrNull { it.id == (editingId ?: currentTime) }?.let { syncNoteUpsert(it) }
        clearEditor()
    }

    fun editNote(note: Note) {
        if (note.isMasked) {
            requestVaultNoteAction(note, encrypt = false)
            return
        }
        if (note.isLocked && !com.teddy.bpositive.security.VaultSessionManager.isUnlocked(userId)) {
            maskProtectedNote(note)
            val masked = notes.firstOrNull { it.id == note.id } ?: note.copy(
                title = "Locked note",
                content = "Unlock Secure Vault to view this note.",
                isPinned = false,
                isMasked = true
            )
            requestVaultNoteAction(masked, encrypt = false)
            return
        }
        openedNoteId = note.id
        showEditor = false
    }

    fun saveOpenedNote(noteId: Long, newTitle: String, newContent: String, newColor: String) {
        val index = notes.indexOfFirst { it.id == noteId }
        if (index < 0) return
        val previous = notes[index]
        if (previous.isMasked) {
            Toast.makeText(context, "Unlock Secure Vault before editing this note.", Toast.LENGTH_LONG).show()
            openedNoteId = null
            title = ""
            content = ""
            return
        }
        if (previous.isLocked && !com.teddy.bpositive.security.VaultSessionManager.isUnlocked(userId)) {
            maskProtectedNote(previous)
            Toast.makeText(context, "Vault session expired. The note was hidden; unlock it before editing.", Toast.LENGTH_LONG).show()
            openedNoteId = null
            title = ""
            content = ""
            return
        }
        var updated = previous.copy(
            title = newTitle.trim(), content = newContent,
            colorHex = newColor, updatedAt = System.currentTimeMillis()
        )
        if (previous.isLocked) {
            updated = updated.copy(
                encryptedPayload = NoteEncryptionCodec.encodeNote(userId, updated),
                isMasked = false
            )
        }
        notes[index] = updated
        saveChanges()
        syncNoteUpsert(updated)
        if (draftNoteId == noteId) draftNoteId = null
        Toast.makeText(context, "Note saved.", Toast.LENGTH_SHORT).show()
    }

    fun createNewNote() {
        val now = System.currentTimeMillis()
        var newId = now
        while (notes.any { it.id == newId }) newId++
        notes.add(
            0,
            Note(
                id = newId,
                title = "",
                content = "",
                isPinned = false,
                createdAt = now,
                updatedAt = now
            )
        )
        draftNoteId = newId
        openedNoteId = newId
    }

    fun deleteNoteFromNotes(note: Note) {
        syncNoteDelete(note)
        notes.removeAll { it.id == note.id }
        saveChanges()
        Toast.makeText(context, "Note moved to Trash.", Toast.LENGTH_SHORT).show()
    }

    fun shareNote(note: Note) {
        if (note.isMasked) {
            Toast.makeText(context, "Unlock Secure Vault before sharing this note.", Toast.LENGTH_LONG).show()
            return
        }
        if (note.isLocked && !com.teddy.bpositive.security.VaultSessionManager.isUnlocked(userId)) {
            maskProtectedNote(note)
            Toast.makeText(context, "Vault session expired. The note was hidden; unlock it before sharing.", Toast.LENGTH_LONG).show()
            return
        }
        val shareText = buildString {
            if (note.title.isNotBlank()) {
                append(note.title)
                append("\n\n")
            }
            append(note.content)
        }
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share note"))
    }

    fun togglePin(note: Note) {
        if (note.isMasked) {
            Toast.makeText(context, "Unlock Secure Vault before changing this note's pin state.", Toast.LENGTH_LONG).show()
            return
        }
        if (note.isLocked && !com.teddy.bpositive.security.VaultSessionManager.isUnlocked(userId)) {
            maskProtectedNote(note)
            Toast.makeText(context, "Vault session expired. The note was hidden; unlock it before changing its pin.", Toast.LENGTH_LONG).show()
            return
        }

        val index = notes.indexOfFirst {
            it.id == note.id
        }

        if (index != -1) {
            var updated = toggleNotePinned(note)
            if (note.isLocked) {
                updated = updated.copy(
                    encryptedPayload = NoteEncryptionCodec.encodeNote(userId, updated),
                    isMasked = false
                )
            }
            notes[index] = updated

            saveChanges()
            notes.firstOrNull { it.id == note.id }?.let { syncNoteUpsert(it) }
        }
    }
        TeddyAppTheme(darkMode = darkMode) {

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (openedNoteId == null) AppBottomNavigationBar(
                    selectedTab = selectedTab,
                    showAdmin = hasVerifiedAdminAccess,
                    onTabSelected = { tab ->
                        selectedTab = tab
                        showVaultScreen = false
                        showNotesFeature = false
                        if (tab == "Inbox") {
                            val session = activeSession
                            if (session != null) {
                                isLoadingInbox = true
                                InboxRepository.load(session) { result ->
                                    if (activeSession?.userId != session.userId) return@load
                                    isLoadingInbox = false
                                    if (result.success) { inboxItems = result.items; inboxStatus = "" }
                                    else inboxStatus = result.message
                                }
                            }
                        }
                        if (tab == "Trash") {
                            showTrash = true
                            showConflicts = false
                            showPinnedOnly = false
                            searchQuery = ""
                        } else if (tab == "Home") {
                            showTrash = false
                            showConflicts = false
                        }
                    }
                )
            },
            topBar = {
                if (openedNoteId == null && !(selectedTab == "Home" && showNotesFeature) && !(selectedTab == "Messages" && !showChatScreen && !showUserSearch)) WorkspaceTopBar(
                    fullName = currentProfile?.fullName.orEmpty(),
                    avatarUrl = currentProfile?.avatarUrl.orEmpty(),
                    darkMode = darkMode,
                    onProfileClick = {
                        currentProfile?.let { profile ->
                            profileFullName = profile.fullName
                            profileUsername = profile.username.orEmpty()
                            profileBio = profile.bio
                            profileAvatarUrl = profile.avatarUrl.orEmpty()
                        }
                        showProfileDialog = true
                    },
                    onToggleDarkMode = {
                        darkMode = !darkMode
                        context.getSharedPreferences(
                            "teddy_settings",
                            Context.MODE_PRIVATE
                        ).edit()
                            .putBoolean("dark_mode", darkMode)
                            .apply()
                    }
                )
            }

        ) { padding ->

            if (selectedTab == "Home" && openedNoteId != null) {
                val openedNote = notes.firstOrNull { it.id == openedNoteId }
                if (openedNote != null) {
                    NoteDetailScreen(
                        note = openedNote,
                        contentPadding = padding,
                        onBack = {
                            if (draftNoteId == openedNote.id) {
                                notes.removeAll { it.id == openedNote.id }
                                draftNoteId = null
                                saveChanges()
                            }
                            openedNoteId = null
                        },
                        onSave = { newTitle, newContent, newColor ->
                            saveOpenedNote(openedNote.id, newTitle, newContent, newColor)
                        },
                        startInEditingMode = draftNoteId == openedNote.id
                    )
                } else openedNoteId = null
            } else if (selectedTab == "Home" && showNotesFeature) {
                NotesScreen(
                    notes = filteredNotes,
                    contentPadding = padding,
                    listState = notesListState,
                    collapseProgress = notesHeaderCollapseProgress,
                    onNoteClick = { note ->
                        if (note.isMasked) requestVaultNoteAction(note, encrypt = false)
                        else editNote(note)
                    },
                    onCreateNote = { createNewNote() },
                    onTogglePin = { note -> togglePin(note) },
                    onDeleteNote = { note -> deleteNoteFromNotes(note) },
                    onShareNote = { note -> shareNote(note) },
                    onLockNote = { note ->
                        when {
                            note.isMasked -> requestVaultNoteAction(note, encrypt = false)
                            note.isLocked -> maskProtectedNote(note)
                            else -> requestVaultNoteAction(note, encrypt = true)
                        }
                    },
                    onDeleteSelected = { selectedNotes ->
                        selectedNotes.forEach { syncNoteDelete(it) }
                        val selectedIds = selectedNotes.map { it.id }.toSet()
                        notes.removeAll { selectedIds.contains(it.id) }
                        saveChanges()
                    }
                )
            } else if (selectedTab == "Home") {
                HomeScreen(
                    contentPadding = padding,
                    onOpenNotes = { showNotesFeature = true }
                )
            } else if (selectedTab == "Messages" && showChatScreen && activeChatProfile != null) {
                val chatSession = activeSession
                val chatConversationId = activeConversationId
                if (chatSession != null && !chatConversationId.isNullOrBlank()) {
                    MessagingChatScreen(
                        context = context,
                        contentPadding = padding,
                        session = chatSession,
                        conversationId = chatConversationId,
                        displayName = activeChatProfile?.fullName.orEmpty(),
                        username = activeChatProfile?.username.orEmpty(),
                        onBack = {
                            showChatScreen = false
                            activeConversationId = null
                            activeChatProfile = null
                        }
                    )
                } else {
                    Text("Your session or conversation is unavailable. Please go back and try again.")
                }
            } else if (selectedTab == "Messages" && showUserSearch) {
                MessagingUserSearchScreen(
                    contentPadding = padding,
                    profiles = userSearchResults,
                    isSearching = isSearchingUsers,
                    status = userSearchStatus,
                    onBack = {
                        showUserSearch = false
                        userSearchResults = emptyList()
                        userSearchStatus = ""
                    },
                    onSearch = { query ->
                        val session = activeSession ?: return@MessagingUserSearchScreen
                        isSearchingUsers = true
                        userSearchStatus = "Searching Supabase profiles..."
                        UserSearchApi.search(context, session, query) { result ->
                            if (activeSession?.userId != session.userId) return@search
                            isSearchingUsers = false
                            userSearchResults = if (result.success) result.profiles else emptyList()
                            userSearchStatus = result.message
                        }
                    },
                    onStartChat = { profile ->
                        val session = activeSession
                        if (session == null || isCreatingConversation) return@MessagingUserSearchScreen
                        isCreatingConversation = true
                        userSearchStatus = "Creating private conversation..."
                        DirectConversationApi.createOrGet(session, profile.id) { result ->
                            if (activeSession?.userId != session.userId) return@createOrGet
                            isCreatingConversation = false
                            if (result.success && !result.conversationId.isNullOrBlank()) {
                                activeConversationId = result.conversationId
                                activeChatProfile = profile
                                showUserSearch = false
                                showChatScreen = true
                                userSearchStatus = ""
                            } else {
                                userSearchStatus = result.message
                                Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                )
            } else if (selectedTab == "Messages") {
                MessagingHomeScreen(
                    contentPadding = padding,
                    onFindPeople = {
                        userSearchResults = emptyList()
                        userSearchStatus = ""
                        showUserSearch = true
                    }
                )
            } else if (selectedTab == "Trash") {
                TrashScreen(
                    deletedNotes = deletedNotes,
                    contentPadding = padding,
                    onRestore = { restoreDeletedNote(it) },
                    onDeletePermanently = { permanentDeleteTarget = it.id }
                )
            } else if (selectedTab == "Inbox") {
                NotificationInboxScreen(
                    items = inboxItems,
                    loading = isLoadingInbox,
                    status = inboxStatus,
                    padding = padding,
                    onRefresh = {
                        val session = activeSession ?: return@NotificationInboxScreen
                        isLoadingInbox = true
                        InboxRepository.load(session) { result ->
                            if (activeSession?.userId != session.userId) return@load
                            isLoadingInbox = false
                            if (result.success) { inboxItems = result.items; inboxStatus = "" }
                            else inboxStatus = result.message
                        }
                    },
                    onRead = { item ->
                        val session = activeSession ?: return@NotificationInboxScreen
                        InboxRepository.markRead(session, item.id) { success, message ->
                            if (activeSession?.userId != session.userId) return@markRead
                            if (success) {
                                inboxItems = inboxItems.map {
                                    if (it.id == item.id) it.copy(readAt = java.time.Instant.now().toString()) else it
                                }
                            } else inboxStatus = message
                        }
                    }
                )
            } else if (selectedTab == "Profile") {
                ProfilePage(
                    profile = currentProfile,
                    email = activeSession?.email.orEmpty(),
                    contentPadding = padding,
                    canEdit = hasValidatedInternet(context),
                    onEdit = {
                    currentProfile?.let { profile ->
                        profileFullName = profile.fullName
                        profileUsername = profile.username.orEmpty()
                        profileBio = profile.bio
                        profileAvatarUrl = profile.avatarUrl.orEmpty()
                    }
                    showProfileDialog = true
                })
            } else if (selectedTab == "Admin" && hasVerifiedAdminAccess) {
                AdminPanelScreen(
                    accountEmail = activeSession?.email.orEmpty(),
                    isLoading = isLoadingAdminHistory || isCheckingAdmin,
                    isSending = isSendingAdminNotification,
                    title = adminTitle,
                    message = adminMessage,
                    history = adminHistory,
                    directory = adminDirectory,
                    isLoadingDirectory = isLoadingAdminDirectory,
                    statusMessage = adminStatusMessage,
                    onTitleChange = { adminTitle = it.take(100) },
                    onMessageChange = { adminMessage = it.take(500) },
                    onSendTopic = { topic ->
                        val session = activeSession ?: return@AdminPanelScreen
                        val titleToSend = adminTitle.trim()
                        val bodyToSend = adminMessage.trim()
                        if (titleToSend.isNotBlank() && bodyToSend.isNotBlank()) {
                            isSendingAdminNotification = true
                            adminStatusMessage = "Sending topic notification..."
                            TopicRepository.send(session, topic, titleToSend, bodyToSend) { success, resultMessage ->
                                if (activeSession?.userId != session.userId) return@send
                                isSendingAdminNotification = false
                                adminStatusMessage = resultMessage
                                if (success) {
                                    adminTitle = ""
                                    adminMessage = ""
                                    isLoadingAdminHistory = true
                                    AdminRepository.loadHistory(session) { historyResult ->
                                        if (activeSession?.userId != session.userId) return@loadHistory
                                        isLoadingAdminHistory = false
                                        if (historyResult.success) adminHistory = historyResult.data.orEmpty()
                                    }
                                }
                                Toast.makeText(context, resultMessage, Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    onSend = { audience, recipientEmails ->
                        val session = activeSession ?: return@AdminPanelScreen
                        val titleToSend = adminTitle.trim()
                        val bodyToSend = adminMessage.trim()
                        if (titleToSend.isNotBlank() && bodyToSend.isNotBlank()) {
                            isSendingAdminNotification = true
                            adminStatusMessage = "Sending notification..."
                            AdminRepository.sendNotification(
                                session, titleToSend, bodyToSend, audience, recipientEmails
                            ) { result ->
                                if (activeSession?.userId != session.userId) return@sendNotification
                                isSendingAdminNotification = false
                                adminStatusMessage = result.message
                                if (result.success) {
                                    adminTitle = ""
                                    adminMessage = ""
                                    isLoadingAdminHistory = true
                                    AdminRepository.loadHistory(session) { historyResult ->
                                        if (activeSession?.userId != session.userId) return@loadHistory
                                        isLoadingAdminHistory = false
                                        if (historyResult.success) adminHistory = historyResult.data.orEmpty()
                                        else adminStatusMessage = historyResult.message
                                    }
                                }
                                Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    onRefresh = {
                        val session = activeSession ?: return@AdminPanelScreen
                        isLoadingAdminHistory = true
                        AdminRepository.loadHistory(session) { result ->
                            if (activeSession?.userId != session.userId) return@loadHistory
                            isLoadingAdminHistory = false
                            if (result.success) {
                                adminHistory = result.data.orEmpty()
                                adminStatusMessage = "History refreshed."
                            } else adminStatusMessage = result.message
                        }
                    },
                    onRefreshDirectory = {
                        val session = activeSession ?: return@AdminPanelScreen
                        isLoadingAdminDirectory = true
                        AdminRepository.loadUserDirectory(session) { result ->
                            if (activeSession?.userId != session.userId) return@loadUserDirectory
                            isLoadingAdminDirectory = false
                            if (result.success) {
                                adminDirectory = result.data.orEmpty()
                                adminStatusMessage = "User directory refreshed."
                            } else adminStatusMessage = result.message
                        }
                    }
                )
            } else if (selectedTab == "Settings" && showVaultScreen) {
                activeSession?.let { vaultSession ->
                    SecureVaultScreen(
                        userId = vaultSession.userId.orEmpty(),
                        session = vaultSession,
                        contentPadding = padding,
                        onBack = {
                            showVaultScreen = false
                            if (pendingVaultNoteAction != null) {
                                selectedTab = "Home"
                                showNotesFeature = true
                            }
                        }
                    )
                } ?: Text("Please sign in again to open Secure Vault.")
            } else if (selectedTab == "Settings") {
                SettingsScreen(
                    darkMode = darkMode,
                    accountEmail = activeSession?.email.orEmpty(),
                    isAdmin = hasVerifiedAdminAccess,
                    onOpenAdminPanel = { selectedTab = "Admin" },
                    onOpenVault = { showVaultScreen = true },
                    contentPadding = padding,
                    onDarkModeChange = { enabled ->
                        darkMode = enabled
                        context.getSharedPreferences("teddy_settings", Context.MODE_PRIVATE).edit()
                            .putBoolean("dark_mode", enabled).apply()
                    },
                    topicPreferences = topicPreferences,
                    topicStatus = topicStatus,
                    installedVersionName = installedVersionName,
                    installedVersionCode = installedVersionCode,
                    latestVersionName = updateMetadata?.latestVersionName,
                    latestVersionCode = updateMetadata?.latestVersionCode,
                    updateStatus = updateStatus,
                    isCheckingUpdate = isCheckingUpdate,
                    onCheckForUpdates = { checkForAppUpdates(manualCheck = true) },
                    onTopicChange = { topic, enabled ->
                        val session = activeSession
                        if (session != null) {
                            topicStatus = "Updating $topic subscription..."
                            TopicRepository.set(session, topic, enabled) { success, message ->
                                if (activeSession?.userId != session.userId) return@set
                                if (success) {
                                    topicPreferences = when (topic) {
                                        "all_users" -> topicPreferences.copy(allUsers = enabled)
                                        "announcements" -> topicPreferences.copy(announcements = enabled)
                                        else -> topicPreferences.copy(updates = enabled)
                                    }
                                    topicStatus = "Topic preference updated."
                                } else topicStatus = message
                            }
                        }
                    },
                    onSendTestNotification = {
                        val session = activeSession
                        if (session != null) {
                            FcmTokenRepository.sendTestNotification(session) { success, message ->
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                android.util.Log.i("TeddyFCM", "Push test result: success=$success; $message")
                            }
                        }
                    },
                    onSignOut = {
                        com.teddy.bpositive.security.VaultSessionManager.lock()
                        deletedNotes.clear()
                        conflictCopies.clear()
                        notes.clear()
                        currentProfile = null
                        profileFullName = ""
                        profileUsername = ""
                        profileBio = ""
                        profileAvatarUrl = ""
                        showProfileDialog = false
                        showEditor = false
                        openedNoteId = null
                        draftNoteId = null
                        editingId = null
                        title = ""
                        content = ""
                        searchQuery = ""
                        showPinnedOnly = false
                        showTrash = false
                        showConflicts = false
                        selectedTab = "Home"
                        showUserSearch = false
                        showChatScreen = false
                        activeConversationId = null
                        activeChatProfile = null
                        isCreatingConversation = false
                        userSearchResults = emptyList()
                        userSearchStatus = ""
                        showVaultScreen = false
                        isAdmin = false
                        adminVerifiedUserId = null
                        isCheckingAdmin = false
                        isSendingAdminNotification = false
                        isLoadingAdminHistory = false
                        isLoadingAdminDirectory = false
                        adminHistory = emptyList()
                        adminDirectory = emptyList()
                        inboxItems = emptyList()
                        inboxStatus = ""
                        isLoadingInbox = false
                        adminStatusMessage = ""
                        activeSession?.let { session ->
                            FcmTokenRepository.unregisterCurrentDevice(context, session)
                        }
                        SupabaseSessionManager.clear(context)
                        activeSession = null
                    }
                )
            }
        }
    }

    if (showProfileDialog) {
        ProfileEditorDialog(
            fullName = profileFullName,
            username = profileUsername,
            bio = profileBio,
            avatarUrl = profileAvatarUrl,
            email = activeSession?.email.orEmpty(),
            createdAt = currentProfile?.createdAt ?: "Loading...",
            isSaving = isSavingProfile,
            canSave = hasValidatedInternet(context),
            onFullNameChange = { profileFullName = it },
            onUsernameChange = { value -> profileUsername = value.filter { it.isLetterOrDigit() || it == '_' || it == '.' } },
            onBioChange = { if (it.length <= 160) profileBio = it },
            onAvatarUrlChange = { profileAvatarUrl = it },
            onDismiss = { if (!isSavingProfile) showProfileDialog = false },
            onSave = {
                val session = activeSession ?: return@ProfileEditorDialog
                isSavingProfile = true
                SupabaseProfilesApi.saveProfile(context, session, profileFullName, profileUsername, profileBio, profileAvatarUrl) { result ->
                    if (activeSession?.userId != session.userId) return@saveProfile
                    isSavingProfile = false
                    if (result.success && result.data != null) {
                        currentProfile = result.data
                        showProfileDialog = false
                        Toast.makeText(context, "Profile updated successfully.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Profile update failed: " + result.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    if (showUpdateDialog && updateMetadata != null) {
        UpdatePromptDialog(
            metadata = updateMetadata!!,
            mandatory = isMandatoryUpdate,
            isDownloading = isDownloadingUpdate,
            progress = updateDownloadProgress,
            downloadedBytes = updateDownloadBytes,
            totalBytes = updateTotalBytes,
            downloadMessage = updateDownloadMessage,
            isVerified = verifiedUpdateApk?.exists() == true,
            onLater = { dismissOptionalUpdateForToday() },
            onDownload = { openUpdateDownload() }
        )
    }

    if (permanentDeleteTarget != null) {
        val target = deletedNotes.firstOrNull { it.id == permanentDeleteTarget }
        PermanentDeleteConfirmationDialog(
            onConfirm = {
                if (target != null) permanentlyDeleteNote(target)
                permanentDeleteTarget = null
            },
            onCancel = { permanentDeleteTarget = null }
        )
    }

    if (deleteTarget != null) {

        DeleteConfirmationDialog(

            onConfirm = {

                val noteToDelete = notes.firstOrNull { it.id == deleteTarget }
                if (noteToDelete != null) {
                    syncNoteDelete(noteToDelete)
                    notes.removeAll { it.id == deleteTarget }
                    saveChanges()
                }
                deleteTarget = null

                Toast.makeText(
                    context,
                    "Note deleted.",
                    Toast.LENGTH_SHORT
                ).show()
            },

            onCancel = {
                deleteTarget = null
            }
        )
    }
}




@Composable
private fun UpdatePromptDialog(
    metadata: UpdateMetadata,
    mandatory: Boolean,
    isDownloading: Boolean,
    progress: Float,
    downloadedBytes: Long,
    totalBytes: Long,
    downloadMessage: String,
    isVerified: Boolean,
    onLater: () -> Unit,
    onDownload: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!mandatory && !isDownloading) onLater() },
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnBackPress = !mandatory && !isDownloading,
            dismissOnClickOutside = !mandatory && !isDownloading
        ),
        title = {
            Text(if (mandatory) "Required update" else "Update available")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Version ${metadata.latestVersionName} is ready to install.")
                if (metadata.releaseNotes.isNotBlank()) {
                    Text(metadata.releaseNotes, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (mandatory) {
                    Text(
                        "This version is no longer supported. Update the app to continue.",
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (isDownloading) {
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                    val sizeText = if (totalBytes > 0L) {
                        "${formatUpdateBytes(downloadedBytes)} / ${formatUpdateBytes(totalBytes)}"
                    } else {
                        "${formatUpdateBytes(downloadedBytes)} downloaded"
                    }
                    Text(sizeText, style = MaterialTheme.typography.bodySmall)
                }
                if (downloadMessage.isNotBlank()) {
                    Text(downloadMessage, style = MaterialTheme.typography.bodySmall)
                }
                if (isVerified) {
                    Text(
                        "SHA-256 verification successful.",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDownload,
                enabled = !isDownloading && metadata.downloadUrl.startsWith("https://") &&
                    metadata.sha256.matches(Regex("(?i)[a-f0-9]{64}"))
            ) {
                Text(
                    when {
                        isDownloading -> "Downloading..."
                        isVerified -> "Install update"
                        else -> "Download update"
                    }
                )
            }
        },
        dismissButton = if (mandatory || isDownloading) null else {
            {
                TextButton(onClick = onLater) {
                    Text("Later")
                }
            }
        }
    )
}

private fun formatUpdateBytes(bytes: Long): String {
    if (bytes < 1024L) return "${bytes} B"
    val kb = bytes / 1024.0
    if (kb < 1024.0) return String.format(Locale.US, "%.1f KB", kb)
    return String.format(Locale.US, "%.1f MB", kb / 1024.0)
}
