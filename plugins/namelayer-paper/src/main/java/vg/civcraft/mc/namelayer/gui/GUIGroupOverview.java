package vg.civcraft.mc.namelayer.gui;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
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
import vg.civcraft.mc.civmodcore.ui.Kind;
import vg.civcraft.mc.namelayer.GroupManager.PlayerType;
import vg.civcraft.mc.namelayer.GroupManager;
import vg.civcraft.mc.namelayer.NameLayerAPI;
import vg.civcraft.mc.namelayer.NameLayerPlugin;
import vg.civcraft.mc.namelayer.RunnableOnGroup;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.misc.NLText;
import vg.civcraft.mc.namelayer.permission.GroupPermission;
import vg.civcraft.mc.namelayer.permission.PermissionType;

public class GUIGroupOverview {

    private int currentPage;
    private Player p;
    private static GroupManager gm;
    private boolean autoAccept;

    public GUIGroupOverview(Player p) {
        if (gm == null) {
            gm = NameLayerAPI.getGroupManager();
        }
        autoAccept = NameLayerPlugin.getAutoAcceptHandler().getAutoAccept(p.getUniqueId());
        this.p = p;
        this.currentPage = 0;
    }

    public void showScreen() {

        ClickableInventory ci = new ClickableInventory(54, MenuUtils.title("Your Groups"));
        final List<Clickable> groups = getGroupClickables();
        if (groups.size() < 45 * currentPage) {
            // would show an empty page, so go to previous
            currentPage--;
            showScreen();
        }

        for (int i = 45 * currentPage; i < 45 * (currentPage + 1)
            && i < groups.size(); i++) {
            ci.setSlot(groups.get(i), i - (45 * currentPage));
        }

        // create group
        ci.setSlot(getCreateGroupClickable(), 46);
        //join group
        ci.setSlot(getJoinGroupClickable(), 47);

        //autoaccept toggle
        ItemStack toggle = MenuUtils.toggleButton(autoAccept, "Automatically accept group invites", true);
        Clickable toggleClick = new Clickable(toggle) {

            @Override
            public void clicked(Player p) {
                if (autoAccept) {
                    NameLayerPlugin.log(Level.INFO,
                        p.getName() + " turned autoaccept for invites off "
                            + " via the gui");
                    CivUI.success(p, "Auto-accept invites ", CivText.toggle(false));
                } else {
                    NameLayerPlugin.log(Level.INFO,
                        p.getName() + " turned autoaccept for invites on "
                            + " via the gui");
                    CivUI.success(p, "Auto-accept invites ", CivText.toggle(true));
                }
                autoAccept = !autoAccept;
                NameLayerPlugin.getAutoAcceptHandler().toggleAutoAccept(p.getUniqueId(), true);
                showScreen();
            }
        };
        ci.setSlot(toggleClick, 48);

        // previous button
        if (currentPage > 0) {
            ItemStack back = new ItemStack(Material.ARROW);
            MenuUtils.name(back, Component.text("Go to previous page", CivText.VALUE));
            Clickable baCl = new Clickable(back) {

                @Override
                public void clicked(Player arg0) {
                    if (currentPage > 0) {
                        currentPage--;
                    }
                    showScreen();
                }
            };
            ci.setSlot(baCl, 45);
        }
        // next button
        if ((45 * (currentPage + 1)) < groups.size()) {
            ItemStack forward = new ItemStack(Material.ARROW);
            MenuUtils.name(forward, Component.text("Go to next page", CivText.VALUE));
            Clickable forCl = new Clickable(forward) {

                @Override
                public void clicked(Player arg0) {
                    if ((45 * (currentPage + 1)) < groups.size()) {
                        currentPage++;
                    }
                    showScreen();
                }
            };
            ci.setSlot(forCl, 53);
        }

        // close button
        ItemStack backToOverview = AbstractGroupGUI.goBackStack();
        MenuUtils.name(backToOverview, Component.text("Close", CivText.VALUE));
        ci.setSlot(new Clickable(backToOverview) {

            @Override
            public void clicked(Player arg0) {
                ClickableInventory.forceCloseInventory(arg0);
            }
        }, 49);
        ci.showInventory(p);
    }

