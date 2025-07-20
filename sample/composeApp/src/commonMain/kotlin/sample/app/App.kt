package sample.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.github.rufenkhokhar.KMPPlayer
import io.github.rufenkhokhar.KMPPlayerEvent
import io.github.rufenkhokhar.rememberKMPPlayerState

@OptIn(ExperimentalMultiplatform::class)
@Composable
fun App() {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        val playerState = rememberKMPPlayerState()
        KMPPlayer(
            state = playerState,
            showControls = true,
            modifier = Modifier.fillMaxSize().background(Color.Black)
        )
        LaunchedEffect(Unit) {
            playerState.setFileUrl("https://www.sample-videos.com/video321/mp4/360/big_buck_bunny_360p_20mb.mp4")
            playerState.play()
            playerState.observePlayerEvent().collect { event ->
                when (event) {
                    KMPPlayerEvent.Buffering -> {
                        println("kmp_state: Buffering")

                    }

                    KMPPlayerEvent.Ended -> {
                        println("kmp_state: ended")
                    }

                    is KMPPlayerEvent.Error -> {
                        println("kmp_state: error ${event.message}")
                    }

                    KMPPlayerEvent.Ideal -> {
                        println("kmp_state: Ideal")
                    }

                    KMPPlayerEvent.Playing -> {
                        println("kmp_state: Playing")
                    }

                    KMPPlayerEvent.Stop -> {
                        println("kmp_state: Stop")
                    }

                    KMPPlayerEvent.Paused -> {
                        println("kmp_state: Paused")
                    }
                }

            }
        }

    }
}