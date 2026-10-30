package com.webscare.volumenormalizer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin

private val WaveGreen = Color(0xFF7AE829)
private val WaveBackground = Color(0xFF0B0D10)

@Composable
fun AnimatedWave(modifier: Modifier = Modifier) {
    var phase by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        var lastFrame = 0L

        while (true) {
            withFrameNanos { frame ->
                if (lastFrame != 0L) {
                    val delta =
                        (frame - lastFrame) / 1_000_000_000f

                    phase += delta * 1.8f
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
            modifier = Modifier.fillMaxSize()
        ) {
            val w = size.width
            val h = size.height

            if (w <= 0f || h <= 0f) return@Canvas

            val lineCount = 60
            val segmentCount = (w / 4f).toInt()
                .coerceIn(160, 700)

            val centerY = h * 0.52f
            val scaleY = minOf(w * 0.25f, h * 0.38f)

            val paths = ArrayList<Path>(lineCount)

            // Create the flowing ribbon strands.
            for (i in 0 until lineCount) {
                val ratio = i.toFloat() / (lineCount - 1)
                val inverse = 1f - ratio
                val path = Path()

                for (s in 0..segmentCount) {
                    val u = s.toFloat() / segmentCount
                    val x = u * w

                    val wavePhase =
                        u * PI.toFloat() * 7.7f - 0.55f

                    val carrier = sin(
                        wavePhase -
                                phase * 0.35f +
                                inverse * 0.8f
                    )

                    val slow = cos(
                        u * PI.toFloat() * 2f -
                                phase * 0.7f
                    ) * 0.08f

                    val growth =
                        0.30f + 0.70f * u.pow(1.08f)

                    val peakBoost = 1f + 0.32f * exp(
                        -((u - 0.88f) / 0.14f).pow(2)
                    )

                    val baseline =
                        -0.04f * exp(-(u / 0.25f).pow(2)) +
                                0.09f * exp(
                            -((u - 0.65f) / 0.18f).pow(2)
                        ) -
                                0.08f * exp(
                            -((u - 0.90f) / 0.15f).pow(2)
                        )

                    val spine =
                        carrier * growth * peakBoost +
                                slow + baseline

                    val fanDirection =
                        -carrier * (0.48f + 0.28f * u) +
                                0.24f * cos(
                            u * PI.toFloat() * 1.15f
                        )

                    val fanOffset =
                        inverse * 2.2f * 0.38f * fanDirection

                    val y = centerY +
                            (spine + fanOffset) * scaleY

                    if (s == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }

                paths.add(path)
            }

            // Soft neon glow behind the brightest strands.
            for (i in paths.indices) {
                val ratio = i.toFloat() / (lineCount - 1)

                if (ratio >= 0.96f) {
                    drawPath(
                        path = paths[i],
                        color = WaveGreen.copy(
                            alpha = if (ratio > 0.99f) {
                                0.20f
                            } else {
                                0.08f
                            }
                        ),
                        style = Stroke(
                            width = 14f,
                            cap = StrokeCap.Round
                        )
                    )
                }
            }

            // Draw the neon ribbon lines.
            for (i in paths.indices) {
                val ratio = i.toFloat() / (lineCount - 1)

                val alpha = (
                        0.10f +
                                ratio.pow(1.65f) * 0.48f +
                                if (i == lineCount - 1) 0.35f
                                else 0f
                        ).coerceIn(0f, 1f)

                drawPath(
                    path = paths[i],
                    color = WaveGreen.copy(alpha = alpha),
                    style = Stroke(
                        width = if (i == lineCount - 1) {
                            2.1f
                        } else {
                            1.05f + ratio * 0.45f
                        },
                        cap = StrokeCap.Round
                    )
                )
            }
        }
    }
}