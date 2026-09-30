package com.untamedears.jukealert.commands;

import static com.untamedears.jukealert.util.JAUtility.findLookingAtOrClosestSnitch;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Syntax;
import com.untamedears.jukealert.model.Snitch;
import com.untamedears.jukealert.util.JAUtility;
import com.untamedears.jukealert.util.JukeAlertPermissionHandler;
import org.bukkit.entity.Player;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.namelayer.permission.PermissionType;

public class NameCommand extends BaseCommand {

    @CommandAlias("janame")
    @Syntax("<name>")
    @Description("Name a snitch")
    public void execute(Player player, String snitchName) {
        Snitch snitch = findLookingAtOrClosestSnitch(player, getPermission());
        if (snitch == null) {
            CivUI.error(player, "There's no snitch near you that you can rename");
            return;
        }

        JAUtility.renameSnitch(player, snitch, snitchName);
    }

    private static PermissionType getPermission() {
        return JukeAlertPermissionHandler.getRenameSnitch();
    }
}
