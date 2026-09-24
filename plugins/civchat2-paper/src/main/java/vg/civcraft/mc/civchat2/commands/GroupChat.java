package vg.civcraft.mc.civchat2.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Syntax;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import vg.civcraft.mc.civchat2.CivChat2;
import vg.civcraft.mc.civchat2.CivChat2Manager;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.namelayer.GroupManager;
import vg.civcraft.mc.namelayer.NameLayerAPI;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.permission.PermissionType;

public class GroupChat extends BaseCommand {

    @CommandAlias("groupc|groupchat|gchat|g|gc")
    @Syntax("[group] [message]")
    @Description("Enters a group chat or sends a message to a group chat")
    @CommandCompletion("@CC_Groups @nothing")
    public void execute(Player player, @Optional String targetGroup, @Optional String chatMessage) {
        CivChat2Manager chatMan = CivChat2.getInstance().getCivChat2Manager();
        boolean isGroupChatting = chatMan.getGroupChatting(player) != null;
        Group group;
        if (targetGroup == null && chatMessage == null) {
            // Check if player is in groupchat and move them to normal chat
            if (isGroupChatting) {
                CivUI.success(player, "You're now in local chat");
                chatMan.removeGroupChat(player);
                return;
            }
        }
        if (targetGroup == null) {
            return;
        }
        group = GroupManager.getGroup(targetGroup);
        if (group == null) {
            CivUI.error(player, "There's no group with that name");
            return;
        }
        if (!NameLayerAPI.getGroupManager().hasAccess(group, player.getUniqueId(),
            PermissionType.getPermission("WRITE_CHAT"))) {
            CivUI.error(player, "You don't have permission to chat in ", CivText.group(group.getName()));
            return;
        }
        if (CivChat2.getInstance().getDatabaseManager().isIgnoringGroup(player.getUniqueId(), group.getName())) {
            CivUI.error(player, "You need to unignore ", CivText.group(group.getName()));
            return;
        }
        if (chatMessage == null) {
            if (isGroupChatting) {
                // Player already groupchatting check if it's this group
                Group curGroup = chatMan.getGroupChatting(player);
                if (curGroup == group) {
                    CivUI.info(player, "You're already chatting in ", CivText.group(group.getName()));
                } else {
                    CivUI.success(player, "You're now chatting in ", CivText.group(group.getName()));
                    chatMan.removeGroupChat(player);
                    chatMan.addGroupChat(player, group);
                }
            } else {
                CivUI.success(player, "You're now chatting in ", CivText.group(group.getName()));
                if (chatMan.getChannel(player) != null) {
                    chatMan.removeChannel(player);
                }
                chatMan.addGroupChat(player, group);
            }
        } else {
            if (isGroupChatting) {
                // Player already groupchatting check if it's this group
                Group curGroup = chatMan.getGroupChatting(player);
                if (curGroup == group) {
                    chatMan.sendGroupMsg(player, group, Component.text(chatMessage));
                } else {
                    chatMan.sendGroupMsg(player, group, Component.text(chatMessage));
                }
            } else {
                if (chatMan.getChannel(player) != null) {
                    chatMan.removeChannel(player);
                }
                chatMan.sendGroupMsg(player, group, Component.text(chatMessage));
            }
        }
    }
}
