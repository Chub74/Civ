package vg.civcraft.mc.civmodcore.commands;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.command.UnknownCommandEvent;
import vg.civcraft.mc.civmodcore.ui.Bedrock;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.civmodcore.ui.Kind;

/**
 * Replaces the vanilla "Unknown or incomplete command" reply to a command no plugin registered.
 */
public final class UnknownCommandListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onUnknownCommand(final UnknownCommandEvent event) {
        if (event.message() == null) {
            return;
        }
        final String label = event.getCommandLine().split(" ", 2)[0];
        event.message(Bedrock.adapt(event.getSender(), CivUI.render(Kind.ERROR, event.getSender() instanceof Player, false,
            "There's no command called ", CivText.value("/" + label))));
    }
}
