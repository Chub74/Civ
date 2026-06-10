package net.civmc.civadvancements;

import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class RewardManager {

    private final CivAdvancementsConfig config;

    public RewardManager(CivAdvancementsConfig config) {
        this.config = config;
    }

    public void giveReward(Player player, String key) {
        List<Map<?, ?>> items = config.getRewardItems(key);
        if (items.isEmpty()) return;
        for (Map<?, ?> m : items) {
            Object matObj = m.get("material");
            if (!(matObj instanceof String matStr)) continue;
            Material mat = Material.getMaterial(matStr.toUpperCase());
            if (mat == null) continue;
            int amount = 1;
            Object amtObj = m.get("amount");
            if (amtObj instanceof Number num) {
                amount = num.intValue();
            }
            ItemStack stack = new ItemStack(mat, amount);
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(stack);
            leftover.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        }
    }
}
