package vg.civcraft.mc.civchat2.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Syntax;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import vg.civcraft.mc.civchat2.CivChat2;
import vg.civcraft.mc.civchat2.CivChat2Manager;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;

public class Reply extends BaseCommand {

    @CommandAlias("reply|r")
    @Syntax("[message]")
    @Description("Replies to the last person you sent a message to or received one from")
    public void execute(Player player, @Optional String chatMessage) {
        CivChat2Manager chatMan = CivChat2.getInstance().getCivChat2Manager();
        UUID receiverUUID = chatMan.getPlayerReply(player);

        Player receiver = Bukkit.getPlayer(receiverUUID);
        if (receiver == null) {
            CivUI.error(player, "You have no one to reply to");
            return;
        }

        if (!(receiver.isOnline())) {
            CivUI.error(player, "That player is offline");
            return;
        }

        if (player.getUniqueId().equals(receiver.getUniqueId())) {
            CivUI.error(player, "You can't message yourself");
            return;
        }

        if (!(chatMessage == null)) {
            chatMan.sendPrivateMsg(player, receiver, Component.text(chatMessage));
            return;
        }
        // Player to chat with reply user
        chatMan.removeChannel(player);
        chatMan.addChatChannel(player, receiver);
        CivUI.success(player, "You're now chatting with ", CivText.subject(receiver.getName()));
    }
}
