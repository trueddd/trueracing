package com.github.trueddd.trueracing

import org.bukkit.plugin.java.JavaPlugin

@Suppress("unused")
class TrueRacing : JavaPlugin() {

    override fun onEnable() {
        server.consoleSender.sendMessage("Plugin enabled")
    }

    override fun onDisable() {
        server.consoleSender.sendMessage("Plugin disabled")
    }
}
