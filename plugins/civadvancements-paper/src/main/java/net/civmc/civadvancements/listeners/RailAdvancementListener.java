package net.civmc.civadvancements.listeners;

import java.util.Set;
import net.civmc.civadvancements.AdvancementManager;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;

public class RailAdvancementListener implements Listener {

    private static final Set<Material> COPPER_VARIANTS = Set.of(
        Material.COPPER_BLOCK,
        Material.EXPOSED_COPPER,
        Material.WEATHERED_COPPER,
        Material.OXIDIZED_COPPER
    );

    private final AdvancementManager adv;

    public RailAdvancementListener(AdvancementManager adv) {
        this.adv = adv;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onVehicleEnter(VehicleEnterEvent event) {
        if (event.getVehicle() instanceof Minecart && event.getEntered() instanceof Player p) {
            adv.award(p, AdvancementManager.FIRST_RAIL);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!Tag.RAILS.isTagged(event.getBlock().getType())) return;
        Block below = event.getBlock().getRelative(BlockFace.DOWN);
        if (COPPER_VARIANTS.contains(below.getType())) {
            adv.award(event.getPlayer(), AdvancementManager.COPPER_RAIL);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;
        if (!(event.getClickedBlock().getState() instanceof Sign sign)) return;
        String line = PlainTextComponentSerializer.plainText().serialize(sign.getSide(Side.FRONT).line(0));
        if (line.contains("[Switch]") || line.contains("[Inverted Switch]")) {
            adv.award(event.getPlayer(), AdvancementManager.DEST_SIGN);
        }
    }
}
