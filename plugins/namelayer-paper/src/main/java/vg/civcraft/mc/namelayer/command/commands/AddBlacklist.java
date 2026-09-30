package vg.civcraft.mc.namelayer.command.commands;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Syntax;
import java.util.UUID;
import org.bukkit.entity.Player;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.namelayer.GroupManager;
import vg.civcraft.mc.namelayer.NameLayerAPI;
import vg.civcraft.mc.namelayer.NameLayerPlugin;
import vg.civcraft.mc.namelayer.command.BaseCommandMiddle;
import vg.civcraft.mc.namelayer.group.BlackList;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.misc.NLText;
import vg.civcraft.mc.namelayer.permission.PermissionType;

public class AddBlacklist extends BaseCommandMiddle {

    @CommandAlias("nlbl|blacklist|addblacklist")
    @Syntax("<group> <player>")
    @Description("Blacklist a player for a specific group")
    @CommandCompletion("@NL_Groups @allplayers")
    public void execute(Player sender, String groupName, String playerName) {
        Player p = (Player) sender;
        Group g = GroupManager.getGroup(groupName);
        if (g == null) {
            CivUI.error(p, "There's no group called ", NLText.group(groupName));
            return;
        }
        if (!gm.hasAccess(g, p.getUniqueId(),
            PermissionType.getPermission("BLACKLIST"))
            && !(p.isOp() || p.hasPermission("namelayer.admin"))) {
            CivUI.error(p, "You don't have permission to blacklist on ", NLText.group(g));
            return;
        }
        UUID targetUUID = NameLayerAPI.getUUID(playerName);
        if (targetUUID == null) {
            CivUI.error(p, "There's no player called ", CivText.subject(playerName));
            return;
        }
        if (g.isMember(targetUUID)) {
            CivUI.error(p, "You can't blacklist a member of ", NLText.group(g), ". Remove them first");
            return;
        }
        BlackList bl = NameLayerPlugin.getBlackList();
        if (bl.isBlacklisted(g, targetUUID)) {
            CivUI.error(p, NLText.player(targetUUID), " is already blacklisted on ", NLText.group(g));
            return;
        }
        bl.addBlacklistMember(g, targetUUID, true);
        CivUI.success(p, "Blacklisted ", NLText.player(targetUUID), " on ", NLText.group(g));
    }
}
