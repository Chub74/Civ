package vg.civcraft.mc.civmodcore.players.settings.gui;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import vg.civcraft.mc.civmodcore.inventory.gui.Clickable;
import vg.civcraft.mc.civmodcore.inventory.gui.MultiPageView;
import vg.civcraft.mc.civmodcore.players.settings.AdminSetting;
import vg.civcraft.mc.civmodcore.players.settings.AdminSettingAPI;
import vg.civcraft.mc.civmodcore.ui.CivMenu;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;

/**
 * Menu section of admin settings, with buttons to flush or discard the unflushed changes of the settings it covers
 */
public class AdminMenuSection extends MenuSection {

    private static final int FLUSH_SLOT = 3;
    private static final int DISCARD_SLOT = 5;

    private final Supplier<Collection<AdminSetting<?>>> settings;

    /**
     * @param settings Settings the flush and discard buttons apply to
     */
    public AdminMenuSection(String name, String description, MenuSection parent,
                            Supplier<Collection<AdminSetting<?>>> settings) {
        super(name, description, parent, new ItemStack(Material.COMMAND_BLOCK));
        this.settings = settings;
    }

    @Override
    protected void addMenuSlots(MultiPageView pageView, Player player) {
        List<AdminSetting<?>> unflushed = settings.get().stream().filter(AdminSetting::hasUnflushedChange).toList();
        if (unflushed.isEmpty()) {
            return;
        }
        ItemStack flushItem = new ItemStack(Material.LIME_CONCRETE);
        CivMenu.name(flushItem, "Flush to config");
        CivMenu.lore(flushItem, CivMenu.stat("Unflushed changes", String.valueOf(unflushed.size())));
        for (AdminSetting<?> setting : unflushed) {
            CivMenu.lore(flushItem, CivText.info(setting.getNiceName() + ": " + setting.getFlushedValueText()
                + " -> " + setting.getValueText()));
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

}
