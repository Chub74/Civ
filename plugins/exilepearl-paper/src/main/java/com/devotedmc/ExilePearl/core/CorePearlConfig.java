package com.devotedmc.ExilePearl.core;

import com.devotedmc.ExilePearl.ExileRule;
import com.devotedmc.ExilePearl.PearlLogger;
import com.devotedmc.ExilePearl.PearlType;
import com.devotedmc.ExilePearl.RepairMaterial;
import com.devotedmc.ExilePearl.config.Configurable;
import com.devotedmc.ExilePearl.config.Document;
import com.devotedmc.ExilePearl.config.DocumentConfig;
import com.devotedmc.ExilePearl.config.PearlConfig;
import com.devotedmc.ExilePearl.storage.StorageType;
import com.google.common.base.Preconditions;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import vg.civcraft.mc.civmodcore.players.settings.AdminSetting;
import vg.civcraft.mc.civmodcore.players.settings.AdminSettingAPI;
import vg.civcraft.mc.civmodcore.players.settings.gui.AdminMenuSection;
import vg.civcraft.mc.civmodcore.players.settings.impl.BooleanSetting;
import vg.civcraft.mc.civmodcore.players.settings.impl.BoundedIntegerSetting;
import vg.civcraft.mc.civmodcore.players.settings.impl.DoubleSetting;
import vg.civcraft.mc.civmodcore.players.settings.impl.IntegerSetting;

final class CorePearlConfig implements DocumentConfig, PearlConfig {

    private record RuleInfo(String path, boolean defaultValue, String name, String description) {
    }

    private static final Map<ExileRule, RuleInfo> RULES = new EnumMap<>(ExileRule.class);

    static {
        rule(ExileRule.DAMAGE_REINFORCEMENT, "damage_reinforcement", false, "Damage reinforcements",
            "Exiles can damage reinforced blocks");
        rule(ExileRule.DAMAGE_BASTION, "damage_bastion", false, "Damage bastions", "Exiles can damage bastions");
        rule(ExileRule.CREATE_BASTION, "create_bastion", false, "Create bastions", "Exiles can create bastions");
        rule(ExileRule.ENTER_BASTION, "enter_bastion", false, "Enter bastion fields",
            "Exiles can enter bastion fields they aren't a member of");
        rule(ExileRule.THROW_PEARL, "throw_pearl", false, "Throw pearls", "Exiles can throw ender pearls");
        rule(ExileRule.CHAT, "chat_local", false, "Local chat", "Exiles can chat locally");
        rule(ExileRule.PVP, "pvp", false, "PvP", "Exiles can harm other players");
        rule(ExileRule.KILL_PETS, "kill_pets", false, "Kill pets", "Exiles can harm pets");
        rule(ExileRule.KILL_MOBS, "kill_mobs", false, "Kill protected mobs", "Exiles can harm protected mobs");
        rule(ExileRule.IGNITE, "ignite", false, "Ignite", "Exiles can set things on fire");
        rule(ExileRule.USE_BUCKET, "use_bucket", false, "Empty buckets",
            "Exiles can empty buckets or move them to other inventories");
        rule(ExileRule.FILL_BUCKET, "fill_bucket", false, "Fill buckets", "Exiles can fill buckets");
        rule(ExileRule.MILK_COWS, "milk_cows", true, "Milk cows", "Exiles can milk cows");
        rule(ExileRule.FILL_CAULDRON, "fill_cauldron", true, "Fill cauldrons",
            "Exiles can fill cauldrons from water buckets");
        rule(ExileRule.USE_POTIONS, "use_potions", false, "Use potions", "Exiles can use potions");
        rule(ExileRule.DRINK_BREWS, "drink_brews", true, "Drink brews", "Exiles can drink Brewery brews");
        rule(ExileRule.USE_BED, "use_bed", false, "Use beds", "Exiles can use beds");
        rule(ExileRule.SPAWN_RESET, "spawn_reset", false, "Reset bed on death",
            "Reset an exile's bed when another player kills them");
        rule(ExileRule.SUICIDE, "suicide", false, "Suicide", "Exiles can use /suicide");
        rule(ExileRule.SNITCH, "place_snitch", false, "Place snitches", "Exiles can place jukeboxes and noteblocks");
        rule(ExileRule.MINE, "mine_blocks", true, "Mine blocks", "Exiles can break blocks");
        rule(ExileRule.BREW, "brew", true, "Brew", "Exiles can brew potions");
        rule(ExileRule.ENCHANT, "enchant", true, "Enchant", "Exiles can enchant items");
        rule(ExileRule.COLLECT_XP, "collect_xp", true, "Collect XP", "Exiles can collect xp orbs");
        rule(ExileRule.USE_ANVIL, "use_anvil", true, "Use anvils", "Exiles can use anvils");
        rule(ExileRule.PLACE_TNT, "place_tnt", false, "Place TNT", "Exiles can place TNT");
    }

