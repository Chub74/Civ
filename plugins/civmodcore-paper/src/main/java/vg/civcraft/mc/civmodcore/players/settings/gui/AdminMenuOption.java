package vg.civcraft.mc.civmodcore.players.settings.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import vg.civcraft.mc.civmodcore.inventory.gui.Clickable;
import vg.civcraft.mc.civmodcore.inventory.gui.IClickable;
import vg.civcraft.mc.civmodcore.players.settings.AdminSetting;
import vg.civcraft.mc.civmodcore.players.settings.impl.BooleanSetting;
import vg.civcraft.mc.civmodcore.ui.CivMenu;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;

public class AdminMenuOption extends MenuItem {

    private final AdminSetting<?> setting;

    public AdminMenuOption(MenuSection menu, AdminSetting<?> setting) {
        super(setting.getNiceName(), menu);
        this.setting = setting;
    }

    @Override
    public IClickable getMenuRepresentation(Player player) {
        ItemStack item = setting.getGuiRepresentation();
        CivMenu.lore(item,
            CivMenu.stat("Default", setting.getDefaultValueText()),
            CivMenu.stat("Config", setting.getFileName() + ": " + setting.getPath()));
        if (setting.hasUnflushedChange()) {
            CivMenu.lore(item, CivMenu.stat("In config", setting.getFlushedValueText()),
                CivText.info("Not flushed to config yet"));
        }
        CivMenu.lore(item, Component.empty(), CivMenu.hint("Click to change"));
        if (setting.isChanged()) {
            CivMenu.lore(item, CivMenu.hint("Shift right click to reset to default"));
        }
        return new Clickable(item) {

            @Override
            public void clicked(@NotNull Player p) {
                change(p);
            }

            @Override
            protected void onShiftRightClick(@NotNull Player p) {
                setting.reset();
                CivUI.success(p, "Reset ", CivText.subject(setting.getNiceName()), " to ",
                    CivText.value(setting.getValueText()));
                getParent().showScreen(p);
            }
        };
    }

    @SuppressWarnings("unchecked")
    private void change(Player player) {
        // Per-player click handlers would write to the clicking admin's own value, so only toggles are special cased
        if (setting.getType() instanceof BooleanSetting) {
            AdminSetting<Boolean> toggle = (AdminSetting<Boolean>) setting;
            toggle.setValue(!toggle.getValue());
            getParent().showScreen(player);
            return;
        }
        CivUI.prompt(setting.getNiceName())
            .label("New value")
            .initial(setting.getSerializedValue())
            .validate(setting::isValidValue, "Invalid input")
            .onSubmit((p, value) -> {
                setting.setValueFromString(value);
                CivUI.success(p, "Set ", CivText.subject(setting.getNiceName()), " to ", CivText.value(value));
                getParent().showScreen(p);
            })
            .onCancel(getParent()::showScreen)
            .open(player);
    }

    public AdminSetting<?> getSetting() {
        return this.setting;
    }

}
