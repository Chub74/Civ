package vg.civcraft.mc.namelayer.command;

import co.aikar.commands.BaseCommand;
import org.bukkit.command.CommandSender;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.namelayer.GroupManager;
import vg.civcraft.mc.namelayer.NameLayerAPI;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.misc.NLText;

public abstract class BaseCommandMiddle extends BaseCommand {

    public BaseCommandMiddle() {
    }

    protected GroupManager gm = NameLayerAPI.getGroupManager();

    protected boolean groupIsNull(CommandSender sender, String groupname, Group group) {
        if (group == null) {
            CivUI.error(sender, "There's no group called ", NLText.group(groupname));
            return true;
        }
        return false;
    }
}
