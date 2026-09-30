package vg.civcraft.mc.namelayer.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import vg.civcraft.mc.civmodcore.inventory.gui.Clickable;
import vg.civcraft.mc.civmodcore.inventory.gui.ClickableInventory;
import vg.civcraft.mc.civmodcore.inventory.gui.DecorationStack;
import vg.civcraft.mc.civmodcore.inventory.items.ItemUtils;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.namelayer.GroupManager.PlayerType;
import vg.civcraft.mc.namelayer.NameLayerPlugin;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.misc.NLText;
import vg.civcraft.mc.namelayer.permission.GroupPermission;
import vg.civcraft.mc.namelayer.permission.PermissionType;

public class PermissionManageGUI extends AbstractGroupGUI {

    private MainGroupGUI parent;
    private int currentPage;

    public PermissionManageGUI(Group g, Player p, MainGroupGUI parentGUI) {
        super(g, p);
        this.parent = parentGUI;
        currentPage = 0;
    }

    public void showScreen() {
        ClickableInventory ci = new ClickableInventory(27, MenuUtils.title(g.getName()));
        if (!validGroup()) {
            return;
        }
        //dye blacklisted clickable black
        Clickable blackClick = produceSelectionClickable(Material.LEATHER_CHESTPLATE, PlayerType.NOT_BLACKLISTED);
        ItemStack blackStack = blackClick.getItemStack();
        LeatherArmorMeta meta = (LeatherArmorMeta) blackStack.getItemMeta();
        meta.setColor(Color.BLACK);
        blackStack.setItemMeta(meta);
        ci.setSlot(blackClick, 9);
        ci.setSlot(
            produceSelectionClickable(Material.LEATHER_CHESTPLATE,
                PlayerType.MEMBERS), 11);
        ci.setSlot(
            produceSelectionClickable(modMat(),
                PlayerType.MODS), 13);
        ci.setSlot(
            produceSelectionClickable(Material.IRON_CHESTPLATE,
                PlayerType.ADMINS), 15);
        ci.setSlot(
            produceSelectionClickable(Material.DIAMOND_CHESTPLATE,
                PlayerType.OWNER), 17);
        ItemStack backStack = new ItemStack(Material.ARROW);
        MenuUtils.name(backStack, Component.text("Go back to member management", CivText.VALUE));
        ci.setSlot(new Clickable(backStack) {

            @Override
            public void clicked(Player arg0) {
                parent.showScreen();
            }
        }, 22);
        ci.showInventory(p);
    }

    private Clickable produceSelectionClickable(Material mat,
                                                final PlayerType pType) {
        ItemStack is = new ItemStack(mat);
        ItemMeta im = is.getItemMeta();
        im.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        is.setItemMeta(im);
        Clickable c;
        MenuUtils.name(is, Component.text("View and edit permissions for " + PlayerType.getNiceRankName(pType), CivText.VALUE));
        if (!gm.hasAccess(g, p.getUniqueId(),
            PermissionType.getPermission("LIST_PERMS"))) {
            MenuUtils.lore(is, Component.text("You are not allowed to list", CivText.ERROR), Component.text("permissions for this group", CivText.ERROR));
            c = new DecorationStack(is);
        } else {
            c = new Clickable(is) {

                @Override
                public void clicked(Player arg0) {
                    showPermissionEditing(pType);
                }
            };
        }
        return c;
    }

