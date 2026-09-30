package vg.civcraft.mc.namelayer.command.commands;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Syntax;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.namelayer.GroupManager.PlayerType;
import vg.civcraft.mc.namelayer.NameLayerAPI;
import vg.civcraft.mc.namelayer.NameLayerPlugin;
import vg.civcraft.mc.namelayer.command.BaseCommandMiddle;
import vg.civcraft.mc.namelayer.command.TabCompleters.GroupTabCompleter;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.misc.NLText;
import vg.civcraft.mc.namelayer.permission.PermissionType;

public class GroupStats extends BaseCommandMiddle {

    @CommandAlias("nlgs")
    @CommandPermission("namelayer.admin")
    @Syntax("<group>")
    @Description("Get stats about a group.")
    public void execute(Player sender, String groupName) {
        Player p = (Player) sender;
        Group g = gm.getGroup(groupName);
        UUID uuid = NameLayerAPI.getUUID(p.getName());

        if (groupIsNull(sender, groupName, g)) {
            return;
        }
        PlayerType pType = g.getPlayerType(uuid);
        if (!g.isMember(uuid) && !(p.isOp() || p.hasPermission("namelayer.admin"))) {
            CivUI.error(p, "You're not on ", NLText.group(g));
            return;
        }
        boolean hasPerm = NameLayerAPI.getGroupManager().hasAccess(g, uuid, PermissionType.getPermission("GROUPSTATS"));
        if (!(p.isOp() || p.hasPermission("namelayer.admin")) && !hasPerm) {
            CivUI.error(p, "You don't have permission to see the stats of ", NLText.group(g));
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(NameLayerPlugin.getInstance(), new StatsMessage(p, g));
    }

    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (!(sender instanceof Player))
            return null;

        if (args.length > 0)
            return GroupTabCompleter.complete(args[args.length - 1], null, (Player) sender);
        else {
            return GroupTabCompleter.complete(null, null, (Player) sender);
        }
    }

    public class StatsMessage implements Runnable {

        private final Player p;
        private final Group g;

        public StatsMessage(Player p, Group g) {
            this.p = p;
            this.g = g;
        }

        @Override
        public void run() {
            List<Component> lines = new ArrayList<>();
            for (PlayerType type : PlayerType.values()) {
                List<UUID> members = g.getAllMembers(type);
                lines.add(Component.text()
                    .append(NLText.rank(type))
                    .append(Component.text(": ", CivText.INFO))
                    .append(members.isEmpty() ? CivText.info("none") : NLText.players(members))
                    .build());
            }
            lines.add(Component.text()
                .append(CivText.value(String.valueOf(g.getAllMembers().size())))
                .append(CivText.info(" members in total"))
                .build());
            if (p != null && !p.isOnline()) // meh be safe
                return;
            p.sendMessage(NLText.list(CivText.heading(g.getName()), lines));
        }

    }
}
