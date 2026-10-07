package com.example.sgu.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sgu.R
import com.example.sgu.ui.theme.Carbon850
import com.example.sgu.ui.theme.Carbon900
import com.example.sgu.ui.theme.CarbonBorder
import com.example.sgu.ui.theme.PoliceBlue
import com.example.sgu.ui.theme.PoliceBlueDim
import com.example.sgu.ui.theme.TextMuted
import com.example.sgu.ui.theme.TextPrimary
import com.example.sgu.ui.theme.TextSecondary

@Composable
fun PttSection(
    isRecording: Boolean,
    isVoicePlaying: Boolean,
    statusInfo: String,
    timerText: String,
    hasRecordedAudio: Boolean,
    onPttDown: () -> Unit,
    onPttUp: () -> Unit,
    onReplayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Carbon900)
            .border(1.dp, CarbonBorder, RoundedCornerShape(14.dp))
            .padding(12.dp)
            .testTag("ptt_section_card")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Title & Timer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(PoliceBlue)
                    )
                    Text(
                        text = stringResource(R.string.ptt_title),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                }

                Text(
                    text = timerText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isRecording) PoliceBlue else TextMuted,
                    modifier = Modifier.testTag("ptt_record_timer")
                )
            }

            // Big PTT Button
            val borderCol by animateColorAsState(
                targetValue = if (isRecording) PoliceBlue else CarbonBorder,
                animationSpec = tween(80),
                label = "pttBorder"
            )
            val bgCol by animateColorAsState(
                targetValue = if (isRecording) Color(0xFF0F1E38) else Carbon850,
                animationSpec = tween(80),
                label = "pttBg"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgCol)
                    .border(1.5.dp, borderCol, RoundedCornerShape(12.dp))
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            onPttDown()
                            waitForUpOrCancellation()
                            onPttUp()
                        }
                    }
                    .padding(vertical = 16.dp)
                    .testTag("record_ptt_btn"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(PoliceBlueDim),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(PoliceBlue)
                        )
                    }

                    Text(
                        text = if (isRecording) stringResource(R.string.ptt_broadcasting)
                        else stringResource(R.string.ptt_action),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        color = if (isRecording) PoliceBlue else TextPrimary,
                        modifier = Modifier.testTag("ptt_title_text")
                    )
                }
            }

            // Footer: Info & Replay
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = statusInfo,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = if (isVoicePlaying) PoliceBlue else TextMuted,
                    modifier = Modifier.testTag("rec_status_info")
                )

                Text(
                    text = stringResource(R.string.replay_title),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (hasRecordedAudio && !isRecording && !isVoicePlaying) TextSecondary else TextMuted.copy(alpha = 0.4f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable(enabled = hasRecordedAudio && !isRecording && !isVoicePlaying) {
                            onReplayClick()
                        }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .testTag("replay_voice_btn")
                )
            }
        }
    }
}
