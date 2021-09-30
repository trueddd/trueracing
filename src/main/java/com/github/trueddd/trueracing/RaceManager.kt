package com.github.trueddd.trueracing

import com.github.trueddd.trueracing.data.Location
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.block.Block
import org.bukkit.block.data.Lightable
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent

class RaceManager(
    private val plugin: TrueRacing,
    private val pluginConfigManager: PluginConfigManager,
    private val scoreboardManager: ScoreboardManager,
    private val pilotsManager: PilotsManager,
) {

    private val raceListeners = mutableMapOf<String, Job>()

    fun isRaceRunning(trackName: String): Boolean {
        return raceListeners[trackName]?.isActive == true
    }

    fun stopRace(commandSender: CommandSender, trackName: String) {
        raceListeners[trackName]
            ?.let {
                it.cancel()
                commandSender.sendMessage("Race is now finished.")
            }
            ?: run {
                commandSender.sendMessage("Race hasn\'t been started.")
            }
    }

    fun startRace(commandSender: CommandSender, trackName: String) {
        if (commandSender !is Player) {
            commandSender.sendMessage("You are not allowed to start race.")
            return
        }
        val track = pluginConfigManager.getAllTracks().firstOrNull { it.name == trackName } ?: run {
            commandSender.sendMessage("Track not found.")
            return
        }
        val line = track.finishLine ?: run {
            commandSender.sendMessage("Finish line is not specified for this track.")
            return
        }
        val timings = mutableMapOf<String, List<Long>>()
        val lapTimes = mutableMapOf<String, List<Long>>()
        val registeredPilots = pilotsManager.getAllPilots()
        val pilots = commandSender.world.players.filter { pilot ->
            registeredPilots.any { it.playerName == pilot.name }
        }
        val pilotNames = pilots.map { it.name }
        val finishLineListener = FinishLineListener(pilotNames)
        raceListeners[trackName] = finishLineListener.playerPositionFlow
            .onStart {
                plugin.server.pluginManager.registerEvents(scoreboardManager, plugin)
                plugin.server.pluginManager.registerEvents(finishLineListener, plugin)
                val startTime = startRaceTimer(track.lights)
                pilots.forEach {
                    timings[it.name] = listOf(startTime)
                    if (track.lights.isNullOrEmpty()) {
                        it.sendActionBar(Component.text("Start!"))
                    }
                }
            }
            .flowOn(plugin.coroutineContext)
            .filterIfCrossed(line, pilotNames)
            .flowOn(Dispatchers.Default)
            .onEach { player ->
                val time = System.currentTimeMillis()
                val lapTime = time - timings[player.name]!!.last()
                val lapTimesForRacer = lapTimes[player.name]
                val bestPersonal = lapTime < (lapTimesForRacer?.minOrNull() ?: Long.MAX_VALUE)
                val bestLap = lapTime < (lapTimes.values.flatten().minOrNull() ?: Long.MAX_VALUE)
                lapTimes[player.name] = (lapTimesForRacer ?: emptyList()) + lapTime
                scoreboardManager.updateLaps(player, lapTimes[player.name]!!)
                timings[player.name] = (timings[player.name] ?: emptyList()) + time
            }
            .takeWhile { lapTimes.values.any { it.size < track.lapCount } }
            .onCompletion {
                raceListeners.remove(trackName)
                scoreboardManager.clearAllBoards()
                PlayerQuitEvent.getHandlerList().unregister(scoreboardManager)
                PlayerMoveEvent.getHandlerList().unregister(finishLineListener)
            }
            .launchIn(plugin)
    }

    private suspend fun startRaceTimer(lights: List<Location>?): Long {
        if (lights == null) {
            return System.currentTimeMillis()
        }
        val lamps = lights.mapNotNull { Bukkit.getWorld(it.world)?.getBlockAt(it.x, it.y, it.z) }
        lamps.forEach {
            it.toggleLampLit(false)
        }
        lamps.forEach {
            delay(800L)
            it.toggleLampLit(true)
        }
        delay((700..1200).random().toLong())
        lamps.forEach {
            it.toggleLampLit(false)
        }
        return System.currentTimeMillis()
    }

    private fun Block.toggleLampLit(isLit: Boolean) {
        val data = blockData as? Lightable ?: return
        blockData = data.apply { this.isLit = isLit }
    }
}