package vg.civcraft.mc.civchat2.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Syntax;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import vg.civcraft.mc.civchat2.CivChat2;
import vg.civcraft.mc.civchat2.CivChat2Manager;
import vg.civcraft.mc.civchat2.database.CivChatDAO;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;


public class Tell extends BaseCommand {

    @CommandAlias("tell|message|m|pm|msg")
    @Syntax("[player] [message]")
    @Description("Sends a private message to someone or enters a private chat with them")
    @CommandCompletion("@allplayers @nothing")
    public void execute(Player player, @Optional String targetPlayer, @Optional String chatMessage) {
        CivChat2Manager chatMan = CivChat2.getInstance().getCivChat2Manager();
        if (targetPlayer == null && chatMessage == null) {
            UUID chattingWith = chatMan.getChannel(player);
            if (chattingWith != null) {
                chatMan.removeChannel(player);
                CivUI.success(player, "You left private chat");
            } else {
                CivUI.error(player, "You aren't in private chat");
            }
            return;
        }

        Player receiver = Bukkit.getPlayer(targetPlayer);
        if (receiver == null) {
            CivUI.error(player, "No player exists with that name");
            return;
        }

        if (!(receiver.isOnline())) {
            CivUI.error(player, "That player is offline");
            return;
        }

        if (player == receiver) {
            CivUI.error(player, "You can't message yourself");
            return;
        }

        if (!(chatMessage == null)) {
            chatMan.sendPrivateMsg(player, receiver, Component.text(chatMessage));
            return;
        } else {
            CivChatDAO db = CivChat2.getInstance().getDatabaseManager();
            if (db.isIgnoringPlayer(player.getUniqueId(), receiver.getUniqueId())) {
                CivUI.error(player, "You need to unignore ", CivText.subject(receiver.getName()));
                return;
            }

            if (db.isIgnoringPlayer(receiver.getUniqueId(), player.getUniqueId())) {
                CivUI.error(player, "That player is ignoring you");
                return;
            }
            chatMan.addChatChannel(player, receiver);
            CivUI.success(player, "You're now chatting with ", CivText.subject(receiver.getName()));
        }
    }
}
