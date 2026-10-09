package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.GppMaybe
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RiskStatus
import com.example.ui.theme.CyberPink
import com.example.ui.theme.CyberTeal
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.HighRiskRed
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.SuspiciousAmber
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun CyberGridBackground(
    isDark: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cyber_grid_bg")
    val gridShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "grid_shift"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.20f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 44.dp.toPx()
            val lineColor = if (isDark) {
                ElectricCyan.copy(alpha = 0.055f)
            } else {
                ElectricBlue.copy(alpha = 0.06f)
            }

            // Ambient glowing radial orbs
            if (isDark) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(ElectricCyan.copy(alpha = glowAlpha), Color.Transparent),
                        center = Offset(size.width * 0.85f, size.height * 0.12f),
                        radius = size.width * 0.65f
                    ),
                    center = Offset(size.width * 0.85f, size.height * 0.12f),
                    radius = size.width * 0.65f
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(NeonPurple.copy(alpha = glowAlpha * 0.9f), Color.Transparent),
                        center = Offset(size.width * 0.15f, size.height * 0.78f),
                        radius = size.width * 0.7f
                    ),
                    center = Offset(size.width * 0.15f, size.height * 0.78f),
                    radius = size.width * 0.7f
                )
            }

            val yOffset = gridShift * step
            var y = (yOffset % step) - step
            while (y < size.height + step) {
                drawLine(
                    color = lineColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
                y += step
            }

            var x = 0f
            while (x < size.width) {
                drawLine(
                    color = lineColor,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1f
                )
                x += step
            }
        }
        content()
    }
}

@Composable
fun AnimatedCircularScoreMeter(
    score: Int,
    label: String,
    subLabel: String,
    isSecurityScore: Boolean = true, // true = 100 is best (Green); false = Risk Score where 100 is High Risk (Red)
    size: Dp = 165.dp
) {
    val animatedScore by animateFloatAsState(
        targetValue = score.coerceIn(0, 100).toFloat(),
        animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
        label = "score_gauge"
    )

    val primaryColor = if (isSecurityScore) {
        when {
            score >= 75 -> SafeGreen
            score >= 45 -> SuspiciousAmber
            else -> HighRiskRed
        }
    } else {
        when {
            score >= 65 -> HighRiskRed
            score >= 32 -> SuspiciousAmber
            else -> SafeGreen
        }
    }

    val secondaryColor = if (isSecurityScore) ElectricCyan else NeonPurple

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(size)
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
            val strokeWidth = 14.dp.toPx()
            val arcSize = Size(this.size.width - strokeWidth, this.size.height - strokeWidth)
            val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

            // Track arc
            drawArc(
                color = primaryColor.copy(alpha = 0.16f),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Active gradient arc
            val sweep = (animatedScore / 100f) * 270f
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(secondaryColor, primaryColor, primaryColor)
                ),
                startAngle = 135f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${animatedScore.toInt()}",
                style = MaterialTheme.typography.displayMedium.copy(
                    color = primaryColor,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PulsingStatusShield(
    status: RiskStatus,
    modifier: Modifier = Modifier,
    size: Dp = 84.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shield_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (status == RiskStatus.HIGH_RISK) 620 else 1300,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val color = when (status) {
        RiskStatus.SAFE -> SafeGreen
        RiskStatus.SUSPICIOUS -> SuspiciousAmber
        RiskStatus.HIGH_RISK -> HighRiskRed
    }

    val icon = when (status) {
        RiskStatus.SAFE -> Icons.Default.GppGood
        RiskStatus.SUSPICIOUS -> Icons.Default.GppMaybe
        RiskStatus.HIGH_RISK -> Icons.Default.GppBad
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size * 1.3f)
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(color.copy(alpha = 0.42f), Color.Transparent)
                    )
                )
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(size * 0.85f)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.18f))
                .border(2.dp, color, CircleShape)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = status.label,
                tint = color,
                modifier = Modifier.size(size * 0.5f)
            )
        }
    }
}

