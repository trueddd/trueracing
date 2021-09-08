package com.github.trueddd.trueracing

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin

@Suppress("unused")
class TrueRacing : JavaPlugin() {

    private val finishLineListener by lazy { FinishLineListener() }
    private val finishLineRegistrar by lazy { FinishLineRegistrar() }

    override fun onEnable() {
        server.consoleSender.sendMessage("Plugin enabled")
        server.pluginManager.registerEvents(finishLineListener, this)
        server.pluginManager.registerEvents(finishLineRegistrar, this)
    }

    override fun onDisable() {
        server.consoleSender.sendMessage("Plugin disabled")
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        when (command.label) {
            "finish-line" -> {
                finishLineRegistrar.isNowMarking = !finishLineRegistrar.isNowMarking
            }
        }
        return super.onCommand(sender, command, label, args)
    }
}
