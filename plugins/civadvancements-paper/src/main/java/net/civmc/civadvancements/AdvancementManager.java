package net.civmc.civadvancements;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;

public class AdvancementManager {

    public static final String ROOT = "root";
    public static final String JOINED = "joined";

    // Reinforcement chain
    public static final String REINFORCE_STONE = "reinforce_stone";
    public static final String REINFORCE_DEEPSLATE = "reinforce_deepslate";
    public static final String REINFORCE_IRON = "reinforce_iron";
    public static final String REINFORCE_DIAMOND = "reinforce_diamond";
    public static final String REINFORCE_100 = "reinforce_100";
    public static final String REINFORCE_1000 = "reinforce_1000";
    public static final String REINFORCE_10K = "reinforce_10k";
    public static final String REINFORCE_100K = "reinforce_100k";
    public static final String REINFORCE_1M = "reinforce_1m";

    // Chat chain
    public static final String CHAT_LOCAL = "chat_local";
    public static final String CHAT_GLOBAL = "chat_global";

    // Server info
    public static final String INTRO_BOOK = "intro_book";
    public static final String SERVER_WIKI = "server_wiki";
    public static final String SERVER_VOTE = "server_vote";

    // Factory chain
    public static final String CHARCOAL = "charcoal";
    public static final String FACTORY_CREATE = "factory_create";
    public static final String FACTORY_ACTIVATE = "factory_activate";
    public static final String FACTORY_RUNS_100 = "factory_runs_100";
    public static final String FACTORY_RUNS_1000 = "factory_runs_1000";

    // HiddenOre
    public static final String HIDDEN_ORE = "hidden_ore";
    public static final String ORE_IRON = "ore_iron";
    public static final String ORE_GOLD = "ore_gold";
    public static final String ORE_DIAMOND = "ore_diamond";
    public static final String ORE_EMERALD = "ore_emerald";
    public static final String ORE_ANCIENT_DEBRIS = "ore_ancient_debris";

    // Farming chain
    public static final String CLAY_COLLECTED = "clay_collected";
    public static final String RB_FRUITFUL = "rb_fruitful";
    public static final String RB_BARREN = "rb_barren";
    public static final String HARVEST_100 = "harvest_100";
    public static final String HARVEST_1000 = "harvest_1000";
    public static final String HARVEST_10000 = "harvest_10000";
    public static final String HARVEST_100K = "harvest_100k";
    public static final String HARVEST_1M = "harvest_1m";

    // ExilePearl
    public static final String GOT_PEARLED = "got_pearled";
    public static final String PEARLED_THRICE = "pearled_thrice";

    // Death challenges
    public static final String DIE_10 = "die_10";
    public static final String DIE_50 = "die_50";

    // JukeAlert
    public static final String SNITCH_LOGGED = "snitch_logged";

    // ItemExchange
    public static final String FIRST_TRADE = "first_trade";

    // NameLayer
    public static final String NAMELAYER_CREATE = "namelayer_create";
    public static final String NAMELAYER_INVITE = "namelayer_invite";
    public static final String NAMELAYER_LIST = "namelayer_list";

    // Bastion
    public static final String PLACE_BASTION = "place_bastion";
    public static final String BASTION_CITY = "bastion_city";
    public static final String BASTION_DENIED = "bastion_denied";

    // JukeAlert (snitch_logged is above)
    public static final String SNITCH_NOTEBLOCK = "snitch_noteblock";
    public static final String SNITCH_JUKEBOX = "snitch_jukebox";
    public static final String SNITCH_LOGS = "snitch_logs";
    public static final String SNITCH_MUTE = "snitch_mute";

    // BreweryX
    public static final String BREW_CAULDRON = "brew_cauldron";
    public static final String BREW_FIRST = "brew_first";
    public static final String BREW_QUALITY = "brew_quality";

    // Fishing
    public static final String FIRST_FISH = "first_fish";
    public static final String EMF_CATCH = "emf_catch";
    public static final String EMF_RARE = "emf_rare";
    public static final String EMF_LEGENDARY = "emf_legendary";

    // Rail
    public static final String FIRST_RAIL = "first_rail";
    public static final String COPPER_RAIL = "copper_rail";
    public static final String DEST_COMMAND = "dest_command";
    public static final String DEST_SIGN = "dest_sign";

    // Mining challenges
    public static final String MINE_STONE_1K = "mine_stone_1k";
    public static final String MINE_STONE_10K = "mine_stone_10k";
    public static final String MINE_STONE_100K = "mine_stone_100k";
    public static final String MINE_STONE_1M = "mine_stone_1m";
    public static final String MINE_STONE_5M = "mine_stone_5m";
    public static final String MINE_STONE_10M = "mine_stone_10m";
    public static final String MINE_DEEPSLATE_1K = "mine_deepslate_1k";
    public static final String MINE_DEEPSLATE_10K = "mine_deepslate_10k";
    public static final String MINE_DEEPSLATE_100K = "mine_deepslate_100k";
    public static final String MINE_DEEPSLATE_1M = "mine_deepslate_1m";
    public static final String MINE_GRAVEL_1K = "mine_gravel_1k";

    // Factory milestones
    public static final String FACTORY_RUNS_10K = "factory_runs_10k";
    public static final String FACTORY_RUNS_100K = "factory_runs_100k";

