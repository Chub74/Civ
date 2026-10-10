package com.github.maxopoly.essenceglue;

import com.github.maxopoly.essenceglue.reward.Rewarder;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import vg.civcraft.mc.civmodcore.ui.CivText;

public class RewardManager {

    private final Rewarder rewarder;
    private final int dailyReward;
    private final int voteReward;

    public RewardManager(Rewarder rewarder, int dailyReward, int voteReward) {
        this.rewarder = rewarder;
        this.dailyReward = dailyReward;
        this.voteReward = voteReward;
    }

    public void giveLoginReward(Player p, int streak) {
        rewarder.reward(p, dailyReward * streak, Component.text("for logging in today"));
    }

    public void giveVoteReward(Player p, String page) {
        rewarder.reward(p, voteReward, Component.text("for voting on ").append(CivText.value(page)));
    }

}
