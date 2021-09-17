package com.github.trueddd.trueracing

import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.bukkit.plugin.Plugin
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.lang.reflect.Type

abstract class BaseConfigManager<T>(
    protected val plugin: Plugin,
) {

    abstract val fileName: String

    abstract fun getDefaultConfig(): T

    abstract fun getConfigType(): Type

    private val gson = Gson()

    private val configFile by lazy { File(plugin.dataFolder, fileName) }

    protected val config = MutableStateFlow(getDefaultConfig())

    private fun readConfigFromFile() {
        config.value = gson.fromJson(JsonReader(FileReader(configFile)), getConfigType()) ?: getDefaultConfig()
    }

    init {
        if (!configFile.exists()) {
            configFile.parentFile.mkdir()
            configFile.createNewFile()
        }
        readConfigFromFile()
    }

    @OptIn(DelicateCoroutinesApi::class)
    protected fun updateConfig() {
        GlobalScope.launch(Dispatchers.IO) {
            val writer = FileWriter(configFile, false)
            gson.toJson(config.value, getConfigType(), writer)
            writer.close()
        }
    }
}