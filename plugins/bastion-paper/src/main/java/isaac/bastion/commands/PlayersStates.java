package isaac.bastion.commands;

import isaac.bastion.Bastion;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;

public class PlayersStates {

    private static final Component SEPARATOR = Component.text(" ▶ ", CivText.MUTED);

    static public enum Mode {
        NORMAL, INFO, DELETE, OFF, BASTION, MATURE
    }

    static private Map<UUID, Mode> playersModes = new HashMap<>();
    static private Map<UUID, Integer> playersCallback = new HashMap<>();

    static public boolean playerInMode(Player player, Mode mode) {
        Mode result = playersModes.get(player.getUniqueId());
        return result == mode;
    }

    static private class CallBack implements Runnable {

        private UUID forPlayer;
        private Mode forMode;

        public CallBack(UUID player, Mode mode) {
            forPlayer = player;
            forMode = mode;
        }

        @Override
        public void run() {
            try {
                PlayersStates.setModeForPlayer(Bastion.getPlugin().getServer().getPlayer(forPlayer), forMode);
            } catch (NullPointerException e) {
                // TODO: player offline perhaps? do something here.
            }
        }
    }

    public static void setModeForPlayer(Player player, Mode mode) {
        if (mode == null) {
            mode = Mode.NORMAL;
        }

        UUID pid = player.getUniqueId();
        Mode old = playersModes.getOrDefault(pid, Mode.NORMAL);
        playersModes.put(pid, mode);

        if (old == mode) {
            CivUI.info(player, "You're already in ", CivText.mode(modeName(mode)));
            return;
        }
        CivUI.success(player, CivText.mode(modeName(old)), SEPARATOR, CivText.mode(modeName(mode)));
    }

    public static void toggleModeForPlayer(Player player, Mode mode) {
        Mode old = playersModes.get(player.getUniqueId());
        if (old == mode) {
            mode = Mode.NORMAL;
        }
        setModeForPlayer(player, mode);
    }

    private static String modeName(Mode mode) {
        return switch (mode) {
            case NORMAL -> "Normal mode";
            case INFO -> "Info mode";
            case DELETE -> "Delete mode";
            case OFF -> "Off mode";
            case BASTION -> "Bastion mode";
            case MATURE -> "Mature mode";
        };
    }
}
