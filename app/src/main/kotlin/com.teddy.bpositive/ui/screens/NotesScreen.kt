package com.teddy.bpositive.ui.screens

import com.teddy.bpositive.model.Note
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
    contentPadding: PaddingValues
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 7.dp)
        ) {
            Text(
                text = "Notes",
                modifier = Modifier.padding(start = 10.dp, top = 24.dp),
                fontSize = 34.sp,
                lineHeight = 40.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(48.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 12.dp),
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
            Spacer(Modifier.height(18.dp))

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
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(notes, key = { it.id }) { note ->
                        NoteListItem(note = note)
                    }
                }
            }
        }

        val plusColor = Color.White
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
                    plusColor,
                    Offset(center.x, size.height * 0.18f),
                    Offset(center.x, size.height * 0.82f),
                    strokeWidth = 3.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    plusColor,
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
        modifier = Modifier
            .size(25.dp)
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
