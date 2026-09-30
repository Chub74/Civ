package com.untamedears.jukealert.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Description;
import com.untamedears.jukealert.JukeAlert;
import com.untamedears.jukealert.gui.SnitchLogGUI;
import com.untamedears.jukealert.gui.SnitchOverviewGUI;
import com.untamedears.jukealert.model.Snitch;
import com.untamedears.jukealert.util.JAUtility;
import com.untamedears.jukealert.util.JukeAlertPermissionHandler;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import org.bukkit.entity.Player;
import vg.civcraft.mc.civmodcore.ui.CivUI;

public class GUICommand extends BaseCommand {

    @CommandAlias("ja")
    @Description("Opens snitch log GUI")
    public void execute(Player player) {
        Snitch cursorSnitch = JAUtility.getSnitchUnderCursor(player);
        if (cursorSnitch != null && cursorSnitch.hasPermission(player, JukeAlertPermissionHandler.getReadLogs())) {
            SnitchLogGUI gui = new SnitchLogGUI(player, cursorSnitch);
            gui.showScreen();
            return;
        }
        // No snitch under cursor, so search around player
        Collection<Snitch> snitches = JukeAlert.getInstance().getSnitchManager()
            .getSnitchesCovering(player.getLocation());
        // Sort out the ones the player has no perms for
        Iterator<Snitch> iter = snitches.iterator();
        while (iter.hasNext()) {
            Snitch snitch = iter.next();
            if (!snitch.hasPermission(player, JukeAlertPermissionHandler.getReadLogs())) {
                iter.remove();
            }
        }
        if (snitches.isEmpty()) {
            CivUI.error(player, "There's no snitch near you whose logs you can read");
            return;
        }
        if (snitches.size() == 1) {
            SnitchLogGUI gui = new SnitchLogGUI(player, snitches.iterator().next());
            gui.showScreen();
            return;
        }
        SnitchOverviewGUI gui = new SnitchOverviewGUI(player, new ArrayList<>(snitches), "Nearby Snitches", true);
        gui.showScreen();
    }
}
