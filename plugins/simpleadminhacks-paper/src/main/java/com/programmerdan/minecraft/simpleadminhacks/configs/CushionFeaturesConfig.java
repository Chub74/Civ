package com.programmerdan.minecraft.simpleadminhacks.configs;

import com.programmerdan.minecraft.simpleadminhacks.SimpleAdminHacks;
import com.programmerdan.minecraft.simpleadminhacks.framework.SimpleHackConfig;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import vg.civcraft.mc.civmodcore.utilities.CivLogger;

public final class CushionFeaturesConfig extends SimpleHackConfig {

    private final CivLogger logger;

    private boolean disableSittingInCombat;
    private boolean dismountOnCombatTag;

    private long mountCooldown;

    private boolean blockDismountThroughWalls;

    private String inCombatMessage;
    private String cooldownMessage;

    public CushionFeaturesConfig(@NotNull final SimpleAdminHacks plugin,
                                 @NotNull final ConfigurationSection base) {
        super(plugin, base, false);
        this.logger = CivLogger.getLogger(getClass());
        wireup(base);
    }

    @Override
    protected void wireup(@NotNull final ConfigurationSection config) {
        this.disableSittingInCombat = config.getBoolean("disableSittingInCombat", true);

        this.dismountOnCombatTag = config.getBoolean("dismountOnCombatTag", true);

        this.mountCooldown = config.getLong("mountCooldown", 1000L);
        if (this.mountCooldown < 0L) {
            this.logger.warning("[mountCooldown] was set to [" + this.mountCooldown + "], " +
                "which is invalid, defaulting to: 1000L");
            this.mountCooldown = 1000L;
        }

        this.blockDismountThroughWalls = config.getBoolean("blockDismountThroughWalls", true);

        this.inCombatMessage = config.getString("messages.inCombat", "");

        this.cooldownMessage = config.getString("messages.cooldown", "");
    }

    public boolean isSittingDisabledInCombat() {
        return this.disableSittingInCombat;
    }

    public boolean isDismountOnCombatTag() {
        return this.dismountOnCombatTag;
    }

    public long getMountCooldown() {
        return this.mountCooldown;
    }

    public boolean isDismountThroughWallsBlocked() {
        return this.blockDismountThroughWalls;
    }

    public String getInCombatMessage() {
        return this.inCombatMessage;
    }

    public String getCooldownMessage() {
        return this.cooldownMessage;
    }

}
