package com.arnab.mediaplayer.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

/** Marks the content that floating glass panels (nav bar, player controls) should blur. */
fun Modifier.glassSource(hazeState: HazeState): Modifier = this.haze(hazeState)

/**
 * A frosted "liquid glass" panel: it blurs and tints whatever was marked with [glassSource]
 * behind it, clipped to [shape]. [tint] is the base color the frost is mixed from — pass a
 * brand color for a colored glass look, or a neutral surface color for a clearer one.
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun Modifier.glassPanel(
    hazeState: HazeState,
    shape: Shape,
    tint: Color = MaterialTheme.colorScheme.surface
): Modifier = this
    .clip(shape)
    .hazeChild(hazeState, HazeMaterials.thin(tint))
