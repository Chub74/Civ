package vg.civcraft.mc.civmodcore.players.scoreboard.side;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.BiFunction;
import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import vg.civcraft.mc.civmodcore.CivModCorePlugin;

public class CivScoreBoard {

    private String scoreName;
    private Map<UUID, String> currentScoreText;
    /** What the sidebar shows instead of the text key, when the line has one - e.g. inline sprites. */
    private Map<UUID, Component> currentDisplay;
    private BukkitRunnable updater;

    CivScoreBoard(String scoreName) {
        this.scoreName = scoreName;
        this.currentScoreText = new TreeMap<>();
        this.currentDisplay = new TreeMap<>();
    }

    public String getName() {
        return scoreName;
    }

    public void updatePeriodically(BiFunction<Player, String, String> updateFunction, long delay) {
        if (updater != null) {
            updater.cancel();
        }
        updater = new BukkitRunnable() {

            @Override
            public void run() {
                Iterator<Entry<UUID, String>> iter = currentScoreText.entrySet().iterator();
                while (iter.hasNext()) {
                    Entry<UUID, String> entry = iter.next();
                    Player player = Bukkit.getPlayer(entry.getKey());
                    if (player != null) {
                        String newText = fit(updateFunction.apply(player, entry.getValue()));
                        if (newText == null) {
                            hideForPlayer(player);
                            iter.remove();
                            currentDisplay.remove(entry.getKey());
                            continue;
                        }
                        if (!newText.equals(entry.getValue())) {
                            currentDisplay.remove(entry.getKey());
                            internalUpdate(player, entry.getValue(), newText, null);
                            entry.setValue(newText);
                        }
                    }
                }
            }
        };
        updater.runTaskTimer(CivModCorePlugin.getInstance(), delay, delay);
    }

    public void set(Player p, String newText) {
        set(p, newText, null);
    }

    /**
     * text is the line's key - unique per line, and what clients without display names see.
     * display, if given, is shown instead and can hold anything a component can, such as sprites.
     */
    public void set(Player p, String newText, Component display) {
        if (newText == null) {
            hide(p);
            return;
        }
        newText = fit(newText);
        String oldText = get(p);
        internalUpdate(p, oldText, newText, display);
        currentScoreText.put(p.getUniqueId(), newText);
        if (display == null) {
            currentDisplay.remove(p.getUniqueId());
        } else {
            currentDisplay.put(p.getUniqueId(), display);
        }
    }

    /** Re-applies the current line, e.g. after the header changed and the objective was rebuilt. */
    void refresh(Player p) {
        set(p, get(p), currentDisplay.get(p.getUniqueId()));
    }

    private static void internalUpdate(Player p, String oldText, String newText, Component display) {
        if (oldText != null) {
            p.getScoreboard().resetScores(oldText);
        } else {
            ScoreBoardAPI.adjustScore(p.getUniqueId(), 1);
        }
        Score score = getObjective(p).getScore(newText);
        score.setScore(0);
        score.customName(display);
    }

    /**
     * Cut to 40 before the text is cached, so the entry reset later is the one that was shown -
     * cutting only at display time left stale lines behind. Never ends on a dangling colour code.
     */
    private static String fit(String text) {
        if (text == null || text.length() <= 40) {
            return text;
        }
        String cut = text.substring(0, 40);
        return cut.charAt(39) == LegacyComponentSerializer.SECTION_CHAR ? cut.substring(0, 39) : cut;
    }

    public String get(Player p) {
        return currentScoreText.get(p.getUniqueId());
    }

    public void hide(Player p) {
        hideForPlayer(p);
        currentScoreText.remove(p.getUniqueId());
        currentDisplay.remove(p.getUniqueId());
    }

    private void hideForPlayer(Player p) {
        String text = get(p);
        if (text == null) {
            return;
        }
        p.getScoreboard().resetScores(text);
        ScoreBoardAPI.adjustScore(p.getUniqueId(), -1);
    }

    void tearDown() {
        for (Entry<UUID, String> entry : currentScoreText.entrySet()) {
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p == null) {
                continue;
            }
            p.getScoreboard().resetScores(entry.getValue());
        }
        currentScoreText.clear();
        currentDisplay.clear();
    }

    void purge(Player p) {
        currentScoreText.remove(p.getUniqueId());
        currentDisplay.remove(p.getUniqueId());
    }

    private static Objective getObjective(Player p) {
        Scoreboard scb = p.getScoreboard();
        String title = ScoreBoardAPI.getBoardHeader(p.getUniqueId());
        Objective objective = scb.getObjective(title);
        if (objective == null) {
            scb.getObjectives().forEach(Objective::unregister);
            scb.clearSlot(DisplaySlot.SIDEBAR);
            objective = scb.registerNewObjective(title, title, title);
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            // Lines are text, not scores - hide the red numbers on the right
            objective.numberFormat(NumberFormat.blank());
        }
        return objective;
    }

}