@Composable
fun AnimatedRadarScannerOverlay(
    scannerTitle: String,
    stepText: String,
    isDemo: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar_sweep")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_angle"
    )

    val laserY by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        ElectricCyan.copy(alpha = 0.12f),
                        NeonPurple.copy(alpha = 0.14f)
                    )
                )
            )
            .border(1.5.dp, ElectricCyan.copy(alpha = 0.55f), RoundedCornerShape(24.dp))
            .padding(28.dp)
    ) {
        if (isDemo) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(SuspiciousAmber.copy(alpha = 0.2f))
                    .border(1.dp, SuspiciousAmber, RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "DEMO SIMULATION MODE",
                    style = MaterialTheme.typography.labelMedium,
                    color = SuspiciousAmber
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(190.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxRadius = min(size.width, size.height) / 2f

                // Radar concentric rings
                for (i in 1..3) {
                    drawCircle(
                        color = ElectricCyan.copy(alpha = 0.25f),
                        radius = maxRadius * (i / 3f),
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }

                // Crosshairs
                drawLine(
                    color = ElectricCyan.copy(alpha = 0.2f),
                    start = Offset(0f, center.y),
                    end = Offset(size.width, center.y),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = ElectricCyan.copy(alpha = 0.2f),
                    start = Offset(center.x, 0f),
                    end = Offset(center.x, size.height),
                    strokeWidth = 1.dp.toPx()
                )

                // Rotating sweep arc
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(
                            Color.Transparent,
                            ElectricCyan.copy(alpha = 0.05f),
                            ElectricCyan.copy(alpha = 0.45f)
                        ),
                        center = center
                    ),
                    startAngle = rotation,
                    sweepAngle = 90f,
                    useCenter = true,
                    topLeft = Offset(center.x - maxRadius, center.y - maxRadius),
                    size = Size(maxRadius * 2, maxRadius * 2)
                )

                // Horizontal scanning laser beam
                val beamY = size.height * laserY
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(Color.Transparent, ElectricCyan, NeonPurple, Color.Transparent)
                    ),
                    start = Offset(0f, beamY),
                    end = Offset(size.width, beamY),
                    strokeWidth = 3.dp.toPx()
                )
            }

            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = "Scanning",
                tint = ElectricCyan,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Running $scannerTitle...",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stepText,
            style = MaterialTheme.typography.bodyMedium,
            color = ElectricCyan,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun AnimatedThreatRadarChart(
    axisScores: Map<String, Float>, // 6 categories, each 0f..1f
    hasData: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "threat_radar_sweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_sweep_angle"
    )

    val categories = listOf(
        "Phishing",
        "Social Scams",
        "Financial Scams",
        "Fake Identities",
        "Malware Risk",
        "Media Manipulation"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
        ) {
            Canvas(modifier = Modifier.size(195.dp)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = min(size.width, size.height) / 2f * 0.85f
                val count = categories.size

                // Concentric hexagon web
                for (level in 1..3) {
                    val r = radius * (level / 3f)
                    val hexPath = Path()
                    for (i in 0 until count) {
                        val angleRad = (2 * PI * i / count) - (PI / 2)
                        val x = center.x + (r * cos(angleRad)).toFloat()
                        val y = center.y + (r * sin(angleRad)).toFloat()
                        if (i == 0) hexPath.moveTo(x, y) else hexPath.lineTo(x, y)
                    }
                    hexPath.close()
                    drawPath(
                        path = hexPath,
                        color = ElectricCyan.copy(alpha = 0.22f),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }

                // Axis spokes
                for (i in 0 until count) {
                    val angleRad = (2 * PI * i / count) - (PI / 2)
                    val endX = center.x + (radius * cos(angleRad)).toFloat()
                    val endY = center.y + (radius * sin(angleRad)).toFloat()
                    drawLine(
                        color = ElectricCyan.copy(alpha = 0.22f),
                        start = center,
                        end = Offset(endX, endY),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Rotating radar sweep line
                val sweepRad = sweepAngle * (PI / 180.0)
                val sweepX = center.x + (radius * cos(sweepRad)).toFloat()
                val sweepY = center.y + (radius * sin(sweepRad)).toFloat()
                drawLine(
                    color = ElectricCyan.copy(alpha = 0.55f),
                    start = center,
                    end = Offset(sweepX, sweepY),
                    strokeWidth = 2.dp.toPx()
                )

                // Polygon of detected threat levels
                if (hasData) {
                    val dataPath = Path()
                    for (i in 0 until count) {
                        val cat = categories[i]
                        val value = (axisScores[cat] ?: 0.15f).coerceIn(0.12f, 1f)
                        val angleRad = (2 * PI * i / count) - (PI / 2)
                        val ptX = center.x + (radius * value * cos(angleRad)).toFloat()
                        val ptY = center.y + (radius * value * sin(angleRad)).toFloat()
                        if (i == 0) dataPath.moveTo(ptX, ptY) else dataPath.lineTo(ptX, ptY)
                    }
                    dataPath.close()

                    drawPath(
                        path = dataPath,
                        brush = Brush.radialGradient(
                            colors = listOf(
                                ElectricCyan.copy(alpha = 0.42f),
                                NeonPurple.copy(alpha = 0.36f),
                                CyberPink.copy(alpha = 0.28f)
                            ),
                            center = center,
                            radius = radius
                        )
                    )
                    drawPath(
                        path = dataPath,
                        color = ElectricCyan,
                        style = Stroke(width = 2.2.dp.toPx())
                    )
                }
            }
        }

        // Category Legend Chips
        val pairs = categories.chunked(2)
        pairs.forEach { rowCats ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowCats.forEach { cat ->
                    val pct = ((axisScores[cat] ?: 0f) * 100).toInt()
                    val dotColor = when {
                        pct >= 65 -> HighRiskRed
                        pct >= 32 -> SuspiciousAmber
                        pct > 0 -> CyberTeal
                        else -> ElectricCyan.copy(alpha = 0.5f)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = cat,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = if (hasData) "$pct%" else "--",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = dotColor
                        )
                    }
                }
            }
        }
    }
}
