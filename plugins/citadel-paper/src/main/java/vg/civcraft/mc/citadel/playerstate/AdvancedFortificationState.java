package vg.civcraft.mc.citadel.playerstate;

import java.util.HashMap;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import vg.civcraft.mc.citadel.Citadel;
import vg.civcraft.mc.citadel.CitadelUtility;
import vg.civcraft.mc.citadel.reinforcementtypes.ReinforcementType;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.civmodcore.ui.HudLine;
import vg.civcraft.mc.namelayer.group.Group;

public class AdvancedFortificationState extends AbstractPlayerState {

    private Map<ItemStack, ReinforcingSetup> setups;

    public AdvancedFortificationState(Player p) {
        super(p);
        setups = new HashMap<>();
    }

    /** Appended to a failure message, so the drop out of this mode is part of the same sentence. */
    private static final Component DROPPED_TO_NORMAL = Component.text()
        .append(Component.text(", switched to ")).append(CivText.mode("Normal mode"))
        .build();

    @Override
    public Component getName() {
        return CivText.mode("Advanced fortification mode");
    }

    @Override
    public void handleBlockPlace(BlockPlaceEvent e) {
        ItemStack placed = e.getItemInHand();
        Player player = e.getPlayer();
        // check if we have a setup for this block, we need a copy for lookup so we can
        // set amount to 1
        ItemStack lookUpCopy = placed.clone();
        lookUpCopy.setAmount(1);
        ReinforcingSetup setup = setups.get(lookUpCopy);
        if (setup == null) {
            CitadelUtility.sendAndLog(player, ChatColor.RED,
                "No setup configured for this block type, no reinforcement was applied");
            return;
        }
        boolean hadError = CitadelUtility.attemptReinforcementCreation(e.getBlock(), setup.type, setup.group, e.getPlayer(), DROPPED_TO_NORMAL);
        if (hadError) {
            e.setCancelled(true);
            Citadel.getInstance().getStateManager().setState(e.getPlayer(), null, false);
        }
    }

    public void addSetup(ItemStack forBlock, ReinforcementType type, Group group) {
        ItemStack copy = forBlock.clone();
        copy.setAmount(1);
        ReinforcingSetup existing = setups.get(copy);
        setups.put(copy, new ReinforcingSetup(type, group));
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            if (existing != null) {
                CivUI.success(player, "Replaced ", CitadelUtility.item(existing.type), " on ",
                    CitadelUtility.group(existing.group), " for ",
                    CivText.value(copy.getType().name()), " with ", CitadelUtility.item(type), " on ",
                    CitadelUtility.group(group));
            } else {
                CivUI.success(player, CivText.value(copy.getType().name()), " will be reinforced with ",
                    CitadelUtility.item(type), " on ", CitadelUtility.group(group));
            }
        }
    }

    @Override
    public void handleInteractBlock(PlayerInteractEvent e) {

    }

    private class ReinforcingSetup {

        ReinforcementType type;
        Group group;

        ReinforcingSetup(ReinforcementType type, Group group) {
            this.type = type;
            this.group = group;
        }

    }

    @Override
    public boolean equals(Object o) {
        //just always make a new state for this one
        return false;
    }

    @Override
    public String getOverlayText() {
        return HudLine.of(CivText.mode("Advanced fortification"));
    }

}
