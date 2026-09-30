package com.untamedears.jukealert.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Description;
import com.untamedears.jukealert.model.Snitch;
import com.untamedears.jukealert.model.appender.SnitchLogAppender;
import com.untamedears.jukealert.util.JAUtility;
import com.untamedears.jukealert.util.JukeAlertPermissionHandler;
import com.untamedears.jukealert.util.JAText;
import org.bukkit.entity.Player;
import vg.civcraft.mc.civmodcore.ui.CivUI;

public class ClearCommand extends BaseCommand {

    @CommandAlias("jaclear")
    @Description("Deletes all logs a snitch has")
    public void execute(final Player sender, String[] args) {
        Snitch snitch = JAUtility.findLookingAtOrClosestSnitch(sender, JukeAlertPermissionHandler.getClearLogs());
        if (snitch == null) {
            CivUI.error(sender, "There's no snitch near you whose logs you can clear");
            return;
        }
        SnitchLogAppender logAppender = (SnitchLogAppender) snitch.getAppender(SnitchLogAppender.class);
        if (logAppender == null) {
            CivUI.error(sender, JAText.snitch(snitch), " doesn't keep logs");
            return;
        }
        CivUI.confirm("Clear the logs of ", JAText.snitch(snitch), "?")
            .warning("You can't undo this")
            .yesLabel("Clear")
            .noLabel("Keep them")
            .onYes(player -> {
                // Checked again: permissions may have changed while the dialog was open
                if (!snitch.hasPermission(player, JukeAlertPermissionHandler.getClearLogs())) {
                    CivUI.error(player, "You can no longer clear the logs of ", JAText.snitch(snitch));
                    return;
                }
                logAppender.deleteLogs();
                CivUI.success(player, "Cleared the logs of ", JAText.snitch(snitch));
            })
            .open(sender);
    }
}
