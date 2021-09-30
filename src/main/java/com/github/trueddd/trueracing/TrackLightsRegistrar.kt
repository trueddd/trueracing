package com.github.trueddd.trueracing

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.plugin.Plugin

class TrackLightsRegistrar(
    private val plugin: Plugin,
) : Listener {

    private val markers = mutableMapOf<String, Set<Location>>()

    fun isPlayerMarking(player: Player): Boolean {
        return markers[player.name] != null
    }

    fun startMarking(player: Player) {
        if (markers.keys.isEmpty()) {
            plugin.server.pluginManager.registerEvents(this, plugin)
        }
        markers[player.name] = emptySet()
    }

    fun stopMarking(player: Player): Set<Location>? {
        if (markers.keys.size == 1) {
            PlayerInteractEvent.getHandlerList().unregister(this)
        }
        val line = markers[player.name]
        if (line.isNullOrEmpty()) {
            markers.remove(player.name)
            return null
        }
        return line
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        val currentLine = markers[event.player.name] ?: return
        val block = event.clickedBlock ?: return
        if (block.blockData.material != Material.REDSTONE_LAMP) {
            return
        }
        if (event.hand != EquipmentSlot.HAND) {
            return
        }
        event.player.playSound(event.player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.2f, 1.0f)
        markers[event.player.name] = currentLine + block.location
    }
}