package net.civeden.civadvancements;

import java.util.List;
import java.util.Map;
import org.bukkit.configuration.file.FileConfiguration;

public class CivAdvancementsConfig {

    private final FileConfiguration config;

    public CivAdvancementsConfig(FileConfiguration config) {
        this.config = config;
    }

    // --- Welcome ---

    public boolean isWelcomeEnabled() {
        return config.getBoolean("welcome.enabled", true);
    }

    public String getWelcomeTitle() {
        return config.getString("welcome.title", "<gold><bold>Welcome to Eden!</bold></gold>");
    }

    public String getWelcomeSubtitle() {
        return config.getString("welcome.subtitle", "<yellow>Press L to open your Tutorial</yellow>");
    }

    public List<String> getWelcomeMessages() {
        return config.getStringList("welcome.messages");
    }

    // --- Per-advancement text ---

    public String getAdvancementTitle(String key) {
        return config.getString("advancements." + key + ".title", key);
    }

    public String getAdvancementDescription(String key) {
        return config.getString("advancements." + key + ".description", "");
    }

    /**
     * Returns the configured parent key for this advancement.
     * Used to substitute {{parent}} in JSON templates.
     * Root advancement has no parent — callers should skip it.
     */
    public String getAdvancementParent(String key) {
        return config.getString("advancements." + key + ".parent", "joined");
    }

    /**
     * Returns the Citadel reinforcement type name that triggers this advancement.
     * Matches the 'name:' field in Citadel's config.yml reinforcement entries.
     */
    public String getCitadelTypeName(String key) {
        return config.getString("advancements." + key + ".citadel_type_name", "");
    }

    /**
     * Returns the FactoryMod factory name that triggers the factory_create advancement.
     * Matches Factory.getName() at runtime.
     */
    public String getFactoryCreateName() {
        return config.getString("advancements.factory_create.factory_name", "");
    }

    public boolean getAdvancementHidden(String key) {
        return config.getBoolean("advancements." + key + ".hidden", false);
    }

    public List<Map<?, ?>> getRewardItems(String key) {
        return config.getMapList("advancements." + key + ".reward.items");
    }

    public String getBastionTypeName(String key) {
        return config.getString("advancements." + key + ".bastion_type_name", "");
    }

    public String getSnitchTypeName(String key) {
        return config.getString("advancements." + key + ".snitch_type_name", "");
    }

    public int getBrewQualityThreshold() {
        return config.getInt("advancements.brew_quality.quality_threshold", 7);
    }

    public int getEmfLegendaryCount() {
        return config.getInt("advancements.emf_legendary.count", 50);
    }

    public String getOreMaterial(String key) {
        return config.getString("advancements." + key + ".ore_material", "");
    }
}
