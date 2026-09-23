package vg.civcraft.mc.citadel.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import org.bukkit.command.CommandSender;
import vg.civcraft.mc.citadel.Citadel;
import vg.civcraft.mc.civmodcore.ui.CivUI;

public class Reload extends BaseCommand {

    @CommandAlias("citadelreload")
    @CommandPermission("citadel.admin")
    @Description("Reloads Citadel entirely")
    public void execute(CommandSender sender) {
        Citadel.getInstance().reload();
        CivUI.success(sender, "Reloaded Citadel");
    }
}
