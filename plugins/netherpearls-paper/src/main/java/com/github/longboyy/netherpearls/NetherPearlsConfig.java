package com.github.longboyy.netherpearls;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import vg.civcraft.mc.civmodcore.config.ConfigParser;
import vg.civcraft.mc.civmodcore.players.settings.AdminSetting;
import vg.civcraft.mc.civmodcore.players.settings.AdminSettingAPI;
import vg.civcraft.mc.civmodcore.players.settings.AdminSettingMap;
import vg.civcraft.mc.civmodcore.players.settings.gui.AdminMenuSection;
import vg.civcraft.mc.civmodcore.players.settings.impl.DoubleSetting;
import vg.civcraft.mc.civmodcore.players.settings.impl.IntegerSetting;

/**
 * Values are admin settings, so they can be changed live through /adminconfig
 */
public class NetherPearlsConfig extends ConfigParser {

    private AdminSettingMap<EntityType, Double> entityDropChances;
    private AdminSetting<Integer> minDrop;
    private AdminSetting<Integer> maxDrop;

    public NetherPearlsConfig(Plugin plugin) {
        super(plugin);
    }

    /**
     * @return Live drop chances, null for mobs which do not drop pearls
     */
    public AdminSettingMap<EntityType, Double> getEntityDropChances() {
        return this.entityDropChances;
    }

    public int getMinDrop() {
        return this.minDrop.getValue();
    }

    public int getMaxDrop() {
        return this.maxDrop.getValue();
    }

    @Override
    protected boolean parseInternal(ConfigurationSection config) {
        JavaPlugin javaPlugin = (JavaPlugin) plugin;
        AdminSettingAPI.setPluginIcon(plugin, new ItemStack(Material.ENDER_PEARL));
        this.minDrop = AdminSettingAPI.registerSetting(new IntegerSetting(javaPlugin, 1, "Min drop",
            "netherpearls.minDrop", new ItemStack(Material.ENDER_PEARL), "Least pearls dropped at once", false),
            "minDrop");
        this.maxDrop = AdminSettingAPI.registerSetting(new IntegerSetting(javaPlugin, 2, "Max drop",
            "netherpearls.maxDrop", new ItemStack(Material.ENDER_PEARL), "Most pearls dropped at once", false),
            "maxDrop");
        AdminMenuSection dropMenu = AdminSettingAPI.getPluginMenu(plugin).getSubsection("Drop chances",
            "Chance per mob to drop pearls when killed by a player", new ItemStack(Material.ENDER_PEARL));
        this.entityDropChances = AdminSettingAPI.registerMap(javaPlugin, "netherpearls.dropChances", "dropChances",
            dropMenu,
            key -> EnumUtils.getEnum(EntityType.class, key.toUpperCase(Locale.ROOT)),
            Enum::name,
            (type, identifier) -> {
                Material egg = Material.matchMaterial(type.name() + "_SPAWN_EGG");
                return new DoubleSetting(javaPlugin, 0.05, niceName(type), identifier,
                    new ItemStack(egg == null ? Material.ENDER_PEARL : egg), "Chance from 0 to 1 to drop pearls");
            });
        return true;
    }

    /**
     * @return ZOMBIFIED_PIGLIN as Zombified Piglin
     */
    private static String niceName(EntityType type) {
        return Arrays.stream(type.name().toLowerCase(Locale.ROOT).split("_"))
            .map(StringUtils::capitalize)
            .collect(Collectors.joining(" "));
    }
}
