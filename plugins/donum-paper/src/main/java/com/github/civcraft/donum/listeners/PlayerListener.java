package com.github.civcraft.donum.listeners;

import com.github.civcraft.donum.Donum;
import java.util.UUID;
import net.minelink.ctplus.CombatTagPlus;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import vg.civcraft.mc.civmodcore.inventory.items.ItemMap;

public class PlayerListener implements Listener {

    @EventHandler
    public void playerWasStupid(PlayerDeathEvent e) {
        Donum.getManager().saveDeathInventory(getRealUUID(e.getEntity()), new ItemMap(e.getDrops()));
    }

    private UUID getRealUUID(Player player) {
        if (Bukkit.getPluginManager().isPluginEnabled("CombatTagPlus")) {
            CombatTagPlus ctPlus = (CombatTagPlus) Bukkit.getPluginManager().getPlugin("CombatTagPlus");
            if (ctPlus.getNpcPlayerHelper().isNpc(player)) {
                return ctPlus.getNpcPlayerHelper().getIdentity(player).getId();
            }
        }
        return player.getUniqueId();
    }
}
