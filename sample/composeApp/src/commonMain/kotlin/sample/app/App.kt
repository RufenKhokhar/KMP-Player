package sample.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.rufenkhokhar.KMPPlayer
import io.github.rufenkhokhar.KMPPlayerEvent
import io.github.rufenkhokhar.KMPPlayerState
import io.github.rufenkhokhar.rememberKMPPlayerState
import kotlin.math.max
import kotlin.math.roundToInt

@OptIn(ExperimentalMultiplatform::class)
@Composable
fun App() {
    val playerState = rememberKMPPlayerState()
    Column(
        modifier = Modifier
            .safeDrawingPadding()
            .fillMaxSize()
            .background(Color.White)
    ) {
        KMPPlayer(
            state = playerState,
            showControls = false,
            modifier = Modifier.fillMaxWidth().height(300.dp).background(Color.Black)
        )
        PlayerController(
            player = playerState,
            modifier = Modifier.fillMaxWidth()
                .weight(1f)
        )
    }

    LaunchedEffect(Unit) {
        playerState.setFileUrl("https://www.sample-videos.com/video321/mp4/360/big_buck_bunny_360p_20mb.mp4")
        playerState.play()
    }
}


@OptIn(ExperimentalMultiplatform::class)
@Composable
fun PlayerController(
    player: KMPPlayerState,
    modifier: Modifier = Modifier
) {
    val event by remember(player) { player.observePlayerEvent() }.collectAsState(initial = KMPPlayerEvent.Ideal)

    var positionMs by remember { mutableStateOf(0L) }
    var durationMs by remember { mutableStateOf(0L) }
    var volume by remember { mutableStateOf(1f) }
    var speed by remember { mutableStateOf(1f) }
    var loop by remember { mutableStateOf(false) }
    var scrubbing by remember { mutableStateOf(false) }
    var url by remember { mutableStateOf("") }
    var localPath by remember { mutableStateOf("") }
    LaunchedEffect(loop) { player.setVideoLoop(loop) }

    Column(
        modifier = modifier.padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Text(
            text = when (event) {
                is KMPPlayerEvent.Playing ->{
                    if (!scrubbing){

                        positionMs = (event as KMPPlayerEvent.Playing).currentPosition
                        durationMs = (event as KMPPlayerEvent.Playing).duration
                    }
                    "Playing ${formatMs(positionMs)} / ${formatMs(durationMs)}"
                }


                is KMPPlayerEvent.Paused -> "Paused"
                is KMPPlayerEvent.Buffering -> "Buffering…"
                is KMPPlayerEvent.Ended -> "Ended"
                is KMPPlayerEvent.Error -> "Error"
                is KMPPlayerEvent.Ideal -> "Idle"
                KMPPlayerEvent.Stop -> "Stoped"
            },
            style = MaterialTheme.typography.titleMedium
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val safeDuration = max(1f, durationMs.toFloat())
            Slider(
                value = positionMs.coerceIn(0, durationMs).toFloat(),
                onValueChange = {
                    scrubbing = true
                    positionMs = it.toLong()
                },
                onValueChangeFinished = {
                    scrubbing = false
                    player.seekTo(positionMs)
                },
                valueRange = 0f..safeDuration,
                modifier = Modifier.fillMaxWidth()
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatMs(positionMs), style = MaterialTheme.typography.labelSmall)
                Text(formatMs(durationMs), style = MaterialTheme.typography.labelSmall)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                player.play()
                positionMs = player.getCurrentPosition()
                durationMs = player.getDuration()
            }) { Text("Play") }

            Button(onClick = { player.pause() }) { Text("Pause") }

            OutlinedButton(onClick = {
                player.stop()
                positionMs = 0
                durationMs = player.getDuration()
            }) { Text("Stop") }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = loop, onCheckedChange = { loop = it })
            Spacer(Modifier.width(8.dp))
            Text("Loop")
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Volume: ${percent0(volume)}%")
            Slider(
                value = volume.coerceIn(0f, 1f),
                onValueChange = {
                    volume = it
                    player.setVolume(it)
                },
                valueRange = 0f..1f
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Speed: ${oneDecimal(speed)}x")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0.5f, 1f, 1.5f, 2f).forEach { s ->
                    val selected = speed == s
                    val onClick = {
                        speed = s
                        player.setPlaybackSpeed(s)
                    }
                    if (selected) {
                        Button(onClick = onClick) { Text("${oneDecimal(s)}x") }
                    } else {
                        OutlinedButton(onClick = onClick) { Text("${oneDecimal(s)}x") }
                    }
                }
            }
        }

        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("Remote URL") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                val cleaned = url.trim()
                if (cleaned.isNotEmpty()) {
                    player.setFileUrl(cleaned)
                    positionMs = 0
                }
            }) { Text("Load URL") }
        }


        OutlinedTextField(
            value = localPath,
            onValueChange = { localPath = it },
            label = { Text("Local file path") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                val cleaned = localPath.trim()
                if (cleaned.isNotEmpty()) {
                    player.setLocalFile(cleaned)
                    positionMs = 0
                }
            }) { Text("Load File") }
        }
    }
}


private fun formatMs(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    val mm = m.toString().padStart(2, '0')
    val ss = s.toString().padStart(2, '0')
    return if (h > 0) "$h:$mm:$ss" else "$mm:$ss"
}

private fun percent0(value: Float): Int {
    // 0..1 -> 0..100, rounded
    return (value.coerceIn(0f, 1f) * 100f).roundToInt()
}

private fun oneDecimal(value: Float): String {
    // Round to one decimal without platform formatting
    val clamped = value
    val scaled = (clamped * 10f).roundToInt() // e.g., 1.26 -> 13
    val intPart = scaled / 10
    val frac = scaled % 10
    return if (frac == 0) intPart.toString() else "$intPart.$frac"
}



