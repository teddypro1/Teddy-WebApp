package com.teddy.bpositive

import android.os.Bundle
import android.content.Context
import android.widget.Toast
import com.teddy.bpositive.data.SupabaseClient
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.KeyboardCapitalization
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Note(
    val id: Long,
    val title: String,
    val content: String,
    val isPinned: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Temporary Supabase connectivity test
        SupabaseClient.testConnectionAsync { result ->
            Toast.makeText(this, result, Toast.LENGTH_LONG).show()
        }

        setContent {
            TeddyNotesApp()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeddyNotesApp() {

    val context = androidx.compose.ui.platform.LocalContext.current

    val notes = remember {
        mutableStateListOf<Note>().apply {
            addAll(loadNotes(context))
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
        saveNotes(context, notes)
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

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {

                DashboardHeader()

                Spacer(Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    StatsCard(
                        modifier = Modifier.weight(1f),
                        emoji = "▤",
                        label = "Total Notes",
                        count = totalNotes
                    )

                    StatsCard(
                        modifier = Modifier.weight(1f),
                        emoji = "📌",
                        label = "Pinned Notes",
                        count = pinnedNotes
                    )
                }

                Spacer(Modifier.height(25.dp))

                if (showEditor) {

                    NoteEditor(
                        title = title,
                        content = content,
                        isEditing = editingId != null,

                        onTitleChange = {
                            title = it
                        },

                        onContentChange = {
                            if (it.length <= 3000) {
                                content = it
                            }
                        },

                        onSave = {
                            saveNote()
                        },

                        onCancel = {
                            clearEditor()
                        }
                    )

                    Spacer(Modifier.height(25.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Column {
                        Text(
                            "YOUR COLLECTION",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )

                        Spacer(Modifier.height(5.dp))

                        Text(
                            if (showPinnedOnly) {
                                "Pinned Notes ($pinnedNotes)"
                            } else {
                                "All Notes ($totalNotes)"
                            },
                            fontSize = 23.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = {
                            if (!showEditor) {
                                editingId = null
                                title = ""
                                content = ""
                                showEditor = true
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(
                            horizontal = 14.dp,
                            vertical = 10.dp
                        )
                    ) {
                        Text("+ New Note")
                    }
                }

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text("Search your notes...")
                    },
                    leadingIcon = {
                        Text("⌕", fontSize = 25.sp)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        FilterChip(
                            selected = !showPinnedOnly,
                            onClick = {
                                showPinnedOnly = false
                            },
                            label = {
                                Text("All Notes")
                            }
                        )

                        FilterChip(
                            selected = showPinnedOnly,
                            onClick = {
                                showPinnedOnly = true
                            },
                            label = {
                                Text("📌 Pinned")
                            }
                        )
                    }

                    TextButton(
                        onClick = {
                            sortNewest = !sortNewest
                        }
                    ) {
                        Text(
                            if (sortNewest) "Newest ↓" else "Oldest ↑",
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                if (filteredNotes.isEmpty()) {

                    EmptyNotes()

                } else {

                    filteredNotes.forEach { note ->

                        NoteCard(
                            note = note,

                            onEdit = {
                                editNote(note)
                            },

                            onPin = {
                                togglePin(note)
                            },

                            onDelete = {
                                deleteTarget = note.id
                            }
                        )

                        Spacer(Modifier.height(12.dp))
                    }
                }

                Spacer(Modifier.height(25.dp))

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                Spacer(Modifier.height(15.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        "Teddy WebApp",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )

                    Spacer(Modifier.height(5.dp))

                    Text(
                        "Your thoughts, organized beautifully.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        "© 2026 Teddy WebApp. All rights reserved.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }

                Spacer(Modifier.height(15.dp))
            }
        }
    }

    if (deleteTarget != null) {

        DeleteConfirmationDialog(

            onConfirm = {

                notes.removeAll {
                    it.id == deleteTarget
                }

                saveChanges()
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
fun DashboardHeader() {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF263F9D),
                        Color(0xFF6485F0)
                    )
                ),
                shape = RoundedCornerShape(25.dp)
            )
            .padding(25.dp)
    ) {

        Text(
            "YOUR PERSONAL SPACE",
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )

        Spacer(Modifier.height(15.dp))

        Text(
            "Organize your thoughts.",
            color = Color.White,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(12.dp))

        Text(
            "Organize your thoughts and keep everything in one place.",
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 14.sp,
            lineHeight = 21.sp
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "✦",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 32.sp
        )
    }
}

@Composable
fun StatsCard(
    modifier: Modifier = Modifier,
    emoji: String,
    label: String,
    count: Int
) {

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {

        Column(
            modifier = Modifier.padding(15.dp)
        ) {

            Text(
                emoji,
                fontSize = 23.sp
            )

            Spacer(Modifier.height(9.dp))

            Text(
                label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )

            Spacer(Modifier.height(4.dp))

            Text(
                count.toString(),
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun NoteEditor(
    title: String,
    content: String,
    isEditing: Boolean,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(23.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp
        )
    ) {

        Column(
            modifier = Modifier.padding(20.dp)
        ) {

            Text(
                "NOTE EDITOR",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    if (isEditing) "Edit Note" else "Create a Note",
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "✎",
                    fontSize = 27.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(22.dp))

            Text(
                "Note Title",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text("Give your note a title...")
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Text(
                    "Note Content",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )

                Text(
                    "${content.length} / 3000",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = content,
                onValueChange = onContentChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp),
                placeholder = {
                    Text("Write your thoughts here...")
                },
                minLines = 5,
                maxLines = 10,
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(Modifier.height(18.dp))

            Button(
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {

                Text(
                    if (isEditing) "Update Note" else "+ Add Note",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Cancel")
            }
        }
    }
}

@Composable
fun EmptyNotes() {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                "📝",
                fontSize = 38.sp
            )

            Spacer(Modifier.height(12.dp))

            Text(
                "No notes available yet.",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(5.dp))

            Text(
                "Create your first note to get started!",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
@Composable
fun NoteCard(
    note: Note,
    onEdit: () -> Unit,
    onPin: () -> Unit,
    onDelete: () -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {

        Column(
            modifier = Modifier.padding(17.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    note.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                TextButton(
                    onClick = onPin
                ) {
                    Text(
                        if (note.isPinned) "📌" else "📍",
                        fontSize = 19.sp
                    )
                }
            }

            Spacer(Modifier.height(5.dp))

            Text(
                note.content,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 21.sp,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(12.dp))

            Text(
                "Updated: ${
                    SimpleDateFormat(
                        "dd MMM yyyy, hh:mm a",
                        Locale.getDefault()
                    ).format(Date(note.updatedAt))
                }",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {

                TextButton(
                    onClick = onEdit
                ) {
                    Text("✎ Edit")
                }

                TextButton(
                    onClick = onDelete
                ) {
                    Text(
                        "🗑 Delete",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun DeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {

    AlertDialog(

        onDismissRequest = onCancel,

        title = {
            Text(
                "Delete Note?",
                fontWeight = FontWeight.Bold
            )
        },

        text = {
            Text(
                "Are you sure you want to delete this note? This action cannot be undone."
            )
        },

        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
                Text(
                    "Delete",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
        },

        dismissButton = {
            TextButton(
                onClick = onCancel
            ) {
                Text("Cancel")
            }
        }
    )
}
fun saveNotes(
    context: Context,
    notes: List<Note>
) {

    try {

        val jsonArray = JSONArray()

        notes.forEach { note ->

            val jsonObject = JSONObject().apply {

                put("id", note.id)
                put("title", note.title)
                put("content", note.content)
                put("isPinned", note.isPinned)
                put("createdAt", note.createdAt)
                put("updatedAt", note.updatedAt)
            }

            jsonArray.put(jsonObject)
        }

        context.getSharedPreferences(
            "teddy_notes",
            Context.MODE_PRIVATE
        ).edit()
            .putString("notes_data", jsonArray.toString())
            .apply()

    } catch (e: Exception) {

        Toast.makeText(
            context,
            "Unable to save notes.",
            Toast.LENGTH_SHORT
        ).show()
    }
}

fun loadNotes(
    context: Context
): List<Note> {

    return try {

        val preferences = context.getSharedPreferences(
            "teddy_notes",
            Context.MODE_PRIVATE
        )

        val savedData = preferences.getString(
            "notes_data",
            "[]"
        ) ?: "[]"

        val jsonArray = JSONArray(savedData)

        val loadedNotes = mutableListOf<Note>()

        for (i in 0 until jsonArray.length()) {

            val jsonObject = jsonArray.getJSONObject(i)

            val note = Note(

                id = jsonObject.optLong("id"),

                title = jsonObject.optString("title"),

                content = jsonObject.optString("content"),

                isPinned = jsonObject.optBoolean(
                    "isPinned",
                    false
                ),

                createdAt = jsonObject.optLong("createdAt"),

                updatedAt = jsonObject.optLong("updatedAt")
            )

            loadedNotes.add(note)
        }

        loadedNotes

    } catch (e: Exception) {

        emptyList()
    }
}
