package net.civmc.civadvancements.commands;

import org.bukkit.plugin.Plugin;
import vg.civcraft.mc.civmodcore.commands.CommandManager;

public class CivAdvancementsCommandHandler extends CommandManager {

    public CivAdvancementsCommandHandler(Plugin plugin) {
        super(plugin);
    }

    @Override
    public void registerCommands() {
        registerCommand(new ReloadCommand());
    }
}
