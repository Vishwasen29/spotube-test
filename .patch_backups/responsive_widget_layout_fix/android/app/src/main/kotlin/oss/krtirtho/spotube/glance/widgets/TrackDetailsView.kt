package oss.krtirtho.spotube.glance.widgets

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import oss.krtirtho.spotube.glance.models.Track

@Composable
fun TrackDetailsView(
    activeTrack: Track?,
    compact: Boolean = false,
    counterText: String? = null,
    accent: ColorProvider = ColorProvider(Color(0xFFFF5A36)),
) {
    val context = LocalContext.current

    val artistText: String = activeTrack?.artists
        ?.mapNotNull { it.name }
        ?.joinToString(", ")
        ?.ifBlank { "Unknown artist" }
        ?: "Unknown artist"

    val imgLocalPath: String? = activeTrack?.album?.images?.firstOrNull()?.path
    val titleText: String = activeTrack?.name ?: "Nothing playing"

    Column(modifier = GlanceModifier.fillMaxWidth()) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Image(
                provider =
                    if (imgLocalPath.isNullOrBlank())
                        ImageProvider(
                            BitmapFactory.decodeResource(
                                context.resources,
                                android.R.drawable.ic_media_play
                            )
                        )
                    else
                        ImageProvider(
                            BitmapFactory.decodeFile(imgLocalPath!!)
                        ),
                contentDescription = "Album Art",
                modifier = GlanceModifier
                    .cornerRadius(16.dp)
                    .size(if (compact) 56.dp else 72.dp),
                contentScale = ContentScale.Crop,
            )

            Spacer(modifier = GlanceModifier.width(12.dp))

            Column(modifier = GlanceModifier.fillMaxWidth()) {
                Text(
                    text = titleText,
                    style = TextStyle(
                        fontSize = if (compact) 15.sp else 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.onBackground,
                    ),
                    maxLines = if (compact) 1 else 2,
                )
                Spacer(modifier = GlanceModifier.size(4.dp))
                Text(
                    text = artistText,
                    style = TextStyle(
                        fontSize = if (compact) 12.sp else 13.sp,
                        color = GlanceTheme.colors.onBackground,
                    ),
                    maxLines = 1,
                )
            }
        }

        if (!counterText.isNullOrBlank()) {
            Spacer(modifier = GlanceModifier.size(6.dp))
            Row(
                modifier = GlanceModifier
                    .cornerRadius(999.dp)
                    .background(colorProvider = accent)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.Vertical.CenterVertically,
            ) {
                Text(
                    text = counterText,
                    style = TextStyle(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(Color(0xFF111111)),
                    ),
                )
            }
        }
    }
}
