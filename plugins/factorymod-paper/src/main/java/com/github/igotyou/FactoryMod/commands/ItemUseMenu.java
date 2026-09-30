package com.github.igotyou.FactoryMod.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Syntax;
import com.github.igotyou.FactoryMod.utility.ItemUseGUI;
import java.util.Locale;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import vg.civcraft.mc.civmodcore.inventory.CustomItem;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.civmodcore.ui.CivText;

public class ItemUseMenu extends BaseCommand {

    @CommandAlias("item")
    @Syntax("[material|custom item]")
    @Description("Opens a GUI allowing you to browse all recipes which use or output the item in your main hand")
    @CommandCompletion("@materials_and_custom_items")
    public void execute(Player p, @Optional String material) {
        if (material == null) {
            new ItemUseGUI(p).showItemOverview(p.getInventory().getItemInMainHand());
            return;
        }
        ItemStack item = findItem(material);
        if (item == null) {
            CivUI.error(p, "There's no item called ", CivText.value(material));
            return;
        }
        new ItemUseGUI(p).showItemOverview(item);
    }

    /** Custom items and named factory items take precedence over vanilla materials */
    private static ItemStack findItem(String name) {
        ItemStack custom = CustomItem.getCustomItem(name);
        if (custom == null) {
            custom = CustomItem.getCustomItem(name.toLowerCase(Locale.ROOT));
        }
        if (custom != null) {
            return custom;
        }
        ItemStack named = ItemUseGUI.getNamedRecipeItems().get(ItemUseGUI.toLookupName(name));
        if (named != null) {
            return named;
        }
        Material mat = Material.matchMaterial(name);
        return mat == null ? null : new ItemStack(mat);
    }
}
