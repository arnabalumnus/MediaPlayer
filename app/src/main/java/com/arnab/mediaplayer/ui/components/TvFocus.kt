package com.arnab.mediaplayer.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Visible "focused" ring for D-pad/remote navigation (Android TV). Touch users never see this -
 * it only activates while [interactionSource] reports focus, which on a touchscreen normally
 * doesn't happen from a tap alone. Pass the same `interactionSource` you give the component
 * itself (`IconButton`, `Card`, etc. all accept one) so the ring tracks its real focus state
 * rather than duplicating a second, competing focus target.
 */
@Composable
fun Modifier.tvFocusHalo(
    interactionSource: InteractionSource,
    shape: Shape = CircleShape
): Modifier {
    val isFocused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (isFocused) 1.15f else 1f, label = "tvFocusScale")
    val ringColor = MaterialTheme.colorScheme.tertiary
    return this
        .scale(scale)
        .then(if (isFocused) Modifier.border(3.dp, ringColor, shape) else Modifier)
}
