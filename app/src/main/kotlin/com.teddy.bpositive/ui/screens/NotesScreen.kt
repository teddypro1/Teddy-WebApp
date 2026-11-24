package com.teddy.bpositive.ui.screens
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.tooling.preview.Preview
import com.teddy.bpositive.ui.theme.TeddyAppTheme

import com.teddy.bpositive.model.Note
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Checkbox
import androidx.compose.material3.TextButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.*
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotesScreen(
    notes: List<Note>,
    contentPadding: PaddingValues,
    listState: LazyListState,
    collapseProgress: MutableFloatState,
    onNoteClick: (Note) -> Unit,
    onCreateNote: () -> Unit,
    onDeleteSelected: (List<Note>) -> Unit = {},
    onTogglePin: (Note) -> Unit = {},
    onDeleteNote: (Note) -> Unit = {},
    onShareNote: (Note) -> Unit = {},
    onLockNote: (Note) -> Unit = {}
) {
    val context = LocalContext.current
    val settings = remember { context.getSharedPreferences("notes_feature_settings", android.content.Context.MODE_PRIVATE) }
    var compactCards by remember { mutableStateOf(settings.getBoolean("compact_cards", false)) }
    var largeText by remember { mutableStateOf(settings.getBoolean("large_text", false)) }
    var newestFirst by remember { mutableStateOf(settings.getBoolean("newest_first", true)) }
    var showSettings by remember { mutableStateOf(false) }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    var actionNote by remember { mutableStateOf<Note?>(null) }
    var deleteConfirmationNote by remember { mutableStateOf<Note?>(null) }
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val filteredNotes = remember(notes, searchQuery, newestFirst) {
        val matches = if (searchQuery.isBlank()) notes else notes.filter { note ->
            note.title.contains(searchQuery, ignoreCase = true) ||
                note.content.contains(searchQuery, ignoreCase = true)
        }
        if (newestFirst) matches.sortedByDescending { it.updatedAt } else matches.sortedBy { it.updatedAt }
    }

    val density = androidx.compose.ui.platform.LocalDensity.current
    val collapseRangePx = with(density) { 128.dp.toPx() }

    val scrollConnection = remember(collapseRangePx) {
        object : NestedScrollConnection {
            // Consume downward drag for the header first. The note list starts
            // scrolling only after the header has completed its collapse.
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (available.y >= 0f || collapseProgress.floatValue >= 1f) {
                    return Offset.Zero
                }

                // Strict two-stage handoff:
                // 1) While the header is not fully collapsed, it owns the
                //    entire downward scroll delta.
                // 2) Even if this delta reaches progress == 1, do not pass
                //    its leftover distance to LazyColumn. The list may start
                //    scrolling only on the next nested-scroll delta.
                //
                // This prevents the first card from beginning its own scroll
                // during the same gesture frame that finishes the collapse.
                val collapseDistance = -available.y
                collapseProgress.floatValue = (
                    collapseProgress.floatValue + collapseDistance / collapseRangePx
                ).coerceIn(0f, 1f)

                return Offset(0f, available.y)
            }

            // Expand progressively only when the list cannot consume more
            // upward scroll (i.e. the list has reached its top boundary).
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (available.y <= 0f || collapseProgress.floatValue <= 0f) return Offset.Zero

                val availableExpansion = collapseProgress.floatValue * collapseRangePx
                val used = minOf(available.y, availableExpansion)
                collapseProgress.floatValue = (collapseProgress.floatValue - used / collapseRangePx)
                    .coerceIn(0f, 1f)
                return Offset(0f, used)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        if (showSettings) {
            NotesFeatureSettingsScreen(
                compactCards = compactCards,
                largeText = largeText,
                newestFirst = newestFirst,
                onCompactCardsChange = { compactCards = it; settings.edit().putBoolean("compact_cards", it).apply() },
                onLargeTextChange = { largeText = it; settings.edit().putBoolean("large_text", it).apply() },
                onNewestFirstChange = { newestFirst = it; settings.edit().putBoolean("newest_first", it).apply() },
                onBack = { showSettings = false },
                onReset = {
                    compactCards = false; largeText = false; newestFirst = true
                    settings.edit().putBoolean("compact_cards", false).putBoolean("large_text", false).putBoolean("newest_first", true).apply()
                }
            )
        } else Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .nestedScroll(scrollConnection)
        ) {
            if (selectionMode) {
                Row(Modifier.fillMaxWidth().height(62.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { selectionMode = false; selectedIds = emptySet() }) { Text("✕") }
                    Text("${selectedIds.size} selected", modifier = Modifier.weight(1f), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = { selectedIds = if (selectedIds.size == filteredNotes.size) emptySet() else filteredNotes.map { it.id }.toSet() }) { Text(if (selectedIds.size == filteredNotes.size) "None" else "All") }
                    TextButton(enabled = selectedIds.isNotEmpty(), onClick = {
                        val chosen = notes.filter { selectedIds.contains(it.id) }
                        onDeleteSelected(chosen)
                        selectedIds = emptySet()
                        selectionMode = false
                    }) { Text("Delete") }
                }
            } else NotesCollapsingHeader(
                collapseProgress = collapseProgress,
                isSearchActive = isSearchActive,
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onSearchClick = { isSearchActive = true },
                onSearchClose = {
                    isSearchActive = false
                    searchQuery = ""
                },
                onChecklistClick = { selectionMode = true; selectedIds = emptySet() },
                onSettingsClick = { showSettings = true },
                modifier = Modifier.fillMaxWidth()
            )

            if (filteredNotes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isBlank()) "No notes yet" else "No matching notes",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 15.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    // Keep the list end tight; the FAB floats above it and the app
                    // Scaffold already accounts for the global bottom navigation.
                    state = listState,
                    contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    items(filteredNotes, key = { it.id }) { note ->
                        NoteListItem(
                            note = note,
                            compact = compactCards,
                            largeText = largeText,
                            selected = selectedIds.contains(note.id),
                            selectionMode = selectionMode,
                            onSelect = { checked -> selectedIds = if (checked) selectedIds + note.id else selectedIds - note.id },
                            onClick = { if (selectionMode) selectedIds = if (selectedIds.contains(note.id)) selectedIds - note.id else selectedIds + note.id else onNoteClick(note) },
                            onLongPress = { if (!selectionMode) actionNote = note },
                            onTogglePin = { if (!selectionMode && note.isPinned) onTogglePin(note) }
                        )
                    }
                }
            }
        }


        actionNote?.let { note ->
            AlertDialog(
                onDismissRequest = { actionNote = null },
                confirmButton = {},
                title = {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text("Note actions", fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                note.title.ifBlank { "Untitled note" },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        NoteActionGlyph(
                            kind = "close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(20.dp)
                                .clickable { actionNote = null }
                        )
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        if (!note.isMasked) {
                            NoteActionRow(
                                if (note.isPinned) "Unpin note" else "Pin note",
                                if (note.isPinned) "Remove this note from pinned items" else "Keep this note easy to find",
                                "pin"
                            ) {
                                actionNote = null
                                onTogglePin(note)
                            }
                        }
                        NoteActionRow(
                            if (note.isMasked) "Unlock note" else if (note.isLocked) "Hide note" else "Lock note",
                            when {
                                note.isMasked -> "Open with Secure Vault"
                                note.isLocked -> "Hide this protected note"
                                else -> "Encrypt this note with Secure Vault"
                            },
                            if (note.isMasked) "unlock" else "lock"
                        ) {
                            actionNote = null
                            onLockNote(note)
                        }
                        if (!note.isMasked) {
                            NoteActionRow(
                                "Share note",
                                "Send a copy to someone",
                                "share"
                            ) {
                                actionNote = null
                                onShareNote(note)
                            }
                        }
                        NoteActionRow(
                            "Delete note",
                            "Move this note to Trash",
                            "delete",
                            destructive = true
                        ) {
                            actionNote = null
                            deleteConfirmationNote = note
                        }
                    }
                },
                shape = RoundedCornerShape(28.dp),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        }

        deleteConfirmationNote?.let { note ->
            AlertDialog(
                onDismissRequest = { deleteConfirmationNote = null },
                title = { Text("Move note to Trash?") },
                text = { Text("This note will be moved to Trash. You can restore it later.") },
                confirmButton = {
                    TextButton(onClick = {
                        deleteConfirmationNote = null
                        onDeleteNote(note)
                    }) { Text("Move to Trash", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { deleteConfirmationNote = null }) { Text("Cancel") }
                }
            )
        }

        if (!showSettings) Surface(
            onClick = onCreateNote,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = 18.dp)
                .height(60.dp),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shadowElevation = 10.dp,
            tonalElevation = 3.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val plusIconColor = MaterialTheme.colorScheme.onPrimary
                    Canvas(modifier = Modifier.size(18.dp)) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val stroke = 2.4.dp.toPx()
                        drawLine(
                            plusIconColor,
                            Offset(center.x, size.height * 0.12f),
                            Offset(center.x, size.height * 0.88f),
                            stroke,
                            StrokeCap.Round
                        )
                        drawLine(
                            plusIconColor,
                            Offset(size.width * 0.12f, center.y),
                            Offset(size.width * 0.88f, center.y),
                            stroke,
                            StrokeCap.Round
                        )
                    }
                }
                Text(
                    text = "New note",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun NotesCollapsingHeader(
    collapseProgress: State<Float>,
    isSearchActive: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchClick: () -> Unit,
    onSearchClose: () -> Unit,
    onChecklistClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val titleColor = MaterialTheme.colorScheme.onBackground
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            focusRequester.requestFocus()
            keyboardController?.show()
        } else {
            keyboardController?.hide()
        }
    }

    // Read scroll state in the measure/placement/draw phases instead of
    // composition. This prevents NotesScreen and its LazyColumn from being
    // recomposed for every tiny nested-scroll delta.
    Box(
        modifier = modifier
            .fillMaxWidth()
            .layout { measurable, constraints ->
                val progress = collapseProgress.value.coerceIn(0f, 1f)
                val targetHeight = (190f + (62f - 190f) * progress)
                    .dp
                    .roundToPx()
                    .coerceIn(constraints.minHeight, constraints.maxHeight)
                val placeable = measurable.measure(
                    constraints.copy(
                        minHeight = targetHeight,
                        maxHeight = targetHeight
                    )
                )
                layout(placeable.width, targetHeight) {
                    placeable.place(0, 0)
                }
            }
            .clipToBounds()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (!isSearchActive) {
            val titleMeasurer = rememberTextMeasurer()
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
            ) {
                val progress = collapseProgress.value.coerceIn(0f, 1f)
                val fontSize = (34f + (26f - 34f) * progress).sp
                val titleTop = (24f + (12f - 24f) * progress).dp.toPx()
                val titleLayout = titleMeasurer.measure(
                    text = AnnotatedString("Notes"),
                    style = TextStyle(
                        color = titleColor,
                        fontSize = fontSize,
                        lineHeight = (40f + (32f - 40f) * progress).sp,
                        fontWeight = FontWeight.Normal
                    )
                )
                drawText(textLayoutResult = titleLayout, topLeft = Offset(10.dp.toPx(), titleTop))
            }
        } else {
            BasicTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 10.dp, top = 10.dp, end = 64.dp)
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 9.dp)
                    .focusRequester(focusRequester),
                singleLine = true,
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                decorationBox = { innerTextField ->
                    Box {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search notes...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 15.sp
                            )
                        }
                        innerTextField()
                    }
                }
            )
            NotesActionIcon(
                kind = "close",
                onClick = onSearchClose,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 17.dp, end = 22.dp)
            )
        }

        if (!isSearchActive) Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 22.dp)
                .offset {
                    val progress = collapseProgress.value.coerceIn(0f, 1f)
                    IntOffset(
                        x = 0,
                        y = (132f + (18f - 132f) * progress).dp.roundToPx()
                    )
                },
            horizontalArrangement = Arrangement.spacedBy(22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NotesActionIcon(kind = "checklist", onClick = onChecklistClick)
            NotesActionIcon(kind = "search", onClick = onSearchClick)
            NotesActionIcon(kind = "settings", onClick = onSettingsClick)
        }
    }
}

