package com.github.trueddd.trueracing.command

import com.github.trueddd.trueracing.*
import org.bukkit.ChatColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class CommandHandler(
    private val finishLineRegistrar: FinishLineRegistrar,
    private val trackLightsRegistrar: TrackLightsRegistrar,
    private val pluginConfigManager: PluginConfigManager,
    private val pilotsManager: PilotsManager,
    private val raceManager: RaceManager,
    private val plugin: TrueRacing,
) {

    fun handle(commandSender: CommandSender, name: String, args: List<String>): Boolean {
        val (command, commandArgs) = Commands.parse(name, args) ?: return false
        return when (command) {
            Commands.Track.Finish.Create -> registerFinishLine(commandSender, commandArgs)
            Commands.Track.List -> listTracks(commandSender)
            Commands.Track.Create -> addTrack(commandSender, commandArgs)
            Commands.Track.Delete -> deleteTrack(commandSender, commandArgs)
            Commands.Race -> toggleRaceStatus(commandSender, commandArgs)
            Commands.Team.Create -> addTeam(commandSender, commandArgs)
            Commands.Team.Delete -> deleteTeam(commandSender, commandArgs)
            Commands.Team.List -> listTeams(commandSender)
            Commands.Track.Lights -> registerTrackLights(commandSender, commandArgs)
            else -> false
        }
    }

    private fun registerTrackLights(commandSender: CommandSender, commandArgs: List<String>): Boolean {
        if (commandSender !is Player) {
            commandSender.sendMessage("Only player can register track lights.")
            return true
        }
        val trackName = commandArgs.firstOrNull() ?: run {
            commandSender.sendMessage("Pass track name to register finish line.")
            return true
        }
        if (pluginConfigManager.getAllTracks().none { it.name == trackName }) {
            commandSender.sendMessage("Track not found.")
            return true
        }
        val wasMarking = trackLightsRegistrar.isPlayerMarking(commandSender)
        if (wasMarking) {
            val finishLine = trackLightsRegistrar.stopMarking(commandSender) ?: run {
                commandSender.sendMessage("Cannot retrieve lights location")
                return true
            }
            pluginConfigManager.setTrackLights(trackName, finishLine)
            commandSender.sendMessage("Lights for track \"$trackName\" saved.")
        } else {
            trackLightsRegistrar.startMarking(commandSender)
            commandSender.sendMessage("Right click on Redstone Lamps to regitster them.")
        }
        return true
    }

    private fun addTeam(commandSender: CommandSender, args: List<String>): Boolean {
        if (commandSender !is Player) {
            commandSender.sendMessage("You are not allowed to register team.")
            return true
        }
        val teamName = args.firstOrNull()
        if (teamName.isNullOrEmpty()) {
            commandSender.sendMessage("Team needs to be named.")
            return true
        }
        val colorCode = args.getOrNull(1)
        if (colorCode.isNullOrEmpty()) {
            commandSender.sendMessage("Specify color code for team.")
            return true
        }
        if (pilotsManager.getAllTeams().any { it.name == teamName }) {
            commandSender.sendMessage("Team with this name is already registered.")
            return true
        }
        pilotsManager.addTeam(commandSender, teamName, colorCode)
        commandSender.sendMessage("Team \"$teamName\" created")
        return true
    }

    private fun deleteTeam(commandSender: CommandSender, args: List<String>): Boolean {
        if (commandSender !is Player) {
            commandSender.sendMessage("You are not allowed to delete team.")
            return true
        }
        val teamName = args.firstOrNull()
        if (teamName.isNullOrEmpty()) {
            commandSender.sendMessage("Team needs to be named.")
            return true
        }
        val team = pilotsManager.getAllTeams().firstOrNull { it.name == teamName }
        when {
            team == null -> {
                commandSender.sendMessage("Team not found.")
            }
            team.headPlayerName != commandSender.name -> {
                commandSender.sendMessage("You are not allowed to delete team.")
            }
            else -> {
                pilotsManager.deleteTeam(teamName)
                commandSender.sendMessage("Team \"$teamName\" deleted")
            }
        }
        return true
    }

    private fun listTeams(commandSender: CommandSender): Boolean {
        val teams = pilotsManager.getAllTeams()
            .map {
                val color = ChatColor.getByChar(it.color) ?: ""
                "$color${it.name}"
            }
        if (teams.isEmpty()) {
            commandSender.sendMessage("No teams registered.")
        } else {
            commandSender.sendMessage("Registered teams: ${teams.joinToString("${ChatColor.WHITE}, ")}${ChatColor.WHITE}.")
        }
        return true
    }

    private fun addTrack(commandSender: CommandSender, args: List<String>): Boolean {
        if (!commandSender.isOp || commandSender !is Player) {
            commandSender.sendMessage("You are not allowed to register tracks.")
            return true
        }
        val trackName = args.getOrNull(0)
        if (trackName.isNullOrEmpty()) {
            commandSender.sendMessage("Track needs to be named.")
            return true
        }
        val lapsCount = args.getOrNull(1)?.toIntOrNull()
        if (lapsCount == null) {
            commandSender.sendMessage("Specify laps count for the track.")
            return true
        }
        pluginConfigManager.addTrack(trackName, lapsCount, commandSender.location)
        commandSender.sendMessage("Track \"$trackName\" created")
        return true
    }

    private fun deleteTrack(commandSender: CommandSender, args: List<String>): Boolean {
        if (!commandSender.isOp || commandSender !is Player) {
            commandSender.sendMessage("You are not allowed to delete tracks.")
            return true
        }
        val trackName = args.firstOrNull()
        if (trackName.isNullOrEmpty()) {
            commandSender.sendMessage("Track needs to be named.")
            return true
        }
        pluginConfigManager.removeTrack(trackName)
        commandSender.sendMessage("Track \"$trackName\" deleted")
        return true
    }

    private fun listTracks(commandSender: CommandSender): Boolean {
        val tracks = pluginConfigManager.getAllTracks().map { it.name }
        if (tracks.isEmpty()) {
            commandSender.sendMessage("No tracks registered.")
        } else {
            commandSender.sendMessage("Registered tracks: ${tracks.joinToString()}.")
        }
        return true
    }

    private fun registerFinishLine(commandSender: CommandSender, commandArgs: List<String>): Boolean {
        if (commandSender !is Player) {
            commandSender.sendMessage("Only player can register finish lines.")
            return true
        }
        val trackName = commandArgs.firstOrNull() ?: run {
            commandSender.sendMessage("Pass track name to register finish line.")
            return true
        }
        if (pluginConfigManager.getAllTracks().none { it.name == trackName }) {
            commandSender.sendMessage("Track not found.")
            return true
        }
        val wasMarking = finishLineRegistrar.isPlayerMarking(commandSender)
        if (wasMarking) {
            val finishLine = finishLineRegistrar.stopMarking(commandSender) ?: run {
                commandSender.sendMessage("Cannot retrieve finish line")
                return true
            }
            pluginConfigManager.setFinishLine(trackName, finishLine)
            commandSender.sendMessage("Finish line for track \"$trackName\" saved.")
        } else {
            finishLineRegistrar.startMarking(commandSender)
            commandSender.sendMessage("Take Wooden Sword and right click on blocks of finish line.")
        }
        return true
    }

    private fun toggleRaceStatus(commandSender: CommandSender, commandArgs: List<String>): Boolean {
        val trackName = commandArgs.firstOrNull()
        if (trackName.isNullOrEmpty()) {
            commandSender.sendMessage("Specify track for the race.")
            return true
        }
        if (raceManager.isRaceRunning(trackName)) {
            raceManager.stopRace(commandSender, trackName)
        } else {
            raceManager.startRace(commandSender, trackName)
        }
        return true
    }
}