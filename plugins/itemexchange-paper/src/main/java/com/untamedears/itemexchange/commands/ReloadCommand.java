package com.untamedears.itemexchange.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import com.untamedears.itemexchange.ItemExchangePlugin;
import org.bukkit.command.CommandSender;
import vg.civcraft.mc.civmodcore.ui.CivUI;

@CommandAlias("ier|iereload")
@CommandPermission("itemexchange.reload")
public final class ReloadCommand extends BaseCommand {

    private final ItemExchangePlugin plugin;

    public ReloadCommand(ItemExchangePlugin plugin) {
        this.plugin = plugin;
    }

    @Default
    @Description("Reloads the ItemExchange config")
    public void onReloadConfig(CommandSender sender) {
        this.plugin.saveDefaultConfig();
        ItemExchangePlugin.config().reset();
        this.plugin.reloadConfig();
        ItemExchangePlugin.config().parse();
        CivUI.success(sender, "Reloaded the ItemExchange config");
    }

}
