package io.github.zirlak.chunksteal.Listeners

import io.github.zirlak.chunksteal.ChunkSteal
import io.github.zirlak.chunksteal.Commands.MapCommand.MapUtils.updatePlayer
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.map.MapCanvas
import org.bukkit.map.MapCursor
import org.bukkit.map.MapRenderer
import org.bukkit.map.MapView
import org.bukkit.plugin.java.JavaPlugin

class MapListener : Listener {

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        updatePlayer(JavaPlugin.getPlugin(ChunkSteal::class.java))

    }

}