package com.untamedears.itemexchange.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import com.untamedears.itemexchange.ItemExchangeConfig;
import com.untamedears.itemexchange.events.BlockInventoryRequestEvent;
import com.untamedears.itemexchange.rules.ExchangeRule;
import com.untamedears.itemexchange.rules.ExchangeRule.Type;
import com.untamedears.itemexchange.utility.Utilities;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BlockIterator;
import vg.civcraft.mc.civmodcore.inventory.items.ItemUtils;
import vg.civcraft.mc.civmodcore.inventory.items.MaterialUtils;
import vg.civcraft.mc.civmodcore.world.WorldUtils;
import vg.civcraft.mc.civmodcore.ui.CivUI;

/**
 * Commands class involved in creating shop exchange rules
 */
@CommandAlias(CreateCommand.ALIAS)
public final class CreateCommand extends BaseCommand {

    public static final String ALIAS = "iec|iecreate";
    public static final String ALIAS_INPUT_TYPES = "input|i|in|inputs";
    public static final String ALIAS_OUTPUT_TYPES = "output|o|out|outputs";

    // ------------------------------------------------------------
    // Creating from shop block
    // ------------------------------------------------------------

    @Default
    @Description("Creates an exchange rule based on a shop block")
    public void createFromShop(Player player) {
        if (!ItemExchangeConfig.canCreateFromShop()) {
            CivUI.error(player, "Making a shop from a chest is turned off");
            return;
        }
        BlockIterator ray = new BlockIterator(player, 6);
        while (ray.hasNext()) {
            Block block = ray.next();
            if (!WorldUtils.isValidBlock(block)) {
                continue;
            }
            if (!ItemExchangeConfig.hasCompatibleShopBlock(block.getType())) {
                // If the block is a full block then prevent the ray from going through walls.
                if (block.getType().isOccluding()) {
                    break;
                } else {
                    continue;
                }
            }
            BlockInventoryRequestEvent event = BlockInventoryRequestEvent.emit(block, player,
                BlockInventoryRequestEvent.Purpose.ACCESS);
            if (event.isCancelled()) {
                CivUI.error(player, "You don't have access to that");
                return;
            }
            ItemStack inputItem = null;
            ItemStack outputItem = null;
            Inventory inventory = event.getInventory();
            for (ItemStack item : inventory.getContents()) {
                if (!ItemUtils.isValidItem(item)) {
                    continue;
                }
                if (inputItem == null) {
                    inputItem = item.clone();
                } else if (inputItem.isSimilar(item)) {
                    inputItem.setAmount(inputItem.getAmount() + item.getAmount());
                } else if (outputItem == null) {
                    outputItem = item.clone();
                } else if (outputItem.isSimilar(item)) {
                    outputItem.setAmount(outputItem.getAmount() + item.getAmount());
                } else {
                    CivUI.error(player, "To make a shop from it, the chest can only hold two kinds of items");
                    return;
                }
            }
            if (inputItem == null) {
                CivUI.error(player, "Put the item you want to sell in the chest first");
                return;
            }
            if (Utilities.isExchangeRule(inputItem) || Utilities.isExchangeRule(outputItem)) {
                CivUI.error(player, "You can't trade exchange rules");
                return;
            }
            ExchangeRule inputRule = new ExchangeRule(Type.INPUT, inputItem);
            if (outputItem == null) {
                Utilities.giveItemsOrDrop(inventory, inputRule.toItem());
            } else {
                ExchangeRule outputRule = new ExchangeRule(Type.OUTPUT, outputItem);
                Utilities.giveItemsOrDrop(inventory, inputRule.toItem(), outputRule.toItem());
            }
            CivUI.success(player, "Created the exchange rule");
            return;
        }
        CivUI.error(player, "You aren't looking at a block that can be a shop");
    }

    // ------------------------------------------------------------
    // Creating from a held item
    // ------------------------------------------------------------

    public static final String HELD_DESCRIPTION = "Creates an exchange rule based on a held item";

    private void createFromHeld(Player player, Type type) {
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!ItemUtils.isValidItem(held)) {
            throw new InvalidCommandArgument("Hold an item to do that");
        }
        if (Utilities.isExchangeRule(held)) {
            throw new InvalidCommandArgument("You can't make a rule from a rule", false);
        }
        Utilities.givePlayerExchangeRule(player, new ExchangeRule(type, held));
        CivUI.success(player, "Created the exchange rule");
    }

    @Subcommand(ALIAS_INPUT_TYPES)
    @Description(HELD_DESCRIPTION)
    public void createInputFromHeld(Player player) {
        createFromHeld(player, Type.INPUT);
    }

    @Subcommand(ALIAS_OUTPUT_TYPES)
    @Description(HELD_DESCRIPTION)
    public void createOutputFromHeld(Player player) {
        createFromHeld(player, Type.OUTPUT);
    }

    // ------------------------------------------------------------
    // Creating from explicit details
    // ------------------------------------------------------------

    public static final String DETAILS_SYNTAX = "<material> [amount]";

    public static final String DETAILS_DESCRIPTION = "Sets the material of an exchange rule";

    public static final String DETAILS_COMPLETION = "@itemMaterials";

    private void createFromDetails(Player player, Type type, String slug, int amount) {
        Material material = MaterialUtils.getMaterial(slug);
        if (!ItemUtils.isValidItemMaterial(material)) {
            throw new InvalidCommandArgument("There's no item by that name");
        }
        ExchangeRule rule = new ExchangeRule();
        rule.setType(type);
        rule.setMaterial(material);
        if (amount <= 0) {
            throw new InvalidCommandArgument("Enter an amount above 0");
        }
        rule.setAmount(amount);
        Utilities.givePlayerExchangeRule(player, rule);
        CivUI.success(player, "Created the exchange rule");
    }

    @Subcommand(ALIAS_INPUT_TYPES)
    @Syntax(DETAILS_SYNTAX)
    @Description(DETAILS_DESCRIPTION)
    @CommandCompletion(DETAILS_COMPLETION)
    public void createInputFromDetails(Player player, String slug, @Default("1") int amount) {
        createFromDetails(player, Type.INPUT, slug, amount);
    }

    @Subcommand(ALIAS_OUTPUT_TYPES)
    @Syntax(DETAILS_SYNTAX)
    @Description(DETAILS_DESCRIPTION)
    @CommandCompletion(DETAILS_COMPLETION)
    public void createOutputFromDetails(Player player, String slug, @Default("1") int amount) {
        createFromDetails(player, Type.OUTPUT, slug, amount);
    }

}
