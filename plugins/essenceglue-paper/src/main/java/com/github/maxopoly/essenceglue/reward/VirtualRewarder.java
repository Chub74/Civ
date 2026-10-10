package com.github.maxopoly.essenceglue.reward;

import com.github.maxopoly.essenceglue.EssenceText;
import com.github.maxopoly.essenceglue.VirtualEssenceManager;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.civmodcore.ui.Kind;

public class VirtualRewarder implements Rewarder {

    private final VirtualEssenceManager manager;

    public VirtualRewarder(VirtualEssenceManager manager) {
        this.manager = manager;
    }

    @Override
    public void reward(Player player, int amount, Component reason) {
        UUID playerId = player.getUniqueId();
        int added = manager.addEssence(playerId, amount);
        int lost = amount - added;
        int stored = manager.getEssence(playerId);
        int cap = manager.getCap();
        Component withdraw = EssenceText.withdrawCommand();

        if (added == 0) {
            CivUI.notice(Kind.WARN, "Your essence is full, so you lost the ", EssenceText.amount(amount),
                    " essence ", reason, ". Take some out with ", withdraw)
                .icon().to(player).send();
        } else if (lost > 0) {
            CivUI.notice(Kind.WARN, "Your essence is full, so you lost ", EssenceText.amount(lost), " of the ",
                    EssenceText.amount(amount), " essence ", reason, ". Take some out with ", withdraw)
                .icon().to(player).send();
        } else if (stored >= cap) {
            CivUI.warn(player, "You received ", EssenceText.amount(amount), " essence ", reason,
                " and you're now full at ", EssenceText.amount(cap), ". Take some out with ", withdraw,
                " to keep earning");
        } else {
            CivUI.success(player, "You received ", EssenceText.amount(amount), " essence ", reason, ", ",
                EssenceText.amount(stored), " of ", EssenceText.amount(cap), " stored. Take it out with ", withdraw);
        }
    }
}
