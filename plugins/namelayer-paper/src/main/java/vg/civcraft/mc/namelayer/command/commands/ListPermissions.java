package vg.civcraft.mc.namelayer.command.commands;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Syntax;
import java.util.UUID;
import org.bukkit.entity.Player;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.namelayer.GroupManager.PlayerType;
import vg.civcraft.mc.namelayer.NameLayerAPI;
import vg.civcraft.mc.namelayer.command.BaseCommandMiddle;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.misc.NLText;
import vg.civcraft.mc.namelayer.permission.GroupPermission;
import vg.civcraft.mc.namelayer.permission.PermissionType;

public class ListPermissions extends BaseCommandMiddle {

    @CommandAlias("nllp|listperms|perms|permissions")
    @Syntax("<group> [rank (e.g: MEMBERS)]")
    @Description("Show permissions for a PlayerType in a specific group.")
    @CommandCompletion("@NL_Groups @NL_Ranks")
    public void execute(Player sender, String groupName, @Optional String playerRank) {
        Player p = (Player) sender;
        Group g = gm.getGroup(groupName);
        if (groupIsNull(sender, groupName, g)) {
            return;
        }
        UUID uuid = NameLayerAPI.getUUID(p.getName());
        PlayerType playerType = g.getPlayerType(uuid);
        if (playerType == null) {
            CivUI.error(p, "You're not on ", NLText.group(g));
            return;
        }
        String perms = null;
        PlayerType shown = playerType;
        GroupPermission gPerm = gm.getPermissionforGroup(g);
        if (playerRank != null) {
            if (!gm.hasAccess(g, uuid, PermissionType.getPermission("LIST_PERMS"))) {
                CivUI.error(p, "You don't have permission to see other ranks' permissions on ", NLText.group(g));
                return;
            }
            PlayerType check = PlayerType.getPlayerType(playerRank);
            if (check == null) {
                PlayerType.displayPlayerTypes(p);
                return;
            }
            perms = gPerm.listPermsforPlayerType(check);
            shown = check;
        } else
            perms = gPerm.listPermsforPlayerType(playerType);

        CivUI.info(p, NLText.rank(shown), " on ", NLText.group(g), " can: ", CivText.value(perms.trim()));
    }
}
