package vg.civcraft.mc.citadel.playerstate;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import vg.civcraft.mc.citadel.Citadel;
import vg.civcraft.mc.citadel.CitadelPermissionHandler;
import vg.civcraft.mc.citadel.CitadelUtility;
import vg.civcraft.mc.citadel.ReinforcementLogic;
import vg.civcraft.mc.citadel.command.PatchMode;
import vg.civcraft.mc.citadel.events.ReinforcementRepairEvent;
import vg.civcraft.mc.citadel.listener.ModeListener;
import vg.civcraft.mc.citadel.model.Reinforcement;
import vg.civcraft.mc.civmodcore.inventory.items.ItemMap;
import vg.civcraft.mc.civmodcore.ui.CivText;

public class PatchState extends AbstractPlayerState {

    public PatchState(Player p) {
        super(p);
    }

    @Override
    public Component getName() {
        return CivText.mode("Patch mode");
    }

    @Override
    public void handleBlockPlace(BlockPlaceEvent e) {

    }

    @Override
    public void handleInteractBlock(PlayerInteractEvent e) {
        Reinforcement rein = ReinforcementLogic.getReinforcementProtecting(e.getClickedBlock());
        Player player = e.getPlayer();
        if (rein == null) {
            CitadelUtility.errorAt(player, e.getClickedBlock().getLocation(),
                "That block isn't reinforced");
            return;
        }
        if (!rein.hasPermission(player, CitadelPermissionHandler.getRepair())) {
            CitadelUtility.sendAndLog(player, ChatColor.RED,
                "You can't repair reinforcements on this group",
                e.getClickedBlock().getLocation());
            return;
        }
        if (rein.getHealth() >= rein.getType().getHealth()) {
            if (rein.hasPermission(player, CitadelPermissionHandler.getRepair())) {
                CitadelUtility.infoAt(player, e.getClickedBlock().getLocation(),
                    "Already at ", ModeListener.formatHealthRgb(rein), " health with ",
                    CitadelUtility.item(rein.getType()), " on ", CitadelUtility.group(rein.getGroup()));
            } else {
                CitadelUtility.infoAt(player, e.getClickedBlock().getLocation(),
                    "Already at ", ModeListener.formatHealthRgb(rein), " health");
            }
            return;
        }
        ItemMap playerMap = new ItemMap(player.getInventory());
        if (playerMap.getAmount(rein.getType().getItem()) <= 0) {
            CitadelUtility.errorAt(player, e.getClickedBlock().getLocation(),
                "You need ", CitadelUtility.item(rein.getType()), " to repair that");
            return;
        }
        ReinforcementRepairEvent repairEvent = new ReinforcementRepairEvent(e.getPlayer(), rein);
        Bukkit.getPluginManager().callEvent(repairEvent);
        if (repairEvent.isCancelled()) {
            return;
        }
        if (!rein.rollForItemReturn()) {
            if (!CitadelUtility.consumeReinforcementItems(player, rein.getType(), false)) {
                Citadel.getInstance().getStateManager().setState(player, null, false);
                return;
            }
        }

        CitadelUtility.successAt(player, e.getClickedBlock().getLocation(),
            "Patched ", CitadelUtility.item(rein.getType()), " on ",
            CitadelUtility.group(rein.getGroup()), " from ", ModeListener.formatHealthRgb(rein));

        rein.setHealth(rein.getType().getHealth());
        rein.resetCreationTime();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PatchMode;
    }

    @Override
    public String getOverlayText() {
        return ChatColor.GREEN + "CTP";
    }
}
