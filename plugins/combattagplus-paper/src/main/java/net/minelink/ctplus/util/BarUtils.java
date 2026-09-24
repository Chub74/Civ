package net.minelink.ctplus.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.minelink.ctplus.CombatTagPlus;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class BarUtils {

    private static final CombatTagPlus plugin = CombatTagPlus.getPlugin(CombatTagPlus.class);
    private static final Map<UUID, BossBar> bars = new HashMap<>();

    private BarUtils() {
    }

    public static boolean hasBar(Player player) {
        return bars.containsKey(player.getUniqueId());
    }

    /** Shows or updates the player's bar in place. progress is 0 to 1. */
    public static void show(Player player, Component name, BossBar.Color color, float progress) {
        float clamped = Math.max(0F, Math.min(1F, progress));
        BossBar bar = bars.get(player.getUniqueId());
        if (bar == null) {
            bar = BossBar.bossBar(name, clamped, color, BossBar.Overlay.PROGRESS);
            bars.put(player.getUniqueId(), bar);
            player.showBossBar(bar);
        } else {
            bar.name(name).progress(clamped).color(color);
        }
    }

    /** Shows a full bar, then removes it - unless something else replaced it meanwhile. */
    public static void showBriefly(Player player, Component name, BossBar.Color color, int seconds) {
        show(player, name, color, 1F);
        BossBar shown = bars.get(player.getUniqueId());
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (bars.get(player.getUniqueId()) == shown) {
                removeBar(player);
            }
        }, 20L * seconds);
    }

    public static void removeBar(Player player) {
        BossBar bar = bars.remove(player.getUniqueId());
        if (bar != null) {
            player.hideBossBar(bar);
        }
    }

    /** For players who already left - the client dropped the bar itself. */
    public static void forget(UUID playerId) {
        bars.remove(playerId);
    }
}