    private static void rule(ExileRule rule, String key, boolean defaultValue, String name, String description) {
        RULES.put(rule, new RuleInfo("rules." + key, defaultValue, name, description));
    }

    private final Plugin plugin;
    private final PearlLogger logger;
    private JavaPlugin javaPlugin;
    private Document doc;
    private Set<Configurable> configurables = new HashSet<>();

    // Gameplay values admins can change live through /adminconfig, the rest is only read from the config file
    private AdminSetting<Integer> suicideTimeSeconds;
    private AdminSetting<Integer> maxPearledAlts;
    private AdminSetting<Boolean> canElytraWithPearls;
    private AdminSetting<Boolean> canTakeThroughPortals;
    private AdminSetting<Boolean> canPearledPlayersPearlOthers;
    private AdminSetting<Boolean> freeByThrowing;
    private AdminSetting<Boolean> freeTeleport;
    private AdminSetting<Boolean> hotbarNeeded;
    private AdminSetting<Boolean> announceExileLocation;
    private AdminSetting<Boolean> allowPearlStealing;
    private AdminSetting<Boolean> allowSummoning;
    private AdminSetting<Double> bastionHarmAmount;
    private AdminSetting<Integer> decayIntervalMin;
    private AdminSetting<Integer> decayAmount;
    private AdminSetting<Integer> decayTimeoutMin;
    private AdminSetting<Integer> startValue;
    private AdminSetting<Integer> maxValue;
    private AdminSetting<Integer> costMultiplierDays;
    private AdminSetting<Double> costMultiplierCapDays;
    private AdminSetting<Integer> damageLogAlgorithm;
    private AdminSetting<Integer> damageLogTickInterval;
    private AdminSetting<Double> damageLogDecayAmount;
    private AdminSetting<Double> damageLogMaxAmount;
    private AdminSetting<Double> damageLogPotionDamage;
    private AdminSetting<Integer> pearlRadius;
    private final Map<ExileRule, AdminSetting<Boolean>> rules = new EnumMap<>(ExileRule.class);

    public CorePearlConfig(final Plugin plugin, final PearlLogger logger) {
        Preconditions.checkNotNull(plugin, "plugin");
        Preconditions.checkNotNull(logger, "logger");

        this.plugin = plugin;
        this.logger = logger;

        doc = new Document(plugin.getConfig());
    }

    @Override
    public Document getDocument() {
        return this.doc;
    }

    @Override
    public void reload() {
        plugin.reloadConfig();
        doc = new Document(plugin.getConfig());
        // Registering again replaces the previous settings with the values in the reloaded file
        registerAdminSettings();
        loadConfigurables(false);
        logger.log("Configuration loaded.");
    }

    /**
     * @param tasksOnly Only reload the tasks, which cache values and schedule themselves from the config
     */
    private void loadConfigurables(boolean tasksOnly) {
        for (Configurable c : configurables) {
            if (tasksOnly && !(c instanceof ExilePearlTask)) {
                continue;
            }
            try {
                c.loadConfig(this);
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Failed to load configuration for class %s", c.getClass().getName());
                ex.printStackTrace();
            }
        }
    }

