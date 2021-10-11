package com.github.trueddd.trueracing.command

import com.github.trueddd.trueracing.*
import net.kyori.adventure.text.Component
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BookMeta

class CommandHandler(
    private val finishLineRegistrar: FinishLineRegistrar,
    private val trackLightsRegistrar: TrackLightsRegistrar,
    private val pluginConfigManager: PluginConfigManager,
    private val pilotsManager: PilotsManager,
    private val raceManager: RaceManager,
) {

    fun handle(commandSender: CommandSender, name: String, args: List<String>): Boolean {
        val (command, commandArgs) = Commands.parse(name, args) ?: return false
        when (command) {
            Commands.Track.Finish.Create -> registerFinishLine(commandSender, commandArgs)
            Commands.Track.List -> listTracks(commandSender)
            Commands.Track.Create -> addTrack(commandSender, commandArgs)
            Commands.Track.Delete -> deleteTrack(commandSender, commandArgs)
            Commands.Race.Start -> startRace(commandSender, commandArgs)
            Commands.Race.Stop -> stopRace(commandSender, commandArgs)
            Commands.Race.Register -> registerRace(commandSender, commandArgs)
            Commands.Team.Create -> addTeam(commandSender, commandArgs)
            Commands.Team.Delete -> deleteTeam(commandSender, commandArgs)
            Commands.Team.List -> listTeams(commandSender)
            Commands.Track.Lights -> registerTrackLights(commandSender, commandArgs)
            Commands.Test -> test(commandSender, commandArgs)
        }
        return true
    }

    private fun test(commandSender: CommandSender, commandArgs: List<String>) {
        if (commandSender !is Player) {
            return
        }
        commandSender.inventory.addItem(
            ItemStack(Material.WRITTEN_BOOK).apply {
                val builder = (itemMeta as? BookMeta)?.toBuilder() ?: return@apply
                val meta = builder
                    .author(Component.text("Race Plugin"))
                    .title(Component.text("Title"))
                    .addPage(Component.text("${ChatColor.GREEN}First row\n${ChatColor.AQUA}Second row"))
                    .build()
                itemMeta = meta
            }
        )
    }

    private fun registerTrackLights(commandSender: CommandSender, commandArgs: List<String>) {
        if (commandSender !is Player) {
            commandSender.sendMessage("Only player can register track lights.")
            return
        }
        val trackName = commandArgs.firstOrNull() ?: run {
            commandSender.sendMessage("Pass track name to register finish line.")
            return
        }
        if (pluginConfigManager.getAllTracks().none { it.name == trackName }) {
            commandSender.sendMessage("Track not found.")
            return
        }
        val wasMarking = trackLightsRegistrar.isPlayerMarking(commandSender)
        if (wasMarking) {
            val finishLine = trackLightsRegistrar.stopMarking(commandSender) ?: run {
                commandSender.sendMessage("Cannot retrieve lights location")
                return
            }
            pluginConfigManager.setTrackLights(trackName, finishLine)
            commandSender.sendMessage("Lights for track \"$trackName\" saved.")
        } else {
            trackLightsRegistrar.startMarking(commandSender)
            commandSender.sendMessage("Right click on Redstone Lamps to regitster them.")
        }
    }

    private fun addTeam(commandSender: CommandSender, args: List<String>) {
        if (commandSender !is Player) {
            commandSender.sendMessage("You are not allowed to register team.")
            return
        }
        val teamName = args.firstOrNull()
        if (teamName.isNullOrEmpty()) {
            commandSender.sendMessage("Team needs to be named.")
            return
        }
        val colorCode = args.getOrNull(1)
        if (colorCode.isNullOrEmpty()) {
            commandSender.sendMessage("Specify color code for team.")
            return
        }
        if (pilotsManager.getAllTeams().any { it.name == teamName }) {
            commandSender.sendMessage("Team with this name is already registered.")
            return
        }
        pilotsManager.addTeam(commandSender, teamName, colorCode)
        commandSender.sendMessage("Team \"$teamName\" created")
    }

    private fun deleteTeam(commandSender: CommandSender, args: List<String>) {
        if (commandSender !is Player) {
            commandSender.sendMessage("You are not allowed to delete team.")
            return
        }
        val teamName = args.firstOrNull()
        if (teamName.isNullOrEmpty()) {
            commandSender.sendMessage("Team needs to be named.")
            return
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
    }

    private fun listTeams(commandSender: CommandSender) {
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
    }

    private fun addTrack(commandSender: CommandSender, args: List<String>) {
        if (!commandSender.isOp || commandSender !is Player) {
            commandSender.sendMessage("You are not allowed to register tracks.")
            return
        }
        val trackName = args.getOrNull(0)
        if (trackName.isNullOrEmpty()) {
            commandSender.sendMessage("Track needs to be named.")
            return
        }
        val lapsCount = args.getOrNull(1)?.toIntOrNull()
        if (lapsCount == null) {
            commandSender.sendMessage("Specify laps count for the track.")
            return
        }
        pluginConfigManager.addTrack(trackName, lapsCount, commandSender.location)
        commandSender.sendMessage("Track \"$trackName\" created")
    }

    private fun deleteTrack(commandSender: CommandSender, args: List<String>) {
        if (!commandSender.isOp || commandSender !is Player) {
            commandSender.sendMessage("You are not allowed to delete tracks.")
            return
        }
        val trackName = args.firstOrNull()
        if (trackName.isNullOrEmpty()) {
            commandSender.sendMessage("Track needs to be named.")
            return
        }
        pluginConfigManager.removeTrack(trackName)
        commandSender.sendMessage("Track \"$trackName\" deleted")
    }

    private fun listTracks(commandSender: CommandSender) {
        val tracks = pluginConfigManager.getAllTracks().map { it.name }
        if (tracks.isEmpty()) {
            commandSender.sendMessage("No tracks registered.")
        } else {
            commandSender.sendMessage("Registered tracks: ${tracks.joinToString()}.")
        }
    }

    private fun registerFinishLine(commandSender: CommandSender, commandArgs: List<String>) {
        if (commandSender !is Player) {
            commandSender.sendMessage("Only player can register finish lines.")
            return
        }
        val trackName = commandArgs.firstOrNull() ?: run {
            commandSender.sendMessage("Pass track name to register finish line.")
            return
        }
        if (pluginConfigManager.getAllTracks().none { it.name == trackName }) {
            commandSender.sendMessage("Track not found.")
            return
        }
        val wasMarking = finishLineRegistrar.isPlayerMarking(commandSender)
        if (wasMarking) {
            val finishLine = finishLineRegistrar.stopMarking(commandSender) ?: run {
                commandSender.sendMessage("Cannot retrieve finish line")
                return
            }
            pluginConfigManager.setFinishLine(trackName, finishLine)
            commandSender.sendMessage("Finish line for track \"$trackName\" saved.")
        } else {
            finishLineRegistrar.startMarking(commandSender)
            commandSender.sendMessage("Take Wooden Sword and right click on blocks of finish line.")
        }
    }

    private fun startRace(commandSender: CommandSender, commandArgs: List<String>) {
        val trackName = commandArgs.firstOrNull()
        if (trackName.isNullOrEmpty()) {
            commandSender.sendMessage("Specify track for the race.")
            return
        }
        if (raceManager.isRaceRunning(trackName)) {
            commandSender.sendMessage("Race on this track is already started.")
        } else {
            raceManager.startRace(commandSender, trackName)
        }
    }

    private fun stopRace(commandSender: CommandSender, commandArgs: List<String>) {
        val trackName = commandArgs.firstOrNull()
        if (trackName.isNullOrEmpty()) {
            commandSender.sendMessage("Specify track for the race.")
            return
        }
        if (raceManager.isRaceRunning(trackName)) {
            raceManager.stopRace(commandSender, trackName)
        } else {
            commandSender.sendMessage("Race is not started.")
        }
    }

    private fun registerRace(commandSender: CommandSender, commandArgs: List<String>) {
        val trackName = commandArgs.firstOrNull()
        if (trackName.isNullOrEmpty()) {
            commandSender.sendMessage("Specify track for the race.")
            return
        }
        val pilots = commandArgs.drop(1)
        if (pilots.isEmpty()) {
            commandSender.sendMessage("Register pilots for the race.")
            return
        }
        if (raceManager.isRaceRunning(trackName)) {
            commandSender.sendMessage("Race on this track is already started.")
        } else {
            raceManager.registerRace(commandSender, trackName, pilots)
        }
    }
}