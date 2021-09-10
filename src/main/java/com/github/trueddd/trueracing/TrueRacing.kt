package com.github.trueddd.trueracing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.plus
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin

// todo: refactor
@Suppress("unused")
class TrueRacing : JavaPlugin() {

    private val finishLineListener by lazy { FinishLineListener() }
    private val finishLineRegistrar by lazy { FinishLineRegistrar() }
    private val bossBarManager by lazy { BossBarManager(finishLineListener.playerPosition) }

    private var lineCrossJob: Job? = null

    override fun onEnable() {
        server.consoleSender.sendMessage("Plugin enabled")
        server.pluginManager.registerEvents(finishLineListener, this)
        server.pluginManager.registerEvents(finishLineRegistrar, this)
        server.pluginManager.registerEvents(bossBarManager, this)
    }

    override fun onDisable() {
        server.consoleSender.sendMessage("Plugin disabled")
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        when (command.label) {
            "finish-line" -> {
                finishLineRegistrar.isNowMarking = !finishLineRegistrar.isNowMarking
            }
            "race" -> {
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
                    .launchIn(GlobalScope + PluginDispatcher(this))
            }
        }
        return super.onCommand(sender, command, label, args)
    }
}
