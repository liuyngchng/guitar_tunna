package com.guitartuna.app.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.guitartuna.app.audio.PitchDetector
import com.guitartuna.app.ui.theme.TuneGreen
import com.guitartuna.app.ui.theme.TuneOrange
import com.guitartuna.app.ui.theme.TuneRed
import com.guitartuna.app.ui.theme.TuneYellow
import com.guitartuna.app.viewmodel.TunerState
import com.guitartuna.app.viewmodel.TunerUiState
import com.guitartuna.app.viewmodel.TuningMode
import kotlin.math.abs

@Composable
fun TunerScreen(
    uiState: TunerUiState,
    onStartStop: () -> Unit,
    onSelectString: (Int) -> Unit,
    onSelectAuto: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StatusBar(uiState.tunerState)
        Spacer(modifier = Modifier.height(12.dp))
        StringSelector(
            selectedIndex = uiState.selectedStringIndex,
            mode = uiState.mode,
            onSelect = onSelectString,
            onSelectAuto = onSelectAuto,
        )
        Spacer(modifier = Modifier.weight(0.3f))
        NoteDisplay(
            note = uiState.detectedNote,
            cents = uiState.centsFromTarget,
            state = uiState.tunerState,
        )
        Spacer(modifier = Modifier.height(8.dp))
        TuningMeter(cents = uiState.centsFromTarget, state = uiState.tunerState)
        Spacer(modifier = Modifier.height(8.dp))
        CentsFrequencyRow(cents = uiState.centsFromTarget, frequency = uiState.detectedFrequency, state = uiState.tunerState)
        Spacer(modifier = Modifier.weight(0.3f))
        StartStopButton(isRunning = uiState.tunerState != TunerState.IDLE && uiState.tunerState != TunerState.ERROR, onClick = onStartStop)
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun StatusBar(state: TunerState) {
    val (text, color) = when (state) {
        TunerState.IDLE -> "点击下方按钮开始调音" to Color.Gray
        TunerState.LISTENING -> "正在监听..." to TuneYellow
        TunerState.DETECTING -> "检测中..." to TuneOrange
        TunerState.LOCKED -> "已调准！" to TuneGreen
        TunerState.ERROR -> "出错了" to TuneRed
    }
    Text(text = text, color = color, fontSize = 14.sp, fontWeight = FontWeight.Medium)
}

@Composable
private fun StringSelector(
    selectedIndex: Int,
    mode: TuningMode,
    onSelect: (Int) -> Unit,
    onSelectAuto: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StringChip("E", selected = mode == TuningMode.MANUAL && selectedIndex == 0) { onSelect(0) }
        StringChip("A", selected = mode == TuningMode.MANUAL && selectedIndex == 1) { onSelect(1) }
        StringChip("D", selected = mode == TuningMode.MANUAL && selectedIndex == 2) { onSelect(2) }
        StringChip("G", selected = mode == TuningMode.MANUAL && selectedIndex == 3) { onSelect(3) }
        StringChip("B", selected = mode == TuningMode.MANUAL && selectedIndex == 4) { onSelect(4) }
        StringChip("e", selected = mode == TuningMode.MANUAL && selectedIndex == 5) { onSelect(5) }
        Spacer(modifier = Modifier.width(8.dp))
        FilterChip(
            selected = mode == TuningMode.AUTO,
            onClick = onSelectAuto,
            label = { Text("自动", fontWeight = if (mode == TuningMode.AUTO) FontWeight.Bold else FontWeight.Normal) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = TuneGreen.copy(alpha = 0.2f),
                selectedLabelColor = TuneGreen,
            ),
        )
    }
}

@Composable
private fun StringChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = TuneGreen.copy(alpha = 0.2f),
            selectedLabelColor = TuneGreen,
        ),
        modifier = Modifier.padding(horizontal = 2.dp),
    )
}

@Composable
private fun NoteDisplay(note: String, cents: Float, state: TunerState) {
    val color by animateColorAsState(
        targetValue = centsColor(abs(cents), state),
        animationSpec = tween(150),
    )
    Text(
        text = note,
        fontSize = 72.sp,
        fontWeight = FontWeight.Bold,
        color = color,
    )
}

@Composable
private fun TuningMeter(cents: Float, state: TunerState) {
    val animatedCents by animateFloatAsState(
        targetValue = cents.coerceIn(-50f, 50f),
        animationSpec = tween(80),
    )
    val needleFraction = ((animatedCents + 50f) / 100f).coerceIn(0f, 1f)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        val barHeight = 20f
        val barTop = (size.height - barHeight) / 2
        val barRect = Size(size.width, barHeight)
        val barOffset = Offset(0f, barTop)

        // Meter bar
        drawRoundRect(
            brush = Brush.horizontalGradient(
                0f to TuneRed,
                0.25f to TuneYellow,
                0.45f to TuneGreen,
                0.55f to TuneGreen,
                0.75f to TuneYellow,
                1f to TuneRed,
            ),
            topLeft = barOffset,
            size = barRect,
            cornerRadius = CornerRadius(barHeight / 2),
        )

        // Needle
        val needleX = needleFraction * size.width
        drawLine(
            color = Color.White,
            start = Offset(needleX, 0f),
            end = Offset(needleX, size.height),
            strokeWidth = 3f,
            cap = StrokeCap.Round,
        )

        // Center tick mark
        drawLine(
            color = Color.White.copy(alpha = 0.4f),
            start = Offset(size.width / 2, 0f),
            end = Offset(size.width / 2, size.height),
            strokeWidth = 1f,
        )
    }
}

@Composable
private fun CentsFrequencyRow(cents: Float, frequency: Float, state: TunerState) {
    val color = centsColor(abs(cents), state)
    val centsText = if (state == TunerState.IDLE || state == TunerState.LISTENING) {
        "-- 音分"
    } else {
        "${if (cents >= 0) "+" else ""}${"%.1f".format(cents)} 音分"
    }
    val freqText = if (frequency > 0f) "${"%.1f".format(frequency)} Hz" else "-- Hz"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(text = centsText, color = color, fontSize = 18.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.width(32.dp))
        Text(text = freqText, color = Color.Gray, fontSize = 18.sp)
    }
}

@Composable
private fun StartStopButton(isRunning: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = CircleShape,
        modifier = Modifier.size(72.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isRunning) TuneRed else TuneGreen,
        ),
    ) {
        Icon(
            imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
            contentDescription = if (isRunning) "Stop" else "Start",
            modifier = Modifier.size(32.dp),
            tint = Color.Black,
        )
    }
}

private fun centsColor(absCents: Float, state: TunerState): Color {
    if (state == TunerState.IDLE || state == TunerState.LISTENING) return Color.Gray
    if (state == TunerState.LOCKED) return TuneGreen
    return when {
        absCents <= 2.5f -> TuneGreen
        absCents <= 10f -> TuneYellow
        absCents <= 25f -> TuneOrange
        else -> TuneRed
    }
}
