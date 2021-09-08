package com.github.trueddd.trueracing

import org.bukkit.*
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot

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

    private fun onMarked() {
        currentLine.forEach {
            it.world.playEffect(it, Effect.LAVA_CONVERTS_BLOCK, 0)
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