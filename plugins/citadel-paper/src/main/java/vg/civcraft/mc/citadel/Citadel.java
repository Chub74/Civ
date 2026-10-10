package vg.civcraft.mc.citadel;

import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import vg.civcraft.mc.citadel.listener.ActivityListener;
import vg.civcraft.mc.citadel.activity.ActivityMap;
import vg.civcraft.mc.citadel.command.CitadelCommandManager;
import vg.civcraft.mc.citadel.listener.BlockListener;
import vg.civcraft.mc.citadel.listener.CopperGolemListener;
import vg.civcraft.mc.citadel.listener.EntityListener;
import vg.civcraft.mc.citadel.listener.HiddenOreListener;
import vg.civcraft.mc.citadel.listener.InventoryListener;
import vg.civcraft.mc.citadel.listener.ModeListener;
import vg.civcraft.mc.citadel.listener.RedstoneListener;
import vg.civcraft.mc.citadel.listener.WorldBorderListener;
import vg.civcraft.mc.citadel.model.AcidManager;
import vg.civcraft.mc.citadel.model.CitadelChunkData;
import vg.civcraft.mc.citadel.model.CitadelDAO;
import vg.civcraft.mc.citadel.model.CitadelSettingManager;
import vg.civcraft.mc.citadel.model.HologramManager;
import vg.civcraft.mc.citadel.model.Reinforcement;
import vg.civcraft.mc.citadel.model.ReinforcementOverlayManager;
import vg.civcraft.mc.citadel.playerstate.PlayerStateManager;
import vg.civcraft.mc.citadel.reinforcementtypes.ReinforcementTypeManager;
import vg.civcraft.mc.civmodcore.ACivMod;
import vg.civcraft.mc.civmodcore.world.locations.chunkmeta.api.BlockBasedChunkMetaView;
import vg.civcraft.mc.civmodcore.world.locations.chunkmeta.api.ChunkMetaAPI;
import vg.civcraft.mc.civmodcore.world.locations.chunkmeta.block.table.TableBasedDataObject;
import vg.civcraft.mc.civmodcore.world.locations.chunkmeta.block.table.TableStorageEngine;


public class Citadel extends ACivMod {

    private static final String MIN_CIVMODCORE_VERSION = "3.1.0";

    private static Citadel instance;

    public static Citadel getInstance() {
        return instance;
    }

    private Logger logger;
    private ReinforcementManager reinManager;
    private CitadelConfigManager config;
    private AcidManager acidManager;
    private ReinforcementTypeManager typeManager;
    private HologramManager holoManager;
    private ReinforcementOverlayManager overlayManager;
    private CitadelSettingManager settingManager;
    private CitadelDAO dao;
    private ActivityMap activityMap;
    private CitadelCommandManager commandManager;
    private ReinforcementMultiplier multiplier;

    private PlayerStateManager stateManager;

    public AcidManager getAcidManager() {
        return acidManager;
    }

    public CitadelConfigManager getConfigManager() {
        return config;
    }

    public ReinforcementManager getReinforcementManager() {
        return reinManager;
    }

    public ReinforcementTypeManager getReinforcementTypeManager() {
        return typeManager;
    }

    public CitadelSettingManager getSettingManager() {
        return settingManager;
    }

    public PlayerStateManager getStateManager() {
        return stateManager;
    }

    public HologramManager getHologramManager() {
        return holoManager;
    }

    /** Null without ProtocolLib */
    public ReinforcementOverlayManager getOverlayManager() {
        return overlayManager;
    }

    public ActivityMap getActivityMap() {
        return activityMap;
    }

    public ReinforcementMultiplier getMultiplier() {
        return multiplier;
    }

    CitadelDAO getDAO() {
        return dao;
    }

    @Override
    public void onDisable() {
        if (overlayManager != null) {
            overlayManager.shutdown();
        }
        activityMap.disable();
        dao.setBatchMode(true);
        reinManager.shutDown();
        dao.cleanupBatches();
        HandlerList.unregisterAll(this);
        Bukkit.getScheduler().cancelTasks(this);
    }

