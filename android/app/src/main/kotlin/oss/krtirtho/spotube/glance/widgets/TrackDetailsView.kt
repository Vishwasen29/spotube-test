package oss.krtirtho.spotube.glance.widgets

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.defaultWeight
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.color.ColorProvider
import oss.krtirtho.spotube.glance.models.Track

private val titleColor = ColorProvider(Color(0xFFFFFFFF))
private val artistColor = ColorProvider(Color(0xFFB7B7BE))

@Composable
fun TrackDetailsView(activeTrack: Track?, compact: Boolean = false) {
    val context = LocalContext.current
    val artistStr = activeTrack?.artists?.joinToString(", ") { it.name } ?: "Unknown artist"
    val imgLocalPath = activeTrack?.album?.images?.firstOrNull()?.path
    val title = activeTrack?.name ?: "Nothing playing"

    Row(
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Image(
            provider =
                if (imgLocalPath == null)
                    ImageProvider(android.R.drawable.ic_media_play)
                else ImageProvider(BitmapFactory.decodeFile(imgLocalPath)),
            contentDescription = "Album Art",
            modifier = GlanceModifier
                .size(if (compact) 52.dp else 68.dp),
            contentScale = ContentScale.Crop,
        )
        Spacer(modifier = GlanceModifier.size(10.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = title,
                maxLines = if (compact) 1 else 2,
                style = TextStyle(
                    fontSize = if (compact) 14.sp else 16.sp,
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
    }
}
