package vg.civcraft.mc.namelayer.command.commands;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Syntax;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.namelayer.command.BaseCommandMiddle;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.gui.GUIGroupOverview;
import vg.civcraft.mc.namelayer.gui.MainGroupGUI;
import vg.civcraft.mc.namelayer.misc.NLText;
import vg.civcraft.mc.namelayer.permission.PermissionType;

public class NameLayerGroupGui extends BaseCommandMiddle {

    @CommandAlias("nl")
    @Syntax("[group]")
    @Description("Open the group management GUI")
    @CommandCompletion("@NL_Groups")
    public void execute(CommandSender sender, @Optional String groupName) {
        if (!(sender instanceof Player)) {
            CivUI.error(sender, "Only players can open the group menu");
            return;
        }
        if (groupName == null) {
            GUIGroupOverview gui = new GUIGroupOverview((Player) sender);
            gui.showScreen();
            return;
        }
        Group g = gm.getGroup(groupName);
        if (g == null) {
            CivUI.error(sender, "There's no group called ", NLText.group(groupName));
            return;
        }
        if (!gm.hasAccess(g, ((Player) sender).getUniqueId(), PermissionType.getPermission("OPEN_GUI"))) {
            CivUI.error(sender, "You don't have permission to open the menu for ", NLText.group(g));
            return;
        }
        MainGroupGUI gui = new MainGroupGUI((Player) sender, g);
    }
}