    public void reload() {
        onDisable();
        onEnable();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        instance = this;
        logger = getLogger();
        if (!Bukkit.getPluginManager().isPluginEnabled("NameLayer")) {
            logger.info("Citadel is shutting down because it could not find NameLayer");
            Bukkit.shutdown();
            return;
        }
        if (!isCivModCoreCompatible()) {
            Plugin civModCore = Bukkit.getPluginManager().getPlugin("CivModCore");
            String found = civModCore == null ? "not installed" : civModCore.getDescription().getVersion();
            logger.severe("Citadel " + getDescription().getVersion() + " requires CivModCore "
                + MIN_CIVMODCORE_VERSION + "+ but found " + found + ". Update CivModCore, then restart.");
            Bukkit.shutdown();
            return;
        }
        config = new CitadelConfigManager(this);
        if (!config.parse()) {
            logger.severe("Errors in config file, shutting down");
            Bukkit.shutdown();
            return;
        }
        typeManager = new ReinforcementTypeManager();
        config.getReinforcementTypes().forEach(t ->
        {
            if (!typeManager.register(t)) {
                logger.severe("Errors in the config file, shutting down");
                Bukkit.shutdown();
                return;
            }
        });
        dao = new CitadelDAO(this.logger, config.getDatabase());
        if (!dao.updateDatabase()) {
            logger.severe("Errors setting up database, shutting down");
            Bukkit.shutdown();
            return;
        }

        activityMap = new ActivityMap(this.logger, config.getDatabase());
        activityMap.enable();

        BlockBasedChunkMetaView<CitadelChunkData, TableBasedDataObject, TableStorageEngine<Reinforcement>> chunkMetaData =
            ChunkMetaAPI.registerBlockBasedPlugin(this, () -> new CitadelChunkData(false, dao), dao, true);
        if (chunkMetaData == null) {
            logger.severe("Errors setting up chunk metadata API, shutting down");
            Bukkit.shutdown();
            return;
        }
        reinManager = new ReinforcementManager(chunkMetaData);
        stateManager = new PlayerStateManager();
        acidManager = new AcidManager(config.getAcidTypes());
        settingManager = new CitadelSettingManager();
        holoManager = new HologramManager(settingManager);
        if (Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")) {
            overlayManager = new ReinforcementOverlayManager(settingManager);
        }
        multiplier = config.getMultiplier();
        commandManager = new CitadelCommandManager(this);
        CitadelPermissionHandler.setup();
        registerListeners();
    }

    private boolean isCivModCoreCompatible() {
        Plugin civModCore = Bukkit.getPluginManager().getPlugin("CivModCore");
        if (civModCore == null) {
            return false;
        }
        return compareVersions(civModCore.getDescription().getVersion(), MIN_CIVMODCORE_VERSION) >= 0;
    }

    private static int compareVersions(String actual, String required) {
        String[] actualParts = actual.split("\\.");
        String[] requiredParts = required.split("\\.");
        int length = Math.max(actualParts.length, requiredParts.length);
        for (int i = 0; i < length; i++) {
            int actualPart = i < actualParts.length ? parseIntSafe(actualParts[i]) : 0;
            int requiredPart = i < requiredParts.length ? parseIntSafe(requiredParts[i]) : 0;
            int cmp = Integer.compare(actualPart, requiredPart);
            if (cmp != 0) {
                return cmp;
            }
        }
        return 0;
    }

    private static int parseIntSafe(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Registers the listeners for Citadel.
     */
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new BlockListener(this), this);
        getServer().getPluginManager().registerEvents(new EntityListener(), this);
        getServer().getPluginManager().registerEvents(new CopperGolemListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryListener(), this);
        getServer().getPluginManager().registerEvents(new ModeListener(this), this);
        getServer().getPluginManager().registerEvents(new RedstoneListener(config.getMaxRedstoneDistance()), this);
        getServer().getPluginManager().registerEvents(new ActivityListener(activityMap), this);
        getServer().getPluginManager().registerEvents(new WorldBorderListener(), this);
        if (overlayManager != null) {
            getServer().getPluginManager().registerEvents(overlayManager, this);
        }
        if (getServer().getPluginManager().isPluginEnabled("HiddenOre")) {
            getServer().getPluginManager().registerEvents(new HiddenOreListener(), this);
        }
    }
}