    private void showPermissionEditing(final PlayerType pType) {
        if (!validGroup()) {
            return;
        }
        if (!gm.hasAccess(g, p.getUniqueId(),
            PermissionType.getPermission("LIST_PERMS"))) {
            CivUI.error(p, "You don't have permission to see the permissions of ", NLText.group(g));
            showScreen();
            return;
        }
        ClickableInventory ci = new ClickableInventory(54, MenuUtils.title(g.getName()));
        final List<Clickable> clicks = new ArrayList<>();
        final GroupPermission gp = gm.getPermissionforGroup(g);

        final PermissionType[] permissions = PermissionType.getAllPermissions().toArray(new PermissionType[0]);
        Arrays.sort(permissions, Comparator.comparing(PermissionType::getName));
        for (final PermissionType perm : permissions) {
            ItemStack is = null;
            Clickable c;
            final boolean hasPerm = gp.hasPermission(pType, perm);
            boolean canEdit = gm.hasAccess(g, p.getUniqueId(),
                PermissionType.getPermission("PERMS"));

            if (hasPerm) {
                is = yesStack();
                MenuUtils.lore(is, Component.text("" + PlayerType.getNiceRankName(pType) + "s currently have", CivText.INFO), Component.text("this permission", CivText.INFO));
            } else {
                is = noStack();
                MenuUtils.lore(is, Component.text("" + PlayerType.getNiceRankName(pType) + "s currently don't have", CivText.INFO), Component.text("this permission", CivText.INFO));
            }
            MenuUtils.name(is, Component.text(String.valueOf(perm.getName()), CivText.VALUE));
            String desc = perm.getDescription();
            if (desc != null) {
                final int MAX_CHARS = 35;
                String[] words = desc.split(" ");
                StringBuilder line = new StringBuilder();
                for (int i = 0; i < words.length; i++) {
                    line.append(words[i]).append(" ");
                    if (line.length() >= MAX_CHARS || i == words.length - 1) {
                        MenuUtils.lore(is, Component.text(String.valueOf(line.toString().trim()), CivText.INFO));
                        line = new StringBuilder();
                    }
                }
            }
            if (pType == PlayerType.NOT_BLACKLISTED && !perm.getCanBeBlacklisted()) {
                canEdit = false;

                MenuUtils.lore(is, Component.text("This permission cannot be toggled for " + PlayerType.getNiceRankName(pType), CivText.INFO));
            }

            if (canEdit) {
                MenuUtils.lore(is, MenuUtils.hint("Click to toggle"));
                c = new Clickable(is) {

                    @Override
                    public void clicked(Player arg0) {
                        if (hasPerm == gp.hasPermission(pType, perm)) { // recheck
                            if (gm.hasAccess(g, p.getUniqueId(),
                                PermissionType.getPermission("PERMS"))) {
                                NameLayerPlugin.log(Level.INFO, p.getName()
                                    + (hasPerm ? " removed " : " added ")
                                    + "the permission " + perm.getName()
                                    + "for player type " + pType.toString()
                                    + " for " + g.getName() + " via the gui");
                                if (hasPerm) {
                                    gp.removePermission(pType, perm);
                                } else {
                                    gp.addPermission(pType, perm);
                                }
                            }
                        } else {
                            CivUI.error(p, "The permissions changed while you were editing them, so nothing was saved");
                        }
                        showPermissionEditing(pType);
                    }
                };
            } else {
                c = new DecorationStack(is);
            }
            clicks.add(c);
        }

        for (int i = 45 * currentPage; i < 45 * (currentPage + 1) && i < clicks.size(); i++) {
            ci.setSlot(clicks.get(i), i - (45 * currentPage));
        }

        if (currentPage > 0) {
            ItemStack back = new ItemStack(Material.ARROW);
            MenuUtils.name(back, Component.text("Go to previous page", CivText.VALUE));
            Clickable baCl = new Clickable(back) {

                @Override
                public void clicked(Player arg0) {
                    if (currentPage > 0) {
                        currentPage--;
                    }
                    showPermissionEditing(pType);
                }
            };
            ci.setSlot(baCl, 45);
        }
        // next button
        if ((45 * (currentPage + 1)) < clicks.size()) {
            ItemStack forward = new ItemStack(Material.ARROW);
            MenuUtils.name(forward, Component.text("Go to next page", CivText.VALUE));
            Clickable forCl = new Clickable(forward) {

                @Override
                public void clicked(Player arg0) {
                    if ((45 * (currentPage + 1)) < clicks.size()) {
                        currentPage++;
                    }
                    showPermissionEditing(pType);
                }
            };
            ci.setSlot(forCl, 53);
        }

        ItemStack backToOverview = goBackStack();
        MenuUtils.name(backToOverview, Component.text("Go back", CivText.VALUE));
        ci.setSlot(new Clickable(backToOverview) {

            @Override
            public void clicked(Player arg0) {
                showScreen();
            }
        }, 49);
        ci.showInventory(p);
    }

}
