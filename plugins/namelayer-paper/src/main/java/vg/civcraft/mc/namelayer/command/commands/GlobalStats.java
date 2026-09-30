package vg.civcraft.mc.namelayer.command.commands;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.namelayer.NameLayerPlugin;
import vg.civcraft.mc.namelayer.command.BaseCommandMiddle;

public class GlobalStats extends BaseCommandMiddle {

    @CommandAlias("nlgls")
    @CommandPermission("namelayer.admin")
    @Description("Get the amount of global groups.")
    public void execute(final CommandSender sender) {
        Bukkit.getScheduler().runTaskAsynchronously(NameLayerPlugin.getInstance(), new Runnable() {

            @Override
            public void run() {
                int count = NameLayerPlugin.getGroupManagerDao().countGroups();
                CivUI.info(sender, "There are ", CivText.value(String.valueOf(count)), " groups");
            }

        });
    }
}
