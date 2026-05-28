package com.github.longboyy.repelshitters;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.plugin.Plugin;
import vg.civcraft.mc.civmodcore.config.ConfigHelper;
import vg.civcraft.mc.civmodcore.config.ConfigParser;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class RepelShittersConfig extends ConfigParser {

    private long inactivityTimeMillis;

    private CitadelConfig citadelConfig;
    private DamageConfig damageConfig;

    private double ghastBlocksPerSecond;
    private double ghastMaxHealth;
    private int ghastConfigHash;

    // Bastion interaction config
    private String cityBastionName;
    private String claimsBastionName;
    private int cityAltitudeCap;
    private double citySpeedMultiplier;
    private double repelStrength;
    private boolean requireMaturity;

    public RepelShittersConfig(Plugin plugin) {
        super(plugin);
    }

    public long getInactivityTimeMillis() {
        return inactivityTimeMillis;
    }

    public double getGhastBlocksPerSecond() {
        return ghastBlocksPerSecond;
    }

    public double getGhastMaxHealth() {
        return ghastMaxHealth;
    }

    public int getGhastConfigHash(){
        return this.ghastConfigHash;
    }

    public CitadelConfig getCitadelConfig() {
        return citadelConfig;
    }

    public DamageConfig getDamageConfig() {
        return damageConfig;
    }

    public String getCityBastionName() {
        return cityBastionName;
    }

    public String getClaimsBastionName() {
        return claimsBastionName;
    }

    public int getCityAltitudeCap() {
        return cityAltitudeCap;
    }

    public double getCitySpeedMultiplier() {
        return citySpeedMultiplier;
    }

    public double getRepelStrength() {
        return repelStrength;
    }

    public boolean isRequireMaturity() {
        return requireMaturity;
    }

    @Override
    protected boolean parseInternal(ConfigurationSection config) {
        String timeString = config.getString("inactivityTime", "5m");
        this.inactivityTimeMillis = ConfigHelper.parseTime(timeString, TimeUnit.MILLISECONDS);
        // sqrt(BLOCK_PER_SECOND/1440) = real speed
        this.ghastBlocksPerSecond = Math.sqrt(config.getDouble("ghastBlocksPerSecond", 7.0)/1440);
        this.ghastMaxHealth = config.getDouble("ghastMaxHealth", 40.0);
        this.ghastConfigHash = Objects.hash(this.ghastBlocksPerSecond, this.ghastMaxHealth);
        var citadelConfigSection = config.getConfigurationSection("citadel");
        if(citadelConfigSection == null){
            citadelConfigSection = new MemoryConfiguration();
        }
        this.citadelConfig = CitadelConfig.parse(citadelConfigSection);

        var damageConfigSection = config.getConfigurationSection("damage");
        if(damageConfigSection == null){
            damageConfigSection = new MemoryConfiguration();
        }
        this.damageConfig = DamageConfig.parse(damageConfigSection);

        // Ghast bastion interaction settings — all read live so /rsreload takes immediate effect
        var ghastSection = config.getConfigurationSection("ghast");
        if (ghastSection == null) {
            ghastSection = new MemoryConfiguration();
        }
        this.cityBastionName = ghastSection.getString("cityBastionName", "citybastion");
        this.claimsBastionName = ghastSection.getString("claimsBastionName", "claimbastion");
        this.cityAltitudeCap = ghastSection.getInt("cityAltitudeCap", 3);
        this.citySpeedMultiplier = ghastSection.getDouble("citySpeedMultiplier", 0.3);
        this.repelStrength = ghastSection.getDouble("repelStrength", 0.45);
        this.requireMaturity = ghastSection.getBoolean("requireMaturity", false);

        return true;
    }
}
