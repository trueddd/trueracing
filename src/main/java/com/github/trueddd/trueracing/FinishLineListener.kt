package com.github.trueddd.trueracing

import org.bukkit.Bukkit
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent

class FinishLineListener : Listener {

    private val bossBar by lazy {
        Bukkit.createBossBar("Position", BarColor.GREEN, BarStyle.SOLID)
            .also { it.isVisible = true }
    }

    @EventHandler
    fun onLogin(event: PlayerJoinEvent) {
        bossBar.addPlayer(event.player)
    }

    @EventHandler
    fun onPlayerDisconnect(event: PlayerQuitEvent) {
        bossBar.removePlayer(event.player)
    }

    @EventHandler
    fun onEvent(event: PlayerMoveEvent) {
        if (!event.hasChangedPosition()) {
            return
        }
        val location = event.player.location
        val x = location.x.toString().take(6)
        val y = location.y.toString().take(6)
        val z = location.z.toString().take(6)
        bossBar.setTitle("$x $y $z")
    }
}