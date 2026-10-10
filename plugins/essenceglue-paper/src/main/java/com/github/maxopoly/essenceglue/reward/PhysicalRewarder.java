package com.github.maxopoly.essenceglue.reward;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import vg.civcraft.mc.civmodcore.inventory.items.ItemMap;
import vg.civcraft.mc.civmodcore.ui.CivUI;

public class PhysicalRewarder implements Rewarder {

    private final ItemMap items;

    public PhysicalRewarder(ItemMap items) {
        this.items = items;
    }

    @Override
    public void reward(Player p, int amount, Component reason) {
        ItemMap clone = items.clone();
        clone.multiplyContent(amount);
        boolean dropped = false;
        for (ItemStack is : clone.getItemStackRepresentation()) {
            for (ItemStack toDrop : p.getInventory().addItem(is).values()) {
                p.getWorld().dropItemNaturally(p.getLocation(), toDrop);
                dropped = true;
            }
        }
        if (dropped) {
            CivUI.warn(p, "Your inventory is full, so your reward ", reason, " dropped at your feet");
        } else {
            CivUI.success(p, "You received your reward ", reason);
        }
    }
}
