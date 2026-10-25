package vg.civcraft.mc.civmodcore.players.settings;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.plugin.Plugin;
import vg.civcraft.mc.civmodcore.CivModCorePlugin;
import vg.civcraft.mc.civmodcore.players.settings.gui.AdminMenuOption;
import vg.civcraft.mc.civmodcore.players.settings.gui.AdminMenuSection;
import vg.civcraft.mc.civmodcore.players.settings.gui.MenuSection;
import vg.civcraft.mc.civmodcore.players.settings.impl.AltConsistentSetting;

/**
 * Allows plugins to expose values from their config files to admins, who can then change them at runtime through
 * /adminconfig without editing files or restarting. Each setting is bound to a path in a config file. Changes only
 * apply live until they are flushed, which writes them to the config file.
 */
public final class AdminSettingAPI {

    private static final Map<String, AdminSetting<?>> SETTINGS_BY_IDENTIFIER = new HashMap<>();

    private static final Map<String, List<AdminSetting<?>>> SETTINGS_BY_PLUGIN = new HashMap<>();

    private static final Map<String, MenuSection> MENUS_BY_PLUGIN = new HashMap<>();

    private static final MenuSection MAIN_MENU = new AdminMenuSection("Admin Config", "", null,
        AdminSettingAPI::getAllSettings);

    /**
     * @return GUI main menu, containing one section per plugin
     */
    public static MenuSection getMainMenu() {
        return MAIN_MENU;
    }

    public static Collection<AdminSetting<?>> getAllSettings() {
        return Collections.unmodifiableCollection(SETTINGS_BY_IDENTIFIER.values());
    }

    /**
     * @param pluginName Name of the plugin, case sensitive
     * @return All settings registered by that plugin
     */
    public static List<AdminSetting<?>> getSettings(String pluginName) {
        return Collections.unmodifiableList(SETTINGS_BY_PLUGIN.getOrDefault(pluginName, Collections.emptyList()));
    }

    /**
     * @return Names of all plugins which registered settings
     */
    public static Collection<String> getPluginNames() {
        return Collections.unmodifiableCollection(SETTINGS_BY_PLUGIN.keySet());
    }

    /**
     * Gets a setting by its identifier
     *
     * @param identifier Identifier to get setting for
     * @return Setting with the given identifier or null if no such setting exists
     */
    public static AdminSetting<?> getSetting(String identifier) {
        return SETTINGS_BY_IDENTIFIER.get(identifier);
    }

    /**
     * Binds a setting to a path in the owning plugin's config.yml
     *
     * @see #registerSetting(PlayerSetting, String, String)
     */
    public static <T> AdminSetting<T> registerSetting(PlayerSetting<T> type, String path) {
        return registerSetting(type, "config.yml", path);
    }

