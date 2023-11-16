package io.github.zirlak.chunksteal

import io.github.zirlak.chunksteal.Listeners.AdvancementsListener
import org.bukkit.plugin.java.JavaPlugin

class ChunkSteal : JavaPlugin() {
    override fun onEnable() {
        // Plugin startup logic
        server.pluginManager.registerEvents(AdvancementsListener(this), this)
        logger.info("ChunkSteal is enabled!")
    }

    override fun onDisable() {
        // Plugin shutdown logic
        logger.info("ChunkSteal is disabled!")
    }
}
