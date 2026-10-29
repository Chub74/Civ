package vg.civcraft.mc.civmodcore.players.settings;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import vg.civcraft.mc.civmodcore.CivModCorePlugin;
import vg.civcraft.mc.civmodcore.players.settings.gui.AdminMenuOption;
import vg.civcraft.mc.civmodcore.players.settings.gui.AdminMenuSection;
import vg.civcraft.mc.civmodcore.players.settings.impl.AltConsistentSetting;

/**
 * Allows plugins to expose values from their config files to admins, who can then change them at runtime through
 * /adminconfig without editing files or restarting. Each setting is bound to a path in a config file. Changes only
 * apply live until they are flushed, which writes them to the config file.
 */
public final class AdminSettingAPI {

    private static final Map<String, AdminSetting<?>> SETTINGS_BY_IDENTIFIER = new HashMap<>();

    private static final Map<String, List<AdminSetting<?>>> SETTINGS_BY_PLUGIN = new HashMap<>();

    private static final Map<String, AdminMenuSection> MENUS_BY_PLUGIN = new HashMap<>();

    private static final Map<String, AdminSettingMap<?, ?>> MAPS_BY_IDENTIFIER = new HashMap<>();

    private static final AdminMenuSection MAIN_MENU = new AdminMenuSection("Admin Config", "", null, null,
        new ItemStack(Material.COMMAND_BLOCK));

