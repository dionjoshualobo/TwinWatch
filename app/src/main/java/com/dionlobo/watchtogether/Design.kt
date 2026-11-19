package com.dionlobo.watchtogether

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal data class WatchPalette(
    val ink: Color,
    val inkSoft: Color,
    val paper: Color,
    val paperDim: Color,
    val blue: Color,
    val amber: Color,
    val mint: Color,
    val coral: Color,
    val muted: Color,
    val border: Color,
    val surface: Color,
    val onPrimary: Color,
    val onAccent: Color,
    val stroke: Color,
    val shadow: Color,
    val inverseSurface: Color,
    val inverseOnSurface: Color,
    val skeleton: Color,
    val imageScrim: Color
)

private val LightWatchPalette = WatchPalette(
    ink = Color(0xFF17202A),
    inkSoft = Color(0xFFEFF1EC),
    paper = Color(0xFFF8F7F2),
    paperDim = Color(0xFFDDE1DB),
    blue = Color(0xFF2563EB),
    amber = Color(0xFFF5C518),
    mint = Color(0xFF00A88E),
    coral = Color(0xFFFF6B57),
    muted = Color(0xFF5F6B7A),
    border = Color(0xFFD6DBE2),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color(0xFFFFFFFF),
    onAccent = Color(0xFF17202A),
    stroke = Color(0xFF17202A),
    shadow = Color(0x3817202A),
    inverseSurface = Color(0xFF17202A),
    inverseOnSurface = Color(0xFFFFFFFF),
    skeleton = Color(0xFF17202A),
    imageScrim = Color.Transparent
)

private val DarkWatchPalette = WatchPalette(
    ink = Color(0xFFF3F6FA),
    inkSoft = Color(0xFF22303F),
    paper = Color(0xFF0F151C),
    paperDim = Color(0xFF2B3949),
    blue = Color(0xFF7AA2FF),
    amber = Color(0xFFFFD45C),
    mint = Color(0xFF4DD9C0),
    coral = Color(0xFFFF8C7A),
    muted = Color(0xFFA9B6C7),
    border = Color(0xFF3A4858),
    surface = Color(0xFF1B2531),
    onPrimary = Color(0xFF0B1118),
    onAccent = Color(0xFF0B1118),
    stroke = Color(0xFF0A0F15),
    shadow = Color(0x99000000),
    inverseSurface = Color(0xFFE8EDF4),
    inverseOnSurface = Color(0xFF111922),
    skeleton = Color(0xFF6B7B8F),
    imageScrim = Color(0x33000000)
)

internal val LocalWatchPalette = compositionLocalOf { LightWatchPalette }

object Brand {
    @get:Composable val Ink: Color get() = LocalWatchPalette.current.ink
    @get:Composable val InkSoft: Color get() = LocalWatchPalette.current.inkSoft
    @get:Composable val Paper: Color get() = LocalWatchPalette.current.paper
    @get:Composable val PaperDim: Color get() = LocalWatchPalette.current.paperDim
    @get:Composable val Blue: Color get() = LocalWatchPalette.current.blue
    @get:Composable val Amber: Color get() = LocalWatchPalette.current.amber
    @get:Composable val Mint: Color get() = LocalWatchPalette.current.mint
    @get:Composable val Coral: Color get() = LocalWatchPalette.current.coral
    @get:Composable val Muted: Color get() = LocalWatchPalette.current.muted
    @get:Composable val Border: Color get() = LocalWatchPalette.current.border
    @get:Composable val Surface: Color get() = LocalWatchPalette.current.surface
    @get:Composable val OnPrimary: Color get() = LocalWatchPalette.current.onPrimary
    @get:Composable val OnAccent: Color get() = LocalWatchPalette.current.onAccent
    @get:Composable val Stroke: Color get() = LocalWatchPalette.current.stroke
    @get:Composable val Shadow: Color get() = LocalWatchPalette.current.shadow
    @get:Composable val InverseSurface: Color get() = LocalWatchPalette.current.inverseSurface
    @get:Composable val InverseOnSurface: Color get() = LocalWatchPalette.current.inverseOnSurface
    @get:Composable val Skeleton: Color get() = LocalWatchPalette.current.skeleton
    @get:Composable val ImageScrim: Color get() = LocalWatchPalette.current.imageScrim
}

