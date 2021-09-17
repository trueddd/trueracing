package com.github.trueddd.trueracing

import fr.minuskube.netherboard.Netherboard
import fr.minuskube.netherboard.bukkit.BPlayerBoard
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

class ScoreboardManager : Listener {

    private val boards = mutableMapOf<String, BPlayerBoard>()

    fun updateLaps(player: Player, laps: List<Long>) {
        val board = boards[player.name] ?: Netherboard.instance().createBoard(player, "Your laps time").also {
            boards[player.name] = it
        }
        val shift = if (laps.size <= 5) 0 else laps.size - 5
        val maxIndex = laps.indexOf(laps.minOrNull()!!) - shift
        laps.takeLast(5).forEachIndexed { index, lapTime ->
            val colorPrefix = if (index == maxIndex) ChatColor.GREEN else ""
            board.set("$colorPrefix${lapTime.toTiming()}", index + 1 + shift)
        }
    }

    fun clearAllBoards() {
        boards.forEach { (_, board) ->
            board.delete()
        }
        boards.clear()
    }

    @EventHandler
    fun onPlayerDisconnect(event: PlayerQuitEvent) {
        println("${event.player.name} quit")
        boards.remove(event.player.name)?.delete()
    }
}