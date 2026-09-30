package vg.civcraft.mc.namelayer.command.commands;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Syntax;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;
import vg.civcraft.mc.namelayer.GroupManager.PlayerType;
import vg.civcraft.mc.namelayer.NameLayerAPI;
import vg.civcraft.mc.namelayer.command.BaseCommandMiddle;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.permission.PermissionType;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.civmodcore.ui.Kind;
import vg.civcraft.mc.namelayer.misc.NLText;

public class DeleteGroup extends BaseCommandMiddle {

    private Map<UUID, String[]> confirmDeleteGroup = new HashMap<UUID, String[]>();

    @CommandAlias("nldg|delete|deletegroup|groupdelete")
    @Syntax("<group>")
    @Description("Delete a group.")
    @CommandCompletion("@NL_Groups")
    public void execute(Player sender, String groupName) {
        Player p = (Player) sender;
        UUID uuid = NameLayerAPI.getUUID(p.getName());
        String x = groupName;
        String confirm = "CONFIRM DELETION";
        if (x.toLowerCase().contains(confirm.toLowerCase())) {
            //check if they met the 15 second window
            if (confirmDeleteGroup.containsKey(uuid)) {
                //user is in the hashmap
                String[] entry = confirmDeleteGroup.get(uuid);
                Group gD = gm.getGroup(entry[0]);

                if (x.equalsIgnoreCase("%s %s".formatted(confirm, gD.getName()))) {

                    //player could have lost delete permission in the mean time
                    if (!NameLayerAPI.getGroupManager().hasAccess(gD, uuid, PermissionType.getPermission("DELETE"))) {
                        CivUI.error(p, "You don't have permission to delete ", NLText.group(gD));
                        return;
                    }
                    Date now = new Date(System.currentTimeMillis() - 15000);
                    //if it has been less than 15 seconds
                    if (now.getTime() < Long.parseLong(entry[1])) {
                        //good to go delete the group
                        if (gm.deleteGroup(gD.getName()))
                            CivUI.success(p, "Deleted the group ", NLText.group(gD));
                        else
                            CivUI.warn(p, NLText.group(gD), " is disciplined while it's deleted. Check back later");

                        confirmDeleteGroup.remove(uuid);
                        return;
                    } else {
                        CivUI.error(p, "You didn't confirm within 15 seconds. Run ", NLText.command("/nldg " + gD.getName()), " to start over");
                        confirmDeleteGroup.remove(uuid);
                        return;
                    }
                }
            }


        }
        Group g = gm.getGroup(x);
        if (groupIsNull(sender, x, g)) {
            return;
        }
        if (!NameLayerAPI.getGroupManager().hasAccess(g, uuid, PermissionType.getPermission("DELETE"))) {
            CivUI.error(p, "You don't have permission to delete ", NLText.group(g));
            return;
        }
        PlayerType pType = g.getPlayerType(uuid);
        if (pType == null && !p.hasPermission("namelayer.admin")) {
            CivUI.error(p, "You're not on ", NLText.group(g));
            return;
        }
        if (g.isDisciplined() && !p.hasPermission("namelayer.admin")) {
            CivUI.error(p, NLText.group(g), " is disciplined");
            return;
        }
        //set that user can confirm group in 15 seconds
        Date date = new Date();
        Long dateString = date.getTime();
        String[] groupDate = new String[]{g.getName(), dateString.toString()};
        CivUI.notice(Kind.WARN, "This deletes ", NLText.group(g), " and every reinforcement, bastion and snitch on it. Run ",
            NLText.command("/nldg %s %s".formatted(confirm, g.getName())), " within 15 seconds to confirm")
            .icon()
            .to(p)
            .send();
        confirmDeleteGroup.put(uuid, groupDate);
        return;
    }
}
