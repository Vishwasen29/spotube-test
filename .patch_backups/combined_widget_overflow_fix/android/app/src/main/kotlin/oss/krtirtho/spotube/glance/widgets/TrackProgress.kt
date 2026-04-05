package oss.krtirtho.spotube.glance.widgets

import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import androidx.glance.unit.ColorProvider
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
  accent: Color = Color(0xFFE8E39A),
  inactive: Color = Color(0xFF4A4A35),
  compact: Boolean = false,
) {
  val position = effectivePositionSeconds(prefs).seconds
  val duration = max(prefs.getInt("duration", 0), 1).seconds
  val progress =
      position.inWholeSeconds.toFloat() / max(duration.inWholeSeconds.toFloat(), 1.0f)

  val barHeights = if (compact) {
    listOf(6, 10, 8, 12, 9, 13, 7, 11, 8, 12, 9, 10)
  } else {
    listOf(8, 14, 10, 18, 12, 20, 9, 16, 11, 18, 8, 14, 10, 18, 12, 20)
  }

  val computedActive = (progress * barHeights.size).toInt()
  val activeCount =
      if (position.inWholeSeconds > 0 && computedActive == 0) 1 else computedActive

  Column(modifier = GlanceModifier.fillMaxWidth()) {
    Row(
      modifier = GlanceModifier.fillMaxWidth(),
      verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
      for (i in barHeights.indices) {
        Box(
          modifier = GlanceModifier
            .height(barHeights[i].dp)
            .width(if (compact) 8.dp else 10.dp)
            .background(color = if (i < activeCount) accent else inactive)
        ) {}
        if (i < barHeights.lastIndex) {
          Spacer(modifier = GlanceModifier.width(if (compact) 3.dp else 4.dp))
        }
      }
    }

    Spacer(modifier = GlanceModifier.height(if (compact) 6.dp else 10.dp))

    Row(
      modifier = GlanceModifier.fillMaxWidth(),
      verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
      Text(
        text = position.format(),
        style = TextStyle(
          fontSize = if (compact) 12.sp else 14.sp,
          color = ColorProvider(Color(0xFFF3F1D0)),
        )
      )
      Spacer(modifier = GlanceModifier.width(8.dp))
      Text(
        text = "/ ${duration.format()}",
        style = TextStyle(
          fontSize = if (compact) 12.sp else 14.sp,
          color = ColorProvider(Color(0xFFF3F1D0)),
        )
      )
    }
  }
}
