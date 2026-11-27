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
        modifier = modifier.padding(horizontal = 28.dp, vertical = 8.dp),
        color = ChatEncryptionNoticeBackground,
        contentColor = ChatEncryptionNoticeText,
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Canvas(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(width = 13.dp, height = 16.dp)
            ) {
                val stroke = 1.35.dp.toPx()
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
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.6.dp.toPx()),
                    style = Stroke(width = stroke)
                )
            }

            androidx.compose.foundation.layout.Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Messages are protected in transit and at rest.",
                    color = ChatEncryptionNoticeText,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Normal
                )
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                color = ChatEncryptionNoticeLink,
                                fontWeight = FontWeight.SemiBold
                            )
                        ) {
                            append("Learn more")
                        }
                    },
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .clickable(onClick = onLearnMore),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
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
            modifier = Modifier
                .background(Color(0xFFF7F2EA))
                .padding(vertical = 8.dp)
        ) {
            ChatEncryptionNotice()
        }
    }
}
