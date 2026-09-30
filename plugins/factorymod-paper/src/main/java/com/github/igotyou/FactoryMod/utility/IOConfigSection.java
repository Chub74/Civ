package com.github.igotyou.FactoryMod.utility;

import com.github.igotyou.FactoryMod.FactoryMod;
import com.github.igotyou.FactoryMod.FactoryModManager;
import com.github.igotyou.FactoryMod.FactoryModPlayerSettings;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import vg.civcraft.mc.civmodcore.inventory.gui.Clickable;
import vg.civcraft.mc.civmodcore.inventory.gui.ClickableInventory;
import vg.civcraft.mc.civmodcore.inventory.gui.LClickable;
import vg.civcraft.mc.civmodcore.inventory.gui.components.StaticDisplaySection;
import vg.civcraft.mc.civmodcore.inventory.items.ItemUtils;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.civmodcore.ui.Kind;

/**
 * @author caucow
 */
public class IOConfigSection extends StaticDisplaySection {

    private final UUID viewerId;
    private final IOSelector ioSelector;
    private final Material centerDisplay;
    private final BlockFace front;
    private final Block centerBlock;
    private final IIOFInventoryProvider iofProvider;
    private FactoryModPlayerSettings.IoConfigDirectionMode ioDirectionMode;

    public IOConfigSection(Player viewer, IOSelector ioSelector, Material centerDisplay, Block centerBlock,
                           BlockFace front, IIOFInventoryProvider iofProvider) {
        super(9);
        this.viewerId = viewer.getUniqueId();
        this.ioSelector = ioSelector;
        this.centerDisplay = centerDisplay;
        this.centerBlock = centerBlock;
        this.front = front;
        this.iofProvider = iofProvider;
        rebuild();
    }

    private Clickable getIoButton(BlockFace dir) {
        Block relativeBlock = centerBlock.getRelative(dir);
        Material type = relativeBlock.getType();
        Direction relativeDir = Direction.getDirection(front, dir);
        return getIoClickable(type, relativeDir, dir.name());
    }

    private Clickable getIoButton(Direction dir) {
        BlockFace relativeDir = dir.getBlockFacing(front);
        Block relativeBlock = centerBlock.getRelative(relativeDir);
        Material type = relativeBlock.getType();
        return getIoClickable(type, dir, dir.name());
    }

