package com.github.maxopoly.essenceglue;

import com.github.maxopoly.essenceglue.reward.Rewarder;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import vg.civcraft.mc.civmodcore.ui.CivText;

public class RewardManager {

    private final Rewarder rewarder;
    private final EssenceConfigManager config;

    public RewardManager(Rewarder rewarder, EssenceConfigManager config) {
        this.rewarder = rewarder;
        this.config = config;
    }

    public void giveLoginReward(Player p, int streak) {
        rewarder.reward(p, config.getLoginReward() * streak, Component.text("for logging in today"));
    }

    public void giveVoteReward(Player p, String page) {
        rewarder.reward(p, config.getVotingReward(), Component.text("for voting on ").append(CivText.value(page)));
    }

}
