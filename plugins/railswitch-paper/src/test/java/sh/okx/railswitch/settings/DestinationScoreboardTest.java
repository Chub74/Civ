package sh.okx.railswitch.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.bukkit.ChatColor;
import org.junit.jupiter.api.Test;

class DestinationScoreboardTest {

    @Test
    void render_nullDestination_returnsNull() {
        assertNull(DestinationScoreboard.render(null));
    }

    @Test
    void render_emptyDestination_returnsNull() {
        assertNull(DestinationScoreboard.render(""));
    }

    @Test
    void render_blankDestination_returnsNull() {
        assertNull(DestinationScoreboard.render("   "));
    }

    @Test
    void render_value_returnsLabelledColouredLine() {
        String expected = ChatColor.GOLD + "/dest: " + ChatColor.LIGHT_PURPLE + "Spawn";
        assertEquals(expected, DestinationScoreboard.render("Spawn"));
    }

    @Test
    void render_longValue_isNotTruncatedHere() {
        String dest = "A".repeat(60);
        String line = DestinationScoreboard.render(dest);
        assertEquals(ChatColor.GOLD + "/dest: " + ChatColor.LIGHT_PURPLE + dest, line);
    }
}
