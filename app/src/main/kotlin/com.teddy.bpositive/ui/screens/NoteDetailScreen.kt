package com.teddy.bpositive.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teddy.bpositive.model.Note

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    note: Note,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var editing by remember(note.id) { mutableStateOf(false) }
    var title by remember(note.id) { mutableStateOf(note.title) }
    var content by remember(note.id) { mutableStateOf(note.content) }
    var colorHex by remember(note.id) { mutableStateOf(note.colorHex) }
    val palette = listOf("#FFFFFF", "#FFF3B0", "#FADADD", "#D7F5E8", "#DCEBFF", "#E9DDFB")
    val bg = try { Color(android.graphics.Color.parseColor(colorHex)) } catch (_: Exception) { MaterialTheme.colorScheme.surface }
    // Note surfaces use light palette colors even in dark mode, so their text must keep sufficient contrast.
    val noteTextColor = if (bg.luminance() > 0.5f) Color(0xFF202124) else Color(0xFFF5F5F5)

    BackHandler(onBack = onBack)
    Column(modifier = Modifier.fillMaxSize().padding(contentPadding).background(bg)) {
        CompositionLocalProvider(LocalContentColor provides noteTextColor) {
        TopAppBar(
            title = { Text(if (editing) "Edit note" else "Note") },
            navigationIcon = { TextButton(onClick = onBack) { Text("← Back") } },
            actions = {
                if (editing) TextButton(onClick = { onSave(title, content, colorHex) }) { Text("Save") }
                else TextButton(onClick = { editing = true }) { Text("Edit") }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = bg)
        )
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 12.dp)) {
            if (editing) {
                OutlinedTextField(
                    value = title, onValueChange = { title = it }, modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    placeholder = { Text("Title") }
                )
                Spacer(Modifier.height(12.dp))
                Text("Formatting shortcuts insert simple Markdown markers.", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("**" to "Bold", "_" to "Italic", "# " to "Heading", "• " to "Bullet").forEach { (marker, label) ->
                        AssistChip(onClick = {
                            content += when (marker) { "# " -> "\n# Heading"; "• " -> "\n• List item"; "**" -> " **bold text**"; else -> " _italic text_" }
                        }, label = { Text(label) })
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Note color", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    palette.forEach { hex ->
                        Surface(
                            modifier = Modifier.size(34.dp).clickable { colorHex = hex },
                            shape = RoundedCornerShape(50),
                            color = Color(android.graphics.Color.parseColor(hex)),
                            border = if (colorHex == hex) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                        ) {}
                    }
                }
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = content, onValueChange = { content = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 320.dp),
                    placeholder = { Text("Write your note...") }, minLines = 12
                )
                Spacer(Modifier.height(14.dp))
                Button(onClick = { onSave(title, content, colorHex) }, modifier = Modifier.fillMaxWidth()) { Text("Save changes") }
            } else {
                Text(
                    title.ifBlank { "Untitled" },
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.fillMaxWidth().clickable { editing = true }
                )
                Spacer(Modifier.height(18.dp))
                Text(
                    content,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 28.sp),
                    modifier = Modifier.fillMaxWidth().clickable { editing = true }
                )
                Spacer(Modifier.height(20.dp))
                Text("Tap the note text to edit", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        }
    }
}
