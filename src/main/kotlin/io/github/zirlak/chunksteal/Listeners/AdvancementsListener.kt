package io.github.zirlak.chunksteal.Listeners

import me.croabeast.advancementinfo.AdvancementInfo
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.event.ClickEvent
import org.bukkit.Bukkit
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
            val normalAxe = ItemStack(Material.IRON_AXE)
            val normalAxeMeta = normalAxe.itemMeta
            normalAxeMeta.displayName(Component.text("§a[일반 땅]"))
            normalAxeMeta.setCustomModelData(1)
            val key = NamespacedKey(plugin, "normal")
            normalAxeMeta.persistentDataContainer.set(key, PersistentDataType.STRING, "normal")
            normalAxeMeta.isUnbreakable = true
            normalAxe.itemMeta = normalAxeMeta

            if (player.inventory.firstEmpty() == -1) {
                player.world.dropItem(player.location, normalAxe)
            } else player.inventory.addItem(normalAxe)

            sendToPlayer(player,
                "&7",
                "&a &lClear!",
                "&7 &6&nAdvancement: &e$title",
                "&8  • &7Description: &f$description",
                "&7"
            )
        } else if (frame == "challenge" && Announced) {
            sendToPlayer(
                player,
                "&7",
                "&a &5Clear!",
                "&7 &6&nAdvancement: &e$title",
                "&8  • &7Description: &f$description",
                "&7"
            )
            sendTellraw(player)
        }
    }


    fun sendTellraw(player: Player) {
        // 플레이어에게 tellraw 메시지를 전송하는 함수
        val message1 = "${ChatColor.GREEN}[일반 땅 X 5]ﾠﾠﾠﾠﾠﾠﾠﾠﾠ${ChatColor.RESET}" // 메시지 내용
        val message2 = "${ChatColor.DARK_PURPLE}[특수 땅]${ChatColor.RESET}"
        val command1 = "/normalaxe 5" // 클릭하면 실행할 명령어 1
        val command2 = "/specialaxe 1" // 클릭하면 실행할 명령어 2
        val json = "[\"\",{\"text\":\"$message1\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"$command1\"}},{\"text\":\"$message2\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"$command2\"}}]" // 메시지를 JSON 형식으로 변환
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw ${player.name} $json") // 콘솔에서 tellraw 명령어 실행
    }
}