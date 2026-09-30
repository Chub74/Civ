package com.github.igotyou.FactoryMod.recipes;

import com.github.igotyou.FactoryMod.factories.FurnCraftChestFactory;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import com.github.igotyou.FactoryMod.utility.MultiInventoryWrapper;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import vg.civcraft.mc.civmodcore.inventory.items.EnchantUtils;
import vg.civcraft.mc.civmodcore.inventory.items.ItemMap;
import vg.civcraft.mc.civmodcore.inventory.items.ItemUtils;
import vg.civcraft.mc.civmodcore.ui.CivText;
import com.github.igotyou.FactoryMod.utility.FMText;
import net.kyori.adventure.text.Component;

public class RandomEnchantingRecipe extends InputRecipe {

    private List<RandomEnchant> enchants;
    private Material tool;
    private static Random rng;

    public class RandomEnchant {

        private Enchantment enchant;
        private int level;
        private double chance;

        public RandomEnchant(Enchantment enchant, int level, double chance) {
            this.enchant = enchant;
            this.level = level;
            this.chance = chance;
        }
    }

    public RandomEnchantingRecipe(String identifier, String name, int productionTime,
                                  ItemMap input, Material tool, List<RandomEnchant> enchants) {
        super(identifier, name, productionTime, input);
        this.enchants = enchants;
        this.tool = tool;
        if (rng == null) {
            rng = new Random();
        }
    }

    @Override
    public Material getRecipeRepresentationMaterial() {
        return tool;
    }

    @Override
    public List<ItemStack> getInputRepresentation(Inventory i, FurnCraftChestFactory fccf) {
        if (i == null) {
            List<ItemStack> bla = input.getItemStackRepresentation();
            bla.add(new ItemStack(tool));
            return bla;
        }
        List<ItemStack> returns = createLoredStacksForInfo(i);
        ItemStack toSt = new ItemStack(tool);
        FMText.lore(toSt, FMText.runs(new ItemMap(toSt).getMultiplesContainedIn(i)));
        returns.add(toSt);
        return returns;
    }

    @Override
    public List<ItemStack> getOutputRepresentation(Inventory i, FurnCraftChestFactory fccf) {
        ItemStack is = new ItemStack(tool);
        for (RandomEnchant re : enchants) {
            is.addEnchantment(re.enchant, re.level);
        }
        if (i != null) {
            FMText.lore(is, FMText.runs(String.valueOf(Math.max(new ItemMap( new ItemStack(tool)) .getMultiplesContainedIn(i), input .getMultiplesContainedIn(i)))));
        }
        for (RandomEnchant re : enchants) {
            FMText.lore(is, Component.text().append(CivText.value(re.chance * 100 + "%")).append(CivText.info(" chance of ")).append(CivText.value(EnchantUtils.getEnchantNiceName(re.enchant) + " " + re.level)).build());
        }
        FMText.lore(is, CivText.info("At least one is guaranteed"));
        List<ItemStack> stacks = new LinkedList<>();
        stacks.add(is);
        return stacks;
    }

    @Override
    public boolean applyEffect(Inventory inputInv, Inventory outputInv, FurnCraftChestFactory fccf) {
        MultiInventoryWrapper combo = new MultiInventoryWrapper(inputInv, outputInv);
        logBeforeRecipeRun(combo, fccf);
        for (ItemStack is : input.getItemStackRepresentation()) {
            inputInv.removeItem(is);
        }
        for (ItemStack is : inputInv.getContents()) {
            if (is != null && is.getType() == tool
                && !is.getItemMeta().hasEnchants()) {
                boolean applied = false;
                while (!applied) {
                    for (RandomEnchant re : enchants) {
                        if (rng.nextDouble() <= re.chance) {
                            is.getItemMeta().addEnchant(re.enchant, re.level,
                                true);
                            applied = true;
                        }
                    }
                }
                break;
            }
        }
        logAfterRecipeRun(combo, fccf);
        return true;
    }

    @Override
    public String getTypeIdentifier() {
        return "RANDOMENCHANT";
    }

    @Override
    public List<String> getTextualOutputRepresentation(Inventory i, FurnCraftChestFactory fccf) {
        return Arrays.asList("The tool input with a random enchant applied");
    }

}
