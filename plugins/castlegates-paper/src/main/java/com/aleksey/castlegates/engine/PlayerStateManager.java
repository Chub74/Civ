/**
 * @author Aleksey Terzi
 */

package com.aleksey.castlegates.engine;

import java.util.Map;
import java.util.HashMap;

import com.aleksey.castlegates.CastleGates;
import com.aleksey.castlegates.types.TimerMode;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.aleksey.castlegates.types.CommandMode;
import com.aleksey.castlegates.types.TimerOperation;
import com.aleksey.castlegates.utils.Text;
import net.kyori.adventure.text.Component;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;

public class PlayerStateManager {

    private static final Component SEPARATOR = Component.text(" ▶ ", CivText.MUTED);

    public static class PlayerState {

        public CommandMode mode;
        public Integer timer;
        public TimerOperation timerOperation;
        public TimerMode timerMode;
        public long lastInteracted;
    }

    private final Map<Player, PlayerState> _states = new HashMap<>();
    private final Map<Player, Integer> _tasks = new HashMap<>();

    public void clearPlayerMode(Player player) {
        clearPlayerMode(player, false);
    }

    private void clearPlayerMode(Player player, boolean timedOut) {
        // Removed, not just cancelled, so addTask schedules a fresh one next time.
        Integer taskId = _tasks.remove(player);

        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
        }

        PlayerState state = _states.get(player);

        if (state == null) return;

        _states.remove(player);

        if (player.isOnline()) {
            if (timedOut) {
                CivUI.info(player, Text.mode(state.mode), " timed out", SEPARATOR, Text.mode(CommandMode.NORMAL));
            } else {
                CivUI.success(player, Text.mode(state.mode), SEPARATOR, Text.mode(CommandMode.NORMAL));
            }
        }
    }

    public PlayerState getPlayerState(Player player) {
        return _states.get(player);
    }

    public void setPlayerMode(Player player, CommandMode mode, Integer timer, TimerOperation timerOperation, TimerMode timerMode) {
        if (mode == CommandMode.OFF) {
            clearPlayerMode(player);
            return;
        }

        PlayerState state = _states.get(player);

        if (state == null) {
            if (mode == CommandMode.NORMAL) {
                CivUI.info(player, "You're already in ", Text.mode(CommandMode.NORMAL));
                return;
            }

            _states.put(player, state = new PlayerState());
        } else if (state.mode == mode) {
            clearPlayerMode(player);
            return;
        }

        CommandMode previous = state.mode == null ? CommandMode.NORMAL : state.mode;

        state.mode = mode;
        state.timer = timer;
        state.timerOperation = timerOperation;
        state.timerMode = timerMode;
        state.lastInteracted = System.currentTimeMillis();

        addTask(player);

        CivUI.success(player, Text.mode(previous), SEPARATOR, Text.mode(mode));
    }

    public void interact(Player player) {
        PlayerState state = _states.get(player);

        if (state != null) {
            state.lastInteracted = System.currentTimeMillis();
        }
    }

    private void addTask(final Player player) {
        if (_tasks.containsKey(player)) return;

        final PlayerStateManager stateManager = this;

        int taskId = Bukkit.getScheduler().runTaskTimer(
            CastleGates.getInstance(),
            () -> {
                PlayerState state = stateManager._states.get(player);
                if (state != null) {
                    long offTime = state.lastInteracted + 1000L * CastleGates.getConfigManager().getPlayerStateResetInSeconds();

                    if (offTime < System.currentTimeMillis()) {
                        clearPlayerMode(player, true);
                    }
                }
            },
            20L * 30,
            20L * 30
        ).getTaskId();

        _tasks.put(player, taskId);
    }
}
