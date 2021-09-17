package com.github.trueddd.trueracing

import com.github.trueddd.trueracing.command.CommandHandler
import kotlinx.coroutines.CoroutineScope
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin
import kotlin.coroutines.CoroutineContext

@Suppress("unused")
class TrueRacing : JavaPlugin(), CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = PluginDispatcher(this)

    private val finishLineListener by lazy { FinishLineListener() }
    private val finishLineRegistrar by lazy { FinishLineRegistrar(this) }
    private val pluginConfigManager by lazy { PluginConfigManager(this) }
    private val pilotsManager by lazy { PilotsManager(this) }
    private val commandHandler by lazy { CommandHandler(this, finishLineRegistrar, finishLineListener, pluginConfigManager, pilotsManager) }

    override fun onEnable() {
        server.consoleSender.sendMessage("Plugin enabled")
        server.pluginManager.registerEvents(finishLineListener, this)
    }

    override fun onDisable() {
        server.consoleSender.sendMessage("Plugin disabled")
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        return commandHandler.handle(sender, label, args.toList())
    }
}
