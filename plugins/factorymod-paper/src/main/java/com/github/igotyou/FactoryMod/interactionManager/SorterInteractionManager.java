package com.github.igotyou.FactoryMod.interactionManager;

import com.github.igotyou.FactoryMod.FactoryMod;
import com.github.igotyou.FactoryMod.FactoryModManager;
import com.github.igotyou.FactoryMod.factories.Sorter;
import com.github.igotyou.FactoryMod.structures.BlockFurnaceStructure;
import com.github.igotyou.FactoryMod.structures.MultiBlockStructure;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.inventory.ItemStack;
import vg.civcraft.mc.citadel.ReinforcementLogic;
import vg.civcraft.mc.citadel.model.Reinforcement;
import vg.civcraft.mc.civmodcore.inventory.items.ItemUtils;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.civmodcore.ui.Kind;
import vg.civcraft.mc.civmodcore.ui.CivText;
import com.github.igotyou.FactoryMod.utility.FMText;

public class SorterInteractionManager implements IInteractionManager {

    private Sorter sorter;
    private BlockFurnaceStructure bfs;
    private FactoryModManager manager;

    public SorterInteractionManager(Sorter sorter) {
        if (sorter != null) {
            setSorter(sorter);
        }
        manager = FactoryMod.getInstance().getManager();
    }

    public SorterInteractionManager() {
        this(null);
    }

    public void setSorter(Sorter sorter) {
        this.sorter = sorter;
        this.bfs = (BlockFurnaceStructure) sorter.getMultiBlockStructure();
    }

    @Override
    public void blockBreak(Player p, Block b) {
        sorter.getRepairManager().breakIt();
        if (p != null) {
            CivUI.warn(p, "You destroyed ", FMText.factory(sorter.getName()));
        }
    }

    @Override
    public void rightClick(Player p, Block b, BlockFace bf) {
        // not needed here
    }

    @Override
    public void leftClick(Player p, Block b, BlockFace bf) {
        if (manager.isCitadelEnabled()) {
            Reinforcement rein = ReinforcementLogic.getReinforcementProtecting(b);
            if (rein != null && !rein.hasPermission(p, FactoryMod.getInstance().getPermissionManager().getUseFactory())) {
                FMText.deny(p, "You don't have permission to use ", FMText.factory(sorter.getName()));
                return;
            }
        }
        if (b.equals(bfs.getFurnace())) {
            if (p.getInventory().getItemInMainHand().getType()
                .equals(manager.getFactoryInteractionMaterial())) {
                sorter.attemptToActivate(p, false);
            }
        } else { // center
            if (p.isSneaking() && p.getInventory().getItemInMainHand().getType()
                .equals(manager.getFactoryInteractionMaterial())) {
                //TODO
                return;
            }
            ItemStack is = p.getInventory().getItemInMainHand();
            BlockFace side = sorter.getSide(is);
            if (side == null) {
                sorter.addAssignment(bf, is);
                FMText.bar(p, Kind.SUCCESS, "Sorting ", FMText.item(is), " to the ", CivText.value(bf.name().toLowerCase()), " side");
            } else {
                if (side == bf) {
                    sorter.removeAssignment(is);
                    FMText.bar(p, Kind.SUCCESS, "Stopped sorting ", FMText.item(is), " to the ", CivText.value(side.name().toLowerCase()), " side");
                } else {
                    FMText.bar(p, Kind.ERROR, FMText.item(is), " already sorts to the ", CivText.value(side.name().toLowerCase()), " side");
                }
            }
        }
    }

    @Override
    public void redStoneEvent(BlockRedstoneEvent e, Block factoryBlock) {
        int threshold = manager.getRedstonePowerOn();
        if (!factoryBlock.getLocation()
            .equals(((BlockFurnaceStructure) sorter.getMultiBlockStructure()).getFurnace().getLocation())) {
            return;
        }
        if (e.getOldCurrent() >= threshold && e.getNewCurrent() < threshold && sorter.isActive()) {
            if ((!manager.isCitadelEnabled()
                || MultiBlockStructure.citadelRedstoneChecks(e.getBlock()))) {
                sorter.deactivate();
            }
        } else if (e.getOldCurrent() < threshold && e.getNewCurrent() >= threshold && !sorter.isActive()) {
            if (!manager.isCitadelEnabled()
                || MultiBlockStructure.citadelRedstoneChecks(e.getBlock())) {
                sorter.attemptToActivate(null, false);
            }
        }
    }
}
