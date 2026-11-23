package com.octaviantocan.vercelnative.dashboard

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.LinearGradient
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntSize
import com.octaviantocan.vercelnative.dashboard.DashboardTokens as Tokens
import kotlin.math.ceil

internal val LocalDashboardScrollPaper = staticCompositionLocalOf<Color?> { null }

/** Inventory's timing for presses, floating controls, sheets, and page transitions. */
internal object DashboardMotion {
    const val Quick = 150
    const val Fast = 250
    const val Medium = 350
    const val Slow = 400
    const val Dissolve = 160
    const val Tabs = 220
    const val Rail = 260
    const val Push = 270
    const val PushBack = 240
    const val Search = 280
    const val Dismiss = 220
    private val SmoothOut = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
    private val EaseOut = CubicBezierEasing(0f, 0f, .58f, 1f)
    private val EaseIn = CubicBezierEasing(.42f, 0f, 1f, 1f)
    private val EaseInOut = CubicBezierEasing(.42f, 0f, .58f, 1f)

    fun <T> move(duration: Int) = tween<T>(duration, easing = SmoothOut)
    fun <T> prototype(duration: Int) = tween<T>(duration, easing = EaseOut)
    fun <T> push(back: Boolean) = tween<T>(if (back) PushBack else Push, easing = EaseInOut)
    fun <T> dismiss() = tween<T>(Dismiss, easing = EaseIn)
}

private val darkGlassRim = object : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        // The approved stroke uses a 45-degree transform in normalized frame coordinates.
        val normal = Offset(.707f / size.width, .707f / size.height)
        val axis = normal / (normal.x * normal.x + normal.y * normal.y)
        return LinearGradientShader(axis * .207f, axis * 1.207f,
            colors = listOf(.48f, .10f, .16f, .38f).map { Color.White.copy(alpha = it) },
            colorStops = listOf(0f, .48f, .76f, 1f))
    }
}

@Composable
internal fun Modifier.dashboardGlass(backdrop: GraphicsLayer? = null, shape: Shape = CircleShape, tint: Color? = null): Modifier {
    val palette = LocalDashboardPalette.current
    val paper = LocalDashboardScrollPaper.current ?: palette.canvas
    val dark = palette == DashboardPalette.Dark
    val rim = remember(dark) {
        if (dark) darkGlassRim
        else Brush.linearGradient(listOf(Color.White, Color.White.copy(alpha = .24f), palette.border.copy(alpha = .70f)))
    }
    val surface = dropShadow(shape, Shadow(Tokens.GlassShadowRadius, Color.Black.copy(alpha = .26f),
        offset = DpOffset(Dp.Hairline, Tokens.Space4))).clip(shape)
    val lens = when {
        backdrop != null -> surface.refract(backdrop, shape, paper)
        Build.VERSION.SDK_INT >= 33 -> surface.glassOnSolidSurface(shape, paper)
        else -> surface.background(paper)
    }
    return lens
        .background(tint ?: palette.glassTint)
        .border(Tokens.Stroke, rim, shape)
}

private data class GlassOptics(val blur: Dp, val depth: Dp, val refraction: Float, val dispersion: Float, val light: Float)

private val ControlOptics = GlassOptics(Tokens.GlassBlur, Tokens.GlassDepth, .9f, .035f, .8f)
private val ReadyOptics = GlassOptics(Tokens.ReadyGlassBlur, Tokens.ReadyGlassDepth, .42f, .012f, .34f)

// An opaque sheet has a uniform backdrop. The same lens still catches light at its rim.
@RequiresApi(33)
private fun Modifier.glassOnSolidSurface(shape: Shape, paper: Color): Modifier = drawWithCache {
    val color = paper.toArgb()
    val shader = createGlassShader(ControlOptics)
    val outline = shape.createOutline(size, layoutDirection, this)
    val radius = (outline as? Outline.Rounded)?.roundRect?.topLeftCornerRadius?.x ?: 0f
    shader.setFloatUniform("bounds", size.width, size.height)
    shader.setFloatUniform("padding", 0f)
    shader.setFloatUniform("radius", radius)
    shader.setFloatUniform("depth", ControlOptics.depth.toPx())
    shader.setInputShader("source", LinearGradient(0f, 0f, 1f, 1f, color, color, android.graphics.Shader.TileMode.CLAMP))
    val brush = ShaderBrush(shader)
    onDrawBehind { drawRect(brush) }
}

