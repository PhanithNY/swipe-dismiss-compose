package com.suykorng.swipedismiss

import android.os.Build
import android.view.RoundedCorner
import android.view.View
import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animate
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Slides a full-screen foreground over the caller's background during system Back
 * or an optional in-app rightward swipe. The caller removes the screen in [onDismiss].
 */
@Composable
internal fun PredictiveDismiss(
    onDismiss: (swiped: Boolean) -> Unit,
    enabled: Boolean,
    allowRightSwipe: Boolean,
    handleSystemBack: Boolean,
    motion: SwipeDismissMotion,
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    val layoutDirection = LocalLayoutDirection.current
    val systemGestures = WindowInsets.systemGestures
    val scope = rememberCoroutineScope()
    val dismiss by rememberUpdatedState(onDismiss)
    val settleAnimation by rememberUpdatedState(motion.settleAnimation)
    var offset by remember { mutableFloatStateOf(0f) }
    var direction by remember { mutableFloatStateOf(1f) }
    var corners by remember { mutableStateOf(DeviceCorners()) }
    var dismissing by remember { mutableStateOf(false) }
    var systemBackInProgress by remember { mutableStateOf(false) }
    var settling by remember { mutableStateOf<Job?>(null) }

    fun beginSwipe(swipeDirection: Float) {
        settling?.cancel()
        offset = 0f
        direction = swipeDirection
        corners = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            view.deviceCorners()
        } else {
            DeviceCorners()
        }
    }

    fun finishSwipe(commit: Boolean, animated: Boolean = true, releaseVelocity: Float = 0f) {
        settling?.cancel()
        dismissing = commit
        settling = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            if (animated) {
                animate(
                    initialValue = offset,
                    targetValue = if (commit) 1f else 0f,
                    initialVelocity = releaseVelocity,
                    animationSpec = settleAnimation,
                ) { value, _ -> offset = value }
            }
            if (commit) dismiss(animated)
        }
    }

    LaunchedEffect(enabled, allowRightSwipe) {
        if (enabled && dismissing && settling?.isCompleted == true) {
            // AnimatedContent can reuse the same item if it reopens during its exit.
            offset = 0f
            dismissing = false
        } else if ((!enabled || (!allowRightSwipe && !systemBackInProgress)) &&
            !dismissing && offset != 0f
        ) {
            finishSwipe(commit = false)
        }
    }

    PredictiveBackHandler(enabled = enabled && handleSystemBack) { events ->
        if (dismissing) {
            events.collect()
            return@PredictiveBackHandler
        }

        systemBackInProgress = true
        var swiping = false
        try {
            events.collect { event ->
                if (!swiping) {
                    swiping = true
                    beginSwipe(if (event.swipeEdge == BackEventCompat.EDGE_RIGHT) -1f else 1f)
                }
                offset = event.progress * 0.5f
            }
            finishSwipe(commit = true, animated = swiping)
        } catch (cancelled: CancellationException) {
            if (swiping) finishSwipe(commit = false)
            throw cancelled
        } finally {
            systemBackInProgress = false
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(enabled, allowRightSwipe, systemGestures, layoutDirection) {
                if (enabled && allowRightSwipe) {
                    detectRightSwipe(
                        canDrag = { !dismissing && !systemBackInProgress },
                        canStartAt = { x ->
                            x >= systemGestures.getLeft(this, layoutDirection) &&
                                x < size.width - systemGestures.getRight(this, layoutDirection)
                        },
                        onStart = { beginSwipe(1f) },
                        onProgress = { offset = it },
                        onFinish = { commit, releaseVelocity ->
                            finishSwipe(commit, releaseVelocity = releaseVelocity)
                        },
                    )
                } else {
                    // Keep the revealed background from receiving taps until dismissal.
                    awaitPointerEventScope {
                        while (true) awaitPointerEvent()
                    }
                }
            },
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val fraction = offset.coerceIn(0f, 1f)
                    clip = fraction > 0f
                    if (clip) {
                        val rounding = (fraction / 0.08f).coerceIn(0f, 1f)
                        shape = corners.shape(rounding)
                        shadowElevation = 12.dp.toPx() * rounding
                    } else {
                        shadowElevation = 0f
                    }
                    motion.swipeTransform(this, fraction, direction)
                },
        ) {
            content()
        }
    }
}

private suspend fun PointerInputScope.detectRightSwipe(
    canDrag: () -> Boolean,
    canStartAt: (Float) -> Boolean,
    onStart: () -> Unit,
    onProgress: (Float) -> Unit,
    onFinish: (commit: Boolean, releaseVelocity: Float) -> Unit,
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        if (!canDrag() || !canStartAt(down.position.x)) return@awaitEachGesture

        val width = size.width.toFloat()
        val touchSlop = viewConfiguration.touchSlop
        val velocity = VelocityTracker()
        velocity.addPointerInputChange(down)
        var dragging = false

        while (true) {
            val event = awaitPointerEvent()
            if (!canDrag()) break
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            if (change.isConsumed || event.changes.any { it.id != down.id && it.pressed }) break

            val delta = change.position - down.position
            if (!change.pressed) {
                if (dragging) {
                    change.consume()
                    val releaseVelocity = if (change.uptimeMillis - change.previousUptimeMillis <= 50L) {
                        velocity.calculateVelocity().x
                    } else {
                        0f
                    }
                    val isFlick = delta.x >= 24.dp.toPx() && releaseVelocity >= 900.dp.toPx()
                    onFinish(delta.x >= width / 3f || isFlick, releaseVelocity / width)
                }
                return@awaitEachGesture
            }
            velocity.addPointerInputChange(change)

            if (!dragging) {
                if (maxOf(abs(delta.x), abs(delta.y)) < touchSlop) continue
                if (delta.x <= abs(delta.y) * 1.25f) return@awaitEachGesture
                dragging = true
                onStart()
            }

            change.consume()
            onProgress((delta.x / width).coerceIn(0f, 1f))
        }

        if (dragging && canDrag()) onFinish(false, 0f)
    }
}

private data class DeviceCorners(
    val topLeft: Int = 0,
    val topRight: Int = 0,
    val bottomLeft: Int = 0,
    val bottomRight: Int = 0,
) {
    fun shape(fraction: Float) = AbsoluteRoundedCornerShape(
        topLeft = CornerSize(topLeft * fraction),
        topRight = CornerSize(topRight * fraction),
        bottomLeft = CornerSize(bottomLeft * fraction),
        bottomRight = CornerSize(bottomRight * fraction),
    )
}

@RequiresApi(Build.VERSION_CODES.S)
private fun View.deviceCorners(): DeviceCorners {
    val insets = rootWindowInsets ?: return DeviceCorners()
    return DeviceCorners(
        topLeft = insets.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)?.radius ?: 0,
        topRight = insets.getRoundedCorner(RoundedCorner.POSITION_TOP_RIGHT)?.radius ?: 0,
        bottomLeft = insets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_LEFT)?.radius ?: 0,
        bottomRight = insets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_RIGHT)?.radius ?: 0,
    )
}
