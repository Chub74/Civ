package vg.civcraft.mc.namelayer.command.commands;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Syntax;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.namelayer.GroupManager;
import vg.civcraft.mc.namelayer.NameLayerAPI;
import vg.civcraft.mc.namelayer.command.BaseCommandMiddle;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.misc.NLText;
import vg.civcraft.mc.namelayer.permission.PermissionType;

public class SetGroupColor extends BaseCommandMiddle {

    @CommandAlias("nlsgc|setgroupcolor")
    @Syntax("<group> <color>")
    @Description("Set the groups color")
    @CommandCompletion("@NL_Groups @ADV_Colors")
    public void execute(Player player, String groupName, String targetColor) {
        UUID uuid = NameLayerAPI.getUUID(player.getName());
        Group group = gm.getGroup(groupName);
        if (groupIsNull(player, groupName, group)) {
            return;
        }

        if (targetColor == null || targetColor.isEmpty()) {
            CivUI.error(player, "Give a colour name or a hex value");
            return;
        }

        TextColor color = NamedTextColor.NAMES.value(targetColor);
        if (color == null) {
            color = TextColor.fromHexString(targetColor);
        }
        if (color == null) {
            CivUI.error(player, CivText.value(targetColor), " isn't a colour name or hex value");
            return;
        }
        GroupManager.PlayerType playerType = group.getPlayerType(uuid);
        if (playerType == null) {
            CivUI.error(player, "You're not on ", NLText.group(group));
            return;
        }
        if (!gm.hasAccess(group, uuid, PermissionType.getPermission("EDIT_COLOR"))) {
            CivUI.error(player, "You don't have permission to change the colour of ", NLText.group(group));
            return;
        }
        group.setGroupColor(color);
        CivUI.success(player, "Changed the colour of ", NLText.group(group), " to ",
            Component.text(group.getName(), group.getGroupColor()));
    }

}
