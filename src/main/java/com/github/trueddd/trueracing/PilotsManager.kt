package com.github.trueddd.trueracing

import com.github.trueddd.trueracing.data.Pilot
import com.github.trueddd.trueracing.data.PilotsConfig
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
        return object : TypeToken<PilotsConfig>() {}.type
    }

    private fun updateTeams(shouldSave: Boolean = true, block: (List<RacingTeam>) -> List<RacingTeam>) {
        config.value = config.value.copy(
            teams = config.value.teams.let(block)
        )
        if (shouldSave) {
            updateConfig()
        }
    }

    private fun updatePilots(shouldSave: Boolean = true, block: (List<Pilot>) -> List<Pilot>) {
        config.value = config.value.copy(
            pilots = config.value.pilots.let(block)
        )
        if (shouldSave) {
            updateConfig()
        }
    }

    fun getAllTeams(): List<RacingTeam> {
        return config.value.teams
    }

    fun getAllPilots(): List<Pilot> {
        return config.value.pilots
    }

    fun addPilot(player: Player, teamName: String) {
        updatePilots {
            it + Pilot(player.name, teamName)
        }
    }

    fun addTeam(player: Player, teamName: String, colorCode: String) {
        updateTeams(shouldSave = false) {
            it + RacingTeam(teamName, player.name, colorCode)
        }
        addPilot(player, teamName)
    }

    fun deleteTeam(teamName: String) {
        updateTeams(shouldSave = false) { teams ->
            teams.filterNot { it.name == teamName }
        }
        updatePilots { pilots ->
            pilots.map {
                if (it.teamName == teamName) {
                    it.copy(teamName = null)
                } else {
                    it
                }
            }
        }
    }
}