package vg.civcraft.mc.namelayer.command.commands;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Syntax;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.namelayer.GroupManager.PlayerType;
import vg.civcraft.mc.namelayer.NameLayerAPI;
import vg.civcraft.mc.namelayer.command.BaseCommandMiddle;
import vg.civcraft.mc.namelayer.events.PromotePlayerEvent;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.misc.NLText;
import vg.civcraft.mc.namelayer.permission.PermissionType;

public class PromotePlayer extends BaseCommandMiddle {

    @CommandAlias("nlpp|promote|promoteplayer")
    @Syntax("<group> <player> <rank>")
    @Description("Promote/Demote a Player in a Group")
    @CommandCompletion("@NL_Groups @allplayers @NL_Ranks")
    public void execute(CommandSender sender, String groupName, String playerName, String playerType) {
        final boolean isAdmin = sender instanceof ConsoleCommandSender || sender.hasPermission("namelayer.admin");

        UUID executor = sender instanceof Player p ? NameLayerAPI.getUUID(p.getName()) : null;
        UUID promotee = NameLayerAPI.getUUID(playerName);

        if (promotee == null) {
            CivUI.error(sender, "There's no player called ", CivText.subject(playerName));
            return;
        }

        if (promotee.equals(executor)) {
            CivUI.error(sender, "You can't change your own rank");
            return;
        }

        Group group = gm.getGroup(groupName);
        if (groupIsNull(sender, groupName, group)) {
            return;
        }
        if (group.isDisciplined()) {
            CivUI.error(sender, NLText.group(group), " is disciplined");
            return;
        }

        PlayerType promoteecurrentType = group.getPlayerType(promotee);
        PlayerType promoteeType = PlayerType.getPlayerType(playerType);
        if (promoteeType == null) {
            PlayerType.displayPlayerTypes(sender);
            return;
        }
        if (promoteeType == PlayerType.NOT_BLACKLISTED) {
            CivUI.error(sender, "You can't give anyone the rank ", NLText.rank(promoteeType));
            return;
        }

        boolean allowed = false;
        if (!isAdmin) {
            PlayerType t = group.getPlayerType(executor); // playertype for the player running the command.

            if (t == null) {
                CivUI.error(sender, "You're not on ", NLText.group(group));
                return;
            }

            switch (promoteeType) { // depending on the type the executor wants to add the player to
                case MEMBERS:
                    allowed = gm.hasAccess(group, executor, PermissionType.getPermission("MEMBERS"));
                    break;
                case MODS:
                    allowed = gm.hasAccess(group, executor, PermissionType.getPermission("MODS"));
                    break;
                case ADMINS:
                    allowed = gm.hasAccess(group, executor, PermissionType.getPermission("ADMINS"));
                    break;
                case OWNER:
                    allowed = gm.hasAccess(group, executor, PermissionType.getPermission("OWNER"));
                    break;
                default:
                    allowed = false;
                    break;
            }

            if (!allowed) {
                CivUI.error(sender, "You don't have permission to give out ", NLText.rank(promoteeType), " on ", NLText.group(group));
                return;
            }

            if (promoteecurrentType != null) {
                switch (promoteecurrentType) { // depending on the type the executor wants to add the player to
                    case MEMBERS:
                        allowed = gm.hasAccess(group, executor, PermissionType.getPermission("MEMBERS"));
                        break;
                    case MODS:
                        allowed = gm.hasAccess(group, executor, PermissionType.getPermission("MODS"));
                        break;
                    case ADMINS:
                        allowed = gm.hasAccess(group, executor, PermissionType.getPermission("ADMINS"));
                        break;
                    case OWNER:
                        allowed = gm.hasAccess(group, executor, PermissionType.getPermission("OWNER"));
                        break;
                    default:
                        allowed = false;
                        break;
                }
            } else {
                allowed = false;
            }
        } else {
            allowed = true;
        }

        if (!allowed || !group.isMember(promotee)) { //can't edit a player who isn't in the group
            CivUI.error(sender, NLText.player(promotee), " isn't on ", NLText.group(group), ", or you can't change their rank");
            return;
        }

        if (group.isOwner(promotee)) {
            CivUI.error(sender, NLText.player(promotee), " owns ", NLText.group(group), ", so you can't change their rank");
            return;
        }

        OfflinePlayer prom = Bukkit.getOfflinePlayer(promotee);
        if (prom.isOnline()) {
            Player oProm = (Player) prom;
            PromotePlayerEvent event = new PromotePlayerEvent(oProm, group, promoteecurrentType, promoteeType);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return;
            }
            group.removeMember(promotee);
            group.addMember(promotee, promoteeType);
            CivUI.success(sender, NLText.player(promotee), " is now ", NLText.rank(promoteeType), " on ", NLText.group(group));
            CivUI.info(oProm, "You're now ", NLText.rank(promoteeType), " on ", NLText.group(group));
        } else {
            //player is offline change their perms
            group.removeMember(promotee);
            group.addMember(promotee, promoteeType);
            CivUI.success(sender, NLText.player(promotee), " is now ", NLText.rank(promoteeType), " on ", NLText.group(group));
        }
    }
}
