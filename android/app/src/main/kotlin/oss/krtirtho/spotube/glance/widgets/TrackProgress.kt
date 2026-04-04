package oss.krtirtho.spotube.glance.widgets

import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.defaultWeight
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import kotlin.math.max
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val activeBar = ColorProvider(Color(0xFFFF3B30))
private val inactiveBar = ColorProvider(Color(0xFF2A2A31))
private val timerText = ColorProvider(Color(0xFFD6D6DB))

fun Duration.format(): String {
  return this.toComponents { hour, minutes, seconds, _ ->
    val paddedSeconds = seconds.toString().padStart(2, '0')
    val paddedMinutes = minutes.toString().padStart(2, '0')
    val paddedHour = hour.toString().padStart(2, '0')
    if (hour == 0L) {
      "$paddedMinutes:$paddedSeconds"
    } else {
      "$paddedHour:$paddedMinutes:$paddedSeconds"
    }
  }
}

private fun effectivePositionSeconds(prefs: SharedPreferences): Int {
  val stored = prefs.getInt("position", 0)
  val duration = max(prefs.getInt("duration", 0), 0)
  val isPlaying = prefs.getBoolean("isPlaying", false)
  val updatedAt = prefs.getLong("positionTimestampMs", 0L)

  if (!isPlaying || updatedAt <= 0L) {
    return stored.coerceAtMost(duration)
  }

  val elapsedSeconds = ((System.currentTimeMillis() - updatedAt) / 1000L).toInt()
  return (stored + elapsedSeconds).coerceAtMost(duration)
}

@Composable
private fun WaveBar(progress: Float, index: Int, total: Int) {
  val barHeights = listOf(4, 8, 6, 10, 7, 12, 5, 9, 6, 11, 4, 8, 6, 10, 7, 12, 5, 9, 6, 11)
  val activeCount = (progress * total).toInt()
  val heightDp = barHeights[index % barHeights.size].dp
  Box(
    modifier = GlanceModifier
      .defaultWeight()
      .height(heightDp)
      .background(if (index < activeCount) activeBar else inactiveBar)
  ) {}
}

@Composable
fun TrackProgress(prefs: SharedPreferences) {
  val position = effectivePositionSeconds(prefs).seconds
  val duration = max(prefs.getInt("duration", 0), 1).seconds
  val progress =
      position.inWholeSeconds.toFloat() / max(duration.inWholeSeconds.toFloat(), 1.0f)

  Column(modifier = GlanceModifier.fillMaxWidth()) {
    Row(
      modifier = GlanceModifier.fillMaxWidth(),
      verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
      for (i in 0 until 20) {
        WaveBar(progress = progress, index = i, total = 20)
        if (i < 19) {
          Spacer(modifier = GlanceModifier.width(3.dp))
        }
      }
    }
    Spacer(modifier = GlanceModifier.height(8.dp))
    Row(modifier = GlanceModifier.fillMaxWidth()) {
      Text(
        text = position.format(),
        style = TextStyle(
          color = timerText,
          fontWeight = FontWeight.Medium,
          fontSize = 12.sp,
        ),
      )
      Spacer(modifier = GlanceModifier.defaultWeight())
      Text(
        text = duration.format(),
        style = TextStyle(
          color = timerText,
          fontWeight = FontWeight.Medium,
          fontSize = 12.sp,
        ),
      )
    }
  }
}
