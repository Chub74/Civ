package vg.civcraft.mc.namelayer.command.commands;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Description;
import java.util.List;
import java.util.UUID;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.namelayer.NameLayerAPI;
import vg.civcraft.mc.namelayer.command.BaseCommandMiddle;
import vg.civcraft.mc.namelayer.command.TabCompleters.GroupTabCompleter;
import vg.civcraft.mc.namelayer.misc.NLText;

public class GetDefaultGroup extends BaseCommandMiddle {

    @CommandAlias("nlgdg")
    @Description("Get a players default group")
    public void execute(Player sender) {
        Player p = (Player) sender;
        UUID uuid = NameLayerAPI.getUUID(p.getName());

        String x = gm.getDefaultGroup(uuid);
        if (x == null) {
            CivUI.error(p, "You don't have a default group. Set one with ", NLText.command("/nlsdg "));
        } else {
            CivUI.info(p, "Your default group is ", NLText.group(x));
        }
    }

    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (!(sender instanceof Player))
            return null;

        if (args.length == 1)
            return GroupTabCompleter.complete(args[0], null, (Player) sender);
        else {
            return GroupTabCompleter.complete(null, null, (Player) sender);
        }
    }
}
