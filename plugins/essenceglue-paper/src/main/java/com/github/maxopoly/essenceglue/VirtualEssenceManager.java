package com.github.maxopoly.essenceglue;

import org.bukkit.plugin.java.JavaPlugin;
import vg.civcraft.mc.civmodcore.players.settings.PlayerSettingAPI;
import vg.civcraft.mc.civmodcore.players.settings.impl.IntegerSetting;
import java.util.UUID;

public class VirtualEssenceManager {

    private final IntegerSetting playerEssence;
    private final EssenceConfigManager config;

    public VirtualEssenceManager(JavaPlugin plugin, EssenceConfigManager config) {
        playerEssence = new IntegerSetting(plugin, 0, "Player Essence", "playerEssence");
        this.config = config;
        PlayerSettingAPI.registerSetting(playerEssence, null);
    }

    public int getEssence(UUID playerId) {
        Integer value = playerEssence.getValue(playerId);
        return value == null ? 0 : value;
    }

    public void setEssence(UUID playerId, int essence) {
        playerEssence.setValue(playerId, Math.min(essence, getCap()));
    }

    /** Adds as much as fits under the cap and returns how much that was. */
    public int addEssence(UUID playerId, int amount) {
        int current = getEssence(playerId);
        int added = Math.max(0, Math.min(amount, getCap() - current));
        if (added > 0) {
            playerEssence.setValue(playerId, current + added);
        }
        return added;
    }

    /** Not capped, so lowering the cap in config never eats what a player already has. */
    public void removeEssence(UUID playerId, int amount) {
        playerEssence.setValue(playerId, Math.max(0, getEssence(playerId) - amount));
    }

    /**
     * @return Live cap, admins can change it
     */
    public int getCap() {
        return config.getVirtualCap();
    }

    public boolean isAtEssenceCapacity(UUID playerId) {
        return getEssence(playerId) >= getCap();
    }
}
