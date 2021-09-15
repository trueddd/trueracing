package com.github.trueddd.trueracing.command

import com.github.trueddd.trueracing.*
import com.github.trueddd.trueracing.data.FinishLine
import com.github.trueddd.trueracing.data.PluginConfig
import com.github.trueddd.trueracing.data.Track
import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import org.bukkit.Location
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.io.File
import java.io.FileReader
import java.io.FileWriter

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
            Commands.Race -> toggleRaceStatus()
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
        val wasMarking = finishLineRegistrar.isNowMarking
        finishLineRegistrar.isNowMarking = !finishLineRegistrar.isNowMarking
        if (wasMarking) {
            finishLineRegistrar.finishLineCorners?.let { (first, second) ->
                val finishLine = FinishLine(first.toSimpleLocation(), second.toSimpleLocation())
                // todo
            }
        }
        return true
    }

    private var lineCrossJob: Job? = null

    private fun toggleRaceStatus(): Boolean {
        if (lineCrossJob != null) {
            lineCrossJob?.cancel()
            lineCrossJob = null
            return true
        }
        val line = finishLineRegistrar.finishLine
        if (line.isEmpty()) {
            return true
        }
        val timings = mutableListOf<Long>()
        lineCrossJob = finishLineListener.playerPosition
            .onStart { println("race started") }
            .filterIfCrossed(line)
            .flowOn(Dispatchers.Default)
            .onEach {
                val time = System.currentTimeMillis()
                if (timings.isEmpty()) {
                    consoleLog("started first lap")
                } else {
                    val lapTime = time - timings.last()
                    val lapTiming = lapTime.toTiming()
                    val bestLapSoFar = timings.windowed(2, 1)
                        .map { range -> range.last() - range.first() }
                        .minOrNull()
                    val formatted = if (bestLapSoFar == null || bestLapSoFar > lapTime) {
                        lapTiming.formatPurple()
                    } else {
                        lapTiming.formatRed()
                    }
                    consoleLog("Lap time: $formatted")
                }
                timings.add(time)
            }
            .onCompletion { println("stop") }
            .launchIn(pluginScope)
        return true
    }
}