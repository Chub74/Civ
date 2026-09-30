package vg.civcraft.mc.namelayer.command.commands;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Syntax;
import java.util.UUID;
import org.bukkit.entity.Player;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.namelayer.NameLayerAPI;
import vg.civcraft.mc.namelayer.command.BaseCommandMiddle;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.misc.NLText;

public class LeaveGroup extends BaseCommandMiddle {

    @CommandAlias("nlleg|leave|leavegroup")
    @Syntax("<group>")
    @Description("Leave a group")
    @CommandCompletion("@NL_Groups")
    public void execute(Player sender, String groupName) {
        Player p = (Player) sender;
        Group g = gm.getGroup(groupName);
        if (groupIsNull(sender, groupName, g)) {
            return;
        }
        UUID uuid = NameLayerAPI.getUUID(p.getName());
        if (!g.isCurrentMember(uuid)) {
            CivUI.error(p, "You're not a member of ", NLText.group(g));
            return;
        }
        if (g.isDisciplined()) {
            CivUI.error(p, NLText.group(g), " is disciplined");
            return;
        }
        g.removeMember(uuid);
        CivUI.success(p, "Left the group ", NLText.group(g));
    }
}
