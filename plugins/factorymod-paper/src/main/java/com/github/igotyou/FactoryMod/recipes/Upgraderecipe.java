package com.github.igotyou.FactoryMod.recipes;

import com.github.igotyou.FactoryMod.eggs.FurnCraftChestEgg;
import com.github.igotyou.FactoryMod.factories.FurnCraftChestFactory;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import com.github.igotyou.FactoryMod.utility.MultiInventoryWrapper;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import vg.civcraft.mc.civmodcore.inventory.items.ItemMap;
import vg.civcraft.mc.civmodcore.inventory.items.ItemUtils;
import vg.civcraft.mc.civmodcore.ui.CivText;
import com.github.igotyou.FactoryMod.utility.FMText;
import net.kyori.adventure.text.Component;

public class Upgraderecipe extends InputRecipe {

    private FurnCraftChestEgg egg;

    public Upgraderecipe(String identifier, String name, int productionTime, ItemMap input,
                         FurnCraftChestEgg egg) {
        super(identifier, name, productionTime, input);
        this.egg = egg;
    }

    @Override
    public boolean applyEffect(Inventory inputInv, Inventory outputInv, FurnCraftChestFactory fccf) {
        MultiInventoryWrapper combo = new MultiInventoryWrapper(inputInv, outputInv);
        logBeforeRecipeRun(combo, fccf);
        if (input.isContainedIn(inputInv)) {
            if (input.removeSafelyFrom(inputInv)) {
                FurnCraftChestEgg e = egg;
                fccf.upgrade(e.getName(),
                    e.getRecipes(), e.getFuel(),
                    e.getFuelConsumptionIntervall(), e.getUpdateTime(), e.getMaximumHealth(),
                    e.getDamagePerDamagingPeriod(), e.getBreakGracePeriod(), e.getCitadelBreakReduction(),
                    e.getDecayIntervallMillis());
            }
        }
        logAfterRecipeRun(combo, fccf);
        return true;
    }

    @Override
    public ItemStack getRecipeRepresentation() {
        ItemStack res = ((InputRecipe) egg.getRecipes().get(0)).getOutputRepresentation(null, null).get(0);
        res.setAmount(1);
        ItemMeta im = res.getItemMeta();
        im.addEnchant(Enchantment.SHARPNESS, 1, true);
        im.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        res.setItemMeta(im);
        FMText.name(res, FMText.recipe(name));
        return res;
    }

    @Override
    public Material getRecipeRepresentationMaterial() {
        return ((InputRecipe) egg.getRecipes().get(0)).getOutputRepresentation(null, null).get(0).getType();
    }

    @Override
    public List<ItemStack> getInputRepresentation(Inventory i, FurnCraftChestFactory fccf) {
        if (i == null) {
            return input.getItemStackRepresentation();
        }
        LinkedList<ItemStack> result = new LinkedList<>();
        ItemMap inventoryMap = new ItemMap(i);
        ItemMap possibleRuns = new ItemMap();
        for (Entry<ItemStack, Integer> entry : input.getAllItems().entrySet()) {
            if (inventoryMap.getAmount(entry.getKey()) != 0) {
                possibleRuns.addItemAmount(
                    entry.getKey(),
                    inventoryMap.getAmount(entry.getKey())
                        / entry.getValue());
            } else {
                possibleRuns.addItemAmount(entry.getKey(), 0);
            }
        }
        for (ItemStack is : input.getItemStackRepresentation()) {
            if (possibleRuns.getAmount(is) != 0) {
                FMText.lore(is, Component.text("Enough of this for the upgrade", CivText.SUCCESS));
            } else {
                FMText.lore(is, Component.text("Not enough of this for the upgrade", CivText.ERROR));
            }
            result.add(is);
        }
        return result;
    }

    @Override
    public List<ItemStack> getOutputRepresentation(Inventory i, FurnCraftChestFactory fccf) {
        List<ItemStack> res = new LinkedList<>();
        ItemStack cr = new ItemStack(Material.CRAFTING_TABLE);
        FMText.name(cr, FMText.factory(egg.getName()));
        FMText.setLore(cr, CivText.info("Upgrade to get new and better recipes"));
        res.add(cr);
        ItemStack fur = new ItemStack(Material.FURNACE);
        FMText.name(fur, FMText.factory(egg.getName()));
        FMText.setLore(fur, FMText.section("Recipes"));
        for (IRecipe rec : egg.getRecipes()) {
            FMText.lore(fur, FMText.entry(FMText.recipe(rec.getName())));
        }
        res.add(fur);
        ItemStack che = new ItemStack(Material.CHEST);
        FMText.setLore(che, Component.text("You can't undo an upgrade", CivText.WARN));
        FMText.name(che, FMText.factory(egg.getName()));
        res.add(che);
        return res;
    }

    public FurnCraftChestEgg getEgg() {
        return egg;
    }

    @Override
    public String getTypeIdentifier() {
        return "UPGRADE";
    }

    @Override
    public List<String> getTextualOutputRepresentation(Inventory i, FurnCraftChestFactory fccf) {
        return Arrays.asList("Upgrades the factory to " + egg.getName());
    }

}
