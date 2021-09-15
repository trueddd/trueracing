package com.github.trueddd.trueracing.data

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
