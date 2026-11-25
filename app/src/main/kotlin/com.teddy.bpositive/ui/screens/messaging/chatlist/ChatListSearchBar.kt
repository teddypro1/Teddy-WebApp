package com.teddy.bpositive.ui.screens.messaging.chatlist

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import com.teddy.bpositive.ui.theme.TeddyAppTheme

/**
 * Static, UI-only Chat List search field based on the supplied reference screenshot.
 *
 * The search icon and placeholder are visual only; no input state, click action,
 * search logic, navigation, repository, API or backend integration is included.
 */
@Composable
fun ChatListSearchBar(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 9.dp)
            .height(34.dp)
            .background(
                color = Color(0xFFF5F4F2),
                shape = RoundedCornerShape(percent = 50)
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 14.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Canvas(
                modifier = Modifier.size(18.dp)
            ) {
                val strokeWidth = 1.8.dp.toPx()
                val lensRadius = 5.2.dp.toPx()
                val lensCenter = Offset(
                    x = size.width * 0.43f,
                    y = size.height * 0.42f
                )

                drawCircle(
                    color = Color(0xFF62686D),
                    radius = lensRadius,
                    center = lensCenter,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = strokeWidth
                    )
                )

                drawLine(
                    color = Color(0xFF62686D),
                    start = Offset(
                        x = lensCenter.x + lensRadius * 0.72f,
                        y = lensCenter.y + lensRadius * 0.72f
                    ),
                    end = Offset(
                        x = size.width * 0.88f,
                        y = size.height * 0.88f
                    ),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }

            Text(
                text = "Ask Meta AI or Search",
                modifier = Modifier.padding(start = 6.dp),
                color = Color(0xFF62686D),
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.sp,
                maxLines = 1
            )
        }
    }
}

@Preview(
    name = "Chat List Search Bar - Screenshot Proportion",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    widthDp = 360,
    heightDp = 800
)
@Composable
private fun ChatListSearchBarPreview() {
    TeddyAppTheme(darkMode = false) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(top = 24.dp)
        ) {
            ChatListHeader()
            ChatListSearchBar()
        }
    }
}