    // EMF milestones
    public static final String EMF_CATCH_250 = "emf_catch_250";
    public static final String EMF_CATCH_1000 = "emf_catch_1000";
    public static final String EMF_CATCH_5000 = "emf_catch_5000";
    public static final String EMF_RARE_10 = "emf_rare_10";
    public static final String EMF_RARE_50 = "emf_rare_50";

    // Silly / marathon
    public static final String SPRINT_MARATHON = "sprint_marathon";

    private static final List<String> ALL_KEYS = List.of(
        ROOT, JOINED,
        REINFORCE_STONE, REINFORCE_DEEPSLATE, REINFORCE_IRON, REINFORCE_DIAMOND,
        REINFORCE_100, REINFORCE_1000, REINFORCE_10K, REINFORCE_100K, REINFORCE_1M,
        CHAT_LOCAL, CHAT_GLOBAL,
        INTRO_BOOK, SERVER_WIKI, SERVER_VOTE,
        CHARCOAL, FACTORY_CREATE, FACTORY_ACTIVATE,
        FACTORY_RUNS_100, FACTORY_RUNS_1000,
        HIDDEN_ORE, ORE_IRON, ORE_GOLD, ORE_DIAMOND, ORE_EMERALD, ORE_ANCIENT_DEBRIS,
        CLAY_COLLECTED, RB_FRUITFUL, RB_BARREN,
        HARVEST_100, HARVEST_1000, HARVEST_10000, HARVEST_100K, HARVEST_1M,
        GOT_PEARLED, PEARLED_THRICE,
        DIE_10, DIE_50,
        SNITCH_LOGGED,
        FIRST_TRADE,
        NAMELAYER_CREATE, NAMELAYER_INVITE, NAMELAYER_LIST,
        PLACE_BASTION, BASTION_CITY, BASTION_DENIED,
        SNITCH_NOTEBLOCK, SNITCH_JUKEBOX, SNITCH_LOGS, SNITCH_MUTE,
        BREW_CAULDRON, BREW_FIRST, BREW_QUALITY,
        FIRST_FISH, EMF_CATCH, EMF_RARE, EMF_LEGENDARY,
        FIRST_RAIL, COPPER_RAIL, DEST_COMMAND, DEST_SIGN,
        MINE_STONE_1K, MINE_STONE_10K, MINE_STONE_100K,
        MINE_STONE_1M, MINE_STONE_5M, MINE_STONE_10M,
        MINE_DEEPSLATE_1K, MINE_DEEPSLATE_10K, MINE_DEEPSLATE_100K, MINE_DEEPSLATE_1M,
        MINE_GRAVEL_1K,
        FACTORY_RUNS_10K, FACTORY_RUNS_100K,
        EMF_CATCH_250, EMF_CATCH_1000, EMF_CATCH_5000,
        EMF_RARE_10, EMF_RARE_50,
        SPRINT_MARATHON
    );

    private final CivAdvancements plugin;
    private CivAdvancementsConfig config;
    private final RewardManager rewardManager;

    public AdvancementManager(CivAdvancements plugin, CivAdvancementsConfig config, RewardManager rewardManager) {
        this.plugin = plugin;
        this.config = config;
        this.rewardManager = rewardManager;
    }

    public void setConfig(CivAdvancementsConfig config) {
        this.config = config;
    }

    public void loadAll() {
        for (String key : ALL_KEYS) {
            try {
                loadAdvancement(key);
            } catch (Exception e) {
                plugin.warning("Failed to load advancement '" + key + "': " + e.getMessage());
            }
        }
    }

    @SuppressWarnings("deprecation")
    public void reloadAll() {
        for (String key : ALL_KEYS) {
            NamespacedKey nsKey = new NamespacedKey(plugin, key);
            try {
                Bukkit.getUnsafe().removeAdvancement(nsKey);
            } catch (Exception e) {
                plugin.warning("Failed to remove advancement '" + key + "': " + e.getMessage());
            }
        }
        Bukkit.reloadData();
        loadAll();
    }

    @SuppressWarnings("deprecation")
    private void loadAdvancement(String key) throws IOException {
        String resource = "advancements/" + key + ".json";
        InputStream stream = plugin.getResource(resource);
        if (stream == null) {
            plugin.warning("Missing advancement resource: " + resource);
            return;
        }
        String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        json = json.replace("{{title}}", escapeJson(config.getAdvancementTitle(key)));
        json = json.replace("{{description}}", escapeJson(config.getAdvancementDescription(key)));
        json = json.replace("{{hidden}}", String.valueOf(config.getAdvancementHidden(key)));
        // Root has no parent — skip substitution for it
        if (!key.equals(ROOT)) {
            String parent = config.getAdvancementParent(key);
            json = json.replace("{{parent}}", escapeJson(parent));
        }

        NamespacedKey nsKey = new NamespacedKey(plugin, key);
        Bukkit.getUnsafe().loadAdvancement(nsKey, json);
    }

    public void award(Player player, String key) {
        NamespacedKey nsKey = new NamespacedKey(plugin, key);
        Advancement advancement = Bukkit.getAdvancement(nsKey);
        if (advancement == null) {
            return;
        }
        AdvancementProgress progress = player.getAdvancementProgress(advancement);
        if (!progress.isDone()) {
            progress.awardCriteria(key);
            rewardManager.giveReward(player, key);
        }
    }

    private static String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}
