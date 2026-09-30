package com.untamedears.jukealert.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Description;
import com.untamedears.jukealert.JukeAlert;
import com.untamedears.jukealert.util.JASettingsManager;
import java.util.Set;
import java.util.TreeSet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.bukkit.entity.Player;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;

public class MuteListCommand extends BaseCommand {

    @CommandAlias("jamutelist")
    @Description("Lists all groups you have snitch notifications ignored.")
    public void execute(Player sender){
        JASettingsManager settingsManager = JukeAlert.getInstance().getSettingsManager();
        Set<String> ignoredGroups = settingsManager.getIgnoredGroupAlerts().getValue(sender.getUniqueId());
        if (ignoredGroups == null || ignoredGroups.isEmpty()) {
            CivUI.info(sender, "You haven't muted snitch alerts from any group");
            return;
        }
        Component groups = Component.join(JoinConfiguration.commas(true),
            new TreeSet<>(ignoredGroups).stream().map(CivText::group).toList());
        CivUI.info(sender, "You've muted snitch alerts from ", groups);
    }
}
