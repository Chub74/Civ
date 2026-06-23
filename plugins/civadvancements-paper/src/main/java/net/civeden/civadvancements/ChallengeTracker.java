package net.civeden.civadvancements;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;

public class ChallengeTracker {

    private final Map<UUID, Integer> factoryRuns = new HashMap<>();
    private final Map<UUID, Integer> reinforceCount = new HashMap<>();
    private final Map<UUID, Integer> pearledCount = new HashMap<>();
    private final Map<UUID, Integer> cropHarvest = new HashMap<>();
    private final Map<UUID, Integer> emfCatches = new HashMap<>();
    private final Map<UUID, Integer> emfRareCatches = new HashMap<>();
    private final Set<UUID> welcomedPlayers = new HashSet<>();

    public void load(File dataFile) {
        if (!dataFile.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(dataFile);
        loadSection(yaml, "factory_runs", factoryRuns);
        loadSection(yaml, "reinforce_count", reinforceCount);
        loadSection(yaml, "pearled_count", pearledCount);
        loadSection(yaml, "crop_harvest", cropHarvest);
        loadSection(yaml, "emf_catches", emfCatches);
        loadSection(yaml, "emf_rare_catches", emfRareCatches);
        for (String s : yaml.getStringList("welcomed")) {
            try { welcomedPlayers.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) {}
        }
    }

    public void save(File dataFile) {
        YamlConfiguration yaml = new YamlConfiguration();
        saveSection(yaml, "factory_runs", factoryRuns);
        saveSection(yaml, "reinforce_count", reinforceCount);
        saveSection(yaml, "pearled_count", pearledCount);
        saveSection(yaml, "crop_harvest", cropHarvest);
        saveSection(yaml, "emf_catches", emfCatches);
        saveSection(yaml, "emf_rare_catches", emfRareCatches);
        List<String> welcomedList = new ArrayList<>();
        for (UUID uuid : welcomedPlayers) welcomedList.add(uuid.toString());
        yaml.set("welcomed", welcomedList);
        try {
            yaml.save(dataFile);
        } catch (IOException e) {
            // logged by caller
            throw new RuntimeException(e);
        }
    }

    public int incrementFactoryRuns(UUID uuid) {
        return increment(factoryRuns, uuid);
    }

    public int incrementReinforce(UUID uuid) {
        return increment(reinforceCount, uuid);
    }

    public int incrementPearled(UUID uuid) {
        return increment(pearledCount, uuid);
    }

    public int incrementCropHarvest(UUID uuid) {
        return increment(cropHarvest, uuid);
    }

    public int incrementEmfCatches(UUID uuid) {
        return increment(emfCatches, uuid);
    }

    public int incrementEmfRareCatches(UUID uuid) {
        return increment(emfRareCatches, uuid);
    }

    public boolean hasSeenWelcome(UUID uuid) {
        return welcomedPlayers.contains(uuid);
    }

    public void markWelcomed(UUID uuid) {
        welcomedPlayers.add(uuid);
    }

    private static int increment(Map<UUID, Integer> map, UUID uuid) {
        int next = map.getOrDefault(uuid, 0) + 1;
        map.put(uuid, next);
        return next;
    }

    private static void loadSection(YamlConfiguration yaml, String section, Map<UUID, Integer> map) {
        if (!yaml.isConfigurationSection(section)) return;
        for (String key : yaml.getConfigurationSection(section).getKeys(false)) {
            try {
                map.put(UUID.fromString(key), yaml.getInt(section + "." + key));
            } catch (IllegalArgumentException ignored) {
                // malformed UUID — skip
            }
        }
    }

    private static void saveSection(YamlConfiguration yaml, String section, Map<UUID, Integer> map) {
        for (Map.Entry<UUID, Integer> entry : map.entrySet()) {
            yaml.set(section + "." + entry.getKey().toString(), entry.getValue());
        }
    }
}
