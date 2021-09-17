package com.github.trueddd.trueracing

import com.github.trueddd.trueracing.data.FinishLineRectangle
import com.github.trueddd.trueracing.data.PluginConfig
import com.github.trueddd.trueracing.data.Track
import com.google.gson.reflect.TypeToken
import org.bukkit.Location
import org.bukkit.plugin.Plugin
import java.lang.reflect.Type

class PluginConfigManager(
    plugin: Plugin,
) : BaseConfigManager<PluginConfig>(plugin) {

    override val fileName: String
        get() = "config.json"

    override fun getDefaultConfig() = PluginConfig.default()

    override fun getConfigType(): Type {
        return object : TypeToken<PluginConfig>() {}.type
    }

    private fun updateTracks(block: (List<Track>) -> List<Track>) {
        config.value = config.value.copy(
            tracks = config.value.tracks.let(block)
        )
        updateConfig()
    }

    fun getAllTracks(): List<Track> {
        return config.value.tracks
    }

    fun addTrack(trackName: String, location: Location) {
        updateTracks {
            it + Track(trackName, location.toSimpleLocation(), null)
        }
    }

    fun removeTrack(trackName: String) {
        updateTracks { tracks ->
            tracks.filterNot { it.name == trackName }
        }
    }

    fun setFinishLine(trackName: String, finishLine: FinishLineRectangle?) {
        updateTracks { tracks ->
            tracks.map { track ->
                if (track.name != trackName) {
                    track
                } else {
                    Track(trackName, track.location, finishLine)
                }
            }
        }
    }
}