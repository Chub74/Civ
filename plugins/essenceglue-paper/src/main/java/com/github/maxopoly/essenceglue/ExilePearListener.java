package com.github.maxopoly.essenceglue;

import com.devotedmc.ExilePearl.event.PearlDecayEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class ExilePearListener implements Listener {

    private final EssenceConfigManager config;
    private final StreakManager streakMan;

    public ExilePearListener(StreakManager streakMan, EssenceConfigManager config) {
        this.config = config;
        this.streakMan = streakMan;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void pearlDecay(PearlDecayEvent event) {
        if (!config.multiplyPearlCost()) {
            return;
        }
        int streak = streakMan.getRecalculatedCurrentStreak(event.getPearl().getPlayerId());
        event.setDamageAmount(
            Math.max(1, Math.round(event.getDamageAmount() * (1 + (streak * 0.5f))))
        );
    }

}
