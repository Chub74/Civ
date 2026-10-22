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
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.Recipe;

/**
 * Removes recipes from the server by key, eg: "minecraft:diamond_sword" or "netherite_scrap_from_blasting".
 * Works for every recipe type (crafting, smelting, smithing, stonecutting...) and also hides them from the
 * recipe book. Removed recipes are put back when the hack is disabled.
 */
public final class DisabledRecipes extends BasicHack {

    private final List<NamespacedKey> disabled = new ArrayList<>();
    private final Map<NamespacedKey, Recipe> removed = new LinkedHashMap<>();

    public DisabledRecipes(final SimpleAdminHacks plugin, final BasicHackConfig config) {
        super(plugin, config);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.disabled.clear();
        for (final String raw : config().getBase().getStringList("recipes")) {
            final NamespacedKey key = NamespacedKey.fromString(raw.toLowerCase(Locale.ROOT));
            if (key == null) {
                plugin().warning("DisabledRecipes: invalid recipe key '" + raw + "'");
                continue;
            }
            this.disabled.add(key);
        }
        removeRecipes();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        for (final Map.Entry<NamespacedKey, Recipe> entry : this.removed.entrySet()) {
            if (Bukkit.getRecipe(entry.getKey()) != null) {
                continue;
            }
            try {
                Bukkit.addRecipe(entry.getValue(), false);
            } catch (final Exception exception) {
                plugin().warning("DisabledRecipes: failed to restore recipe " + entry.getKey(), exception);
            }
        }
        this.removed.clear();
        Bukkit.updateRecipes();
    }

    // /minecraft:reload brings vanilla recipes back
    @EventHandler
    public void on(final ServerResourcesReloadedEvent event) {
        removeRecipes();
    }

    private void removeRecipes() {
        int count = 0;
        for (final NamespacedKey key : this.disabled) {
            final Recipe recipe = Bukkit.getRecipe(key);
            if (recipe == null) {
                if (!this.removed.containsKey(key)) {
                    plugin().warning("DisabledRecipes: no recipe found for " + key);
                }
                continue;
            }
            if (Bukkit.removeRecipe(key, false)) {
                this.removed.put(key, recipe);
                count++;
            }
        }
        if (count > 0) {
            Bukkit.updateRecipes();
        }
        plugin().info("DisabledRecipes: removed " + count + " recipes");
    }

}
