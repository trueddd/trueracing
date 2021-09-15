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
                finishLineCorners = null
            } else {
                onMarked()
            }
        }

    private val currentLine = mutableSetOf<Location>()

    val finishLine: Set<Location>
        get() = currentLine

    var finishLineCorners: Pair<Location, Location>? = null
        private set

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
//        println("X { min: $minX; max: $maxX }; Z { min: $minZ; max: $maxZ }")
        if (currentLine.isEmpty()) {
            finishLineCorners = null
            return
        }
        val world = currentLine.first().world
        val y = currentLine.first().blockY
        finishLineCorners = Location(world, minX.toDouble(), y.toDouble(), minZ.toDouble()) to Location(world, maxX.toDouble(), y.toDouble(), maxZ.toDouble())
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
            if (currentLine.isNotEmpty() && block.location.blockY != currentLine.last().blockY) {
                event.player.sendMessage("Cannot register finish line with changing Y coordinate.")
                return
            }
            event.player.sendMessage("registered ${block.type}")
            event.player.playSound(event.player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.2f, 1.0f)
            currentLine.add(block.location)
        }
    }
}