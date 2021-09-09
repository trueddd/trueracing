package com.github.trueddd.trueracing

import org.bukkit.*
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot

// todo: make it usable by multiple people in a time
class FinishLineRegistrar : Listener {

    var isNowMarking: Boolean = false
        set(value) {
            if (value == field) {
                return
            }
            field = value
            if (value) {
                currentLine.clear()
            } else {
                onMarked()
            }
        }

    private val currentLine = mutableSetOf<Location>()

    val finishLine: Set<Location>
        get() = currentLine

    var direction: String = ""
        set(value) {
            field = value
            consoleLog("Direction is set to $value")
        }

    private fun onMarked() {
        var minX = Int.MAX_VALUE
        var maxX = Int.MIN_VALUE
        var minZ = Int.MAX_VALUE
        var maxZ = Int.MIN_VALUE
        currentLine.forEach {
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
        consoleLog("X { min: $minX; max: $maxX }; Z { min: $minZ; max: $maxZ }")
        val diffX = maxX - minX
        val diffZ = maxZ - minZ
        when {
            diffX > diffZ -> consoleLog("North or South")
            diffZ > diffX -> consoleLog("East or West")
            else -> consoleLog("Area is square")
        }
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        if (!isNowMarking) {
            return
        }
        val block = event.clickedBlock ?: return
        if (event.hand != EquipmentSlot.HAND) {
            return
        }
        if (event.player.inventory.itemInMainHand.type == Material.WOODEN_SWORD) {
            event.player.sendMessage("registered ${block.type}")
            event.player.playSound(event.player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.2f, 1.0f)
            currentLine.add(block.location)
        }
    }
}