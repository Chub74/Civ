package net.civeden.civadvancements.listeners;

import net.civeden.civadvancements.AdvancementManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import vg.civcraft.mc.namelayer.events.GroupAddInvitation;
import vg.civcraft.mc.namelayer.events.GroupCreateEvent;

public class NameLayerAdvancementListener implements Listener {

    private final AdvancementManager adv;

    public NameLayerAdvancementListener(AdvancementManager adv) {
        this.adv = adv;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGroupCreate(GroupCreateEvent event) {
        Player player = Bukkit.getPlayer(event.getOwner());
        if (player == null) return;
        adv.award(player, AdvancementManager.NAMELAYER_CREATE);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGroupInvite(GroupAddInvitation event) {
        Player inviter = Bukkit.getPlayer(event.getInviter());
        if (inviter == null) return;
        adv.award(inviter, AdvancementManager.NAMELAYER_INVITE);
    }
}
