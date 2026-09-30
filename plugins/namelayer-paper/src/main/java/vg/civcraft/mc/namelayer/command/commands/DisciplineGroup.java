package vg.civcraft.mc.namelayer.command.commands;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Syntax;
import org.bukkit.entity.Player;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.namelayer.command.BaseCommandMiddle;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.misc.NLText;

public class DisciplineGroup extends BaseCommandMiddle {

    @CommandAlias("nldig|disablegroup|disable|discipline")
    @CommandPermission("namelayer.admin")
    @Syntax("<group>")
    @Description("Disable a group from working.")
    @CommandCompletion("@NL_Groups")
    public void execute(Player sender, String groupName) {
        Player p = (Player) sender;
        Group g = gm.getGroup(groupName);
        if (groupIsNull(sender, groupName, g)) {
            return;
        }
        if (!p.isOp() || !p.hasPermission("namelayer.admin")) {
            CivUI.error(p, "You don't have permission to discipline groups");
            return;
        }
        if (g.isDisciplined()) {
            g.setDisciplined(false);
            CivUI.success(sender, "Re-enabled the group ", NLText.group(g));
        } else {
            g.setDisciplined(true);
            CivUI.success(sender, "Disciplined the group ", NLText.group(g));
        }
    }
}