@Composable
fun AppTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    val palette = if (darkTheme) DarkWatchPalette else LightWatchPalette
    val colors = if (darkTheme) {
        darkColorScheme(
            primary = palette.blue,
            onPrimary = palette.onPrimary,
            primaryContainer = palette.blue.copy(alpha = 0.22f),
            onPrimaryContainer = palette.ink,
            secondary = palette.mint,
            onSecondary = palette.onAccent,
            secondaryContainer = palette.mint.copy(alpha = 0.2f),
            onSecondaryContainer = palette.ink,
            tertiary = palette.coral,
            onTertiary = palette.onAccent,
            background = palette.paper,
            onBackground = palette.ink,
            surface = palette.surface,
            onSurface = palette.ink,
            surfaceVariant = palette.inkSoft,
            onSurfaceVariant = palette.muted,
            outline = palette.border
        )
    } else {
        lightColorScheme(
            primary = palette.blue,
            onPrimary = palette.onPrimary,
            primaryContainer = palette.blue.copy(alpha = 0.12f),
            onPrimaryContainer = palette.ink,
            secondary = palette.mint,
            onSecondary = palette.onAccent,
            secondaryContainer = palette.mint.copy(alpha = 0.14f),
            onSecondaryContainer = palette.ink,
            tertiary = palette.coral,
            onTertiary = palette.onAccent,
            background = palette.paper,
            onBackground = palette.ink,
            surface = palette.surface,
            onSurface = palette.ink,
            surfaceVariant = palette.inkSoft,
            onSurfaceVariant = palette.muted,
            outline = palette.border
        )
    }
    val typography = Typography(
        displayLarge = TextStyle(fontSize = 44.sp, fontWeight = FontWeight.Black, letterSpacing = (-1.8).sp),
        headlineLarge = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.8).sp),
        headlineMedium = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp),
        titleLarge = TextStyle(fontSize = 19.sp, fontWeight = FontWeight.Bold),
        bodyLarge = TextStyle(
            fontSize = 16.sp,
            lineHeight = 23.sp,
            lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)
        ),
        labelLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold)
    )
    val shapes = Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(20.dp),
        large = RoundedCornerShape(28.dp),
        extraLarge = RoundedCornerShape(36.dp)
    )
    CompositionLocalProvider(LocalWatchPalette provides palette) {
        MaterialTheme(colorScheme = colors, typography = typography, shapes = shapes, content = content)
    }
}

@Composable
private fun pressScale(pressed: Boolean, enabled: Boolean): Modifier {
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.965f else 1f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 560f),
        label = "pressScale"
    )
    return Modifier.graphicsLayer { scaleX = scale; scaleY = scale }
}

