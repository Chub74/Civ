package com.github.longboyy.repelshitters;

import org.bukkit.configuration.ConfigurationSection;
import vg.civcraft.mc.civmodcore.config.ConfigHelper;

public record DamageConfig(double startingScalar, long gameTimeThreshold, long firstJoinThreshold, double gameTimeWeight, double firstTimeWeight) {

    public static DamageConfig parse(ConfigurationSection section){
        double startingScalar = section.getDouble("startingScalar", 2.0D);
        long gameTimeThreshold = ConfigHelper.parseTime(section.getString("gameTimeThreshold", "12h"));
        long firstJoinThreshold = ConfigHelper.parseTime(section.getString("firstJoinThreshold", "48h"));
        double gameTimeWeighting = section.getDouble("gameTimeWeighting", 0.5D);
        double firstJoinWeighting = section.getDouble("firstJoinWeighting", 0.5D);
        return new DamageConfig(startingScalar, gameTimeThreshold, firstJoinThreshold, gameTimeWeighting, firstJoinWeighting);
    }
}
