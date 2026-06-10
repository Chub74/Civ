package net.civmc.civadvancements.listeners;

import net.civmc.civadvancements.AdvancementManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import vg.civcraft.mc.civchat2.CivChat2;
import vg.civcraft.mc.civchat2.event.GlobalChatEvent;
import vg.civcraft.mc.civchat2.event.GroupChatEvent;

public class ChatAdvancementListener implements Listener {

    private final AdvancementManager adv;

    public ChatAdvancementListener(AdvancementManager adv) {
        this.adv = adv;
    }

    // CivChat2 fires GlobalChatEvent for LOCAL proximity chat (naming is inverted in civchat2)
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLocalChat(GlobalChatEvent event) {
        adv.award(event.getPlayer(), AdvancementManager.CHAT_LOCAL);
    }

    // GroupChatEvent fires when a player sends to any named group, including the global "!" channel
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGroupChat(GroupChatEvent event) {
        String globalGroup = CivChat2.getInstance().getPluginConfig().getGlobalChatGroupName();
        if (globalGroup != null && globalGroup.equalsIgnoreCase(event.getGroup())) {
            adv.award(event.getPlayer(), AdvancementManager.CHAT_GLOBAL);
        }
    }
}
