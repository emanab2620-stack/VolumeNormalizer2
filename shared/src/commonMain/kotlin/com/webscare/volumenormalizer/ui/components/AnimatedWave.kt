
package com.webscare.volumenormalizer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private val WaveGreen = Color(0xFF7AE829)
private val WaveBackground = Color(0xFF0B0D10)

@Composable
fun AnimatedWave(modifier: Modifier = Modifier) {
    var lines by remember { mutableFloatStateOf(60f) }
    var speed by remember { mutableFloatStateOf(2f) }
    var amplitude by remember { mutableFloatStateOf(0.70f) }
    var frequency by remember { mutableFloatStateOf(2f) }
    var spread by remember { mutableFloatStateOf(2.20f) }
    var glow by remember { mutableFloatStateOf(45f) }

    var waveColor by remember { mutableStateOf(WaveGreen) }
    var cursorPull by remember { mutableStateOf(true) }
    var paused by remember { mutableStateOf(false) }
    var showPanel by remember { mutableStateOf(true) }

    var phase by remember { mutableFloatStateOf(0f) }
    var pointerX by remember { mutableFloatStateOf(-1000f) }
    var pointerY by remember { mutableFloatStateOf(-1000f) }

    LaunchedEffect(paused, speed) {
        var lastFrame = 0L

        while (true) {
            withFrameNanos { frame ->
                if (lastFrame != 0L && !paused) {
                    val delta = (frame - lastFrame) / 1_000_000_000f
                    phase += delta * speed * 1.4f
                }
                lastFrame = frame
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WaveBackground)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(cursorPull) {
                    detectDragGestures(
                        onDragStart = {
                            pointerX = it.x
                            pointerY = it.y
                        },
                        onDragEnd = {
                            pointerX = -1000f
                            pointerY = -1000f
                        },
                        onDragCancel = {
                            pointerX = -1000f
                            pointerY = -1000f
                        },
                        onDrag = { change, _ ->
                            pointerX = change.position.x
                            pointerY = change.position.y
                        }
                    )
                }
        ) {
            val w = size.width
            val h = size.height

            if (w <= 0f || h <= 0f) return@Canvas

            val count = lines.toInt().coerceAtLeast(2)
            val segmentCount = (w / 4f).toInt().coerceIn(160, 700)
            val centerY = h * 0.53f
            val scaleY = minOf(w * 0.45f, h * 0.72f) * amplitude

            val paths = ArrayList<Path>(count)

            for (i in 0 until count) {
                val ratio = i.toFloat() / (count - 1)
                val inverse = 1f - ratio
                val path = Path()

                for (s in 0..segmentCount) {
                    val u = s.toFloat() / segmentCount
                    val x = u * w

                    val wavePhase =
                        u * PI.toFloat() * 3.85f * frequency - 0.55f

                    val carrier = sin(
                        wavePhase - phase * 0.35f +
                                inverse * 0.36f * spread
                    )

                    val slow = cos(
                        u * PI.toFloat() * 2f - phase * 0.7f
                    ) * 0.08f

                    val growth = 0.30f + 0.70f * u.pow(1.08f)

                    val peakBoost = 1f + 0.32f * exp(
                        -((u - 0.88f) / 0.14f).pow(2)
                    )

                    val baseline =
                        -0.04f * exp(-(u / 0.25f).pow(2)) +
                                0.09f * exp(-((u - 0.65f) / 0.18f).pow(2)) -
                                0.08f * exp(-((u - 0.90f) / 0.15f).pow(2))

                    val spine =
                        carrier * growth * peakBoost + slow + baseline

                    val fanDirection =
                        -carrier * (0.48f + 0.28f * u) +
                                0.24f * cos(u * PI.toFloat() * 1.15f)

                    val fanOffset =
                        inverse * spread * 0.38f * fanDirection

                    var y = centerY + (spine + fanOffset) * scaleY

                    if (cursorPull && pointerX >= 0f) {
                        val dx = x - pointerX
                        val dy = y - pointerY
                        val distance = sqrt(dx * dx + dy * dy)
                        val radius = 220f

                        if (distance < radius) {
                            val influence =
                                (1f - distance / radius).pow(2)

                            y += (pointerY - y) *
                                    influence * 0.28f *
                                    (0.5f + 0.5f * ratio)
                        }
                    }

                    if (s == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }

                paths.add(path)
            }

            // Simulated neon glow using translucent strokes.
            // This avoids Android-only graphics classes.
            for (i in paths.indices) {
                val ratio = i.toFloat() / (count - 1)

                if (ratio >= 0.96f && glow > 0f) {
                    drawPath(
                        path = paths[i],
                        color = waveColor.copy(
                            alpha = if (ratio > 0.99f) 0.25f else 0.10f
                        ),
                        style = Stroke(
                            width = (glow / 45f) * 12f + 2f,
                            cap = StrokeCap.Round
                        )
                    )
                }
            }

            // Main ribbon strands.
            for (i in paths.indices) {
                val ratio = i.toFloat() / (count - 1)

                val alpha = (
                        0.10f +
                                ratio.pow(1.65f) * 0.48f +
                                if (i == count - 1) 0.35f else 0f
                        ).coerceIn(0f, 1f)

                drawPath(
                    path = paths[i],
                    color = waveColor.copy(alpha = alpha),
                    style = Stroke(
                        width = if (i == count - 1) 2.1f else
                            1.05f + ratio * 0.45f,
                        cap = StrokeCap.Round
                    )
                )
            }
        }

        if (showPanel) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .fillMaxWidth(0.88f)
                    .widthIn(max = 320.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xE60F1318))
                    .border(
                        1.dp,
                        waveColor.copy(alpha = 0.20f),
                        RoundedCornerShape(18.dp)
                    )
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .background(waveColor, RoundedCornerShape(50))
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(
                        "Ribbon Wave",
                        color = Color.White,
                        fontSize = 16.sp,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        "×",
                        color = Color.LightGray,
                        modifier = Modifier.clickable {
                            showPanel = false
                        }
                    )
                }

                WaveSlider("Line Count", lines, 15f..60f, 0) {
                    lines = it
                }

                WaveSlider("Flow Speed", speed, 0f..2f, 2) {
                    speed = it
                }

                WaveSlider("Wave Amplitude", amplitude, 0.3f..1.8f, 2) {
                    amplitude = it
                }

                WaveSlider("Frequency", frequency, 0.5f..2f, 2) {
                    frequency = it
                }

                WaveSlider("Ribbon Spread", spread, 0.2f..2.2f, 2) {
                    spread = it
                }

                WaveSlider("Glow Intensity", glow, 0f..45f, 0) {
                    glow = it
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(waveColor)
                            .clickable {
                                waveColor = when (waveColor) {
                                    WaveGreen -> Color(0xFF00E5FF)
                                    Color(0xFF00E5FF) -> Color(0xFFB14CFF)
                                    else -> WaveGreen
                                }
                            }
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(
                        "Wave Color",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        "Cursor Pull",
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )

                    Switch(
                        checked = cursorPull,
                        onCheckedChange = { cursorPull = it }
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { paused = !paused },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF292D30)
                        )
                    ) {
                        Text(if (paused) "Play" else "Pause")
                    }

                    Button(
                        onClick = {
                            lines = 60f
                            speed = 2f
                            amplitude = 0.70f
                            frequency = 2f
                            spread = 2.20f
                            glow = 45f
                            waveColor = WaveGreen
                            cursorPull = true
                            paused = false
                            phase = 0f
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF292D30)
                        )
                    ) {
                        Text("Reset Reference", fontSize = 10.sp)
                    }
                }
            }
        } else {
            Button(
                onClick = { showPanel = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
            ) {
                Text("Controls")
            }
        }
    }
}

@Composable
private fun WaveSlider(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    decimals: Int,
    onChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, color = Color.LightGray, fontSize = 12.sp)

            val shownValue = if (decimals == 0) {
                value.roundToInt().toString()
            } else {
                val rounded = (value * 100).roundToInt() / 100f
                val whole = rounded.toInt()
                val fraction = (rounded * 100).roundToInt() % 100
                "$whole.${fraction.toString().padStart(2, '0')}"
            }

            Text(
                shownValue,
                color = Color(0xFF9AF34B),
                fontSize = 12.sp
            )
        }

        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            colors = androidx.compose.material3.SliderDefaults.colors(
                thumbColor = Color(0xFF84EB34),
                activeTrackColor = Color(0xFF84EB34),
                inactiveTrackColor = Color(0xFF36393C)
            )
        )
    }
}