@Composable
private fun NoteListItem(
    note: Note,
    compact: Boolean,
    largeText: Boolean,
    selected: Boolean,
    selectionMode: Boolean,
    onSelect: (Boolean) -> Unit,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onTogglePin: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (selectionMode) Checkbox(checked = selected, onCheckedChange = onSelect)
        Column(
        modifier = Modifier
            .weight(1f)
            .height(if (compact) 86.dp else 104.dp)
            .pointerInput(note.id, selectionMode) {
                detectTapGestures(onLongPress = { onLongPress() }, onTap = { onClick() })
            }
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 10.dp, vertical = 9.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = note.title.ifBlank { "Untitled" },
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = if (largeText) 18.sp else 16.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (note.isPinned) {
                Surface(
                    onClick = onTogglePin,
                    modifier = Modifier.size(32.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    tonalElevation = 2.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        NoteActionGlyph(
                            kind = "pin",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = note.content.ifBlank { " " },
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = if (largeText) 14.sp else 12.sp,
            lineHeight = 16.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH)
                .format(Date(note.updatedAt)),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = if (largeText) 11.sp else 10.sp,
            lineHeight = 13.sp,
            maxLines = 1
        )
    }
    }
}


@Composable
private fun NoteActionRow(
    title: String,
    subtitle: String,
    icon: String,
    destructive: Boolean = false,
    onClick: () -> Unit
) {
    val accent = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val iconBackground = if (destructive) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val iconForeground = if (destructive) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }

    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 1.dp
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(13.dp),
                color = iconBackground,
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    NoteActionGlyph(
                        kind = icon,
                        tint = iconForeground,
                        modifier = Modifier.size(21.dp)
                    )
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    title,
                    color = if (destructive) accent else MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}


