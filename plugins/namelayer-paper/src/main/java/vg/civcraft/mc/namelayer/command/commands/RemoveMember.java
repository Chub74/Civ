package vg.civcraft.mc.namelayer.command.commands;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Syntax;
import java.util.UUID;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.namelayer.GroupManager.PlayerType;
import vg.civcraft.mc.namelayer.NameLayerAPI;
import vg.civcraft.mc.namelayer.command.BaseCommandMiddle;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.misc.NLText;
import vg.civcraft.mc.namelayer.permission.PermissionType;

public class RemoveMember extends BaseCommandMiddle {

    @CommandAlias("nlrm|remove|removeplayer")
    @Syntax("<group> <player>")
    @Description("Remove a member from a group.")
    @CommandCompletion("@NL_Groups @allplayers")
    public void execute(CommandSender sender, String groupName, String targetPlayer) {
        if (!(sender instanceof Player)) {
            CivUI.error(sender, "Only players can remove members");
            return;
        }
        Player p = (Player) sender;
        Group group = gm.getGroup(groupName);
        if (groupIsNull(sender, groupName, group)) {
            return;
        }
        if (group.isDisciplined()) {
            CivUI.error(p, NLText.group(group), " is disciplined");
            return;
        }
        UUID executor = NameLayerAPI.getUUID(p.getName());
        UUID uuid = NameLayerAPI.getUUID(targetPlayer);

        if (uuid == null) {
            CivUI.error(p, CivText.subject(targetPlayer), " has never played here");
            return;
        }

        String playerName = NameLayerAPI.getCurrentName(uuid);
        PlayerType toBeRemoved = group.getPlayerType(uuid);
        if (toBeRemoved == null) {
            //hides who is actually on the group
            toBeRemoved = PlayerType.MEMBERS;
        }
        boolean allowed = false;
        switch (toBeRemoved) { // depending on the type the executor wants to add the player to
            case MEMBERS:
                allowed = gm.hasAccess(group, executor, PermissionType.getPermission("MEMBERS"));
                break;
            case MODS:
                allowed = gm.hasAccess(group, executor, PermissionType.getPermission("MODS"));
                break;
            case ADMINS:
                allowed = gm.hasAccess(group, executor, PermissionType.getPermission("ADMINS"));
                break;
            case OWNER:
                allowed = gm.hasAccess(group, executor, PermissionType.getPermission("OWNER"));
                break;
        }

        if (!allowed && !(p.isOp() || p.hasPermission("namelayer.admin"))) {
            CivUI.error(p, "You don't have permission to remove ", NLText.rank(toBeRemoved), " from ", NLText.group(group));
            return;
        }

        if (!group.isMember(uuid)) {
            CivUI.error(p, NLText.player(uuid), " isn't on ", NLText.group(group));
            return;
        }

        if (group.isOwner(uuid)) {
            CivUI.error(p, NLText.player(uuid), " owns ", NLText.group(group), ", so you can't remove them");
            return;
        }

        CivUI.success(p, "Removed ", CivText.subject(playerName), " from ", NLText.group(group));
        group.removeMember(uuid);
    }
}
