package vg.civcraft.mc.civchat2.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Syntax;
import org.bukkit.entity.Player;
import vg.civcraft.mc.civchat2.CivChat2;
import vg.civcraft.mc.civchat2.CivChat2Manager;
import vg.civcraft.mc.civchat2.database.CivChatDAO;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.namelayer.GroupManager;
import vg.civcraft.mc.namelayer.group.Group;

public class IgnoreGroup extends BaseCommand {

    @CommandAlias("ignoregroup")
    @Syntax("<group>")
    @Description("Toggles ignoring a group")
    public void execute(Player player, String targetGroup) {
        Group group = GroupManager.getGroup(targetGroup);
        if (group == null) {
            CivUI.error(player, "There's no group with that name");
            return;
        }
        String ignore = group.getName();
        CivChatDAO db = CivChat2.getInstance().getDatabaseManager();
        if (!db.isIgnoringGroup(player.getUniqueId(), ignore)) {
            db.addIgnoredGroup(player.getUniqueId(), ignore);
            CivChat2Manager chatMan = CivChat2.getInstance().getCivChat2Manager();
            CivUI.success(player, "You're now ignoring ", CivText.group(ignore));
            if (group.equals(chatMan.getGroupChatting(player))) {
                chatMan.removeGroupChat(player);
                CivUI.success(player, "You're now in local chat");
            }
            return;
        } else {
            db.removeIgnoredGroup(player.getUniqueId(), ignore);
            CivUI.success(player, "You stopped ignoring ", CivText.group(ignore));
        }
    }
}
