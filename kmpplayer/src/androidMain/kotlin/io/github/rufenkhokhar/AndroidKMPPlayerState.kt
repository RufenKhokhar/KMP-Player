package io.github.rufenkhokhar

import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

@OptIn(ExperimentalMultiplatform::class)
internal class AndroidKMPPlayerState(private val exoPlayer: ExoPlayer) : KMPPlayerState,
    Player.Listener {
    private val playerEvent = MutableStateFlow<KMPPlayerEvent>(KMPPlayerEvent.Ideal)

    init {
        exoPlayer.addListener(this)
    }

    override fun onPlayerError(error: PlaybackException) {
        playerEvent.update {
            KMPPlayerEvent.Error(error.message ?: "Unknown error with ${error.errorCode} code!")
        }
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        when (playbackState) {
            Player.STATE_BUFFERING -> {
                playerEvent.update { KMPPlayerEvent.Buffering }
            }

            Player.STATE_ENDED -> {
                playerEvent.update { KMPPlayerEvent.Ended }
            }

            Player.STATE_IDLE -> {
                playerEvent.update { KMPPlayerEvent.Ideal }
            }

            else -> {
                // nothing
            }
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (isPlaying) {
            playerEvent.update {
                KMPPlayerEvent.Playing
            }
        }
    }

    override fun play() {
        if (!exoPlayer.isPlaying) {
            exoPlayer.play()
        }
    }

    override fun stop() {
        if (exoPlayer.isPlaying) {
            exoPlayer.stop()
            playerEvent.update { KMPPlayerEvent.Stop }
        }
    }

    override fun pause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        }
    }

    override fun isPlaying(): Boolean {
        return exoPlayer.isPlaying
    }

    override fun setLocalFile(absolutePath: String) {
        exoPlayer.setMediaItem(MediaItem.fromUri(absolutePath))
        exoPlayer.prepare()
    }

    override fun setFileUrl(url: String) {
        exoPlayer.setMediaItem(MediaItem.fromUri(url))
        exoPlayer.prepare()
    }

    override fun setVideoLoop(loop: Boolean) {
        exoPlayer.repeatMode = if (loop) {
            ExoPlayer.REPEAT_MODE_ONE
        } else {
            ExoPlayer.REPEAT_MODE_OFF
        }
    }

    override fun observePlayerEvent(): StateFlow<KMPPlayerEvent> {
        return playerEvent
    }

    override fun getPlatformPlayer(): Any {
        return exoPlayer
    }

    override fun destroy() {
        stop()
        exoPlayer.release()
    }


}