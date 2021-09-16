package com.github.trueddd.trueracing

import com.github.trueddd.trueracing.data.FinishLineRectangle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import org.bukkit.Bukkit
import org.bukkit.Location

fun consoleLog(message: String) {
    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "say $message")
}

fun Location.isSame(other: Location): Boolean {
    return world.uid == other.world.uid
            && blockX == other.blockX
            && blockY == other.blockY
            && blockZ == other.blockZ
}

fun Flow<Location>.filterIfCrossed(finishLine: FinishLineRectangle): Flow<Location> {
    var inBlock = false
    return flow {
        collect { location ->
            val nowInBlock = when {
                location.world.name != finishLine.minCorner.world -> false
                location.blockX >= finishLine.minCorner.x
                        && location.blockX <= finishLine.maxCorner.x
                        && location.blockZ >= finishLine.minCorner.z
                        && location.blockZ <= finishLine.maxCorner.z -> true
                else -> false
            }
            if (nowInBlock && !inBlock) {
                emit(location)
            }
            inBlock = nowInBlock
        }
    }
}

fun Long.toTiming(): String {
    val minutes = this / 60_000
    val seconds = this.rem (60_000) / 1_000
    val millis = this.rem(1000)
    return String.format("%d:%02d.%03d", minutes, seconds, millis)
}

fun String.formatRed(): String {
    return "§4$this"
}

fun String.formatGreen(): String {
    return "§2$this"
}

fun String.formatPurple(): String {
    return "§5$this"
}

fun Location.toSimpleLocation(): com.github.trueddd.trueracing.data.Location {
    return com.github.trueddd.trueracing.data.Location(
        blockX,
        blockY,
        blockZ,
        world.name,
    )
}