    private List<Clickable> getGroupClickables() {
        String defaultGroupName = gm.getDefaultGroup(p.getUniqueId());
        List<String> groupNames = gm.getAllGroupNames(p.getUniqueId());
        groupNames.sort(Comparator.comparing((String g) -> {
                Group group = GroupManager.getGroup(g);
                return group != null && group.isOwner(p.getUniqueId());
            }).thenComparing((String g) -> {
                Group group = GroupManager.getGroup(g);
                if (group != null) {
                    return group.getPlayerType(p.getUniqueId());
                } else {
                    return PlayerType.NOT_BLACKLISTED;
                }
            }).reversed()
            .thenComparing(String.CASE_INSENSITIVE_ORDER));
        List<Clickable> result = new ArrayList<Clickable>();
        Set<String> alreadyProcessed = new HashSet<String>();
        for (String groupName : groupNames) {
            if (alreadyProcessed.contains(groupName)) {
                continue;
            }
            final Group g = GroupManager.getGroup(groupName);
            if (g == null) {
                continue;
            }
            Clickable c;
            PlayerType pType = g.getPlayerType(p.getUniqueId());
            if (pType == null) {
                continue;
            }
            ItemStack is = null;
            switch (pType) {
                case MEMBERS:
                    is = new ItemStack(Material.LEATHER_CHESTPLATE);
                    MenuUtils.lore(is, Component.text().append(CivText.info("Your rank ")).append(CivText.value("Member")).build());
                    break;
                case MODS:
                    is = AbstractGroupGUI.modStack();
                    MenuUtils.lore(is, Component.text().append(CivText.info("Your rank ")).append(CivText.value("Mod")).build());
                    break;
                case ADMINS:
                    is = new ItemStack(Material.IRON_CHESTPLATE);
                    MenuUtils.lore(is, Component.text().append(CivText.info("Your rank ")).append(CivText.value("Admin")).build());
                    break;
                case OWNER:
                    is = new ItemStack(Material.DIAMOND_CHESTPLATE);
                    if (g.isOwner(p.getUniqueId())) {
                        MenuUtils.lore(is, Component.text().append(CivText.info("Your rank ")).append(CivText.value("Primary owner")).build());
                    } else {
                        MenuUtils.lore(is, Component.text().append(CivText.info("Your rank ")).append(CivText.value("Owner")).build());
                    }
                    break;
            }
            if (is == null) {
                continue;
            }
            ItemMeta im = is.getItemMeta();
            im.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            if (g.getName().equals(defaultGroupName)) {
                im.setEnchantmentGlintOverride(true);
            }
            is.setItemMeta(im);
            if (g.getName().equals(defaultGroupName)) {
                MenuUtils.lore(is, Component.text("Your default group", CivText.INFO));
            }
            if (gm.hasAccess(g, p.getUniqueId(),
                PermissionType.getPermission("GROUPSTATS"))) {
                MenuUtils.lore(is, Component.text(String.valueOf(g.getAllMembers().size()) + " member" + (g.getAllMembers().size() > 1 ? "s" : ""), CivText.INFO));
            }
            MenuUtils.name(is, NLText.group(g));
            if (gm.hasAccess(g, p.getUniqueId(),
                PermissionType.getPermission("OPEN_GUI"))) {
                c = new Clickable(is) {

                    @Override
                    public void clicked(Player arg0) {
                        MainGroupGUI mgui = new MainGroupGUI(p, g);
                    }
                };
            } else {
                MenuUtils.lore(is, Component.text("You don't have permission to open", CivText.ERROR), Component.text("the GUI for this group", CivText.ERROR));
                c = new DecorationStack(is);
            }
            result.add(c);
            alreadyProcessed.add(groupName);
        }
        return result;
    }

    private Clickable getCreateGroupClickable() {
        ItemStack is = new ItemStack(Material.APPLE);
        MenuUtils.name(is, Component.text("Create group", CivText.VALUE));
        Clickable c = new Clickable(is) {

            @Override
            public void clicked(final Player p) {
                CivUI.prompt("Create a group")
                    .label("Name")
                    .maxLength(32)
                    .check(groupName -> checkNewGroupName(p, groupName.trim()))
                    .onSubmit((who, groupName) -> createGroup(who, groupName.trim()))
                    .onCancel(who -> showScreen())
                    .open(p);
            }
        };
        return c;
    }