@Composable
private fun NoteActionGlyph(
    kind: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val stroke = 2.dp.toPx()
        val left = size.width * 0.18f
        val right = size.width * 0.82f
        val top = size.height * 0.12f
        val bottom = size.height * 0.88f
        when (kind) {
            "bookmark" -> {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(left, top)
                    lineTo(right, top)
                    lineTo(right, bottom)
                    lineTo(size.width / 2f, size.height * 0.68f)
                    lineTo(left, bottom)
                    close()
                }
                drawPath(path, tint, style = Stroke(width = stroke, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
            }
            "pin" -> {
                // A compact angled pushpin: rounded head, collar and tapered needle.
                rotate(degrees = -38f, pivot = Offset(size.width * 0.52f, size.height * 0.50f)) {
                    val headCenter = Offset(size.width * 0.50f, size.height * 0.22f)
                    drawCircle(
                        color = tint,
                        radius = size.width * 0.19f,
                        center = headCenter
                    )
                    drawCircle(
                        color = tint.copy(alpha = 0.34f),
                        radius = size.width * 0.075f,
                        center = Offset(headCenter.x - size.width * 0.045f, headCenter.y - size.height * 0.045f)
                    )
                    drawRoundRect(
                        color = tint,
                        topLeft = Offset(size.width * 0.40f, size.height * 0.37f),
                        size = Size(size.width * 0.20f, size.height * 0.12f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width * 0.05f)
                    )
                    val needle = androidx.compose.ui.graphics.Path().apply {
                        moveTo(size.width * 0.44f, size.height * 0.48f)
                        lineTo(size.width * 0.56f, size.height * 0.48f)
                        lineTo(size.width * 0.52f, size.height * 0.94f)
                        close()
                    }
                    drawPath(needle, tint)
                }
            }
            "lock" -> {
                drawArc(tint, 180f, 180f, false,
                    topLeft = Offset(size.width * 0.27f, size.height * 0.08f),
                    size = Size(size.width * 0.46f, size.height * 0.55f),
                    style = Stroke(width = stroke, cap = StrokeCap.Round))
                drawRoundRect(tint,
                    topLeft = Offset(size.width * 0.16f, size.height * 0.42f),
                    size = Size(size.width * 0.68f, size.height * 0.48f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()),
                    style = Stroke(width = stroke))
                drawCircle(tint, radius = stroke * 0.8f, center = Offset(size.width / 2f, size.height * 0.61f))
                drawLine(tint, Offset(size.width / 2f, size.height * 0.64f), Offset(size.width / 2f, size.height * 0.76f), stroke, StrokeCap.Round)
            }
            "share" -> {
                val a = Offset(size.width * 0.28f, size.height * 0.50f)
                val b = Offset(size.width * 0.73f, size.height * 0.25f)
                val c = Offset(size.width * 0.73f, size.height * 0.75f)
                drawLine(tint, a, b, stroke, StrokeCap.Round)
                drawLine(tint, a, c, stroke, StrokeCap.Round)
                drawCircle(tint, stroke * 2f, a, style = Stroke(stroke))
                drawCircle(tint, stroke * 2f, b, style = Stroke(stroke))
                drawCircle(tint, stroke * 2f, c, style = Stroke(stroke))
            }
            "delete" -> {
                drawLine(tint, Offset(size.width * 0.12f, size.height * 0.25f), Offset(size.width * 0.88f, size.height * 0.25f), stroke, StrokeCap.Round)
                drawLine(tint, Offset(size.width * 0.38f, size.height * 0.13f), Offset(size.width * 0.62f, size.height * 0.13f), stroke, StrokeCap.Round)
                drawRoundRect(tint, Offset(size.width * 0.23f, size.height * 0.30f), Size(size.width * 0.54f, size.height * 0.58f), androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()), style = Stroke(stroke))
                drawLine(tint, Offset(size.width * 0.43f, size.height * 0.42f), Offset(size.width * 0.43f, size.height * 0.75f), stroke, StrokeCap.Round)
                drawLine(tint, Offset(size.width * 0.57f, size.height * 0.42f), Offset(size.width * 0.57f, size.height * 0.75f), stroke, StrokeCap.Round)
            }
            "close" -> {
                drawLine(tint, Offset(size.width * 0.22f, size.height * 0.22f), Offset(size.width * 0.78f, size.height * 0.78f), stroke, StrokeCap.Round)
                drawLine(tint, Offset(size.width * 0.78f, size.height * 0.22f), Offset(size.width * 0.22f, size.height * 0.78f), stroke, StrokeCap.Round)
            }
        }
    }
}

