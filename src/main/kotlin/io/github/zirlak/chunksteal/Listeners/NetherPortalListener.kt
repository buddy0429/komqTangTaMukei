package io.github.zirlak.chunksteal.Listeners

import org.bukkit.World
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerPortalEvent

class NetherPortalListener : Listener {

    @EventHandler
    fun onPlayerPortal(event: PlayerPortalEvent) {
        // If the player is travelling to the Nether or the Overworld
        if ((event.from.world.environment == World.Environment.NORMAL && event.to.world.environment == World.Environment.NETHER) || (event.from.world.environment == World.Environment.NETHER && event.to.world.environment == World.Environment.NORMAL)) {
            // Set the destination to the same coordinates in the other world
            event.to.x = event.from.x
            event.to.z = event.from.z
        }
    }
}