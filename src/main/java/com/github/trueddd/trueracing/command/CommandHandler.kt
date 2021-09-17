package com.github.trueddd.trueracing.command

import com.github.trueddd.trueracing.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerQuitEvent
import kotlin.math.abs

class CommandHandler(
    private val plugin: TrueRacing,
    private val finishLineRegistrar: FinishLineRegistrar,
    private val finishLineListener: FinishLineListener,
    private val pluginConfigManager: PluginConfigManager,
    private val pilotsManager: PilotsManager,
) {

    private val pluginScope: CoroutineScope
        get() = plugin

    private val scoreboardManager by lazy { ScoreboardManager() }

    fun handle(commandSender: CommandSender, name: String, args: List<String>): Boolean {
        val (command, commandArgs) = Commands.parse(name, args) ?: return false
        return when (command) {
            Commands.Track.Finish.Create -> registerFinishLine(commandSender, commandArgs)
            Commands.Track.List -> listTracks(commandSender)
            Commands.Track.Create -> addTrack(commandSender, commandArgs)
            Commands.Track.Delete -> deleteTrack(commandSender, commandArgs)
            Commands.Race -> toggleRaceStatus(commandSender, commandArgs)
            Commands.Team.Create -> addTeam(commandSender, commandArgs)
            Commands.Team.List -> listTeams(commandSender)
            else -> false
        }
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

    private fun listTeams(commandSender: CommandSender): Boolean {
        val teams = pilotsManager.getAllTeams().map { it.name }
        if (teams.isEmpty()) {
            commandSender.sendMessage("No teams registered.")
        } else {
            commandSender.sendMessage("Registered teams: ${teams.joinToString()}.")
        }
        return true
    }

    private fun addTrack(commandSender: CommandSender, args: List<String>): Boolean {
        if (!commandSender.isOp || commandSender !is Player) {
            commandSender.sendMessage("You are not allowed to register tracks.")
            return true
        }
        val trackName = args.firstOrNull()
        if (trackName.isNullOrEmpty()) {
            commandSender.sendMessage("Track needs to be named.")
            return true
        }
        pluginConfigManager.addTrack(trackName, commandSender.location)
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

    private var lineCrossJob: Job? = null

    private fun toggleRaceStatus(commandSender: CommandSender, commandArgs: List<String>): Boolean {
        if (commandArgs.isEmpty()) {
            commandSender.sendMessage("Specify track for the race.")
            return true
        }
        if (lineCrossJob != null) {
            lineCrossJob?.cancel()
            lineCrossJob = null
            return true
        }
        val trackName = commandArgs.firstOrNull() ?: run {
            commandSender.sendMessage("Specify track for the race.")
            return true
        }
        val line = pluginConfigManager.getAllTracks().firstOrNull { it.name == trackName }?.finishLine ?: run {
            commandSender.sendMessage("Finish line is not specified for this track.")
            return true
        }
        val timings = mutableListOf<Long>()
        val laps = mutableListOf<Long>()
        lineCrossJob = finishLineListener.playerPosition
            .onStart {
                plugin.server.pluginManager.registerEvents(scoreboardManager, plugin)
            }
            .filterIfCrossed(line)
            .flowOn(Dispatchers.Default)
            .onEach {
                val time = System.currentTimeMillis()
                if (timings.isEmpty()) {
                    consoleLog("started first lap")
                } else {
                    val lapTime = time - timings.last()
                    val isBestSoFar = lapTime < (laps.minOrNull() ?: Long.MAX_VALUE)
                    val diff = laps.minOrNull()?.minus(lapTime)
                        ?.let { " ${if (isBestSoFar) "-" else "+"}${abs(it).toTiming()}" }
                        ?: ""
                    val actionBarColor = if (isBestSoFar) TextColor.color(0, 180, 0) else TextColor.color(160, 0, 0)
                    // fixme: player ref
                    plugin.server.onlinePlayers.first()
                        .sendActionBar(
                            Component.text(
                                "${lapTime.toTiming()}$diff",
                                actionBarColor
                            )
                        )
                    laps.add(lapTime)
                    scoreboardManager.updateLaps(plugin.server.onlinePlayers.first(), laps)
                }
                timings.add(time)
            }
            .onCompletion {
                scoreboardManager.clearAllBoards()
                PlayerQuitEvent.getHandlerList().unregister(scoreboardManager)
            }
            .launchIn(pluginScope)
        return true
    }
}