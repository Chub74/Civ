package vg.civcraft.mc.civmodcore.players.settings.gui;

import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import vg.civcraft.mc.civmodcore.inventory.gui.Clickable;
import vg.civcraft.mc.civmodcore.inventory.gui.MultiPageView;
import vg.civcraft.mc.civmodcore.players.settings.AdminSetting;
import vg.civcraft.mc.civmodcore.players.settings.AdminSettingAPI;
import vg.civcraft.mc.civmodcore.players.settings.AdminSettingMap;
import vg.civcraft.mc.civmodcore.ui.CivMenu;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;

/**
 * Menu section of admin settings, with buttons to flush or discard the unflushed changes of all settings within it,
 * including those in subsections
 */
public class AdminMenuSection extends MenuSection {

    private static final int ADD_SLOT = 1;
    private static final int FLUSH_SLOT = 3;
    private static final int DISCARD_SLOT = 5;

    private final String pluginName;

    /**
     * Use {@link AdminSettingAPI#getPluginMenu(org.bukkit.plugin.Plugin)} and
     * {@link #getSubsection(String, String)} instead of creating sections directly
     *
     * @param pluginName Plugin whose settings this section holds, null for the main menu
     */
    public AdminMenuSection(String name, String description, AdminMenuSection parent, String pluginName,
                            ItemStack itemRepresentation) {
        super(name, description, parent, itemRepresentation);
        this.pluginName = pluginName;
    }

    /**
     * @return Name of the plugin whose settings this section holds, null for the main menu
     */
    public String getPluginName() {
        return pluginName;
    }

    /**
     * Gets the subsection with the given name, creating it shown as a book if it does not exist yet. Safe to call
     * again on every config parse
     *
     * @param name        Name of the subsection, unique within this section
     * @param description Description shown on the subsection's item
     * @return The subsection
     */
    public AdminMenuSection getSubsection(String name, String description) {
        return getSubsection(name, description, null);
    }

    /**
     * Gets the subsection with the given name, creating it if it does not exist yet. Safe to call again on every
     * config parse
     *
     * @param name        Name of the subsection, unique within this section
     * @param description Description shown on the subsection's item, only applied when it is created
     * @param icon        Item the subsection is shown as, null to keep the current one or use a book
     * @return The subsection
     */
    public AdminMenuSection getSubsection(String name, String description, ItemStack icon) {
        for (MenuItem item : getItems()) {
            if (item instanceof AdminMenuSection section && section.getName().equals(name)) {
                if (icon != null) {
                    section.setItemRepresentation(icon);
                }
                return section;
            }
        }
        AdminMenuSection section = new AdminMenuSection(name, description, this, pluginName,
            icon == null ? new ItemStack(Material.BOOK) : icon);
        addItem(section);
        return section;
    }

    /**
     * @param setting Setting to check
     * @return Whether the setting is shown in this section or one of its subsections
     */
    public boolean contains(AdminSetting<?> setting) {
        for (MenuSection section = setting.getMenu(); section != null; section = section.getParent()) {
            if (section == this) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return All settings shown in this section or one of its subsections
     */
    public List<AdminSetting<?>> getSettings() {
        return AdminSettingAPI.getAllSettings().stream().filter(this::contains).toList();
    }

    @Override
    protected void addMenuSlots(MultiPageView pageView, Player player) {
        AdminSettingMap<?, ?> map = AdminSettingAPI.getMap(this);
        if (map != null) {
            ItemStack addItem = new ItemStack(Material.EMERALD);
            CivMenu.name(addItem, "Add entry");
            CivMenu.lore(addItem, CivText.info("Adds an entry to " + map.getPath() + " in " + map.getFileName()),
                CivMenu.hint("Click to add"));
            pageView.setMenuSlot(new Clickable(addItem) {

                @Override
                public void clicked(@NotNull Player p) {
                    promptKey(p, map);
                }
            }, ADD_SLOT);
        }
        List<AdminSetting<?>> unflushed = getSettings().stream().filter(AdminSetting::hasUnflushedChange).toList();
        if (unflushed.isEmpty()) {
            return;
        }
        ItemStack flushItem = new ItemStack(Material.LIME_CONCRETE);
        CivMenu.name(flushItem, "Flush to config");
        CivMenu.lore(flushItem, CivMenu.stat("Unflushed changes", String.valueOf(unflushed.size())));
        for (AdminSetting<?> setting : unflushed) {
            String change;
            if (setting.isRemoved()) {
                change = "removed";
            } else if (setting.isAdded()) {
                change = "added as " + setting.getValueText();
            } else {
                change = setting.getFlushedValueText() + " -> " + setting.getValueText();
            }
            CivMenu.lore(flushItem, CivText.info(setting.getNiceName() + ": " + change));
        }
        CivMenu.lore(flushItem, CivMenu.hint("Click to write these to the config files"));
        pageView.setMenuSlot(new Clickable(flushItem) {

            @Override
            public void clicked(@NotNull Player p) {
                int flushed = AdminSettingAPI.flush(unflushed);
                CivUI.success(p, "Flushed ", CivText.value(String.valueOf(flushed)), " admin settings to config");
                showScreen(p);
            }
        }, FLUSH_SLOT);

        ItemStack discardItem = new ItemStack(Material.RED_CONCRETE);
        CivMenu.name(discardItem, "Discard changes");
        CivMenu.lore(discardItem, CivMenu.hint("Click to set these back to their config values"));
        pageView.setMenuSlot(new Clickable(discardItem) {

            @Override
            public void clicked(@NotNull Player p) {
                int discarded = AdminSettingAPI.discard(unflushed);
                CivUI.success(p, "Discarded ", CivText.value(String.valueOf(discarded)), " admin setting changes");
                showScreen(p);
            }
        }, DISCARD_SLOT);
    }

    private <K> void promptKey(Player player, AdminSettingMap<K, ?> map) {
        CivUI.prompt("Add entry")
            .label("Key")
            .validate(map::isValidNewKey, "Invalid key, or it already exists")
            .onSubmit((p, input) -> promptValue(p, map, map.parseKey(input)))
            .onCancel(this::showScreen)
            .open(player);
    }

    private <K> void promptValue(Player player, AdminSettingMap<K, ?> map, K key) {
        CivUI.prompt("Add entry")
            .label("Value")
            .initial(map.getSerializedDefault(key))
            .validate(value -> map.isValidValue(key, value), "Invalid value")
            .onSubmit((p, value) -> {
                AdminSetting<?> entry = map.addSerialized(key, value);
                CivUI.success(p, "Added ", CivText.subject(entry.getNiceName()), " as ",
                    CivText.value(entry.getValueText()), ", flush to write it to the config");
                showScreen(p);
            })
            .onCancel(this::showScreen)
            .open(player);
    }

}
