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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teddy.bpositive.ui.theme.TeddyAppTheme

private val FeaturedPrimaryText = Color(0xFF111418)
private val FeaturedSecondaryText = Color(0xFF62686D)
private val FeaturedDraftGreen = Color(0xFF159B58)

/**
 * Static visual representation of the featured group/draft row in the
 * supplied WhatsApp Chat List reference.
 *
 * This first pass intentionally uses a drawn group-avatar placeholder.
 * Real profile imagery, click handling and conversation data are not wired yet.
 */
@Composable
fun FeaturedChatItem(
    title: String = "Tedd Music",
    sender: String = "Ted Music",
    draftPreview: String = "https://radhuni.0128j.top/rttwnihl/09300747007...",
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FeaturedGroupAvatar()

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp, end = 6.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                color = FeaturedPrimaryText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = sender,
                    color = FeaturedSecondaryText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Canvas(
                    modifier = Modifier
                        .padding(horizontal = 5.dp)
                        .size(width = 6.dp, height = 10.dp)
                ) {
                    val lineColor = FeaturedSecondaryText
                    val stroke = 1.3.dp.toPx()
                    drawLine(
                        color = lineColor,
                        start = Offset(1.dp.toPx(), 2.dp.toPx()),
                        end = Offset(4.5.dp.toPx(), size.height / 2f),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = lineColor,
                        start = Offset(4.5.dp.toPx(), size.height / 2f),
                        end = Offset(1.dp.toPx(), size.height - 2.dp.toPx()),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }

                Text(
                    text = "Draft:",
                    color = FeaturedDraftGreen,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )

                Text(
                    text = " $draftPreview",
                    modifier = Modifier.weight(1f),
                    color = FeaturedSecondaryText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Canvas(
            modifier = Modifier
                .padding(start = 2.dp, end = 1.dp)
                .size(width = 8.dp, height = 16.dp)
        ) {
            val stroke = 1.6.dp.toPx()
            drawLine(
                color = FeaturedSecondaryText,
                start = Offset(1.dp.toPx(), 2.dp.toPx()),
                end = Offset(size.width - 1.dp.toPx(), size.height / 2f),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            drawLine(
                color = FeaturedSecondaryText,
                start = Offset(size.width - 1.dp.toPx(), size.height / 2f),
                end = Offset(1.dp.toPx(), size.height - 2.dp.toPx()),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun FeaturedGroupAvatar() {
    Box(
        modifier = Modifier.size(width = 34.dp, height = 36.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 0.dp, top = 3.dp)
                .size(25.dp)
                .background(
                    color = Color(0xFFE9E5D9),
                    shape = RoundedCornerShape(7.dp)
                )
        )

        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 4.dp, top = 1.dp)
                .size(27.dp)
                .background(
                    color = Color(0xFFF4EBD7),
                    shape = RoundedCornerShape(7.dp)
                )
        )

        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(28.dp)
                .background(
                    color = Color(0xFFFFF0D4),
                    shape = RoundedCornerShape(7.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(20.dp)) {
                val iconColor = Color(0xFF9B7138)
                val headRadius = 2.5.dp.toPx()

                drawCircle(
                    color = iconColor,
                    radius = headRadius,
                    center = Offset(size.width * 0.5f, size.height * 0.30f)
                )
                drawCircle(
                    color = iconColor,
                    radius = 2.dp.toPx(),
                    center = Offset(size.width * 0.27f, size.height * 0.43f)
                )
                drawCircle(
                    color = iconColor,
                    radius = 2.dp.toPx(),
                    center = Offset(size.width * 0.73f, size.height * 0.43f)
                )

                drawRoundRect(
                    color = iconColor,
                    topLeft = Offset(size.width * 0.32f, size.height * 0.49f),
                    size = androidx.compose.ui.geometry.Size(
                        size.width * 0.36f,
                        size.height * 0.28f
                    ),
                    cornerRadius = CornerRadius(3.dp.toPx())
                )
                drawRoundRect(
                    color = iconColor,
                    topLeft = Offset(size.width * 0.08f, size.height * 0.59f),
                    size = androidx.compose.ui.geometry.Size(
                        size.width * 0.30f,
                        size.height * 0.20f
                    ),
                    cornerRadius = CornerRadius(2.dp.toPx())
                )
                drawRoundRect(
                    color = iconColor,
                    topLeft = Offset(size.width * 0.62f, size.height * 0.59f),
                    size = androidx.compose.ui.geometry.Size(
                        size.width * 0.30f,
                        size.height * 0.20f
                    ),
                    cornerRadius = CornerRadius(2.dp.toPx())
                )
            }
        }
    }
}

@Preview(
    name = "Featured Chat Item - Draft Group Row",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    widthDp = 360,
    heightDp = 120
)
@Composable
private fun FeaturedChatItemPreview() {
    TeddyAppTheme(darkMode = false) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(top = 8.dp)
        ) {
            FeaturedChatItem()
        }
    }
}
