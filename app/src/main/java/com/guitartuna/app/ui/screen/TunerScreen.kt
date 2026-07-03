package com.guitartuna.app.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
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
    darkTheme: Boolean,
    onSelectString: (Int) -> Unit,
    onToggleAuto: () -> Unit,
    onToggleReference: () -> Unit,
    onToggleTheme: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TopBar(
            isAuto = uiState.mode == TuningMode.AUTO,
            darkTheme = darkTheme,
            onToggleAuto = onToggleAuto,
            onToggleTheme = onToggleTheme,
        )
        Spacer(modifier = Modifier.height(12.dp))
        StringSelector(
            selectedIndex = uiState.selectedStringIndex,
            autoDetectedIndex = uiState.autoDetectedIndex,
            mode = uiState.mode,
            onSelect = onSelectString,
        )
        Spacer(modifier = Modifier.weight(0.3f))
        NoteDisplay(
            note = uiState.detectedNote,
            cents = uiState.centsFromTarget,
            state = uiState.tunerState,
        )
        Spacer(modifier = Modifier.height(8.dp))
        TuningMeter(cents = uiState.centsFromTarget, state = uiState.tunerState)
        Spacer(modifier = Modifier.height(6.dp))
        DirectionHint(cents = uiState.centsFromTarget, state = uiState.tunerState)
        Spacer(modifier = Modifier.height(6.dp))
        CentsFrequencyRow(cents = uiState.centsFromTarget, frequency = uiState.detectedFrequency, state = uiState.tunerState)
        Spacer(modifier = Modifier.height(12.dp))
        ReferenceToneButton(
            isPlaying = uiState.isPlayingReference,
            targetNote = uiState.targetNote,
            onClick = onToggleReference,
        )
        Spacer(modifier = Modifier.weight(0.3f))
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun TopBar(isAuto: Boolean, darkTheme: Boolean, onToggleAuto: () -> Unit, onToggleTheme: () -> Unit) {
    val dimColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        Text(
            text = "自动",
            fontSize = 14.sp,
            color = if (isAuto) TuneGreen else dimColor,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Switch(
            checked = isAuto,
            onCheckedChange = { onToggleAuto() },
            modifier = Modifier.padding(start = 0.dp),
            colors = SwitchDefaults.colors(
                checkedThumbColor = TuneGreen,
                checkedTrackColor = TuneGreen.copy(alpha = 0.3f),
            ),
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(onClick = onToggleTheme, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = if (darkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                contentDescription = if (darkTheme) "切换亮色主题" else "切换暗色主题",
                tint = dimColor,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun StringSelector(
    selectedIndex: Int,
    autoDetectedIndex: Int?,
    mode: TuningMode,
    onSelect: (Int) -> Unit,
) {
    val gaugeWidths = listOf(9f, 7f, 5.6f, 4f, 2.8f, 2f) // E A D G B e
    val labels = listOf("E", "A", "D", "G", "B", "e")
    val isAuto = mode == TuningMode.AUTO

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom,
    ) {
        labels.forEachIndexed { index, label ->
            val isSelected = if (isAuto) autoDetectedIndex == index else selectedIndex == index
            StringChipWithGauge(
                label = label,
                selected = isSelected,
                gaugeDp = gaugeWidths[index],
                onClick = { onSelect(index) },
            )
        }
    }
}

@Composable
private fun StringChipWithGauge(label: String, selected: Boolean, gaugeDp: Float, onClick: () -> Unit) {
    val lineColor = if (selected) TuneGreen else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 2.dp),
    ) {
        FilterChip(
            selected = selected,
            onClick = onClick,
            label = { Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = TuneGreen.copy(alpha = 0.2f),
                selectedLabelColor = TuneGreen,
            ),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(gaugeDp.dp)
                .height(14.dp)
                .background(lineColor, CircleShape),
        )
    }
}

@Composable
private fun NoteDisplay(note: String, cents: Float, state: TunerState) {
    val dimColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
    val color by animateColorAsState(
        targetValue = centsColor(abs(cents), state, dimColor),
        animationSpec = tween(80),
    )
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale = if (state == TunerState.LOCKED) {
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(animation = tween(500), repeatMode = RepeatMode.Reverse),
        ).value
    } else 1f

    Text(
        text = note,
        fontSize = 72.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = if (state == TunerState.LOCKED) {
            Modifier
                .shadow(elevation = 16.dp, ambientColor = TuneGreen, spotColor = TuneGreen)
                .graphicsLayer { scaleX = pulseScale; scaleY = pulseScale }
        } else Modifier,
    )
}

@Composable
private fun TuningMeter(cents: Float, state: TunerState) {
    val animatedCents by animateFloatAsState(
        targetValue = cents.coerceIn(-50f, 50f),
        animationSpec = tween(30),
    )
    val needleFraction = ((animatedCents + 50f) / 100f).coerceIn(0f, 1f)
    val needleColor = MaterialTheme.colorScheme.onSurface
    val tickColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)

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
            color = needleColor,
            start = Offset(needleX, 0f),
            end = Offset(needleX, size.height),
            strokeWidth = 3f,
            cap = StrokeCap.Round,
        )

        // Center tick mark
        drawLine(
            color = tickColor,
            start = Offset(size.width / 2, 0f),
            end = Offset(size.width / 2, size.height),
            strokeWidth = 1f,
        )
    }
}

@Composable
private fun CentsFrequencyRow(cents: Float, frequency: Float, state: TunerState) {
    val dimColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    val color = centsColor(abs(cents), state, dimColor)
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
        Text(text = freqText, color = dimColor, fontSize = 18.sp)
    }
}

@Composable
private fun DirectionHint(cents: Float, state: TunerState) {
    if (state == TunerState.IDLE || state == TunerState.LISTENING) return

    val dimColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
    val absCents = abs(cents)
    val isInTune = absCents <= 2.5f
    val isFlat = cents < -2.5f
    val isSharp = cents > 2.5f

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "◀ 偏低",
            fontSize = 16.sp,
            fontWeight = if (isFlat) FontWeight.Bold else FontWeight.Normal,
            color = if (isFlat) TuneOrange else dimColor,
        )
        Text(
            text = "● 准确",
            fontSize = 16.sp,
            fontWeight = if (isInTune) FontWeight.Bold else FontWeight.Normal,
            color = if (isInTune) TuneGreen else dimColor,
        )
        Text(
            text = "偏高 ▶",
            fontSize = 16.sp,
            fontWeight = if (isSharp) FontWeight.Bold else FontWeight.Normal,
            color = if (isSharp) TuneOrange else dimColor,
        )
    }
}

@Composable
private fun ReferenceToneButton(isPlaying: Boolean, targetNote: String, onClick: () -> Unit) {
    val dimColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    val buttonBg = if (isPlaying) TuneOrange else MaterialTheme.colorScheme.surfaceVariant
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "参考音",
            fontSize = 12.sp,
            color = dimColor,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Button(
            onClick = onClick,
            shape = CircleShape,
            modifier = Modifier.size(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = buttonBg,
            ),
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = "播放参考音 $targetNote",
                modifier = Modifier.size(24.dp),
                tint = if (isPlaying) Color.Black else TuneGreen,
            )
        }
    }
}

private fun centsColor(absCents: Float, state: TunerState, dimColor: Color = Color.Gray): Color {
    if (state == TunerState.IDLE || state == TunerState.LISTENING) return dimColor
    if (state == TunerState.LOCKED) return TuneGreen
    return when {
        absCents <= 2.5f -> TuneGreen
        absCents <= 10f -> TuneYellow
        absCents <= 25f -> TuneOrange
        else -> TuneRed
    }
}