@Composable
private fun NotesFeatureSettingsScreen(
    compactCards: Boolean,
    largeText: Boolean,
    newestFirst: Boolean,
    onCompactCardsChange: (Boolean) -> Unit,
    onLargeTextChange: (Boolean) -> Unit,
    onNewestFirstChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onReset: () -> Unit
) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(horizontal = 18.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("‹  Back") }
            Text("Notes Settings", fontSize = 23.sp, fontWeight = FontWeight.SemiBold)
        }
        Text("Customize how your notes look and behave.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        androidx.compose.material3.Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Display", fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) { Text("Compact cards"); Text("Show notes with less vertical spacing", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Checkbox(checked = compactCards, onCheckedChange = onCompactCardsChange)
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) { Text("Larger text"); Text("Increase note title and preview size", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Checkbox(checked = largeText, onCheckedChange = onLargeTextChange)
                }
            }
        }
        androidx.compose.material3.Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Sort notes", fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) { RadioButton(selected = newestFirst, onClick = { onNewestFirstChange(true) }); Text("Newest first") }
                Row(verticalAlignment = Alignment.CenterVertically) { RadioButton(selected = !newestFirst, onClick = { onNewestFirstChange(false) }); Text("Oldest first") }
            }
        }
        TextButton(onClick = onReset, modifier = Modifier.fillMaxWidth()) { Text("Reset to default") }
    }
}

