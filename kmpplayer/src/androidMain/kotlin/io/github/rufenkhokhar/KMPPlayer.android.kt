package io.github.rufenkhokhar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@OptIn(ExperimentalMultiplatform::class)
@Composable
actual fun KMPPlayer(
    state: KMPPlayerState,
    showControls: Boolean,
    modifier: Modifier
) {

    AndroidView(
        factory = {
            PlayerView(it).apply {
                player = state.getPlatformPlayer() as Player
            }
        },
        modifier = modifier,
        update = {
            it.useController = showControls
        }
    )

}

@OptIn(ExperimentalMultiplatform::class)
@Composable
actual fun rememberKMPPlayerState(): KMPPlayerState {
    val context = LocalContext.current
    return remember {
        val player = ExoPlayer.Builder(context).build()
        AndroidKMPPlayerState(player)
    }
}