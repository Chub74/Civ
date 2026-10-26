package vg.civcraft.mc.civmodcore.players.settings;

/**
 * Allows listening for value changes in an admin setting
 */
@FunctionalInterface
public interface AdminSettingChangeListener<T> {

    void handle(AdminSetting<T> setting, T oldValue, T newValue);

}
