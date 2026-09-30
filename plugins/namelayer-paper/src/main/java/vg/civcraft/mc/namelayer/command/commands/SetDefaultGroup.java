package vg.civcraft.mc.namelayer.command.commands;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Syntax;
import java.util.UUID;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.namelayer.GroupManager.PlayerType;
import vg.civcraft.mc.namelayer.NameLayerAPI;
import vg.civcraft.mc.namelayer.command.BaseCommandMiddle;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.misc.NLText;

public class SetDefaultGroup extends BaseCommandMiddle {

    @CommandAlias("nlsdg")
    @Syntax("<group>")
    @Description("Set or change a default group")
    @CommandCompletion("@NL_Groups")
    public void execute(CommandSender sender, String groupName) {
        if (!(sender instanceof Player)) {
            CivUI.error(sender, "Only players have a default group");
            return;
        }
        Player p = (Player) sender;
        UUID uuid = NameLayerAPI.getUUID(p.getName());
        Group g = gm.getGroup(groupName);
        if (groupIsNull(sender, groupName, g)) {
            return;
        }

        PlayerType pType = g.getPlayerType(uuid);
        if (pType == null) {
            CivUI.error(p, "You're not on ", NLText.group(g));
            return;
        }

        String x = gm.getDefaultGroup(uuid);
        if (x == null) {
            g.setDefaultGroup(uuid);
            CivUI.success(p, "Your default group is now ", NLText.group(g));
        } else {
            g.changeDefaultGroup(uuid);
            CivUI.success(p, "Changed your default group from ", NLText.group(x), " to ", NLText.group(gm.getDefaultGroup(uuid)));
        }
    }
}
