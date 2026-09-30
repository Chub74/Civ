package vg.civcraft.mc.namelayer.gui;

import java.util.UUID;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import vg.civcraft.mc.civmodcore.inventory.gui.Clickable;
import vg.civcraft.mc.civmodcore.inventory.gui.ClickableInventory;
import vg.civcraft.mc.civmodcore.inventory.gui.DecorationStack;
import vg.civcraft.mc.civmodcore.inventory.items.ItemUtils;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.namelayer.GroupManager.PlayerType;
import vg.civcraft.mc.namelayer.NameLayerAPI;
import vg.civcraft.mc.namelayer.NameLayerPlugin;
import vg.civcraft.mc.namelayer.command.commands.InvitePlayer;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.misc.NLText;
import vg.civcraft.mc.namelayer.permission.PermissionType;

public class InvitationGUI extends AbstractGroupGUI {

    private PlayerType selectedType;
    private MainGroupGUI parent;

    public InvitationGUI(Group g, Player p, MainGroupGUI parent) {
        super(g, p);
        this.parent = parent;
        showScreen();
    }

    private void showScreen() {
        ClickableInventory ci = new ClickableInventory(27, MenuUtils.title(g.getName()));

        ItemStack explain = new ItemStack(Material.PAPER);
        MenuUtils.name(explain, Component.text("Select an option", CivText.VALUE));
        MenuUtils.lore(explain, Component.text("Please select the rank ", CivText.INFO), Component.text("you want the invited player to have", CivText.INFO));
        ci.setSlot(new DecorationStack(explain), 4);
        ci.setSlot(produceOptionStack(Material.LEATHER_CHESTPLATE, "member", PlayerType.MEMBERS, PermissionType.getPermission("MEMBERS")), 10);
        ci.setSlot(produceOptionStack(modMat(), "mod", PlayerType.MODS, PermissionType.getPermission("MODS")), 12);
        ci.setSlot(produceOptionStack(Material.IRON_CHESTPLATE, "admin", PlayerType.ADMINS, PermissionType.getPermission("ADMINS")), 14);
        ci.setSlot(produceOptionStack(Material.DIAMOND_CHESTPLATE, "owner", PlayerType.OWNER, PermissionType.getPermission("OWNER")), 16);
        ci.showInventory(p);
    }

    private Clickable produceOptionStack(Material item, String niceRankName, final PlayerType pType, PermissionType perm) {
        ItemStack is = new ItemStack(item);
        ItemMeta im = is.getItemMeta();
        im.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        is.setItemMeta(im);
        MenuUtils.name(is, Component.text("Invite as " + niceRankName, CivText.VALUE));
        Clickable c;
        if (gm.hasAccess(g, p.getUniqueId(), perm)) {
            c = new Clickable(is) {

                @Override
                public void clicked(Player arg0) {
                    selectedType = pType;
                    CivUI.prompt("Invite to ", NLText.group(g), " as ", NLText.rank(pType))
                        .body("Separate several names with spaces to invite them all")
                        .label("Players")
                        .validate(names -> !names.isBlank(), "Type at least one name")
                        .onSubmit((who, names) -> {
                            if (gm.hasAccess(g, p.getUniqueId(), MainGroupGUI.getAccordingPermission(selectedType))) {
                                for (String s : names.trim().split("\\s+")) {
                                    UUID inviteUUID = NameLayerAPI.getUUID(s);
                                    if (inviteUUID == null) {
                                        CivUI.error(p, "There's no player called ", CivText.subject(s));
                                        continue;
                                    }
                                    if (g.isMember(inviteUUID)) { // So a player can't demote someone who is above them.
                                        CivUI.error(p, NLText.player(inviteUUID), " is already a member of ", NLText.group(g));
                                        continue;
                                    }
                                    if (NameLayerPlugin.getBlackList().isBlacklisted(g, inviteUUID)) {
                                        CivUI.error(p, NLText.player(inviteUUID), " is blacklisted. Unblacklist them first");
                                        continue;
                                    }
                                    NameLayerPlugin.log(Level.INFO,
                                        p.getName() + " invited "
                                            + NameLayerAPI.getCurrentName(inviteUUID)
                                            + " to group " + g.getName()
                                            + " via the gui");

                                    InvitePlayer.sendInvitation(g, pType, inviteUUID, p.getUniqueId(), true);

                                    CivUI.success(p, "Invited ", NLText.player(inviteUUID), " to the group ", NLText.group(g), " as ", NLText.rank(pType));
                                }
                            } else {
                                CivUI.error(p, "You don't have permission to invite ", NLText.rank(pType), " to ", NLText.group(g));
                            }
                            parent.showScreen();
                        })
                        .onCancel(who -> parent.showScreen())
                        .open(arg0);
                }
            };
        } else {
            MenuUtils.lore(is, Component.text("You don't have permission to invite " + niceRankName, CivText.ERROR));
            c = new DecorationStack(is);
        }
        return c;
    }
}