    /**
     * Writes values changed through /ep config set or /adminconfig to the config file
     */
    @Override
    public void saveToFile() {
        AdminSettingAPI.flush(AdminSettingAPI.getSettings(plugin.getName()));
    }

    @Override
    public void addConfigurable(Configurable configurable) {
        configurables.add(configurable);
    }

    private void registerAdminSettings() {
        // plugin is the ExilePearlCore wrapper, admin settings need the actual plugin owning the config file
        javaPlugin = JavaPlugin.getProvidingPlugin(CorePearlConfig.class);
        AdminSettingAPI.setPluginIcon(javaPlugin, new ItemStack(Material.ENDER_EYE));
        AdminMenuSection menu = AdminSettingAPI.getPluginMenu(javaPlugin);

        AdminMenuSection general = menu.getSubsection("General", "Suicide, alts and moving pearls around",
            new ItemStack(Material.ENDER_PEARL));
        suicideTimeSeconds = integer(general, "general.suicide_time_seconds", "Suicide time", 180, Material.CLOCK,
            "Seconds /suicide takes before the player dies", true);
        maxPearledAlts = integer(general, "general.max_pearled_alts", "Max pearled alts", 1, Material.PLAYER_HEAD,
            "Pearled accounts after which further BanStick alts can't log in", false);
        canElytraWithPearls = toggle(general, "general.can_elytra_with_pearls", "Elytra with pearls", true,
            "Players can glide while carrying a pearl");
        canTakeThroughPortals = toggle(general, "general.can_take_through_portals", "Pearls through portals", false,
            "Players can take pearls through portals");
        canPearledPlayersPearlOthers = toggle(general, "general.can_pearled_players_pearl_others",
            "Pearled players pearl others", true, "Pearled players can pearl other players");

        AdminMenuSection pearls = menu.getSubsection("Pearls", "Pearling, freeing and summoning",
            new ItemStack(Material.ENDER_EYE));
        hotbarNeeded = toggle(pearls, "pearls.hotbar_needed", "Hotbar needed", true,
            "Killers only pearl players with a pearl in their hotbar");
        freeByThrowing = toggle(pearls, "pearls.free_by_throwing", "Free by throwing", false,
            "Throwing a pearl frees the player in it");
        freeTeleport = toggle(pearls, "pearls.free_teleport", "Free teleport", true,
            "Freed players are teleported to whoever freed them");
        announceExileLocation = toggle(pearls, "pearls.announce_exile_location", "Announce exile location", true,
            "Announce where players were exiled");
        allowPearlStealing = toggle(pearls, "pearls.allow_pearl_stealing", "Pearl stealing", true,
            "Prison pearls can be stolen by killing a summoned player");
        allowSummoning = toggle(pearls, "pearls.allow_summoning", "Summoning", true, "Prison pearls can be summoned");
        bastionHarmAmount = decimal(pearls, "pearls.bastion_harm_amount", "Bastion harm", 0.5,
            Material.GOLDEN_SWORD, "Damage dealt to exiles inside bastion fields they aren't on", false);

        AdminMenuSection health = menu.getSubsection("Pearl health", "How pearls decay and how much they hold",
            new ItemStack(Material.OBSIDIAN));
        startValue = integer(health, "pearls.start_value", "Start health", 12, Material.EMERALD,
            "Health of newly made pearls", false);
        maxValue = integer(health, "pearls.max_value", "Max health", 336, Material.EMERALD_BLOCK,
            "Highest health a pearl can have, which the health percentage is based on", false);
        decayAmount = integer(health, "pearls.decay_amount", "Decay amount", 1, Material.REDSTONE,
            "Health removed each time pearls decay", false);
        decayIntervalMin = integer(health, "pearls.decay_interval_min", "Decay interval", 60, Material.CLOCK,
            "Minutes between pearl decays", true);
        decayTimeoutMin = integer(health, "pearls.decay_timeout_min", "Decay timeout", 10080, Material.COMPASS,
            "Minutes offline after which a pearl stops decaying", false);
        costMultiplierDays = integer(health, "pearls.cost_multiplier_days", "Cost multiplier days", 0,
            Material.EXPERIENCE_BOTTLE, "Days after which repairs cost more, 0 disables it", false);
        costMultiplierCapDays = decimal(health, "pearls.cost_multiplier_cap_days", "Cost multiplier cap", 0.0,
            Material.EXPERIENCE_BOTTLE, "Days of pearling after which repair costs stop rising", false);

        AdminMenuSection damageLog = menu.getSubsection("Damage log", "Who gets awarded the pearl in a fight",
            new ItemStack(Material.IRON_SWORD));
        damageLogAlgorithm = registerTask(AdminSettingAPI.registerSetting(new BoundedIntegerSetting(
            javaPlugin, 0, "Algorithm", "exilepearl.damage_log.algorithm", new ItemStack(Material.COMPARATOR),
            "0 awards the most recent damager, 1 the one who dealt the most damage", false, 0, 1),
            "damage_log.algorithm", damageLog), true);
        damageLogTickInterval = integer(damageLog, "damage_log.tick_interval", "Tick interval", 20, Material.CLOCK,
            "Ticks between damage decays", true);
        damageLogDecayAmount = decimal(damageLog, "damage_log.decay_amount", "Decay amount", 1.0,
            Material.REDSTONE, "Tracked damage removed each interval", true);
        damageLogMaxAmount = decimal(damageLog, "damage_log.max_amount", "Max damage", 30.0,
            Material.NETHERITE_SWORD, "Most damage tracked per damager", true);
        damageLogPotionDamage = decimal(damageLog, "damage_log.potion_damge", "Potion damage", 6.0,
            Material.SPLASH_POTION, "Base damage tracked for harming, poison and weakness splash potions", true);

        AdminMenuSection ruleMenu = menu.getSubsection("Exile rules", "What exiled players can do",
            new ItemStack(Material.IRON_BARS));
        pearlRadius = integer(ruleMenu, "rules.pearl_radius", "Pearl radius", 1000, Material.COMPASS,
            "Blocks around their pearl exiles can't enter, 0 disables it", false);
        rules.clear();
        for (Map.Entry<ExileRule, RuleInfo> entry : RULES.entrySet()) {
            RuleInfo info = entry.getValue();
            rules.put(entry.getKey(), toggle(ruleMenu, info.path(), info.name(), info.defaultValue(),
                info.description()));
        }
    }