    /**
     * Binds a setting to a path in a config file of the owning plugin. Settings must be registered on every startup
     * to be available and identifiers must be unique globally. Registering an identifier the same plugin already
     * registered replaces the old setting, so this can be called from a config parser which runs again on reload.
     * Any {@link PlayerSetting} implementation for a single
     * value can be used to define the value type, for example:
     * <pre>{@code
     * AdminSetting<Integer> maxClaims = AdminSettingAPI.registerSetting(
     *     new IntegerSetting(plugin, 10, "Max claims", "myplugin.maxClaims"), "claims.max");
     * // Apply changes live, the config file is only written when an admin flushes
     * maxClaims.registerListener((setting, oldValue, newValue) -> config.maxClaims = newValue);
     * }</pre>
     * The current value is read from the file when registering, falling back to the default if the path is missing
     * or holds an invalid value. The passed setting must not also be registered with {@link PlayerSettingAPI}.
     *
     * @param type     Setting defining identifier, name, default value and value type
     * @param fileName Name of the config file within the plugin's data folder
     * @param path     Path of the value within the config file
     * @return Handle to read and change the value through
     */
    public static <T> AdminSetting<T> registerSetting(PlayerSetting<T> type, String fileName, String path) {
        Preconditions.checkArgument(type != null, "Admin setting cannot be null.");
        Preconditions.checkArgument(fileName != null && !fileName.isEmpty(), "Config file name cannot be empty.");
        Preconditions.checkArgument(path != null && !path.isEmpty(), "Config path cannot be empty.");
        Preconditions.checkArgument(!(type instanceof AltConsistentSetting),
            "Admin settings are global, alt consistency does not apply.");
        String pluginName = type.getOwningPlugin().getName();
        AdminSetting<?> previous = SETTINGS_BY_IDENTIFIER.get(type.getIdentifier());
        Preconditions.checkArgument(previous == null || previous.getOwningPlugin().getName().equals(pluginName),
            "An admin setting with identifier " + type.getIdentifier() + " is already registered by "
                + (previous == null ? null : previous.getOwningPlugin().getName()) + ".");
        AdminSetting<T> setting = new AdminSetting<>(type, fileName, path);
        try {
            setting.load();
        } catch (RuntimeException e) {
            CivModCorePlugin.getInstance().warning("Using default for admin setting " + type.getIdentifier(), e);
        }
        MenuSection menu = MENUS_BY_PLUGIN.computeIfAbsent(pluginName, k -> {
            MenuSection section = new AdminMenuSection(k, "Settings of " + k, MAIN_MENU, () -> getSettings(k));
            section.registerToParentMenu();
            return section;
        });
        List<AdminSetting<?>> pluginSettings = SETTINGS_BY_PLUGIN.computeIfAbsent(pluginName, k -> new ArrayList<>());
        if (previous == null) {
            pluginSettings.add(setting);
        } else {
            // Re-registered, for example by a config parser after a reload. The old handle and its listeners are
            // dropped and any unflushed change is lost, since the new setting was just read from the file
            pluginSettings.set(pluginSettings.indexOf(previous), setting);
            menu.removeItem(item -> item instanceof AdminMenuOption option && option.getSetting() == previous);
        }
        SETTINGS_BY_IDENTIFIER.put(type.getIdentifier(), setting);
        menu.addItem(new AdminMenuOption(menu, setting));
        return setting;
    }

    /**
     * @return All settings whose live value is not flushed to their config file yet
     */
    public static List<AdminSetting<?>> getUnflushedSettings() {
        return SETTINGS_BY_IDENTIFIER.values().stream().filter(AdminSetting::hasUnflushedChange).toList();
    }

    /**
     * Writes all unflushed changes of the given settings to their config files
     *
     * @param settings Settings to flush
     * @return Amount of settings which had changes written
     */
    public static int flush(Collection<AdminSetting<?>> settings) {
        int flushed = 0;
        for (AdminSetting<?> setting : settings) {
            if (!setting.hasUnflushedChange()) {
                continue;
            }
            try {
                setting.flush();
                flushed++;
            } catch (RuntimeException e) {
                CivModCorePlugin.getInstance().severe("Failed to flush admin setting " + setting.getIdentifier(), e);
            }
        }
        return flushed;
    }

    /**
     * Throws away all unflushed changes of the given settings, setting them back to their config file values
     *
     * @param settings Settings to discard changes of
     * @return Amount of settings which had changes discarded
     */
    public static int discard(Collection<AdminSetting<?>> settings) {
        int discarded = 0;
        for (AdminSetting<?> setting : settings) {
            if (setting.hasUnflushedChange()) {
                setting.discard();
                discarded++;
            }
        }
        return discarded;
    }

    /**
     * Re-reads all settings of a plugin from its config files, throwing away unflushed changes. Call this after the
     * plugin reloads its config
     *
     * @param plugin Plugin to reload settings of
     */
    public static void reload(Plugin plugin) {
        for (AdminSetting<?> setting : SETTINGS_BY_PLUGIN.getOrDefault(plugin.getName(), Collections.emptyList())) {
            try {
                setting.reload();
            } catch (RuntimeException e) {
                CivModCorePlugin.getInstance().warning("Failed to reload admin setting " + setting.getIdentifier(), e);
            }
        }
    }
}
