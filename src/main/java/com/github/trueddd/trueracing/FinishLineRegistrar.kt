package com.github.trueddd.trueracing

import com.github.trueddd.trueracing.data.model.FinishLineRectangle
import org.bukkit.Effect
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.plugin.Plugin

class FinishLineRegistrar(
    private val plugin: Plugin,
) : Listener {

    private val markingMap = mutableMapOf<String, Set<Location>>()

    fun isPlayerMarking(player: Player): Boolean {
        return markingMap[player.name] != null
    }

    fun startMarking(player: Player) {
        if (markingMap.keys.isEmpty()) {
            plugin.server.pluginManager.registerEvents(this, plugin)
        }
        markingMap[player.name] = emptySet()
    }

    fun stopMarking(player: Player): FinishLineRectangle? {
        if (markingMap.keys.size == 1) {
            PlayerInteractEvent.getHandlerList().unregister(this)
        }
        val line = markingMap[player.name]
        if (line.isNullOrEmpty()) {
            markingMap.remove(player.name)
            return null
        }
        var minX = Int.MAX_VALUE
        var maxX = Int.MIN_VALUE
        var minZ = Int.MAX_VALUE
        var maxZ = Int.MIN_VALUE
        line.forEach {
            it.world.playEffect(it, Effect.VILLAGER_PLANT_GROW, 5)
            if (it.blockX > maxX) {
                maxX = it.blockX
            }
            if (it.blockX < minX) {
                minX = it.blockX
            }
            if (it.blockZ > maxZ) {
                maxZ = it.blockZ
            }
            if (it.blockZ < minZ) {
                minZ = it.blockZ
            }
        }
        val world = line.first().world.name
        val y = line.first().blockY
        return FinishLineRectangle(
            com.github.trueddd.trueracing.data.model.Location(minX, y, minZ, world),
            com.github.trueddd.trueracing.data.model.Location(maxX, y, maxZ, world),
        )
            .also { markingMap.remove(player.name) }
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        val currentLine = markingMap[event.player.name] ?: return
        val block = event.clickedBlock ?: return
        if (event.hand != EquipmentSlot.HAND) {
            return
        }
        if (event.player.inventory.itemInMainHand.type == Material.WOODEN_SWORD) {
            if (currentLine.isNotEmpty() && block.location.blockY != currentLine.last().blockY) {
                event.player.sendMessage("Cannot register finish line with changing Y coordinate.")
                return
            }
            event.player.playSound(event.player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.2f, 1.0f)
            markingMap[event.player.name] = currentLine + block.location
        }
    }
}