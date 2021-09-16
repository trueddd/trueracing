package com.github.trueddd.trueracing.data

data class Track(
    val name: String,
    val location: Location,
    val finishLine: FinishLineRectangle?,
)
