package com.programmerdan.minecraft.simpleadminhacks.hacks.basic;

import com.programmerdan.minecraft.simpleadminhacks.SimpleAdminHacks;
import com.programmerdan.minecraft.simpleadminhacks.framework.BasicHack;
import com.programmerdan.minecraft.simpleadminhacks.framework.BasicHackConfig;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockDispenseArmorEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

/**
 * Prevents mobs from picking up any item that has durability (armor, tools, weapons).
 * Also prevents dispensers from equipping armor onto mobs.
 *
 * <p>On Eden, armor is very hard to obtain. Vanilla Minecraft allows mobs to pick up
 * dropped items and equip them; when that mob dies the item is converted to raw materials,
 * destroying enchantments and the item entirely. This hack closes that loss vector.</p>
 *
 * <p>The {@code Damageable} meta check covers all armor, tools, and weapons automatically
 * without requiring a hardcoded material list.</p>
 */
public class NoMobGearPickup extends BasicHack {

    public NoMobGearPickup(SimpleAdminHacks plugin, BasicHackConfig config) {
        super(plugin, config);
    }

    /**
     * Cancel pickup of any damageable item (armor/tools/weapons) by non-player mobs.
     * Uses EntityPickupItemEvent which fires BEFORE equip, so cancelling is effective.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMobPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player) {
            return;
        }
        if (event.getEntity() instanceof Mob) {
            ItemStack item = event.getItem().getItemStack();
            if (item.hasItemMeta() && item.getItemMeta() instanceof Damageable) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Cancel dispensers equipping armor onto mobs.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDispenseArmor(BlockDispenseArmorEvent event) {
        if (event.getTargetEntity() instanceof Mob) {
            event.setCancelled(true);
        }
    }
}
