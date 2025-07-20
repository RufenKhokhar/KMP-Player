package io.github.rufenkhokhar

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier


@OptIn(ExperimentalMultiplatform::class)
@Composable
expect fun KMPPlayer(
    state: KMPPlayerState,
    showControls: Boolean,
    modifier: Modifier = Modifier
)

@OptIn(ExperimentalMultiplatform::class)
@Composable
expect fun rememberKMPPlayerState(): KMPPlayerState