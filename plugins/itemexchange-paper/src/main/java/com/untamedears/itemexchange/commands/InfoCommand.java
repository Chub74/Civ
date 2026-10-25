package com.untamedears.itemexchange.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Subcommand;
import com.untamedears.itemexchange.ItemExchangeConfig;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;

@CommandAlias("iei|ieinfo")
public final class InfoCommand extends BaseCommand {

    @Subcommand("shop|shopblocks")
    @Description("Shows what blocks can be made into shops")
    public void onShopBlocksInfo(CommandSender sender) {
        Set<Material> shopBlocks = ItemExchangeConfig.getShopCompatibleBlocks();
        if (shopBlocks.isEmpty()) {
            CivUI.info(sender, "No blocks can be made into shops");
        } else {
            CivUI.info(sender, "You can make shops from ", blocks(shopBlocks));
        }
    }

    @Subcommand("button|successbutton")
    @Description("Shows what shop blocks can trigger a successful transaction button")
    public void onSuccessButtonInfo(CommandSender sender) {
        Set<Material> successBlocks = ItemExchangeConfig.getSuccessButtonBlocks();
        if (successBlocks.isEmpty()) {
            CivUI.info(sender, "No shop blocks press a button after a trade");
        } else {
            CivUI.info(sender, "These shop blocks press a button after a trade: ", blocks(successBlocks));
        }
    }

    @Subcommand("rule|ruleitem")
    @Description("Shows what material exchange rules will be made out of")
    public void onRuleItemInfo(CommandSender sender) {
        CivUI.info(sender, "Exchange rules are made from ",
            Component.translatable(ItemExchangeConfig.getRuleItemMaterial(), CivText.VALUE));
    }

    /** Block names as the client shows them, separated by commas. */
    private static Component blocks(Set<Material> materials) {
        return Component.join(JoinConfiguration.separator(Component.text(", ", CivText.INFO)),
            materials.stream().map(material -> Component.translatable(material, CivText.VALUE)).toList());
    }

}
