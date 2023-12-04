package io.github.zirlak.chunksteal.Listeners

import com.google.common.primitives.Doubles.min
import io.github.zirlak.chunksteal.ChunkSteal
import io.github.zirlak.chunksteal.Commands.MapCommand
import net.kyori.adventure.text.Component
import org.bukkit.*
import org.bukkit.Bukkit.broadcastMessage
import org.bukkit.Bukkit.createBlockData
import org.bukkit.entity.BlockDisplay
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.sql.PreparedStatement
import org.bukkit.event.block.Action
import java.sql.ResultSet
import java.sql.SQLException
import io.github.zirlak.chunksteal.Commands.MapCommand.MapUtils.updateMap
import java.util.*
import kotlin.collections.HashSet


class ChunkListener(private val plugin: ChunkSteal) : Listener {
    @EventHandler
    fun onPlayerMoveEffect(event: PlayerMoveEvent) {
        val player = event.player
        val chunk = player.location.chunk
        val owner = findChunkOwner(chunk.chunkKey.toString()).toString()
        if (owner != player.name) {
            player.addPotionEffect(PotionEffect(PotionEffectType.WITHER, 20 * 10, 2), true)
            player.addPotionEffect(PotionEffect(PotionEffectType.HUNGER, 20 * 10, 2), true)
        } else {
            player.removePotionEffect(PotionEffectType.WITHER)
            player.removePotionEffect(PotionEffectType.HUNGER)
        }
    }

    private fun findChunkOwner(chunkKey: String): String? {
        var statement: PreparedStatement? = null
        var resultSet: ResultSet? = null
        try {
            statement = plugin.connection!!.prepareStatement("SELECT owner FROM chunks WHERE chunk = ?")
            statement.setString(1, chunkKey)
            resultSet = statement.executeQuery()
            return if (resultSet.next()) resultSet.getString("owner") else null
        } catch (e: SQLException) {
            // Handle the error
            return null
        } finally {
            // Close the ResultSet and the PreparedStatement
            resultSet?.close()
            statement?.close()
        }
    }


    @EventHandler
    fun onPlayerInteractGlass(event: PlayerInteractEvent) {
        val player: Player = event.player
        val item = event.player.inventory.itemInMainHand
        if (event.action == Action.RIGHT_CLICK_AIR || event.action == Action.RIGHT_CLICK_BLOCK) {
            val clickedEntity = player.getTargetEntity(3.0)
            if (isSpecialItem(item) && isGlassBorder(clickedEntity)) {
                val chunk = clickedEntity!!.location.chunk
                player.inventory.removeItem(item)
                giveChunkToPlayer(player, chunk)
            }
        }
    }


    private fun Player.getTargetEntity(radius: Double): Entity? {
        // Get the entity the player is looking at within a certain radius
        val entities = this.getNearbyEntities(radius, radius, radius)
        val sightBlock = this.getTargetBlock(null, (radius * 2).toInt())
        var closestEntity: Entity? = null
        var closestDistance = Double.MAX_VALUE
        for (entity in entities) {
            val distance = entity.location.distance(sightBlock.location)
            if (distance < closestDistance) {
                closestEntity = entity
                closestDistance = distance
            }
        }
        return closestEntity
    }

    private fun isSpecialItem(item: ItemStack): Boolean {
        // Check if the item is the special item
        val normalAxe = ItemStack(Material.IRON_AXE)
        val normalAxeMeta = normalAxe.itemMeta
        normalAxeMeta.displayName(Component.text("§a[일반 땅]"))
        normalAxeMeta.setCustomModelData(1)
        val key = NamespacedKey(plugin, "normal")
        normalAxeMeta.persistentDataContainer.set(key, PersistentDataType.STRING, "normal")
        normalAxeMeta.isUnbreakable = true
        normalAxe.itemMeta = normalAxeMeta

        return item.type == normalAxe.type && item.itemMeta.isUnbreakable == normalAxe.itemMeta.isUnbreakable
    }

    private fun isGlassBorder(entity: Entity?): Boolean {
        // Check if the entity is a BlockDisplay with a red glass block
        return entity is BlockDisplay && entity.block.placementMaterial == Material.RED_STAINED_GLASS
    }

