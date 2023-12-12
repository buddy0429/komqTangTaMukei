package io.github.zirlak.chunksteal.Listeners

import io.github.zirlak.chunksteal.ChunkSteal
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.BlockDisplay
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException

class ChunkGlassListener(private val plugin: ChunkSteal) : Listener {

    /*

   private fun createGlassBorder(player: Player, remove: Boolean, direction: String) {
       val world = player.world
       val playerChunk = player.location.chunk

       // Calculate the nearest chunk border
       val chunkX = if (player.location.x % 16 > 8) playerChunk.x + 1 else playerChunk.x
       val chunkZ = if (player.location.z % 16 > 8) playerChunk.z + 1 else playerChunk.z

       // Convert chunk coordinates to world coordinates
       var borderX = chunkX * 16
       var borderZ = chunkZ * 16

       if (getNearestChunkDirection(player) == "west") {
           borderX -= 1
       }

       if (getNearestChunkDirection(player) == "north") {
           borderZ -= 1
       }

       // Create or remove the glass border at the nearest chunk border
       for (y in -1..3) {
           for (i in -2..2) {
               val location = getLocation(world, borderX, borderZ, player, y, i, direction)
               val block = world.getBlockAt(location)
               if (remove && isGlassBorder(block)) {
                       block.type = Material.AIR
               } else if (block.type == Material.RED_STAINED_GLASS && (y == -1 || y == 3) && (i == -2 || i == 2)) {
                   block.type = Material.AIR
               } else if (!remove && block.type == Material.AIR && y in 0..2 && i in -1..1) {
                   block.type = Material.RED_STAINED_GLASS
               }
           }
       }
   }

   */

    private val playerDirections = mutableMapOf<Player, String>()

    @EventHandler
    fun onPlayerMoveGlass(event: PlayerMoveEvent) {
        val player = event.player
        val currentChunk = player.location.chunk
        var nearestChunk: Chunk? = null
        var nearestDistance = Double.MAX_VALUE
        val previousDirection: String = playerDirections[player].toString()

        for (chunk in player.world.loadedChunks) {
            if (chunk != currentChunk) {
                val chunkDistance = chunk.closestPointTo(player.location).distance(player.location)
                if (nearestChunk == null || chunkDistance < nearestDistance) {
                    nearestChunk = chunk
                    nearestDistance = chunkDistance
                }
            }
        }

        if (nearestChunk != null) {
            val direction = player.location.directionTo(nearestChunk.closestPointTo(player.location))
            if(direction == "West" || direction == "North") {nearestDistance -=1}

            if (previousDirection != direction) {
                createGlassBorder(player, true, previousDirection, currentChunk)
                playerDirections[player] = direction
            }

            if (nearestDistance <= 3 && !isPlayerOwnerOfChunk(player, nearestChunk)) {
                createGlassBorder(player, false, direction, nearestChunk)
            } else {
                createGlassBorder(player, true, direction, nearestChunk)
            }
        }
    }

    private fun Chunk.closestPointTo(location: Location): Location {
        val chunkX = this.x shl 4
        val chunkZ = this.z shl 4

        val minX = chunkX.toDouble()
        val minZ = chunkZ.toDouble()
        val maxX = minX + 15
        val maxZ = minZ + 15

        val x = clamp(location.x, minX, maxX)
        val z = clamp(location.z, minZ, maxZ)

        return Location(this.world, x, location.y, z)
    }

    private fun clamp(value: Double, min: Double, max: Double): Double {
        return Math.max(min, Math.min(max, value))
    }

    private fun Location.directionTo(location: Location): String {
        val dx = Math.abs(location.x - this.x)
        val dz = Math.abs(location.z - this.z)

        return when {
            dx > dz -> if (location.x > this.x) "East" else "West"
            else -> if (location.z > this.z) "South" else "North"
        }
    }

    private fun getLocation(location: Location, player: Player, y: Int, i: Int, i2: Int, direction: String): Location {
        val x = if (direction == "West"||direction == "East") location.x + i2.toDouble() else player.location.x + i.toDouble()
        val z = if (direction == "North"||direction == "South") location.z + i2.toDouble() else player.location.z + i.toDouble()
        return Location(location.world, x, player.location.y + y, z)
    }

