package com.github.igotyou.FactoryMod.recipes;

import com.github.igotyou.FactoryMod.FactoryMod;
import com.github.igotyou.FactoryMod.factories.FurnCraftChestFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import com.github.igotyou.FactoryMod.utility.MultiInventoryWrapper;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import vg.civcraft.mc.civmodcore.inventory.items.ItemMap;
import vg.civcraft.mc.civmodcore.inventory.items.ItemUtils;

public class RandomOutputRecipe extends InputRecipe {

    private Map<ItemMap, Double> outputs;
    private static Random rng;
    private ItemMap lowestChanceMap;
    private ItemMap displayOutput;

    public RandomOutputRecipe(String identifier, String name, int productionTime, ItemMap input,
                              Map<ItemMap, Double> outputs, ItemMap displayOutput) {
        super(identifier, name, productionTime, input);
        this.outputs = outputs;
        if (rng == null) {
            rng = new Random();
        }
        if (displayOutput == null) {
            for (Entry<ItemMap, Double> entry : outputs.entrySet()) {
                if (lowestChanceMap == null) {
                    lowestChanceMap = entry.getKey();
                    continue;
                }
                if (entry.getValue() < outputs.get(lowestChanceMap)) {
                    lowestChanceMap = entry.getKey();
                }
            }
            if (lowestChanceMap == null) {
                lowestChanceMap = new ItemMap(new ItemStack(Material.STONE));
            }
            this.displayOutput = lowestChanceMap;
        } else {
            lowestChanceMap = displayOutput;
            this.displayOutput = displayOutput;
        }
    }

    @Override
    public boolean applyEffect(Inventory inputInv, Inventory outputInv, FurnCraftChestFactory fccf) {
        MultiInventoryWrapper combo = new MultiInventoryWrapper(inputInv, outputInv);
        logBeforeRecipeRun(combo, fccf);
        ItemMap toRemove = input.clone();
        ItemMap toAdd = getRandomOutput(outputInv);
        if (toAdd == null) {
            FactoryMod.getInstance().warning("Unable to find a random item to output. Recipe execution was cancelled," + fccf.getLogData());
            return false;
        }
        toAdd = toAdd.clone();
        if (!toRemove.isContainedIn(inputInv)) {
            return false;
        }
        if (!toRemove.removeSafelyFrom(inputInv)) {
            return false;
        }

        List<ItemStack> insertedOutput = new ArrayList<>();
        if (!addOutputToInventorySafely(toAdd, outputInv, insertedOutput)) {
            rollbackOutput(outputInv, insertedOutput);
            restoreInput(toRemove, inputInv, fccf);
            return false;
        }
        logAfterRecipeRun(combo, fccf);
        return true;
    }

    public Map<ItemMap, Double> getOutputs() {
        return outputs;
    }

    public ItemMap getRandomOutput() {
        double random = rng.nextDouble();
        double count = 0.0;
        for (Entry<ItemMap, Double> entry : outputs.entrySet()) {
            Double chance = entry.getValue();
            if (chance == null || chance <= 0.0) {
                continue;
            }
            count += chance;
            if (count >= random) {
                return entry.getKey();
            }
        }
        return null;
    }

    public ItemMap getRandomOutput(Inventory outputInv) {
        List<Entry<ItemMap, Double>> possibleOutputs = new ArrayList<>();
        double totalChance = 0.0;
        for (Entry<ItemMap, Double> entry : outputs.entrySet()) {
            Double chance = entry.getValue();
            if (chance == null || chance <= 0.0) {
                continue;
            }
            if (outputInv != null && !canFitInOutput(entry.getKey(), outputInv)) {
                continue;
            }
            possibleOutputs.add(entry);
            totalChance += chance;
        }

        if (possibleOutputs.isEmpty() || totalChance <= 0.0) {
            return null;
        }

        double random = rng.nextDouble() * totalChance;
        double count = 0.0;
        for (Entry<ItemMap, Double> entry : possibleOutputs) {
            count += entry.getValue();
            if (count >= random) {
                return entry.getKey();
            }
        }
        return possibleOutputs.get(possibleOutputs.size() - 1).getKey();
    }

    @Override
    public Material getRecipeRepresentationMaterial() {
        return displayOutput.getItemStackRepresentation().get(0).getType();
    }

    @Override
    public List<ItemStack> getInputRepresentation(Inventory i, FurnCraftChestFactory fccf) {
        if (i == null) {
            return input.getItemStackRepresentation();
        }
        return createLoredStacksForInfo(i);
    }

    @Override
    public List<ItemStack> getOutputRepresentation(Inventory i, FurnCraftChestFactory fccf) {
        List<ItemStack> items = lowestChanceMap.getItemStackRepresentation();
        for (ItemStack is : items) {
            ItemUtils.addLore(is, ChatColor.LIGHT_PURPLE + "Randomized output");
        }
        return items;
    }

    @Override
    public EffectFeasibility evaluateEffectFeasibility(Inventory inputInv, Inventory outputInv) {
        for (Entry<ItemMap, Double> outputEntry : outputs.entrySet()) {
            Double chance = outputEntry.getValue();
            if (chance == null || chance <= 0.0) {
                continue;
            }
            if (canFitInOutput(outputEntry.getKey(), outputInv)) {
                return new EffectFeasibility(true, null);
            }
        }
        return new EffectFeasibility(false, "it ran out of storage space");
    }

