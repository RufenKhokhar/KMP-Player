# 🎬 KMPPlayer — Kotlin Multiplatform Media Player

**KMPPlayer** is a Kotlin Multiplatform media playback library that delivers a unified, reactive interface to control media playback across **Android** and **iOS** platforms. Internally, it leverages **ExoPlayer** on Android and **AVPlayer** on iOS, while exposing a clean Kotlin API usable from shared code and Jetpack Compose.

---

## ✨ Features

- 🧩 Unified playback interface (`KMPPlayerState`)
- 📁 Supports local files & remote media URLs
- 🎮 Built-in media controls: play, pause, stop, loop
- 📡 Reactive `StateFlow`-based event stream (`KMPPlayerEvent`)
- 🎨 Jetpack Compose support (`KMPPlayer`)
- ⚙️ Access native players: ExoPlayer (Android), AVPlayer (iOS)
- 🧼 Proper resource cleanup with `destroy()`

---

## 📦 Installation

### 1. Add Maven Repository

If published to **Maven Central**:

```kotlin
repositories {
    mavenCentral()
}
```
---

### 2. Add the Dependency

For Kotlin Multiplatform (shared code):

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies  {
            dependencies {
                implementation("io.github.rufenkhokhar:KMP-Player:1.0.1-beta")
            }
        }
    }
}
```

Or for Android-only usage:

```kotlin
dependencies {
    implementation("io.github.rufenkhokhar:KMP-Player:1.0.1-beta")
}
```

---

## ⚡ Quick Start (Jetpack Compose)

```kotlin
val playerState = rememberKMPPlayerState()

KMPPlayer(
    state = playerState,
    showControls = true,
    modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)
)

LaunchedEffect(Unit) {
    playerState.setFileUrl("https://www.sample-videos.com/video321/mp4/360/big_buck_bunny_360p_20mb.mp4")
    playerState.play()

    playerState.observePlayerEvent().collect { event ->
        when (event) {
            KMPPlayerEvent.Buffering -> println("kmp_state: Buffering")
            KMPPlayerEvent.Ended     -> println("kmp_state: Ended")
            is KMPPlayerEvent.Error  -> println("kmp_state: Error: ${event.message}")
            KMPPlayerEvent.Ideal     -> println("kmp_state: Ideal")
            KMPPlayerEvent.Playing   -> println("kmp_state: Playing")
            KMPPlayerEvent.Stop      -> println("kmp_state: Stop")
            KMPPlayerEvent.Paused    -> println("kmp_state: Paused")
        }
    }
}
```

---

## 🧩 API Reference

### 🔧 `KMPPlayerState`

```kotlin
interface KMPPlayerState {
    fun play()
    fun stop()
    fun pause()
    fun isPlaying(): Boolean
    fun setLocalFile(absolutePath: String)
    fun setFileUrl(url: String)
    fun setVideoLoop(loop: Boolean)
    fun observePlayerEvent(): StateFlow<KMPPlayerEvent>
    fun getPlatformPlayer(): Any?
    fun destroy()
}
```

### 📡 `KMPPlayerEvent`

```kotlin
sealed interface KMPPlayerEvent {
    data object Playing : KMPPlayerEvent
    data object Paused : KMPPlayerEvent
    data object Stop : KMPPlayerEvent
    data object Buffering : KMPPlayerEvent
    data object Ideal : KMPPlayerEvent
    data object Ended : KMPPlayerEvent
    data class Error(val message: String) : KMPPlayerEvent
}
```

## 💡 Tips

- Use `setVideoLoop(true)` for infinite looping
- Always call `destroy()` to release player resources when not in use
- For advanced control, use `getPlatformPlayer()` and cast to native types


---

## 📄 License

MIT License  
© 2025 [Rufan Khokhar](https://github.com/rufenkhokhar)

---

## 🤝 Contributing

Pull requests and feature ideas are welcome!  
Open an issue to start the discussion.

---

## 👤 Maintainer

**Rufen Khokhar**  
📧 rufankhokhar@gmail.com  
🔗 [github.com/rufenkhokhar](https://github.com/rufenkhokhar)
