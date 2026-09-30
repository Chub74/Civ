package vg.civcraft.mc.namelayer.command.commands;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Syntax;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
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

public class ListSubGroups extends BaseCommandMiddle {


    @CommandAlias("nllsg|subgroups|listsubgroups")
    @Syntax("[group]")
    @Description("List the nested group hierarchy.")
    public void execute(CommandSender sender, @Optional String targetGroupName) {
        if (!(sender instanceof Player)) {
            CivUI.error(sender, "Only players have subgroups to list");
            return;
        }

        UUID uuid = NameLayerAPI.getUUID(sender.getName());

        List<Component> lines = new ArrayList<>();

        if (targetGroupName != null) {
            String groupname = targetGroupName;

            Group group = GroupManager.getGroup(groupname);
            if (groupIsNull(sender, groupname, group)) {
                return;
            }

            if (!gm.hasAccess(group, uuid, PermissionType.getPermission("GROUPSTATS"))) {
                CivUI.error(sender, "You don't have permission to see the subgroups of ", NLText.group(group));
                return;
            }

            lines.add(line("", group, uuid));
            buildList(lines, uuid, group.getSubgroups(), "  ");
        } else {
            List<String> groups = gm.getAllGroupNames(uuid);
            Set<String> supergroups = Sets.newHashSet(groups);

            for (String groupname : groups) {
                Group group = GroupManager.getGroup(groupname);
                removeSubs(group.getSubgroups(), supergroups);
            }

            for (String supergroup : supergroups) {
                Group group = GroupManager.getGroup(supergroup);
                lines.add(line("", group, uuid));
                buildList(lines, uuid, group.getSubgroups(), "  ");
            }
        }
        sender.sendMessage(NLText.list(CivText.heading("Group Hierarchy"), lines));
    }

    private void removeSubs(List<Group> subs, Set<String> groups) {
        for (Group group : subs) {
            groups.remove(group.getName());
            removeSubs(group.getSubgroups(), groups);
        }
    }

    private void buildList(List<Component> lines, UUID uuid, List<Group> subs, String prefix) {
        for (Group group : subs) {
            lines.add(line(prefix + "\u2514 ", group, uuid));
            buildList(lines, uuid, group.getSubgroups(), "  " + prefix);
        }
    }

    private static Component line(String prefix, Group group, UUID uuid) {
        PlayerType rank = group.getPlayerType(uuid);
        return Component.text()
            .append(Component.text(prefix, CivText.MUTED))
            .append(NLText.group(group))
            .append(Component.text(rank == null ? "" : " " + rank.name(), CivText.MUTED))
            .build();
    }
}
