package net.civmc.civadvancements.listeners;

import java.util.List;
import net.civmc.civadvancements.AdvancementManager;
import net.civmc.civadvancements.ChallengeTracker;
import net.civmc.civadvancements.CivAdvancements;
import net.civmc.civadvancements.CivAdvancementsConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class WelcomeListener implements Listener {

    private final AdvancementManager adv;
    private final CivAdvancementsConfig config;
    private final ChallengeTracker tracker;
    private static final MiniMessage MM = MiniMessage.miniMessage();

    public WelcomeListener(AdvancementManager adv, CivAdvancementsConfig config, ChallengeTracker tracker) {
        this.adv = adv;
        this.config = config;
        this.tracker = tracker;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Award root immediately so the tab is visible whether or not it's first join
        adv.award(player, AdvancementManager.ROOT);

        if (!tracker.hasSeenWelcome(player.getUniqueId())) {
            // Delay 40 ticks (2 s) so the world finishes loading before we send UI
            CivAdvancements.getInstance().getServer().getScheduler()
                .runTaskLater(CivAdvancements.getInstance(), () -> sendWelcome(player), 40L);
        }
    }

    private void sendWelcome(Player player) {
        if (!player.isOnline()) return;
        if (!config.isWelcomeEnabled()) return;
        tracker.markWelcomed(player.getUniqueId());

        Component titleComp = MM.deserialize(config.getWelcomeTitle());
        Component subtitleComp = MM.deserialize(config.getWelcomeSubtitle());
        player.showTitle(Title.title(titleComp, subtitleComp));

        List<String> messages = config.getWelcomeMessages();
        for (String line : messages) {
            player.sendMessage(MM.deserialize(line));
        }

        adv.award(player, AdvancementManager.JOINED);
    }
}