    /** What's wrong with the name, or null if a group can be made with it. */
    private Object checkNewGroupName(Player p, String groupName) {
        if (groupName.isEmpty()) {
            return "Type a name first";
        }
        if (groupName.contains(" ")) {
            return "Group names can't contain spaces";
        }
        if (NameLayerPlugin.getInstance().getGroupLimit() < gm
            .countGroups(p.getUniqueId()) + 1
            && !(p.isOp() || p
            .hasPermission("namelayer.admin"))) {
            return Component.text()
                .append(Component.text("You're at the limit of "))
                .append(CivText.value(String.valueOf(NameLayerPlugin.getInstance().getGroupLimit())))
                .append(Component.text(" groups. Delete one before making another"))
                .build();
        }
        // enforce regulations on the name
        if (groupName.length() > 32) {
            return "Group names can be at most 32 characters";
        }
        Charset latin1 = StandardCharsets.ISO_8859_1;
        boolean invalidChars = false;
        if (!latin1.newEncoder().canEncode(groupName)) {
            invalidChars = true;
        }

        for (char c : groupName.toCharArray()) {
            if (Character.isISOControl(c)) {
                invalidChars = true;
            }
        }

        if (invalidChars) {
            return "Group names can't contain that character";
        }

        if (GroupManager.getGroup(groupName) != null) {
            return "That name is already taken. Try another";
        }
        return null;
    }

    private void createGroup(Player p, String groupName) {
        final UUID uuid = p.getUniqueId();
        Group g = new Group(groupName, uuid, false, null, -1, System.currentTimeMillis(), "GRAY");
        gm.createGroupAsync(g, new RunnableOnGroup() {
            @Override
            public void run() {
                Player p = null;
                p = Bukkit.getPlayer(uuid);
                Group g = getGroup();
                if (p != null) {
                    if (g.getGroupId() == -1) { // failure
                        CivUI.error(p, "Couldn't create ", NLText.group(g), ". The name may already be taken");
                    } else {
                        CivUI.notice(Kind.SUCCESS, "Created the group ", NLText.group(g), ". Open it with ", NLText.open(g))
                            .hover(CivText.info("Invite people with /nlip " + g.getName() + " <player>"))
                            .to(p)
                            .send();
                        if (NameLayerPlugin.getInstance().getGroupLimit() == gm.countGroups(p.getUniqueId())) {
                            CivUI.warn(p, "That's your last group - you've reached the limit of ",
                                CivText.value(String.valueOf(NameLayerPlugin.getInstance().getGroupLimit())));
                        }
                    }
                    showScreen();

                } else {
                    NameLayerPlugin.getInstance().getLogger().log(Level.INFO, "Group {0} creation complete resulting in group id: {1}",
                        new Object[]{g.getName(), g.getGroupId()});
                }
            }
        }, false);
    }

    private Clickable getJoinGroupClickable() {
        ItemStack is = new ItemStack(Material.CHEST);
        MenuUtils.name(is, Component.text("Join password protected group", CivText.VALUE));
        Clickable c = new Clickable(is) {

            @Override
            public void clicked(final Player p) {
                CivUI.prompt("Join a group")
                    .body("Groups can let people join with a password")
                    .label("Group")
                    .maxLength(32)
                    .check(groupName -> {
                        final Group g = gm.getGroup(groupName.trim());
                        if (g == null) {
                            return "There's no group with that name";
                        }
                        if (g.isMember(p.getUniqueId())) {
                            return Component.text()
                                .append(Component.text("You're already a member of "))
                                .append(NLText.group(g))
                                .build();
                        }
                        return null;
                    })
                    .onSubmit((who, groupName) -> askForPassword(who, gm.getGroup(groupName.trim())))
                    .onCancel(who -> showScreen())
                    .open(p);
            }
        };
        return c;
    }

    private void askForPassword(Player p, Group g) {
        CivUI.prompt("Join ", NLText.group(g))
            .label("Password")
            .onSubmit((who, password) -> {
                if (g.getPassword() == null || !g.getPassword().equals(password)) {
                    CivUI.error(who, "That password is wrong");
                    showScreen();
                    return;
                }
                Group gro = ensureFreshGroup(g);
                GroupPermission groupPerm = gm.getPermissionforGroup(gro);
                PlayerType pType = groupPerm.getFirstWithPerm(PermissionType.getPermission("JOIN_PASSWORD"));
                if (pType == null) {
                    CivUI.error(who, NLText.group(gro), " doesn't let anyone join with its password");
                    showScreen();
                    return;
                }
                if (NameLayerPlugin.getBlackList().isBlacklisted(gro, who.getUniqueId())) {
                    CivUI.error(who, "You're blacklisted from ", NLText.group(gro));
                    showScreen();
                    return;
                }

                NameLayerPlugin.log(Level.INFO,
                    who.getName() + " joined with password "
                        + " to group " + g.getName()
                        + " via the gui");
                gro.addMember(who.getUniqueId(), pType);
                CivUI.success(who, "Joined the group ", NLText.group(gro));
                showScreen();
            })
            .onCancel(who -> showScreen())
            .open(p);
    }

    private Group ensureFreshGroup(Group g) {
        if (!g.isValid()) {
            return GroupManager.getGroup(g.getName());
        }
        return g;
    }
}
