package com.teddy.bpositive.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teddy.bpositive.model.Note
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NoteDetailScreen(
    note: Note,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var editing by remember(note.id) { mutableStateOf(false) }
    var title by remember(note.id) { mutableStateOf(TextFieldValue(note.title)) }
    var body by remember(note.id) {
        mutableStateOf(TextFieldValue(note.content, selection = TextRange(note.content.length)))
    }
    var colorHex by remember(note.id) { mutableStateOf(note.colorHex) }
    var activeTool by remember(note.id) { mutableStateOf("none") }
    var locked by remember(note.id) { mutableStateOf(false) }
    var focusTitleAfterEnter by remember(note.id) { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    val bodyFocus = remember(note.id) { FocusRequester() }
    val titleFocus = remember(note.id) { FocusRequester() }
    var bodyLayoutResult by remember(note.id) { mutableStateOf<TextLayoutResult?>(null) }
    var lastBodyTapPosition by remember(note.id) { mutableStateOf(Offset.Zero) }
    val palette = listOf("#FFFFFF", "#FFF3B0", "#FADADD", "#D7F5E8", "#DCEBFF", "#E9DDFB")
    val textColor = MaterialTheme.colorScheme.onBackground
    val subtleColor = MaterialTheme.colorScheme.onSurfaceVariant

    fun beginEditing(atPosition: Int? = null, focusTitle: Boolean = false) {
        if (locked) return
        editing = true
        activeTool = "none"
        focusTitleAfterEnter = focusTitle
        if (!focusTitle && atPosition != null) {
            body = body.copy(selection = TextRange(atPosition.coerceIn(0, body.text.length)))
        }
    }

    LaunchedEffect(editing, focusTitleAfterEnter) {
        if (editing) {
            if (focusTitleAfterEnter) titleFocus.requestFocus() else bodyFocus.requestFocus()
            keyboard?.show()
        }
    }

    fun saveAndStay() {
        onSave(title.text, body.text, colorHex)
        editing = false
        activeTool = "none"
        keyboard?.hide()
    }

    fun wrapSelection(before: String, after: String = before) {
        val start = body.selection.min
        val end = body.selection.max
        val selected = body.text.substring(start, end)
        val replacement = before + selected + after
        val updated = body.text.replaceRange(start, end, replacement)
        body = TextFieldValue(
            updated,
            selection = TextRange(start + before.length, start + before.length + selected.length)
        )
        bodyFocus.requestFocus()
        keyboard?.show()
    }

    fun insertAtCursor(value: String) {
        val start = body.selection.min
        val end = body.selection.max
        val updated = body.text.replaceRange(start, end, value)
        val cursor = start + value.length
        body = TextFieldValue(updated, selection = TextRange(cursor))
        bodyFocus.requestFocus()
        keyboard?.show()
    }

    BackHandler {
        if (editing) {
            editing = false
            activeTool = "none"
            keyboard?.hide()
        } else onBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .imePadding()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = {
                if (editing) {
                    editing = false
                    activeTool = "none"
                    keyboard?.hide()
                } else onBack()
            }) {
                Text("‹", fontSize = 34.sp, color = textColor, lineHeight = 36.sp)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (editing) {
                    TextButton(onClick = { body = body.copy(selection = TextRange(0)) }) {
                        Text("↶", fontSize = 27.sp, color = subtleColor)
                    }
                    TextButton(onClick = { body = body.copy(selection = TextRange(body.text.length)) }) {
                        Text("↷", fontSize = 27.sp, color = subtleColor)
                    }
                }
                TextButton(onClick = { saveAndStay() }) {
                    Text("✓", fontSize = 30.sp, color = Color(0xFFFFC928), fontWeight = FontWeight.Bold)
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 22.dp)
        ) {
            if (editing) {
                BasicTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(titleFocus),
                    textStyle = TextStyle(
                        color = textColor,
                        fontSize = 27.sp,
                        lineHeight = 34.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next
                    ),
                    decorationBox = { inner ->
                        Box {
                            if (title.text.isEmpty()) Text("Title", color = subtleColor, fontSize = 27.sp)
                            inner()
                        }
                    }
                )
            } else {
                Text(
                    title.text.ifBlank { "Untitled" },
                    color = textColor,
                    fontSize = 27.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { beginEditing(focusTitle = true) }
                )
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = SimpleDateFormat("MMMM d h:mm a EEE", Locale.ENGLISH).format(Date(note.updatedAt)) +
                    "  |  ${body.text.length} characters",
                color = subtleColor,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(30.dp))

            if (editing) {
                BasicTextField(
                    value = body,
                    onValueChange = { body = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .focusRequester(bodyFocus),
                    textStyle = TextStyle(
                        color = textColor,
                        fontSize = 17.sp,
                        lineHeight = 27.sp
                    ),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Default
                    ),
                    decorationBox = { inner ->
                        Box(Modifier.fillMaxSize()) {
                            if (body.text.isEmpty()) {
                                Text("Write your note...", color = subtleColor, fontSize = 17.sp)
                            }
                            inner()
                        }
                    }
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .pointerInput(body.text) {
                            awaitEachGesture {
                                val down = awaitFirstDown(pass = PointerEventPass.Initial)
                                lastBodyTapPosition = down.position
                            }
                        }
                        .clickable {
                            val cursorPosition = bodyLayoutResult
                                ?.getOffsetForPosition(lastBodyTapPosition)
                                ?.coerceIn(0, body.text.length)
                                ?: body.text.length
                            beginEditing(atPosition = cursorPosition)
                        }
                ) {
                    Text(
                        body.text,
                        color = textColor,
                        fontSize = 17.sp,
                        lineHeight = 27.sp,
                        onTextLayout = { bodyLayoutResult = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        if (editing) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { activeTool = "lists" }) { Text("☷", fontSize = 22.sp) }
                    TextButton(onClick = { activeTool = "numbered" }) { Text("1☷", fontSize = 18.sp) }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { activeTool = "none"; keyboard?.show() }) { Text("×", fontSize = 25.sp) }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { wrapSelection("**") }) { Text("B", fontWeight = FontWeight.Black, fontSize = 20.sp) }
                    TextButton(onClick = { wrapSelection("_") }) { Text("I", fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, fontSize = 20.sp) }
                    TextButton(onClick = { wrapSelection("<u>", "</u>") }) { Text("U", textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline, fontSize = 20.sp) }
                    TextButton(onClick = { insertAtCursor("\n") }) { Text("↵", fontSize = 20.sp) }
                    TextButton(onClick = { activeTool = "color" }) { Text("✎", fontSize = 20.sp) }
                }
                if (activeTool != "none") {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    when (activeTool) {
                        "lists" -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            TextButton(onClick = { insertAtCursor("• ") }) { Text("Bullet list") }
                            TextButton(onClick = { insertAtCursor("☐ ") }) { Text("Checklist") }
                        }
                        "numbered" -> TextButton(onClick = { insertAtCursor("1. ") }) { Text("Insert numbered item") }
                        "color" -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            palette.forEach { hex ->
                                Box(
                                    Modifier.size(28.dp).background(Color(android.graphics.Color.parseColor(hex)), RoundedCornerShape(50))
                                        .clickable { colorHex = hex }
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("◷", "✦", "▤", "⇩", "▣", "↗").forEachIndexed { index, glyph ->
                    TextButton(onClick = {
                        activeTool = when (index) {
                            0 -> "reminder"
                            1 -> "color"
                            2 -> "delete"
                            3 -> "export"
                            4 -> "lock"
                            else -> "share"
                        }
                    }) { Text(glyph, fontSize = 22.sp, color = textColor) }
                }
            }
            if (activeTool != "none") {
                Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            when (activeTool) {
                                "reminder" -> "Reminder"
                                "color" -> "Note appearance"
                                "delete" -> "Delete note"
                                "export" -> "Export note"
                                "lock" -> if (locked) "Note locked" else "Lock note"
                                else -> "Share note"
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                        if (activeTool == "color") {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 12.dp)) {
                                palette.forEach { hex ->
                                    Box(
                                        Modifier.size(30.dp)
                                            .background(Color(android.graphics.Color.parseColor(hex)), RoundedCornerShape(50))
                                            .clickable { colorHex = hex }
                                    )
                                }
                            }
                        } else if (activeTool == "lock") {
                            TextButton(onClick = { locked = !locked; activeTool = "none" }) {
                                Text(if (locked) "Unlock editing" else "Lock editing")
                            }
                        } else {
                            Text(
                                when (activeTool) {
                                    "reminder" -> "Reminder controls will be connected in a dedicated reminder workflow."
                                    "delete" -> "Use the note list's delete action to move this note to Trash."
                                    "export" -> "Note export will be available in a later workflow."
                                    else -> "Sharing will be available from the share action."
                                },
                                color = subtleColor,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}
