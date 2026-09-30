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
import vg.civcraft.mc.namelayer.GroupManager;
import vg.civcraft.mc.namelayer.NameLayerAPI;
import vg.civcraft.mc.namelayer.NameLayerPlugin;
import vg.civcraft.mc.namelayer.command.BaseCommandMiddle;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.misc.NLText;

public class TransferGroup extends BaseCommandMiddle {

    @CommandAlias("nltg|transfer|transfergroup")
    @Syntax("<group> <player>")
    @Description("Transfer one group to another person.")
    @CommandCompletion("@NL_Groups @allplayers")
    public void execute(CommandSender sender, String groupName, String playerName) {
        if (!(sender instanceof Player)) {
            CivUI.error(sender, "Only players can transfer groups");
            return;
        }
        Player p = (Player) sender;
        Group g = GroupManager.getGroup(groupName);
        if (groupIsNull(sender, groupName, g)) {
            return;
        }

        UUID oPlayer = NameLayerAPI.getUUID(playerName); // uuid of the second player

        if (oPlayer == null) {
            CivUI.error(p, CivText.subject(playerName), " has never played here");
            return;
        }

        attemptTransfer(g, p, oPlayer);
    }

    public static boolean attemptTransfer(Group g, Player owner, UUID futureOwner) {
        GroupManager gm = NameLayerAPI.getGroupManager();
        if (!g.isOwner(owner.getUniqueId()) && !(owner.isOp() || owner.hasPermission("namelayer.admin"))) {
            CivUI.error(owner, "You don't own ", NLText.group(g));
            return false;
        }
        if (g.isDisciplined()) {
            CivUI.error(owner, NLText.group(g), " is disciplined");
            return false;
        }
        if (NameLayerPlugin.getInstance().getGroupLimit() < gm
            .countGroups(futureOwner) + 1) {
            CivUI.error(owner, NLText.player(futureOwner), " is already at the group limit");
            return false;
        }
        if (!g.isMember(futureOwner)) {
            CivUI.error(owner, NLText.player(futureOwner), " isn't on ", NLText.group(g), ", so they can't own it");
            return false;
        }
        g.removeMember(futureOwner);
        g.addMember(futureOwner, GroupManager.PlayerType.OWNER);
        g.setOwner(futureOwner);
        CivUI.success(owner, "Transferred the group ", NLText.group(g), " to ", NLText.player(futureOwner));
        return true;
    }
}
