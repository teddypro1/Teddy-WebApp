package com.teddy.bpositive.ui.screens.messaging.chatlist

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teddy.bpositive.ui.theme.TeddyAppTheme

private val EncryptionFooterText = Color(0xFF667781)
private val EncryptionFooterGreen = Color(0xFF16865A)
private val EncryptionFooterDivider = Color(0xFFE9EDEF)

/**
 * WhatsApp-style end-to-end encryption notice at the end of the conversation list.
 */
@Composable
fun EncryptionFooter(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        // Thin separator above the notice, as shown in the WhatsApp chat list.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(EncryptionFooterDivider)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Canvas(
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(width = 12.dp, height = 14.dp)
            ) {
                val stroke = 1.35.dp.toPx()

                // Rounded shackle.
                drawArc(
                    color = EncryptionFooterText,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(
                        x = size.width * 0.23f,
                        y = size.height * 0.02f
                    ),
                    size = androidx.compose.ui.geometry.Size(
                        width = size.width * 0.54f,
                        height = size.height * 0.62f
                    ),
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )

                // Lock body.
                drawRoundRect(
                    color = EncryptionFooterText,
                    topLeft = androidx.compose.ui.geometry.Offset(
                        x = size.width * 0.10f,
                        y = size.height * 0.40f
                    ),
                    size = androidx.compose.ui.geometry.Size(
                        width = size.width * 0.80f,
                        height = size.height * 0.56f
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                        x = 1.6.dp.toPx(),
                        y = 1.6.dp.toPx()
                    ),
                    style = Stroke(width = stroke)
                )
            }

            Text(
                text = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            color = EncryptionFooterText,
                            fontWeight = FontWeight.Normal
                        )
                    ) {
                        append("Your personal messages are ")
                    }
                    withStyle(
                        SpanStyle(
                            color = EncryptionFooterGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    ) {
                        append("end-to-end encrypted")
                    }
                },
                fontSize = 12.sp,
                lineHeight = 16.sp,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Preview(
    name = "WhatsApp Encryption Footer",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    widthDp = 360,
    heightDp = 64
)
@Composable
private fun EncryptionFooterPreview() {
    TeddyAppTheme(darkMode = false) {
        EncryptionFooter()
    }
}
