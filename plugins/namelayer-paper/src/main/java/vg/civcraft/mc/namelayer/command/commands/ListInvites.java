package vg.civcraft.mc.namelayer.command.commands;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Syntax;
import com.google.common.collect.Lists;
import org.bukkit.entity.Player;
import vg.civcraft.mc.namelayer.GroupManager;
import vg.civcraft.mc.namelayer.GroupManager.PlayerType;
import vg.civcraft.mc.namelayer.NameLayerAPI;
import vg.civcraft.mc.namelayer.command.BaseCommandMiddle;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.permission.PermissionType;

import java.util.List;
import java.util.UUID;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.namelayer.misc.NLText;
import net.kyori.adventure.text.Component;
import java.util.ArrayList;

public class ListInvites extends BaseCommandMiddle {

    @CommandAlias("nllim|listinvites|listinvitedmembers")
    @Syntax("<group> [rank (e.g: MEMBERS)]")
    @Description("List the invitees of a group")
    @CommandCompletion("@NL_Groups @NL_Ranks")
    public void execute(Player sender, String groupName, @Optional String playerType, @Optional String playerName) {
        UUID uuid = NameLayerAPI.getUUID(sender.getName());

        Group group = GroupManager.getGroup(groupName);
        if (groupIsNull(sender, groupName, group)) {
            return;
        }

        assert group != null;
        if (!sender.hasPermission("namelayer.admin")) {
            if (!group.isMember(uuid)) {
                CivUI.error(sender, "You're not on ", NLText.group(group));
                return;
            }

            if (!gm.hasAccess(group, uuid, PermissionType.getPermission("GROUPSTATS"))) {
                CivUI.error(sender, "You don't have permission to see the invites of ", NLText.group(group));
                return;
            }
        }

        List<UUID> uuids;
        if (playerType != null && playerName != null) {
            List<UUID> invitees = group.getAllInvites();
            uuids = Lists.newArrayList();

            for (UUID invitee : invitees) {
                String name = NameLayerAPI.getCurrentName(invitee);
                if (name.compareToIgnoreCase(playerType) >= 0
                    && name.compareToIgnoreCase(playerName) <= 0) {
                    uuids.add(invitee);
                }
            }
        } else if (playerType != null) {
            PlayerType filterType = PlayerType.getPlayerType(playerType);
            if (filterType == null) {
                // user entered invalid type, show them
                PlayerType.displayPlayerTypes(sender);
                return;
            }

            uuids = group.getAllInvites(filterType);
        } else {
            uuids = group.getAllInvites();
        }

        if (uuids.isEmpty()) {
            CivUI.info(sender, NLText.group(group), " has no open invites");
            return;
        }
        List<Component> lines = new ArrayList<>();
        for (UUID uu : uuids) {
            lines.add(Component.text()
                .append(NLText.player(uu))
                .append(Component.text(" " + group.getPlayerInviteType(uu).name(), CivText.MUTED))
                .build());
        }
        sender.sendMessage(NLText.list(CivText.heading("Invites to " + group.getName()), lines));
    }
}
