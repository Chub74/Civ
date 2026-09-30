package com.untamedears.jukealert.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Syntax;
import com.untamedears.jukealert.JukeAlert;
import com.untamedears.jukealert.gui.SnitchOverviewGUI;
import com.untamedears.jukealert.model.Snitch;
import com.untamedears.jukealert.model.appender.DormantCullingAppender;
import com.untamedears.jukealert.util.JukeAlertPermissionHandler;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.namelayer.GroupManager;
import vg.civcraft.mc.namelayer.NameLayerAPI;
import vg.civcraft.mc.namelayer.command.TabCompleters.GroupTabCompleter;
import vg.civcraft.mc.namelayer.group.Group;

public class ListCommand extends BaseCommand {

    @CommandAlias("jalist")
    @Syntax("[group]")
    @Description("Lists all snitches you have access to if given no arguments or the ones on the given groups")
    public void execute(final Player player, @Optional final String[] targetGroups) {
        boolean playerProvidedGroups = false;
        List<String> groupNames = null;
        if (targetGroups.length != 0) {
            groupNames = new ArrayList<String>(Arrays.asList(targetGroups));
            playerProvidedGroups = true;
        } else {
            groupNames = NameLayerAPI.getGroupManager().getAllGroupNames(player.getUniqueId());
        }
        final var groupIds = new IntArrayList();
        for (final String groupName : groupNames) {
            final Group group = GroupManager.getGroup(groupName);
            if (group == null) {
                if (playerProvidedGroups) {
                    CivUI.error(player, "There's no group called ", CivText.group(groupName));
                }
                continue;
            }
            if (!NameLayerAPI.getGroupManager().hasAccess(group, player.getUniqueId(),
                JukeAlertPermissionHandler.getListSnitches())) {
                if (playerProvidedGroups) {
                    CivUI.error(player, "You don't have permission to list the snitches on ", CivText.group(group.getName()));
                }
                continue;
            }
            groupIds.addAll(group.getGroupIds());
        }
        if (groupIds.isEmpty()) {
            CivUI.info(player, "You can't list the snitches of any group");
            return;
        }
        CivUI.info(player, "Loading snitches from ", CivText.value(String.valueOf(groupNames.size())),
            groupNames.size() == 1 ? " group" : " groups", ", which may take a moment");
        JukeAlert.getInstance().getTaskChainFactory().newChain()
            .async((unused) -> JukeAlert.getInstance().getDAO().loadSnitchesByGroupID(groupIds).parallel()
                .map((snitch) -> {
                    final DormantCullingAppender appender = snitch.getAppender(DormantCullingAppender.class);
                    if (appender == null) {
                        return null;
                    }
                    return new SnitchCache(snitch, appender.getTimeUntilCulling());
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingLong((entry) -> entry.timeUntilCulling))
                .map((entry) -> entry.snitch)
                .collect(Collectors.toList()))
            .syncLast((snitches) -> new SnitchOverviewGUI(player, snitches, "Your Snitches",
                player.hasPermission("jukealert.admin")).showScreen())
            .execute();
    }

    private static class SnitchCache {

        private final Snitch snitch;
        private final long timeUntilCulling;

        public SnitchCache(@NotNull final Snitch snitch,
                           final long timeUntilCulling) {
            this.snitch = snitch;
            this.timeUntilCulling = timeUntilCulling;
        }
    }

    public List<String> tabComplete(final CommandSender sender, final String[] arguments) {
        final String last = ArrayUtils.isEmpty(arguments) ? "" : arguments[arguments.length - 1];
        return GroupTabCompleter.complete(last, JukeAlertPermissionHandler.getListSnitches(), (Player) sender);
    }

}
