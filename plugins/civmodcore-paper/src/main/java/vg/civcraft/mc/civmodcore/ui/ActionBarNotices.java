package vg.civcraft.mc.civmodcore.ui;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import vg.civcraft.mc.civmodcore.players.scoreboard.bottom.BottomLineAPI;

/** One-shot action bar notices */
final class ActionBarNotices {

    /** How long the client shows an action bar, fade included */
    private static final long CLIENT_TICKS = 60L;

    private ActionBarNotices() {
    }

    static void show(@NotNull final Player player, @NotNull final Component message) {
        player.sendActionBar(Bedrock.adapt(player, message));
        // Status lines resend every few ticks and would overwrite this, so they wait until it has faded
        BottomLineAPI.yieldTo(player.getUniqueId(), CLIENT_TICKS);
    }
}