@Composable
fun PunchButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
    borderColor: Color = Color.Unspecified,
    enabled: Boolean = true,
    haptic: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    val palette = LocalWatchPalette.current
    val resolvedColor = if (color == Color.Unspecified) palette.blue else color
    val resolvedContentColor = if (contentColor == Color.Unspecified) palette.onPrimary else contentColor
    val resolvedBorderColor = if (borderColor == Color.Unspecified) palette.stroke else borderColor
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val feedback = LocalHapticFeedback.current
    val radius by animateDpAsState(
        if (pressed && enabled) 26.dp else 18.dp,
        spring(dampingRatio = 0.7f, stiffness = 520f),
        label = "buttonShape"
    )
    val shift by animateDpAsState(
        if (pressed && enabled) 4.dp else 0.dp,
        spring(dampingRatio = 0.7f, stiffness = 520f),
        label = "buttonShift"
    )
    val shadowShift by animateDpAsState(
        if (pressed && enabled) 0.dp else 5.dp,
        spring(dampingRatio = 0.7f, stiffness = 520f),
        label = "buttonShadow"
    )
    Box(modifier.height(56.dp).then(pressScale(pressed, enabled))) {
        Box(
            Modifier
                .matchParentSize()
                .offset(y = shadowShift)
                .clip(RoundedCornerShape(radius))
                .background(palette.shadow.copy(alpha = if (enabled) 1f else 0.4f))
        )
        Surface(
            shape = RoundedCornerShape(radius),
            color = if (enabled) resolvedColor else palette.paperDim,
            contentColor = if (enabled) resolvedContentColor else palette.muted,
            border = BorderStroke(2.dp, if (enabled) resolvedBorderColor else palette.border),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .offset(y = shift)
                .clip(RoundedCornerShape(radius))
                .clickable(
                    interactionSource = interaction,
                    indication = LocalIndication.current,
                    enabled = enabled
                ) {
                    if (haptic) feedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
        ) {
            Row(
                Modifier.padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
            ) {
                if (leadingIcon != null) {
                    leadingIcon()
                    Spacer(Modifier.width(10.dp))
                }
                Text(
                    text,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun PunchIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val palette = LocalWatchPalette.current
    val resolvedColor = if (color == Color.Unspecified) palette.surface else color
    val resolvedContentColor = if (contentColor == Color.Unspecified) palette.ink else contentColor
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val radius by animateDpAsState(
        if (pressed && enabled) 20.dp else 15.dp,
        spring(dampingRatio = 0.7f, stiffness = 520f),
        label = "iconShape"
    )
    val shift by animateDpAsState(
        if (pressed && enabled) 3.dp else 0.dp,
        spring(dampingRatio = 0.7f, stiffness = 520f),
        label = "iconShift"
    )
    Box(Modifier.size(48.dp).then(modifier).then(pressScale(pressed, enabled))) {
        Box(
            Modifier
                .matchParentSize()
                .offset(y = if (pressed) 0.dp else 4.dp)
                .clip(RoundedCornerShape(radius))
                .background(palette.shadow)
        )
        Surface(
            shape = RoundedCornerShape(radius),
            color = resolvedColor,
            contentColor = resolvedContentColor,
            border = BorderStroke(2.dp, palette.stroke),
            modifier = Modifier
                .fillMaxSize()
                .offset(y = shift)
                .clip(RoundedCornerShape(radius))
                .clickable(interactionSource = interaction, indication = LocalIndication.current, enabled = enabled, onClick = onClick)
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
        }
    }
}

@Composable
fun PunchCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    accent: Color = Color.Unspecified,
    content: @Composable () -> Unit
) {
    val palette = LocalWatchPalette.current
    val resolvedColor = if (color == Color.Unspecified) palette.surface else color
    val resolvedAccent = if (accent == Color.Unspecified) palette.blue else accent
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shift by animateDpAsState(if (pressed) 3.dp else 0.dp, spring(dampingRatio = 0.7f, stiffness = 520f), label = "cardShift")
    val radius by animateDpAsState(if (pressed) 26.dp else 20.dp, spring(dampingRatio = 0.7f, stiffness = 520f), label = "cardShape")
    Box(modifier.then(pressScale(pressed, true))) {
        Box(
            Modifier
                .matchParentSize()
                .offset(y = if (pressed) 0.dp else 5.dp)
                .clip(RoundedCornerShape(radius))
                .background(palette.shadow)
        )
        Surface(
            shape = RoundedCornerShape(radius),
            color = resolvedColor,
            border = BorderStroke(2.dp, palette.stroke),
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = shift)
                .clip(RoundedCornerShape(radius))
                .clickable(interactionSource = interaction, indication = LocalIndication.current, onClick = onClick)
        ) {
            Box {
                Box(Modifier.fillMaxWidth().height(4.dp).background(resolvedAccent))
                content()
            }
        }
    }
}

@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    content: @Composable () -> Unit
) {
    val palette = LocalWatchPalette.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = if (color == Color.Unspecified) palette.surface else color,
        border = BorderStroke(1.dp, palette.border),
        shadowElevation = 2.dp
    ) {
        content()
    }
}

@Composable
fun ScreenHeader(step: String, title: String, subtitle: String, accent: Color = Color.Unspecified) {
    Column {
        Eyebrow(step, accent)
        Spacer(Modifier.height(10.dp))
        Text(title, style = MaterialTheme.typography.displayLarge, color = Brand.Ink)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, color = Brand.Muted, fontSize = 15.sp)
        Spacer(Modifier.height(12.dp))
        AccentDivider(accent)
    }
}

@Composable
fun AnimatedEnter(delayMs: Int = 0, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(360, delayMs), label = "enterAlpha")
    val offset by animateFloatAsState(if (visible) 0f else 18f, tween(420, delayMs), label = "enterOffset")
    Box(Modifier.alpha(alpha).offset(y = offset.dp)) { content() }
}

