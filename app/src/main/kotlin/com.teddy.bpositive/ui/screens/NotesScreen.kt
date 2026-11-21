package com.teddy.bpositive.ui.screens

import com.teddy.bpositive.model.Note
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotesScreen(
    notes: List<Note>,
    contentPadding: PaddingValues
) {
    var isHeaderCollapsed by remember { mutableStateOf(false) }
    val scrollConnection = remember {
        object : NestedScrollConnection {
            // Scrolling down collapses the header immediately.
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (available.y < -1f) isHeaderCollapsed = true
                return Offset.Zero
            }

            // Scrolling up expands the header only after the list reaches its top.
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (available.y > 1f) isHeaderCollapsed = false
                return Offset.Zero
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .nestedScroll(scrollConnection)
        ) {
            NotesCollapsingHeader(
                collapsed = isHeaderCollapsed,
                modifier = Modifier.fillMaxWidth()
            )

            if (notes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No notes yet",
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
                    contentPadding = PaddingValues(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(notes, key = { it.id }) { note ->
                        NoteListItem(note = note)
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 18.dp)
                .size(64.dp)
                .background(Color(0xFFFFC928), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(28.dp)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                drawLine(
                    Color.White,
                    Offset(center.x, size.height * 0.18f),
                    Offset(center.x, size.height * 0.82f),
                    strokeWidth = 3.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    Color.White,
                    Offset(size.width * 0.18f, center.y),
                    Offset(size.width * 0.82f, center.y),
                    strokeWidth = 3.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
private fun NotesCollapsingHeader(
    collapsed: Boolean,
    modifier: Modifier = Modifier
) {
    val titleColor = MaterialTheme.colorScheme.onBackground

    AnimatedContent(
        targetState = collapsed,
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .animateContentSize(),
        label = "notes_collapsing_header"
    ) { isCollapsed ->
        if (isCollapsed) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 62.dp)
                    .padding(start = 18.dp, end = 22.dp, top = 8.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Notes",
                    fontSize = 26.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.Normal,
                    color = titleColor
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(22.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NotesActionIcon(kind = "checklist")
                    NotesActionIcon(kind = "search")
                    NotesActionIcon(kind = "settings")
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 22.dp, top = 24.dp, bottom = 18.dp)
            ) {
                Text(
                    text = "Notes",
                    modifier = Modifier.padding(start = 0.dp),
                    fontSize = 34.sp,
                    lineHeight = 40.sp,
                    fontWeight = FontWeight.Normal,
                    color = titleColor
                )
                Spacer(Modifier.height(48.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(22.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NotesActionIcon(kind = "checklist")
                        NotesActionIcon(kind = "search")
                        NotesActionIcon(kind = "settings")
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteListItem(note: Note) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(104.dp)
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 10.dp, vertical = 9.dp)
    ) {
        Text(
            text = note.title.ifBlank { "Untitled" },
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = note.content.ifBlank { " " },
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH)
                .format(Date(note.updatedAt)),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 10.sp,
            lineHeight = 13.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun NotesActionIcon(kind: String) {
    val iconColor = MaterialTheme.colorScheme.onBackground
    Canvas(
        modifier = Modifier.size(25.dp)
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
