package io.github.zirlak.chunksteal.Commands

import io.github.zirlak.chunksteal.ChunkSteal
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.sql.PreparedStatement
import java.sql.SQLException

class ChunkCommand(private val plugin: ChunkSteal) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender is Player) {
            val chunk = sender.location.chunk
            var statement: PreparedStatement? = null
            try {
                statement = plugin.connection!!.prepareStatement("INSERT INTO chunks (owner, chunk) VALUES (?, ?)")
                statement.setString(1, sender.name)
                statement.setString(2, chunk.chunkKey.toString())
                statement.executeUpdate()
                sender.sendMessage("You bought this chunk!")
            } catch (e: SQLException) {
                // Handle the error
            } finally {
                // Close the PreparedStatement
                statement?.close()
            }
        }
        return true
    }
}