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
import androidx.compose.runtime.LaunchedEffect
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
            var darkTheme by remember { mutableStateOf(true) }
            GuitarTunaTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val viewModel: TunerViewModel = viewModel()
                    val uiState by viewModel.uiState.collectAsState()
                    var didAutoStart by remember { mutableStateOf(false) }

                    val permissionLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) { granted ->
                        if (granted) viewModel.startTuning()
                    }

                    LaunchedEffect(Unit) {
                        if (didAutoStart) return@LaunchedEffect
                        didAutoStart = true
                        if (hasRecordPermission()) {
                            viewModel.startTuning()
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }

                    TunerScreen(
                        uiState = uiState,
                        darkTheme = darkTheme,
                        onSelectString = { index -> viewModel.selectString(index) },
                        onToggleAuto = { viewModel.toggleAuto() },
                        onToggleReference = { viewModel.toggleReference() },
                        onToggleTheme = { darkTheme = !darkTheme },
                    )
                }
            }
        }
    }

    private fun hasRecordPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
}