@Composable
internal fun Modifier.dashboardReadyGlass(backdrop: GraphicsLayer, backdropOrigin: Offset, outlined: Boolean): Modifier {
    val palette = LocalDashboardPalette.current
    return clip(CircleShape).refract(backdrop, CircleShape, palette.previewPaper, backdropOrigin, ReadyOptics)
        .background(palette.readySurface.copy(alpha = .88f), CircleShape)
        .then(if (outlined) Modifier.border(Tokens.Stroke, palette.previewReadyBorder, CircleShape) else Modifier)
}

@Composable
private fun Modifier.refract(
    backdrop: GraphicsLayer, shape: Shape, paper: Color,
    backdropOrigin: Offset = Offset.Zero, optics: GlassOptics = ControlOptics,
): Modifier {
    val density = LocalDensity.current
    val blur = with(density) { optics.blur.toPx() }
    val depth = with(density) { optics.depth.toPx() }
    val pad = ceil(2 * blur + depth).toInt()
    val layer = rememberGraphicsLayer()
    val lens = remember(blur, optics) { if (Build.VERSION.SDK_INT >= 33) DashboardLens(blur, optics) else null }
    val fallback = remember(blur) { BlurEffect(blur, blur, TileMode.Clamp) }
    var origin by remember { mutableStateOf(Offset.Zero) }
    return onGloballyPositioned { origin = it.positionInRoot() }.drawBehind {
        if (size.isEmpty()) return@drawBehind
        val outline = shape.createOutline(size, layoutDirection, this)
        val radius = (outline as? Outline.Rounded)?.roundRect?.topLeftCornerRadius?.x ?: 0f
        layer.renderEffect = if (Build.VERSION.SDK_INT >= 33 && lens != null) {
            lens.effect(size, radius, pad, depth)
        } else fallback
        layer.record(size = IntSize(ceil(size.width).toInt() + pad * 2, ceil(size.height).toInt() + pad * 2)) {
            drawRect(paper)
            translate(pad + backdropOrigin.x - origin.x, pad + backdropOrigin.y - origin.y) { drawLayer(backdrop) }
        }
        translate(-pad.toFloat(), -pad.toFloat()) { drawLayer(layer) }
    }
}

@RequiresApi(33)
private fun createGlassShader(optics: GlassOptics) = RuntimeShader(
        """
        uniform shader source;
        uniform float2 bounds;
        uniform float padding;
        uniform float radius;
        uniform float depth;
        uniform float refraction;
        uniform float dispersion;
        uniform float light;

        half4 main(float2 p) {
            float2 local = p - float2(padding);
            float2 nearest = clamp(local, float2(radius), bounds - float2(radius));
            float2 delta = local - nearest;
            float distance = length(delta);
            float inside = radius - distance;
            float2 normal = delta / max(distance, 0.001);
            float rim = 1.0 - smoothstep(0.0, depth, inside);
            float2 displacement = normal * (depth * refraction) * rim * rim;
            float2 sampleAt = p - displacement;
            float2 split = displacement * dispersion;
            half4 glass = source.eval(sampleAt);
            glass.r = source.eval(sampleAt - split).r;
            glass.b = source.eval(sampleAt + split).b;
            float facing = max(dot(normal, float2(-0.7071, -0.7071)), 0.0);
            float reflection = (1.0 - smoothstep(0.0, depth * 0.075, inside)) * (0.21 + 0.12 * facing) * (light / 0.8);
            glass.rgb = mix(glass.rgb, half3(1.0), half(reflection));
            return glass;
        }
        """.trimIndent(),
).apply {
    setFloatUniform("refraction", optics.refraction)
    setFloatUniform("dispersion", optics.dispersion)
    setFloatUniform("light", optics.light)
}

/** Bend the live sample at the rounded edge. The highlight and color split follow its normal. */
@RequiresApi(33)
private class DashboardLens(blur: Float, optics: GlassOptics) {
    private val softened = RenderEffect.createBlurEffect(blur, blur, android.graphics.Shader.TileMode.CLAMP)
    private val shader = createGlassShader(optics)
    private var geometry: Geometry? = null
    private var cachedEffect: androidx.compose.ui.graphics.RenderEffect? = null

    fun effect(bounds: Size, radius: Float, pad: Int, depth: Float): androidx.compose.ui.graphics.RenderEffect {
        val next = Geometry(bounds, radius, pad, depth)
        if (next == geometry) cachedEffect?.let { return it }
        shader.setFloatUniform("bounds", bounds.width, bounds.height)
        shader.setFloatUniform("radius", radius)
        shader.setFloatUniform("padding", pad.toFloat())
        shader.setFloatUniform("depth", depth)
        return RenderEffect.createChainEffect(RenderEffect.createRuntimeShaderEffect(shader, "source"), softened)
            .asComposeRenderEffect().also { cachedEffect = it; geometry = next }
    }

    private data class Geometry(val bounds: Size, val radius: Float, val pad: Int, val depth: Float)
}

