package com.github.maxopoly.essenceglue;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import vg.civcraft.mc.civmodcore.ACivMod;
import vg.civcraft.mc.civmodcore.config.ConfigHelper;
import vg.civcraft.mc.civmodcore.config.ConfigParser;
import vg.civcraft.mc.civmodcore.dao.DatabaseCredentials;
import vg.civcraft.mc.civmodcore.dao.ManagedDatasource;
import vg.civcraft.mc.civmodcore.inventory.items.ItemMap;
import vg.civcraft.mc.civmodcore.players.settings.AdminSetting;
import vg.civcraft.mc.civmodcore.players.settings.AdminSettingAPI;
import vg.civcraft.mc.civmodcore.players.settings.impl.BooleanSetting;
import vg.civcraft.mc.civmodcore.players.settings.impl.BoundedIntegerSetting;
import vg.civcraft.mc.civmodcore.players.settings.impl.DurationSetting;
import vg.civcraft.mc.civmodcore.players.settings.impl.IntegerSetting;

public class EssenceConfigManager extends ConfigParser {

    private ManagedDatasource db;
    // Admins can change these live through /adminconfig
    private AdminSetting<Integer> maxStreak;
    private AdminSetting<Duration> streakDelay;
    private AdminSetting<Duration> streakGracePeriod;
    private AdminSetting<Duration> timeForGain;
    private ItemMap rewards;
    private AdminSetting<Integer> loginReward;
    private AdminSetting<Integer> votingReward;
    private Map<String, VotingSite> votingCooldowns;
    private AdminSetting<Boolean> giveRewardToPearled;
    private AdminSetting<Boolean> multiplyPearlCost;
    private boolean physical;
    private AdminSetting<Integer> virtualCap;

    public EssenceConfigManager(ACivMod plugin) {
        super(plugin);
    }

    @Override
    protected boolean parseInternal(ConfigurationSection config) {
        if (config.contains("database")) {
            db = ManagedDatasource.construct((ACivMod) plugin, (DatabaseCredentials) config.get("database"));
        }
        rewards = ConfigHelper.parseItemMap(config.getConfigurationSection("rewards"));
        physical = config.getBoolean("physical", true);
        votingCooldowns = new LinkedHashMap<>();
        if (config.isConfigurationSection("voting_sites")) {
            ConfigurationSection votingKeySection = config.getConfigurationSection("voting_sites");
            for (String key : votingKeySection.getKeys(false)) {
                if (votingKeySection.isConfigurationSection(key)) {
                    ConfigurationSection current = votingKeySection.getConfigurationSection(key);
                    long votingCooldown = ConfigHelper.parseTime(current.getString("voting_cooldown", "20h"));
                    String votingUrl = current.getString("voting_url");
                    String internalKey = current.getString("internal_key");
                    String name = current.getString("name");
                    votingCooldowns.put(internalKey, new VotingSite(name, votingUrl, internalKey, votingCooldown));
                } else {
                    logger.warning("Ignoring invalid entry " + key + " at  " + votingKeySection.getCurrentPath());
                }
            }
        }
        registerAdminSettings();
        return true;
    }

    private void registerAdminSettings() {
        JavaPlugin javaPlugin = (JavaPlugin) plugin;
        AdminSettingAPI.setPluginIcon(plugin, new ItemStack(Material.ENDER_EYE));
        // Streaks are stored as the bits of an int, so at most 31 days fit
        maxStreak = AdminSettingAPI.registerSetting(new BoundedIntegerSetting(javaPlugin, 8, "Max streak",
            "essenceglue.max_streak", new ItemStack(Material.BLAZE_POWDER), "Most days a login streak counts", false,
            1, 31), "max_streak");
        streakDelay = duration("streak_delay", "Streak delay", Duration.ofHours(20), Material.CLOCK,
            "Time between two login rewards");
        streakGracePeriod = duration("streak_grace_period", "Streak grace period", Duration.ofDays(1),
            Material.COMPASS, "Time after the streak delay a player can miss before their streak drops");
        timeForGain = duration("online_for_reward", "Online for reward", Duration.ofMinutes(30), Material.SUNFLOWER,
            "Time online needed each day for the login reward, counted in whole minutes");
        loginReward = integer("login_reward", "Login reward", 1, Material.ENDER_EYE,
            "Essence per streak day for logging in");
        votingReward = integer("voting_reward", "Voting reward", 1, Material.PAPER, "Essence per vote");
        virtualCap = integer("virtual_cap", "Virtual cap", 64, Material.CHEST,
            "Most virtual essence a player can hold, only used if essence isn't physical");
        giveRewardToPearled = AdminSettingAPI.registerSetting(new BooleanSetting(javaPlugin, false,
            "Reward pearled players", "essenceglue.give_reward_to_pearled",
            "Players with pearled alts still get login rewards"), "give_reward_to_pearled");
        multiplyPearlCost = AdminSettingAPI.registerSetting(new BooleanSetting(javaPlugin, true,
            "Multiply pearl cost", "essenceglue.multiply_pearl_cost",
            "Pearls of players with a streak decay faster, by half per streak day"), "multiply_pearl_cost");
    }

    private AdminSetting<Duration> duration(String path, String name, Duration defaultValue, Material icon,
                                            String description) {
        return AdminSettingAPI.registerSetting(new DurationSetting((JavaPlugin) plugin, defaultValue, name,
            "essenceglue." + path, new ItemStack(icon), description), path);
    }

    private AdminSetting<Integer> integer(String path, String name, int defaultValue, Material icon,
                                          String description) {
        return AdminSettingAPI.registerSetting(new IntegerSetting((JavaPlugin) plugin, defaultValue, name,
            "essenceglue." + path, new ItemStack(icon), description, false), path);
    }

    public ManagedDatasource getDatabase() {
        return db;
    }

    public Map<String, VotingSite> getVotingCooldowns() {
        return votingCooldowns;
    }

    public boolean giveRewardToPearled() {
        return giveRewardToPearled.getValue();
    }

    public boolean multiplyPearlCost() {
        return multiplyPearlCost.getValue();
    }

    public ItemMap getRewards() {
        return rewards;
    }

    public int getLoginReward() {
        return Math.max(0, loginReward.getValue());
    }

    public int getVotingReward() {
        return Math.max(0, votingReward.getValue());
    }

    public int getMaxStreak() {
        return maxStreak.getValue();
    }

    public long getStreakDelay() {
        return streakDelay.getValue().toMillis();
    }

    public long getStreakGracePeriod() {
        return streakGracePeriod.getValue().toMillis();
    }

    public long getOnlineTimeForReward() {
        return timeForGain.getValue().toMillis();
    }

    public boolean isPhysical() {
        return physical;
    }

    public int getVirtualCap() {
        return Math.max(0, virtualCap.getValue());
    }
}
