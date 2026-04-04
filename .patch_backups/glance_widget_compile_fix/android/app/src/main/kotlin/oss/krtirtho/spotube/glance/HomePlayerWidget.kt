package oss.krtirtho.spotube.glance

import HomeWidgetGlanceState
import HomeWidgetGlanceStateDefinition
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
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
import androidx.glance.layout.defaultWeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.google.gson.Gson
import es.antonborri.home_widget.HomeWidgetBackgroundIntent
import oss.krtirtho.spotube.MainActivity
import oss.krtirtho.spotube.glance.models.Track
import oss.krtirtho.spotube.glance.widgets.TrackDetailsView
import oss.krtirtho.spotube.glance.widgets.TrackProgress

private val gson = Gson()
private val serverAddressKey = ActionParameters.Key<String>("serverAddress")

private val defaultAccent = ColorProvider(Color(0xFFFF5A36))
private val widgetSurface = ColorProvider(Color(0xFF0B0B0F))
private val widgetSurface2 = ColorProvider(Color(0xFF17171C))
private val widgetText = ColorProvider(Color(0xFFFFFFFF))
private val widgetSubText = ColorProvider(Color(0xFFB7B7BE))
private val widgetOutline = ColorProvider(Color(0xFF2A2A31))

class Breakpoints {
    companion object {
        val SMALL_SQUARE = DpSize(120.dp, 120.dp)
        val HORIZONTAL_RECTANGLE = DpSize(300.dp, 150.dp)
        val BIG_SQUARE = DpSize(300.dp, 300.dp)
    }
}

private data class WidgetPalette(
    val accent: ColorProvider,
    val accentSoft: ColorProvider,
)

