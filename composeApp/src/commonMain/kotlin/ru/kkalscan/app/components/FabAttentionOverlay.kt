package ru.kkalscan.app.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Star
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
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "fab-attention-alpha",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("fab-attention-overlay"),
        contentAlignment = Alignment.Center,
    ) {
        val stars = listOf(
            Triple((-28).dp, (-22).dp, 14.dp),
            Triple(24.dp, (-26).dp, 12.dp),
            Triple((-22).dp, 20.dp, 11.dp),
            Triple(26.dp, 18.dp, 13.dp),
            Triple(0.dp, (-34).dp, 10.dp),
        )
        stars.forEachIndexed { index, (x, y, size) ->
            Icon(
                imageVector = Icons.Outlined.Star,
                contentDescription = null,
                tint = KkalScanColors.Tertiary,
                modifier = Modifier
                    .offset(x = x, y = y)
                    .size(size)
                    .alpha(twinkleAlpha * (0.7f + (index % 3) * 0.1f)),
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
            scale.animateTo(1.08f, tween(350, easing = FastOutSlowInEasing))
            scale.animateTo(1f, tween(350, easing = FastOutSlowInEasing))
            delay(100)
        }
    }
    return scale.value
}

