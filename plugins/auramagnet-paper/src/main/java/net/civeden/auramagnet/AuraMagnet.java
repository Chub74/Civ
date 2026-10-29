package net.civeden.auramagnet;

import net.civeden.auramagnet.commands.AuraMagnetCommand;
import net.civeden.auramagnet.listeners.HoeTagListener;
import net.civeden.auramagnet.settings.MagnetHoeSettings;
import net.civeden.auramagnet.tasks.MagnetScanTask;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import vg.civcraft.mc.civmodcore.ACivMod;
import vg.civcraft.mc.civmodcore.players.settings.AdminSetting;
import vg.civcraft.mc.civmodcore.players.settings.AdminSettingAPI;
import vg.civcraft.mc.civmodcore.players.settings.impl.DoubleSetting;
import vg.civcraft.mc.civmodcore.players.settings.impl.IntegerSetting;

public class AuraMagnet extends ACivMod {

    private static AuraMagnet instance;

    public static NamespacedKey MAGNET_HOE_KEY;

    private MagnetHoeSettings magnetHoeSettings;
    // Admins can change these live through /adminconfig
    private AdminSetting<Double> magnetRadius;
    private AdminSetting<Integer> durabilityCostPerItem;
    private AdminSetting<Double> durabilityChancePerItem;
    private String magnetLoreTrigger;

    @Override
    public void onEnable() {
        super.onEnable();
        instance = this;
        saveDefaultConfig();

        MAGNET_HOE_KEY = new NamespacedKey(this, "aura_magnet_hoe");

        loadConfigValues();
        magnetHoeSettings = new MagnetHoeSettings(this);

        registerListener(new HoeTagListener());

        AuraMagnetCommand command = new AuraMagnetCommand();
        getCommand("auramagnet").setExecutor(command);
        getCommand("auramagnet").setTabCompleter(command);

        new MagnetScanTask().runTaskTimer(this, 10L, 5L);
    }

    private void loadConfigValues() {
        reloadConfig();
        AdminSettingAPI.setPluginIcon(this, new ItemStack(Material.GOLDEN_HOE));
        magnetRadius = AdminSettingAPI.registerSetting(new DoubleSetting(this, 8.0, "Magnet radius",
            "auramagnet.magnet-radius", new ItemStack(Material.LODESTONE),
            "Blocks around the player the hoe pulls items from"), "magnet-radius");
        durabilityCostPerItem = AdminSettingAPI.registerSetting(new IntegerSetting(this, 1, "Durability cost",
            "auramagnet.durability-cost-per-item", new ItemStack(Material.ANVIL),
            "Durability taken from the hoe when an item costs durability, 0 never wears it", false),
            "durability-cost-per-item");
        durabilityChancePerItem = AdminSettingAPI.registerSetting(new DoubleSetting(this, 1.0, "Durability chance",
            "auramagnet.durability-chance-per-item", new ItemStack(Material.RABBIT_FOOT),
            "Chance from 0 to 1 that a pulled item costs durability"), "durability-chance-per-item");
        magnetLoreTrigger = getConfig().getString("magnet-lore-trigger", "&6Aura Magnet Hoe");
    }

    public static AuraMagnet getInstance() {
        return instance;
    }

    public MagnetHoeSettings getMagnetHoeSettings() {
        return magnetHoeSettings;
    }

    public double getMagnetRadius() {
        return Math.max(0, magnetRadius.getValue());
    }

    public int getDurabilityCostPerItem() {
        // A negative cost would repair the hoe
        return Math.max(0, durabilityCostPerItem.getValue());
    }

    public double getDurabilityChancePerItem() {
        return durabilityChancePerItem.getValue();
    }

    public String getMagnetLoreTrigger() {
        return magnetLoreTrigger;
    }
}
