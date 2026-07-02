package com.guitartuna.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.guitartuna.app.ui.screen.TunerScreen
import com.guitartuna.app.ui.theme.GuitarTunaTheme
import com.guitartuna.app.viewmodel.TunerViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GuitarTunaTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val viewModel: TunerViewModel = viewModel()
                    val uiState by viewModel.uiState.collectAsState()
                    var pendingStart by remember { mutableStateOf(false) }

                    val permissionLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) { granted ->
                        if (granted && pendingStart) {
                            viewModel.startTuning()
                        }
                        pendingStart = false
                    }

                    TunerScreen(
                        uiState = uiState,
                        onStartStop = {
                            if (viewModel.isRunning()) {
                                viewModel.stopTuning()
                            } else if (hasRecordPermission()) {
                                viewModel.startTuning()
                            } else {
                                pendingStart = true
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        onSelectString = { index -> viewModel.selectString(index) },
                        onSelectAuto = { viewModel.selectAuto() },
                    )
                }
            }
        }
    }

    private fun hasRecordPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
}
