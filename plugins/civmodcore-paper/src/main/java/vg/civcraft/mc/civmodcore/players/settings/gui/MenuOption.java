package vg.civcraft.mc.civmodcore.players.settings.gui;

import net.kyori.adventure.text.Component;
import vg.civcraft.mc.civmodcore.ui.CivMenu;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import vg.civcraft.mc.civmodcore.inventory.gui.Clickable;
import vg.civcraft.mc.civmodcore.inventory.gui.IClickable;
import vg.civcraft.mc.civmodcore.players.settings.PlayerSetting;

public class MenuOption extends MenuItem {

    private final PlayerSetting<?> setting;

    public MenuOption(MenuSection menu, PlayerSetting<?> setting) {
        super(setting.getNiceName(), menu);
        this.setting = setting;
    }

    @Override
    public IClickable getMenuRepresentation(Player player) {
        ItemStack item = setting.getGuiRepresentation(player.getUniqueId());
        CivMenu.lore(item, Component.empty(), CivMenu.hint("Click to change"));
        return new Clickable(item) {

            @Override
            public void clicked(Player p) {
                setting.handleMenuClick(p, getParent());
            }
        };
    }

    public PlayerSetting<?> getSetting() {
        return this.setting;
    }

}
