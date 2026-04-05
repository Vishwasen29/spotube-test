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
import androidx.glance.layout.defaultWeight
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
    compact: Boolean = true,
    counterText: String? = null,
    showCounterChip: Boolean = true,
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
                    ImageProvider(BitmapFactory.decodeFile(imgLocalPath)),
            contentDescription = "Album Art",
            modifier = GlanceModifier
                .cornerRadius(18.dp)
                .size(if (compact) 68.dp else 80.dp),
            contentScale = ContentScale.Crop,
        )

        Spacer(modifier = GlanceModifier.width(12.dp))

        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = titleText,
                style = TextStyle(
                    fontSize = if (compact) 16.sp else 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlanceTheme.colors.onBackground,
                ),
                maxLines = 1,
            )
            Spacer(modifier = GlanceModifier.size(4.dp))
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Vertical.CenterVertically,
            ) {
                Text(
                    text = artistText,
                    style = TextStyle(
                        fontSize = if (compact) 12.sp else 13.sp,
                        color = GlanceTheme.colors.onBackground,
                    ),
                    maxLines = 1,
                    modifier = GlanceModifier.defaultWeight(),
                )

                if (showCounterChip && !counterText.isNullOrBlank()) {
                    Spacer(modifier = GlanceModifier.width(8.dp))
                    Row(
                        modifier = GlanceModifier
                            .cornerRadius(999.dp)
                            .background(colorProvider = accent)
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.Vertical.CenterVertically,
                    ) {
                        Text(
                            text = counterText,
                            style = TextStyle(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorProvider(Color(0xFF111111)),
                            ),
                        )
                    }
                }
            }
        }
    }
}