@Composable
private fun NotesActionIcon(
    kind: String,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val iconColor = MaterialTheme.colorScheme.onBackground
    Canvas(
        modifier = modifier.size(25.dp).clickable(onClick = onClick)
    ) {
        val stroke = 2.2.dp.toPx()
        when (kind) {
            "checklist" -> {
                drawRoundRect(
                    color = iconColor,
                    topLeft = Offset(size.width * .12f, size.height * .12f),
                    size = Size(size.width * .76f, size.height * .76f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
                    style = Stroke(stroke)
                )
                drawLine(iconColor, Offset(size.width * .28f, size.height * .49f), Offset(size.width * .43f, size.height * .64f), stroke, StrokeCap.Round)
                drawLine(iconColor, Offset(size.width * .43f, size.height * .64f), Offset(size.width * .73f, size.height * .34f), stroke, StrokeCap.Round)
            }
            "search" -> {
                drawCircle(iconColor, radius = size.width * .31f, center = Offset(size.width * .42f, size.height * .40f), style = Stroke(stroke))
                drawLine(iconColor, Offset(size.width * .64f, size.height * .64f), Offset(size.width * .88f, size.height * .88f), stroke, StrokeCap.Round)
            }
            "close" -> {
                drawLine(iconColor, Offset(size.width * .25f, size.height * .25f), Offset(size.width * .75f, size.height * .75f), stroke, StrokeCap.Round)
                drawLine(iconColor, Offset(size.width * .75f, size.height * .25f), Offset(size.width * .25f, size.height * .75f), stroke, StrokeCap.Round)
            }
            else -> {
                drawCircle(iconColor, radius = size.width * .19f, style = Stroke(stroke))
                for (i in 0 until 8) {
                    val angle = i * (Math.PI / 4.0)
                    val inner = size.width * .30f
                    val outer = size.width * .45f
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    drawLine(
                        iconColor,
                        Offset(cx + kotlin.math.cos(angle).toFloat() * inner, cy + kotlin.math.sin(angle).toFloat() * inner),
                        Offset(cx + kotlin.math.cos(angle).toFloat() * outer, cy + kotlin.math.sin(angle).toFloat() * outer),
                        stroke,
                        StrokeCap.Round
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Notes Screen")
@Composable
private fun NotesScreenPreview() {
    TeddyAppTheme(darkMode = false) {
        NotesScreen(
            notes = listOf(
                Note(1L, "Project ideas", "A sample note used to inspect the notes list card.", true, 1730000000000L, 1730000000000L),
                Note(2L, "Reading list", "Books and articles to read later.", false, 1730000000000L, 1730000000000L),
                Note(3L, "Daily plan", "Tasks and reminders for the day.", false, 1730000000000L, 1730000000000L)
            ),
            contentPadding = PaddingValues(0.dp),
            listState = rememberLazyListState(),
            collapseProgress = remember { mutableFloatStateOf(0f) },
            onNoteClick = {},
            onCreateNote = {}
        )
    }
}
