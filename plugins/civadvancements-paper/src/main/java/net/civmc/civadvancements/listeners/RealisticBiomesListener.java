package net.civmc.civadvancements.listeners;

import com.untamedears.realisticbiomes.RealisticBiomes;
import com.untamedears.realisticbiomes.growthconfig.PlantGrowthConfig;
import net.civmc.civadvancements.AdvancementManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

public class RealisticBiomesListener implements Listener {

    private final AdvancementManager adv;

    public RealisticBiomesListener(AdvancementManager adv) {
        this.adv = adv;
    }

    // Fire at MONITOR so RealisticBiomes' HIGHEST-priority handler has already run
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (item == null) return;

        PlantGrowthConfig pgc = RealisticBiomes.getInstance()
            .getGrowthConfigManager()
            .getGrowthConfigByItem(item);

        if (pgc == null) return; // not a tracked plant

        long growthTime = pgc.getPersistentGrowthTime(event.getBlock());
        if (growthTime < 0) {
            adv.award(event.getPlayer(), AdvancementManager.RB_BARREN);
        } else {
            adv.award(event.getPlayer(), AdvancementManager.RB_FRUITFUL);
        }
    }
}
