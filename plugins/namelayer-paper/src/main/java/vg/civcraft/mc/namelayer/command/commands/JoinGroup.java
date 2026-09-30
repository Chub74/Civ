package vg.civcraft.mc.namelayer.command.commands;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Syntax;
import java.util.UUID;
import org.bukkit.entity.Player;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.namelayer.GroupManager.PlayerType;
import vg.civcraft.mc.namelayer.NameLayerAPI;
import vg.civcraft.mc.namelayer.NameLayerPlugin;
import vg.civcraft.mc.namelayer.command.BaseCommandMiddle;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.misc.NLText;
import vg.civcraft.mc.namelayer.permission.GroupPermission;
import vg.civcraft.mc.namelayer.permission.PermissionType;

public class JoinGroup extends BaseCommandMiddle {

    @CommandAlias("nljg|join|joingroup")
    @Syntax("<group> [password]")
    @Description("Join a password protected group.")
    public void execute(Player sender, String groupName, String attemptedPassword) {
        Player p = (Player) sender;
        Group g = gm.getGroup(groupName);
        if (groupIsNull(sender, groupName, g)) {
            return;
        }
        if (g.isDisciplined()) {
            CivUI.error(p, NLText.group(g), " is disciplined");
            return;
        }
        if (g.getPassword() == null) {
            CivUI.error(p, NLText.group(g), " has no password, so you can't join it that way");
            return;
        }
        if (!g.getPassword().equals(attemptedPassword)) {
            CivUI.error(p, "That password is wrong");
            return;
        }
        UUID uuid = NameLayerAPI.getUUID(p.getName());
        GroupPermission groupPerm = gm.getPermissionforGroup(g);
        PlayerType pType = groupPerm.getFirstWithPerm(PermissionType.getPermission("JOIN_PASSWORD"));
        if (pType == null) {
            CivUI.error(p, NLText.group(g), " doesn't let anyone join with its password");
            return;
        }
        if (g.isCurrentMember(uuid)) {
            CivUI.error(p, "You're already a member of ", NLText.group(g));
            return;
        }
        if (NameLayerPlugin.getBlackList().isBlacklisted(g, uuid)) {
            CivUI.error(p, "You're blacklisted from ", NLText.group(g));
            return;
        }

        g.addMember(uuid, pType);
        CivUI.success(p, "Joined the group ", NLText.group(g), " as ", NLText.rank(pType));
    }
}
