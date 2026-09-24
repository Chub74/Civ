package vg.civcraft.mc.civchat2.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
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

public class GlobalChat extends BaseCommand {

    @CommandAlias("global|globalchat")
    @Syntax("[message]")
    @Description("Enters global group chat or sends a message to global group chat")
    public void execute(Player player, @Optional String chatMessage) {
        String globalGroupName = CivChat2.getInstance().getPluginConfig().getGlobalChatGroupName();
        if (globalGroupName == null) {
            return;
        }
        Group globalGroup = GroupManager.getGroup(globalGroupName);
        CivChat2Manager chatMan = CivChat2.getInstance().getCivChat2Manager();
        Group currentGroup = chatMan.getGroupChatting(player);

        if (globalGroup == null) {
            CivUI.error(player, "There's no group with that name");
            return;
        }
        if (!NameLayerAPI.getGroupManager().hasAccess(globalGroup, player.getUniqueId(),
            PermissionType.getPermission("WRITE_CHAT"))) {
            CivUI.error(player, "You don't have permission to chat in ", CivText.group(globalGroupName));
            return;
        }
        if (CivChat2.getInstance().getDatabaseManager().isIgnoringGroup(player.getUniqueId(), globalGroupName)) {
            CivUI.error(player, "You need to unignore ", CivText.group(globalGroupName));
            return;
        }
        if (chatMessage == null) {
            if (currentGroup != null) { // Check if currently in group
                if (currentGroup == globalGroup) { // Check if current group global
                    CivUI.info(player, "You're already chatting in ", CivText.group(globalGroupName));
                } else { // Switch to global group from current group
                    CivUI.success(player, "You're now chatting in ", CivText.group(globalGroupName));
                    chatMan.removeGroupChat(player);
                    chatMan.addGroupChat(player, globalGroup);
                }
            } else { // Switch to global group
                CivUI.success(player, "You're now chatting in ", CivText.group(globalGroupName));
                if (chatMan.getChannel(player) != null) {
                    chatMan.removeChannel(player);
                }
                chatMan.addGroupChat(player, globalGroup);
            }
        } else { // Send message to global group
            if (currentGroup != null) {
                chatMan.sendGroupMsg(player, globalGroup, Component.text(chatMessage));
            } else {
                if (chatMan.getChannel(player) != null) {
                    chatMan.removeChannel(player);
                }
                chatMan.sendGroupMsg(player, globalGroup, Component.text(chatMessage));
            }
        }
    }
}
