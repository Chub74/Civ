package com.github.igotyou.FactoryMod.interactionManager;

import com.github.igotyou.FactoryMod.FactoryMod;
import com.github.igotyou.FactoryMod.FactoryModManager;
import com.github.igotyou.FactoryMod.factories.Pipe;
import com.github.igotyou.FactoryMod.repairManager.NoRepairDestroyOnBreakManager;
import com.github.igotyou.FactoryMod.structures.MultiBlockStructure;
import com.github.igotyou.FactoryMod.structures.PipeStructure;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.inventory.ItemStack;
import vg.civcraft.mc.citadel.ReinforcementLogic;
import vg.civcraft.mc.citadel.model.Reinforcement;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.civmodcore.ui.Kind;
import com.github.igotyou.FactoryMod.utility.FMText;

public class PipeInteractionManager implements IInteractionManager {

    private Pipe pipe;
    private FactoryModManager manager;

    public PipeInteractionManager() {
        this.manager = FactoryMod.getInstance().getManager();
    }

    public void setPipe(Pipe pipe) {
        this.pipe = pipe;
    }

    @Override
    public void rightClick(Player p, Block b, BlockFace bf) {
        // no use for this here
    }

    @Override
    public void leftClick(Player p, Block b, BlockFace bf) {
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (manager.isCitadelEnabled()) {
            Reinforcement rein = ReinforcementLogic.getReinforcementProtecting(b);
            if (rein != null && !rein.hasPermission(p, FactoryMod.getInstance().getPermissionManager().getUseFactory())) {
                FMText.deny(p, "You don't have permission to use ", FMText.factory(pipe.getName()));
                return;
            }
        }
        if (b.equals(((PipeStructure) (pipe.getMultiBlockStructure())).getStart())) {
            if (hand.getType() == manager.getFactoryInteractionMaterial()) {
                //TODO
            } else {
                if (pipe.isActive()) {
                    FMText.bar(p, Kind.ERROR, "You can't change what ", FMText.factory(pipe.getName()), " carries while it's running");
                    return;
                }
                List<Material> allowedMats = pipe.getAllowedMaterials();
                if (allowedMats == null || !allowedMats.contains(hand.getType())) {
                    pipe.addAllowedMaterial(hand.getType());
                    FMText.bar(p, Kind.SUCCESS, FMText.factory(pipe.getName()), " now carries ", FMText.item(hand.getType()));
                } else {
                    pipe.removeAllowedMaterial(hand.getType());
                    FMText.bar(p, Kind.SUCCESS, FMText.factory(pipe.getName()), " no longer carries ", FMText.item(hand.getType()));
                }
            }
        } else if (b.equals(((PipeStructure) (pipe.getMultiBlockStructure())).getFurnace())) {
            if (pipe.isActive()) {
                pipe.deactivate();
                FMText.bar(p, Kind.SUCCESS, "Stopped ", FMText.factory(pipe.getName()));
            } else {
                pipe.attemptToActivate(p, false);
            }
        }
    }

    @Override
    public void redStoneEvent(BlockRedstoneEvent e, Block factoryBlock) {
        int threshold = manager.getRedstonePowerOn();
        if (!factoryBlock.getLocation()
            .equals(((PipeStructure) pipe.getMultiBlockStructure()).getFurnace().getLocation())) {
            return;
        }
        if (e.getOldCurrent() >= threshold && e.getNewCurrent() < threshold && pipe.isActive()) {
            if ((!manager.isCitadelEnabled()
                || MultiBlockStructure.citadelRedstoneChecks(e.getBlock()))) {
                pipe.deactivate();
            }
        } else if (e.getOldCurrent() < threshold && e.getNewCurrent() >= threshold && !pipe.isActive()) {
            if (!manager.isCitadelEnabled()
                || MultiBlockStructure.citadelRedstoneChecks(e.getBlock())) {
                pipe.attemptToActivate(null, false);
            }
        }
    }

    @Override
    public void blockBreak(Player p, Block b) {
        ((NoRepairDestroyOnBreakManager) (pipe.getRepairManager())).breakIt();
        if (p != null) {
            CivUI.warn(p, "You destroyed ", FMText.factory(pipe.getName()));
        }
    }
}
