package io.github.rufenkhokhar

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerItemDidPlayToEndTimeNotification
import platform.AVFoundation.AVPlayerItemPlaybackStalledNotification
import platform.AVFoundation.addPeriodicTimeObserverForInterval
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.playbackBufferEmpty
import platform.AVFoundation.currentItem
import platform.AVFoundation.playbackLikelyToKeepUp
import platform.AVFoundation.rate
import platform.AVFoundation.removeTimeObserver
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.AVFoundation.seekToTime
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL
import platform.darwin.NSEC_PER_SEC
import platform.darwin.dispatch_get_main_queue

@OptIn(ExperimentalMultiplatform::class)
internal class IOSKMPPlayerState(private val player: AVPlayer) : KMPPlayerState {

    private val _playerState = MutableStateFlow<KMPPlayerEvent>(KMPPlayerEvent.Ideal)
    override fun observePlayerEvent(): StateFlow<KMPPlayerEvent> = _playerState
    private var observer: Any? = null
    private var endObserver: Any? = null
    private var stallObserver: Any? = null
    private var timeObserver: Any? = null

    private var loop: Boolean=false


    override fun play() {
        if (player.rate().toDouble() == 0.0) {
            player.play()
            _playerState.update { KMPPlayerEvent.Playing }
        }
    }

    override fun pause() {
        if (player.rate().toDouble() > 0.0) {
            player.pause()
            _playerState.update { KMPPlayerEvent.Paused }
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun stop() {
        player.pause()
        player.seekToTime(CMTimeMakeWithSeconds(0.0, 1))
        _playerState.update { KMPPlayerEvent.Ended }
    }

    override fun isPlaying(): Boolean = player.rate().toDouble() > 0.0

    override fun setLocalFile(absolutePath: String) {
        cleanupObserver()
        runCatching {
            val url = NSURL.fileURLWithPath(absolutePath)
            preparePlayer(url)
        }.onFailure { e ->
            _playerState.update { KMPPlayerEvent.Error(e.message.orEmpty()) }
        }
    }

    override fun setFileUrl(url: String) {
        cleanupObserver()
        runCatching {
            NSURL.URLWithString(url)?.let { url ->
                preparePlayer(url)
            } ?: throw IllegalArgumentException("Invalid URL: $url")
        }.onFailure { e ->
            _playerState.update { KMPPlayerEvent.Error(e.message.orEmpty()) }
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun preparePlayer(url: NSURL) {
        val item = AVPlayerItem(url)
        player.replaceCurrentItemWithPlayerItem(item)
        // 1) End‐of‐playback observer (your existing loop logic)
        endObserver = NSNotificationCenter.defaultCenter.addObserverForName(
            AVPlayerItemDidPlayToEndTimeNotification,
            item,
            NSOperationQueue.mainQueue
        ) { _ ->
            player.seekToTime(CMTimeMakeWithSeconds(0.0, 1))
            if (loop){
                player.play()
                _playerState.update {
                    KMPPlayerEvent.Playing
                }
            }else {
                _playerState.update {
                    KMPPlayerEvent.Ended
                }
            }
        }
        // 2) Buffer‐stall notification
        stallObserver = NSNotificationCenter.defaultCenter.addObserverForName(
            AVPlayerItemPlaybackStalledNotification,
            item,
            NSOperationQueue.mainQueue
        ) { _ ->
            _playerState.value = KMPPlayerEvent.Buffering
        }

        // 3) Periodic observer to detect when 'likelyToKeepUp' flips back on
        val interval = CMTimeMakeWithSeconds(0.5, NSEC_PER_SEC.toInt())
        timeObserver = player.addPeriodicTimeObserverForInterval(
            interval,
            dispatch_get_main_queue()
        ) { _ ->

            player.currentItem?.let { current ->
                // Buffer started
                if (current.playbackBufferEmpty && _playerState.value != KMPPlayerEvent.Buffering) {
                    _playerState.value = KMPPlayerEvent.Buffering
                }
                // Buffer recovered
                else if (!current.playbackBufferEmpty
                    && current.playbackLikelyToKeepUp
                    && _playerState.value == KMPPlayerEvent.Buffering
                ) {
                    _playerState.value = KMPPlayerEvent.Playing
                }
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun setVideoLoop(loop: Boolean) {
      this.loop = loop
    }

    private fun cleanupObserver() {
        observer?.let {
            NSNotificationCenter.defaultCenter.removeObserver(it)
            observer = null
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
        finalize()
    }

    protected fun finalize() {
        cleanupObserver()
        stop()
    }
}
