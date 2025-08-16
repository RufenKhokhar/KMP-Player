package io.github.rufenkhokhar

import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalMultiplatform::class)
internal class AndroidKMPPlayerState(private val exoPlayer: ExoPlayer) : KMPPlayerState,
    Player.Listener {

    private var progressUpdateJob: Job? = null
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
            startUpdateProgress()
        } else {
            stopUpdateProgress()
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

    override fun setVolume(volume: Float) {
        exoPlayer.volume = volume
    }

    override fun getVolume(): Float {
        return exoPlayer.volume
    }

    override fun setPlaybackSpeed(speed: Float) {
        exoPlayer.setPlaybackSpeed(speed)
    }

    override fun getCurrentPosition(): Long {
        return exoPlayer.currentPosition
    }

    override fun getDuration(): Long {
        return exoPlayer.duration
    }

    override fun seekTo(position: Long) {
        exoPlayer.seekTo(position)
    }

    private fun startUpdateProgress() {
        progressUpdateJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                playerEvent.update {
                    KMPPlayerEvent.Playing(getCurrentPosition(), getDuration())
                }
                delay(100)
            }
        }
    }

    private fun stopUpdateProgress() {
        progressUpdateJob?.cancel()
    }


}