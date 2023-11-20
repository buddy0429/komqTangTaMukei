package io.github.zirlak.chunksteal.Commands

import io.github.zirlak.chunksteal.ChunkSteal
import io.github.zirlak.chunksteal.Listeners.AdvancementsListener
import io.github.zirlak.chunksteal.Listeners.AdvancementsListener.PlayerStatus.playerStatusFalse
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

class SpecialAxeCommand(private val plugin: ChunkSteal) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) return false
        val playerStatus = AdvancementsListener.PlayerStatus.playerStatusReturn()
        if (playerStatus[sender.name] == true) {

            // Get the number from the command arguments or use 64 as default
            val amount = if (args.isNotEmpty() && args[0].toIntOrNull() != null) args[0].toInt() else 1

            val specialAxe = ItemStack(Material.NETHERITE_AXE)
            val specialAxeMeta = specialAxe.itemMeta
            specialAxeMeta.displayName(Component.text("§5[특수 땅]"))
            specialAxeMeta.setCustomModelData(2)
            val key = NamespacedKey(plugin, "special")
            specialAxeMeta.persistentDataContainer.set(key, PersistentDataType.STRING, "special")
            specialAxeMeta.isUnbreakable = true
            specialAxe.itemMeta = specialAxeMeta

            for (i in 1..amount) {
                if (sender.inventory.firstEmpty() == -1) {
                    sender.world.dropItem(sender.location, specialAxe)
                } else sender.inventory.addItem(specialAxe)
            }

            playerStatusFalse(sender.name)
            sender.sendMessage("§5You got $amount special axe!")
            return true
        } else {
            sender.sendMessage("§4You've already been rewarded.")
            return false
        }
    }
}