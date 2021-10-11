package com.github.trueddd.trueracing.data.model

data class Race(
    val pilots: List<Pilot>,
    val status: RaceStatus,
    val timings: Map<String, List<Long>>,
)
