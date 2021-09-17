package com.github.trueddd.trueracing.data

data class PilotsConfig(
    val pilots: List<Pilot>,
    val teams: List<RacingTeam>,
) {

    companion object {

        fun default(): PilotsConfig {
            return PilotsConfig(
                emptyList(),
                emptyList(),
            )
        }
    }
}
