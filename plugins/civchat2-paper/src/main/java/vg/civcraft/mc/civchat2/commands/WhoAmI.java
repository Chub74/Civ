package vg.civcraft.mc.civchat2.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Description;
import com.google.common.base.Strings;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.namelayer.NameLayerAPI;

public class WhoAmI extends BaseCommand {

    @CommandAlias("whoami")
    @Description("Tells you who you are")
    public void execute(CommandSender sender) {
        String name = sender.getName();
        if (sender instanceof Player player) {
            String currentName = NameLayerAPI.getCurrentName(player.getUniqueId());
            if (!Strings.isNullOrEmpty(currentName)) {
                name = currentName;
            }
        }
        CivUI.info(sender, "You are: ", CivText.subject(name));
    }
}
