package com.github.longboyy.repelshitters;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import vg.civcraft.mc.civmodcore.config.ConfigParser;
import vg.civcraft.mc.civmodcore.players.settings.AdminSetting;
import vg.civcraft.mc.civmodcore.players.settings.AdminSettingAPI;
import vg.civcraft.mc.civmodcore.players.settings.gui.AdminMenuSection;
import vg.civcraft.mc.civmodcore.players.settings.impl.BooleanSetting;
import vg.civcraft.mc.civmodcore.players.settings.impl.DoubleSetting;
import vg.civcraft.mc.civmodcore.players.settings.impl.DurationSetting;
import vg.civcraft.mc.civmodcore.players.settings.impl.IntegerSetting;
import java.time.Duration;
import java.util.Objects;

public class RepelShittersConfig extends ConfigParser {

    // Values admins can change live through /adminconfig
    private AdminSetting<Duration> inactivityTime;
    private AdminSetting<Duration> citadelGameTimeThreshold;
    private AdminSetting<Duration> citadelFirstJoinThreshold;
    private AdminSetting<Duration> damageGameTimeThreshold;
    private AdminSetting<Duration> damageFirstJoinThreshold;
    private AdminSetting<Double> citadelStartingScalar;
    private AdminSetting<Double> citadelGameTimeWeighting;
    private AdminSetting<Double> citadelFirstJoinWeighting;
    private AdminSetting<Double> damageStartingScalar;
    private AdminSetting<Double> damageGameTimeWeighting;
    private AdminSetting<Double> damageFirstJoinWeighting;

    private AdminSetting<Double> ghastBlocksPerSecond;
    private AdminSetting<Double> ghastMaxHealth;

    // Bastion interaction config
    private String cityBastionName;
    private String claimsBastionName;
    private AdminSetting<Integer> cityAltitudeCap;
    private AdminSetting<Double> citySpeedMultiplier;
    private AdminSetting<Double> repelStrength;
    private AdminSetting<Boolean> requireMaturity;

    public RepelShittersConfig(Plugin plugin) {
        super(plugin);
    }

    public long getInactivityTimeMillis() {
        return inactivityTime.getValue().toMillis();
    }

    public double getGhastBlocksPerSecond() {
        // sqrt(BLOCK_PER_SECOND/1440) = real speed
        return Math.sqrt(Math.max(0, ghastBlocksPerSecond.getValue()) / 1440);
    }

    public double getGhastMaxHealth() {
        return Math.max(1, ghastMaxHealth.getValue());
    }

    /**
     * @return Hash of the ghast attributes, which changes when admins change them so ghasts get updated
     */
    public int getGhastConfigHash(){
        return Objects.hash(getGhastBlocksPerSecond(), getGhastMaxHealth());
    }

    public CitadelConfig getCitadelConfig() {
        return new CitadelConfig(citadelStartingScalar.getValue(), threshold(citadelGameTimeThreshold),
            threshold(citadelFirstJoinThreshold), citadelGameTimeWeighting.getValue(),
            citadelFirstJoinWeighting.getValue());
    }

    public DamageConfig getDamageConfig() {
        return new DamageConfig(damageStartingScalar.getValue(), threshold(damageGameTimeThreshold),
            threshold(damageFirstJoinThreshold), damageGameTimeWeighting.getValue(),
            damageFirstJoinWeighting.getValue());
    }

    /**
     * @return Threshold in millis, at least 1 since players' progress is divided by it
     */
    private static long threshold(AdminSetting<Duration> setting) {
        return Math.max(1, setting.getValue().toMillis());
    }

    public String getCityBastionName() {
        return cityBastionName;
    }

    public String getClaimsBastionName() {
        return claimsBastionName;
    }

    public int getCityAltitudeCap() {
        return cityAltitudeCap.getValue();
    }

    public double getCitySpeedMultiplier() {
        return citySpeedMultiplier.getValue();
    }

    public double getRepelStrength() {
        return repelStrength.getValue();
    }

    public boolean isRequireMaturity() {
        return requireMaturity.getValue();
    }

    @Override
    protected boolean parseInternal(ConfigurationSection config) {
        // Ghast bastion interaction settings — all read live so /rsreload takes immediate effect
        var ghastSection = config.getConfigurationSection("ghast");
        if (ghastSection == null) {
            ghastSection = new MemoryConfiguration();
        }
        this.cityBastionName = ghastSection.getString("cityBastionName", "citybastion");
        this.claimsBastionName = ghastSection.getString("claimsBastionName", "claimbastion");

        registerAdminSettings();
        return true;
    }

