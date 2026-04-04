package oss.krtirtho.spotube.glance.widgets

import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import kotlin.math.max
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

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
fun TrackProgress(
  prefs: SharedPreferences,
  accent: Color = Color(0xFFFF5A36),
  inactive: Color = Color(0xFF31313A),
) {
  val position = effectivePositionSeconds(prefs).seconds
  val duration = max(prefs.getInt("duration", 0), 1).seconds
  val progress =
      position.inWholeSeconds.toFloat() / max(duration.inWholeSeconds.toFloat(), 1.0f)

  val barHeights = listOf(4, 8, 6, 10, 7, 12, 5, 9, 6, 11, 4, 8, 6, 10, 7, 12, 5, 9, 6, 11)
  val activeCount = (progress * barHeights.size).toInt()

  Column(modifier = GlanceModifier.fillMaxWidth()) {
    Row(
      modifier = GlanceModifier.fillMaxWidth(),
      verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
      for (i in barHeights.indices) {
        Box(
          modifier = GlanceModifier
            .height(barHeights[i].dp)
            .width(8.dp)
            .background(color = if (i < activeCount) accent else inactive)
        ) {}
        if (i < barHeights.lastIndex) {
          Spacer(modifier = GlanceModifier.width(3.dp))
        }
      }
    }

    Spacer(modifier = GlanceModifier.height(8.dp))

    Row(
      modifier = GlanceModifier.fillMaxWidth(),
      horizontalAlignment = Alignment.Horizontal.SpaceBetween
    ) {
      Text(text = position.format(), style = TextStyle())
      Text(text = duration.format(), style = TextStyle())
    }
  }
}