    /**
     * @return GUI main menu, containing one section per plugin
     */
    public static AdminMenuSection getMainMenu() {
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
     * Binds a setting to a path in the owning plugin's config.yml, shown directly in the plugin's menu section
     *
     * @see #registerSetting(PlayerSetting, String, String, AdminMenuSection)
     */
    public static <T> AdminSetting<T> registerSetting(PlayerSetting<T> type, String path) {
        return registerSetting(type, "config.yml", path, null);
    }

    /**
     * Binds a setting to a path in the owning plugin's config.yml, shown in the given menu section
     *
     * @see #registerSetting(PlayerSetting, String, String, AdminMenuSection)
     */
    public static <T> AdminSetting<T> registerSetting(PlayerSetting<T> type, String path, AdminMenuSection menu) {
        return registerSetting(type, "config.yml", path, menu);
    }

    /**
     * Binds a setting to a path in a config file of the owning plugin, shown directly in the plugin's menu section
     *
     * @see #registerSetting(PlayerSetting, String, String, AdminMenuSection)
     */
    public static <T> AdminSetting<T> registerSetting(PlayerSetting<T> type, String fileName, String path) {
        return registerSetting(type, fileName, path, null);
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
     * @param menu     Section of the plugin's menu to show the setting in, see {@link #getPluginMenu(Plugin)} and
     *                 {@link AdminMenuSection#getSubsection(String, String)}. Null for the plugin's section itself
     * @return Handle to read and change the value through
     */
    public static <T> AdminSetting<T> registerSetting(PlayerSetting<T> type, String fileName, String path,
                                                      AdminMenuSection menu) {
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
        if (menu == null) {
            menu = getPluginMenu(type.getOwningPlugin());
        }
        Preconditions.checkArgument(pluginName.equals(menu.getPluginName()),
            "Admin setting " + type.getIdentifier() + " must be shown in a menu section of " + pluginName + ".");
        setting.setMenu(menu);
        List<AdminSetting<?>> pluginSettings = SETTINGS_BY_PLUGIN.computeIfAbsent(pluginName, k -> new ArrayList<>());
        if (previous == null) {
            pluginSettings.add(setting);
        } else {
            // Re-registered, for example by a config parser after a reload. The old handle and its listeners are
            // dropped and any unflushed change is lost, since the new setting was just read from the file
            pluginSettings.set(pluginSettings.indexOf(previous), setting);
            previous.getMenu().removeItem(item -> item instanceof AdminMenuOption option
                && option.getSetting() == previous);
        }
        SETTINGS_BY_IDENTIFIER.put(type.getIdentifier(), setting);
        menu.addItem(new AdminMenuOption(menu, setting));
        return setting;
    }

    /**
     * Changes the item a plugin's section is shown as in the admin config main menu, which is a command block by
     * default. Can be called before or after registering the plugin's settings
     *
     * @param plugin Plugin whose section to change
     * @param icon   Item to show, its name and lore are replaced
     */
    public static void setPluginIcon(Plugin plugin, ItemStack icon) {
        Preconditions.checkArgument(plugin != null, "Plugin cannot be null.");
        getPluginMenu(plugin).setItemRepresentation(icon);
    }

    /**
     * Gets the section of the admin config main menu for a plugin, creating it if needed. Use
     * {@link AdminMenuSection#getSubsection(String, String)} on it to group settings
     *
     * @param plugin Plugin to get the section of
     * @return The plugin's section
     */
    public static AdminMenuSection getPluginMenu(Plugin plugin) {
        Preconditions.checkArgument(plugin != null, "Plugin cannot be null.");
        return MENUS_BY_PLUGIN.computeIfAbsent(plugin.getName(), k -> {
            AdminMenuSection section = new AdminMenuSection(k, "Settings of " + k, MAIN_MENU, k,
                new ItemStack(Material.COMMAND_BLOCK));
            section.registerToParentMenu();
            return section;
        });
    }

    /**
     * Binds a section of the owning plugin's config.yml to an admin setting map
     *
     * @see #registerMap(JavaPlugin, String, String, String, AdminMenuSection, Function, Function, BiFunction)
     */
    public static <K, V> AdminSettingMap<K, V> registerMap(JavaPlugin plugin, String identifier, String path,
                                                           AdminMenuSection menu, Function<String, K> keyParser,
                                                           Function<K, String> keySerializer,
                                                           BiFunction<K, String, PlayerSetting<V>> entryFactory) {
        return registerMap(plugin, identifier, "config.yml", path, menu, keyParser, keySerializer, entryFactory);
    }

    /**
     * Binds a section of a config file mapping keys to values to an admin setting map, whose entries admins can
     * change, add and remove at runtime. Every key in the section becomes an entry shown in the given menu section,
     * which also gets a button to add entries. For example, for drop chances per mob:
     * <pre>{@code
     * AdminSettingMap<EntityType, Double> dropChances = AdminSettingAPI.registerMap(plugin,
     *     "myplugin.dropChances", "dropChances", menu,
     *     key -> EnumUtils.getEnum(EntityType.class, key.toUpperCase()), Enum::name,
     *     (type, identifier) -> new DoubleSetting(plugin, 0.0, type.name(), identifier,
     *         new ItemStack(Material.ENDER_PEARL), "Chance to drop"));
     * Double chance = dropChances.get(EntityType.GHAST); // null if ghasts are not in the map
     * }</pre>
     * Registering an identifier the same plugin already registered replaces the old map and its entries, so this
     * can be called from a config parser which runs again on reload.
     *
     * @param plugin        Plugin owning the config file
     * @param identifier    Unique identifier of the map, entries are identified as {@code <identifier>.<key>}
     * @param fileName      Name of the config file within the plugin's data folder
     * @param path          Path of the section within the config file
     * @param menu          Menu section to show the entries in, which can only hold one map
     * @param keyParser     Parses keys as written in the config or typed by admins, null or an exception if invalid
     * @param keySerializer Turns keys into how they are written in the config
     * @param entryFactory  Creates the setting defining the value type of an entry, given its key and the
     *                      identifier it must use
     * @return Handle to read and change the entries through
     */
    public static <K, V> AdminSettingMap<K, V> registerMap(JavaPlugin plugin, String identifier, String fileName,
                                                           String path, AdminMenuSection menu,
                                                           Function<String, K> keyParser,
                                                           Function<K, String> keySerializer,
                                                           BiFunction<K, String, PlayerSetting<V>> entryFactory) {
        Preconditions.checkArgument(plugin != null, "Plugin cannot be null.");
        Preconditions.checkArgument(identifier != null && !identifier.isEmpty(), "Identifier cannot be empty.");
        Preconditions.checkArgument(fileName != null && !fileName.isEmpty(), "Config file name cannot be empty.");
        Preconditions.checkArgument(path != null && !path.isEmpty(), "Config path cannot be empty.");
        Preconditions.checkArgument(menu != null && plugin.getName().equals(menu.getPluginName()),
            "Admin setting map " + identifier + " must be shown in a menu section of " + plugin.getName() + ".");
        Preconditions.checkArgument(keyParser != null && keySerializer != null && entryFactory != null,
            "Key parser, key serializer and entry factory cannot be null.");
        AdminSettingMap<?, ?> previous = MAPS_BY_IDENTIFIER.get(identifier);
        Preconditions.checkArgument(previous == null || previous.getOwningPlugin().getName().equals(plugin.getName()),
            "An admin setting map with identifier " + identifier + " is already registered by "
                + (previous == null ? null : previous.getOwningPlugin().getName()) + ".");
        AdminSettingMap<?, ?> menuMap = getMap(menu);
        Preconditions.checkArgument(menuMap == null || menuMap == previous,
            "Menu section " + menu.getName() + " already holds admin setting map "
                + (menuMap == null ? null : menuMap.getIdentifier()) + ".");
        if (previous != null) {
            previous.unregisterAll();
        }
        AdminSettingMap<K, V> map = new AdminSettingMap<>(plugin, identifier, fileName, path, menu, keyParser,
            keySerializer, entryFactory);
        MAPS_BY_IDENTIFIER.put(identifier, map);
        map.load();
        return map;
    }

    /**
     * @return All registered admin setting maps
     */
    public static Collection<AdminSettingMap<?, ?>> getAllMaps() {
        return Collections.unmodifiableCollection(MAPS_BY_IDENTIFIER.values());
    }

    /**
     * @param identifier Identifier to get map for
     * @return Map with the given identifier or null if no such map exists
     */
    public static AdminSettingMap<?, ?> getMap(String identifier) {
        return MAPS_BY_IDENTIFIER.get(identifier);
    }

    /**
     * @param menu Menu section to get the map of
     * @return Map whose entries are shown in the menu section, or null if it holds none
     */
    public static AdminSettingMap<?, ?> getMap(AdminMenuSection menu) {
        for (AdminSettingMap<?, ?> map : MAPS_BY_IDENTIFIER.values()) {
            if (map.getMenu() == menu) {
                return map;
            }
        }
        return null;
    }

    /**
     * Removes a setting entirely, used for map entries which were deleted
     */
    static void unregister(AdminSetting<?> setting) {
        if (SETTINGS_BY_IDENTIFIER.get(setting.getIdentifier()) != setting) {
            return;
        }
        SETTINGS_BY_IDENTIFIER.remove(setting.getIdentifier());
        List<AdminSetting<?>> pluginSettings = SETTINGS_BY_PLUGIN.get(setting.getOwningPlugin().getName());
        if (pluginSettings != null) {
            pluginSettings.remove(setting);
        }
        setting.getMenu().removeItem(item -> item instanceof AdminMenuOption option
            && option.getSetting() == setting);
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
        // Copied, since flushing a removed map entry unregisters it
        for (AdminSetting<?> setting : new ArrayList<>(settings)) {
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
        // Copied, since discarding an added map entry unregisters it
        for (AdminSetting<?> setting : new ArrayList<>(settings)) {
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
        for (AdminSettingMap<?, ?> map : new ArrayList<>(MAPS_BY_IDENTIFIER.values())) {
            if (!map.getOwningPlugin().getName().equals(plugin.getName())) {
                continue;
            }
            try {
                map.reload();
            } catch (RuntimeException e) {
                CivModCorePlugin.getInstance().warning("Failed to reload admin setting map " + map.getIdentifier(), e);
            }
        }
        for (AdminSetting<?> setting : SETTINGS_BY_PLUGIN.getOrDefault(plugin.getName(), Collections.emptyList())) {
            // Map entries were just reloaded with their map
            if (setting.getMap() != null) {
                continue;
            }
            try {
                setting.reload();
            } catch (RuntimeException e) {
                CivModCorePlugin.getInstance().warning("Failed to reload admin setting " + setting.getIdentifier(), e);
            }
        }
    }
}
