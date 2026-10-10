package com.github.maxopoly.essenceglue.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Description;
import com.github.maxopoly.essenceglue.EssenceGluePlugin;
import com.github.maxopoly.essenceglue.EssenceText;
import com.github.maxopoly.essenceglue.StreakManager;
import java.util.UUID;
import org.bukkit.entity.Player;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;

public class StreakCommand extends BaseCommand {

    @CommandAlias("streak")
    @Description("Displays stats about your daily login streak")
    public void execute(Player p) {
        UUID uuid = StreakManager.getTrueUUID(p.getUniqueId());
        StreakManager streakMan = EssenceGluePlugin.instance().getStreakManager();
        if (!streakMan.receivesRewards(p)) {
            CivUI.warn(p, "You've turned off essence on this account. Turn it back on in ", CivText.value("/config"));
            return;
        }
        int streak = streakMan.getRecalculatedCurrentStreak(uuid);
        long cooldown = streakMan.getRewardCooldown(uuid);
        if (cooldown > 0) {
            CivUI.info(p, "Your login streak is ", EssenceText.amount(streak), ". Your next login reward unlocks in ",
                EssenceText.duration(cooldown));
        } else {
            long left = streakMan.untilTodaysReward(uuid);
            if (left > 0) {
                CivUI.info(p, "Your login streak is ", EssenceText.amount(streak), ". Stay online ",
                    EssenceText.duration(left), " more for today's login reward");
            } else {
                CivUI.info(p, "Your login streak is ", EssenceText.amount(streak),
                    ". Today's login reward arrives within a minute");
            }
        }
    }
}
