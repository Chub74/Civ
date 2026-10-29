package vg.civcraft.mc.civmodcore.players.settings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import vg.civcraft.mc.civmodcore.CivModCorePlugin;
import vg.civcraft.mc.civmodcore.players.settings.gui.AdminMenuSection;

/**
 * A section of a config file mapping keys to values, such as drop chances per mob, which admins can edit at runtime
 * including adding and removing entries. Every entry is its own {@link AdminSetting} at {@code <path>.<key>}, shown
 * in the map's menu section. Like value changes, added and removed entries only reach the config file once flushed.
 * <p>
 * Obtained through {@link AdminSettingAPI#registerMap}.
 */
public final class AdminSettingMap<K, V> {

    private final JavaPlugin plugin;
    private final String identifier;
    private final String fileName;
    private final String path;
    private final AdminMenuSection menu;
    private final Function<String, K> keyParser;
    private final Function<K, String> keySerializer;
    private final BiFunction<K, String, PlayerSetting<V>> entryFactory;
    private final Map<K, AdminSetting<V>> entries = new LinkedHashMap<>();
    private List<AdminSettingMapListener<K, V>> listeners;

    AdminSettingMap(JavaPlugin plugin, String identifier, String fileName, String path, AdminMenuSection menu,
                    Function<String, K> keyParser, Function<K, String> keySerializer,
                    BiFunction<K, String, PlayerSetting<V>> entryFactory) {
        this.plugin = plugin;
        this.identifier = identifier;
        this.fileName = fileName;
        this.path = path;
        this.menu = menu;
        this.keyParser = keyParser;
        this.keySerializer = keySerializer;
        this.entryFactory = entryFactory;
    }

