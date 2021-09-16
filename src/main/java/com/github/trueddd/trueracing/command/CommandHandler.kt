package com.github.trueddd.trueracing.command

import com.github.trueddd.trueracing.*
import com.github.trueddd.trueracing.data.PluginConfig
import com.github.trueddd.trueracing.data.Track
import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Location
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerQuitEvent
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import kotlin.math.abs

class CommandHandler(
    private val plugin: TrueRacing,
    private val finishLineRegistrar: FinishLineRegistrar,
    private val finishLineListener: FinishLineListener,
) {

    private val pluginScope: CoroutineScope
        get() = plugin

    private val gson = Gson()

    private val configFile by lazy { File(plugin.dataFolder, "config.json") }

    private val config = MutableStateFlow(PluginConfig.default())

    private val scoreboardManager by lazy { ScoreboardManager() }

    init {
        if (!configFile.exists()) {
            configFile.parentFile.mkdir()
            configFile.createNewFile()
        }
        config.value = gson.fromJson(JsonReader(FileReader(configFile)), PluginConfig::class.java) ?: PluginConfig.default()
    }

    private fun updateConfig() {
        val writer = FileWriter(configFile, false)
        gson.toJson(config.value, PluginConfig::class.java, writer)
        writer.close()
    }

    private fun getAllTracks(): List<String> {
        return config.value.tracks.map { it.name }
    }

    private fun addTrackToConfig(trackName: String, location: Location) {
        config.value = config.value.copy(
            tracks = config.value.tracks + Track(trackName, location.toSimpleLocation(), null)
        )
        updateConfig()
    }

    fun handle(commandSender: CommandSender, name: String, args: List<String>): Boolean {
        val (command, commandArgs) = Commands.parse(name, args) ?: return false
        return when (command) {
            Commands.Track.Finish.Create -> registerFinishLine(commandSender, commandArgs)
            Commands.Track.List -> listTracks(commandSender)
            Commands.Track.Create -> addTrack(commandSender, commandArgs)
            Commands.Race -> toggleRaceStatus(commandSender, commandArgs)
            else -> false
        }
    }

    private fun addTrack(commandSender: CommandSender, args: List<String>): Boolean {
        if (!commandSender.isOp) {
            commandSender.sendMessage("You are not allowed to register tracks.")
            return true
        }
        if (args.isEmpty() || args.firstOrNull().isNullOrEmpty()) {
            commandSender.sendMessage("Track needs to be named.")
            return true
        }
        if (commandSender !is Player) {
            commandSender.sendMessage("Only players can create tracks.")
            return true
        }
        addTrackToConfig(args.first(), commandSender.location)
        commandSender.sendMessage("Track \"${args.first()}\" created")
        return true
    }

    private fun listTracks(commandSender: CommandSender): Boolean {
        val tracks = getAllTracks()
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
        if (config.value.tracks.none { it.name == trackName }) {
            commandSender.sendMessage("Track not found.")
            return true
        }
        val wasMarking = finishLineRegistrar.isPlayerMarking(commandSender)
        if (wasMarking) {
            val finishLine = finishLineRegistrar.stopMarking(commandSender) ?: run {
                commandSender.sendMessage("Cannot retrieve finish line")
                return true
            }
            config.value = config.value.copy(
                tracks = config.value.tracks.map { track ->
                    if (track.name != trackName) {
                        track
                    } else {
                        Track(trackName, track.location, finishLine)
                    }
                }
            )
            updateConfig()
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
        println(commandArgs)
        val trackName = commandArgs.firstOrNull() ?: run {
            commandSender.sendMessage("Specify track for the race.")
            return true
        }
        val line = config.value.tracks.firstOrNull { it.name == trackName }?.finishLine ?: run {
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