@Composable
fun ProgressRing(progress: Float, diameter: Dp = 72.dp) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), spring(dampingRatio = 0.7f, stiffness = 160f), label = "progress")
    val track = Brand.PaperDim
    val fill = Brush.sweepGradient(listOf(Brand.Blue, Brand.Mint, Brand.Amber, Brand.Blue))
    Box(Modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 8.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            drawArc(fill, -90f, 360f * animated, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Text("${(animated * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = Brand.Ink)
    }
}

@Composable
fun Avatar(initials: String, color: Color, size: Dp = 38.dp) {
    Box(
        Modifier.size(size).clip(CircleShape).background(color).border(2.dp, Brand.Stroke, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(initials, color = Brand.OnAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AccentDivider(accent: Color = Color.Unspecified) {
    val palette = LocalWatchPalette.current
    val resolved = if (accent == Color.Unspecified) palette.blue else accent
    Box(
        Modifier.padding(vertical = 4.dp).size(height = 4.dp, width = 52.dp)
            .clip(CircleShape).background(Brush.horizontalGradient(listOf(resolved, palette.coral)))
    )
}

@Composable
fun DecorBackground(content: @Composable () -> Unit) {
    val palette = LocalWatchPalette.current
    Box(Modifier.fillMaxSize().background(palette.paper)) {
        Canvas(Modifier.fillMaxSize()) {
            val blue = palette.blue.copy(alpha = 0.07f)
            val amber = palette.amber.copy(alpha = 0.10f)
            val grid = palette.border.copy(alpha = 0.35f)
            drawCircle(blue, radius = size.minDimension * 0.42f, center = Offset(size.width, 0f))
            drawCircle(amber, radius = size.minDimension * 0.34f, center = Offset(0f, size.height))
            val step = 56.dp.toPx()
            var x = step
            while (x < size.width) {
                drawLine(grid, Offset(x, 0f), Offset(x, size.height), 1f)
                x += step
            }
            var y = step
            while (y < size.height) {
                drawLine(grid, Offset(0f, y), Offset(size.width, y), 1f)
                y += step
            }
        }
        content()
    }
}

@Composable
fun Eyebrow(text: String, accent: Color = Color.Unspecified) {
    val palette = LocalWatchPalette.current
    val resolved = if (accent == Color.Unspecified) palette.blue else accent
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(resolved).border(1.dp, palette.stroke, CircleShape))
        Spacer(Modifier.width(8.dp))
        Text(
            text.uppercase(),
            color = resolved,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp
        )
    }
}

@Composable
fun StatusPill(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, color)
    ) {
        Text(
            text.uppercase(),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            color = Brand.Ink,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun PunchTabs(tabs: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
        tabs.forEachIndexed { index, label ->
            val palette = LocalWatchPalette.current
            val isSelected = selected == index
            val background by animateColorAsState(
                if (isSelected) palette.inverseSurface else palette.surface,
                spring(dampingRatio = 0.7f, stiffness = 420f),
                label = "tabBackground"
            )
            val foreground by animateColorAsState(
                if (isSelected) palette.inverseOnSurface else palette.ink,
                spring(dampingRatio = 0.7f, stiffness = 420f),
                label = "tabForeground"
            )
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = background,
                contentColor = foreground,
                border = BorderStroke(2.dp, palette.stroke),
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(index) }
            ) {
                Text(
                    label.uppercase(),
                    modifier = Modifier.padding(vertical = 11.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun ShimmerBar(width: Dp, height: Dp = 12.dp) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.10f,
        targetValue = 0.28f,
        animationSpec = infiniteRepeatable(tween(850), RepeatMode.Reverse),
        label = "shimmerAlpha"
    )
    Box(
        Modifier.size(width, height).clip(RoundedCornerShape(height / 2))
            .background(Brand.Skeleton.copy(alpha = alpha))
    )
}

@Composable
fun ShowSkeletonRow() {
    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        ShimmerBar(52.dp, 74.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            ShimmerBar(150.dp, 14.dp)
            Spacer(Modifier.height(8.dp))
            ShimmerBar(90.dp, 10.dp)
        }
    }
}
