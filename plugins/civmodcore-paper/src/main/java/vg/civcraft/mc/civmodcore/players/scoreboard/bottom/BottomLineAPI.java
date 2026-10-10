package vg.civcraft.mc.civmodcore.players.scoreboard.bottom;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import vg.civcraft.mc.civmodcore.CivModCorePlugin;
import vg.civcraft.mc.civmodcore.ui.Bedrock;
import vg.civcraft.mc.civmodcore.ui.CivText;

public final class BottomLineAPI {

    private static Set<BottomLine> lines = new TreeSet<>();

    /** Players whose bar belongs to a one-off message until the given tick */
    private static final Map<UUID, Integer> yielded = new HashMap<>();

    private static final Component SEPARATOR = Component.text("  |  ", CivText.MUTED);

    public static void init() {
        BukkitRunnable run = new BukkitRunnable() {

            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    refreshIndividually(player.getUniqueId());
                }
            }
        };
        run.runTaskTimer(CivModCorePlugin.getInstance(), 15, 15);
    }

    public static BottomLine createBottomLine(String identifier, int priority) {
        BottomLine line = new BottomLine(identifier, priority);
        lines.add(line);
        return line;
    }

    public static void deleteBottomLine(BottomLine line) {
        lines.remove(line);
    }

    /** Keeps status lines off this player's bar for the given ticks, so a one-off message isn't overwritten. */
    public static void yieldTo(UUID uuid, long ticks) {
        yielded.put(uuid, Bukkit.getCurrentTick() + (int) ticks);
    }

    /** Sends this player's status lines, if they have any and the bar isn't taken by a one-off message. */
    public static void refreshIndividually(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            yielded.remove(uuid);
            return;
        }
        Integer until = yielded.get(uuid);
        if (until != null) {
            if (Bukkit.getCurrentTick() < until) {
                return;
            }
            yielded.remove(uuid);
        }
        TextComponent.Builder bar = Component.text();
        boolean empty = true;
        for (BottomLine line : lines) {
            Component entry = line.getCurrentComponent(uuid);
            if (entry != null) {
                if (!empty) {
                    bar.append(SEPARATOR);
                }
                // Each segment is closed off so its styling can't leak into the next
                bar.append(Component.text().append(entry));
                empty = false;
            }
        }
        if (empty) {
            return;
        }
        // Bedrock can't draw sprites, so they come off per viewer rather than for everyone
        player.sendActionBar(Bedrock.adapt(player, bar.build()));
    }

}
