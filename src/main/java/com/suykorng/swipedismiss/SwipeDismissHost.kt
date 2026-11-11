package com.suykorng.swipedismiss

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.hideFromAccessibility

/**
 * [enter] and [exit] control programmatic presentation and dismissal. [settleAnimation] animates
 * a released swipe. [swipeTransform] receives progress from 0 (shown) to 1 (dismissed) and a
 * direction of +1 for right or -1 for left. It can change translation, scale, alpha, and shape.
 */
public class SwipeDismissMotion(
    public val enter: EnterTransition = fadeIn(),
    public val exit: ExitTransition = fadeOut(),
    public val settleAnimation: AnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium,
        visibilityThreshold = 0.002f,
    ),
    public val swipeTransform: GraphicsLayerScope.(progress: Float, direction: Float) -> Unit =
        { progress, direction -> translationX = direction * size.width * progress },
) {
    public companion object {
        public val Default: SwipeDismissMotion = SwipeDismissMotion()
    }
}

/**
 * Shows [content] over [background] while [presentation] is non-null. Give each presentation a
 * stable identity. When [onDismissRequest] runs, the caller clears the presentation.
 *
 * Set [handleSystemBack] to false when the surrounding navigation owns Back. [gesturesEnabled]
 * can temporarily suspend both gestures, for example while a dialog covers this host.
 */
@Composable
public fun <T : Any> SwipeDismissHost(
    presentation: T?,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    motion: SwipeDismissMotion = SwipeDismissMotion.Default,
    allowRightSwipe: Boolean = true,
    handleSystemBack: Boolean = true,
    gesturesEnabled: Boolean = true,
    background: @Composable () -> Unit,
    content: @Composable (T) -> Unit,
) {
    var dismissedBySwipe by remember { mutableStateOf<T?>(null) }
    val currentPresentation by rememberUpdatedState(presentation)
    LaunchedEffect(presentation) {
        if (presentation != null) dismissedBySwipe = null
    }

    Box(modifier.fillMaxSize()) {
        Box(
            Modifier.fillMaxSize().then(
                if (presentation != null) {
                    Modifier.clearAndSetSemantics { hideFromAccessibility() }
                } else {
                    Modifier
                },
            ),
        ) {
            background()
        }

        AnimatedContent(
            targetState = presentation,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                val enter = if (targetState != null) motion.enter else EnterTransition.None
                val exit = if (targetState == null && initialState != dismissedBySwipe) {
                    motion.exit
                } else {
                    ExitTransition.None
                }
                (enter togetherWith exit).apply {
                    targetContentZIndex = if (targetState != null) 1f else -1f
                }
            },
            label = "swipe-dismiss-presentation",
        ) { item ->
            if (item != null) {
                PredictiveDismiss(
                    onDismiss = { swiped ->
                        if (currentPresentation == item) {
                            if (swiped) dismissedBySwipe = item
                            onDismissRequest()
                        }
                    },
                    enabled = gesturesEnabled && item == presentation,
                    allowRightSwipe = allowRightSwipe,
                    handleSystemBack = handleSystemBack,
                    motion = motion,
                ) {
                    content(item)
                }
            }
        }
    }
}
