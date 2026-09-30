package vg.civcraft.mc.namelayer.command.commands;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Description;
import java.util.UUID;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.namelayer.NameLayerAPI;
import vg.civcraft.mc.namelayer.NameLayerPlugin;
import vg.civcraft.mc.namelayer.command.BaseCommandMiddle;
import vg.civcraft.mc.namelayer.group.AutoAcceptHandler;

public class ToggleAutoAcceptInvites extends BaseCommandMiddle {

    private AutoAcceptHandler handler = NameLayerPlugin.getAutoAcceptHandler();

    @CommandAlias("nltaai|autoaccept")
    @Description("Toggle the acceptance of invites.")
    public void execute(CommandSender sender) {
        if (!(sender instanceof Player)) {
            CivUI.error(sender, "Only players get invites");
            return;
        }
        Player p = (Player) sender;
        UUID uuid = NameLayerAPI.getUUID(p.getName());
        handler.toggleAutoAccept(uuid, true);
        CivUI.success(p, "Auto-accept invites ", CivText.toggle(handler.getAutoAccept(uuid)));
    }
}