@Composable
internal fun DashboardList(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    surface: Color = LocalDashboardScrollPaper.current ?: LocalDashboardPalette.current.canvas,
    content: LazyListScope.() -> Unit,
) {
    LazyColumn(
        modifier.scrollEdges(surface, topDistance = {
            if (state.firstVisibleItemIndex == 0) state.firstVisibleItemScrollOffset.toFloat() else Float.MAX_VALUE
        }, bottomDistance = {
            val layout = state.layoutInfo
            val last = layout.visibleItemsInfo.lastOrNull()
            when {
                !state.canScrollForward -> 0f
                last == null || last.index < layout.totalItemsCount - 1 -> Float.MAX_VALUE
                else -> (last.offset + last.size + layout.afterContentPadding - layout.viewportEndOffset).toFloat()
            }
        }),
        state = state, contentPadding = contentPadding, verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment, flingBehavior = flingBehavior, content = content,
    )
}

@Composable
internal fun Modifier.dashboardScroll(
    state: ScrollState = rememberScrollState(),
    surface: Color = LocalDashboardScrollPaper.current ?: LocalDashboardPalette.current.canvas,
): Modifier = scrollEdges(
    surface, topDistance = { state.value.toFloat() },
    bottomDistance = { if (state.maxValue == Int.MAX_VALUE) 0f else (state.maxValue - state.value).toFloat() },
).verticalScroll(state)

/** Inventory's progressive blur. Three short bands get softer towards the viewport edge. */
@Composable
private fun Modifier.scrollEdges(
    surface: Color,
    topDistance: () -> Float,
    bottomDistance: () -> Float,
): Modifier {
    val source = rememberGraphicsLayer()
    val top = List(3) { rememberGraphicsLayer() }
    val bottom = List(3) { rememberGraphicsLayer() }
    return drawWithCache {
        val reach = Tokens.Space20.toPx()
        val blur = reach * .15f
        val radii = List(top.size) { index -> blur * (index + 1) / top.size }
        for ((index, radius) in radii.withIndex()) {
            val effect = BlurEffect(radius, radius, TileMode.Decal)
            for (layer in listOf(top[index], bottom[index])) {
                layer.renderEffect = effect
                layer.compositingStrategy = CompositingStrategy.Offscreen
            }
        }
        val fade = arrayOf(0f to surface, .22f to surface.copy(alpha = surface.alpha * .90f),
            .44f to surface.copy(alpha = surface.alpha * .62f), .66f to surface.copy(alpha = surface.alpha * .26f),
            .89f to surface.copy(alpha = surface.alpha * .03f), 1f to surface.copy(alpha = 0f))
        onDrawWithContent {
            val topOpacity = (topDistance() / reach).coerceIn(0f, 1f)
            val bottomOpacity = (bottomDistance() / reach).coerceIn(0f, 1f)
            val height = reach.coerceAtMost(size.height / 2f)
            if ((topOpacity == 0f && bottomOpacity == 0f) || height < 1f || size.width < 1f) {
                drawContent()
            } else {
                source.record { this@onDrawWithContent.drawContent() }
                drawLayer(source)
                if (topOpacity > 0f) drawScrollEdge(source, top, radii, 0f, height, topOpacity, true, fade)
                if (bottomOpacity > 0f) drawScrollEdge(source, bottom, radii, size.height - height, height, bottomOpacity, false, fade)
            }
        }
    }
}

private fun DrawScope.drawScrollEdge(
    source: GraphicsLayer, layers: List<GraphicsLayer>, radii: List<Float>,
    y: Float, height: Float, opacity: Float, top: Boolean, fade: Array<Pair<Float, Color>>,
) {
    layers.forEachIndexed { index, layer ->
        val span = height * (layers.size - index) / layers.size
        val pad = ceil(radii[index] * 3)
        val start = if (top) y else y + height - span
        layer.alpha = opacity
        layer.record(size = IntSize(ceil(size.width).toInt(), ceil(span + 2 * pad).toInt())) {
            translate(top = pad - start) { drawLayer(source) }
            val edge = if (top) pad else pad + span
            val clear = if (top) pad + span else pad
            drawRect(Brush.verticalGradient(listOf(Color.Black, Color.Transparent), edge, clear), blendMode = BlendMode.DstIn)
        }
        clipRect(0f, y, size.width, y + height) { translate(top = start - pad) { drawLayer(layer) } }
    }
    drawRect(
        Brush.verticalGradient(*fade, startY = if (top) y else y + height, endY = if (top) y + height else y),
        topLeft = Offset(0f, y), size = Size(size.width, height), alpha = opacity,
    )
}
