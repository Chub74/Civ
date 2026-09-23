package vg.civcraft.mc.citadel.playerstate;

import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import vg.civcraft.mc.citadel.Citadel;
import vg.civcraft.mc.citadel.CitadelUtility;
import vg.civcraft.mc.citadel.ReinforcementLogic;
import vg.civcraft.mc.citadel.reinforcementtypes.ReinforcementType;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.namelayer.group.Group;

public class FortificationState extends AbstractPlayerState {

    private final ReinforcementType type;
    private final Group group;

    public FortificationState(Player player, ReinforcementType type, Group group) {
        super(player);
        this.type = type;
        this.group = group;
    }

    public Group getGroup() {
        return group;
    }

    /** Appended to a failure message, so the drop out of this mode is part of the same sentence. */
    private static final Component DROPPED_TO_NORMAL = Component.text()
        .append(Component.text(", switched to ")).append(CivText.mode("Normal mode"))
        .build();

    @Override
    public Component getName() {
        return Component.text()
            .append(CivText.mode("Fortifying")).append(Component.text(" with "))
            .append(CitadelUtility.item(type))
            .append(Component.text(" on "))
            .append(CitadelUtility.group(group))
            .build();
    }

    public ReinforcementType getType() {
        return type;
    }

    @Override
    public void handleBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        // Prevent double reinforcement (slabs)
        if (ReinforcementLogic.getReinforcementAt(block.getLocation()) != null) {
            return;
        }
        if (CitadelUtility.attemptReinforcementCreation(block, type, group, player, DROPPED_TO_NORMAL)) { // true == fail
            Citadel.getInstance().getStateManager().setState(player, null, false);
            event.setCancelled(true);
            //return;
        }
    }

    @Override
    public void handleInteractBlock(PlayerInteractEvent event) {
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof FortificationState)) {
            return false;
        }
        FortificationState fort = (FortificationState) other;
        return fort.type == this.type && fort.group.getName().equals(this.getGroup().getName());
    }

    @Override
    public String getOverlayText() {
        return String.format("%sCTF %s%s %s%s", ChatColor.GOLD, ChatColor.LIGHT_PURPLE, group.getName(), ChatColor.AQUA,
            type.getName());
    }
}
