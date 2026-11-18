package com.dionlobo.watchtogether

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object Brand {
    val Ink = Color(0xFF12161C)
    val InkSoft = Color(0xFF1C222B)
    val Paper = Color(0xFFF5F0E6)
    val PaperDim = Color(0xFFE8E0D2)
    val Amber = Color(0xFFFFB454)
    val Mint = Color(0xFF6EE7C3)
    val Coral = Color(0xFFFF806B)
    val Muted = Color(0xFF8C95A3)
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val colors = androidx.compose.material3.darkColorScheme(
        primary = Brand.Amber,
        onPrimary = Brand.Ink,
        secondary = Brand.Mint,
        onSecondary = Brand.Ink,
        tertiary = Brand.Coral,
        background = Brand.Ink,
        onBackground = Brand.Paper,
        surface = Brand.InkSoft,
        onSurface = Brand.Paper,
        surfaceVariant = Color(0xFF262E39),
        onSurfaceVariant = Color(0xFFAAB3C0),
        outline = Color(0xFF3A4452)
    )
    val typography = Typography(
        displayLarge = TextStyle(fontSize = 46.sp, fontWeight = FontWeight.Black, letterSpacing = (-1.5).sp),
        headlineLarge = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.7).sp),
        headlineMedium = TextStyle(fontSize = 23.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp),
        titleLarge = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
        bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 22.sp, lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)),
        labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    )
    MaterialTheme(colorScheme = colors, typography = typography, content = content)
}

@Composable
fun AnimatedEnter(delayMs: Int = 0, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(420, delayMs))
    val offset by animateFloatAsState(if (visible) 0f else 26f, tween(480, delayMs))
    Box(Modifier.alpha(alpha).offset(y = offset.dp)) { content() }
}

@Composable
fun ProgressRing(progress: Float, size: Dp = 72.dp) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), tween(900))
    val track = MaterialTheme.colorScheme.surfaceVariant
    val fill = Brush.sweepGradient(listOf(Brand.Mint, Brand.Amber, Brand.Mint))
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 7.dp.toPx()
            val inset = stroke / 2
            drawArc(track, 0f, 360f, false, Offset(inset, inset), Size(this.size.width - stroke, this.size.height - stroke), style = Stroke(stroke))
            drawArc(fill, -90f, 360f * animated, false, Offset(inset, inset), Size(this.size.width - stroke, this.size.height - stroke), style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Text("${(animated * 100).toInt()}%", style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun Avatar(initials: String, color: Color) {
    Box(
        Modifier.size(34.dp).clip(CircleShape).background(color).border(2.dp, Brand.Ink),
        contentAlignment = Alignment.Center
    ) {
        Text(initials, color = Brand.Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AccentDivider() {
    Box(
        Modifier.padding(vertical = 4.dp).size(height = 3.dp, width = 46.dp)
            .clip(CircleShape).background(Brush.horizontalGradient(listOf(Brand.Amber, Brand.Coral)))
    )
}

@Composable
fun DecorBackground(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(Brand.Ink)) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Brand.Amber.copy(alpha = 0.06f), radius = size.minDimension * 0.45f, center = Offset(size.width, 0f))
            drawCircle(Brand.Mint.copy(alpha = 0.05f), radius = size.minDimension * 0.38f, center = Offset(0f, size.height))
        }
        content()
    }
}
