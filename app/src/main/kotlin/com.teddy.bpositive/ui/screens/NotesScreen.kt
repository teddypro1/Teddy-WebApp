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
    onDeleteSelected: (List<Note>) -> Unit = {}
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
                            onLongPress = { if (!selectionMode) actionNote = note }
                        )
                    }
                }
            }
        }


        actionNote?.let { note ->
            AlertDialog(
                onDismissRequest = { actionNote = null },
                title = { Text(note.title.ifBlank { "Untitled note" }, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        NoteActionRow("Pin", "Keep this note at the top", "📌") { actionNote = null }
                        NoteActionRow("Lock / Encryption", "Protect this note with a password", "🔒") { actionNote = null }
                        NoteActionRow("Share", "Share this note with others", "↗") { actionNote = null }
                        NoteActionRow("Delete", "Move this note to Trash", "⌫", MaterialTheme.colorScheme.error) { actionNote = null }
                    }
                },
                confirmButton = { TextButton(onClick = { actionNote = null }) { Text("Close") } },
                shape = RoundedCornerShape(24.dp)
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
    onLongPress: () -> Unit
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
        Text(
            text = note.title.ifBlank { "Untitled" },
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = if (largeText) 18.sp else 16.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
    symbol: String,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Surface(onClick = onClick, color = Color.Transparent, shape = RoundedCornerShape(12.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(symbol, fontSize = 20.sp, color = titleColor)
            Column(Modifier.weight(1f)) {
                Text(title, color = titleColor, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
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