    private fun giveChunkToPlayer(player: Player, chunk: Chunk) {
        giveChunkToPlayer(player, chunk.chunkKey.toString())
        claimSurroundedChunks(player, chunk)
    }

    private fun claimSurroundedChunks(player: Player, chunk: Chunk) {
        val x = chunk.x
        val z = chunk.z
        val world = chunk.world
        val playerChunks = HashSet<Chunk>()

        for (i in -1..1) {
            for (j in -1..1) {
                if (i != 0 && j != 0) continue
                val adjacent = world.getChunkAt(x + i, z + j)

                val owner = findChunkOwner(adjacent.chunkKey.toString())
                if (owner == player.name) {
                    playerChunks.add(adjacent)
                }
            }
        }

        if (playerChunks.size >= 2 && isConnected(playerChunks)) {
            broadcastMessage("§a${player.name}님이 땅을 차지했습니다!")

            val surroundedChunks = getSurroundedChunks(playerChunks)
            for (surroundedChunk in surroundedChunks) {
                if (findChunkOwner(surroundedChunk.chunkKey.toString()) == null) {
                    giveChunkToPlayer(player, surroundedChunk.chunkKey.toString())
                }
            }
        }
    }

    private fun getSurroundedChunks(playerChunks: HashSet<Chunk>): HashSet<Chunk> {
        val surroundedChunks = HashSet<Chunk>()

        for (chunk in playerChunks) {
            val x = chunk.x
            val z = chunk.z
            val world = chunk.world

            for (i in -1..1) {
                for (j in -1..1) {
                    if (i == 0 && j == 0) continue
                    if (i != 0 && j != 0) continue

                    val surroundingChunk = world.getChunkAt(x + i, z + j)
                    if (!playerChunks.contains(surroundingChunk)) {
                        surroundedChunks.add(surroundingChunk)
                    }
                }
            }
        }

        return surroundedChunks
    }

    private fun isConnected(playerChunks: HashSet<Chunk>): Boolean {
        if (playerChunks.isEmpty()) return false

        val start = playerChunks.first()
        val queue = LinkedList<Chunk>()
        val visited = HashSet<Chunk>()

        queue.add(start)
        visited.add(start)

        while (queue.isNotEmpty()) {
            val current = queue.poll()
            val x = current.x
            val z = current.z
            val world = current.world

            for (i in -1..1) {
                for (j in -1..1) {
                    if (i == 0 && j == 0) continue
                    val adjacent = world.getChunkAt(x + i, z + j)

                    if (playerChunks.contains(adjacent) && !visited.contains(adjacent)) {
                        queue.add(adjacent)
                        visited.add(adjacent)
                    }
                }
            }
        }

        return visited.size == playerChunks.size
    }




    private fun giveChunkToPlayer(player: Player, chunk: String) {
        val currentWorld = player.world
        // Get the corresponding world
        val correspondingWorld = if (currentWorld.environment == World.Environment.NETHER) {
            Bukkit.getWorld(currentWorld.name.replace("_nether", ""))
        } else {
            Bukkit.getWorld(currentWorld.name + "_nether")
        }
        // Get the corresponding chunk in the corresponding world
        val correspondingChunk = correspondingWorld?.getChunkAt(player.location.chunk.x, player.location.chunk.z)?.chunkKey.toString()

        // Insert the chunks into the database asynchronously
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            var statement: PreparedStatement? = null
            try {
                // Insert the first chunk
                statement = plugin.connection!!.prepareStatement("INSERT INTO chunks (owner, chunk) VALUES (?, ?)")
                statement.setString(1, player.name)
                statement.setString(2, chunk)
                statement.executeUpdate()
                statement.close()

                // Insert the corresponding chunk
                statement = plugin.connection!!.prepareStatement("INSERT INTO chunks (owner, chunk) VALUES (?, ?)")
                statement.setString(1, player.name)
                statement.setString(2, correspondingChunk)
                statement.executeUpdate()
            } catch (e: SQLException) {
                // Handle the error
            } finally {
                // Close the statement and the connection
                statement?.close()
            }
            MapCommand.MapUtils.update(plugin)
        })
    }
}