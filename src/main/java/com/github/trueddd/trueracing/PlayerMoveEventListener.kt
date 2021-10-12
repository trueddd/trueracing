package com.github.trueddd.trueracing

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.plugin.Plugin

@OptIn(ExperimentalCoroutinesApi::class)
fun Plugin.playerMoves(players: List<String>): Flow<PlayerMoveEvent> {
    return callbackFlow {
        val listener = object : Listener {
            @EventHandler
            fun onEventReceived(event: PlayerMoveEvent) {
                if (event.player.name !in players) {
                    return
                }
                if (!event.hasChangedPosition()) {
                    return
                }
                trySend(event)
            }
        }
        server.pluginManager.registerEvents(listener, this@playerMoves)
        awaitClose {
            PlayerMoveEvent.getHandlerList().unregister(listener)
        }
    }
}
