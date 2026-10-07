package com.example.sgu.ui

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sgu.R
import com.example.sgu.ui.components.PttSection
import com.example.sgu.ui.components.SgoOverlay
import com.example.sgu.ui.components.SguHeader
import com.example.sgu.ui.components.SirenGrid
import com.example.sgu.ui.components.TactileHornManualRow
import com.example.sgu.ui.components.VuMeterBar
import com.example.sgu.ui.theme.Carbon950
import com.example.sgu.ui.theme.CarbonBorder
import com.example.sgu.ui.theme.TextMuted

@Composable
fun SguScreen(
    viewModel: SguViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startPtt()
        }
    }

    // Back handling
    BackHandler(enabled = uiState.isSgoActive || uiState.activeSiren != null) {
        if (uiState.isSgoActive) {
            viewModel.stopSgo()
        } else {
            viewModel.stopAll()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Carbon950)
    ) {
        Scaffold(
            topBar = {
                SguHeader(
                    statusText = uiState.statusText,
                    isSgoActive = uiState.isSgoActive,
                    headerStrobeLeftOn = uiState.headerStrobeLeftOn,
                    headerStrobeRightOn = uiState.headerStrobeRightOn,
                    onToggleSgo = { viewModel.toggleSgo() },
                    modifier = Modifier.statusBarsPadding()
                )
            },
            bottomBar = {
                // Footer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .drawBehind {
                            drawLine(
                                color = CarbonBorder.copy(alpha = 0.7f),
                                start = Offset(0f, 0f),
                                end = Offset(size.width, 0f),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("sgu_footer")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 480.dp)
                            .align(Alignment.Center),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.footer_left),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = TextMuted
                        )
                        Text(
                            text = stringResource(R.string.footer_right),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = TextMuted
                        )
                    }
                }
            },
            containerColor = Carbon950
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 480.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // BLOCK 1: HORN 70HZ & MANUAL
                    TactileHornManualRow(
                        isKryakActive = uiState.isKryakActive,
                        isManualActive = uiState.isManualActive,
                        onKryakDown = { viewModel.startKryak() },
                        onKryakUp = { viewModel.stopKryak() },
                        onManualDown = { viewModel.startManual() },
                        onManualUp = { viewModel.stopManual() }
                    )

                    // BLOCK 2: SIREN GRID
                    SirenGrid(
                        activeSiren = uiState.activeSiren,
                        isKlaxonActive = uiState.isKlaxonActive,
                        isSgoActive = uiState.isSgoActive,
                        onToggleSiren = { viewModel.toggleSiren(it) },
                        onDoubleKryakClick = { viewModel.playDoubleKryak() },
                        onBibikaPressStart = { viewModel.onBibikaPressStart() },
                        onBibikaPressEnd = { viewModel.onBibikaPressEnd() },
                        onToggleSgo = { viewModel.toggleSgo() },
                        onStopAll = { viewModel.stopAll() }
                    )

                    // BLOCK 3: PTT TANGENTA
                    PttSection(
                        isRecording = uiState.isRecording,
                        isVoicePlaying = uiState.isVoicePlaying,
                        statusInfo = uiState.pttStatusInfo,
                        timerText = uiState.recordTimerText,
                        hasRecordedAudio = uiState.hasRecordedAudio,
                        onPttDown = {
                            if (viewModel.hasRecordPermission()) {
                                viewModel.startPtt()
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        onPttUp = { viewModel.stopPtt() },
                        onReplayClick = { viewModel.replayVoice() }
                    )

                    // VU METER
                    VuMeterBar(
                        activeLevel = uiState.vuMeterLevel
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }

        // FULLSCREEN SGO STROBE OVERLAY
        SgoOverlay(
            isVisible = uiState.isSgoActive,
            strobeLeftOn = uiState.strobeLeftOn,
            strobeRightOn = uiState.strobeRightOn,
            onKryakDown = { viewModel.startKryak() },
            onKryakUp = { viewModel.stopKryak() },
            onDismiss = { viewModel.stopSgo() }
        )
    }
}
