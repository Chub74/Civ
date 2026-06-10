package net.civmc.civadvancements.listeners;

import net.civmc.civadvancements.AdvancementManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class CommandAdvancementListener implements Listener {

    private final AdvancementManager adv;

    public CommandAdvancementListener(AdvancementManager adv) {
        this.adv = adv;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String msg = event.getMessage().toLowerCase();
        Player player = event.getPlayer();
        if (msg.equals("/jalist") || msg.startsWith("/jalist ")) {
            adv.award(player, AdvancementManager.SNITCH_LOGS);
        }
        if (msg.startsWith("/jamute ")) {
            adv.award(player, AdvancementManager.SNITCH_MUTE);
        }
        if (msg.equals("/introbook") || msg.startsWith("/introbook ")) {
            adv.award(player, AdvancementManager.INTRO_BOOK);
        }
        if (msg.equals("/wiki") || msg.startsWith("/wiki ")) {
            adv.award(player, AdvancementManager.SERVER_WIKI);
        }
        if (msg.equals("/vote") || msg.startsWith("/vote ")) {
            adv.award(player, AdvancementManager.SERVER_VOTE);
        }
        if (msg.startsWith("/dest ") || msg.startsWith("/destination ")
                || msg.startsWith("/switch ") || msg.startsWith("/setdestination ")) {
            adv.award(player, AdvancementManager.DEST_COMMAND);
        }
        if (msg.equals("/nl") || msg.startsWith("/nl ")) {
            adv.award(player, AdvancementManager.NAMELAYER_LIST);
        }
    }
}
