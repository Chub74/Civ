package com.github.devotedmc.hiddenore.commands;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.github.devotedmc.hiddenore.listeners.WorldGenerationListener;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;

import com.github.devotedmc.hiddenore.BlockConfig;
import com.github.devotedmc.hiddenore.Config;
import com.github.devotedmc.hiddenore.DropConfig;
import com.github.devotedmc.hiddenore.DropItemConfig;
import com.github.devotedmc.hiddenore.HiddenOre;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;

/**
 * Management and maintenance commands
 *
 * @author programmerdan
 */
public class CommandHandler implements CommandExecutor {

    private final HiddenOre plugin;
    private final List<WorldGenerationListener> worldGenerationListeners;

    public CommandHandler(HiddenOre instance, List<WorldGenerationListener> worldGenerationListeners) {
        this.plugin = instance;
        this.worldGenerationListeners = worldGenerationListeners;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("hiddenore")) {
            if (sender.hasPermission(plugin.getCommand("hiddenore").getPermission())) {
                if (args.length >= 2) {
                    if ("debug".equals(args[0])) {
                        Config.isDebug = Boolean.parseBoolean(args[1]);
                        CivUI.success(sender, "HiddenOre debug mode is now ", CivText.toggle(Config.isDebug));
                        return true;
                    }
                }

                if (args.length >= 1) {
                    if ("save".equals(args[0])) {
                        plugin.getTracking().liveSave();
                        CivUI.success(sender, "Scheduled a save of HiddenOre tracking");
                        return true;
                    } else if ("generate".equals(args[0])) {
                        if (!(sender instanceof Player)) {
                            CivUI.error(sender, "You can't run this from the console");
                            return true;
                        }
                        Player player = (Player) sender;
                        double mult = 1;
                        try {
                            mult = Integer.parseInt(args[1]);
                        } catch (Exception e) {
                            mult = 1;
                        }
                        final double vmult = mult;
                        final UUID world = player.getWorld().getUID();

                        CivUI.warn(sender, "Dropping every configured drop on you - this could cause lag");

                        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                            long delay = 0l;
                            Map<NamespacedKey, List<BlockConfig>> worldBlockConfigs = Config.instance.blockConfigs.getOrDefault(world, Config.instance.blockConfigs.get(null));
                            if (worldBlockConfigs == null) {
                                CivUI.error(sender, "No drops are configured for blocks in this world");
                                return;
                            }
                            for (NamespacedKey blockConf : worldBlockConfigs.keySet()) {
                                for (BlockConfig block : worldBlockConfigs.get(blockConf)) {
                                    for (String dropConf : block.getDrops()) {
                                        DropConfig drop = block.getDropConfig(dropConf);
                                        for (DropItemConfig item : drop.drops) {
                                            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                                CivUI.info(sender, "Dropping ", CivText.value(dropConf), " from ", CivText.value(blockConf.toString()));
                                                Item dropped = player.getWorld().dropItem(player.getLocation().add(0, 1.0, 0), item.render(vmult));
                                                dropped.setPickupDelay(20);
                                            }, delay++);
                                        }
                                    }
                                }
                            }
                        });

                        return true;
                    } else if ("clearores".equals(args[0])) {
                        if (!(sender instanceof Player player)) {
                            return false;
                        }
                        for (WorldGenerationListener worldGenerationListener : worldGenerationListeners) {
                            if (worldGenerationListener.getWorldName().equals(player.getWorld().getName())) {
                                worldGenerationListener.clearManually(sender, Integer.parseInt(args[1]));
                                return true;
                            }
                        }
                        CivUI.error(sender, "HiddenOre isn't generating ores in this world");
                        return true;
                    } else if ("isprocessed".equals(args[0])) {
                        if (!(sender instanceof Player player)) {
                            return false;
                        }
                        for (WorldGenerationListener worldGenerationListener : worldGenerationListeners) {
                            if (worldGenerationListener.getWorldName().equals(player.getWorld().getName())) {
                                CivUI.info(sender, "This chunk is ", CivText.value(worldGenerationListener.isProcessed(player.getChunk()) ? "processed" : "unprocessed"));
                                return true;
                            }
                        }
                        CivUI.error(sender, "HiddenOre isn't generating ores in this world");
                        return true;
                    }
                } else {
                    Bukkit.getPluginManager().disablePlugin(plugin);
                    Bukkit.getPluginManager().enablePlugin(plugin);
                    CivUI.success(sender, "Reloaded HiddenOre");
                    return true;
                }
            }
        }

        return false;
    }
}