    private AdminSetting<Integer> integer(AdminMenuSection menu, String path, String name, int defaultValue,
                                          Material icon, String description, boolean usedByTasks) {
        return registerTask(AdminSettingAPI.registerSetting(new IntegerSetting(javaPlugin, defaultValue,
            name, "exilepearl." + path, new ItemStack(icon), description, false), path, menu), usedByTasks);
    }

    private AdminSetting<Double> decimal(AdminMenuSection menu, String path, String name, double defaultValue,
                                         Material icon, String description, boolean usedByTasks) {
        return registerTask(AdminSettingAPI.registerSetting(new DoubleSetting(javaPlugin, defaultValue,
            name, "exilepearl." + path, new ItemStack(icon), description), path, menu), usedByTasks);
    }

    private AdminSetting<Boolean> toggle(AdminMenuSection menu, String path, String name, boolean defaultValue,
                                         String description) {
        return AdminSettingAPI.registerSetting(new BooleanSetting(javaPlugin, defaultValue, name,
            "exilepearl." + path, description), path, menu);
    }

    /**
     * @param usedByTasks Whether tasks cache the value or schedule themselves with it, so they need reloading
     */
    private <T> AdminSetting<T> registerTask(AdminSetting<T> setting, boolean usedByTasks) {
        if (usedByTasks) {
            setting.registerListener((changed, oldValue, newValue) -> loadConfigurables(true));
        }
        return setting;
    }

