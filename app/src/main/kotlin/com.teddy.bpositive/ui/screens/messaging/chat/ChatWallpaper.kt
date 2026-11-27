package com.teddy.bpositive.ui.screens.messaging.chat

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

private val ChatWallpaperBase = Color(0xFFF7F2EA)
private val ChatWallpaperDoodle = Color(0xFFE7DED2)

/**
 * Subtle WhatsApp-inspired chat wallpaper made entirely with Compose Canvas.
 * The low-contrast doodle pattern stays behind chat content and needs no image asset.
 */
@Composable
fun ChatWallpaper(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(ChatWallpaperBase)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stepX = 104.dp.toPx()
            val stepY = 96.dp.toPx()
            val stroke = 1.25.dp.toPx()
            var row = 0
            var y = 18.dp.toPx()

            while (y < size.height + stepY) {
                var x = if (row % 2 == 0) 14.dp.toPx() else 62.dp.toPx()
                while (x < size.width + stepX) {
                    val center = Offset(x, y)
                    when ((row + (x / stepX).toInt()) % 5) {
                        0 -> {
                            drawCircle(
                                color = ChatWallpaperDoodle,
                                radius = 13.dp.toPx(),
                                center = center,
                                style = Stroke(stroke)
                            )
                            drawCircle(
                                color = ChatWallpaperDoodle,
                                radius = 3.dp.toPx(),
                                center = center
                            )
                        }
                        1 -> {
                            drawRoundRect(
                                color = ChatWallpaperDoodle,
                                topLeft = Offset(x - 15.dp.toPx(), y - 10.dp.toPx()),
                                size = Size(30.dp.toPx(), 21.dp.toPx()),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()),
                                style = Stroke(stroke)
                            )
                            drawLine(
                                color = ChatWallpaperDoodle,
                                start = Offset(x - 5.dp.toPx(), y + 11.dp.toPx()),
                                end = Offset(x - 10.dp.toPx(), y + 17.dp.toPx()),
                                strokeWidth = stroke
                            )
                        }
                        2 -> {
                            drawCircle(
                                color = ChatWallpaperDoodle,
                                radius = 10.dp.toPx(),
                                center = center,
                                style = Stroke(stroke)
                            )
                            drawLine(
                                color = ChatWallpaperDoodle,
                                start = Offset(x - 5.dp.toPx(), y),
                                end = Offset(x + 5.dp.toPx(), y),
                                strokeWidth = stroke
                            )
                            drawLine(
                                color = ChatWallpaperDoodle,
                                start = Offset(x, y - 5.dp.toPx()),
                                end = Offset(x, y + 5.dp.toPx()),
                                strokeWidth = stroke
                            )
                        }
                        3 -> {
                            drawRoundRect(
                                color = ChatWallpaperDoodle,
                                topLeft = Offset(x - 12.dp.toPx(), y - 12.dp.toPx()),
                                size = Size(24.dp.toPx(), 24.dp.toPx()),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()),
                                style = Stroke(stroke)
                            )
                            drawCircle(
                                color = ChatWallpaperDoodle,
                                radius = 2.dp.toPx(),
                                center = Offset(x - 4.dp.toPx(), y - 3.dp.toPx())
                            )
                            drawCircle(
                                color = ChatWallpaperDoodle,
                                radius = 2.dp.toPx(),
                                center = Offset(x + 4.dp.toPx(), y - 3.dp.toPx())
                            )
                        }
                        else -> {
                            drawLine(
                                color = ChatWallpaperDoodle,
                                start = Offset(x - 10.dp.toPx(), y + 8.dp.toPx()),
                                end = Offset(x, y - 10.dp.toPx()),
                                strokeWidth = stroke
                            )
                            drawLine(
                                color = ChatWallpaperDoodle,
                                start = Offset(x, y - 10.dp.toPx()),
                                end = Offset(x + 10.dp.toPx(), y + 8.dp.toPx()),
                                strokeWidth = stroke
                            )
                            drawLine(
                                color = ChatWallpaperDoodle,
                                start = Offset(x + 10.dp.toPx(), y + 8.dp.toPx()),
                                end = Offset(x - 10.dp.toPx(), y + 8.dp.toPx()),
                                strokeWidth = stroke
                            )
                        }
                    }
                    x += stepX
                }
                y += stepY
                row++
            }
        }
    }
}
