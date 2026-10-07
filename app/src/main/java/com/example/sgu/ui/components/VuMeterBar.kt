package com.example.sgu.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sgu.ui.theme.Carbon900
import com.example.sgu.ui.theme.CarbonBorder
import com.example.sgu.ui.theme.PoliceBlue
import com.example.sgu.ui.theme.PoliceRed
import com.example.sgu.ui.theme.TextMuted
import com.example.sgu.ui.theme.VuInactive

@Composable
fun VuMeterBar(
    activeLevel: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Carbon900)
            .border(1.dp, CarbonBorder.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag("vu_meter_container")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "VU",
                fontFamily = FontFamily.Monospace,
                fontSize = 8.sp,
                color = TextMuted
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(5.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                for (i in 0 until 12) {
                    val isActive = i < activeLevel
                    val targetColor = when {
                        !isActive -> VuInactive
                        i < 8 -> PoliceBlue
                        else -> PoliceRed
                    }
                    val color by animateColorAsState(
                        targetValue = targetColor,
                        animationSpec = tween(50),
                        label = "vuSeg$i"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(5.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(color)
                    )
                }
            }
        }
    }
}
