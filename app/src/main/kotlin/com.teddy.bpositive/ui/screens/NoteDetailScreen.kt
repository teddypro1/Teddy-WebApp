package com.teddy.bpositive.ui.screens
import androidx.compose.ui.tooling.preview.Preview
import com.teddy.bpositive.ui.theme.TeddyAppTheme

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalDensity
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
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalMaterial3Api::class)
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
    val density = LocalDensity.current
    val bodyFocus = remember(note.id) { FocusRequester() }
    val titleFocus = remember(note.id) { FocusRequester() }
    var bodyLayoutResult by remember(note.id) { mutableStateOf<TextLayoutResult?>(null) }
    val bodyBringIntoViewRequester = remember(note.id) { BringIntoViewRequester() }
    val noteScrollState = rememberScrollState()
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
            if (focusTitleAfterEnter) {
                titleFocus.requestFocus()
            } else {
                bodyFocus.requestFocus()
                keyboard?.show()

                // Wait until the IME finishes resizing the reader viewport.
                // A bring-into-view request made during the keyboard animation
                // can be calculated against the old viewport and leave a
                // bottom-of-note cursor hidden behind the keyboard.
                snapshotFlow {
                    WindowInsets.ime.getBottom(density) to
                        WindowInsets.imeAnimationTarget.getBottom(density)
                }.first { (current, target) ->
                    target > 0 && current == target
                }

                // Let the final inset-driven layout pass complete before
                // calculating the cursor rectangle and scrolling to it.
                withFrameNanos { }
                withFrameNanos { }
                val cursorRect = bodyLayoutResult?.getCursorRect(
                    body.selection.start.coerceIn(0, body.text.length)
                )
                if (cursorRect != null) {
                    bodyBringIntoViewRequester.bringIntoView(cursorRect)
                }
            }
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

    CompositionLocalProvider(
        LocalRippleConfiguration provides null
    ) {
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
                .padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        if (editing) {
                            editing = false
                            activeTool = "none"
                            keyboard?.hide()
                        } else onBack()
                    },
                contentAlignment = Alignment.Center
            ) {
                NoteToolbarIcon("back", textColor)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (editing) {
                    Box(
                        Modifier.size(24.dp).clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { body = body.copy(selection = TextRange(0)) },
                        contentAlignment = Alignment.Center
                    ) { NoteToolbarIcon("undo", subtleColor) }
                    Box(
                        Modifier.size(24.dp).clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { body = body.copy(selection = TextRange(body.text.length)) },
                        contentAlignment = Alignment.Center
                    ) { NoteToolbarIcon("redo", subtleColor) }
                    Box(
                        Modifier.size(24.dp).clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { saveAndStay() },
                        contentAlignment = Alignment.Center
                    ) { NoteToolbarIcon("check", Color(0xFFFFC928)) }
                } else {
                    Box(
                        Modifier.size(24.dp).clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { saveAndStay() },
                        contentAlignment = Alignment.Center
                    ) { NoteToolbarIcon("bookmark", textColor) }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(noteScrollState)
                .clickable(
                    enabled = !editing && !locked,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    beginEditing(atPosition = body.text.length)
                }
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

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 240.dp)
            ) {
                BasicTextField(
                    value = body,
                    onValueChange = { body = it },
                    readOnly = !editing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 240.dp)
                        .bringIntoViewRequester(bodyBringIntoViewRequester)
                        .pointerInput(editing, locked, body.text) {
                            awaitEachGesture {
                                val down = awaitFirstDown(
                                    requireUnconsumed = false,
                                    pass = PointerEventPass.Initial
                                )
                                val up = waitForUpOrCancellation(
                                    pass = PointerEventPass.Initial
                                )
                                if (up != null && !editing && !locked) {
                                    val cursorPosition = bodyLayoutResult
                                        ?.getOffsetForPosition(down.position)
                                        ?.coerceIn(0, body.text.length)
                                        ?: body.text.length
                                    beginEditing(atPosition = cursorPosition)
                                }
                            }
                        }
                        .focusRequester(bodyFocus),
                    textStyle = TextStyle(
                        color = textColor,
                        fontSize = 17.sp,
                        lineHeight = 27.sp
                    ),
                    onTextLayout = { bodyLayoutResult = it },
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(
                        if (editing) MaterialTheme.colorScheme.primary else Color.Transparent
                    ),
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
}

@Composable
private fun NoteToolbarIcon(
    type: String,
    tint: Color
) {
    Canvas(Modifier.size(24.dp)) {
        val sx = size.width / 24f
        val sy = size.height / 24f
        fun p(x: Float, y: Float) = Offset(x * sx, y * sy)
        val stroke = 2.1f * sx
        val style = androidx.compose.ui.graphics.drawscope.Stroke(
            width = stroke,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
            join = androidx.compose.ui.graphics.StrokeJoin.Round
        )
        when (type) {
            "back" -> {
                drawLine(tint, p(19f, 12f), p(5f, 12f), stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                drawLine(tint, p(5f, 12f), p(12f, 5f), stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                drawLine(tint, p(5f, 12f), p(12f, 19f), stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            }
            "bookmark" -> {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(7f * sx, 3.5f * sy)
                    lineTo(17f * sx, 3.5f * sy)
                    quadraticTo(19f * sx, 3.5f * sy, 19f * sx, 5.5f * sy)
                    lineTo(19f * sx, 21f * sy)
                    lineTo(12f * sx, 16f * sy)
                    lineTo(5f * sx, 21f * sy)
                    lineTo(5f * sx, 5.5f * sy)
                    quadraticTo(5f * sx, 3.5f * sy, 7f * sx, 3.5f * sy)
                    close()
                }
                drawPath(path, tint, style = style)
            }
            "check" -> {
                drawLine(tint, p(4f, 12f), p(10f, 18f), stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                drawLine(tint, p(10f, 18f), p(21f, 6f), stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            }
            "undo", "redo" -> {
                val path = androidx.compose.ui.graphics.Path()
                if (type == "undo") {
                    path.moveTo(20f * sx, 18f * sy)
                    path.cubicTo(20f * sx, 9f * sy, 13f * sx, 7f * sy, 5f * sx, 12f * sy)
                    drawLine(tint, p(5f, 12f), p(10f, 7f), stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    drawLine(tint, p(5f, 12f), p(10f, 17f), stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                } else {
                    path.moveTo(4f * sx, 18f * sy)
                    path.cubicTo(4f * sx, 9f * sy, 11f * sx, 7f * sy, 19f * sx, 12f * sy)
                    drawLine(tint, p(19f, 12f), p(14f, 7f), stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    drawLine(tint, p(19f, 12f), p(14f, 17f), stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                }
                drawPath(path, tint, style = style)
            }
        }
    }
}

@Preview(showBackground = true, name = "Note Detail")
@Composable
private fun NoteDetailScreenPreview() {
    TeddyAppTheme(darkMode = false) {
        NoteDetailScreen(
            note = Note(
                id = 1L, title = "A note preview",
                content = "This is sample note content. Use this preview to inspect reading layout, typography, spacing, and toolbar placement.",
                isPinned = false, createdAt = 1730000000000L, updatedAt = 1730000000000L
            ),
            contentPadding = PaddingValues(0.dp),
            onBack = {}, onSave = { _, _, _ -> }
        )
    }
}
