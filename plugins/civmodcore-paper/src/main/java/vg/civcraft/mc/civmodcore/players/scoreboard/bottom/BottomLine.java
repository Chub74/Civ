package vg.civcraft.mc.civmodcore.players.scoreboard.bottom;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.BiFunction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import vg.civcraft.mc.civmodcore.CivModCorePlugin;

/**
 * One segment of the action bar. Held as components so sprites survive; the String methods take and give
 * legacy section-coded text for the callers that still build strings.
 */
public class BottomLine implements Comparable<BottomLine> {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private Map<UUID, Component> texts;
    /** The legacy text a line was last set from, so periodic updates compare strings instead of reserializing */
    private Map<UUID, String> legacyTexts = new TreeMap<>();
    private String identifier;
    private BukkitRunnable updater;
    private int priority;

    BottomLine(String identifier, int priority) {
        this.identifier = identifier;
        this.priority = priority;
        this.texts = new TreeMap<>();
    }

    public String getIdentifier() {
        return identifier;
    }

    public void updatePlayer(Player player, String text) {
        updatePlayer(player.getUniqueId(), text);
    }

    public void updatePlayer(UUID player, String text) {
        texts.put(player, LEGACY.deserialize(text));
        legacyTexts.put(player, text);
    }

    public void updatePlayer(Player player, Component text) {
        updatePlayer(player.getUniqueId(), text);
    }

    public void updatePlayer(UUID player, Component text) {
        texts.put(player, Objects.requireNonNull(text, "text cannot be null"));
        legacyTexts.remove(player);
    }

    public String getCurrentText(UUID uuid) {
        String legacy = legacyTexts.get(uuid);
        if (legacy != null) {
            return legacy;
        }
        Component text = texts.get(uuid);
        return text == null ? null : LEGACY.serialize(text);
    }

    public Component getCurrentComponent(UUID uuid) {
        return texts.get(uuid);
    }

    public void updatePeriodically(BiFunction<Player, String, String> updateFunction, long delay) {
        if (updater != null) {
            updater.cancel();
        }
        updater = new BukkitRunnable() {

            @Override
            public void run() {
                Iterator<Entry<UUID, Component>> iter = texts.entrySet().iterator();
                while (iter.hasNext()) {
                    Entry<UUID, Component> entry = iter.next();
                    Player player = Bukkit.getPlayer(entry.getKey());
                    if (player != null) {
                        String oldText = getCurrentText(entry.getKey());
                        String newText = updateFunction.apply(player, oldText);
                        if (newText == null) {
                            iter.remove();
                            legacyTexts.remove(entry.getKey());
                            BottomLineAPI.refreshIndividually(player.getUniqueId());
                            continue;
                        }
                        if (!newText.equals(oldText)) {
                            entry.setValue(LEGACY.deserialize(newText));
                            legacyTexts.put(entry.getKey(), newText);
                            BottomLineAPI.refreshIndividually(player.getUniqueId());
                        }
                    }
                }
            }
        };
        updater.runTaskTimer(CivModCorePlugin.getInstance(), delay, delay);
    }

    public void removePlayer(Player player) {
        removePlayer(player.getUniqueId());
    }

    public void removePlayer(UUID uuid) {
        legacyTexts.remove(uuid);
        if (texts.remove(uuid) != null) {
            BottomLineAPI.refreshIndividually(uuid);
        }
    }

    Map<UUID, Component> getAll() {
        return texts;
    }

    @Override
    public boolean equals(Object o) {
        return o == this;
    }

    @Override
    public int compareTo(BottomLine o) {
        int prio = Integer.compare(priority, o.priority);
        if (prio != 0) {
            return prio;
        }
        return Integer.compare(hashCode(), o.hashCode());
    }

}
