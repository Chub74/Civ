package vg.civcraft.mc.namelayer.command.commands;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Description;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import vg.civcraft.mc.namelayer.GroupManager.PlayerType;
import vg.civcraft.mc.namelayer.command.BaseCommandMiddle;

public class ListPlayerTypes extends BaseCommandMiddle {

    @CommandAlias("nllpt|ranks")
    @Description("List PlayerTypes.")
    public void execute(CommandSender sender) {
        PlayerType.displayPlayerTypesnllpt(sender);
    }
}
