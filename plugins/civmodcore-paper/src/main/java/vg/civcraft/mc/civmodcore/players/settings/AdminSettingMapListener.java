package vg.civcraft.mc.civmodcore.players.settings;

/**
 * Allows listening for changes in an admin setting map, including entries being added or removed
 */
@FunctionalInterface
public interface AdminSettingMapListener<K, V> {

    /**
     * @param oldValue Previous value, null if the entry was just added
     * @param newValue New value, null if the entry was just removed
     */
    void handle(AdminSettingMap<K, V> map, K key, V oldValue, V newValue);

}