private fun computePalette(track: Track?): WidgetPalette {
    val path = track?.album?.images?.firstOrNull()?.path ?: return WidgetPalette(
        accent = defaultAccent,
        accentSoft = ColorProvider(Color(0xFF2A1613)),
    )

    return try {
        val bitmap = BitmapFactory.decodeFile(path) ?: return WidgetPalette(
            accent = defaultAccent,
            accentSoft = ColorProvider(Color(0xFF2A1613)),
        )

        val small = android.graphics.Bitmap.createScaledBitmap(bitmap, 20, 20, true)
        var r = 0
        var g = 0
        var b = 0
        var count = 0

        for (x in 0 until small.width) {
            for (y in 0 until small.height) {
                val pixel = small.getPixel(x, y)
                val alpha = AndroidColor.alpha(pixel)
                if (alpha < 180) continue

                val hsv = FloatArray(3)
                AndroidColor.colorToHSV(pixel, hsv)
                if (hsv[1] < 0.18f || hsv[2] < 0.18f) continue

                r += AndroidColor.red(pixel)
                g += AndroidColor.green(pixel)
                b += AndroidColor.blue(pixel)
                count++
            }
        }

        if (count == 0) {
            return WidgetPalette(
                accent = defaultAccent,
                accentSoft = ColorProvider(Color(0xFF2A1613)),
            )
        }

        val rr = (r / count).coerceIn(70, 255)
        val gg = (g / count).coerceIn(55, 220)
        val bb = (b / count).coerceIn(55, 220)

        WidgetPalette(
            accent = ColorProvider(Color(AndroidColor.rgb(rr, gg, bb))),
            accentSoft = ColorProvider(
                Color(AndroidColor.argb(255, rr / 5, gg / 5, bb / 5))
            ),
        )
    } catch (_: Throwable) {
        WidgetPalette(
            accent = defaultAccent,
            accentSoft = ColorProvider(Color(0xFF2A1613)),
        )
    }
}

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
        val prefs = currentState.preferences
        val size = LocalSize.current
        val context = LocalContext.current

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
        val palette = computePalette(activeTrack)
        val counterText =
            if (queueLength > 0 && currentIndex > 0) "$currentIndex/$queueLength" else null

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(24.dp)
                .background(widgetSurface)
                .clickable(actionStartActivity<MainActivity>())
                .padding(12.dp)
        ) {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(24.dp)
                    .background(palette.accentSoft)
            ) {}

            Column(modifier = GlanceModifier.fillMaxSize()) {
                TrackDetailsView(
                    activeTrack = activeTrack,
                    compact = size.height <= 145.dp,
                    counterText = counterText,
                    accent = palette.accent
                )

                Spacer(modifier = GlanceModifier.height(10.dp))

                TrackProgress(
                    prefs = prefs,
                    accent = palette.accent,
                    inactive = widgetOutline
                )

                Spacer(modifier = GlanceModifier.height(10.dp))

                if (size.width > size.height) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Vertical.CenterVertically
                    ) {
                        Row {
                            WidgetPill(
                                label = "◀◀",
                                onClick = actionRunCallback<PreviousAction>(
                                    parameters = actionParametersOf(serverAddressKey to playbackServerAddress)
                                )
                            )
                            Spacer(modifier = GlanceModifier.width(8.dp))
                            WidgetPill(
                                label = if (isPlaying) "PAUSE" else "PLAY",
                                onClick = actionRunCallback<PlayPauseAction>(
                                    parameters = actionParametersOf(serverAddressKey to playbackServerAddress)
                                ),
                                filled = true,
                                accent = palette.accent,
                            )
                            Spacer(modifier = GlanceModifier.width(8.dp))
                            WidgetPill(
                                label = "▶▶",
                                onClick = actionRunCallback<NextAction>(
                                    parameters = actionParametersOf(serverAddressKey to playbackServerAddress)
                                )
                            )
                        }

                        Spacer(modifier = GlanceModifier.defaultWeight())

                        Row {
                            WidgetPill(
                                label = "SHUF",
                                onClick = actionRunCallback<ShuffleAction>(
                                    parameters = actionParametersOf(serverAddressKey to playbackServerAddress)
                                ),
                                active = isShuffled,
                                activeColor = palette.accentSoft,
                            )
                            Spacer(modifier = GlanceModifier.width(8.dp))
                            WidgetPill(
                                label = when (loopMode) {
                                    "single" -> "ONE"
                                    "loop" -> "ALL"
                                    else -> "REP"
                                },
                                onClick = actionRunCallback<RepeatAction>(
                                    parameters = actionParametersOf(serverAddressKey to playbackServerAddress)
                                ),
                                active = loopMode != "none",
                                activeColor = palette.accentSoft,
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Horizontal.CenterHorizontally
                    ) {
                        WidgetPill(
                            label = "◀◀",
                            onClick = actionRunCallback<PreviousAction>(
                                parameters = actionParametersOf(serverAddressKey to playbackServerAddress)
                            )
                        )
                        Spacer(modifier = GlanceModifier.width(8.dp))
                        WidgetPill(
                            label = if (isPlaying) "PAUSE" else "PLAY",
                            onClick = actionRunCallback<PlayPauseAction>(
                                parameters = actionParametersOf(serverAddressKey to playbackServerAddress)
                            ),
                            filled = true,
                            accent = palette.accent,
                        )
                        Spacer(modifier = GlanceModifier.width(8.dp))
                        WidgetPill(
                            label = "▶▶",
                            onClick = actionRunCallback<NextAction>(
                                parameters = actionParametersOf(serverAddressKey to playbackServerAddress)
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.height(8.dp))

                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Horizontal.CenterHorizontally
                    ) {
                        WidgetPill(
                            label = "SHUF",
                            onClick = actionRunCallback<ShuffleAction>(
                                parameters = actionParametersOf(serverAddressKey to playbackServerAddress)
                            ),
                            active = isShuffled,
                            activeColor = palette.accentSoft,
                        )
                        Spacer(modifier = GlanceModifier.width(8.dp))
                        WidgetPill(
                            label = when (loopMode) {
                                "single" -> "ONE"
                                "loop" -> "ALL"
                                else -> "REP"
                            },
                            onClick = actionRunCallback<RepeatAction>(
                                parameters = actionParametersOf(serverAddressKey to playbackServerAddress)
                            ),
                            active = loopMode != "none",
                            activeColor = palette.accentSoft,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetPill(
    label: String,
    onClick: Action,
    filled: Boolean = false,
    active: Boolean = false,
    accent: ColorProvider = defaultAccent,
    activeColor: ColorProvider = widgetSurface2,
) {
    val background = when {
        filled -> accent
        active -> activeColor
        else -> widgetSurface2
    }

    val textColor = if (filled) ColorProvider(Color(0xFF111111)) else widgetText

    Box(
        modifier = GlanceModifier
            .cornerRadius(18.dp)
            .background(background)
            .clickable(onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = TextStyle(
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
            ),
        )
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
