package io.github.zirlak.chunksteal

import io.github.zirlak.chunksteal.Commands.MapCommand
import io.github.zirlak.chunksteal.Commands.ChunkCommand
import io.github.zirlak.chunksteal.Commands.NormalAxeCommand
import io.github.zirlak.chunksteal.Commands.SpecialAxeCommand
import io.github.zirlak.chunksteal.Listeners.*
import org.bukkit.Bukkit
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
        server.pluginManager.registerEvents(NetherPortalListener(), this)
        server.pluginManager.registerEvents(MapListener(), this)
        getCommand("buychunk")?.setExecutor(ChunkCommand(this))
        getCommand("normalaxe")?.setExecutor(NormalAxeCommand(this))
        getCommand("specialaxe")?.setExecutor(SpecialAxeCommand(this))
        getCommand("map")?.setExecutor(MapCommand(this))

        logger.info("ChunkSteal is enabled!")
    }

    override fun onDisable() {
        // Plugin shutdown logic
        logger.info("ChunkSteal is disabled!")
        Bukkit.getScheduler().cancelTasks(this)
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
