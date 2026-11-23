package com.octaviantocan.vercelnative.dashboard

import androidx.compose.animation.core.animate
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** Inventory's sheet position, stops, and nested scroll ownership. */
@Stable
internal class DashboardSheetPosition(
    private val onLand: () -> Unit = {},
    private val onThreshold: () -> Unit = {},
) {
    var offset by mutableFloatStateOf(0f)
        private set
    var reveal by mutableFloatStateOf(0f)
        private set

    /** The panel, which is the sheet's tall height, in px. */
    private var height by mutableIntStateOf(0)

    /** What the sheet's content takes, in px, at most [height]. A [fills] sheet does not measure it. */
    private var content by mutableIntStateOf(0)
    private var pinned by mutableStateOf(false)

    /** Half the window: the most a sheet shows at its normal height. */
    var half = 0f

    /** Where the panel is when it is off the screen, below it. */
    private val away: Float get() = height.toFloat()

    /** The sheet has a tall height to lift to; without one it rests at its normal height only. */
    var expandable = true

    /** Its content fills the panel (chat, Commands), so its normal height is half the window whatever it holds. */
    var fills = false

    /** Resting at the tall height, or on its way there. */
    var tall by mutableStateOf(false)
        private set

    /** Where the panel is resting or heading, so a change of [rest] can tell it has to move. */
    private var goal = 0f

    var closing by mutableStateOf(false)
        private set
    private var opened = false
    private var dragging = false

    /** Where the panel was when a finger took hold of it, so letting go can tell whether it went to another stop. */
    private var heldFrom = 0f

    /** The finger has pulled the panel far enough down that letting go closes it. */
    private var pastClose = false
    private var offsetJob: Job? = null
    private var revealJob: Job? = null

    /** How far down a panel [h] px tall sits at its normal height. */
    private fun normalFor(h: Float): Float {
        val shows = when {
            fills -> if (expandable) half else h
            expandable -> minOf(content.toFloat(), half)
            else -> content.toFloat()
        }
        return (h - shows).coerceIn(0f, h)
    }

    private fun restFor(h: Float): Float = if (tall && expandable) 0f else normalFor(h)
    private val rest: Float get() = restFor(height.toFloat())

    /** The highest the panel goes: its tall height, or its normal one when it has no other. */
    private val highest: Float get() = if (expandable) 0f else normalFor(height.toFloat())

    /** How far down the panel sits, in px, for a panel [h] px tall: its stop while it rests, [offset] while it moves. */
    fun shift(h: Float): Float = if (pinned) restFor(h) else offset

    /** Lower than it can go, so a finger moving up should raise it before the content scrolls. */
    val canRise: Boolean get() = shift(height.toFloat()) > highest + 0.5f

    /** A finger has moved the panel and is still holding it. */
    val held: Boolean get() = dragging

    /**
     * How tall a [fills] sheet lays its content out, in px: as much of the panel as shows, and never less than
     * its normal height, so the footer stays on screen at either height and rides down with the panel as it leaves.
     */
    fun shown(): Int {
        val h = height.toFloat()
        return (h - minOf(shift(h), normalFor(h))).roundToInt()
    }

    /** The scrim: fully there while the panel rests, gone as it is pushed off the screen. */
    val scrim: Float
        get() {
            val span = height - rest
            val gone = if (span <= 0f) 0f else ((shift(height.toFloat()) - rest) / span).coerceIn(0f, 1f)
            return reveal * (1f - gone)
        }

    /** Opens at the tall height rather than the normal one; nothing once the sheet is open. */
    fun openTall() {
        if (!opened && !closing) tall = true
    }

    fun startClosing(): Boolean {
        if (closing) return false
        closing = true
        return true
    }

    /** Takes the panel off its stop, so [offset] is where it is and can be moved. */
    private fun unpin() {
        if (!pinned) return
        offset = rest
        pinned = false
    }

    /** The panel has its size, the tall height: the first time it opens (once the content has a size too), later the stops follow it. */
    fun measured(px: Int, scope: CoroutineScope, motion: Boolean) {
        if (px == height || px <= 0) return
        height = px
        sized(scope, motion)
    }

    /** The content has a size; a [fills] sheet's is the panel's, so it is not asked. */
    fun measuredContent(px: Int, scope: CoroutineScope, motion: Boolean) {
        if (fills || px == content || px <= 0) return
        content = px
        sized(scope, motion)
    }

    private fun sized(scope: CoroutineScope, motion: Boolean) {
        when {
            closing -> Unit
            !opened -> if (height > 0 && (fills || content > 0)) open(scope, motion)
            else -> resized(scope, motion)
        }
    }

    /**
     * The panel or its content changed size while open. One resting on its stop stays pinned, and [shift]
     * moves it with the size; one on its way is sent to the stop's new place. One under a finger or on its
     * way out is left alone.
     */
    private fun resized(scope: CoroutineScope, motion: Boolean) {
        if (!opened || closing || dragging) return
        if (pinned) {
            goal = rest
            return
        }
        follow(scope, motion)
    }

    /**
     * Where the panel rests moved with no change of size, because half the window did (split screen): a panel
     * that was resting, or on its way, goes to the new place. One under a finger or on its way out is left alone.
     */
    fun follow(scope: CoroutineScope, motion: Boolean) {
        if (!opened || closing || dragging || abs(goal - rest) < 2f) return
        glide(scope, rest, DashboardMotion.Fast, motion)
    }

    /** Half the window moved: a panel resting on its old stop is taken off it first, so it glides rather than jumps. */
    fun updateHalf(value: Float) {
        if (value == half) return
        unpin()
        half = value
    }

    private fun open(scope: CoroutineScope, motion: Boolean) {
        opened = true
        if (!motion) {
            offset = rest
            goal = rest
            reveal = 1f
            pinned = true
            onLand()
            return
        }
        offset = away
        goal = rest
        // The frames that build the sheet (and its window) are slow; it starts rising after them rather than jumping
        // ahead by however long they took.
        revealJob = scope.launch {
            repeat(DashboardMotion.SheetSettleFrames) { withFrameNanos { } }
            // A finger or the keyboard that took it meanwhile keeps it.
            if (!dragging && offsetJob == null) {
                // It is felt as it comes up, the moment it reaches its place (J12).
                glide(scope, rest, DashboardMotion.Slow, true, bounce = true, land = true)
            }
            animate(0f, 1f, animationSpec = DashboardMotion.move(DashboardMotion.Slow)) { v, _ -> reveal = v }
        }
    }

    /**
     * Moves the panel to [target]: on [DashboardMotion.SheetSpring] when it [bounce]s (opening, and settling after a finger,
     * starting at the finger's [velocity]), else in [ms] on the smooth-out curve. The spring never lifts the panel above
     * fully up, where there is no paper under it. With [land] the phone ticks the moment the panel reaches [target].
     */
    private fun glide(scope: CoroutineScope, target: Float, ms: Int, motion: Boolean, bounce: Boolean = false, velocity: Float = 0f, land: Boolean = false) {
        offsetJob?.cancel()
        unpin()
        goal = target
        if (!motion) {
            offset = target
            pinned = !closing && abs(target - rest) < 2f
            if (land) onLand()
            return
        }
        val down = target > offset
        var landed = !land
        offsetJob = scope.launch {
            if (bounce) animate(offset, target, initialVelocity = velocity, animationSpec = DashboardMotion.SheetSpring) { v, _ ->
                offset = maxOf(v, 0f)
                if (!landed && (if (down) v >= target else v <= target + 1f)) {
                    landed = true
                    onLand()
                }
            }
            else animate(offset, target, animationSpec = DashboardMotion.move(ms)) { v, _ -> offset = v }
            // It got where it was going: from here the stop follows the panel's size.
            pinned = !closing && abs(target - rest) < 2f
        }
    }

    /**
     * Up to the tall height, because a field took the keyboard or the content asked. A sheet that is not open
     * yet opens straight to it; one under a finger is left to the finger, and one with no tall height stays.
     */
    fun lift(scope: CoroutineScope, motion: Boolean) {
        if (!expandable || closing || tall) return
        tall = true
        // On the same spring as a sheet let go, so it rises with the keyboard rather than popping up (J10).
        if (opened && !dragging) glide(scope, 0f, DashboardMotion.Slow, motion, bounce = true)
    }

    /** Moves the panel with a finger; returns how much of [dy] it used. */
    fun dragBy(dy: Float): Float {
        if (closing || height == 0) return 0f
        offsetJob?.cancel()
        unpin()
        if (!dragging) {
            heldFrom = offset
            pastClose = false
        }
        dragging = true
        val next = (offset + dy).coerceIn(minOf(highest, offset), height.toFloat())
        val used = next - offset
        offset = next
        // Pulled far enough that letting go closes it, or back under that line: felt either way (rule 109).
        val low = if (expandable) normalFor(height.toFloat()) else highest
        val past = offset - low > (height - low) * 0.3f
        if (past != pastClose) {
            pastClose = past
            onThreshold()
        }
        return used
    }

    /**
     * The finger lets go at [velocity] (px/s, down is positive): a quick throw goes on to the next
     * place in its direction, a slow release to the nearest one, and one dragged well past its normal
     * height dismisses.
     */
    fun settle(scope: CoroutineScope, velocity: Float, speed: Float, motion: Boolean, dismiss: () -> Unit) {
        if (closing) return
        unpin()
        dragging = false
        val normal = normalFor(height.toFloat())
        val rests = if (expandable && normal > 1f) listOf(0f, normal) else listOf(highest)
        val gone = height.toFloat()
        val target = dashboardSheetTarget(offset, velocity, speed, rests, gone)
        if (target >= gone) {
            // Thrown down: it carries on down, the way the finger went (rule 105).
            dismiss()
            return
        }
        tall = expandable && target < 1f
        // Let go on its way to the other stop, it ticks as it gets there.
        glide(scope, target, DashboardMotion.Fast, motion, bounce = true, velocity = velocity, land = abs(target - heldFrom) > 2f)
    }

    /** Accessibility controls use the same stops and spring as a finger release. */
    fun expand(scope: CoroutineScope, expanded: Boolean, motion: Boolean) {
        if (expanded) lift(scope, motion) else {
            if (closing) return
            tall = false
            if (opened && !dragging) glide(scope, rest, DashboardMotion.Fast, motion, bounce = true)
        }
    }

    /** Remove the sheet only after its exit, ignoring repeated close requests. */
    fun dismiss(scope: CoroutineScope, motion: Boolean, onClose: () -> Unit) {
        if (startClosing()) scope.launch {
            hide(motion)
            onClose()
        }
    }

    /** The panel and scrim leave together. */
    suspend fun hide(motion: Boolean) {
        offsetJob?.cancel()
        revealJob?.cancel()
        unpin()
        if (!motion) {
            reveal = 0f
            offset = away
            return
        }
        coroutineScope {
            launch { animate(offset, away, animationSpec = DashboardMotion.move(DashboardMotion.Medium)) { v, _ -> offset = v } }
            launch { animate(reveal, 0f, animationSpec = DashboardMotion.move(DashboardMotion.Medium)) { v, _ -> reveal = v } }
        }
    }
}

