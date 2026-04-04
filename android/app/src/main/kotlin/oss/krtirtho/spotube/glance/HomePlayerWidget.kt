package oss.krtirtho.spotube.glance

import HomeWidgetGlanceState
import HomeWidgetGlanceStateDefinition
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
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

private val widgetSurface = ColorProvider(Color(0xFF0B0B0F))
private val widgetSurface2 = ColorProvider(Color(0xFF17171C))
private val widgetAccent = ColorProvider(Color(0xFFFF3B30))
private val widgetText = ColorProvider(Color(0xFFFFFFFF))
private val widgetSubText = ColorProvider(Color(0xFFB7B7BE))
private val widgetOutline = ColorProvider(Color(0xFF2A2A31))

class Breakpoints {
    companion object {
        val SMALL_SQUARE = DpSize(110.dp, 110.dp)
        val HORIZONTAL_RECTANGLE = DpSize(280.dp, 140.dp)
        val BIG_SQUARE = DpSize(280.dp, 280.dp)
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
            GlanceContent(context, currentState())
        }
    }

    @OptIn(ExperimentalGlancePreviewApi::class)
    @Preview(widthDp = 280, heightDp = 140)
    @Composable
    private fun GlanceContent(context: Context, currentState: HomeWidgetGlanceState) {
        val prefs = currentState.preferences
        val size = LocalSize.current

        val activeTrackStr = prefs.getString("activeTrack", null)
        val isPlaying = prefs.getBoolean("isPlaying", false)
        val isShuffled = prefs.getBoolean("isShuffled", false)
        val loopMode = prefs.getString("loopMode", "none") ?: "none"
        val playbackServerAddress = prefs.getString("playbackServerAddress", null) ?: ""

        var activeTrack: Track? = null
        if (activeTrackStr != null) {
            activeTrack = gson.fromJson(activeTrackStr, Track::class.java)
        }

        val openAppAction = actionStartActivity<MainActivity>()

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(widgetSurface)
                .clickable(onClick = openAppAction)
                .padding(12.dp)
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
            ) {
                if (size.width < 140.dp) {
                    TrackDetailsView(activeTrack, compact = true)
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    TrackProgress(prefs)
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Horizontal.CenterHorizontally
                    ) {
                        WidgetChipButton(
                            label = if (isPlaying) "PAUSE" else "PLAY",
                            filled = true,
                            onClick = actionRunCallback<PlayPauseAction>(
                                parameters = actionParametersOf(serverAddressKey to playbackServerAddress)
                            )
                        )
                    }
                } else if (size.width > size.height) {
                    TrackDetailsView(activeTrack, compact = false)
                    Spacer(modifier = GlanceModifier.height(10.dp))
                    TrackProgress(prefs)
                    Spacer(modifier = GlanceModifier.height(10.dp))
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Horizontal.SpaceBetween
                    ) {
                        Row {
                            WidgetChipButton(
                                label = "◀◀",
                                onClick = actionRunCallback<PreviousAction>(
                                    parameters = actionParametersOf(serverAddressKey to playbackServerAddress)
                                )
                            )
                            Spacer(modifier = GlanceModifier.width(8.dp))
                            WidgetChipButton(
                                label = if (isPlaying) "PAUSE" else "PLAY",
                                filled = true,
                                onClick = actionRunCallback<PlayPauseAction>(
                                    parameters = actionParametersOf(serverAddressKey to playbackServerAddress)
                                )
                            )
                            Spacer(modifier = GlanceModifier.width(8.dp))
                            WidgetChipButton(
                                label = "▶▶",
                                onClick = actionRunCallback<NextAction>(
                                    parameters = actionParametersOf(serverAddressKey to playbackServerAddress)
                                )
                            )
                        }
                        Row {
                            WidgetChipButton(
                                label = if (isShuffled) "SHUF" else "SHUF",
                                active = isShuffled,
                                onClick = actionRunCallback<ShuffleAction>(
                                    parameters = actionParametersOf(serverAddressKey to playbackServerAddress)
                                )
                            )
                            Spacer(modifier = GlanceModifier.width(8.dp))
                            WidgetChipButton(
                                label = when (loopMode) {
                                    "single" -> "ONE"
                                    "loop" -> "ALL"
                                    else -> "REP"
                                },
                                active = loopMode != "none",
                                onClick = actionRunCallback<RepeatAction>(
                                    parameters = actionParametersOf(serverAddressKey to playbackServerAddress)
                                )
                            )
                        }
                    }
                } else {
                    TrackDetailsView(activeTrack, compact = false)
                    Spacer(modifier = GlanceModifier.height(12.dp))
                    TrackProgress(prefs)
                    Spacer(modifier = GlanceModifier.height(12.dp))
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Horizontal.CenterHorizontally
                    ) {
                        WidgetChipButton(
                            label = "◀◀",
                            onClick = actionRunCallback<PreviousAction>(
                                parameters = actionParametersOf(serverAddressKey to playbackServerAddress)
                            )
                        )
                        Spacer(modifier = GlanceModifier.width(8.dp))
                        WidgetChipButton(
                            label = if (isPlaying) "PAUSE" else "PLAY",
                            filled = true,
                            onClick = actionRunCallback<PlayPauseAction>(
                                parameters = actionParametersOf(serverAddressKey to playbackServerAddress)
                            )
                        )
                        Spacer(modifier = GlanceModifier.width(8.dp))
                        WidgetChipButton(
                            label = "▶▶",
                            onClick = actionRunCallback<NextAction>(
                                parameters = actionParametersOf(serverAddressKey to playbackServerAddress)
                            )
                        )
                    }
                    Spacer(modifier = GlanceModifier.height(10.dp))
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Horizontal.CenterHorizontally
                    ) {
                        WidgetChipButton(
                            label = "SHUFFLE",
                            active = isShuffled,
                            onClick = actionRunCallback<ShuffleAction>(
                                parameters = actionParametersOf(serverAddressKey to playbackServerAddress)
                            )
                        )
                        Spacer(modifier = GlanceModifier.width(8.dp))
                        WidgetChipButton(
                            label = when (loopMode) {
                                "single" -> "REPEAT 1"
                                "loop" -> "REPEAT"
                                else -> "REPEAT"
                            },
                            active = loopMode != "none",
                            onClick = actionRunCallback<RepeatAction>(
                                parameters = actionParametersOf(serverAddressKey to playbackServerAddress)
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetChipButton(
    label: String,
    onClick: Action,
    filled: Boolean = false,
    active: Boolean = false,
) {
    val background = when {
        filled -> widgetAccent
        active -> widgetSurface2
        else -> widgetSurface2
    }

    val textColor = if (filled) ColorProvider(Color(0xFF111111)) else widgetText

    Box(
        modifier = GlanceModifier
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
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
