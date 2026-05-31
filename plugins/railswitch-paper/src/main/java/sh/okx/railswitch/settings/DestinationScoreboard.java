package sh.okx.railswitch.settings;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import vg.civcraft.mc.civmodcore.players.scoreboard.side.CivScoreBoard;
import vg.civcraft.mc.civmodcore.players.scoreboard.side.ScoreBoardAPI;

/**
 * Shows a player's rail destination on the civmodcore sidebar scoreboard.
 */
public final class DestinationScoreboard {

    private static final String BOARD_KEY = "railSwitchDest";

    private final CivScoreBoard board;

    public DestinationScoreboard() {
        this.board = ScoreBoardAPI.createBoard(BOARD_KEY);
    }

    /**
     * Shows the destination line for the player, or hides it when there is no destination.
     */
    public void update(Player player, String destination) {
        if (player == null) {
            return;
        }
        String line = render(destination);
        if (line == null) {
            board.hide(player);
        } else {
            board.set(player, line);
        }
    }

    public void delete() {
        ScoreBoardAPI.deleteBoard(board);
    }

    static String render(String destination) {
        if (destination == null || destination.isBlank()) {
            return null;
        }
        return ChatColor.GOLD + "/dest: " + ChatColor.LIGHT_PURPLE + destination;
    }
}
