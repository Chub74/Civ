package com.github.maxopoly.essenceglue.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import com.github.maxopoly.essenceglue.EssenceGluePlugin;
import com.github.maxopoly.essenceglue.EssenceText;
import com.github.maxopoly.essenceglue.EssenceWithdrawal;
import com.github.maxopoly.essenceglue.VirtualEssenceManager;
import org.bukkit.entity.Player;
import vg.civcraft.mc.civmodcore.ui.Bedrock;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;

@CommandAlias("essence")
public class EssenceCommand extends BaseCommand {

    private final VirtualEssenceManager manager;
    private final EssenceWithdrawal withdrawal;

    public EssenceCommand(VirtualEssenceManager manager, EssenceWithdrawal withdrawal) {
        this.manager = manager;
        this.withdrawal = withdrawal;
    }

    @Default
    @Description("Shows how much essence you have stored")
    public void onDefault(Player sender) {
        int stored = manager.getEssence(sender.getUniqueId());
        int cap = manager.getCap();
        if (stored <= 0) {
            if (EssenceGluePlugin.instance().getVoteManager() == null) {
                CivUI.info(sender, "You don't have any essence stored. You earn it by playing each day");
            } else {
                CivUI.info(sender, "You don't have any essence stored. You earn it by playing each day and with ",
                    EssenceText.voteCommand());
            }
            return;
        }
        if (stored >= cap) {
            CivUI.warn(sender, "Your essence is full at ", EssenceText.amount(stored), ". Take some out with ",
                EssenceText.withdrawCommand(), " or you'll lose new rewards");
            return;
        }
        CivUI.info(sender, "You have ", EssenceText.amount(stored), " of ", EssenceText.amount(cap),
            " essence stored. Take it out with ", EssenceText.withdrawCommand());
    }

    @Subcommand("withdraw")
    @Syntax("[amount|all]")
    @CommandCompletion("all")
    @Description("Turns stored essence into items. Without an amount, lets you pick one")
    public void onWithdraw(Player sender, @Optional String amount) {
        if (amount == null) {
            // Bedrock can't be relied on to render a slider, so it keeps the old withdraw-everything behaviour
            if (Bedrock.isBedrock(sender)) {
                withdrawal.withdraw(sender, manager.getEssence(sender.getUniqueId()));
            } else {
                withdrawal.openDialog(sender);
            }
            return;
        }
        if (amount.equalsIgnoreCase("all")) {
            withdrawal.withdraw(sender, manager.getEssence(sender.getUniqueId()));
            return;
        }
        int parsed;
        try {
            parsed = Integer.parseInt(amount);
        } catch (NumberFormatException e) {
            CivUI.error(sender, CivText.value(amount), " isn't a number. Give an amount or ", CivText.value("all"));
            return;
        }
        withdrawal.withdraw(sender, parsed);
    }
}
