package io.github.zirlak.chunksteal

import io.github.zirlak.chunksteal.Commands.ChunkCommand
import io.github.zirlak.chunksteal.Listeners.AdvancementsListener
import io.github.zirlak.chunksteal.Listeners.ChunkGlassListener
import io.github.zirlak.chunksteal.Listeners.ChunkListener
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

class ChunkSteal : JavaPlugin() {

    var connection: Connection? = null

    override fun onEnable() {
        // Plugin startup logic
        setupDatabase()
        server.pluginManager.registerEvents(AdvancementsListener(this), this)
        server.pluginManager.registerEvents(ChunkListener(this), this)
        server.pluginManager.registerEvents(ChunkGlassListener(this), this)
        getCommand("buychunk")?.setExecutor(ChunkCommand(this))
        logger.info("ChunkSteal is enabled!")
    }

    override fun onDisable() {
        // Plugin shutdown logic
        logger.info("ChunkSteal is disabled!")
    }

    private fun setupDatabase() {
        val dbFile = File(dataFolder, "chunks.db")
        if (!dbFile.exists()) {
            dbFile.parentFile.mkdirs()
            dbFile.createNewFile()
        }
        try {
            if (connection != null && !connection!!.isClosed) {
                return
            }
            connection = DriverManager.getConnection("jdbc:sqlite:$dbFile")
            val statement = connection!!.createStatement()
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS chunks (owner TEXT NOT NULL,chunk TEXT NOT NULL)")
        } catch (exception: SQLException) {
            exception.printStackTrace()
        }
    }
}
