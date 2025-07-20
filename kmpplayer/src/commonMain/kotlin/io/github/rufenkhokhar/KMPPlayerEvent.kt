package io.github.rufenkhokhar

sealed interface KMPPlayerEvent {

    data object Playing: KMPPlayerEvent
    data object Paused: KMPPlayerEvent
    data object Stop: KMPPlayerEvent
    data object Buffering: KMPPlayerEvent
    data object Ideal: KMPPlayerEvent
    data object Ended: KMPPlayerEvent
    data class Error(val message: String): KMPPlayerEvent

}