    private Clickable getIoClickable(Material adjacentType, Direction dir, String dirLabel) {
        IOSelector.IOState dirState = ioSelector.getState(dir);
        boolean chestMissing = adjacentType != Material.CHEST && adjacentType != Material.TRAPPED_CHEST && adjacentType != Material.BARREL;
        ItemStack display;
        if (chestMissing) {
            display = new ItemStack(Material.BARRIER);
            FMText.lore(display, Component.text("No chest or barrel on this side", CivText.ERROR));
        } else {
            display = dirState.getUIVisual();
        }
        if (ioDirectionMode != null) {
            for (String descLine : ioDirectionMode.fullDescription) {
                FMText.lore(display, CivText.info(descLine));
            }
        }
        // The state keeps its own colour: it's the legend for input, output and fuel
        FMText.name(display, Component.text()
            .append(CivText.value(dirLabel))
            .append(CivText.info(": "))
            .append(Component.text(dirState.displayName, TextColor.color(dirState.color)))
            .build());
        FMText.lore(display,
            Component.empty(),
            FMText.hint("Left click to toggle input"),
            FMText.hint("Shift right click to toggle fuel"),
            FMText.hint("Right click to toggle output"));
        FactoryModManager fmMgr = FactoryMod.getInstance().getManager();
        return new Clickable(display) {
            private ClickableInventory inventory;
            private int slot;
            private BukkitTask reBarrierTask;

            @Override
            protected void clicked(Player player) {
                if (!ioSelector.isInput(dir)) {
                    if (iofProvider.getInputCount() >= fmMgr.getMaxInputChests()) {
                        FMText.bar(player, Kind.ERROR, "This factory already has the most inputs it can have");
                        return;
                    }
                    if (iofProvider.getTotalIOFCount() >= fmMgr.getMaxTotalIOFChests()) {
                        FMText.bar(player, Kind.ERROR, "This factory already has the most inputs, outputs and fuel inputs it can have");
                        return;
                    }
                }
                ioSelector.toggleInput(dir);
                updateItem();
            }

            @Override
            protected void onRightClick(Player player) {
                if (!ioSelector.isOutput(dir)) {
                    if (iofProvider.getOutputCount() >= fmMgr.getMaxOutputChests()) {
                        FMText.bar(player, Kind.ERROR, "This factory already has the most outputs it can have");
                        return;
                    }
                    if (iofProvider.getTotalIOFCount() >= fmMgr.getMaxTotalIOFChests()) {
                        FMText.bar(player, Kind.ERROR, "This factory already has the most inputs, outputs and fuel inputs it can have");
                        return;
                    }
                }
                ioSelector.toggleOutput(dir);
                updateItem();
            }

            @Override
            protected void onShiftRightClick(Player player) {
                if (!ioSelector.isFuel(dir)) {
                    if (iofProvider.getFuelCount() >= fmMgr.getMaxFuelChests()) {
                        FMText.bar(player, Kind.ERROR, "This factory already has the most fuel inputs it can have");
                        return;
                    }
                    if (iofProvider.getTotalIOFCount() >= fmMgr.getMaxTotalIOFChests()) {
                        FMText.bar(player, Kind.ERROR, "This factory already has the most inputs, outputs and fuel inputs it can have");
                        return;
                    }
                }
                ioSelector.toggleFuel(dir);
                updateItem();
            }

            @Override
            protected void onDoubleClick(Player p) {
            } // nop

            @Override
            public void addedToInventory(ClickableInventory inv, int slot) {
                this.inventory = inv;
                this.slot = slot;
            }

            private void updateItem() {
                IOSelector.IOState newState = ioSelector.getState(dir);
                ItemStack curStack = getItemStack();
                curStack.setType(newState.getUIVisual().getType());
                ItemUtils.setComponentDisplayName(curStack,
                    Component.text("\u00a7r")
                        .append(Component.text(dirLabel).color(TextColor.color(192, 192, 192)))
                        .append(Component.text(": "))
                        .append(Component.text(newState.displayName).color(TextColor.color(newState.color)))
                        .asComponent());
                if (inventory != null && inventory.getSlot(slot) == this) {
                    inventory.setSlot(this, slot);
                }
                if (chestMissing) {
                    if (reBarrierTask != null) {
                        reBarrierTask.cancel();
                    }
                    reBarrierTask = Bukkit.getScheduler().runTaskLater(FactoryMod.getInstance(), () -> {
                        getItemStack().setType(Material.BARRIER);
                        inventory.setSlot(this, slot);
                        reBarrierTask = null;
                    }, 20);
                }
            }
        };
    }

    @Override
    protected void rebuild() {
        ioDirectionMode = FactoryMod.getInstance().getManager().getPlayerSettings().getIoDirectionMode(viewerId);
        switch (ioDirectionMode) {
            case VISUAL_RELATIVE: {
                set(getIoButton(Direction.TOP), 1);
                set(getIoButton(Direction.FRONT), 2);
                set(getIoButton(Direction.LEFT), 3);
                set(getIoButton(Direction.RIGHT), 5);
                set(getIoButton(Direction.BOTTOM), 7);
                set(getIoButton(Direction.BACK), 8);
                break;
            }
            case CARDINAL: {
                set(getIoButton(BlockFace.NORTH), 1);
                set(getIoButton(BlockFace.UP), 2);
                set(getIoButton(BlockFace.WEST), 3);
                set(getIoButton(BlockFace.EAST), 5);
                set(getIoButton(BlockFace.SOUTH), 7);
                set(getIoButton(BlockFace.DOWN), 8);
                break;
            }
        }
        set(new LClickable(centerDisplay, p -> {
        }), 4);
    }
}
