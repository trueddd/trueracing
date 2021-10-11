package com.github.trueddd.trueracing.data

import com.github.trueddd.trueracing.data.model.Track

data class PluginConfig(
    val tracks: List<Track>,
) {

    companion object {

        fun default(): PluginConfig {
            return PluginConfig(
                emptyList(),
            )
        }
    }
}
