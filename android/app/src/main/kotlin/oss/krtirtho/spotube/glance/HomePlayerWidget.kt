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
import androidx.glance.unit.ColorProvider
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
        val SMALL_WIDE = DpSize(220.dp, 96.dp)
        val MEDIUM_WIDE = DpSize(280.dp, 108.dp)
        val LARGE_WIDE = DpSize(340.dp, 120.dp)
    }
}

private fun computeAccent(track: Track?): Color {
    val path = track?.album?.images?.firstOrNull()?.path ?: return Color(0xFF7A3844)

    return try {
        val bitmap = BitmapFactory.decodeFile(path) ?: return Color(0xFF7A3844)
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
                if (hsv[1] < 0.18f || hsv[2] < 0.16f) continue

                r += AndroidColor.red(pixel)
                g += AndroidColor.green(pixel)
                b += AndroidColor.blue(pixel)
                count++
            }
        }

        if (count == 0) return Color(0xFF7A3844)

        val rr = (r / count).coerceIn(70, 220)
        val gg = (g / count).coerceIn(50, 180)
        val bb = (b / count).coerceIn(50, 180)
        Color(AndroidColor.rgb(rr, gg, bb))
    } catch (_: Throwable) {
        Color(0xFF7A3844)
    }
}

private fun cp(color: Color): ColorProvider = ColorProvider(color)

class HomePlayerWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(
        setOf(
            Breakpoints.SMALL_WIDE,
            Breakpoints.MEDIUM_WIDE,
            Breakpoints.LARGE_WIDE,
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
    @Preview(widthDp = 280, heightDp = 108)
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
        val accentSoft = accent.copy(alpha = 0.88f)
        val counterText =
            if (queueLength > 0 && currentIndex > 0) "$currentIndex/$queueLength" else null

        val shuffleIcon = Icon.createWithResource(context, android.R.drawable.ic_menu_rotate)
        val previousIcon = Icon.createWithResource(context, android.R.drawable.ic_media_previous)
        val playIcon = Icon.createWithResource(context, android.R.drawable.ic_media_play)
        val pauseIcon = Icon.createWithResource(context, android.R.drawable.ic_media_pause)
        val nextIcon = Icon.createWithResource(context, android.R.drawable.ic_media_next)
        val repeatIcon = Icon.createWithResource(context, android.R.drawable.ic_popup_sync)

        val showExtra = size.width >= 300.dp
        val showCounter = size.width >= 250.dp
        val small = size.width < 250.dp

        GlanceTheme {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(26.dp)
                    .background(color = accentSoft)
                    .clickable(onClick = actionStartActivity<MainActivity>(context))
                    .padding(if (small) 10.dp else 12.dp)
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxSize(),
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    Box(
                        modifier = GlanceModifier.width(if (small) 72.dp else 84.dp)
                    ) {
                        TrackDetailsView(
                            activeTrack = activeTrack,
                            compact = true,
                            counterText = null,
                            showCounterChip = false,
                            accent = cp(accent),
                        )
                    }

                    Spacer(modifier = GlanceModifier.width(12.dp))

                    Column(modifier = GlanceModifier.fillMaxSize()) {
                        TrackDetailsView(
                            activeTrack = activeTrack,
                            compact = true,
                            counterText = if (showCounter) counterText else null,
                            showCounterChip = showCounter,
                            accent = cp(Color(0xFFE9E3D6)),
                        )

                        Spacer(modifier = GlanceModifier.height(8.dp))

                        TrackProgress(
                            prefs = prefs,
                            accent = Color(0xFFF1E7C8),
                            inactive = Color(0x6BFFFFFF),
                            compact = true,
                        )

                        Spacer(modifier = GlanceModifier.height(10.dp))

                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                            verticalAlignment = Alignment.Vertical.CenterVertically
                        ) {
                            if (showExtra) {
                                CircleIconButton(
                                    imageProvider = ImageProvider(shuffleIcon),
                                    contentDescription = "Shuffle",
                                    onClick = actionRunCallback<ShuffleAction>(
                                        parameters = actionParametersOf(serverAddressKey to playbackServerAddress)
                                    ),
                                    modifier = GlanceModifier.size(30.dp),
                                    backgroundColor = cp(if (isShuffled) Color(0xFFF1E7C8) else Color(0x3FFFFFFF)),
                                    contentColor = cp(if (isShuffled) Color(0xFF231A14) else Color(0xFFF6F2EC)),
                                )
                                Spacer(modifier = GlanceModifier.width(10.dp))
                            }

                            CircleIconButton(
                                imageProvider = ImageProvider(previousIcon),
                                contentDescription = "Previous",
                                onClick = actionRunCallback<PreviousAction>(
                                    parameters = actionParametersOf(serverAddressKey to playbackServerAddress)
                                ),
                                modifier = GlanceModifier.size(34.dp),
                                backgroundColor = cp(Color(0x2AFFFFFF)),
                                contentColor = cp(Color(0xFFF6F2EC)),
                            )
                            Spacer(modifier = GlanceModifier.width(10.dp))
                            CircleIconButton(
                                imageProvider = if (isPlaying) ImageProvider(pauseIcon) else ImageProvider(playIcon),
                                contentDescription = "Play/Pause",
                                onClick = actionRunCallback<PlayPauseAction>(
                                    parameters = actionParametersOf(serverAddressKey to playbackServerAddress)
                                ),
                                modifier = GlanceModifier.size(40.dp),
                                backgroundColor = cp(Color(0xFFF6F2EC)),
                                contentColor = cp(Color(0xFF2D221A)),
                            )
                            Spacer(modifier = GlanceModifier.width(10.dp))
                            CircleIconButton(
                                imageProvider = ImageProvider(nextIcon),
                                contentDescription = "Next",
                                onClick = actionRunCallback<NextAction>(
                                    parameters = actionParametersOf(serverAddressKey to playbackServerAddress)
                                ),
                                modifier = GlanceModifier.size(34.dp),
                                backgroundColor = cp(Color(0x2AFFFFFF)),
                                contentColor = cp(Color(0xFFF6F2EC)),
                            )

                            if (showExtra) {
                                Spacer(modifier = GlanceModifier.width(10.dp))
                                CircleIconButton(
                                    imageProvider = ImageProvider(repeatIcon),
                                    contentDescription = "Repeat",
                                    onClick = actionRunCallback<RepeatAction>(
                                        parameters = actionParametersOf(serverAddressKey to playbackServerAddress)
                                    ),
                                    modifier = GlanceModifier.size(30.dp),
                                    backgroundColor = cp(if (loopMode != "none") Color(0xFFF1E7C8) else Color(0x2AFFFFFF)),
                                    contentColor = cp(if (loopMode != "none") Color(0xFF231A14) else Color(0xFFF6F2EC)),
                                )
                            }
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
