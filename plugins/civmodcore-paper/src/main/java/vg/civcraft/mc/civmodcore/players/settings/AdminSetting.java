package vg.civcraft.mc.civmodcore.players.settings;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * A value in one of a plugin's config files which admins can change at runtime. Obtained by registering a
 * {@link PlayerSetting} with {@link AdminSettingAPI}, which is used purely as the value type: it decides parsing,
 * validation, display and the GUI item.
 * <p>
 * Changing the value only changes it live, so admins can try a value without committing to it. The config file is
 * only written once the change is flushed with {@link #flush()}; unflushed changes are lost on restart. Plugins
 * usually parse their config once on startup, so they should register a listener to apply changes live.
 */
public final class AdminSetting<T> {

    /**
     * Key the current value is cached under in the wrapped setting
     */
    private static final UUID GLOBAL = new UUID(0L, 0L);

    private static final String MAIN_CONFIG = "config.yml";

    private final PlayerSetting<T> type;
    private final String fileName;
    private final String path;
    private List<AdminSettingChangeListener<T>> listeners;
    /**
     * Value as it is in the config file
     */
    private T flushedValue;

    AdminSetting(PlayerSetting<T> type, String fileName, String path) {
        this.type = type;
        this.fileName = fileName;
        this.path = path;
    }

    /**
     * @return The setting defining how values of this setting are parsed and shown
     */
    public PlayerSetting<T> getType() {
        return type;
    }

    public String getIdentifier() {
        return type.getIdentifier();
    }

    public String getNiceName() {
        return type.getNiceName();
    }

    public String getDescription() {
        return type.getDescription();
    }

    public JavaPlugin getOwningPlugin() {
        return type.getOwningPlugin();
    }

    /**
     * @return Name of the config file within the owning plugin's data folder this setting is stored in
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * @return Path within the config file this setting is stored at
     */
    public String getPath() {
        return path;
    }

    /**
     * @return The live value, which may not be flushed to the config file yet
     */
    public T getValue() {
        return type.getValue(GLOBAL);
    }

    /**
     * @return Value used if the config does not contain one, and which {@link #reset()} writes
     */
    public T getDefaultValue() {
        return type.getDefaultValue();
    }

    /**
     * @return The value in the config file, or the default value if the config does not contain one
     */
    public T getFlushedValue() {
        return flushedValue;
    }

    public String getFlushedValueText() {
        return type.toText(flushedValue);
    }

    /**
     * Changes the live value and notifies listeners. The config file is not touched until {@link #flush()}
     *
     * @param value New value
     */
    public void setValue(T value) {
        T oldValue = getValue();
        type.setValueInternal(GLOBAL, value);
        notifyListeners(oldValue, value);
    }

    /**
     * @return Whether the live value differs from the one in the config file
     */
    public boolean hasUnflushedChange() {
        return !Objects.equals(getValue(), flushedValue);
    }

    /**
     * Writes the live value to the config file, if it differs from it
     */
    public void flush() {
        if (!hasUnflushedChange()) {
            return;
        }
        T value = getValue();
        FileConfiguration config = loadConfig();
        config.set(path, toYaml(value));
        saveConfig(config);
        flushedValue = value;
    }

    /**
     * Throws away the unflushed change, setting the live value back to the one in the config file
     */
    public void discard() {
        if (hasUnflushedChange()) {
            setValue(flushedValue);
        }
    }

    /**
     * Parses and sets a new value. Check {@link #isValidValue(String)} first
     *
     * @param input Serialized value
     */
    public void setValueFromString(String input) {
        setValue(type.deserialize(input));
    }

    public boolean isValidValue(String input) {
        return type.isValidValue(input);
    }

    /**
     * Sets the live value back to the default. Like any change, this must be flushed to reach the config file
     */
    public void reset() {
        setValue(getDefaultValue());
    }

    /**
     * @return Whether the current value differs from the default
     */
    public boolean isChanged() {
        return !Objects.equals(getValue(), getDefaultValue());
    }

    /**
     * Re-reads the value from the config file, for example after it was edited by hand. Throws away any unflushed
     * change and notifies listeners if the live value changed
     */
    public void reload() {
        T oldValue = getValue();
        load();
        T newValue = getValue();
        if (!Objects.equals(oldValue, newValue)) {
            notifyListeners(oldValue, newValue);
        }
    }

    public String getSerializedValue() {
        return type.serialize(getValue());
    }

    /**
     * @return Human readable form of the current value
     */
    public String getValueText() {
        return type.toText(getValue());
    }

    public String getDefaultValueText() {
        return type.toText(getDefaultValue());
    }

    /**
     * @return ItemStack showing the current value, for GUIs
     */
    public ItemStack getGuiRepresentation() {
        return type.getGuiRepresentation(GLOBAL);
    }

    /**
     * Registers a listener which will be triggered if the value is changed
     *
     * @param listener Listener to register
     */
    public void registerListener(AdminSettingChangeListener<T> listener) {
        if (this.listeners == null) {
            this.listeners = new ArrayList<>();
        }
        this.listeners.add(listener);
    }

    public void unregisterListener(AdminSettingChangeListener<T> listener) {
        if (this.listeners != null) {
            this.listeners.remove(listener);
        }
    }

    private void notifyListeners(T oldValue, T newValue) {
        if (listeners == null) {
            return;
        }
        for (AdminSettingChangeListener<T> listener : listeners) {
            listener.handle(this, oldValue, newValue);
        }
    }

    /**
     * Reads the value from the config file, falling back to the default if it is missing
     *
     * @throws IllegalArgumentException If the value in the config can not be parsed
     */
    void load() {
        Object raw = loadConfig().get(path);
        if (raw == null) {
            flushedValue = getDefaultValue();
            type.setValueInternal(GLOBAL, flushedValue);
            return;
        }
        String serial = String.valueOf(raw);
        if (!type.isValidValue(serial)) {
            flushedValue = getDefaultValue();
            type.setValueInternal(GLOBAL, flushedValue);
            throw new IllegalArgumentException("[" + serial + "] at " + path + " in " + fileName
                + " is not a valid value for admin setting " + getIdentifier());
        }
        flushedValue = type.deserialize(serial);
        type.setValueInternal(GLOBAL, flushedValue);
    }

    /**
     * Keeps numbers, booleans and strings as their YAML types so the config file stays readable by the plugin
     */
    private Object toYaml(T value) {
        if (value instanceof Number || value instanceof Boolean || value instanceof String) {
            return value;
        }
        return type.serialize(value);
    }

    private boolean isMainConfig() {
        return MAIN_CONFIG.equals(fileName);
    }

    private File getFile() {
        return new File(getOwningPlugin().getDataFolder(), fileName);
    }

    private FileConfiguration loadConfig() {
        // Go through the plugin's own instance for config.yml, so what the plugin reads stays in sync
        if (isMainConfig()) {
            return getOwningPlugin().getConfig();
        }
        return YamlConfiguration.loadConfiguration(getFile());
    }

    private void saveConfig(FileConfiguration config) {
        if (isMainConfig()) {
            getOwningPlugin().saveConfig();
            return;
        }
        try {
            config.save(getFile());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save " + fileName + " of " + getOwningPlugin().getName(), e);
        }
    }

}
