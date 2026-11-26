package com.teddy.bpositive.ui.screens.messaging.chatlist

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val EncryptionFooterGray = Color(0xFF667781)

/**
 * Small, non-interactive privacy notice displayed after the conversation rows.
 */
@Composable
fun EncryptionFooter(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .padding(horizontal = 24.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(
            modifier = Modifier
                .padding(end = 7.dp)
                .size(width = 15.dp, height = 17.dp)
        ) {
            val strokeWidth = 1.5.dp.toPx()
            drawArc(
                color = EncryptionFooterGray,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(
                    x = size.width * 0.22f,
                    y = size.height * 0.04f
                ),
                size = androidx.compose.ui.geometry.Size(
                    width = size.width * 0.56f,
                    height = size.height * 0.62f
                ),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            drawRoundRect(
                color = EncryptionFooterGray,
                topLeft = androidx.compose.ui.geometry.Offset(
                    x = size.width * 0.12f,
                    y = size.height * 0.43f
                ),
                size = androidx.compose.ui.geometry.Size(
                    width = size.width * 0.76f,
                    height = size.height * 0.53f
                ),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                    x = 2.dp.toPx(),
                    y = 2.dp.toPx()
                ),
                style = Stroke(width = strokeWidth)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Your personal messages are",
                color = EncryptionFooterGray,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
            Text(
                text = "end-to-end encrypted",
                color = EncryptionFooterGray,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}
