package com.github.trueddd.trueracing

import kotlinx.coroutines.flow.MutableSharedFlow
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent

class FinishLineListener(
    private val players: List<String>,
) : Listener {

    val playerPositionFlow = MutableSharedFlow<Pair<Player, Location>>(extraBufferCapacity = 1)

    @EventHandler
    fun onEvent(event: PlayerMoveEvent) {
        if (event.player.name !in players) {
            return
        }
        if (!event.hasChangedPosition()) {
            return
        }
        val location = event.player.location
        playerPositionFlow.tryEmit(event.player to location)
    }
}