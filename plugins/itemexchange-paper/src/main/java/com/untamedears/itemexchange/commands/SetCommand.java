package com.untamedears.itemexchange.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.annotation.CatchUnknown;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Single;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import com.untamedears.itemexchange.rules.ExchangeRule;
import com.untamedears.itemexchange.utility.RuleHandler;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import vg.civcraft.mc.civmodcore.inventory.items.ItemUtils;
import vg.civcraft.mc.civmodcore.ui.Kind;

@CommandAlias(SetCommand.ALIAS)
public final class SetCommand extends BaseCommand {

    public static final String ALIAS = "ies|ieset|set";

    @CatchUnknown
    @Description("Sets a pertinent field to an exchange rule")
    @Syntax("<field> [...values]")
    public void base(Player player) {
        throw new InvalidCommandArgument();
    }

    @Subcommand("material|mat|m")
    @Description("Sets the material of an exchange rule")
    @Syntax("<material>")
    @CommandCompletion("@itemMaterials")
    public void setMaterial(Player player, @Single String slug) {
        try (RuleHandler handler = new RuleHandler(player)) {
            Material material = Material.getMaterial(slug.toUpperCase());
            if (!ItemUtils.isValidItemMaterial(material)) {
                throw new InvalidCommandArgument("There's no item by that name");
            }
            handler.getRule().setMaterial(material);
            handler.relay(Kind.SUCCESS, "Changed the material");
        }
    }

    @Subcommand("amount|num|number|a")
    @Description("Sets the amount of an exchange rule")
    @Syntax("<amount>")
    public void setAmount(Player player, int amount) {
        try (RuleHandler handler = new RuleHandler(player)) {
            if (amount <= 0) {
                throw new InvalidCommandArgument("Enter an amount above 0");
            }
            handler.getRule().setAmount(amount);
            handler.relay(Kind.SUCCESS, "Changed the amount");
        }
    }

    @Subcommand("switchio|switch|swap|swapio")
    @Description("Sets the amount of an exchange rule")
    public void switchIO(Player player) {
        try (RuleHandler handler = new RuleHandler(player)) {
            if (handler.getRule().getType() == ExchangeRule.Type.INPUT) {
                handler.getRule().setType(ExchangeRule.Type.OUTPUT);
                handler.relay(Kind.SUCCESS, "Switched the rule to an output");
            } else if (handler.getRule().getType() == ExchangeRule.Type.OUTPUT) {
                handler.getRule().setType(ExchangeRule.Type.INPUT);
                handler.relay(Kind.SUCCESS, "Switched the rule to an input");
            } else {
                handler.relay(Kind.ERROR, "You can't switch this rule");
            }
        }
    }

}
