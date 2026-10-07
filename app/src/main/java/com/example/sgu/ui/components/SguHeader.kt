package com.example.sgu.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sgu.R
import com.example.sgu.ui.theme.Carbon900
import com.example.sgu.ui.theme.Carbon950
import com.example.sgu.ui.theme.CarbonBorder
import com.example.sgu.ui.theme.PoliceBlue
import com.example.sgu.ui.theme.PoliceBlueDim
import com.example.sgu.ui.theme.PoliceRed
import com.example.sgu.ui.theme.PoliceRedDim
import com.example.sgu.ui.theme.TextMuted
import com.example.sgu.ui.theme.TextPrimary
import com.example.sgu.ui.theme.TextSecondary

@Composable
fun SguHeader(
    statusText: String,
    isSgoActive: Boolean,
    headerStrobeLeftOn: Boolean,
    headerStrobeRightOn: Boolean,
    onToggleSgo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Carbon900.copy(alpha = 0.95f))
            .border(width = 1.dp, color = CarbonBorder)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("sgu_header")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: System LED & Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(PoliceRed)
                )
                Text(
                    text = stringResource(R.string.system_title),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = TextPrimary
                )
            }

            // Right: Mini Strobe Lightbar & Mode label
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Mini Strobe Button
                val leftColor by animateColorAsState(
                    targetValue = if (headerStrobeLeftOn || (isSgoActive && headerStrobeLeftOn)) PoliceRed else PoliceRedDim,
                    animationSpec = tween(60),
                    label = "strobeLeft"
                )
                val rightColor by animateColorAsState(
                    targetValue = if (headerStrobeRightOn || (isSgoActive && headerStrobeRightOn)) PoliceBlue else PoliceBlueDim,
                    animationSpec = tween(60),
                    label = "strobeRight"
                )

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Carbon950)
                        .border(1.dp, if (isSgoActive) PoliceBlue else CarbonBorder, RoundedCornerShape(4.dp))
                        .clickable { onToggleSgo() }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                        .testTag("header_sgo_button"),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(8.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(leftColor)
                    )
                    Box(
                        modifier = Modifier
                            .width(8.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(rightColor)
                    )
                }

                Text(
                    text = statusText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    color = if (statusText == "ГОТОВ") TextMuted else PoliceBlue,
                    modifier = Modifier.testTag("active_mode_label")
                )
            }
        }
    }
}
