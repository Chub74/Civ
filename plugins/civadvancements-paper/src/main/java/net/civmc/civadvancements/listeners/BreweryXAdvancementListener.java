package net.civmc.civadvancements.listeners;

import com.dre.brewery.Brew;
import net.civmc.civadvancements.AdvancementManager;
import net.civmc.civadvancements.CivAdvancementsConfig;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;

public class BreweryXAdvancementListener implements Listener {

    private final AdvancementManager adv;
    private final CivAdvancementsConfig config;

    public BreweryXAdvancementListener(AdvancementManager adv, CivAdvancementsConfig config) {
        this.adv = adv;
        this.config = config;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCauldronPlace(BlockPlaceEvent event) {
        if (event.getBlock().getType() == Material.CAULDRON) {
            adv.award(event.getPlayer(), AdvancementManager.BREW_CAULDRON);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        Brew brew = Brew.get(event.getItem());
        if (brew == null) return;
        adv.award(event.getPlayer(), AdvancementManager.BREW_FIRST);
        if (brew.getQuality() >= config.getBrewQualityThreshold()) {
            adv.award(event.getPlayer(), AdvancementManager.BREW_QUALITY);
        }
    }
}
