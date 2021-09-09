package com.github.trueddd.trueracing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.plus
import org.bukkit.Location
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin

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

    private fun Location.isSame(other: Location): Boolean {
        return world.uid == other.world.uid
                && blockX == other.blockX
                && blockY == other.blockY
                && blockZ == other.blockZ
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        when (command.label) {
            "finish-line" -> {
                if (args.isEmpty()) {
                    finishLineRegistrar.isNowMarking = !finishLineRegistrar.isNowMarking
                } else {
                    when (args.first()) {
                        "ns" -> finishLineRegistrar.direction = "ns"
                        "sn" -> finishLineRegistrar.direction = "sn"
                        "we" -> finishLineRegistrar.direction = "we"
                        "ew" -> finishLineRegistrar.direction = "ew"
                    }
                }
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
                lineCrossJob = finishLineListener.playerPosition
                    .onStart { println("race started") }
                    .distinctUntilChanged { old, new ->
                        // fixme
                        val movedRight = when (finishLineRegistrar.direction) {
                            "ns" -> old.blockZ < new.blockZ
                            "sn" -> old.blockZ > new.blockZ
                            "we" -> old.blockX < new.blockX
                            "ew" -> old.blockX > new.blockX
                            else -> return@distinctUntilChanged true
                        }
                        !movedRight
                    }
                    .filter { location ->
                        line.any { it.isSame(location) }
                    }
                    .flowOn(Dispatchers.Default)
                    .onEach { consoleLog("crossed line at ${it.blockX} ${it.blockY} ${it.blockZ}") }
                    .launchIn(GlobalScope + PluginDispatcher(this))
            }
        }
        return super.onCommand(sender, command, label, args)
    }
}
