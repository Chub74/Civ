package vg.civcraft.mc.namelayer.command.commands;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Syntax;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.civmodcore.ui.Kind;
import vg.civcraft.mc.namelayer.GroupManager.PlayerType;
import vg.civcraft.mc.namelayer.GroupManager;
import vg.civcraft.mc.namelayer.NameLayerAPI;
import vg.civcraft.mc.namelayer.command.BaseCommandMiddle;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.misc.NLText;
import vg.civcraft.mc.namelayer.permission.GroupPermission;
import vg.civcraft.mc.namelayer.permission.PermissionType;

public class ModifyPermissions extends BaseCommandMiddle {

    @CommandAlias("nlmp|editperms|modifyperms")
    @Syntax("<group> <add/remove> <rank> <permission>")
    @Description("Modify the permissions of a group.")
    @CommandCompletion("@NL_Groups add|remove @NL_Ranks @NL_Perms")
    public void execute(CommandSender sender, String groupName, String adding, String playerRank, String permissionName) {
        if (!(sender instanceof Player)) {
            CivUI.error(sender, "Only players can change group permissions");
            return;
        }
        Player p = (Player) sender;
        Group g = GroupManager.getGroup(groupName);
        if (groupIsNull(sender, groupName, g)) {
            return;
        }
        UUID uuid = NameLayerAPI.getUUID(p.getName());
        PlayerType type = g.getPlayerType(uuid);
        if (type == null) {
            CivUI.error(p, "You're not on ", NLText.group(g));
            return;
        }
        if (g.isDisciplined()) {
            CivUI.error(p, NLText.group(g), " is disciplined");
            return;
        }
        if (!gm.hasAccess(g, uuid, PermissionType.getPermission("PERMS")) && !g.isOwner(uuid) && !(p.isOp() || p.hasPermission("namelayer.admin"))) {
            CivUI.error(p, "You don't have permission to change permissions on ", NLText.group(g));
            return;
        }
        String info = adding;
        PlayerType playerType = PlayerType.getPlayerType(playerRank.toUpperCase());
        if (playerType == null) {
            PlayerType.displayPlayerTypes(p);
            return;
        }
        PermissionType pType = PermissionType.getPermission(permissionName);
        if (pType == null) {
            CivUI.notice(Kind.ERROR, "There's no permission called ", CivText.value(permissionName))
                .hover(Component.text().append(CivText.info("Permissions: ")).append(NLText.permissions()).build())
                .to(p)
                .send();
            return;
        }
        GroupPermission gPerm = gm.getPermissionforGroup(g);

        if (playerType == PlayerType.NOT_BLACKLISTED && !pType.getCanBeBlacklisted()) {
            CivUI.error(sender, "You can't change ", CivText.value(pType.getName()), " for ", NLText.rank(playerType));
            return;
        }

        if (info.equalsIgnoreCase("add")) {
            if (gPerm.hasPermission(playerType, pType))
                CivUI.error(sender, NLText.rank(playerType), " already has ", CivText.value(pType.getName()));
            else {
                if (playerType == PlayerType.NOT_BLACKLISTED && pType == PermissionType.getPermission("JOIN_PASSWORD")) {
                    //we need to prevent players from explicitly adding people to this permission group
                    CivUI.error(sender, NLText.rank(playerType), " already covers everyone who isn't blacklisted");
                }
                gPerm.addPermission(playerType, pType);
                CivUI.success(sender, "Gave ", NLText.rank(playerType), " ", CivText.value(pType.getName()), " on ", NLText.group(g));
            }
        } else if (info.equalsIgnoreCase("remove")) {
            if (gPerm.hasPermission(playerType, pType)) {
                gPerm.removePermission(playerType, pType);
                CivUI.success(sender, "Took ", CivText.value(pType.getName()), " from ", NLText.rank(playerType), " on ", NLText.group(g));
            } else
                CivUI.error(sender, NLText.rank(playerType), " doesn't have ", CivText.value(pType.getName()));
        } else {
            CivUI.error(p, "Say whether to ", CivText.value("add"), " or ", CivText.value("remove"), " the permission");
        }
    }
}
