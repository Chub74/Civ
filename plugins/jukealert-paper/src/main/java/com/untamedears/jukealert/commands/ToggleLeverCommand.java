package com.untamedears.jukealert.commands;

import static com.untamedears.jukealert.util.JAUtility.findLookingAtOrClosestSnitch;


import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Description;
import com.untamedears.jukealert.model.Snitch;
import com.untamedears.jukealert.model.appender.LeverToggleAppender;
import com.untamedears.jukealert.util.JukeAlertPermissionHandler;
import com.untamedears.jukealert.util.JAText;
import org.bukkit.entity.Player;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.namelayer.permission.PermissionType;

public class ToggleLeverCommand extends BaseCommand {

    @CommandAlias("jatogglelevers")
    @Description("Toggles flag that indicates if a juke should trigger a lever.")
    public void execute(Player player, String[] args) {
        if (args.length >= 1) {
            return;
        }
        PermissionType togglePerm = JukeAlertPermissionHandler.getToggleLevers();
        Snitch snitch = findLookingAtOrClosestSnitch(player, togglePerm);
        if (snitch == null) {
            CivUI.error(player, "There's no snitch near you whose levers you can toggle");
            return;
        }
        if (!snitch.hasAppender(LeverToggleAppender.class)) {
            CivUI.error(player, JAText.snitch(snitch), " can't toggle levers");
            return;
        }
        LeverToggleAppender toggleAppender = (LeverToggleAppender) snitch.getAppender(LeverToggleAppender.class);
        toggleAppender.switchState();
        CivUI.success(player, "Lever toggling is now ", CivText.toggle(toggleAppender.shouldToggle()), " for ",
            JAText.snitch(snitch));
    }
}
