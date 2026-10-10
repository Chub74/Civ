package com.github.maxopoly.essenceglue.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Description;
import com.github.maxopoly.essenceglue.EssenceGluePlugin;
import com.github.maxopoly.essenceglue.EssenceText;
import com.github.maxopoly.essenceglue.VotifyManager;
import com.github.maxopoly.essenceglue.VotingSite;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import vg.civcraft.mc.civmodcore.ui.CivUI;

public class VoteCommand extends BaseCommand {

    @CommandAlias("vote")
    @Description("Lists all available voting websites")
    public void execute(Player sender) {
        VotifyManager voteMan = EssenceGluePlugin.instance().getVoteManager();
        if (voteMan == null) {
            CivUI.error(sender, "Voting isn't set up on this server");
            return;
        }
        List<VotingSite> open = voteMan.getOpenSites(sender);
        List<Component> waiting = new ArrayList<>();
        for (VotingSite site : EssenceGluePlugin.instance().getConfigManager().getVotingCooldowns().values()) {
            long remaining = voteMan.getRemainingCooldown(site, sender);
            if (remaining > 0) {
                waiting.add(Component.text()
                    .append(EssenceText.site(site, sender))
                    .append(Component.text(" in "))
                    .append(EssenceText.duration(remaining))
                    .build());
            }
        }
        if (open.isEmpty() && waiting.isEmpty()) {
            CivUI.info(sender, "There are no voting sites set up");
            return;
        }
        if (!open.isEmpty()) {
            voteMan.showOpenSites(sender, open);
        }
        if (!waiting.isEmpty()) {
            CivUI.info(sender, "You can vote again on ", EssenceText.list(waiting));
        }
    }
}
