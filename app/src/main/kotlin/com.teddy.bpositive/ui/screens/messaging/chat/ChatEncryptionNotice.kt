package com.teddy.bpositive.ui.screens.messaging.chat

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teddy.bpositive.ui.theme.TeddyAppTheme

private val ChatEncryptionNoticeText = Color(0xFF54656F)
private val ChatEncryptionNoticeLink = Color(0xFF16865A)
private val ChatEncryptionNoticeBackground = Color(0xFFFFF8E7)

/**
 * Compact privacy notice displayed near the beginning of a conversation.
 *
 * The copy deliberately describes transport/storage protection only.
 * This app does not currently implement end-to-end encryption.
 */
@Composable
fun ChatEncryptionNotice(
    modifier: Modifier = Modifier,
    onLearnMore: () -> Unit = {}
) {
    Surface(
        modifier = modifier.padding(horizontal = 18.dp, vertical = 8.dp),
        color = ChatEncryptionNoticeBackground,
        contentColor = ChatEncryptionNoticeText,
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Canvas(
                modifier = Modifier
                    .padding(end = 8.dp, top = 2.dp)
                    .size(width = 12.dp, height = 14.dp)
            ) {
                val stroke = 1.25.dp.toPx()
                drawArc(
                    color = ChatEncryptionNoticeText,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(
                        size.width * 0.22f, size.height * 0.02f
                    ),
                    size = androidx.compose.ui.geometry.Size(
                        size.width * 0.56f, size.height * 0.62f
                    ),
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                drawRoundRect(
                    color = ChatEncryptionNoticeText,
                    topLeft = androidx.compose.ui.geometry.Offset(
                        size.width * 0.10f, size.height * 0.40f
                    ),
                    size = androidx.compose.ui.geometry.Size(
                        size.width * 0.80f, size.height * 0.56f
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.5.dp.toPx()),
                    style = Stroke(width = stroke)
                )
            }

            Text(
                text = buildAnnotatedString {
                    append("Messages are protected in transit and at rest. ")
                    withStyle(
                        SpanStyle(
                            color = ChatEncryptionNoticeLink,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append("Learn more")
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onLearnMore),
                color = ChatEncryptionNoticeText,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Preview(
    name = "Chat Encryption Notice",
    showBackground = true,
    backgroundColor = 0xFFF7F2EA,
    widthDp = 360
)
@Composable
private fun ChatEncryptionNoticePreview() {
    TeddyAppTheme(darkMode = false) {
        Box(
            modifier = androidx.compose.foundation.layout.Box
                .class.java.let { androidx.compose.ui.Modifier }
                .padding(vertical = 8.dp)
        ) {
            ChatEncryptionNotice()
        }
    }
}
