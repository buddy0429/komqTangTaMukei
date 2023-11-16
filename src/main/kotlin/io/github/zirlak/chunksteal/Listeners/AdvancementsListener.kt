package io.github.zirlak.chunksteal.Listeners

import me.croabeast.advancementinfo.AdvancementInfo
import net.kyori.adventure.text.Component
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerAdvancementDoneEvent
import org.bukkit.advancement.Advancement
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin

class AdvancementsListener(private val plugin: Plugin) : Listener {

    fun colorize(input: String): String {
        return ChatColor.translateAlternateColorCodes('&', input)
    }

    fun sendToPlayer(player: Player, vararg lines: String) {
        for (line in lines) {
            if (line != null) player.sendMessage(colorize(line))
        }
    }

    @EventHandler
    fun onPlayerAdvancement(event: PlayerAdvancementDoneEvent) {
        val adv: Advancement = event.advancement
        val player: Player = event.player

        val info = AdvancementInfo(adv)

        var title: String? = info.title
        if (title == null) title = ""

        val description: String? = info.description
        val frame = info.frame.toString()

        val Announced = info.isAnnouncedToChat

        if (frame != "challenge" && Announced) {
            val NormalAxe = ItemStack(Material.IRON_AXE)
            val NormalAxeMeta = NormalAxe.itemMeta
            NormalAxeMeta.displayName(Component.text("§a[일반 땅]"))
            NormalAxeMeta.setCustomModelData(1)
            val key = NamespacedKey(plugin, "normal")
            NormalAxeMeta.persistentDataContainer.set(key, PersistentDataType.STRING, "normal")
            NormalAxeMeta.isUnbreakable = true
            NormalAxe.itemMeta = NormalAxeMeta

            if (player.inventory.firstEmpty() == -1) {
                player.world.dropItem(player.location, NormalAxe)
            } else player.inventory.addItem(NormalAxe)

            sendToPlayer(player,
                "&7",
                "&a &lClear!",
                "&7 &6&nAdvancement: &e$title",
                "&8  • &7Description: &f$description",
                "&7"
            )
        }
    }
}