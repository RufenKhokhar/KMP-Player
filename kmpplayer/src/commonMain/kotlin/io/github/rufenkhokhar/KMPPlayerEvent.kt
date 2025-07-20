package io.github.rufenkhokhar

/**
 * Represents the various events that can occur within the KMPPlayer.
 * These events are used to communicate the state of the media player
 * to the application.
 */
sealed interface KMPPlayerEvent {

    /**
     * Indicates that the player has started or resumed playback.
     */
    data object Playing: KMPPlayerEvent

    /**
     * Indicates that the player has been paused.
     */
    data object Paused: KMPPlayerEvent

    /**
     * Indicates that the player has been stopped and reset.
     */
    data object Stop: KMPPlayerEvent

    /**
     * Indicates that the player is currently buffering media content.
     */
    data object Buffering: KMPPlayerEvent

    /**
     * Indicates that the player is in an idle state, neither playing, paused, nor buffering.
     * This is often the initial state or the state after an error.
     */
    data object Ideal: KMPPlayerEvent

    /**
     * Indicates that the playback of the current media has reached its end.
     */
    data object Ended: KMPPlayerEvent

    /**
     * Indicates that an error has occurred during playback.
     * @param message A descriptive message about the error.
     */
    data class Error(val message: String): KMPPlayerEvent

}
