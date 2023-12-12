package io.github.zirlak.chunksteal.Listeners

import io.github.zirlak.chunksteal.ChunkSteal
import io.github.zirlak.chunksteal.Commands.MapCommand
import io.github.zirlak.chunksteal.Commands.MapCommand.MapUtils.getAllChunksOwnedByPlayer
import net.kyori.adventure.text.Component
import org.bukkit.*
import org.bukkit.Bukkit.broadcastMessage
import org.bukkit.entity.BlockDisplay
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.util.*


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

    @EventHandler
    fun onPlayerInteractGlass(event: PlayerInteractEvent) {
        val player: Player = event.player
        val item = event.player.inventory.itemInMainHand
        if (event.action == Action.RIGHT_CLICK_AIR || event.action == Action.RIGHT_CLICK_BLOCK) {
            val clickedEntity = player.getTargetEntity(3.0)
            if (isSpecialItem(item) && isGlassBorder(clickedEntity)) {
                val chunk = clickedEntity!!.location.chunk
                if (findChunkOwner(chunk.chunkKey.toString()) == null) {
                    giveChunkToPlayer(player, chunk)
                    player.inventory.removeItem(item)
                } else {
                    player.sendMessage("§4이미 누군가의 땅입니다!")
                }
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

    // Define a data structure to store the chunk ownership
    var gameBoard = Array(32) { IntArray(32) { 0 } }
    val directions = listOf(Pair(-1, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1)) // 위, 아래, 왼쪽, 오른쪽

    fun updateGameBoardWithOwnedChunks(player: Player, chunk: Chunk) {

        gameBoard = Array(34) { IntArray(34) { 0 } }

        // Get all chunks owned by the player
        val ownedChunks = getAllChunksOwnedByPlayer(player.name, plugin)

        val x = chunk.x + 17 // Adjust the x-coordinate so that 0 is at the center
        val z = chunk.z + 17 // Adjust the z-coordinate so that 0 is at the center
        if (x in 0 until 32 && z in 0 until 32) {
            gameBoard[x][z] = 1
        }
        for (chunk2 in ownedChunks) {
            val x2 = chunk2.x + 17 // Adjust the x-coordinate so that 0 is at the center
            val z2 = chunk2.z + 17 // Adjust the z-coordinate so that 0 is at the center
            if (x2 in 0 until 32 && z2 in 0 until 32) {
                gameBoard[x2][z2] = 1
            }
        }
    }

    fun dfs(x: Int, y: Int, visited: Array<BooleanArray>, group: MutableList<Pair<Int, Int>>) {
        val stack = Stack<Pair<Int, Int>>()
        stack.push(Pair(x, y))

        while (stack.isNotEmpty()) {
            val (currentX, currentY) = stack.pop()
            if (currentX >= 0 && currentY >= 0 && currentX < gameBoard.size && currentY < gameBoard[0].size && gameBoard[currentX][currentY] == 0 && !visited[currentX][currentY]) {
                visited[currentX][currentY] = true
                group.add(Pair(currentX, currentY))

                for (i in directions.indices) {
                    val newX = currentX + directions[i].first
                    val newY = currentY + directions[i].second
                    stack.push(Pair(newX, newY))
                }
            }
        }
    }

    fun isGroupSurroundedByOnes(group: List<Pair<Int, Int>>): Boolean {
        for (i in group.indices) {
            val (x, y) = group[i]
            for (j in directions.indices) {
                val newX = x + directions[j].first
                val newY = y + directions[j].second
                if (newX < 0 || newY < 0 || newX >= gameBoard.size || newY >= gameBoard[0].size || (gameBoard[newX][newY] != 1 && gameBoard[newX][newY] != 0)) {
                    return false
                }
            }
        }
        return true
    }

    fun findGroupsOfZerosSurroundedByOnes(player: Player, chunk: Chunk): List<List<Pair<Int, Int>>> {
        updateGameBoardWithOwnedChunks(player, chunk)
        val visited = Array(gameBoard.size) { BooleanArray(gameBoard[0].size) }
        val surroundedGroups = mutableListOf<List<Pair<Int, Int>>>()

        for (x in gameBoard.indices) {
            for (y in gameBoard[0].indices) {
                if (gameBoard[x][y] == 0 && !visited[x][y]) {
                    val group = mutableListOf<Pair<Int, Int>>()
                    dfs(x, y, visited, group)
                    if (isGroupSurroundedByOnes(group)) {
                        surroundedGroups.add(group)
                    }
                }
            }
        }
        return surroundedGroups
    }

    fun changeZerosToTwos(player: Player, chunk: Chunk) {
        val groups = findGroupsOfZerosSurroundedByOnes(player, chunk)
        for (group in groups) {
            for ((x, y) in group) {
                gameBoard[x][y] = 2
            }
        }
    }

    fun changeTwoToChunks(player: Player, chunk: Chunk) {
        changeZerosToTwos(player, chunk)
        for (x in gameBoard.indices) {
            for (y in gameBoard[0].indices) {
                if (gameBoard[x][y] == 2) {
                    val chunk = player.world.getChunkAt(x - 17, y - 17)
                    if (findChunkOwner(chunk.chunkKey.toString()) == null) {
                        setChunkOwner(player, chunk.chunkKey.toString())
                    }
                }
            }
        }
    }

    private fun giveChunkToPlayer(player: Player, chunk: Chunk) {
        setChunkOwner(player, chunk.chunkKey.toString()) //give the chunk to the player
        changeTwoToChunks(player, chunk)
    }
    private fun findChunkOwner(chunkKey: String): String? {
        var statement: PreparedStatement? = null
        var resultSet: ResultSet? = null
        return try {
            statement = plugin.connection!!.prepareStatement("SELECT owner FROM chunks WHERE chunk = ?")
            statement.setString(1, chunkKey)
            resultSet = statement.executeQuery()
            if (resultSet.next()) resultSet.getString("owner") else null
        } catch (e: SQLException) {
            // Handle the error
            null
        } finally {
            // Close the ResultSet and the PreparedStatement
            resultSet?.close()
            statement?.close()
        }
    }


    private fun setChunkOwner(player: Player, chunk: String) {
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
            MapCommand.MapUtils.updateChunk(plugin)
        })
    }
}