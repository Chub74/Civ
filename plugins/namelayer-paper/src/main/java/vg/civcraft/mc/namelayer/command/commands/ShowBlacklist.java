package vg.civcraft.mc.namelayer.command.commands;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Syntax;
import java.util.Set;
import java.util.UUID;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.namelayer.NameLayerAPI;
import vg.civcraft.mc.namelayer.NameLayerPlugin;
import vg.civcraft.mc.namelayer.command.BaseCommandMiddle;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.misc.NLText;
import vg.civcraft.mc.namelayer.permission.PermissionType;

public class ShowBlacklist extends BaseCommandMiddle {

    @CommandAlias("nlsbl|showblacklist")
    @Syntax("<group>")
    @Description("Shows all blacklisted players for a specific group")
    @CommandCompletion("@NL_Groups")
    public void execute(CommandSender sender, String groupName) {
        if (!(sender instanceof Player)) {
            CivUI.error(sender, "Only players can see a blacklist");
            return;
        }
        Player p = (Player) sender;
        Group g = gm.getGroup(groupName);
        if (g == null) {
            CivUI.error(p, "There's no group called ", NLText.group(groupName));
            return;
        }
        if (!gm.hasAccess(g, p.getUniqueId(),
            PermissionType.getPermission("BLACKLIST"))
            && !(p.isOp() || p.hasPermission("namelayer.admin"))) {
            CivUI.error(p, "You don't have permission to see the blacklist of ", NLText.group(g));
            return;
        }
        Set<UUID> ids = NameLayerPlugin.getBlackList().getBlacklist(g);
        if (ids.size() == 0) {
            CivUI.info(p, "Nobody is blacklisted on ", NLText.group(g));
            return;
        }
        CivUI.info(p, "Blacklisted on ", NLText.group(g), ": ", NLText.players(ids));
    }
}
