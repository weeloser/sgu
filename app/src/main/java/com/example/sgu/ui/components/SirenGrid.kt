package com.example.sgu.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import com.example.sgu.ui.theme.Carbon800
import com.example.sgu.ui.theme.Carbon850
import com.example.sgu.ui.theme.Carbon900
import com.example.sgu.ui.theme.CarbonBorder
import com.example.sgu.ui.theme.PoliceAmber
import com.example.sgu.ui.theme.PoliceBlue
import com.example.sgu.ui.theme.PoliceRed
import com.example.sgu.ui.theme.TextMuted
import com.example.sgu.ui.theme.TextPrimary
import com.example.sgu.ui.theme.TextSecondary

@Composable
fun SirenGrid(
    activeSiren: String?,
    isKlaxonActive: Boolean,
    isSgoActive: Boolean,
    onToggleSiren: (String) -> Unit,
    onDoubleKryakClick: () -> Unit,
    onBibikaPressStart: () -> Unit,
    onBibikaPressEnd: () -> Unit,
    onToggleSgo: () -> Unit,
    onStopAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Carbon900)
            .border(1.dp, CarbonBorder, RoundedCornerShape(14.dp))
            .padding(12.dp)
            .testTag("siren_grid_card")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Title & Stop All
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.gost_subtitle),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted
                )

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onStopAll() }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                        .testTag("stop_all_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(PoliceRed)
                    )
                    Text(
                        text = stringResource(R.string.stop_all),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = PoliceRed
                    )
                }
            }

            // Row 1: WAIL 1, WAIL 2, SRT
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SirenButton(
                    title = stringResource(R.string.wail1_title),
                    subtitle = stringResource(R.string.wail1_desc),
                    isActive = activeSiren == "wail1",
                    activeColor = PoliceBlue,
                    testTag = "btn_wail1",
                    onClick = { onToggleSiren("wail1") },
                    modifier = Modifier.weight(1f)
                )
                SirenButton(
                    title = stringResource(R.string.wail2_title),
                    subtitle = stringResource(R.string.wail2_desc),
                    isActive = activeSiren == "wail2",
                    activeColor = PoliceBlue,
                    testTag = "btn_wail2",
                    onClick = { onToggleSiren("wail2") },
                    modifier = Modifier.weight(1f)
                )
                SirenButton(
                    title = stringResource(R.string.srt_title),
                    subtitle = stringResource(R.string.srt_desc),
                    isActive = activeSiren == "srt",
                    activeColor = PoliceAmber,
                    titleColor = PoliceAmber,
                    testTag = "btn_srt",
                    onClick = { onToggleSiren("srt") },
                    modifier = Modifier.weight(1f)
                )
            }

            // Row 2: YELP, HI-LO, КРЯК ×2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SirenButton(
                    title = stringResource(R.string.yelp_title),
                    subtitle = stringResource(R.string.yelp_desc),
                    isActive = activeSiren == "yelp",
                    activeColor = PoliceBlue,
                    testTag = "btn_yelp",
                    onClick = { onToggleSiren("yelp") },
                    modifier = Modifier.weight(1f)
                )
                SirenButton(
                    title = stringResource(R.string.hilo_title),
                    subtitle = stringResource(R.string.hilo_desc),
                    isActive = activeSiren == "hilo",
                    activeColor = PoliceBlue,
                    testTag = "btn_hilo",
                    onClick = { onToggleSiren("hilo") },
                    modifier = Modifier.weight(1f)
                )
                SirenButton(
                    title = stringResource(R.string.double_kryak_title),
                    subtitle = stringResource(R.string.double_kryak_desc),
                    isActive = false,
                    activeColor = PoliceRed,
                    titleColor = Color(0xFFFCA5A5),
                    testTag = "btn_double_kryak",
                    onClick = { onDoubleKryakClick() },
                    modifier = Modifier.weight(1f)
                )
            }

            // Auxiliary Row 3: КЛАКСОН, СГО, ГОРЯЧАЯ
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // КЛАКСОН (Touch Hold/Tap)
                val klaxonBorder by animateColorAsState(
                    targetValue = if (isKlaxonActive) PoliceAmber else CarbonBorder,
                    label = "klaxonBorder"
                )
                val klaxonBg by animateColorAsState(
                    targetValue = if (isKlaxonActive) Color(0xFF2E1C07) else Carbon850,
                    label = "klaxonBg"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(klaxonBg)
                        .border(1.dp, klaxonBorder, RoundedCornerShape(8.dp))
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                onBibikaPressStart()
                                waitForUpOrCancellation()
                                onBibikaPressEnd()
                            }
                        }
                        .padding(vertical = 10.dp, horizontal = 4.dp)
                        .testTag("btn_bibika"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.klaxon_title),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isKlaxonActive) PoliceAmber else TextSecondary
                        )
                        Text(
                            text = stringResource(R.string.klaxon_desc),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = TextMuted
                        )
                    }
                }

                // СГО (Стробоскопы)
                val infiniteTransition = rememberInfiniteTransition(label = "sgoPing")
                val pingAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.4f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(250),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pingAlpha"
                )

                val sgoBorder by animateColorAsState(
                    targetValue = if (isSgoActive) PoliceBlue else PoliceBlue.copy(alpha = 0.4f),
                    label = "sgoBorder"
                )
                val sgoBg by animateColorAsState(
                    targetValue = if (isSgoActive) Color(0xFF0F1E38) else Carbon850,
                    label = "sgoBg"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(sgoBg)
                        .border(1.dp, sgoBorder, RoundedCornerShape(8.dp))
                        .clickable { onToggleSgo() }
                        .padding(vertical = 10.dp, horizontal = 4.dp)
                        .testTag("btn_sgo"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(PoliceRed.copy(alpha = if (isSgoActive) pingAlpha else 0.5f))
                            )
                            Text(
                                text = stringResource(R.string.sgo_title),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = PoliceBlue
                            )
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(PoliceBlue.copy(alpha = if (isSgoActive) pingAlpha else 0.5f))
                            )
                        }
                        Text(
                            text = stringResource(R.string.sgo_desc),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = TextMuted
                        )
                    }
                }

                // ГОРЯЧАЯ (15 сек Смерч)
                SirenButton(
                    title = stringResource(R.string.hotkey_title),
                    subtitle = stringResource(R.string.hotkey_desc),
                    isActive = activeSiren == "hotkey",
                    activeColor = PoliceRed,
                    titleColor = PoliceRed,
                    testTag = "btn_hotkey",
                    onClick = { onToggleSiren("hotkey") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SirenButton(
    title: String,
    subtitle: String,
    isActive: Boolean,
    activeColor: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    titleColor: Color = TextPrimary
) {
    val borderColor by animateColorAsState(
        targetValue = if (isActive) activeColor else CarbonBorder,
        animationSpec = tween(80),
        label = "btnBorder"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isActive) activeColor.copy(alpha = 0.15f) else Carbon850,
        animationSpec = tween(80),
        label = "btnBg"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isActive) activeColor else titleColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                color = TextMuted
            )
        }
    }
}
