package com.programmerdan.minecraft.simpleadminhacks.hacks.basic;

import com.programmerdan.minecraft.simpleadminhacks.SimpleAdminHacks;
import com.programmerdan.minecraft.simpleadminhacks.framework.BasicHack;
import com.programmerdan.minecraft.simpleadminhacks.framework.BasicHackConfig;
import io.papermc.paper.event.server.ServerResourcesReloadedEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.BlastingRecipe;
import org.bukkit.inventory.CampfireRecipe;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.SmokingRecipe;
import org.bukkit.inventory.StonecuttingRecipe;
import vg.civcraft.mc.civmodcore.inventory.items.MaterialUtils;

/**
 * Adds recipes defined in config. Supported types: shaped, shapeless, furnace, blasting, smoking, campfire and
 * stonecutting. An ingredient is a material ("IRON_INGOT"), an item tag ("#minecraft:planks"), or a list of those
 * when any of them is accepted.
 */
public final class CustomRecipes extends BasicHack {

    private final Map<NamespacedKey, Recipe> recipes = new LinkedHashMap<>();

    public CustomRecipes(final SimpleAdminHacks plugin, final BasicHackConfig config) {
        super(plugin, config);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.recipes.clear();
        final ConfigurationSection section = config().getBase().getConfigurationSection("recipes");
        if (section != null) {
            for (final String id : section.getKeys(false)) {
                final ConfigurationSection recipeSection = section.getConfigurationSection(id);
                if (recipeSection == null) {
                    continue;
                }
                final NamespacedKey key = new NamespacedKey(plugin(), id.toLowerCase(Locale.ROOT));
                try {
                    this.recipes.put(key, parseRecipe(key, recipeSection));
                } catch (final IllegalArgumentException exception) {
                    plugin().warning("CustomRecipes: skipping recipe '" + id + "': " + exception.getMessage());
                }
            }
        }
        registerRecipes();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        for (final NamespacedKey key : this.recipes.keySet()) {
            Bukkit.removeRecipe(key, false);
        }
        this.recipes.clear();
        Bukkit.updateRecipes();
    }

    @EventHandler
    public void on(final ServerResourcesReloadedEvent event) {
        registerRecipes();
    }

    private void registerRecipes() {
        int count = 0;
        for (final Map.Entry<NamespacedKey, Recipe> entry : this.recipes.entrySet()) {
            if (Bukkit.getRecipe(entry.getKey()) != null) {
                continue;
            }
            try {
                if (Bukkit.addRecipe(entry.getValue(), false)) {
                    count++;
                }
            } catch (final Exception exception) {
                plugin().warning("CustomRecipes: failed to register recipe " + entry.getKey(), exception);
            }
        }
        if (count > 0) {
            Bukkit.updateRecipes();
        }
        plugin().info("CustomRecipes: registered " + count + " recipes");
    }

    private static Recipe parseRecipe(final NamespacedKey key, final ConfigurationSection config) {
        final String type = config.getString("type", "").toLowerCase(Locale.ROOT);
        final ItemStack result = parseResult(config);
        final float experience = (float) config.getDouble("experience", 0);
        return switch (type) {
            case "shaped" -> parseShaped(key, result, config);
            case "shapeless" -> {
                final ShapelessRecipe recipe = new ShapelessRecipe(key, result);
                final List<?> ingredients = config.getList("ingredients");
                if (ingredients == null || ingredients.isEmpty() || ingredients.size() > 9) {
                    throw new IllegalArgumentException("shapeless recipes need 1 to 9 ingredients");
                }
                for (final Object ingredient : ingredients) {
                    recipe.addIngredient(parseChoice(ingredient));
                }
                yield recipe;
            }
            case "furnace" -> new FurnaceRecipe(key, result, parseChoice(config.get("input")),
                experience, config.getInt("cookingTime", 200));
            case "blasting" -> new BlastingRecipe(key, result, parseChoice(config.get("input")),
                experience, config.getInt("cookingTime", 100));
            case "smoking" -> new SmokingRecipe(key, result, parseChoice(config.get("input")),
                experience, config.getInt("cookingTime", 100));
            case "campfire" -> new CampfireRecipe(key, result, parseChoice(config.get("input")),
                experience, config.getInt("cookingTime", 600));
            case "stonecutting" -> new StonecuttingRecipe(key, result, parseChoice(config.get("input")));
            default -> throw new IllegalArgumentException("unknown type '" + type + "'");
        };
    }

    private static ShapedRecipe parseShaped(final NamespacedKey key, final ItemStack result,
                                            final ConfigurationSection config) {
        final List<String> shape = config.getStringList("shape");
        if (shape.isEmpty() || shape.size() > 3) {
            throw new IllegalArgumentException("shaped recipes need a shape of 1 to 3 rows");
        }
        final ConfigurationSection ingredients = config.getConfigurationSection("ingredients");
        if (ingredients == null) {
            throw new IllegalArgumentException("shaped recipes need an ingredients section");
        }
        final ShapedRecipe recipe = new ShapedRecipe(key, result).shape(shape.toArray(String[]::new));
        for (final String symbol : ingredients.getKeys(false)) {
            if (symbol.length() != 1) {
                throw new IllegalArgumentException("ingredient symbol '" + symbol + "' must be one character");
            }
            recipe.setIngredient(symbol.charAt(0), parseChoice(ingredients.get(symbol)));
        }
        return recipe;
    }

    private static ItemStack parseResult(final ConfigurationSection config) {
        final ConfigurationSection result = config.getConfigurationSection("result");
        final String materialName = result == null ? config.getString("result") : result.getString("material");
        final Material material = MaterialUtils.getMaterial(materialName);
        if (material == null || !material.isItem() || material.isAir()) {
            throw new IllegalArgumentException("invalid result material '" + materialName + "'");
        }
        final int amount = result == null ? 1 : result.getInt("amount", 1);
        return new ItemStack(material, amount);
    }

    private static RecipeChoice parseChoice(final Object value) {
        final List<Material> materials = new ArrayList<>();
        if (value instanceof List<?> list) {
            for (final Object entry : list) {
                addMaterials(materials, String.valueOf(entry));
            }
        } else if (value != null) {
            addMaterials(materials, value.toString());
        }
        if (materials.isEmpty()) {
            throw new IllegalArgumentException("missing ingredient");
        }
        return new RecipeChoice.MaterialChoice(materials);
    }

    private static void addMaterials(final List<Material> materials, final String raw) {
        if (raw.startsWith("#")) {
            final NamespacedKey tagKey = NamespacedKey.fromString(raw.substring(1).toLowerCase(Locale.ROOT));
            final Tag<Material> tag = tagKey == null ? null : Bukkit.getTag(Tag.REGISTRY_ITEMS, tagKey, Material.class);
            if (tag == null) {
                throw new IllegalArgumentException("unknown item tag '" + raw + "'");
            }
            materials.addAll(tag.getValues());
            return;
        }
        final Material material = MaterialUtils.getMaterial(raw);
        if (material == null || !material.isItem() || material.isAir()) {
            throw new IllegalArgumentException("invalid ingredient material '" + raw + "'");
        }
        materials.add(material);
    }

}
