package vg.civcraft.mc.namelayer.command.commands;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Syntax;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.namelayer.GroupManager;
import vg.civcraft.mc.namelayer.NameLayerAPI;
import vg.civcraft.mc.namelayer.NameLayerPlugin;
import vg.civcraft.mc.namelayer.command.BaseCommandMiddle;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.misc.NLText;
import vg.civcraft.mc.namelayer.permission.PermissionType;

public class MergeGroups extends BaseCommandMiddle {

    @CommandAlias("nlmg|merge|mergegroups")
    @Syntax("<group_to_keep> <group_to_delete>")
    @Description("Merge two groups together.")
    @CommandCompletion("@NL_Groups @NL_Groups")
    public void execute(CommandSender sender, String groupToKeep, String groupToDelete) {
        if (!(sender instanceof Player)) {
            CivUI.error(sender, "Only players can merge groups");
            return;
        }
        final Player p = (Player) sender;
        final Group g = GroupManager.getGroup(groupToKeep);
        if (groupIsNull(sender, groupToKeep, g)) {
            return;
        }

        final Group toMerge = GroupManager.getGroup(groupToDelete);
        if (groupIsNull(sender, groupToDelete, toMerge)) {
            return;
        }

        if (g.isDisciplined() || toMerge.isDisciplined()) {
            CivUI.error(p, NLText.group(g.isDisciplined() ? g : toMerge), " is disciplined");
            return;
        }

        if (g == toMerge) {
            CivUI.error(p, "You can't merge a group into itself");
            return;
        }

        UUID uuid = NameLayerAPI.getUUID(p.getName());
        if (!gm.hasAccess(g, uuid, PermissionType.getPermission("MERGE"))) {
            CivUI.error(p, "You don't have permission to merge ", NLText.group(g));
            return;
        }
        if (!gm.hasAccess(toMerge, uuid, PermissionType.getPermission("MERGE"))) {
            CivUI.error(p, "You don't have permission to merge ", NLText.group(toMerge));
            return;
        }
        try {
            gm.mergeGroup(g, toMerge);
            CivUI.success(p, "Merged the group ", NLText.group(toMerge), " into ", NLText.group(g));
        } catch (Exception e) {
            NameLayerPlugin.getInstance().getLogger().log(Level.SEVERE, "Group merging failed", e);
            CivUI.error(p, "Merging the group ", NLText.group(toMerge), " into ", NLText.group(g), " may have failed. Ask an admin to check");
        }
    }
}
