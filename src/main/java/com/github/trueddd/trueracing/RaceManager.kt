package com.github.trueddd.trueracing

import com.github.trueddd.trueracing.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.data.Lightable
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BookMeta

class RaceManager(
    private val plugin: TrueRacing,
    private val pluginConfigManager: PluginConfigManager,
    private val scoreboardManager: ScoreboardManager,
    private val pilotsManager: PilotsManager,
) {

    private val raceListeners = mutableMapOf<String, Pair<Race, Job?>>()

    fun isRaceRunning(trackName: String): Boolean {
        return raceListeners[trackName]?.second?.isActive == true
    }

    fun registerRace(commandSender: CommandSender, trackName: String, pilotNames: List<String>) {
        if (isRaceRunning(trackName)) {
            commandSender.sendMessage("Race on this track is now running.")
            return
        }
        val pilots = pilotsManager.getAllPilots().filter { it.playerName in pilotNames }
        val race = Race(
            pilots,
            RaceStatus.REGISTERED,
            emptyMap(),
        )
        raceListeners[trackName] = race to null
        commandSender.sendMessage("Race registered.")
    }

    fun stopRace(commandSender: CommandSender, trackName: String) {
        raceListeners[trackName]
            ?.let { (_, job) ->
                job?.cancel()
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
        if (raceListeners[trackName] == null) {
            commandSender.sendMessage("Race is not registered.")
            return
        }
        val registeredPilots = raceListeners[trackName]?.first?.pilots
        if (registeredPilots.isNullOrEmpty()) {
            commandSender.sendMessage("Not enough pilots to start the race.")
            return
        }
        val pilots = commandSender.world.players.filter { pilot ->
            registeredPilots.any { it.playerName == pilot.name }
        }
        val pilotNames = pilots.map { it.name }
        val timings = mutableMapOf<String, List<Long>>()
        val lapTimes = mutableMapOf<String, List<Long>>().apply {
            pilotNames.forEach { put(it, emptyList()) }
        }
        raceListeners[trackName] = plugin.playerMoves(pilotNames)
            .onStart {
                plugin.server.pluginManager.registerEvents(scoreboardManager, plugin)
                val startTime = startRaceTimer(track.lights)
                pilots.forEach {
                    timings[it.name] = listOf(startTime)
                    if (track.lights.isNullOrEmpty()) {
                        it.sendActionBar(Component.text("Start!"))
                    }
                }
                raceListeners[trackName]?.let { (race, job) ->
                    raceListeners[trackName] = race.copy(status = RaceStatus.STARTED) to job
                }
            }
            .flowOn(plugin.coroutineContext)
            .filterIfCrossed(line)
            .flowOn(Dispatchers.Default)
            .filter { player -> lapTimes[player.name]?.let { it.size < track.lapCount } ?: false }
            .onEach { player ->
                val time = System.currentTimeMillis()
                val lapTime = time - timings[player.name]!!.last()
                val lapTimesForRacer = lapTimes[player.name]
                lapTimes[player.name] = (lapTimesForRacer ?: emptyList()) + lapTime
                scoreboardManager.updateLaps(player, lapTimes[player.name]!!)
                timings[player.name] = (timings[player.name] ?: emptyList()) + time
            }
            .takeWhile { lapTimes.values.any { it.size < track.lapCount } }
            .onCompletion {
                val raceRecap = writeToBook(
                    trackName,
                    lapTimes.mapKeys { (pilotName) -> registeredPilots.first { it.playerName == pilotName } },
                    pilotsManager.getAllTeams(),
                )
                pilots.forEach {
                    it.inventory.addItem(raceRecap)
                }
                raceListeners.remove(trackName)
                scoreboardManager.clearAllBoards()
                PlayerQuitEvent.getHandlerList().unregister(scoreboardManager)
            }
            .launchIn(plugin)
            .let { raceListeners[trackName]!!.first to it }
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

    private fun writeToBook(trackName: String, timings: Map<Pilot, List<Long>>, teams: List<RacingTeam>): ItemStack {
        return ItemStack(Material.WRITTEN_BOOK).apply {
            val builder = (itemMeta as? BookMeta)?.toBuilder() ?: return@apply
            val table = timings.map { it.key to it.value.sum() }.sortedBy { it.second }
            val tablePage = StringBuilder().apply {
                table.forEachIndexed { index, (pilot, laps) ->
                    append("#${index + 1}. ${pilot.colored(teams)} - ${laps.toTiming()}\n")
                }
            }
            val pilotTimingsPages = table.map { (pilot, _) ->
                val laps = timings[pilot]!!
                StringBuilder().apply {
                    append("${pilot.colored(teams)}\'s laps\n")
                    laps.forEachIndexed { number, time ->
                        append("#${number + 1} - ${time.toTiming()}\n")
                    }
                }
            }
            val meta = builder
                .author(Component.text("Race Plugin"))
                .title(Component.text("$trackName race recap"))
                .addPage(Component.text(tablePage.toString()))
                .apply {
                    pilotTimingsPages.forEach { page ->
                        addPage(Component.text(page.toString()))
                    }
                }
                .build()
            itemMeta = meta
        }
    }
}