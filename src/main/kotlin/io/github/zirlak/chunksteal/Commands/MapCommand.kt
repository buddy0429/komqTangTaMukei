package io.github.zirlak.chunksteal.Commands

import io.github.zirlak.chunksteal.ChunkSteal
import org.bukkit.Bukkit
import org.bukkit.Bukkit.broadcast
import org.bukkit.Bukkit.broadcastMessage
import org.bukkit.Chunk
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.MapMeta
import org.bukkit.map.*
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException

class MapCommand(private val plugin: ChunkSteal) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (sender is Player) {
            val map = Bukkit.getMap(0) // Create a new map
            if (map != null) {

                // Create a map item stack with the map's id
                val itemMap = ItemStack(Material.FILLED_MAP)
                val meta = itemMap.itemMeta as MapMeta
                meta.mapView = map
                itemMap.itemMeta = meta

                MapUtils.update(plugin)

                // Give the map to the player
                sender.inventory.addItem(itemMap)
                sender.sendMessage("You have received a map!")
                return true
            } else {
                val world: World = sender.world
                val map = Bukkit.createMap(world)

                MapUtils.update(plugin)

                // Create a map item stack with the map's id
                val itemMap = ItemStack(Material.FILLED_MAP)
                val meta = itemMap.itemMeta as MapMeta
                meta.mapView = map
                itemMap.itemMeta = meta

                // Give the map to the player
                sender.inventory.addItem(itemMap)
                sender.sendMessage("You have received a map!")
                return true
            }
        }
        return false
    }

    object MapUtils {

        fun update(plugin: ChunkSteal) {
            for (player in Bukkit.getOnlinePlayers()) {
                MapUtils.updateMap(player, plugin)
            }
        }

        fun updateMap(player: Player, plugin: ChunkSteal) {
            val playerAttributes = mapOf(
                "your name" to Pair(MapPalette.BLUE, MapCursor.Type.BLUE_POINTER),
                "your name 2" to Pair(MapPalette.LIGHT_GREEN, MapCursor.Type.GREEN_POINTER)
                // Add more players here...
            )

            val map: MapView? = Bukkit.getMap(0) // Create a new map
            if (map != null) {
                for (renderer in map.renderers) {
                    map.removeRenderer(renderer)
                }
                map.isUnlimitedTracking = true
                map.isTrackingPosition = true
                val ChunkCenterX = player.location.chunk.x
                val ChunkCenterZ = player.location.chunk.z
                val centerX = player.location.chunk.getBlock(0, 0, 0).x
                val centerZ = player.location.chunk.getBlock(0, 0, 0).z
                map.centerX = centerX
                map.centerZ = centerZ

                // Create a custom renderer
                val renderer = object : MapRenderer() {

                    override fun render(map: MapView, canvas: MapCanvas, player: Player) {
                        val playerLocation = player.location

                        map.renderers.clear()

                        // Clear the map
                        for (x in 0 until 128) {
                            for (y in 0 until 128) {
                                val color = if ((x / 4 + y / 4) % 2 == 0) MapPalette.WHITE else MapPalette.LIGHT_GRAY
                                canvas.setPixel(x, y, color)
                            }
                        }

                        // Render chunks owned by each player
                        for ((playerName, attributes) in playerAttributes) {
                            val ownedChunks = getAllChunksOwnedByPlayer(playerName, plugin)

                            for (chunk in ownedChunks) {
                                val chunkX = (chunk.x - ChunkCenterX) * 4 + 64
                                val chunkZ = (chunk.z - ChunkCenterZ) * 4 + 64
                                for (x in chunkX until chunkX + 4) {
                                    for (z in chunkZ until chunkZ + 4) {
                                        if (x in 0 until 128 && z in 0 until 128) {
                                            canvas.setPixel(x, z, attributes.first)
                                        }
                                    }
                                }
                            }
                        }

                        val cursors = MapCursorCollection()

                        // Add a cursor for each online player
                        for (onlinePlayer in Bukkit.getOnlinePlayers()) {
                            val direction = Math.floorMod(Math.round(onlinePlayer.location.yaw / 22.5).toInt(), 16)

                            val pointerX = ((onlinePlayer.location.x - centerX) / 2).toInt().toByte()
                            val pointerZ = ((onlinePlayer.location.z - centerZ) / 2).toInt().toByte()

                            val pointerType = playerAttributes[onlinePlayer.name]?.second ?: MapCursor.Type.WHITE_POINTER

                            val pointer = MapCursor(
                                pointerX,
                                pointerZ,
                                direction.toByte(),
                                pointerType,
                                true
                            )

                            cursors.addCursor(pointer)
                        }

                        canvas.cursors = cursors
                    }
                }
                map.addRenderer(renderer)
            }
        }
// Call updateMap for all online players


        private fun getAllChunksOwnedByPlayer(player: String, plugin: ChunkSteal): List<Chunk> {
            val chunks = mutableListOf<Chunk>()
            var statement: PreparedStatement? = null
            var resultSet: ResultSet? = null
            try {
                statement = plugin.connection!!.prepareStatement("SELECT chunk FROM chunks WHERE owner = ?")
                statement.setString(1, player)
                resultSet = statement.executeQuery()
                while (resultSet.next()) {
                    val chunkKey = resultSet.getString("chunk").toLong()
                    val world = plugin.server.getWorld("world")
                    val chunk = world?.getChunkAt(chunkKey)
                    if (chunk != null) {
                        chunks.add(chunk)
                    }
                }
            } catch (e: SQLException) {
                // Handle the error
            } finally {
                // Close the ResultSet and the PreparedStatement
                resultSet?.close()
                statement?.close()
            }
            return chunks
        }
    }
}