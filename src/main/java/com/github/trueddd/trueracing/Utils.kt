package com.github.trueddd.trueracing

import org.bukkit.Bukkit

fun consoleLog(message: String) {
    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "say $message")
}