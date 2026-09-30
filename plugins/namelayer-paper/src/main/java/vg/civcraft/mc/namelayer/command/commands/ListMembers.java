package vg.civcraft.mc.namelayer.command.commands;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Syntax;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.namelayer.GroupManager.PlayerType;
import vg.civcraft.mc.namelayer.GroupManager;
import vg.civcraft.mc.namelayer.NameLayerAPI;
import vg.civcraft.mc.namelayer.command.BaseCommandMiddle;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.misc.NLText;
import vg.civcraft.mc.namelayer.permission.PermissionType;

public class ListMembers extends BaseCommandMiddle {

    @CommandAlias("nllm|listmembers|members")
    @Syntax("<group> [rank (e.g: MEMBERS)]")
    @Description("List the members in a group")
    @CommandCompletion("@NL_Groups @NL_Ranks")
    public void execute(Player sender, String groupName, @Optional String playerType, @Optional String playerName) {
        Player p = (Player) sender;
        UUID uuid = NameLayerAPI.getUUID(p.getName());
        String groupname = groupName;

        Group group = GroupManager.getGroup(groupname);
        if (groupIsNull(sender, groupname, group)) {
            return;
        }

        if (!p.hasPermission("namelayer.admin")) {
            if (!group.isMember(uuid)) {
                CivUI.error(p, "You're not on ", NLText.group(group));
                return;
            }

            if (!gm.hasAccess(group, uuid, PermissionType.getPermission("GROUPSTATS"))) {
                CivUI.error(p, "You don't have permission to see the members of ", NLText.group(group));
                return;
            }
        }

        List<UUID> uuids = null;
        if (playerType != null && playerName != null) {
            String nameMin = playerType, nameMax = playerName;

            List<UUID> members = group.getAllMembers();
            uuids = Lists.newArrayList();

            for (UUID member : members) {
                String name = NameLayerAPI.getCurrentName(member);
                if (name.compareToIgnoreCase(nameMin) >= 0
                    && name.compareToIgnoreCase(nameMax) <= 0) {
                    uuids.add(member);
                }
            }
        } else if (playerType != null && playerName == null) {
            String playerRank = playerType;
            PlayerType filterType = PlayerType.getPlayerType(playerRank);

            if (filterType == null) {
                // user entered invalid type, show them
                PlayerType.displayPlayerTypes(p);
                return;
            }

            uuids = group.getAllMembers(filterType);
        } else {
            uuids = group.getAllMembers();
        }

        List<Component> lines = new ArrayList<>();
        for (UUID uu : uuids) {
            lines.add(Component.text()
                .append(NLText.player(uu))
                .append(Component.text(" " + group.getPlayerType(uu).name(), CivText.MUTED))
                .build());
        }
        p.sendMessage(NLText.list(CivText.heading("Members of " + group.getName()), lines));
    }
}
