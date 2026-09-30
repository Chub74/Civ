package vg.civcraft.mc.namelayer.command.commands;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Syntax;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.civmodcore.ui.Kind;
import vg.civcraft.mc.namelayer.GroupManager.PlayerType;
import vg.civcraft.mc.namelayer.GroupManager;
import vg.civcraft.mc.namelayer.NameLayerAPI;
import vg.civcraft.mc.namelayer.NameLayerPlugin;
import vg.civcraft.mc.namelayer.command.BaseCommandMiddle;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.listeners.PlayerListener;
import vg.civcraft.mc.namelayer.misc.NLText;
import vg.civcraft.mc.namelayer.permission.PermissionType;

public class InvitePlayer extends BaseCommandMiddle {

    @CommandAlias("nlip|invite|inviteplayer")
    @Syntax("<group> <player> [rank (eg: MEMBERS)]")
    @Description("Invite a player to a group.")
    @CommandCompletion("@NL_Groups @allplayers @NL_Ranks")
    public void execute(CommandSender s, String groupName, String playerName, @Optional String playerRank) {
        final String targetGroup = groupName;
        final String targetPlayer = playerName;
        final String targetType = playerRank;
        final boolean isPlayer = s instanceof Player;
        final Player p = isPlayer ? (Player) s : null;
        final boolean isAdmin = !isPlayer || p.hasPermission("namelayer.admin");
        final Group group = GroupManager.getGroup(targetGroup);
        if (groupIsNull(s, targetGroup, group)) {
            return;
        }
        if (!isAdmin && group.isDisciplined()) {
            CivUI.error(s, NLText.group(group), " is disciplined");
            return;
        }
        final UUID targetAccount = NameLayerAPI.getUUID(targetPlayer);
        if (targetAccount == null) {
            CivUI.error(s, CivText.subject(targetPlayer), " has never played here");
            return;
        }
        final PlayerType pType = targetType != null ? PlayerType.getPlayerType(targetType) : PlayerType.MEMBERS;
        if (pType == null) {
            PlayerType.displayPlayerTypes(s);
            return;
        }
        if (pType == PlayerType.NOT_BLACKLISTED) {
            CivUI.error(s, "You can't invite someone as ", NLText.rank(pType));
            return;
        }
        if (!isAdmin) {
            // Perform access check
            final UUID executor = p.getUniqueId();
            final PlayerType t = group.getPlayerType(executor); // playertype for the player running the command.
            if (t == null) {
                CivUI.error(s, "You're not on ", NLText.group(group));
                return;
            }
            boolean allowed = false;
            switch (pType) { // depending on the type the executor wants to add the player to
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
                CivUI.error(s, "You don't have permission to invite ", NLText.rank(pType), " to ", NLText.group(group));
                return;
            }
        }

        if (group.isCurrentMember(targetAccount)) { // So a player can't demote someone who is above them.
            CivUI.error(s, NLText.player(targetAccount), " is already a member. Change their rank with ",
                NLText.command("/nlpp " + group.getName() + " " + NameLayerAPI.getCurrentName(targetAccount) + " "));
            return;
        }
        if (NameLayerPlugin.getBlackList().isBlacklisted(group, targetAccount)) {
            CivUI.error(s, NLText.player(targetAccount), " is blacklisted. Unblacklist them first with ",
                NLText.command("/nlubl " + group.getName() + " " + NameLayerAPI.getCurrentName(targetAccount)));
            return;
        }
        if (!isAdmin) {
            sendInvitation(group, pType, targetAccount, p.getUniqueId(), true);
        } else {
            sendInvitation(group, pType, targetAccount, null, true);
        }

        CivUI.notice(Kind.SUCCESS, "Invited ", NLText.player(targetAccount), " to the group ", NLText.group(group), " as ", NLText.rank(pType))
            .hover(CivText.info("Undo with /nlri " + group.getName() + " " + NameLayerAPI.getCurrentName(targetAccount)))
            .to(s)
            .send();
    }

    public static void sendInvitation(Group group, PlayerType pType, UUID invitedPlayer, UUID inviter, boolean saveToDB) {
        Player invitee = Bukkit.getPlayer(invitedPlayer);
        boolean shouldAutoAccept = NameLayerPlugin.getAutoAcceptHandler().getAutoAccept(invitedPlayer);
        if (invitee != null) {
            // invitee is online
            if (shouldAutoAccept) {
                // player auto accepts invite
                if (saveToDB) {
                    group.addMember(invitedPlayer, pType);
                } else {
                    group.addMember(invitedPlayer, pType, false);
                }
                CivUI.success(invitee, "You auto-accepted an invite to the group ", NLText.group(group));
            } else {
                group.addInvite(invitedPlayer, pType, saveToDB);
                PlayerListener.addNotification(invitedPlayer, group);
                Component by = inviter == null
                    ? Component.empty()
                    : Component.text().append(Component.text(" by ")).append(NLText.player(inviter)).build();
                CivUI.notice(Kind.INFO, "You're invited to the group ", NLText.group(group), " as ", NLText.rank(pType), by,
                        ". Click to accept")
                    .hover(CivText.info("Accept the invite. /autoaccept accepts every invite for you"))
                    .click(ClickEvent.runCommand("/nlag " + group.getName()))
                    .to(invitee)
                    .send();
            }
        } else {
            // invitee is offline or on a different shard
            if (shouldAutoAccept) {
                if (saveToDB) {
                    group.addMember(invitedPlayer, pType);
                } else {
                    group.addMember(invitedPlayer, pType, false);
                }
            } else {
                // Player did not auto accept
                group.addInvite(invitedPlayer, pType, saveToDB);
            }
            PlayerListener.addNotification(invitedPlayer, group);
        }
    }
}
