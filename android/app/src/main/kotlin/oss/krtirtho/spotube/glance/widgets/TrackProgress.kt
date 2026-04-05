package oss.krtirtho.spotube.glance.widgets

import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.width
import kotlin.math.max

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
  inactive: Color = Color(0xFF433A38),
  compact: Boolean = true,
) {
  val duration = max(prefs.getInt("duration", 0), 1)
  val position = effectivePositionSeconds(prefs)
  val progress = position.toFloat() / max(duration.toFloat(), 1f)

  val segments = if (compact) 16 else 20
  val activeCount = if (position > 0) max(1, (progress * segments).toInt()) else 0
  val barHeight = if (compact) 4.dp else 6.dp
  val barWidth = if (compact) 12.dp else 10.dp
  val gap = if (compact) 4.dp else 3.dp

  Row(
    modifier = GlanceModifier.fillMaxWidth(),
    verticalAlignment = Alignment.Vertical.CenterVertically
  ) {
    for (i in 0 until segments) {
      Box(
        modifier = GlanceModifier
          .height(barHeight)
          .width(barWidth)
          .background(color = if (i < activeCount) accent else inactive)
      ) {}
      if (i < segments - 1) {
        Spacer(modifier = GlanceModifier.width(gap))
      }
    }
  }
}
