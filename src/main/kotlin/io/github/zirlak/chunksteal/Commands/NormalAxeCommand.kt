package io.github.zirlak.chunksteal.Commands

import io.github.zirlak.chunksteal.ChunkSteal
import io.github.zirlak.chunksteal.Listeners.AdvancementsListener.PlayerStatus.playerStatusFalse
import io.github.zirlak.chunksteal.Listeners.AdvancementsListener.PlayerStatus.playerStatusReturn
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import java.util.ArrayList

class NormalAxeCommand(private val plugin: ChunkSteal) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) return false
        val playerStatus = playerStatusReturn()
        if (playerStatus[sender.name] == true) {
            // Get the number from the command arguments or use 64 as default
            val amount = if (args.isNotEmpty() && args[0].toIntOrNull() != null) args[0].toInt() else 1

            val normalAxe = ItemStack(Material.IRON_AXE)
            val normalAxeMeta = normalAxe.itemMeta
            normalAxeMeta.displayName(Component.text("§a[일반 땅]"))
            normalAxeMeta.setCustomModelData(1)
            val key = NamespacedKey(plugin, "normal")
            normalAxeMeta.persistentDataContainer.set(key, PersistentDataType.STRING, "normal")
            normalAxeMeta.isUnbreakable = true
            normalAxe.itemMeta = normalAxeMeta

            //amount 만큼 반복
            for (i in 1..amount) {
                if (sender.inventory.firstEmpty() == -1) {
                    sender.world.dropItem(sender.location, normalAxe)
                } else sender.inventory.addItem(normalAxe)
            }

            playerStatusFalse(sender.name)
            sender.sendMessage("§aYou got $amount normal axe!")
            return true
        } else {
            sender.sendMessage("§4You've already been rewarded.")
            return false
        }
    }
}