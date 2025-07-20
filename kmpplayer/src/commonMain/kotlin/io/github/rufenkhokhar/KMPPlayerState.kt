package io.github.rufenkhokhar

import kotlinx.coroutines.flow.StateFlow

/**
 * Defines a contract for a multiplatform video player state.
 *
 * This interface provides methods to control playback, load media sources,
 * observe playback events, and retrieve the underlying platform-specific player.
 */

@ExperimentalMultiplatform
interface KMPPlayerState {
    /**
     * Starts or resumes playback of the current media.
     * If the player is already playing, this call has no effect.
     */
    fun play()

    /**
     * Stops playback and resets the media to the beginning.
     * This will pause the player and seek to time zero.
     */
    fun stop()

    /**
     * Pauses playback of the current media.
     * If the player is already paused, this call has no effect.
     */
    fun pause()

    /**
     * Returns whether the player is currently playing.
     *
     * @return `true` if playback is in progress, `false` otherwise.
     */
    fun isPlaying(): Boolean

    /**
     * Loads a local media file into the player.
     *
     * @param absolutePath The absolute file system path to the media file.
     */
    fun setLocalFile(absolutePath: String)

    /**
     * Loads a remote media file via URL into the player.
     *
     * @param url The string representation of the remote media URL.
     */
    fun setFileUrl(url: String)

    /**
     * Enables or disables looping of the current media.
     * When looping is enabled, playback will restart automatically after finishing.
     *
     * @param loop `true` to enable infinite looping, `false` to disable.
     */
    fun setVideoLoop(loop: Boolean)

    /**
     * Observes playback events from the player.
     * Emits events such as Ideal, Playing, Paused, Buffering, Ended, or Error.
     *
     * @return A [StateFlow] emitting [KMPPlayerEvent] values representing state changes.
     */
    fun observePlayerEvent(): StateFlow<KMPPlayerEvent>

    /**
     * Retrieves the underlying platform-specific player instance.
     *
     * @return The native player object (e.g., [AVPlayer] on iOS or [ExoPlayer] on Android).
     */
    fun getPlatformPlayer(): Any?

    /**
     * Releases any resources held by the player and performs cleanup.
     * Should be called when the player is no longer needed to avoid memory leaks.
     */
    fun destroy()
}