    private boolean canFitInOutput(ItemMap outputMap, Inventory outputInv) {
        ItemStack[] currentContent = outputInv.getStorageContents();
        ItemStack[] simulatedOutput = new ItemStack[currentContent.length];
        for (int i = 0; i < currentContent.length; i++) {
            ItemStack slot = currentContent[i];
            simulatedOutput[i] = slot == null ? null : slot.clone();
        }

        for (Entry<ItemStack, Integer> outputEntry : outputMap.getAllItems().entrySet()) {
            ItemStack outputTemplate = outputEntry.getKey();
            int remainingAmount = outputEntry.getValue();
            if (outputTemplate == null || outputTemplate.isEmpty() || remainingAmount <= 0) {
                continue;
            }

            int maxStackSize = Math.max(1, outputTemplate.getMaxStackSize());
            for (int i = 0; i < simulatedOutput.length && remainingAmount > 0; i++) {
                ItemStack existingStack = simulatedOutput[i];
                if (existingStack == null || existingStack.isEmpty() || !existingStack.isSimilar(outputTemplate)) {
                    continue;
                }
                int existingMaxStackSize = Math.max(1, existingStack.getMaxStackSize());
                int freeSpace = Math.max(0, existingMaxStackSize - existingStack.getAmount());
                if (freeSpace <= 0) {
                    continue;
                }
                int movedAmount = Math.min(remainingAmount, freeSpace);
                existingStack.setAmount(existingStack.getAmount() + movedAmount);
                remainingAmount -= movedAmount;
            }

            for (int i = 0; i < simulatedOutput.length && remainingAmount > 0; i++) {
                ItemStack existingStack = simulatedOutput[i];
                if (existingStack != null && !existingStack.isEmpty()) {
                    continue;
                }
                int movedAmount = Math.min(remainingAmount, maxStackSize);
                ItemStack toInsert = outputTemplate.clone();
                toInsert.setAmount(movedAmount);
                simulatedOutput[i] = toInsert;
                remainingAmount -= movedAmount;
            }

            if (remainingAmount > 0) {
                return false;
            }
        }
        return true;
    }

    private boolean addOutputToInventorySafely(ItemMap outputMap, Inventory outputInv, List<ItemStack> insertedOutput) {
        for (Entry<ItemStack, Integer> outputEntry : outputMap.getAllItems().entrySet()) {
            ItemStack outputTemplate = outputEntry.getKey();
            int remainingAmount = outputEntry.getValue();
            if (outputTemplate == null || outputTemplate.isEmpty() || remainingAmount <= 0) {
                continue;
            }

            int maxStackSize = Math.max(1, outputTemplate.getMaxStackSize());
            while (remainingAmount > 0) {
                int movedAmount = Math.min(remainingAmount, maxStackSize);
                ItemStack toInsert = outputTemplate.clone();
                toInsert.setAmount(movedAmount);
                Map<Integer, ItemStack> overflow = outputInv.addItem(toInsert);
                int overflowAmount = 0;
                for (ItemStack overflowStack : overflow.values()) {
                    overflowAmount += overflowStack.getAmount();
                }
                int insertedAmount = movedAmount - overflowAmount;
                if (insertedAmount > 0) {
                    ItemStack insertedStack = outputTemplate.clone();
                    insertedStack.setAmount(insertedAmount);
                    insertedOutput.add(insertedStack);
                }
                if (!overflow.isEmpty()) {
                    return false;
                }
                remainingAmount -= movedAmount;
            }
        }
        return true;
    }

    private void rollbackOutput(Inventory outputInv, List<ItemStack> insertedOutput) {
        for (ItemStack outputStack : insertedOutput) {
            outputInv.removeItem(outputStack);
        }
    }

    private void restoreInput(ItemMap removedInput, Inventory inputInv, FurnCraftChestFactory fccf) {
        for (Entry<ItemStack, Integer> removedEntry : removedInput.getAllItems().entrySet()) {
            ItemStack removedTemplate = removedEntry.getKey();
            int remainingAmount = removedEntry.getValue();
            if (removedTemplate == null || removedTemplate.isEmpty() || remainingAmount <= 0) {
                continue;
            }

            int maxStackSize = Math.max(1, removedTemplate.getMaxStackSize());
            while (remainingAmount > 0) {
                int movedAmount = Math.min(remainingAmount, maxStackSize);
                ItemStack removedStack = removedTemplate.clone();
                removedStack.setAmount(movedAmount);
                Map<Integer, ItemStack> overflow = inputInv.addItem(removedStack);
                if (!overflow.isEmpty()) {
                    FactoryMod.getInstance().warning("Failed to fully restore input after random output rollback :(," + fccf.getLogData());
                    return;
                }
                remainingAmount -= movedAmount;
            }
        }
    }

    @Override
    public String getTypeIdentifier() {
        return "RANDOM";
    }

    public ItemMap getDisplayMap() {
        return lowestChanceMap;
    }

    @Override
    public List<String> getTextualOutputRepresentation(Inventory i, FurnCraftChestFactory fccf) {
        return Arrays.asList("A random item");
    }

}
