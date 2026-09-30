package vg.civcraft.mc.namelayer.command.commands;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Syntax;
import java.util.UUID;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.namelayer.GroupManager.PlayerType;
import vg.civcraft.mc.namelayer.NameLayerAPI;
import vg.civcraft.mc.namelayer.command.BaseCommandMiddle;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.misc.NLText;
import vg.civcraft.mc.namelayer.permission.PermissionType;

public class SetPassword extends BaseCommandMiddle {

    @CommandAlias("nlsp|setpassword")
    @Syntax("<group> <password>")
    @Description("Set a password on a group.")
    @CommandCompletion("@NL_Groups")
    public void execute(CommandSender sender, String groupName, String userPassword) {
        if (!(sender instanceof Player)) {
            CivUI.error(sender, "Only players can set group passwords");
            return;
        }
        Player p = (Player) sender;
        UUID uuid = NameLayerAPI.getUUID(p.getName());
        Group g = gm.getGroup(groupName);
        if (groupIsNull(sender, groupName, g)) {
            return;
        }

        PlayerType pType = g.getPlayerType(uuid);
        if (pType == null) {
            CivUI.error(p, "You're not on ", NLText.group(g));
            return;
        }

        if (!gm.hasAccess(g, uuid, PermissionType.getPermission("PASSWORD"))) {
            CivUI.error(p, "You don't have permission to set the password of ", NLText.group(g));
            return;
        }

        String password = null;
        if (userPassword != null)
            password = userPassword;
        g.setPassword(password);
        CivUI.success(p, "Set the password of ", NLText.group(g), " to ", CivText.value(g.getPassword()));
    }
}
