package com.github.longboyy.netherpearls;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import vg.civcraft.mc.civmodcore.ACivMod;
import java.util.Random;

public class NetherPearls extends ACivMod implements Listener {

    private final Random random = new Random();
    private final NetherPearlsConfig config;

    public NetherPearls() {
        this.config = new NetherPearlsConfig(this);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if(!this.config.parse()){
            this.disable();
        }
        this.registerListener(this);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        HandlerList.unregisterAll((Listener) this);
    }

    @EventHandler(ignoreCancelled = true)
    public void OnEntityDeath(EntityDeathEvent event) {

        if(event.getDamageSource().getCausingEntity() == null || event.getDamageSource().getCausingEntity().getType() != EntityType.PLAYER) {
            return;
        }

        Double dropChance = config.getEntityDropChances().get(event.getEntityType());
        if(dropChance == null){
            return;
        }

        if(random.nextFloat() < dropChance){
            // figure out how many we should add to drops
            int minDrop = Math.max(1, config.getMinDrop());
            // Admins can change both live, so don't let a min above max break the bounds
            int maxDrop = Math.max(minDrop, config.getMaxDrop());
            int amount = random.nextInt(minDrop, maxDrop + 1);
            event.getDrops().add(new ItemStack(Material.ENDER_PEARL, amount));
        }
    }
}
