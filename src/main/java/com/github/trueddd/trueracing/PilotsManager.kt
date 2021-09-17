package com.github.trueddd.trueracing

import com.github.trueddd.trueracing.data.PilotsConfig
import com.github.trueddd.trueracing.data.PluginConfig
import com.github.trueddd.trueracing.data.RacingTeam
import com.google.gson.reflect.TypeToken
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.lang.reflect.Type

class PilotsManager(plugin: Plugin) : BaseConfigManager<PilotsConfig>(plugin) {

    override val fileName: String
        get() = "pilots.json"

    override fun getDefaultConfig() = PilotsConfig.default()

    override fun getConfigType(): Type {
        return object : TypeToken<PluginConfig>() {}.type
    }

    private fun updateTeams(block: (List<RacingTeam>) -> List<RacingTeam>) {
        config.value = config.value.copy(
            teams = config.value.teams.let(block)
        )
    }

    fun getAllTeams(): List<RacingTeam> {
        return config.value.teams
    }

    fun addTeam(player: Player, teamName: String, colorCode: String) {
        updateTeams {
            it + RacingTeam(teamName, player.name, colorCode)
        }
    }
}