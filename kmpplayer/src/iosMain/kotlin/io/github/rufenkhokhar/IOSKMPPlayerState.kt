package io.github.rufenkhokhar

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerItemDidPlayToEndTimeNotification
import platform.AVFoundation.AVPlayerItemPlaybackStalledNotification
import platform.AVFoundation.addPeriodicTimeObserverForInterval
import platform.AVFoundation.currentItem
import platform.AVFoundation.currentTime
import platform.AVFoundation.duration
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.playbackBufferEmpty
import platform.AVFoundation.playbackLikelyToKeepUp
import platform.AVFoundation.rate
import platform.AVFoundation.removeTimeObserver
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.AVFoundation.seekToTime
import platform.AVFoundation.volume
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL
import platform.darwin.dispatch_get_main_queue

@OptIn(ExperimentalMultiplatform::class)
internal class IOSKMPPlayerState(private val player: AVPlayer) : KMPPlayerState {

    private val _playerState = MutableStateFlow<KMPPlayerEvent>(KMPPlayerEvent.Ideal)
    override fun observePlayerEvent(): StateFlow<KMPPlayerEvent> = _playerState

    private var progressUpdateJob: Job? = null

    private var endObserver: Any? = null
    private var stallObserver: Any? = null
    private var timeObserver: Any? = null

    private var loop: Boolean = false
    private var disposed = false

    override fun play() {
        if (player.rate().toDouble() == 0.0) {
            player.play()
            startUpdateProgress()
            _playerState.update { KMPPlayerEvent.Playing(getCurrentPosition(), getDuration()) }
        }
    }

    override fun pause() {
        if (player.rate().toDouble() > 0.0) {
            player.pause()
            stopUpdateProgress()
            _playerState.update { KMPPlayerEvent.Paused }
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun stop() {
        player.pause()
        stopUpdateProgress()
        player.seekToTime(CMTimeMakeWithSeconds(0.0, 600))
        player.replaceCurrentItemWithPlayerItem(null)
        _playerState.update { KMPPlayerEvent.Ended }
        cleanupObservers()
    }

    override fun isPlaying(): Boolean = player.rate().toDouble() > 0.0

    override fun setLocalFile(absolutePath: String) {
        cleanupObservers()
        runCatching {
            val url = NSURL.fileURLWithPath(absolutePath)
            preparePlayer(url)
        }.onFailure { e ->
            _playerState.update { KMPPlayerEvent.Error(e.message.orEmpty()) }
        }
    }

    override fun setFileUrl(url: String) {
        cleanupObservers()
        runCatching {
            NSURL.URLWithString(url)?.let { preparePlayer(it) }
                ?: throw IllegalArgumentException("Invalid URL: $url")
        }.onFailure { e ->
            _playerState.update { KMPPlayerEvent.Error(e.message.orEmpty()) }
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun preparePlayer(url: NSURL) {
        // Preparing a new item — make sure progress loop isn't reporting old item
        stopUpdateProgress()

        val item = AVPlayerItem(url)
        player.replaceCurrentItemWithPlayerItem(item)

        endObserver = NSNotificationCenter.defaultCenter.addObserverForName(
            AVPlayerItemDidPlayToEndTimeNotification,
            item,
            NSOperationQueue.mainQueue
        ) { _ ->
            player.seekToTime(CMTimeMakeWithSeconds(0.0, 600))
            if (loop) {
                player.play()
                startUpdateProgress()
            } else {
                stopUpdateProgress()
                _playerState.update { KMPPlayerEvent.Ended }
            }
        }

        stallObserver = NSNotificationCenter.defaultCenter.addObserverForName(
            AVPlayerItemPlaybackStalledNotification,
            item,
            NSOperationQueue.mainQueue
        ) { _ ->
            _playerState.update { KMPPlayerEvent.Buffering }
        }

        val interval = CMTimeMakeWithSeconds(0.5, 600) // 0.5s @ 600 tps
        timeObserver = player.addPeriodicTimeObserverForInterval(
            interval,
            dispatch_get_main_queue()
        ) { _ ->
            player.currentItem?.let { current ->
                val isBuffering = current.playbackBufferEmpty || !current.playbackLikelyToKeepUp
                when {
                    isBuffering && _playerState.value != KMPPlayerEvent.Buffering ->
                        _playerState.update { KMPPlayerEvent.Buffering }
                    !isBuffering && _playerState.value == KMPPlayerEvent.Buffering && isPlaying() ->
                        _playerState.update { KMPPlayerEvent.Playing(getCurrentPosition(), getDuration()) }
                }
            }
        }
    }

    override fun setVideoLoop(loop: Boolean) {
        this.loop = loop
    }

    private fun cleanupObservers() {
        endObserver?.let {
            NSNotificationCenter.defaultCenter.removeObserver(it)
            endObserver = null
        }
        stallObserver?.let {
            NSNotificationCenter.defaultCenter.removeObserver(it)
            stallObserver = null
        }
        timeObserver?.let {
            player.removeTimeObserver(it)
            timeObserver = null
        }
    }

    override fun getPlatformPlayer(): Any? = player

    override fun destroy() {
        if (disposed) return
        disposed = true
        cleanupObservers()
        stopUpdateProgress()
        player.pause()
        player.replaceCurrentItemWithPlayerItem(null)
        _playerState.update { KMPPlayerEvent.Ideal }
    }

    override fun setVolume(volume: Float) {
        player.volume = volume
    }

    override fun getVolume(): Float = player.volume()

    override fun setPlaybackSpeed(speed: Float) {
        if (speed <= 0f) {
            pause() // also stops progress
        } else {
            player.rate = speed
            startUpdateProgress()
            _playerState.update { KMPPlayerEvent.Playing(getCurrentPosition(), getDuration()) }
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun getCurrentPosition(): Long {
        val seconds = CMTimeGetSeconds(player.currentTime())
        return if (seconds.isNaN()) 0L else (seconds * 1000).toLong()
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun getDuration(): Long {
        val dur = player.currentItem?.duration?.let { CMTimeGetSeconds(it) } ?: 0.0
        return if (dur.isNaN()) 0L else (dur * 1000).toLong()
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun seekTo(position: Long) {
        val time = CMTimeMakeWithSeconds(position.toDouble() / 1000.0, 600)
        player.seekToTime(time)
    }

    private fun startUpdateProgress() {
        stopUpdateProgress() // stop if already active
        progressUpdateJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                _playerState.update {
                    KMPPlayerEvent.Playing(getCurrentPosition(), getDuration())
                }
                delay(100)
            }
        }
    }

    private fun stopUpdateProgress() {
        progressUpdateJob?.cancel()
        progressUpdateJob = null
    }
}