/**
 * Where a sheet let go at [offset] goes: one of the [rests] (highest first) or [gone], off the screen.
 * A throw faster than [speed] (px/s, down is positive) goes on to the next place in its direction; a
 * slower release goes to the nearest rest, or dismisses when it was dragged more than 30% of the way
 * from the lowest rest to [gone].
 */
internal fun dashboardSheetTarget(offset: Float, velocity: Float, speed: Float, rests: List<Float>, gone: Float): Float {
    val low = rests.last()
    return when {
        velocity > speed -> rests.firstOrNull { it > offset + 1f } ?: gone
        velocity < -speed -> rests.lastOrNull { it < offset - 1f } ?: rests.first()
        offset - low > (gone - low) * 0.3f -> gone
        else -> rests.minBy { abs(it - offset) }
    }
}

/**
 * Lets the panel follow the content: scrolling up raises a lowered sheet first, and pulling down at the top of the
 * content lowers it, but only when the drag began with the content already at its top. A drag that scrolled the content
 * leaves the sheet where it is when the content reaches its top, and so does a fling that runs into the top: the sheet
 * moves when it is taken hold of, never because a scroll ran out (H2). A fling settles the panel only when the finger
 * moved it, so a fling inside a list scrolls the list.
 */
internal class DashboardSheetScroll(
    private val position: DashboardSheetPosition,
    private val scope: CoroutineScope,
    private val speed: Float,
    private val motion: () -> Boolean,
    private val dismiss: () -> Unit,
) : NestedScrollConnection {
    /** This drag has scrolled the content, so it belongs to the content until the finger lifts. */
    private var scrolledContent = false

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
        if (available.y < 0f && position.canRise && source == NestedScrollSource.UserInput) Offset(0f, position.dragBy(available.y)) else Offset.Zero

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        if (source != NestedScrollSource.UserInput) return Offset.Zero
        if (consumed.y != 0f) scrolledContent = true
        return if (available.y > 0f && !scrolledContent) Offset(0f, position.dragBy(available.y)) else Offset.Zero
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        // The finger is up: the next drag decides afresh.
        scrolledContent = false
        if (!position.held) return Velocity.Zero
        position.settle(scope, available.y, speed, motion(), dismiss)
        // A throw upward that lifted the sheet carries on into its content, so one swipe both raises the sheet and
        // scrolls what is in it: the first swipe on a long sheet no longer stops dead at the tall height ("it's a little
        // hard to scroll down ... it's just at the start": recording 16 Oct 13:06, 03:24 to 03:33; O8).
        return if (available.y < 0f) Velocity.Zero else available
    }
}
