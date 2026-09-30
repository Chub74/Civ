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
import vg.civcraft.mc.namelayer.GroupManager.PlayerType;
import vg.civcraft.mc.namelayer.NameLayerAPI;
import vg.civcraft.mc.namelayer.command.BaseCommandMiddle;
import vg.civcraft.mc.namelayer.command.TabCompleters.InviteTabCompleter;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.listeners.PlayerListener;
import vg.civcraft.mc.namelayer.misc.NLText;

public class AcceptInvite extends BaseCommandMiddle {

    @CommandAlias("nlacceptinvite|nlacceptgroup|acceptinvite|acceptgroup|accept|join|ctjoin|ctj|ag|nlag")
    @Syntax("<group>")
    @Description("Accept an invitation to a group.")
    @CommandCompletion("@NL_Invites")
    public void execute(Player sender, String groupName) {
        Player p = (Player) sender;
        Group group = gm.getGroup(groupName);
        if (groupIsNull(sender, groupName, group)) {
            return;
        }
        UUID uuid = NameLayerAPI.getUUID(p.getName());
        PlayerType type = group.getInvite(uuid);
        if (type == null) {
            CivUI.error(p, "You weren't invited to ", NLText.group(group));
            return;
        }
        if (group.isDisciplined()) {
            CivUI.error(p, NLText.group(group), " is disciplined");
            return;
        }
        if (group.isMember(uuid)) {
            CivUI.error(p, "You're already a member of ", NLText.group(group));
            group.removeInvite(uuid, true);
            return;
        }
        group.addMember(uuid, type);
        group.removeInvite(uuid, true);
        PlayerListener.removeNotification(uuid, group);
        CivUI.success(p, "Joined the group ", NLText.group(group), " as ", NLText.rank(type));
    }

    @TabComplete("NL_Invites")
    public List<String> tabComplete(BukkitCommandCompletionContext context) {
        return InviteTabCompleter.complete(context.getInput(), context.getPlayer());
    }
}
