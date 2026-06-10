package net.civmc.civadvancements.listeners;

import net.civmc.civadvancements.AdvancementManager;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerStatisticIncrementEvent;

public class StatisticAdvancementListener implements Listener {

    private final AdvancementManager adv;

    public StatisticAdvancementListener(AdvancementManager adv) {
        this.adv = adv;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onStatistic(PlayerStatisticIncrementEvent event) {
        Player player = event.getPlayer();
        int newVal = event.getNewValue();
        switch (event.getStatistic()) {
            case MINE_BLOCK -> {
                Material block = event.getMaterial();
                if (block == Material.STONE) {
                    if (newVal >= 1000) adv.award(player, AdvancementManager.MINE_STONE_1K);
                    if (newVal >= 10000) adv.award(player, AdvancementManager.MINE_STONE_10K);
                    if (newVal >= 100000) adv.award(player, AdvancementManager.MINE_STONE_100K);
                    if (newVal >= 1_000_000) adv.award(player, AdvancementManager.MINE_STONE_1M);
                    if (newVal >= 5_000_000) adv.award(player, AdvancementManager.MINE_STONE_5M);
                    if (newVal >= 10_000_000) adv.award(player, AdvancementManager.MINE_STONE_10M);
                } else if (block == Material.COBBLED_DEEPSLATE) {
                    if (newVal >= 1000) adv.award(player, AdvancementManager.MINE_DEEPSLATE_1K);
                    if (newVal >= 10000) adv.award(player, AdvancementManager.MINE_DEEPSLATE_10K);
                    if (newVal >= 100000) adv.award(player, AdvancementManager.MINE_DEEPSLATE_100K);
                    if (newVal >= 1_000_000) adv.award(player, AdvancementManager.MINE_DEEPSLATE_1M);
                } else if (block == Material.GRAVEL) {
                    if (newVal >= 1000) adv.award(player, AdvancementManager.MINE_GRAVEL_1K);
                }
            }
            case DEATHS -> {
                if (newVal >= 10) adv.award(player, AdvancementManager.DIE_10);
                if (newVal >= 50) adv.award(player, AdvancementManager.DIE_50);
            }
            case SPRINT_ONE_CM -> {
                // 42,195m = 4,219,500cm (full marathon)
                if (newVal >= 4219500) adv.award(player, AdvancementManager.SPRINT_MARATHON);
            }
            default -> { /* nothing */ }
        }
    }
}
