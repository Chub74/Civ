package vg.civcraft.mc.namelayer.command.commands;

import co.aikar.commands.BukkitCommandCompletionContext;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Syntax;
import java.util.List;
import java.util.UUID;
import org.bukkit.entity.Player;
import vg.civcraft.mc.civmodcore.commands.TabComplete;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.namelayer.GroupManager;
import vg.civcraft.mc.namelayer.NameLayerAPI;
import vg.civcraft.mc.namelayer.command.BaseCommandMiddle;
import vg.civcraft.mc.namelayer.command.TabCompleters.InviteTabCompleter;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.listeners.PlayerListener;
import vg.civcraft.mc.namelayer.misc.NLText;

public class RejectInvite extends BaseCommandMiddle {

    @CommandAlias("nlrg|reject|rejectinvite")
    @Syntax("<group>")
    @Description("Reject an invitation to a group.")
    @CommandCompletion("@NL_Invites")
    public void execute(Player sender, String targetGroup) {
        Player player = (Player) sender;
        String groupName = targetGroup;
        Group group = GroupManager.getGroup(groupName);
        if (groupIsNull(sender, groupName, group)) {
            return;
        }
        UUID uuid = NameLayerAPI.getUUID(player.getName());
        // The IDE is highlighting this as a potention NullReferenceException
        // but this is checked for in the above groupIsNull() call.
        GroupManager.PlayerType type = group.getInvite(uuid);
        if (type == null) {
            CivUI.error(player, "You weren't invited to ", NLText.group(group));
            return;
        }
        if (group.isMember(uuid)) {
            CivUI.error(player, "You're already a member of ", NLText.group(group));
            group.removeInvite(uuid, true);
            return;
        }
        group.removeInvite(uuid, true);
        PlayerListener.removeNotification(uuid, group);
        CivUI.success(player, "Declined the invite to the group ", NLText.group(group));
    }

    @TabComplete("NL_Invites")
    public List<String> tabComplete(BukkitCommandCompletionContext context) {
        return InviteTabCompleter.complete(context.getInput(), context.getPlayer());
    }
}
