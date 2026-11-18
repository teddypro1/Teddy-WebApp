package com.teddy.bpositive

import android.os.Bundle
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.teddy.bpositive.data.SupabaseAuthRepository
import com.teddy.bpositive.data.SupabaseSessionManager
import com.teddy.bpositive.data.NoteSyncStore
import com.teddy.bpositive.data.NoteSyncRepository
import com.teddy.bpositive.data.SupabaseNotesApi
import com.teddy.bpositive.data.SupabaseRealtimeSubscription
import com.teddy.bpositive.data.SupabaseProfilesApi
import com.teddy.bpositive.model.Note
import com.teddy.bpositive.data.LocalNotesStorage.loadNotes
import com.teddy.bpositive.data.LocalNotesStorage.saveNotes
import com.teddy.bpositive.utils.parseCloudTimestampMillis
import com.teddy.bpositive.utils.hasValidatedInternet
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
import com.teddy.bpositive.ui.screens.HomeScreen
import com.teddy.bpositive.ui.dialogs.ProfileEditorDialog
import com.teddy.bpositive.ui.dialogs.DeleteConfirmationDialog
import com.teddy.bpositive.ui.dialogs.PermanentDeleteConfirmationDialog
import com.teddy.bpositive.ui.components.AppBottomNavigationBar
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TeddyNotesApp()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeddyNotesApp() {

    val context = androidx.compose.ui.platform.LocalContext.current

    var isCheckingSession by remember { mutableStateOf(true) }
    var activeSession by remember { mutableStateOf<SupabaseSessionManager.Session?>(null) }
    val isAuthenticated = activeSession != null

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
                activeSession = session
            }
        )
        return
    }

    val userId = activeSession?.userId.orEmpty()
    val notes = remember(userId) {
        mutableStateListOf<Note>().apply {
            addAll(loadNotes(context, userId))
        }
    }
    val deletedNotes = remember(userId) { mutableStateListOf<SupabaseNotesApi.CloudNote>() }
    val conflictCopies = remember(userId) { mutableStateListOf<SupabaseNotesApi.ConflictCopy>() }
    var currentProfile by remember(userId) { mutableStateOf<SupabaseProfilesApi.Profile?>(null) }
    var showProfileDialog by rememberSaveable { mutableStateOf(false) }
    var profileFullName by remember(userId) { mutableStateOf("") }
    var profileUsername by remember(userId) { mutableStateOf("") }
    var profileBio by remember(userId) { mutableStateOf("") }
    var profileAvatarUrl by remember(userId) { mutableStateOf("") }
    var isSavingProfile by remember { mutableStateOf(false) }

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
                        val localNote = Note(
                            id = localId,
                            title = cloudNote.title,
                            content = cloudNote.content,
                            isPinned = cloudNote.isPinned,
                            createdAt = parseCloudTimestampMillis(cloudNote.createdAt)
                                ?: previousLocalNote?.createdAt
                                ?: System.currentTimeMillis(),
                            updatedAt = parseCloudTimestampMillis(cloudNote.updatedAt)
                                ?: previousLocalNote?.updatedAt
                                ?: System.currentTimeMillis()
                        )

                        val localIndex = notes.indexOfFirst { it.id == localId }
                        if (localIndex >= 0) notes[localIndex] = localNote
                        else notes.add(localNote)
                    }
                }

                saveNotes(context, userId, notes)
                if (result.processed > 0 || fetchResult.data.isNotEmpty()) {
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
                            val refreshedNote = Note(
                                id = localId,
                                title = cloudNote.title,
                                content = cloudNote.content,
                                isPinned = cloudNote.isPinned,
                                createdAt = parseCloudTimestampMillis(cloudNote.createdAt)
                                    ?: previousLocalNote?.createdAt
                                    ?: System.currentTimeMillis(),
                                updatedAt = parseCloudTimestampMillis(cloudNote.updatedAt)
                                    ?: previousLocalNote?.updatedAt
                                    ?: System.currentTimeMillis()
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
                                            ?: System.currentTimeMillis()
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
    var permanentDeleteTarget by remember { mutableStateOf<Long?>(null) }

    var sortNewest by rememberSaveable {
        mutableStateOf(true)
    }

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

    var deleteTarget by remember {
        mutableStateOf<Long?>(null)
    }

    val totalNotes = notes.size
    val pinnedNotes = notes.count { it.isPinned }

    val filteredNotes = notes
        .filter {
            (!showPinnedOnly || it.isPinned) &&
            (
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.content.contains(searchQuery, ignoreCase = true)
            )
        }
        .let {
            if (sortNewest) {
                it.sortedByDescending { note -> note.updatedAt }
            } else {
                it.sortedBy { note -> note.updatedAt }
            }
        }

    fun saveChanges() {
        saveNotes(context, userId, notes)
    }

    fun syncNoteUpsert(note: Note) {
        val session = activeSession ?: return
        val userId = session.userId ?: return

        NoteSyncStore.enqueueUpsert(
            context = context,
            userId = userId,
            localNoteId = note.id,
            title = note.title,
            content = note.content,
            isPinned = note.isPinned
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

    fun syncNoteDelete(note: Note) {
        val session = activeSession ?: return
        val userId = session.userId ?: return

        NoteSyncStore.enqueueDelete(
            context = context,
            userId = userId,
            localNoteId = note.id,
            title = note.title,
            content = note.content,
            isPinned = note.isPinned
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
            fun cloudTime(value: String?): Long = try {
                if (value.isNullOrBlank()) System.currentTimeMillis()
                else java.time.Instant.parse(value).toEpochMilli()
            } catch (_: Exception) {
                System.currentTimeMillis()
            }

            val localNote = Note(
                id = localId,
                title = restored.title,
                content = restored.content,
                isPinned = restored.isPinned,
                createdAt = cloudTime(restored.createdAt),
                updatedAt = cloudTime(restored.updatedAt)
            )
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

            fun cloudTime(value: String?): Long = try {
                if (value.isNullOrBlank()) System.currentTimeMillis()
                else java.time.Instant.parse(value).toEpochMilli()
            } catch (_: Exception) {
                System.currentTimeMillis()
            }

            val localNote = Note(
                id = localId,
                title = restored.title,
                content = restored.content,
                isPinned = restored.isPinned,
                createdAt = cloudTime(restored.createdAt),
                updatedAt = cloudTime(restored.updatedAt)
            )
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
        editingId = note.id
        title = note.title
        content = note.content
        showEditor = true
    }

    fun togglePin(note: Note) {

        val index = notes.indexOfFirst {
            it.id == note.id
        }

        if (index != -1) {
            notes[index] = note.copy(
                isPinned = !note.isPinned,
                updatedAt = System.currentTimeMillis()
            )

            saveChanges()
            notes.firstOrNull { it.id == note.id }?.let { syncNoteUpsert(it) }
        }
    }
        MaterialTheme(
        colorScheme = if (darkMode) {
            darkColorScheme(
                primary = Color(0xFF8BA9FF),
                secondary = Color(0xFFB8C7FF),
                background = Color(0xFF10131C),
                surface = Color(0xFF1A1E29)
            )
        } else {
            lightColorScheme(
                primary = Color(0xFF3459D6),
                secondary = Color(0xFF607CE5),
                background = Color(0xFFF3F5FB),
                surface = Color.White
            )
        }
    ) {

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                AppBottomNavigationBar(
                    selectedTab = selectedTab,
                    onTabSelected = { tab ->
                        selectedTab = tab
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
                CenterAlignedTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(
                                        Color(0xFF5377E8),
                                        RoundedCornerShape(13.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "T",
                                    color = Color.White,
                                    fontSize = 25.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(Modifier.width(12.dp))

                            Column {
                                Text(
                                    "Teddy WebApp",
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Personal Workspace",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },

                    actions = {

                        IconButton(
                            onClick = {
                                currentProfile?.let { profile ->
                                    profileFullName = profile.fullName
                                    profileUsername = profile.username.orEmpty()
                                    profileBio = profile.bio
                                    profileAvatarUrl = profile.avatarUrl.orEmpty()
                                }
                                showProfileDialog = true
                            }
                        ) {
                            Box(
                                modifier = Modifier.size(34.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    currentProfile?.fullName?.trim()?.firstOrNull()?.uppercase() ?: "👤",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                currentProfile?.avatarUrl?.takeIf { it.isNotBlank() }?.let { avatar ->
                                    AsyncImage(
                                        model = avatar,
                                        contentDescription = "Profile picture",
                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = {
                                darkMode = !darkMode

                                context.getSharedPreferences(
                                    "teddy_settings",
                                    Context.MODE_PRIVATE
                                ).edit()
                                    .putBoolean("dark_mode", darkMode)
                                    .apply()
                            }
                        ) {
                            Text(
                                if (darkMode) "☀️" else "🌙",
                                fontSize = 21.sp
                            )
                        }
                    }
                )
            }

        ) { padding ->

            if (selectedTab == "Home") {
                HomeScreen(
                    notes = notes,
                    deletedNotes = deletedNotes,
                    conflictCopies = conflictCopies,
                    contentPadding = padding,
                    searchQuery = searchQuery,
                    showPinnedOnly = showPinnedOnly,
                    showTrash = showTrash,
                    showConflicts = showConflicts,
                    sortNewest = sortNewest,
                    showEditor = showEditor,
                    title = title,
                    content = content,
                    editingId = editingId,
                    filteredNotes = filteredNotes,
                    onSearchQueryChange = { searchQuery = it },
                    onShowPinnedOnlyChange = { showPinnedOnly = it },
                    onToggleSort = { sortNewest = !sortNewest },
                    onNewNote = {
                        if (!showEditor) {
                            editingId = null
                            title = ""
                            content = ""
                            showEditor = true
                        }
                    },
                    onToggleTrash = {
                        showTrash = !showTrash
                        showConflicts = false
                        searchQuery = ""
                        showPinnedOnly = false
                    },
                    onToggleConflicts = {
                        showConflicts = !showConflicts
                        showTrash = false
                        searchQuery = ""
                        showPinnedOnly = false
                        if (showConflicts) refreshConflictCopies()
                    },
                    onTitleChange = { title = it },
                    onContentChange = { if (it.length <= 3000) content = it },
                    onSave = { saveNote() },
                    onCancel = { clearEditor() },
                    onEditNote = { editNote(it) },
                    onTogglePin = { togglePin(it) },
                    onDeleteNote = { deleteTarget = it.id },
                    onRestoreDeleted = { restoreDeletedNote(it) },
                    onDeletePermanently = { permanentDeleteTarget = it.id },
                    onRestoreConflict = { restoreConflictCopy(it) },
                    onDismissConflict = { dismissConflictCopy(it) }
                )
            } else if (selectedTab == "Trash") {
                TrashScreen(
                    deletedNotes = deletedNotes,
                    contentPadding = padding,
                    onRestore = { restoreDeletedNote(it) },
                    onDeletePermanently = { permanentDeleteTarget = it.id }
                )
            } else if (selectedTab == "Profile") {
                ProfilePage(profile = currentProfile, email = activeSession?.email.orEmpty(),
                    canEdit = hasValidatedInternet(context), onEdit = {
                    currentProfile?.let { profile ->
                        profileFullName = profile.fullName
                        profileUsername = profile.username.orEmpty()
                        profileBio = profile.bio
                        profileAvatarUrl = profile.avatarUrl.orEmpty()
                    }
                    showProfileDialog = true
                })
            } else if (selectedTab == "Settings") {
                SettingsScreen(
                    darkMode = darkMode,
                    accountEmail = activeSession?.email.orEmpty(),
                    contentPadding = padding,
                    onDarkModeChange = { enabled ->
                        darkMode = enabled
                        context.getSharedPreferences("teddy_settings", Context.MODE_PRIVATE).edit()
                            .putBoolean("dark_mode", enabled).apply()
                    },
                    onSignOut = {
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
                        editingId = null
                        title = ""
                        content = ""
                        searchQuery = ""
                        showPinnedOnly = false
                        showTrash = false
                        showConflicts = false
                        selectedTab = "Home"
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