    public JavaPlugin getOwningPlugin() {
        return plugin;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getFileName() {
        return fileName;
    }

    public String getPath() {
        return path;
    }

    /**
     * @return Menu section the entries are shown in
     */
    public AdminMenuSection getMenu() {
        return menu;
    }

    /**
     * @param key Key to get value for
     * @return Live value for the key, or null if there is no such entry or it was removed
     */
    public V get(K key) {
        AdminSetting<V> entry = entries.get(key);
        return entry == null || entry.isRemoved() ? null : entry.getValue();
    }

    public boolean containsKey(K key) {
        return get(key) != null;
    }

    /**
     * @return Copy of all live entries
     */
    public Map<K, V> getValues() {
        Map<K, V> values = new LinkedHashMap<>();
        for (Map.Entry<K, AdminSetting<V>> entry : entries.entrySet()) {
            if (!entry.getValue().isRemoved()) {
                values.put(entry.getKey(), entry.getValue().getValue());
            }
        }
        return values;
    }

    /**
     * @return The settings of all entries, including removed ones which are not flushed yet
     */
    public Collection<AdminSetting<V>> getEntries() {
        return Collections.unmodifiableCollection(entries.values());
    }

    /**
     * @param input Key as typed by an admin
     * @return Parsed key or null if it is not valid
     */
    public K parseKey(String input) {
        try {
            K key = keyParser.apply(input);
            // Dots would nest the entry deeper in the config
            return key == null || keySerializer.apply(key).contains(".") ? null : key;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * @param input Key as typed by an admin
     * @return Whether the key is valid and there is no live entry for it yet
     */
    public boolean isValidNewKey(String input) {
        K key = parseKey(input);
        return key != null && !containsKey(key);
    }

    public boolean isValidValue(K key, String input) {
        return createType(key).isValidValue(input);
    }

    /**
     * @return Serialized default value new entries for the key start with, for prompts
     */
    public String getSerializedDefault(K key) {
        PlayerSetting<V> type = createType(key);
        return type.serialize(type.getDefaultValue());
    }

    /**
     * Adds an entry live, or restores it if it was removed. The config file is not touched until flushed
     *
     * @param key   Key of the entry
     * @param value Value of the entry
     * @return Setting of the entry
     * @throws IllegalArgumentException If there already is a live entry for the key
     */
    public AdminSetting<V> add(K key, V value) {
        AdminSetting<V> entry = entries.get(key);
        if (entry != null) {
            if (!entry.isRemoved()) {
                throw new IllegalArgumentException(identifier + " already contains " + keySerializer.apply(key));
            }
            entry.setValue(value);
            return entry;
        }
        String entryPath = path + "." + keySerializer.apply(key);
        entry = AdminSettingAPI.registerSetting(createType(key), fileName, entryPath, menu);
        attach(key, entry);
        entry.markAdded(value);
        return entry;
    }

    /**
     * Parses the value and adds an entry. Check {@link #isValidValue(Object, String)} first
     */
    public AdminSetting<V> addSerialized(K key, String valueInput) {
        return add(key, createType(key).deserialize(valueInput));
    }

    /**
     * Parses key and value and adds an entry. Check {@link #isValidNewKey(String)} and
     * {@link #isValidValue(Object, String)} first
     */
    public AdminSetting<V> addFromInput(String keyInput, String valueInput) {
        K key = parseKey(keyInput);
        if (key == null) {
            throw new IllegalArgumentException(keyInput + " is not a valid key for " + identifier);
        }
        return addSerialized(key, valueInput);
    }

    /**
     * Removes an entry live. The config file is not touched until flushed
     *
     * @param key Key of the entry to remove
     */
    public void remove(K key) {
        AdminSetting<V> entry = entries.get(key);
        if (entry != null) {
            removeEntry(entry);
        }
    }

    void removeEntry(AdminSetting<V> entry) {
        entry.markRemoved();
        // Never written to the file, so there is nothing to flush
        if (entry.isAdded()) {
            unregisterEntry(entry);
        }
    }

    void unregisterEntry(AdminSetting<V> entry) {
        entries.values().remove(entry);
        AdminSettingAPI.unregister(entry);
    }

    void unregisterAll() {
        for (AdminSetting<V> entry : new ArrayList<>(entries.values())) {
            AdminSettingAPI.unregister(entry);
        }
        entries.clear();
    }

    /**
     * Registers an entry for every key in the config section
     */
    void load() {
        ConfigurationSection section = AdminSetting.loadConfig(plugin, fileName).getConfigurationSection(path);
        if (section == null) {
            return;
        }
        for (String rawKey : section.getKeys(false)) {
            K key = parseKey(rawKey);
            if (key == null) {
                CivModCorePlugin.getInstance().warning("Ignoring invalid key " + rawKey + " at " + path + " in "
                    + fileName + " of " + plugin.getName());
                continue;
            }
            // Keep the key as written, so flushing changes the existing line
            attach(key, AdminSettingAPI.registerSetting(createType(key), fileName, path + "." + rawKey, menu));
        }
    }

    /**
     * Re-reads all entries from the config file, throwing away unflushed changes. Notifies listeners of every entry
     * whose live value changed
     */
    public void reload() {
        Map<K, V> oldValues = getValues();
        unregisterAll();
        load();
        Map<K, V> newValues = getValues();
        Set<K> keys = new HashSet<>(oldValues.keySet());
        keys.addAll(newValues.keySet());
        for (K key : keys) {
            if (!Objects.equals(oldValues.get(key), newValues.get(key))) {
                notifyListeners(key, oldValues.get(key), newValues.get(key));
            }
        }
    }

    private void attach(K key, AdminSetting<V> entry) {
        entry.setMap(this);
        entry.registerListener((setting, oldValue, newValue) -> notifyListeners(key, oldValue, newValue));
        entries.put(key, entry);
    }

    private PlayerSetting<V> createType(K key) {
        String entryIdentifier = identifier + "." + keySerializer.apply(key);
        PlayerSetting<V> type = entryFactory.apply(key, entryIdentifier);
        if (!entryIdentifier.equals(type.getIdentifier()) || type.getOwningPlugin() != plugin) {
            throw new IllegalStateException("Entries of admin setting map " + identifier + " must be owned by "
                + plugin.getName() + " and use the identifier they are given, " + entryIdentifier);
        }
        return type;
    }

    /**
     * Registers a listener which will be triggered if an entry is added, removed or changed
     *
     * @param listener Listener to register
     */
    public void registerListener(AdminSettingMapListener<K, V> listener) {
        if (this.listeners == null) {
            this.listeners = new ArrayList<>();
        }
        this.listeners.add(listener);
    }

    public void unregisterListener(AdminSettingMapListener<K, V> listener) {
        if (this.listeners != null) {
            this.listeners.remove(listener);
        }
    }

    private void notifyListeners(K key, V oldValue, V newValue) {
        if (listeners == null) {
            return;
        }
        for (AdminSettingMapListener<K, V> listener : listeners) {
            listener.handle(this, key, oldValue, newValue);
        }
    }

}
