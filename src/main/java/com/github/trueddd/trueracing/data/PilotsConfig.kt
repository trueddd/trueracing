package com.github.trueddd.trueracing.data

import com.github.trueddd.trueracing.data.model.Pilot
import com.github.trueddd.trueracing.data.model.RacingTeam

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
