package com.example.sgu.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.example.sgu.ui.theme.PoliceBlueBright
import com.example.sgu.ui.theme.PoliceRedBright
import com.example.sgu.ui.theme.TextMuted

@Composable
fun SgoOverlay(
    isVisible: Boolean,
    strobeLeftOn: Boolean,
    strobeRightOn: Boolean,
    onKryakDown: () -> Unit,
    onKryakUp: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    onDismiss()
                }
                .testTag("sgo_fullscreen_overlay")
        ) {
            // Split screen flashers: Left Red, Right Blue
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(if (strobeLeftOn) PoliceRedBright else Color.White)
                        .testTag("sgo_left_side")
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(if (strobeRightOn) PoliceBlueBright else Color.White)
                        .testTag("sgo_right_side")
                )
            }

            // Center Floating HUD
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* prevent dismiss when clicking column gap */ },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Tactical Horn Button in SGO Mode
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xE6000000))
                        .border(1.5.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                onKryakDown()
                                waitForUpOrCancellation()
                                onKryakUp()
                            }
                        }
                        .padding(horizontal = 32.dp, vertical = 16.dp)
                        .testTag("sgo_overlay_horn_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.kryak),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        color = Color.White
                    )
                }

                // Dismiss hint badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.85f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.sgo_exit_hint),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF3F3F46)
                    )
                }
            }
        }
    }
}
