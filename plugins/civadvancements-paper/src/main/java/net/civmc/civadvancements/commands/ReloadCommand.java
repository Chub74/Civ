package net.civmc.civadvancements.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Subcommand;
import net.civmc.civadvancements.CivAdvancements;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

@CommandAlias("civadvancements|civadv")
@CommandPermission("civadvancements.admin")
public class ReloadCommand extends BaseCommand {

    @Subcommand("reload")
    @Description("Reload config and re-register all advancements")
    public void reload(CommandSender sender) {
        CivAdvancements.getInstance().reload();
        sender.sendMessage(Component.text("CivAdvancements reloaded.", NamedTextColor.GREEN));
    }
}
