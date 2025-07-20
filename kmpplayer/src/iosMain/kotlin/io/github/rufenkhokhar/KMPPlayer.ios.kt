package io.github.rufenkhokhar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerLayer
import platform.AVKit.AVPlayerViewController
import platform.UIKit.UIView

@OptIn(ExperimentalForeignApi::class, ExperimentalMultiplatform::class)
@Composable
actual fun KMPPlayer(
    state: KMPPlayerState,
    showControls: Boolean,
    modifier: Modifier
) {

    val player = state.getPlatformPlayer() as? AVPlayer
    val playerLayer = remember { AVPlayerLayer() }
    val avPlayerViewController = remember { AVPlayerViewController() }

    UIKitView(
        factory = {
            val playerContainer = UIView()
            playerContainer.addSubview(avPlayerViewController.view)
            avPlayerViewController.player = player
            playerLayer.player = player
            playerContainer
        },
        modifier = modifier,
        update = { view ->
            avPlayerViewController.view.setFrame(view.frame)
            avPlayerViewController.showsPlaybackControls = showControls
        },
        properties = UIKitInteropProperties(
            isInteractive = true,
            isNativeAccessibilityEnabled = true
        )
    )

}

@OptIn(ExperimentalMultiplatform::class)
@Composable
actual fun rememberKMPPlayerState(): KMPPlayerState {
    return remember {
        val player = AVPlayer()
        IOSKMPPlayerState(player)
    }
}