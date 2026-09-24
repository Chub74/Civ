package sh.okx.railswitch.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.HudLine;

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
        String expected = HudLine.of("Destination", CivText.value("Spawn"));
        assertEquals(expected, DestinationScoreboard.render("Spawn"));
    }

    @Test
    void render_longValue_isCappedToFitScoreboardLine() {
        String line = DestinationScoreboard.render("A".repeat(60));
        assertEquals(HudLine.of("Destination", CivText.value("A".repeat(22))), line);
        assertTrue(line.length() <= 40, "CivScoreBoard truncates at 40: " + line.length());
    }
}
