package oss.krtirtho.spotube.glance.widgets

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.cornerRadius
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.defaultWeight
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.background
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import oss.krtirtho.spotube.glance.models.Track

private val titleColor = ColorProvider(Color(0xFFFFFFFF))
private val artistColor = ColorProvider(Color(0xFFB7B7BE))
private val chipText = ColorProvider(Color(0xFF111111))

@Composable
fun TrackDetailsView(
    activeTrack: Track?,
    compact: Boolean = false,
    counterText: String? = null,
    accent: ColorProvider,
) {
    val artistStr = activeTrack?.artists?.joinToString(", ") { it.name } ?: "Unknown artist"
    val imgLocalPath = activeTrack?.album?.images?.firstOrNull()?.path
    val title = activeTrack?.name ?: "Nothing playing"

    Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
        Image(
            provider =
                if (imgLocalPath == null)
                    ImageProvider(android.R.drawable.ic_media_play)
                else ImageProvider(BitmapFactory.decodeFile(imgLocalPath)),
            contentDescription = "Album Art",
            modifier = GlanceModifier
                .cornerRadius(16.dp)
                .size(if (compact) 56.dp else 72.dp),
            contentScale = ContentScale.Crop,
        )

        Spacer(modifier = GlanceModifier.width(12.dp))

        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = title,
                maxLines = if (compact) 1 else 2,
                style = TextStyle(
                    fontSize = if (compact) 15.sp else 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = titleColor,
                ),
            )
            Spacer(modifier = GlanceModifier.size(4.dp))
            Text(
                text = artistStr,
                maxLines = 1,
                style = TextStyle(
                    fontSize = if (compact) 12.sp else 13.sp,
                    color = artistColor,
                ),
            )
        }

        if (counterText != null) {
            Spacer(modifier = GlanceModifier.width(8.dp))
            Row(
                modifier = GlanceModifier
                    .cornerRadius(999.dp)
                    .background(accent)
                    .width(52.dp),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Spacer(modifier = GlanceModifier.width(10.dp))
                Text(
                    text = counterText,
                    style = TextStyle(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = chipText,
                    ),
                )
                Spacer(modifier = GlanceModifier.width(10.dp))
            }
        }
    }
}
