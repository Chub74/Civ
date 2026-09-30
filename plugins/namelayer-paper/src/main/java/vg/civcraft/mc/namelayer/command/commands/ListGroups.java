package vg.civcraft.mc.namelayer.command.commands;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Syntax;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.namelayer.GroupManager;
import vg.civcraft.mc.namelayer.NameLayerAPI;
import vg.civcraft.mc.namelayer.command.BaseCommandMiddle;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.misc.NLText;

public class ListGroups extends BaseCommandMiddle {

    @CommandAlias("nllg|listgroups|groups")
    @Syntax("[page]")
    @Description("List groups.")
    public void execute(CommandSender sender, @Optional String pageNumber) {
        Player p = null;
        UUID uuid = null;
        boolean autopages = false;

        if ((sender.isOp() || sender.hasPermission("namelayer.admin"))) {
            if (pageNumber == null) {
                uuid = NameLayerAPI.getUUID(sender.getName());
            } else if (pageNumber != null) {
                uuid = NameLayerAPI.getUUID(sender.getName());
            }

            if (uuid == null) {
                CivUI.error(sender, "You have no account to list groups for");
                return;
            }
            autopages = true;
        } else {
            p = (Player) sender;
            uuid = NameLayerAPI.getUUID(p.getName());
        }

        List<String> groups = gm.getAllGroupNames(uuid);

        int pages = (groups.size() / 10);
        if (groups.size() % 10 > 0) {
            pages++;
        }
        if (pages == 0) {
            pages = 1;
        }
        int actualPages = pages;

        int target = 1;
        if (pageNumber != null) {
            try {
                target = Integer.parseInt(pageNumber);
            } catch (NumberFormatException e) {
                CivUI.error(sender, CivText.value(pageNumber), " isn't a page number");
                return;
            }
        }

        if (target >= pages) {
            target = pages;
        }

        if (!autopages) {
            pages = target;
        }

        for (int page = target; page <= pages; page++) {
            List<Component> lines = new ArrayList<>();
            int first = (page - 1) * 10;
            for (int x = first; x < first + 10 && x < groups.size(); x++) {
                Group g = GroupManager.getGroup(groups.get(x));
                lines.add(Component.text()
                    .append(NLText.group(g))
                    .append(Component.text(" " + g.getPlayerType(uuid).name(), CivText.MUTED))
                    .build());
            }
            Component heading = Component.text()
                .append(CivText.heading("Your Groups"))
                .append(Component.text(" " + page + "/" + actualPages, CivText.MUTED))
                .build();
            sender.sendMessage(NLText.list(heading, lines));
        }
    }
}
