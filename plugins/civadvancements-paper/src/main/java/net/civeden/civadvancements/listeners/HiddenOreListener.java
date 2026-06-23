package net.civeden.civadvancements.listeners;

import com.github.devotedmc.hiddenore.events.HiddenOreGenerateEvent;
import net.civeden.civadvancements.AdvancementManager;
import net.civeden.civadvancements.CivAdvancementsConfig;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class HiddenOreListener implements Listener {

    private final AdvancementManager adv;
    private final CivAdvancementsConfig config;

    private static final String[] ORE_KEYS = {
        AdvancementManager.ORE_IRON,
        AdvancementManager.ORE_GOLD,
        AdvancementManager.ORE_DIAMOND,
        AdvancementManager.ORE_EMERALD,
        AdvancementManager.ORE_ANCIENT_DEBRIS
    };

    public HiddenOreListener(AdvancementManager adv, CivAdvancementsConfig config) {
        this.adv = adv;
        this.config = config;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOreGenerate(HiddenOreGenerateEvent event) {
        if (event.getPlayer() == null) return;
        Player player = event.getPlayer();
        adv.award(player, AdvancementManager.HIDDEN_ORE);
        Material transform = event.getTransform();
        for (String key : ORE_KEYS) {
            checkOre(player, transform, key);
        }
    }

    private void checkOre(Player player, Material transform, String key) {
        String configured = config.getOreMaterial(key);
        if (configured.isBlank()) return;
        Material mat = Material.matchMaterial(configured);
        if (mat != null && transform == mat) {
            adv.award(player, key);
        }
    }
}
