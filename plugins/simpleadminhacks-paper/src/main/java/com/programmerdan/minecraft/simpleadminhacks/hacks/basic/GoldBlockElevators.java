package com.programmerdan.minecraft.simpleadminhacks.hacks.basic;

import com.programmerdan.minecraft.simpleadminhacks.framework.utilities.SAHText;
import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import com.programmerdan.minecraft.simpleadminhacks.SimpleAdminHacks;
import com.programmerdan.minecraft.simpleadminhacks.framework.BasicHack;
import com.programmerdan.minecraft.simpleadminhacks.framework.BasicHackConfig;
import com.programmerdan.minecraft.simpleadminhacks.framework.autoload.AutoLoad;
import com.programmerdan.minecraft.simpleadminhacks.framework.autoload.DataParser;
import com.programmerdan.minecraft.simpleadminhacks.framework.utilities.TeleportUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import vg.civcraft.mc.civmodcore.inventory.items.ItemUtils;
import vg.civcraft.mc.civmodcore.players.settings.PlayerSettingAPI;
import vg.civcraft.mc.civmodcore.players.settings.gui.MenuSection;
import vg.civcraft.mc.civmodcore.players.settings.impl.BooleanSetting;
import vg.civcraft.mc.civmodcore.utilities.DoubleInteractFixer;

public class GoldBlockElevators extends BasicHack {

    @AutoLoad(processor = DataParser.MATERIAL)
    private Material elevatorBlock;

    private BooleanSetting useJumpSneakTP;
    private DoubleInteractFixer interactFixer;
    private String blockName;

    public GoldBlockElevators(SimpleAdminHacks plugin, BasicHackConfig config) {
        super(plugin, config);
        this.interactFixer = new DoubleInteractFixer(plugin);
    }

    @Override
    public void onEnable() {
        super.onEnable();

        if (elevatorBlock == null)
            elevatorBlock = Material.GOLD_BLOCK;

        this.blockName = ItemUtils.getItemName(this.elevatorBlock).toLowerCase();

        if (this.blockName.startsWith("block of"))
            this.blockName = this.blockName.substring("block of ".length());

        initSettings();
    }

    private void initSettings() {
        MenuSection mainMenu = plugin.getSettingManager().getMainMenu();
        String name = String.format("Use Jump or Sneak to teleport on %s blocks", this.blockName);
        String description = String.format("When true, jumping or sneaking on %s blocks will teleport you up/down, when false, right or left clicking will do the same.",
            this.blockName);
        this.useJumpSneakTP = new BooleanSetting(plugin, true, name, "jumpOrSneakTp", description);
        PlayerSettingAPI.registerSetting(this.useJumpSneakTP, mainMenu);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void goldBlockSneak(PlayerToggleSneakEvent event) {
        if (!this.useJumpSneakTP.getValue(event.getPlayer())) {
            return;
        }
        Block below = event.getPlayer().getLocation().getBlock().getRelative(BlockFace.DOWN);
        if (below.getType() != elevatorBlock) {
            return;
        }
        if (!event.isSneaking()) {
            return;
        }
        for (int y = (below.getY() - 1); y > below.getWorld().getMinHeight(); y--) {
            if (doTeleport(below, event.getPlayer(), y)) {
                return;
            }
        }

        String message = String.format("No %s block below. Jump to go up instead", this.blockName);

        SAHText.deny(event.getPlayer(), message);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void goldBlockJump(PlayerJumpEvent event) {
        if (!this.useJumpSneakTP.getValue(event.getPlayer())) {
            return;
        }
        Block below = event.getFrom().getBlock().getRelative(BlockFace.DOWN);
        if (below.getType() != elevatorBlock) {
            return;
        }
        for (int y = below.getY() + 1; y <= below.getWorld().getMaxHeight(); y++) {
            if (doTeleport(below, event.getPlayer(), y)) {
                return;
            }
        }

        String message = String.format("No %s block above. Sneak to go down instead", this.blockName);

        SAHText.deny(event.getPlayer(), message);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void goldBlockInteract(PlayerInteractEvent event) {
        if (this.useJumpSneakTP.getValue(event.getPlayer())) {
            return;
        }
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (interactFixer.checkInteracted(event.getPlayer(), event.getClickedBlock())) {
                return;
            }
        } else if (event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        Block below = event.getPlayer().getLocation().getBlock().getRelative(BlockFace.DOWN);
        if (below.getType() != elevatorBlock) {
            return;
        }
        if (event.getClickedBlock() == null) {
            return;
        }
        if (!event.getClickedBlock().equals(below)) {
            return;
        }
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            for (int y = below.getY() - 1; y > below.getWorld().getMinHeight(); y--) {
                if (doTeleport(below, event.getPlayer(), y)) {
                    return;
                }
            }

            String message = String.format("No %s block below. Right click to go up instead", this.blockName);

            SAHText.deny(event.getPlayer(), message);
        } else {
            for (int y = below.getY() + 1; y <= below.getWorld().getMaxHeight(); y++) {
                if (doTeleport(below, event.getPlayer(), y)) {
                    return;
                }
            }

            String message = String.format("No %s block above. Left click to go down instead", this.blockName);

            SAHText.deny(event.getPlayer(), message);
        }
    }

    private boolean doTeleport(Block source, Player player, int y) {
        Block target = source.getWorld().getBlockAt(source.getX(), y, source.getZ());
        if (target.getType() != elevatorBlock) {
            return false;
        }
        if (!TeleportUtil.checkForTeleportSpace(target.getRelative(BlockFace.UP).getLocation())) {
            return false;
        }
        source.getWorld().playSound(source.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0F, 8.0F);
        Location adjustedLocation = target.getLocation().clone();
        adjustedLocation.add(0.5, 1.02, 0.5);
        adjustedLocation.setYaw(player.getLocation().getYaw());
        adjustedLocation.setPitch(player.getLocation().getPitch());
        player.playSound(adjustedLocation, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0F, 8.0F);
        player.teleport(adjustedLocation);
        return true;
    }
}
