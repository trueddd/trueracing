package com.github.trueddd.trueracing.data.model

data class Track(
    val name: String,
    val location: Location,
    val lapCount: Int,
    val finishLine: FinishLineRectangle?,
    val lights: List<Location>?,
)
