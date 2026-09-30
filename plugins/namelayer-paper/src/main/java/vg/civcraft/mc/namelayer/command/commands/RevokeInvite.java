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
import vg.civcraft.mc.namelayer.GroupManager;
import vg.civcraft.mc.namelayer.NameLayerAPI;
import vg.civcraft.mc.namelayer.command.BaseCommandMiddle;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.listeners.PlayerListener;
import vg.civcraft.mc.namelayer.misc.NLText;
import vg.civcraft.mc.namelayer.permission.PermissionType;

public class RevokeInvite extends BaseCommandMiddle {

    @CommandAlias("nlri|revokeinvite|revoke")
    @Syntax("<group> <player>")
    @Description("Revoke an Invite.")
    @CommandCompletion("@NL_Groups @allplayers")
    public void execute(CommandSender sender, String groupName, String targetPlayer) {
        if (!(sender instanceof Player)) {
            CivUI.error(sender, "Only players can revoke invites");
            return;
        }
        Player p = (Player) sender;
        Group group = GroupManager.getGroup(groupName);
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

        PlayerType t = group.getPlayerType(executor); // playertype for the player running the command.
        if (t == null) {
            CivUI.error(p, "You're not on ", NLText.group(group));
            return;
        }

        //get invitee PlayerType
        PlayerType pType = group.getInvite(uuid);
        if (pType == null) {
            CivUI.error(p, NLText.player(uuid), " isn't invited to ", NLText.group(group));
            return;
        }

        //check invitee has invite
        if (group.isMember(uuid)) {
            CivUI.error(p, NLText.player(uuid), " is already a member. Remove them with ",
                NLText.command("/nlrm " + group.getName() + " " + NameLayerAPI.getCurrentName(uuid)));
            return;
        }

        boolean allowed = switch (pType) { // depending on the type the executor wants to add the player to
            case MEMBERS -> gm.hasAccess(group, executor, PermissionType.getPermission("MEMBERS"));
            case MODS -> gm.hasAccess(group, executor, PermissionType.getPermission("MODS"));
            case ADMINS -> gm.hasAccess(group, executor, PermissionType.getPermission("ADMINS"));
            case OWNER -> gm.hasAccess(group, executor, PermissionType.getPermission("OWNER"));
            default -> false;
        };
        if (!allowed) {
            CivUI.error(p, "You don't have permission to revoke ", NLText.rank(pType), " invites on ", NLText.group(group));
            return;
        }

        group.removeInvite(uuid, true);
        PlayerListener.removeNotification(uuid, group);

        CivUI.success(p, "Revoked ", NLText.player(uuid), "'s invite to the group ", NLText.group(group));
    }
}
