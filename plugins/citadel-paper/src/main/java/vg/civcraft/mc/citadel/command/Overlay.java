package vg.civcraft.mc.citadel.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Syntax;
import org.bukkit.entity.Player;
import vg.civcraft.mc.citadel.Citadel;
import vg.civcraft.mc.citadel.model.CitadelSettingManager;
import vg.civcraft.mc.civmodcore.players.settings.impl.BooleanSetting;
import vg.civcraft.mc.civmodcore.players.settings.impl.BoundedIntegerSetting;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;

public class Overlay extends BaseCommand {

    @CommandAlias("ctv|ctoverlay")
    @Syntax("[radius]")
    @Description("Toggles labels with group and health on nearby reinforced blocks, a radius turns them on at that range")
    public void execute(Player player, @Optional Integer radius) {
        if (Citadel.getInstance().getOverlayManager() == null) {
            CivUI.error(player, "The reinforcement overlay needs ProtocolLib");
            return;
        }
        CitadelSettingManager settings = Citadel.getInstance().getSettingManager();
        BooleanSetting overlay = settings.getOverlay();
        if (radius != null) {
            BoundedIntegerSetting radiusSetting = settings.getOverlayRadius();
            if (!radiusSetting.isValidValue(String.valueOf(radius))) {
                CivUI.error(player, "The radius has to be between ", CivText.value("2"), " and ", CivText.value("6"));
                return;
            }
            radiusSetting.setValue(player.getUniqueId(), radius);
            overlay.setValue(player.getUniqueId(), true);
            CivUI.success(player, "Reinforcement overlay ", CivText.toggle(true), " within ",
                CivText.value(radius + " blocks"));
            return;
        }
        overlay.toggleValue(player.getUniqueId());
        CivUI.success(player, "Reinforcement overlay ", CivText.toggle(overlay.getValue(player.getUniqueId())));
    }
}
