package vg.civcraft.mc.citadel.playerstate;

import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import vg.civcraft.mc.citadel.events.ReinforcementModeSwitchEvent;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;

public class PlayerStateManager {

    private Map<UUID, AbstractPlayerState> playerStateMap;

    public PlayerStateManager() {
        this.playerStateMap = new TreeMap<>();
    }

    public AbstractPlayerState getState(Player player) {
        if (player == null) {
            throw new IllegalArgumentException("Can not get state for null player");
        }
        AbstractPlayerState state = playerStateMap.get(player.getUniqueId());
        if (state == null) {
            state = new NormalState(player);
            playerStateMap.put(player.getUniqueId(), state);
        }
        return state;
    }

    /** Only the separator is muted. Both states keep their own tokens. */
    private static final Component SEPARATOR = Component.text(" ▶ ", CivText.MUTED);

    public void setState(Player player, AbstractPlayerState state) {
        setState(player, state, true);
    }

    /**
     * announce = false for a reset that follows an error the player was already told about. The mode overlay
     * still updates, because that is driven by the switch event rather than by the chat line.
     */
    public void setState(Player player, AbstractPlayerState state, boolean announce) {
        if (player == null) {
            throw new IllegalArgumentException("Can not set state for null player");
        }
        AbstractPlayerState existingState = getState(player);
        if (existingState == null) {
            existingState = new NormalState(player);
        }
        // null state is allowed, it just resets the state
        if (state == null) {
            state = new NormalState(player);
        }
        if (existingState.equals(state)) {
            if (announce) {
                CivUI.info(player, "You're already in ", state.getName());
            }
            return;
        }
        ReinforcementModeSwitchEvent switchEvent = new ReinforcementModeSwitchEvent(player, existingState, state);
        Bukkit.getPluginManager().callEvent(switchEvent);
        if (switchEvent.isCancelled()) {
            return;
        }
        playerStateMap.put(player.getUniqueId(), state);
        if (announce) {
            CivUI.success(player, existingState.getName(), SEPARATOR, state.getName());
        }
    }

}
