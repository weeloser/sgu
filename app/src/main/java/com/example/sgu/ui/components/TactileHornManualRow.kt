package com.example.sgu.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sgu.R
import com.example.sgu.ui.theme.Carbon800
import com.example.sgu.ui.theme.Carbon850
import com.example.sgu.ui.theme.PoliceBlue
import com.example.sgu.ui.theme.PoliceRed
import com.example.sgu.ui.theme.TextMuted
import com.example.sgu.ui.theme.TextPrimary

@Composable
fun TactileHornManualRow(
    isKryakActive: Boolean,
    isManualActive: Boolean,
    onKryakDown: () -> Unit,
    onKryakUp: () -> Unit,
    onManualDown: () -> Unit,
    onManualUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // HORN 70HZ (КРЯК)
        val kryakScale by animateFloatAsState(
            targetValue = if (isKryakActive) 0.96f else 1f,
            animationSpec = tween(80),
            label = "kryakScale"
        )
        val kryakBorderColor by animateColorAsState(
            targetValue = if (isKryakActive) PoliceRed else PoliceRed.copy(alpha = 0.5f),
            animationSpec = tween(80),
            label = "kryakBorder"
        )
        val kryakBgColor by animateColorAsState(
            targetValue = if (isKryakActive) Color(0xFF2B0E11) else Carbon850,
            animationSpec = tween(80),
            label = "kryakBg"
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .scale(kryakScale)
                .shadow(
                    elevation = if (isKryakActive) 12.dp else 4.dp,
                    shape = RoundedCornerShape(14.dp),
                    ambientColor = if (isKryakActive) PoliceRed else Color.Black,
                    spotColor = if (isKryakActive) PoliceRed else Color.Black
                )
                .clip(RoundedCornerShape(14.dp))
                .background(kryakBgColor)
                .border(width = 1.5.dp, color = kryakBorderColor, shape = RoundedCornerShape(14.dp))
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        onKryakDown()
                        waitForUpOrCancellation()
                        onKryakUp()
                    }
                }
                .padding(horizontal = 14.dp, vertical = 18.dp)
                .testTag("btn_kryak_elina")
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.horn_70hz),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = PoliceRed
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isKryakActive) PoliceRed else Color.Transparent)
                            .border(1.dp, if (isKryakActive) PoliceRed else PoliceRed.copy(alpha = 0.3f), CircleShape)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.kryak),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    color = TextPrimary
                )
                Text(
                    text = stringResource(R.string.kryak_desc),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }
        }

        // MANUAL (ПОДВЫВ)
        val manualScale by animateFloatAsState(
            targetValue = if (isManualActive) 0.96f else 1f,
            animationSpec = tween(80),
            label = "manualScale"
        )
        val manualBorderColor by animateColorAsState(
            targetValue = if (isManualActive) PoliceBlue else PoliceBlue.copy(alpha = 0.4f),
            animationSpec = tween(80),
            label = "manualBorder"
        )
        val manualBgColor by animateColorAsState(
            targetValue = if (isManualActive) Color(0xFF0F1E38) else Carbon850,
            animationSpec = tween(80),
            label = "manualBg"
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .scale(manualScale)
                .shadow(
                    elevation = if (isManualActive) 12.dp else 4.dp,
                    shape = RoundedCornerShape(14.dp),
                    ambientColor = if (isManualActive) PoliceBlue else Color.Black,
                    spotColor = if (isManualActive) PoliceBlue else Color.Black
                )
                .clip(RoundedCornerShape(14.dp))
                .background(manualBgColor)
                .border(width = 1.5.dp, color = manualBorderColor, shape = RoundedCornerShape(14.dp))
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        onManualDown()
                        waitForUpOrCancellation()
                        onManualUp()
                    }
                }
                .padding(horizontal = 14.dp, vertical = 18.dp)
                .testTag("btn_manual")
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.manual_title),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = PoliceBlue
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isManualActive) PoliceBlue else Color.Transparent)
                            .border(1.dp, if (isManualActive) PoliceBlue else PoliceBlue.copy(alpha = 0.3f), CircleShape)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.manual_action),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    color = TextPrimary
                )
                Text(
                    text = stringResource(R.string.manual_desc),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }
        }
    }
}
