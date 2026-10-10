package com.github.maxopoly.essenceglue.reward;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public interface Rewarder {
    void reward(Player player, int amount, Component reason);
}
