package com.programmerdan.minecraft.simpleadminhacks.hacks.basic;

import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import com.programmerdan.minecraft.simpleadminhacks.SimpleAdminHacks;
import com.programmerdan.minecraft.simpleadminhacks.framework.BasicHack;
import com.programmerdan.minecraft.simpleadminhacks.framework.BasicHackConfig;
import org.bukkit.entity.Player;
import vg.civcraft.mc.civmodcore.chat.Componentify;
import vg.civcraft.mc.civmodcore.commands.CommandManager;
import vg.civcraft.mc.civmodcore.world.WorldUtils;

public final class BedLocator extends BasicHack {

    private final CommandManager commands;

    public BedLocator(final SimpleAdminHacks plugin, final BasicHackConfig config) {
        super(plugin, config);
        this.commands = new CommandManager(plugin()) {
            @Override
            public void registerCommands() {
                registerCommand(new BedLocatorCommand());
            }
        };
    }

    @Override
    public void onEnable() {
        super.onEnable();
        commands.init();
    }

    @Override
    public void onDisable() {
        commands.reset();
        super.onDisable();
    }

    @CommandPermission("simpleadmin.bedlocator")
    public static class BedLocatorCommand extends BaseCommand {

        @CommandAlias("wheresmybed|locatebed")
        @Description("Tells you where your bed is")
        public void giveWand(final Player sender) {
            final var bedLocation = sender.getBedSpawnLocation();
            if (bedLocation == null) {
                CivUI.error(sender, "You don't have a bed set");
                return;
            }
            CivUI.info(sender, "Your bed is at ", Componentify.blockLocation(bedLocation).colorIfAbsent(CivText.VALUE));
            if (WorldUtils.doLocationsHaveSameWorld(sender.getLocation(), bedLocation)) {
                sender.sendMessage('['
                    + "name:Bed Location,"
                    + "x:" + bedLocation.getBlockX() + ','
                    + "y:" + bedLocation.getBlockY() + ','
                    + "z:" + bedLocation.getBlockZ()
                    + ']');
            }
        }
    }

}