    /**
     * Registering again on reload replaces the previous settings with the values in the reloaded file
     */
    private void registerAdminSettings() {
        AdminSettingAPI.setPluginIcon(plugin, new ItemStack(Material.HAPPY_GHAST_SPAWN_EGG));
        AdminMenuSection menu = AdminSettingAPI.getPluginMenu(plugin);
        this.inactivityTime = duration(menu, "inactivityTime", "Inactivity time", Duration.ofMinutes(5),
            Material.CLOCK, "How long after their last action players stop counting as active");

        AdminMenuSection ghasts = menu.getSubsection("Happy ghasts", "Ghast stats and how bastions hold them back",
            new ItemStack(Material.HAPPY_GHAST_SPAWN_EGG));
        this.ghastBlocksPerSecond = decimal(ghasts, "ghastBlocksPerSecond", "Speed", 7.0, Material.FEATHER,
            "Flying speed of happy ghasts in blocks per second");
        this.ghastMaxHealth = decimal(ghasts, "ghastMaxHealth", "Max health", 40.0, Material.GOLDEN_APPLE,
            "Max health of happy ghasts");
        this.cityAltitudeCap = AdminSettingAPI.registerSetting(new IntegerSetting((JavaPlugin) plugin, 3,
            "City altitude cap", "repelshitters.ghast.cityAltitudeCap", new ItemStack(Material.LADDER),
            "Blocks above the ground ghasts can hover in a city bastion", false), "ghast.cityAltitudeCap", ghasts);
        this.citySpeedMultiplier = decimal(ghasts, "ghast.citySpeedMultiplier", "City speed multiplier", 0.3,
            Material.SOUL_SAND, "Fraction of normal speed in a city bastion, 0.3 is 30%");
        this.repelStrength = decimal(ghasts, "ghast.repelStrength", "Repel strength", 0.45, Material.SLIME_BALL,
            "How hard ghasts bounce off claim bastions, lower reduces rubberbanding");
        this.requireMaturity = AdminSettingAPI.registerSetting(new BooleanSetting((JavaPlugin) plugin, false,
            "Require maturity", "repelshitters.ghast.requireMaturity",
            "Only mature bastions hold back ghasts"), "ghast.requireMaturity", ghasts);

        AdminMenuSection citadel = menu.getSubsection("Citadel", "Extra reinforcement damage by new players",
            new ItemStack(Material.STONE_BRICKS));
        this.citadelStartingScalar = decimal(citadel, "citadel.startingScalar", "Starting scalar", 4.0,
            Material.IRON_PICKAXE, "Reinforcement damage multiplier for brand new players, down to 1 with age");
        this.citadelGameTimeWeighting = decimal(citadel, "citadel.gameTimeWeighting", "Game time weighting", 0.5,
            Material.CLOCK, "Weight of play time in how old a player counts as");
        this.citadelFirstJoinWeighting = decimal(citadel, "citadel.firstJoinWeighting", "First join weighting", 0.5,
            Material.COMPASS, "Weight of time since first join in how old a player counts as");
        this.citadelGameTimeThreshold = duration(citadel, "citadel.gameTimeThreshold", "Game time threshold",
            Duration.ofHours(12), Material.CLOCK, "Play time after which players deal normal reinforcement damage");
        this.citadelFirstJoinThreshold = duration(citadel, "citadel.firstJoinThreshold", "First join threshold",
            Duration.ofHours(48), Material.COMPASS, "Time since first join after which players deal normal "
                + "reinforcement damage");

        AdminMenuSection damage = menu.getSubsection("Damage", "Extra damage dealt to new players",
            new ItemStack(Material.IRON_SWORD));
        this.damageStartingScalar = decimal(damage, "damage.startingScalar", "Starting scalar", 2.0,
            Material.IRON_SWORD, "Damage scalar for brand new players, down to 1 with age");
        this.damageGameTimeWeighting = decimal(damage, "damage.gameTimeWeighting", "Game time weighting", 0.5,
            Material.CLOCK, "Weight of play time in how old a player counts as");
        this.damageFirstJoinWeighting = decimal(damage, "damage.firstJoinWeighting", "First join weighting", 0.5,
            Material.COMPASS, "Weight of time since first join in how old a player counts as");
        this.damageGameTimeThreshold = duration(damage, "damage.gameTimeThreshold", "Game time threshold",
            Duration.ofHours(12), Material.CLOCK, "Play time after which players take normal damage");
        this.damageFirstJoinThreshold = duration(damage, "damage.firstJoinThreshold", "First join threshold",
            Duration.ofHours(48), Material.COMPASS, "Time since first join after which players take normal damage");
    }

    private AdminSetting<Duration> duration(AdminMenuSection menu, String path, String name, Duration defaultValue,
                                            Material icon, String description) {
        return AdminSettingAPI.registerSetting(new DurationSetting((JavaPlugin) plugin, defaultValue, name,
            "repelshitters." + path, new ItemStack(icon), description), path, menu);
    }

    private AdminSetting<Double> decimal(AdminMenuSection menu, String path, String name, double defaultValue,
                                         Material icon, String description) {
        return AdminSettingAPI.registerSetting(new DoubleSetting((JavaPlugin) plugin, defaultValue, name,
            "repelshitters." + path, new ItemStack(icon), description), path, menu);
    }
}
