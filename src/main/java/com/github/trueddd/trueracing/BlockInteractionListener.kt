package com.github.trueddd.trueracing

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.plugin.Plugin

@OptIn(ExperimentalCoroutinesApi::class)
fun Plugin.blockInteractions(): Flow<PlayerInteractEvent> {
    return callbackFlow {
        val listener = object : Listener {
            @EventHandler
            fun onEventReceived(event: PlayerInteractEvent) {
                trySend(event)
            }
        }
        server.pluginManager.registerEvents(listener, this@blockInteractions)
        awaitClose {
            PlayerInteractEvent.getHandlerList().unregister(listener)
        }
    }
}