    @Override
    public StorageType getStorageType() {
        return StorageType.valueOf(doc.getInteger("storage.type", 0));
    }

    @Override
    public String getMySqlHost() {
        return doc.getString("storage.mysql.host", "localhost");
    }

    @Override
    public String getMySqlDatabaseName() {
        return doc.getString("storage.mysql.dbname", "bukkit");
    }

    @Override
    public String getMySqlUsername() {
        return doc.getString("storage.mysql.username", "bukkit");
    }

    @Override
    public String getMySqlPassword() {
        return doc.getString("storage.mysql.password", "");
    }

    @Override
    public int getMySqlPort() {
        return doc.getInteger("storage.mysql.port", 3306);
    }

    @Override
    public int getMySqlPoolSize() {
        return doc.getInteger("storage.mysql.pool_size", 5);
    }

    @Override
    public int getMySqlConnectionTimeout() {
        return doc.getInteger("storage.mysql.connection_timeout", 5000);
    }

    @Override
    public int getMySqlIdleTimeout() {
        return doc.getInteger("storage.mysql.idle_timeout", 5000);
    }

    @Override
    public int getMySqlMaxLifetime() {
        return doc.getInteger("storage.mysql.max_lifetime", 5000);
    }

    @Override
    public String getMySqlMigrateDatabaseName() {
        return doc.getString("storage.mysql.migrate_dbname", "prisonpearl");
    }

    @Override
    public String getPearlHealthDecayHumanInterval() {
        return doc.getString("pearls.decay_interval_human", "day");
    }

    @Override
    public int getPearlHealthDecayHumanIntervalMin() {
        return doc.getInteger("pearls.decay_interval_min_human", 1440);
    }

    @Override
    public int getPearlHealthDecayIntervalMin() {
        // The decay task can't be scheduled every 0 minutes
        return Math.max(1, decayIntervalMin.getValue());
    }

    @Override
    public int getPearlHealthDecayAmount() {
        return decayAmount.getValue();
    }

    @Override
    public int getPearlHealthDecayTimeout() {
        return decayTimeoutMin.getValue();
    }

    @Override
    public int getPearlHealthStartValue() {
        return startValue.getValue();
    }

    @Override
    public int getPearlHealthMaxValue() {
        return maxValue.getValue();
    }

    @Override
    public boolean getMustPrisonPearlHotBar() {
        return hotbarNeeded.getValue();
    }

    @Override
    public boolean getFreeByThrowing() {
        return freeByThrowing.getValue();
    }

    @Override
    public boolean getShouldFreeTeleport() {
        return freeTeleport.getValue();
    }

    @Override
    public boolean getShouldAnnounceExileLocation() {
        return announceExileLocation.getValue();
    }

    @Override
    public double getBastionDamage() {
        return bastionHarmAmount.getValue();
    }

    @Override
    public Set<RepairMaterial> getRepairMaterials(PearlType type) {
        Set<RepairMaterial> repairs = new HashSet<>();
        Document repairRecipes = doc.getDocument("pearls.repair_materials." + type);

        if (repairRecipes == null) {
            return repairs;
        }

        for (String repairName : repairRecipes.keySet()) {
            repairs.add(RepairMaterial.fromDocument(repairName, repairRecipes.getDocument(repairName)));
        }

        return repairs;
    }

    @Override
    public PearlType getDefaultPearlType() {
        return PearlType.valueOf(doc.getString("pearls.default_pearl_type", "EXILE"));
    }

    @Override
    public Set<String> getDisallowedWorlds() {
        HashSet<String> worlds = new HashSet<>();
        for (String str : doc.getStringList("rules.disallowed_worlds"))
            worlds.add(str);
        return worlds;
    }

    @Override
    public List<String> getProtectedAnimals() {
        return doc.getStringList("rules.protected_mobs");
    }

