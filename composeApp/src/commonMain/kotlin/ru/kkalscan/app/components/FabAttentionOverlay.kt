package ru.kkalscan.app.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import ru.kkalscan.app.theme.KkalScanColors

@Composable
fun FabAttentionOverlay(
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!active) return

    val twinkle = rememberInfiniteTransition(label = "fab-attention-twinkle")
    val twinkleAlpha by twinkle.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(320, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "fab-attention-alpha",
    )

    Box(
        modifier = modifier.testTag("fab-attention-overlay"),
        contentAlignment = Alignment.Center,
    ) {
        val stars = listOf(
            Triple((-36).dp, (-30).dp, 18.dp),
            Triple(32.dp, (-34).dp, 16.dp),
            Triple((-30).dp, 28.dp, 15.dp),
            Triple(34.dp, 26.dp, 17.dp),
            Triple(0.dp, (-42).dp, 14.dp),
            Triple((-40).dp, 0.dp, 13.dp),
        )
        stars.forEachIndexed { index, (x, y, size) ->
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = KkalScanColors.Primary,
                modifier = Modifier
                    .offset(x = x, y = y)
                    .size(size)
                    .alpha(twinkleAlpha * (0.75f + (index % 3) * 0.08f)),
            )
        }
    }
}

@Composable
fun rememberFabAttentionPulse(active: Boolean): Float {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(active) {
        if (!active) {
            scale.snapTo(1f)
            return@LaunchedEffect
        }
        while (true) {
            scale.animateTo(1.14f, tween(280, easing = FastOutSlowInEasing))
            scale.animateTo(1f, tween(280, easing = FastOutSlowInEasing))
            delay(60)
        }
    }
    return scale.value
}
