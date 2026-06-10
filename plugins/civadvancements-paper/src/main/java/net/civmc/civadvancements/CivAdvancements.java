package net.civmc.civadvancements;

import java.io.File;
import net.civmc.civadvancements.commands.CivAdvancementsCommandHandler;
import net.civmc.civadvancements.listeners.BastionAdvancementListener;
import net.civmc.civadvancements.listeners.BreweryXAdvancementListener;
import net.civmc.civadvancements.listeners.CharcoalListener;
import net.civmc.civadvancements.listeners.ChatAdvancementListener;
import net.civmc.civadvancements.listeners.ClayListener;
import net.civmc.civadvancements.listeners.CommandAdvancementListener;
import net.civmc.civadvancements.listeners.CropHarvestListener;
import net.civmc.civadvancements.listeners.EmfAdvancementListener;
import net.civmc.civadvancements.listeners.ExilePearlAdvancementListener;
import net.civmc.civadvancements.listeners.FactoryAdvancementListener;
import net.civmc.civadvancements.listeners.FishingAdvancementListener;
import net.civmc.civadvancements.listeners.HiddenOreListener;
import net.civmc.civadvancements.listeners.ItemExchangeAdvancementListener;
import net.civmc.civadvancements.listeners.JukeAlertAdvancementListener;
import net.civmc.civadvancements.listeners.NameLayerAdvancementListener;
import net.civmc.civadvancements.listeners.RailAdvancementListener;
import net.civmc.civadvancements.listeners.RealisticBiomesListener;
import net.civmc.civadvancements.listeners.ReinforcementListener;
import net.civmc.civadvancements.listeners.StatisticAdvancementListener;
import net.civmc.civadvancements.listeners.WelcomeListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import vg.civcraft.mc.civmodcore.ACivMod;

public class CivAdvancements extends ACivMod {

    private static CivAdvancements instance;
    private CivAdvancementsConfig civConfig;
    private AdvancementManager advancementManager;
    private ChallengeTracker challengeTracker;
    private RewardManager rewardManager;

    @Override
    public void onEnable() {
        super.onEnable();
        instance = this;
        saveDefaultConfig();
        civConfig = new CivAdvancementsConfig(getConfig());

        challengeTracker = new ChallengeTracker();
        challengeTracker.load(getDataFile());

        rewardManager = new RewardManager(civConfig);
        advancementManager = new AdvancementManager(this, civConfig, rewardManager);
        advancementManager.loadAll();

        PluginManager pm = Bukkit.getPluginManager();

        // Always-on listeners (vanilla events only)
        pm.registerEvents(new WelcomeListener(advancementManager, civConfig, challengeTracker), this);
        pm.registerEvents(new CharcoalListener(advancementManager), this);
        pm.registerEvents(new ClayListener(advancementManager), this);
        pm.registerEvents(new StatisticAdvancementListener(advancementManager), this);
        pm.registerEvents(new CropHarvestListener(advancementManager, challengeTracker), this);
        pm.registerEvents(new CommandAdvancementListener(advancementManager), this);
        pm.registerEvents(new FishingAdvancementListener(advancementManager), this);
        pm.registerEvents(new RailAdvancementListener(advancementManager), this);

        // Citadel
        if (pm.isPluginEnabled("Citadel")) {
            pm.registerEvents(new ReinforcementListener(advancementManager, civConfig, challengeTracker), this);
        }

        // CivChat2
        if (pm.isPluginEnabled("CivChat2")) {
            pm.registerEvents(new ChatAdvancementListener(advancementManager), this);
        }

        // FactoryMod
        if (pm.isPluginEnabled("FactoryMod")) {
            pm.registerEvents(new FactoryAdvancementListener(advancementManager, civConfig, challengeTracker), this);
        }

        // HiddenOre
        if (pm.isPluginEnabled("HiddenOre")) {
            pm.registerEvents(new HiddenOreListener(advancementManager, civConfig), this);
        }

        // RealisticBiomes
        if (pm.isPluginEnabled("RealisticBiomes")) {
            pm.registerEvents(new RealisticBiomesListener(advancementManager), this);
        }

        // ExilePearl
        if (pm.isPluginEnabled("ExilePearl")) {
            pm.registerEvents(new ExilePearlAdvancementListener(advancementManager, challengeTracker), this);
        }

        // JukeAlert
        if (pm.isPluginEnabled("JukeAlert")) {
            pm.registerEvents(new JukeAlertAdvancementListener(advancementManager, civConfig), this);
        }

        // ItemExchange
        if (pm.isPluginEnabled("ItemExchange")) {
            pm.registerEvents(new ItemExchangeAdvancementListener(advancementManager), this);
        }

        // NameLayer
        if (pm.isPluginEnabled("NameLayer")) {
            pm.registerEvents(new NameLayerAdvancementListener(advancementManager), this);
        }

        // Bastion
        if (pm.isPluginEnabled("Bastion")) {
            pm.registerEvents(new BastionAdvancementListener(advancementManager, civConfig), this);
        }

        // BreweryX
        if (pm.isPluginEnabled("BreweryX")) {
            pm.registerEvents(new BreweryXAdvancementListener(advancementManager, civConfig), this);
        }

        // EvenMoreFish
        if (pm.isPluginEnabled("EvenMoreFish")) {
            pm.registerEvents(new EmfAdvancementListener(advancementManager, challengeTracker, civConfig), this);
        }

        new CivAdvancementsCommandHandler(this).registerCommands();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (challengeTracker != null) {
            try {
                challengeTracker.save(getDataFile());
            } catch (Exception e) {
                warning("Failed to save challenge data: " + e.getMessage());
            }
        }
    }

    public void reload() {
        reloadConfig();
        civConfig = new CivAdvancementsConfig(getConfig());
        rewardManager = new RewardManager(civConfig);
        advancementManager.setConfig(civConfig);
        advancementManager.reloadAll();
    }

    public static CivAdvancements getInstance() {
        return instance;
    }

    public AdvancementManager getAdvancementManager() {
        return advancementManager;
    }

    private File getDataFile() {
        return new File(getDataFolder(), "data.yml");
    }
}
