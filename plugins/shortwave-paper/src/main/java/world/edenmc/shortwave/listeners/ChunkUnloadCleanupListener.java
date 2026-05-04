package world.edenmc.shortwave.listeners;

import world.edenmc.shortwave.ShortwavePlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.bukkit.persistence.PersistentDataType;

public class ChunkUnloadCleanupListener implements Listener {

    private final ShortwavePlugin plugin;

    public ChunkUnloadCleanupListener(ShortwavePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntitiesUnload(EntitiesUnloadEvent event) {
        NamespacedKey broadcastKey = plugin.getBroadcastKey();
        for (Entity entity : event.getEntities()) {
            if (entity instanceof TextDisplay && entity.getPersistentDataContainer().has(broadcastKey, PersistentDataType.BYTE)) {
                entity.remove();
            }
        }
    }
}