    private fun createGlassBorder(player: Player, remove: Boolean, direction: String, nearestChunk: Chunk) {
        val world = player.world
        val playerChunk = player.location.chunk
        val closetlocation = nearestChunk.closestPointTo(player.location)

        // Create or remove the glass border at the nearest chunk border
        for (y in -1..3) {
            for (i2 in -1..1) {
                for (i in -2..2) {
                    val location = getLocation(closetlocation, player, y, i, i2, direction)
                    //need to fix location error
                    location.x = Math.round(location.x).toDouble()
                    location.y = Math.round(location.y).toDouble()
                    location.z = Math.round(location.z).toDouble()
                    if (remove) {
                        world.getNearbyEntities(location, 0.5, 0.5, 0.5).forEach { entity ->
                            if (entity is BlockDisplay) {
                                entity.remove()
                            }
                        }

                        val entities = world.entities

                        for (entity in entities) {
                            // 엔티티의 타입이 block_display인지 확인
                            if (entity.type == EntityType.BLOCK_DISPLAY) {
                                val blockDisplay = entity as BlockDisplay
                                if (blockDisplay.block.placementMaterial == Material.RED_STAINED_GLASS) {
                                    // block_display 엔티티를 제거
                                    blockDisplay.remove()
                                }
                            }
                        }


                    } else if (y in 0..2 && i in -1..1 && i2 == 0) {
                        // Check if there is already a BlockDisplay at this location
                        val existingBlockDisplay = world.getNearbyEntities(location, 0.5, 0.5, 0.5).any { entity ->
                            entity is BlockDisplay && entity.block.placementMaterial == Material.RED_STAINED_GLASS
                        }
                        if (!existingBlockDisplay && playerChunk != location.chunk) {
                            // Create a BlockDisplay with a red glass block at this location
                            val blockDisplay = world.spawnEntity(location, EntityType.BLOCK_DISPLAY) as BlockDisplay
                            blockDisplay.block = Bukkit.createBlockData(Material.RED_STAINED_GLASS)
                            blockDisplay.setGravity(false)
                            blockDisplay.isSilent = true
                            blockDisplay.interpolationDelay = 0 // The entity will die after 5 minutes
                            blockDisplay.interpolationDuration = 1
                        }

                    } else {
                        world.getNearbyEntities(location, 0.5, 0.5, 0.5).forEach { entity ->
                            if (entity is BlockDisplay && entity.block.placementMaterial == Material.RED_STAINED_GLASS) {
                                entity.remove()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun isPlayerOwnerOfChunk(player: Player, chunk: Chunk): Boolean {
        var statement: PreparedStatement? = null
        var resultSet: ResultSet? = null
        try {
            statement = plugin.connection!!.prepareStatement("SELECT owner FROM chunks WHERE chunk = ?")
            statement.setString(1, chunk.chunkKey.toString())
            resultSet = statement.executeQuery()
            return if (resultSet.next()) resultSet.getString("owner") == player.name else false
        } catch (e: SQLException) {
            // Handle the error
            return false
        } finally {
            // Close the ResultSet and the PreparedStatement
            resultSet?.close()
            statement?.close()
        }
    }

    /*

    @EventHandler
    fun onPlayerMoveGlass(event: PlayerMoveEvent) {
        val player = event.player
        val distanceToBorderX = Doubles.min(player.location.x % 16, 16 - player.location.x % 16)
        val distanceToBorderZ = Doubles.min(player.location.z % 16, 16 - player.location.z % 16)

        // Calculate the nearest chunk border
        val chunkX = if (player.location.x % 16 > 8) player.location.chunk.x + 1 else player.location.chunk.x
        val chunkZ = if (player.location.z % 16 > 8) player.location.chunk.z + 1 else player.location.chunk.z

        // Convert chunk coordinates to world coordinates
        val borderX = chunkX * 16
        val borderZ = chunkZ * 16

        // Get the chunk at the nearest border
        val borderChunk = player.world.getChunkAt(borderX, borderZ)

        if (distanceToBorderX <= 2 && !isPlayerOwnerOfChunk(player, borderChunk)) {
            createGlassBorder(player, remove = false, direction = "x")
        } else if (distanceToBorderZ <= 2 && !isPlayerOwnerOfChunk(player, borderChunk)) {
            createGlassBorder(player, remove = false, direction = "z")
        } else {
            createGlassBorder(player, remove = true, direction = "x")
            createGlassBorder(player, remove = true, direction = "z")
        }
    }







    private fun createGlassBorder(player: Player, remove: Boolean, direction: String) {
        val world = player.world
        val playerChunk = player.location.chunk
        // Calculate the nearest chunk border
        val chunkX = if (player.location.x % 16 > 8) playerChunk.x + 1 else playerChunk.x
        val chunkZ = if (player.location.z % 16 > 8) playerChunk.z + 1 else playerChunk.z

        // Convert chunk coordinates to world coordinates
        var borderX = chunkX * 16
        var borderZ = chunkZ * 16

        if (getNearestChunkDirection(player) == "west") {
            borderX -= 1
        }

        if (getNearestChunkDirection(player) == "north") {
            borderZ -= 1
        }

        // Create or remove the glass border at the nearest chunk border
        for (y in -1..3) {
            for (i2 in -1..1) {
                for (i in -2..2) {
                    val location = getLocation(world, borderX, borderZ, player, y, i, i2, direction)
                    location.x = Math.round(location.x).toDouble()
                    location.y = Math.round(location.y).toDouble()
                    location.z = Math.round(location.z).toDouble()
                    if (remove) {
                        // Remove any ArmorStands at this location
                        world.getNearbyEntities(location, 0.5, 0.5, 0.5).forEach { entity ->
                            if (entity is BlockDisplay) {
                                entity.remove()
                            }
                        }

                        val entities = world.entities

                        for (entity in entities) {
                            // 엔티티의 타입이 block_display인지 확인
                            if (entity.type == EntityType.BLOCK_DISPLAY) {
                                val blockDisplay = entity as BlockDisplay
                                if (blockDisplay.block.placementMaterial == Material.RED_STAINED_GLASS) {
                                    // block_display 엔티티를 제거
                                    blockDisplay.remove()
                                }
                            }
                        }


                    } else if (y in 0..2 && i in -1..1 && i2 == 0) {
                        // Check if there is already a BlockDisplay at this location
                        val existingBlockDisplay = world.getNearbyEntities(location, 0.5, 0.5, 0.5).any { entity ->
                            entity is BlockDisplay && entity.block.placementMaterial == Material.RED_STAINED_GLASS
                        }
                        if (!existingBlockDisplay /*&& player.location.chunk != location.chunk*/) {
                            // Create a BlockDisplay with a red glass block at this location
                            val blockDisplay = world.spawnEntity(location, EntityType.BLOCK_DISPLAY) as BlockDisplay
                            blockDisplay.block = Bukkit.createBlockData(Material.RED_STAINED_GLASS)
                            blockDisplay.setGravity(false)
                            blockDisplay.isSilent = true
                            blockDisplay.interpolationDelay = 0 // The entity will die after 5 minutes
                            blockDisplay.interpolationDuration = 1
                        }

                    } else {
                        world.getNearbyEntities(location, 0.5, 0.5, 0.5).forEach { entity ->
                            if (entity is BlockDisplay && entity.block.placementMaterial == Material.RED_STAINED_GLASS) {
                                entity.remove()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun getNearestChunkDirection(player: Player): String {
        val playerChunk = player.location.chunk

        // Calculate the center of the chunk
        val chunkCenterX = playerChunk.x * 16 + 8
        val chunkCenterZ = playerChunk.z * 16 + 8

        // Calculate the direction to the nearest chunk
        val directionX = player.location.x - chunkCenterX
        val directionZ = player.location.z - chunkCenterZ

        return if (Math.abs(directionX) > Math.abs(directionZ)) {
            if (directionX > 0) "east" else "west"
        } else {
            if (directionZ > 0) "south" else "north"
        }
    }

    private fun getLocation(world: World, borderX: Int, borderZ: Int, player: Player, y: Int, i: Int, i2: Int, direction: String): Location {
        val x = if (direction == "x") borderX.toDouble() + i2.toDouble() else player.location.x + i.toDouble()
        val z = if (direction == "z") borderZ.toDouble() + i2.toDouble() else player.location.z + i.toDouble()
        return Location(world, x, player.location.y + y, z)
    }

    */

}