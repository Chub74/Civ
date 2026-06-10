package net.civmc.civadvancements.listeners;

import com.untamedears.jukealert.JukeAlert;
import com.untamedears.jukealert.model.SnitchFactoryType;
import net.civmc.civadvancements.AdvancementManager;
import net.civmc.civadvancements.CivAdvancementsConfig;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class JukeAlertAdvancementListener implements Listener {

    private final AdvancementManager adv;
    private final CivAdvancementsConfig config;

    public JukeAlertAdvancementListener(AdvancementManager adv, CivAdvancementsConfig config) {
        this.adv = adv;
        this.config = config;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        SnitchFactoryType type = JukeAlert.getInstance().getSnitchConfigManager().getConfig(event.getItemInHand());
        if (type == null) return;
        adv.award(event.getPlayer(), AdvancementManager.SNITCH_LOGGED);
        String typeName = type.getName();
        String noteType = config.getSnitchTypeName(AdvancementManager.SNITCH_NOTEBLOCK);
        String jukeType = config.getSnitchTypeName(AdvancementManager.SNITCH_JUKEBOX);
        if (!noteType.isEmpty() && typeName.equals(noteType)) {
            adv.award(event.getPlayer(), AdvancementManager.SNITCH_NOTEBLOCK);
        }
        if (!jukeType.isEmpty() && typeName.equals(jukeType)) {
            adv.award(event.getPlayer(), AdvancementManager.SNITCH_JUKEBOX);
        }
    }
}
