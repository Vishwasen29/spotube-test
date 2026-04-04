package oss.krtirtho.spotube.glance

import HomeWidgetGlanceState
import HomeWidgetGlanceStateDefinition
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.graphics.drawable.Icon
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.background
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.state.GlanceStateDefinition
import com.google.gson.Gson
import es.antonborri.home_widget.HomeWidgetBackgroundIntent
import es.antonborri.home_widget.actionStartActivity
import oss.krtirtho.spotube.MainActivity
import oss.krtirtho.spotube.glance.models.Track
import oss.krtirtho.spotube.glance.widgets.TrackDetailsView
import oss.krtirtho.spotube.glance.widgets.TrackProgress

private val gson = Gson()
private val serverAddressKey = ActionParameters.Key<String>("serverAddress")

class Breakpoints {
    companion object {
        val SMALL_SQUARE = DpSize(120.dp, 120.dp)
        val HORIZONTAL_RECTANGLE = DpSize(300.dp, 150.dp)
        val BIG_SQUARE = DpSize(300.dp, 300.dp)
    }
}

private fun computeAccent(track: Track?): Color {
    val path = track?.album?.images?.firstOrNull()?.path ?: return Color(0xFFFF5A36)

    return try {
        val bitmap = BitmapFactory.decodeFile(path) ?: return Color(0xFFFF5A36)
        val small = Bitmap.createScaledBitmap(bitmap, 20, 20, true)

        var r = 0
        var g = 0
        var b = 0
        var count = 0

        for (x in 0 until small.width) {
            for (y in 0 until small.height) {
                val pixel = small.getPixel(x, y)
                if (AndroidColor.alpha(pixel) < 180) continue

                val hsv = FloatArray(3)
                AndroidColor.colorToHSV(pixel, hsv)
                if (hsv[1] < 0.18f || hsv[2] < 0.18f) continue

                r += AndroidColor.red(pixel)
                g += AndroidColor.green(pixel)
                b += AndroidColor.blue(pixel)
                count++
            }
        }

        if (count == 0) return Color(0xFFFF5A36)

        val rr = (r / count).coerceIn(70, 255)
        val gg = (g / count).coerceIn(55, 220)
        val bb = (b / count).coerceIn(55, 220)
        Color(AndroidColor.rgb(rr, gg, bb))
    } catch (_: Throwable) {
        Color(0xFFFF5A36)
    }
}

private fun provider(color: Color): ColorProvider = ColorProvider(day = color, night = color)

class HomePlayerWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(
        setOf(
            Breakpoints.SMALL_SQUARE,
            Breakpoints.HORIZONTAL_RECTANGLE,
            Breakpoints.BIG_SQUARE
        )
    )

    override val stateDefinition: GlanceStateDefinition<*>?
        get() = HomeWidgetGlanceStateDefinition()

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceContent(currentState())
        }
    }

    @OptIn(ExperimentalGlancePreviewApi::class)
    @Preview(widthDp = 300, heightDp = 150)
    @Composable
    private fun GlanceContent(currentState: HomeWidgetGlanceState) {
        val context = LocalContext.current
        val prefs = currentState.preferences
        val size = LocalSize.current

        val activeTrackStr = prefs.getString("activeTrack", null)
        val isPlaying = prefs.getBoolean("isPlaying", false)
        val isShuffled = prefs.getBoolean("isShuffled", false)
        val loopMode = prefs.getString("loopMode", "none") ?: "none"
        val playbackServerAddress = prefs.getString("playbackServerAddress", null) ?: ""
        val currentIndex = prefs.getInt("currentIndex", 0)
        val queueLength = prefs.getInt("queueLength", 0)

        val activeTrack = activeTrackStr?.let {
            runCatching { gson.fromJson(it, Track::class.java) }.getOrNull()
        }

        val accent = computeAccent(activeTrack)
        val accentSoft = accent.copy(alpha = 0.18f)
        val counterText =
            if (queueLength > 0 && currentIndex > 0) "$currentIndex/$queueLength" else null

        val playIcon = Icon.createWithResource(context, android.R.drawable.ic_media_play)
        val pauseIcon = Icon.createWithResource(context, android.R.drawable.ic_media_pause)
        val previousIcon = Icon.createWithResource(context, android.R.drawable.ic_media_previous)
        val nextIcon = Icon.createWithResource(context, android.R.drawable.ic_media_next)
        val shuffleIcon = Icon.createWithResource(context, android.R.drawable.ic_menu_rotate)
        val repeatIcon = Icon.createWithResource(context, android.R.drawable.ic_popup_sync)

        val surface = GlanceTheme.colors.surface.getColor(context)
        val surfaceVariant = GlanceTheme.colors.surfaceVariant.getColor(context)
        val primaryContainer = GlanceTheme.colors.primaryContainer.getColor(context)

        GlanceTheme {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(24.dp)
                    .background(color = surface)
                    .clickable { actionStartActivity<MainActivity>(context) }
            ) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(color = accentSoft)
                ) {}

                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    TrackDetailsView(
                        activeTrack = activeTrack,
                        compact = size.height <= 145.dp,
                        counterText = counterText,
                        accent = accent,
                    )

                    Spacer(modifier = GlanceModifier.height(10.dp))

                    TrackProgress(
                        prefs = prefs,
                        accent = accent,
                        inactive = primaryContainer,
                    )

                    Spacer(modifier = GlanceModifier.height(10.dp))

                    if (size.width > size.height) {
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Vertical.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                                CircleIconButton(
                                    imageProvider = ImageProvider(previousIcon),
                                    contentDescription = "Previous",
                                    onClick = actionRunCallback<PreviousAction>(
                                        parameters = actionParametersOf(serverAddressKey to playbackServerAddress)
                                    ),
                                    backgroundColor = provider(surfaceVariant),
                                )
                                Spacer(modifier = GlanceModifier.width(8.dp))
                                CircleIconButton(
                                    imageProvider = if (isPlaying) ImageProvider(pauseIcon) else ImageProvider(playIcon),
                                    contentDescription = "Play/Pause",
                                    onClick = actionRunCallback<PlayPauseAction>(
                                        parameters = actionParametersOf(serverAddressKey to playbackServerAddress)
                                    ),
                                    backgroundColor = provider(accent),
                                    contentColor = provider(Color(0xFF111111)),
                                )
                                Spacer(modifier = GlanceModifier.width(8.dp))
                                CircleIconButton(
                                    imageProvider = ImageProvider(nextIcon),
                                    contentDescription = "Next",
                                    onClick = actionRunCallback<NextAction>(
                                        parameters = actionParametersOf(serverAddressKey to playbackServerAddress)
                                    ),
                                    backgroundColor = provider(surfaceVariant),
                                )
                            }

                            Spacer(modifier = GlanceModifier.width(10.dp))

                            Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                                CircleIconButton(
                                    imageProvider = ImageProvider(shuffleIcon),
                                    contentDescription = "Shuffle",
                                    onClick = actionRunCallback<ShuffleAction>(
                                        parameters = actionParametersOf(serverAddressKey to playbackServerAddress)
                                    ),
                                    backgroundColor = provider(if (isShuffled) accent else surfaceVariant),
                                    contentColor = provider(if (isShuffled) Color(0xFF111111) else Color(0xFFFFFFFF)),
                                )
                                Spacer(modifier = GlanceModifier.width(8.dp))
                                CircleIconButton(
                                    imageProvider = ImageProvider(repeatIcon),
                                    contentDescription = "Repeat",
                                    onClick = actionRunCallback<RepeatAction>(
                                        parameters = actionParametersOf(serverAddressKey to playbackServerAddress)
                                    ),
                                    backgroundColor = provider(if (loopMode != "none") accent else surfaceVariant),
                                    contentColor = provider(if (loopMode != "none") Color(0xFF111111) else Color(0xFFFFFFFF)),
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                            verticalAlignment = Alignment.Vertical.CenterVertically
                        ) {
                            CircleIconButton(
                                imageProvider = ImageProvider(previousIcon),
                                contentDescription = "Previous",
                                onClick = actionRunCallback<PreviousAction>(
                                    parameters = actionParametersOf(serverAddressKey to playbackServerAddress)
                                ),
                                backgroundColor = provider(surfaceVariant),
                            )
                            Spacer(modifier = GlanceModifier.width(8.dp))
                            CircleIconButton(
                                imageProvider = if (isPlaying) ImageProvider(pauseIcon) else ImageProvider(playIcon),
                                contentDescription = "Play/Pause",
                                onClick = actionRunCallback<PlayPauseAction>(
                                    parameters = actionParametersOf(serverAddressKey to playbackServerAddress)
                                ),
                                backgroundColor = provider(accent),
                                contentColor = provider(Color(0xFF111111)),
                            )
                            Spacer(modifier = GlanceModifier.width(8.dp))
                            CircleIconButton(
                                imageProvider = ImageProvider(nextIcon),
                                contentDescription = "Next",
                                onClick = actionRunCallback<NextAction>(
                                    parameters = actionParametersOf(serverAddressKey to playbackServerAddress)
                                ),
                                backgroundColor = provider(surfaceVariant),
                            )
                        }

                        Spacer(modifier = GlanceModifier.height(8.dp))

                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                            verticalAlignment = Alignment.Vertical.CenterVertically
                        ) {
                            CircleIconButton(
                                imageProvider = ImageProvider(shuffleIcon),
                                contentDescription = "Shuffle",
                                onClick = actionRunCallback<ShuffleAction>(
                                    parameters = actionParametersOf(serverAddressKey to playbackServerAddress)
                                ),
                                backgroundColor = provider(if (isShuffled) accent else surfaceVariant),
                                contentColor = provider(if (isShuffled) Color(0xFF111111) else Color(0xFFFFFFFF)),
                            )
                            Spacer(modifier = GlanceModifier.width(8.dp))
                            CircleIconButton(
                                imageProvider = ImageProvider(repeatIcon),
                                contentDescription = "Repeat",
                                onClick = actionRunCallback<RepeatAction>(
                                    parameters = actionParametersOf(serverAddressKey to playbackServerAddress)
                                ),
                                backgroundColor = provider(if (loopMode != "none") accent else surfaceVariant),
                                contentColor = provider(if (loopMode != "none") Color(0xFF111111) else Color(0xFFFFFFFF)),
                            )
                        }
                    }
                }
            }
        }
    }
}

class PlayPauseAction : InteractiveAction("toggle-playback")
class NextAction : InteractiveAction("next")
class PreviousAction : InteractiveAction("previous")
class ShuffleAction : InteractiveAction("toggle-shuffle")
class RepeatAction : InteractiveAction("cycle-loop")

abstract class InteractiveAction(private val command: String) : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val serverAddress = parameters[serverAddressKey] ?: ""
        Log.d("HomePlayerWidget", "Sending command $command to $serverAddress")
        if (serverAddress.isEmpty()) return

        val backgroundIntent = HomeWidgetBackgroundIntent.getBroadcast(
            context,
            Uri.parse("spotube://playback/$command?serverAddress=$serverAddress")
        )
        backgroundIntent.send()
    }
}
