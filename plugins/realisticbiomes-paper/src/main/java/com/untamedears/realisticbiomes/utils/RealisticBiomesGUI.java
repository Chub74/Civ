package com.untamedears.realisticbiomes.utils;

import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.Component;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivMenu;
import com.untamedears.realisticbiomes.RealisticBiomes;
import com.untamedears.realisticbiomes.growthconfig.PlantGrowthConfig;
import com.untamedears.realisticbiomes.growthconfig.inner.BiomeGrowthConfig;
import com.untamedears.realisticbiomes.growthconfig.inner.PersistentGrowthConfig;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import vg.civcraft.mc.civmodcore.inventory.gui.DecorationStack;
import vg.civcraft.mc.civmodcore.inventory.gui.IClickable;
import vg.civcraft.mc.civmodcore.inventory.gui.components.ComponableInventory;
import vg.civcraft.mc.civmodcore.inventory.gui.components.ContentAligners;
import vg.civcraft.mc.civmodcore.inventory.gui.components.Scrollbar;
import vg.civcraft.mc.civmodcore.inventory.gui.components.SlotPredicates;
import vg.civcraft.mc.civmodcore.inventory.gui.components.StaticDisplaySection;
import vg.civcraft.mc.civmodcore.utilities.TextUtil;

public class RealisticBiomesGUI {

    private final Player player;
    private ComponableInventory inventory;
    private static final DecimalFormat decimalFormat = new DecimalFormat("0.00");

    public RealisticBiomesGUI(Player player) {
        this.player = player;
    }

    /**
     * Show GUI containing all RB effected crops, sorted by their biome multiplier in current biome.
     */
    public void showRBOverview(Biome biome) {
        Biome currentBiome;
        if (biome == null) {
            currentBiome = player.getLocation().getBlock().getBiome();
        } else {
            currentBiome = biome;
        }
        if (inventory == null) {
            String biomeText = currentBiome.getKey().value().replace("_", " ");
            biomeText = StringUtils.capitalize(biomeText);
            biomeText = StringUtils.abbreviate(biomeText, 24);
            inventory = new ComponableInventory(CivMenu.title(biomeText), 6, player);
        } else {
            inventory.clear();
        }
        List<IClickable> clicks = new LinkedList<>();
        List<PlantGrowthConfig> plantConfigs = new ArrayList<>(
            RealisticBiomes.getInstance().getConfigManager().getPlantGrowthConfigs());
        plantConfigs.sort((p1, p2) -> {
            int comparision = Double.compare(p2.getBiomeGrowthConfig().getBiomeMultiplier(currentBiome)
                , p1.getBiomeGrowthConfig().getBiomeMultiplier(currentBiome)); //reverse order
            if (comparision == 0) {
                return p1.getItem().getType().compareTo(p2.getItem().getType());
            } else {
                return comparision;
            }
        });
        for (PlantGrowthConfig plant : plantConfigs) {
            Material representation = plant.getItem().getType();
            if (representation == Material.COCOA) {
                representation = Material.COCOA_BEANS;
            } else if (!representation.isItem()) {
                representation = Material.BARRIER;
            }
            ItemStack is = new ItemStack(representation);
            CivMenu.name(is, plant.getName());
            List<Component> lore = new ArrayList<>();
            BiomeGrowthConfig config = plant.getBiomeGrowthConfig();
            double biomeMultiplier = config.getBiomeMultiplier(currentBiome);
            if (config instanceof PersistentGrowthConfig) {
                long timeNeeded = ((PersistentGrowthConfig) config).getTotalGrowthTimeNeeded(currentBiome);
                if (biomeMultiplier == 0) {
                    lore.add(CivMenu.stat("Growth time", Component.text("Never", CivText.ERROR)));
                } else {
                    lore.add(CivMenu.stat("Growth time", TextUtil.formatDuration(timeNeeded, TimeUnit.MILLISECONDS)));
                    is.editMeta(itemMeta -> itemMeta.setEnchantmentGlintOverride(true));
                }
            } else {
                double baseMultiplier = config.getNaturalProgressChance(currentBiome);
                if (baseMultiplier == 0) {
                    lore.add(CivMenu.stat("Growth chance", Component.text("0%", CivText.ERROR)));
                } else {
                    lore.add(CivMenu.stat("Growth chance", decimalFormat.format(baseMultiplier * 100) + "%"));
                    is.editMeta(itemMeta -> itemMeta.setEnchantmentGlintOverride(true));
                }
            }
            lore.add(CivMenu.stat("Biome multiplier", String.valueOf(biomeMultiplier)));
            if (plant.getAllowBoneMeal() || !plant.getNeedsLight()
                || !plant.getSoilBoniPerLevel().isEmpty() || plant.getMaximumSoilLayers() != 0
                || plant.getMaximumSoilBonus() != Integer.MAX_VALUE || !plant.getGreenHouseRates().isEmpty()) {
                lore.add(Component.empty());
            }
            if (plant.getAllowBoneMeal()) {
                lore.add(CivMenu.stat("Bone meal", "Allowed"));
            }
            if (!plant.getNeedsLight()) {
                lore.add(CivMenu.stat("Needs light", "No"));
            }
            for (Map.Entry<Material, Double> entry : plant.getSoilBoniPerLevel().entrySet()) {
                lore.add(CivMenu.stat("Soil bonus", material(entry.getKey(), entry.getValue())));
            }
            if (plant.getMaximumSoilLayers() != 0) {
                lore.add(CivMenu.stat("Max soil layers", String.valueOf(plant.getMaximumSoilLayers())));
            }
            if (plant.getMaximumSoilBonus() < Integer.MAX_VALUE) {
                lore.add(CivMenu.stat("Max soil bonus", decimalFormat.format(plant.getMaximumSoilBonus())));
            }
            for (Map.Entry<Material, Double> entry : plant.getGreenHouseRates().entrySet()) {
                lore.add(CivMenu.stat("Greenhouse rate", material(entry.getKey(), entry.getValue())));
            }
            CivMenu.lore(is, lore.toArray(new Component[0]));
            IClickable click = new DecorationStack(is);
            clicks.add(click);
        }
        Scrollbar middleBar = new Scrollbar(
            clicks, 45, 5, ContentAligners.getCenteredInOrder(clicks.size(), 45, 9));
        inventory.addComponent(middleBar, SlotPredicates.rows(5));
        StaticDisplaySection bottomLine = new StaticDisplaySection(9);
        inventory.addComponent(bottomLine, SlotPredicates.rows(1));
        inventory.show();
    }

    private static Component material(Material material, double amount) {
        TextComponent.Builder text = Component.text();
        Component icon = CivText.icon(material);
        if (icon != null) {
            text.append(icon).append(Component.space());
        }
        return text.append(Component.translatable(material, CivText.VALUE))
            .append(CivText.info(" (" + decimalFormat.format(amount) + ")"))
            .build();
    }
}
