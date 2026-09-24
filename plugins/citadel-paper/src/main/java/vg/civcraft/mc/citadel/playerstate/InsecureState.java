package vg.civcraft.mc.citadel.playerstate;

import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import vg.civcraft.mc.citadel.CitadelPermissionHandler;
import vg.civcraft.mc.citadel.CitadelUtility;
import vg.civcraft.mc.citadel.ReinforcementLogic;
import vg.civcraft.mc.citadel.model.Reinforcement;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.HudLine;

public class InsecureState extends AbstractPlayerState {

    public InsecureState(Player p) {
        super(p);
    }

    @Override
    public Component getName() {
        return CivText.mode("Insecure mode");
    }

    @Override
    public void handleBlockPlace(BlockPlaceEvent e) {
    }

    @Override
    public void handleInteractBlock(PlayerInteractEvent e) {
        Reinforcement rein = ReinforcementLogic.getReinforcementProtecting(e.getClickedBlock());
        if (rein == null) {
            return;
        }
        e.setCancelled(true);
        if (rein.hasPermission(e.getPlayer(), CitadelPermissionHandler.getInsecure())) {
            rein.toggleInsecure();
            if (rein.isInsecure()) {
                CitadelUtility.infoAt(e.getPlayer(), e.getClickedBlock().getLocation(),
                    CivText.value(e.getClickedBlock().getType().name()), " is now insecure");
            } else {
                CitadelUtility.successAt(e.getPlayer(), e.getClickedBlock().getLocation(),
                    CivText.value(e.getClickedBlock().getType().name()), " is now secure");
            }
        } else {
            CitadelUtility.sendAndLog(e.getPlayer(), ChatColor.RED,
                "You are not allowed to make this reinforcement insecure",
                e.getClickedBlock().getLocation());
        }
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof InsecureState;
    }

    @Override
    public String getOverlayText() {
        return HudLine.of(CivText.mode("Insecure mode"));
    }
}
