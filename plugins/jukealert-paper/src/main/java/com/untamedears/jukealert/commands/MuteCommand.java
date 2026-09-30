package com.untamedears.jukealert.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Syntax;
import com.untamedears.jukealert.JukeAlert;
import com.untamedears.jukealert.util.JASettingsManager;
import java.util.List;
import org.bukkit.entity.Player;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.namelayer.GroupManager;
import vg.civcraft.mc.namelayer.group.Group;

public class MuteCommand extends BaseCommand {

    @CommandAlias("jamute")
    @Syntax("<group>")
    @Description("Adds or removes from a snitch notification ignore list.")
    public void execute(Player player, String targetGroup) {
        if (targetGroup == null) {
            return;
        }
        Group group = GroupManager.getGroup(targetGroup);
        if (group == null) {
            CivUI.error(player, "There's no group called ", CivText.group(targetGroup));
            return;
        }
        JASettingsManager settingsManager = JukeAlert.getInstance().getSettingsManager();
        if (settingsManager.doesIgnoreAlert(group.getName(), player.getUniqueId())) {
            settingsManager.getIgnoredGroupAlerts().removeElement(player.getUniqueId(), group.getName());
            CivUI.success(player, "Unmuted snitch alerts from ", CivText.group(group.getName()));
            return;
        }
        settingsManager.getIgnoredGroupAlerts().addElement(player.getUniqueId(), group.getName());
        CivUI.success(player, "Muted snitch alerts from ", CivText.group(group.getName()));
    }
}