    @Override
    public int getSuicideTimeoutSeconds() {
        return Math.max(0, suicideTimeSeconds.getValue());
    }

    @Override
    public int getRulePearlRadius() {
        return pearlRadius.getValue();
    }

    /**
     * Changes the radius live, {@link #saveToFile()} writes it to the config file
     */
    @Override
    public void setRulePearlRadius(int value) {
        pearlRadius.setValue(value);
    }

    @Override
    public boolean canPerform(ExileRule rule) {
        if (rule == ExileRule.PEARL_RADIUS) {
            return getRulePearlRadius() > 0;
        }
        AdminSetting<Boolean> setting = rules.get(rule);
        return setting != null && setting.getValue();
    }

    /**
     * Changes the rule live, {@link #saveToFile()} writes it to the config file
     */
    @Override
    public void setRule(ExileRule rule, boolean value) {
        AdminSetting<Boolean> setting = rules.get(rule);
        if (setting != null) {
            setting.setValue(value);
        }
    }

    @Override
    public boolean getUseHelpItem() {
        return doc.getBoolean("help_item.enabled", true);
    }

    @Override
    public String getHelpItemName() {
        return doc.getString("help_item.item_name", "You've been exiled!");
    }

    @Override
    public List<String> getHelpItemText() {
        return doc.getStringList("help_item.item_text");
    }

    @Override
    public boolean getDamageLogEnabled() {
        return doc.getBoolean("damage_log.enabled", true);
    }

    @Override
    public int getDamageLogAlgorithm() {
        return damageLogAlgorithm.getValue();
    }

    @Override
    public int getDamageLogInterval() {
        // The damage log task can't be scheduled every 0 ticks
        return Math.max(1, damageLogTickInterval.getValue());
    }

    @Override
    public double getDamageLogDecayAmount() {
        return damageLogDecayAmount.getValue();
    }

    @Override
    public double getDamageLogMaxDamage() {
        return damageLogMaxAmount.getValue();
    }

    @Override
    public double getDamageLogPotionDamage() {
        return damageLogPotionDamage.getValue();
    }

    @Override
    public World getPrisonWorld() {
        return Bukkit.getWorld(doc.getString("pearls.prison_world", "world_the_end"));
    }

    @Override
    public World getMainWorld() {
        return Bukkit.getWorld(doc.getString("pearls.main_world", "world"));
    }

    @Override
    public Set<RepairMaterial> getUpgradeMaterials() {
        Set<RepairMaterial> upgrades = new HashSet<>();
        Document upgradeRecipes = doc.getDocument("pearls.upgrade_materials");

        if (upgradeRecipes == null) {
            return upgrades;
        }

        for (String upgradeName : upgradeRecipes.keySet()) {
            upgrades.add(RepairMaterial.fromDocument(upgradeName, upgradeRecipes.getDocument(upgradeName)));
        }

        return upgrades;
    }

    @Override
    public boolean allowPearlStealing() {
        return allowPearlStealing.getValue();
    }

    @Override
    public boolean allowSummoning() {
        return allowSummoning.getValue();
    }

    @Override
    public int maxAltsPearled() {
        return maxPearledAlts.getValue();
    }

    @Override
    public String altBanMessage() {
        return doc.getString("general.max_pearled_message", "You have too many imprisoned alts");
    }

    @Override
    public int pearlCostMultiplicationTimerDays() {
        return costMultiplierDays.getValue();
    }

    @Override
    public double pearlCostMultiplierCapDays() {
        return costMultiplierCapDays.getValue();
    }

    @Override
    public boolean canElytraWithPearls() {
        return canElytraWithPearls.getValue();
    }

    @Override
    public boolean canTakeThroughPortals() {
        return canTakeThroughPortals.getValue();
    }

    @Override
    public boolean canPearledPlayersPearlOthers() {
        return canPearledPlayersPearlOthers.getValue();
    }
}
