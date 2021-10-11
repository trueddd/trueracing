package com.github.trueddd.trueracing

import com.github.trueddd.trueracing.data.model.FinishLineRectangle
import com.github.trueddd.trueracing.data.model.Pilot
import com.github.trueddd.trueracing.data.model.RacingTeam
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.entity.Player

fun Flow<Pair<Player, Location>>.filterIfCrossed(finishLine: FinishLineRectangle, pilots: List<String>): Flow<Player> {
    val inBlockMap = pilots.associateWith { false }.toMutableMap()
    return flow {
        collect { (player, location) ->
            val nowInBlock = when {
                location.world.name != finishLine.minCorner.world -> false
                location.blockX >= finishLine.minCorner.x
                        && location.blockX <= finishLine.maxCorner.x
                        && location.blockZ >= finishLine.minCorner.z
                        && location.blockZ <= finishLine.maxCorner.z -> true
                else -> false
            }
            if (nowInBlock && inBlockMap[player.name] == false) {
                emit(player)
            }
            inBlockMap[player.name] = nowInBlock
        }
    }
}

fun Long.toTiming(): String {
    val minutes = this / 60_000
    val seconds = this.rem (60_000) / 1_000
    val millis = this.rem(1000)
    return String.format("%d:%02d.%03d", minutes, seconds, millis)
}

fun Location.toSimpleLocation(): com.github.trueddd.trueracing.data.model.Location {
    return com.github.trueddd.trueracing.data.model.Location(
        blockX,
        blockY,
        blockZ,
        world.name,
    )
}

fun Pilot.colored(teams: List<RacingTeam>): String {
    val team = teams.firstOrNull { it.name == teamName }
    return if (team == null) {
        playerName
    } else {
        val color = ChatColor.getByChar(team.color) ?: ""
        "$color$playerName${ChatColor.RESET}"
    }
}
