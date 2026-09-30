package vg.civcraft.mc.civmodcore.players.settings;

import org.bukkit.entity.Player;
import vg.civcraft.mc.civmodcore.players.settings.gui.MenuSection;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;

/** Opens a prompt for a new value of a setting, then returns to the menu. */
public class MenuDialog {

    public MenuDialog(Player player, PlayerSetting<?> setting, MenuSection menu, String errorMsg) {
        CivUI.prompt(setting.getNiceName())
            .label("New value")
            .initial(setting.getSerializedValueFor(player.getUniqueId()))
            .validate(setting::isValidValue, errorMsg)
            .onSubmit((p, value) -> {
                setting.setValueFromString(p.getUniqueId(), value);
                CivUI.success(p, "Set ", CivText.subject(setting.getNiceName()), " to ", CivText.value(value));
                menu.showScreen(p);
            })
            .onCancel(menu::showScreen)
            .open(player);
    }

}
