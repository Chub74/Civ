package vg.civcraft.mc.namelayer.gui;

import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import vg.civcraft.mc.civmodcore.inventory.gui.Clickable;
import vg.civcraft.mc.civmodcore.inventory.gui.ClickableInventory;
import vg.civcraft.mc.civmodcore.inventory.gui.DecorationStack;
import vg.civcraft.mc.civmodcore.inventory.items.ItemUtils;
import vg.civcraft.mc.namelayer.GroupManager.PlayerType;
import vg.civcraft.mc.namelayer.NameLayerAPI;
import vg.civcraft.mc.namelayer.NameLayerPlugin;
import vg.civcraft.mc.namelayer.events.PromotePlayerEvent;
import vg.civcraft.mc.namelayer.group.BlackList;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.listeners.PlayerListener;
import vg.civcraft.mc.namelayer.permission.PermissionType;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.civmodcore.ui.CivText;
import java.util.ArrayList;
import java.util.List;
import vg.civcraft.mc.civmodcore.ui.CivMenu;
import vg.civcraft.mc.namelayer.misc.NLText;

public class MainGroupGUI extends AbstractGroupGUI {

    private boolean showInheritedMembers;
    private boolean showBlacklist;
    private boolean showInvites;
    private boolean showMembers;
    private boolean showMods;
    private boolean showAdmins;
    private boolean showOwners;

    private int currentPage;
    private boolean[] savedToggleState;

    public MainGroupGUI(Player p, Group g) {
        super(g, p);
        showBlacklist = false;
        showInvites = false;
        showInheritedMembers = false;
        showMembers = gm.hasAccess(g, p.getUniqueId(),
            PermissionType.getPermission("MEMBERS"))
            || gm.hasAccess(g, p.getUniqueId(),
            PermissionType.getPermission("GROUPSTATS"));
        showMods = gm.hasAccess(g, p.getUniqueId(),
            PermissionType.getPermission("MODS"))
            || gm.hasAccess(g, p.getUniqueId(),
            PermissionType.getPermission("GROUPSTATS"));
        showAdmins = gm.hasAccess(g, p.getUniqueId(),
            PermissionType.getPermission("ADMINS"))
            || gm.hasAccess(g, p.getUniqueId(),
            PermissionType.getPermission("GROUPSTATS"));
        showOwners = gm.hasAccess(g, p.getUniqueId(),
            PermissionType.getPermission("OWNER"))
            || gm.hasAccess(g, p.getUniqueId(),
            PermissionType.getPermission("GROUPSTATS"));
        currentPage = 0;
        showScreen();
    }

    /**
     * Shows the main gui overview for a specific group based on the properties
     * of this class
     */
    public void showScreen() {
        if (!validGroup()) {
            return;
        }
        ClickableInventory ci = new ClickableInventory(54, MenuUtils.title(g.getName()));
        final List<Clickable> clicks = constructClickables();
        if (clicks.size() < 36 * currentPage) {
            // would show an empty page, so go to previous
            currentPage--;
            showScreen();
        }
        // fill gui
        for (int i = 36 * currentPage; i < 36 * (currentPage + 1)
            && i < clicks.size(); i++) {
            ci.setSlot(clicks.get(i), 9 + i - (36 * currentPage));
        }
        // back button
        if (currentPage > 0) {
            ItemStack back = CivMenu.previousPage();
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
        } else {
            // back to overview button
            ci.setSlot(getSuperMenuClickable(), 45);
        }
        // next button
        if ((36 * (currentPage + 1)) < clicks.size()) {
            ItemStack forward = CivMenu.nextPage();
            Clickable forCl = new Clickable(forward) {

                @Override
                public void clicked(Player arg0) {
                    if ((36 * (currentPage + 1)) < clicks.size()) {
                        currentPage++;
                    }
                    showScreen();
                }
            };
            ci.setSlot(forCl, 53);
        }

        // options

        ci.setSlot(createInheritedMemberToggle(), 46);
        ci.setSlot(createInviteToggle(), 47);
        ci.setSlot(setupMemberTypeToggle(PlayerType.MEMBERS, showMembers), 48);

        ci.setSlot(setupMemberTypeToggle(PlayerType.MODS, showMods), 50);

        ci.setSlot(setupMemberTypeToggle(PlayerType.ADMINS, showAdmins), 51);

        ci.setSlot(setupMemberTypeToggle(PlayerType.OWNER, showOwners), 52);

        // exit button
        ItemStack backToOverview = CivMenu.close();
        ci.setSlot(new Clickable(backToOverview) {

            @Override
            public void clicked(Player arg0) {
                ClickableInventory.forceCloseInventory(arg0);
            }
        }, 49);

        // options at the top
        ci.setSlot(getInvitePlayerClickable(), 0);
        ci.setSlot(createBlacklistToggle(), 1);
        ci.setSlot(getAddBlackListClickable(), 2);
        ci.setSlot(getLeaveGroupClickable(), 3);
        ci.setSlot(getInfoStack(), 4);
        ci.setSlot(getDefaultGroupStack(), 5);
        ci.setSlot(getPasswordClickable(), 6);
        ci.setSlot(getPermOptionClickable(), 7);
        ci.setSlot(getAdminStuffClickable(), 8);
        ci.showInventory(p);
    }

    /**
     * Creates a list of all Clickables representing members, invitees and
     * blacklisted players, if they are supposed to be displayed. This is whats
     * directly fed into the middle of the gui
     */
    private List<Clickable> constructClickables() {
        List<Clickable> clicks = new ArrayList<>();
        if (showInheritedMembers) {
            if (g.hasSuperGroup()) {
                clicks.addAll(getRecursiveInheritedMembers(g.getSuperGroup()));
            }
        }
        if (showBlacklist) {
            final BlackList black = NameLayerPlugin.getBlackList();
            for (final UUID uuid : black.getBlacklist(g)) {
                ItemStack is = new ItemStack(Material.LEATHER_CHESTPLATE);
                LeatherArmorMeta meta = (LeatherArmorMeta) is.getItemMeta();
                meta.setColor(Color.BLACK);
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                is.setItemMeta(meta);
                MenuUtils.name(is, Component.text(String.valueOf(NameLayerAPI.getCurrentName(uuid)), CivText.VALUE));
                Clickable c;
                if (gm.hasAccess(g, p.getUniqueId(),
                    PermissionType.getPermission("BLACKLIST"))) {
                    MenuUtils.lore(is, Component.text("Click to remove " + NameLayerAPI.getCurrentName(uuid), CivText.MUTED), Component.text("from the blacklist", CivText.INFO));
                    c = new Clickable(is) {

                        @Override
                        public void clicked(Player arg0) {
                            if (gm.hasAccess(g, p.getUniqueId(),
                                PermissionType.getPermission("BLACKLIST"))) {
                                NameLayerPlugin.log(
                                    Level.INFO,
                                    arg0.getName() + " removed "
                                        + NameLayerAPI.getCurrentName(uuid)
                                        + " from the blacklist of "
                                        + g.getName() + " via the gui");
                                black.removeBlacklistMember(g, uuid, true);
                                CivUI.success(p, "Unblacklisted ", NLText.player(uuid), " on ", NLText.group(g));
                            } else {
                                CivUI.error(p, "You no longer have permission to unblacklist on ", NLText.group(g));
                            }
                            showScreen();
                        }
                    };
                } else {
                    MenuUtils.lore(is, Component.text("You don't have permission to remove", CivText.ERROR), Component.text("" + NameLayerAPI.getCurrentName(uuid) + "from the blacklist", CivText.ERROR));
                    c = new DecorationStack(is);
                }
                clicks.add(c);
            }

        }
        if (showInvites) {
            Map<UUID, PlayerType> invites = NameLayerPlugin
                .getGroupManagerDao().getInvitesForGroup(g.getName());
            for (Entry<UUID, PlayerType> entry : invites.entrySet()) {
                ItemStack is = new ItemStack(Material.CHAINMAIL_CHESTPLATE);
                ItemMeta im = is.getItemMeta();
                im.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                is.setItemMeta(im);
                final String playerName = NameLayerAPI
                    .getCurrentName(entry.getKey());
                MenuUtils.name(is, Component.text(String.valueOf(playerName), CivText.VALUE));
                boolean canRevoke = false;
                switch (entry.getValue()) {
                    case MEMBERS:
                        MenuUtils.lore(is, Component.text().append(CivText.info("Invited as ")).append(CivText.value("Member")).build());
                        if (gm.hasAccess(g, p.getUniqueId(),
                            PermissionType.getPermission("MEMBERS"))) {
                            canRevoke = true;
                        }
                        break;
                    case MODS:
                        MenuUtils.lore(is, Component.text().append(CivText.info("Invited as ")).append(CivText.value("Mod")).build());
                        if (gm.hasAccess(g, p.getUniqueId(),
                            PermissionType.getPermission("MODS"))) {
                            canRevoke = true;
                        }
                        break;
                    case ADMINS:
                        MenuUtils.lore(is, Component.text().append(CivText.info("Invited as ")).append(CivText.value("Admin")).build());
                        if (gm.hasAccess(g, p.getUniqueId(),
                            PermissionType.getPermission("ADMINS"))) {
                            canRevoke = true;
                        }
                        break;
                    case OWNER:
                        MenuUtils.lore(is, Component.text().append(CivText.info("Invited as ")).append(CivText.value("Owner")).build());
                        if (gm.hasAccess(g, p.getUniqueId(),
                            PermissionType.getPermission("OWNER"))) {
                            canRevoke = true;
                        }
                        break;
                    default:
                        continue;
                }
                Clickable c = null;
                if (canRevoke) {
                    MenuUtils.lore(is, MenuUtils.hint("Click to revoke this invite"));
                    c = new Clickable(is) {

                        @Override
                        public void clicked(Player arg0) {
                            UUID invitedUUID = NameLayerAPI.getUUID(playerName);
                            PlayerType pType = g.getInvite(invitedUUID);
                            if (pType == null) {
                                CivUI.error(p, CivText.subject(playerName), " isn't invited any more");
                                showScreen();
                            }
                            // make sure the player still has permission to do
                            // this
                            boolean allowed = false;
                            switch (pType) {
                                case MEMBERS:
                                    allowed = gm.hasAccess(g, p.getUniqueId(),
                                        PermissionType.getPermission("MEMBERS"));
                                    break;
                                case MODS:
                                    allowed = gm.hasAccess(g, p.getUniqueId(),
                                        PermissionType.getPermission("MODS"));
                                    break;
                                case ADMINS:
                                    allowed = gm.hasAccess(g, p.getUniqueId(),
                                        PermissionType.getPermission("ADMINS"));
                                    break;
                                case OWNER:
                                    allowed = gm.hasAccess(g, p.getUniqueId(),
                                        PermissionType.getPermission("OWNER"));
                                    break;
                                default:
                                    allowed = false;
                                    break;
                            }
                            if (!allowed) {
                                CivUI.error(p, "You don't have permission to revoke that invite");
                            } else {
                                NameLayerPlugin.log(Level.INFO, arg0.getName()
                                    + " revoked an invite for " + NameLayerAPI.getCurrentName(invitedUUID)
                                    + " for group " + g.getName() + " via the gui");
                                g.removeInvite(invitedUUID, true);
                                PlayerListener.removeNotification(invitedUUID, g);

                                CivUI.success(p, "Revoked ", CivText.subject(playerName), "'s invite to the group ", NLText.group(g));
                            }
                            showScreen();
                        }
                    };
                } else {
                    MenuUtils.lore(is, Component.text("You don't have permission to revoke this invite", CivText.ERROR));
                    c = new DecorationStack(is);
                }
                if (c != null) {
                    clicks.add(c);
                }
            }
        }
        List<UUID> allMembers = g.getAllMembers();
        allMembers.sort(Comparator.comparing(g::isOwner).thenComparing(g::getPlayerType).reversed()
            .thenComparing(NameLayerAPI::getCurrentName, String.CASE_INSENSITIVE_ORDER));
        for (UUID uuid : allMembers) {
            Clickable c = null;
            switch (g.getPlayerType(uuid)) {
                case MEMBERS:
                    if (showMembers) {
                        c = constructMemberClickable(Material.LEATHER_CHESTPLATE,
                            uuid, PlayerType.MEMBERS);
                    }
                    break;
                case MODS:
                    if (showMods) {
                        c = constructMemberClickable(modMat(),
                            uuid, PlayerType.MODS);
                    }
                    break;
                case ADMINS:
                    if (showAdmins) {
                        c = constructMemberClickable(Material.IRON_CHESTPLATE,
                            uuid, PlayerType.ADMINS);
                    }
                    break;
                case OWNER:
                    if (showOwners) {
                        c = constructMemberClickable(Material.DIAMOND_CHESTPLATE,
                            uuid, PlayerType.OWNER);
                    }
                    break;
                default:
                    // should never happen
            }
            if (c != null) {
                clicks.add(c);
            }
        }

        return clicks;
    }

    /**
     * Convenience method to create the toggles for the displaying of different
     * group member types
     */
    private Clickable setupMemberTypeToggle(final PlayerType pType,
                                            boolean initialState) {
        boolean canEdit = gm.hasAccess(g, p.getUniqueId(),
            getAccordingPermission(pType))
            || gm.hasAccess(g, p.getUniqueId(),
            PermissionType.getPermission("GROUPSTATS"));
        ItemStack is = MenuUtils.toggleButton(initialState, "Show " + PlayerType.getNiceRankName(pType) + "s", canEdit);
        Clickable c;
        if (canEdit) {
            c = new Clickable(is) {

                @Override
                public void clicked(Player arg0) {
                    switch (pType) {
                        case MEMBERS:
                            showMembers = !showMembers;
                            break;
                        case MODS:
                            showMods = !showMods;
                            break;
                        case ADMINS:
                            showAdmins = !showAdmins;
                            break;
                        case OWNER:
                            showOwners = !showOwners;
                            break;
                    }
                    currentPage = 0;
                    showScreen();
                }
            };
        } else {
            c = new DecorationStack(is);
        }

        return c;

    }

    /**
     * Convenience method used when constructing clickables in the middle of the
     * gui, which represent members
     */
    private Clickable constructMemberClickable(Material material,
                                               final UUID toDisplay, PlayerType rank) {
        Clickable c;
        ItemStack is = new ItemStack(material);
        ItemMeta im = is.getItemMeta();
        im.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        is.setItemMeta(im);
        MenuUtils.name(is, Component.text(String.valueOf(NameLayerAPI.getCurrentName(toDisplay)), CivText.VALUE));
        if (g.isOwner(toDisplay)) { // special case for primary owner
            is.editMeta(itemMeta -> itemMeta.setEnchantmentGlintOverride(true));
            MenuUtils.lore(is, Component.text().append(CivText.info("Rank ")).append(CivText.value("Primary Owner")).build());
            MenuUtils.lore(is, Component.text("You don't have permission", CivText.ERROR), Component.text("to modify the rank of this player", CivText.ERROR));
            c = new DecorationStack(is);

        } else {
            MenuUtils.lore(is, Component.text().append(CivText.info("Rank ")).append(CivText.value(getRankName(toDisplay))).build());
            if (gm.hasAccess(g, p.getUniqueId(), getAccordingPermission(rank))) {
                MenuUtils.lore(is, MenuUtils.hint("Click to modify this player's"), Component.text("rank or to remove them", CivText.MUTED));
                c = new Clickable(is) {

                    @Override
                    public void clicked(Player arg0) {
                        showDetail(toDisplay);
                    }
                };
            } else {
                MenuUtils.lore(is, Component.text("You don't have permission", CivText.ERROR), Component.text("to modify the rank of this player", CivText.ERROR));
                c = new DecorationStack(is);
            }
        }
        return c;
    }

    /**
     * Called when the icon representing a member in the middle of the gui is
     * clicked, this opens up a detailed view where you can select what to do
     * (promoting/removing)
     *
     * @param uuid the UUID to show the inventory to
     */
    public void showDetail(final UUID uuid) {
        if (!validGroup()) {
            showScreen();
            return;
        }
        ClickableInventory ci = new ClickableInventory(27, MenuUtils.title(g.getName()));
        String playerName = NameLayerAPI.getCurrentName(uuid);

        ItemStack info = new ItemStack(Material.PAPER);
        MenuUtils.name(info, Component.text(String.valueOf(playerName), CivText.VALUE));
        String rankName = getRankName(uuid);
        MenuUtils.lore(info, Component.text().append(CivText.info("Current rank ")).append(CivText.value(rankName)).build());
        ci.setSlot(new DecorationStack(info), 4);

        Clickable memberClick = setupDetailSlot(Material.LEATHER_CHESTPLATE,
            uuid, PlayerType.MEMBERS);
        ci.setSlot(memberClick, 10);
        Clickable modClick = setupDetailSlot(modMat(), uuid,
            PlayerType.MODS);
        ci.setSlot(modClick, 12);
        Clickable adminClick = setupDetailSlot(Material.IRON_CHESTPLATE, uuid,
            PlayerType.ADMINS);
        ci.setSlot(adminClick, 14);
        Clickable ownerClick = setupDetailSlot(Material.DIAMOND_CHESTPLATE,
            uuid, PlayerType.OWNER);
        ci.setSlot(ownerClick, 16);

        ItemStack backToOverview = CivMenu.back("overview");
        ci.setSlot(new Clickable(backToOverview) {

            @Override
            public void clicked(Player arg0) {
                showScreen();
            }
        }, 22);
        ci.showInventory(p);
    }

    /**
     * Used by the gui that allows selecting an action for a specific member to
     * easily construct the clickables needed
     */
    private Clickable setupDetailSlot(Material slotMaterial,
                                      final UUID toChange, final PlayerType pType) {
        final PlayerType rank = g.getCurrentRank(toChange);
        ItemStack mod = new ItemStack(slotMaterial);
        ItemMeta im = mod.getItemMeta();
        im.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        mod.setItemMeta(im);
        Clickable modClick;
        if (rank == pType) {
            MenuUtils.name(mod, Component.text("Remove this player", CivText.VALUE));
            mod.lore(List.of(Component.text("/nlrm " + g.getName() + " " + NameLayerAPI.getCurrentName(toChange))
                .color(CivText.VALUE).decoration(TextDecoration.ITALIC, false)));
            if (!gm.hasAccess(g, p.getUniqueId(), getAccordingPermission(pType))) {
                MenuUtils.lore(mod, Component.text("You don't have permission to do this", CivText.ERROR));
                modClick = new DecorationStack(mod);
            } else {
                modClick = new Clickable(mod) {

                    @Override
                    public void clicked(Player arg0) {
                        if (gm.hasAccess(g, p.getUniqueId(),
                            getAccordingPermission(g
                                .getCurrentRank(toChange)))) {
                            removeMember(toChange);
                            showScreen();
                        }
                    }
                };
            }
        } else {
            MenuUtils.name(mod, Component.text("" + demoteOrPromote(g.getPlayerType(toChange), pType, true) + " this player to " + PlayerType.getNiceRankName(pType), CivText.VALUE));
            mod.lore(List.of(Component.text("/nlpp " + g.getName() + " " + NameLayerAPI.getCurrentName(toChange) + " " + pType)
                .color(CivText.VALUE).decoration(TextDecoration.ITALIC, false)));
            if (!gm.hasAccess(g, p.getUniqueId(), getAccordingPermission(pType))) {
                MenuUtils.lore(mod, Component.text("You don't have permission to do this", CivText.ERROR));
                modClick = new DecorationStack(mod);
            } else {
                modClick = new Clickable(mod) {

                    @Override
                    public void clicked(Player arg0) {
                        changePlayerRank(toChange, pType);
                        showDetail(toChange);
                    }
                };
            }
        }
        return modClick;
    }

    private void removeMember(UUID toRemove) {
        if (gm.hasAccess(g, p.getUniqueId(),
            getAccordingPermission(g.getCurrentRank(toRemove)))) {
            if (!g.isMember(toRemove)) {
                CivUI.error(p, NLText.player(toRemove), " isn't on ", NLText.group(g), " any more");
                return;
            }
            if (g.isOwner(toRemove)) {
                CivUI.error(p, NLText.player(toRemove), " owns ", NLText.group(g), ", so you can't remove them");
            }
            NameLayerPlugin.log(Level.INFO,
                p.getName() + " kicked " + NameLayerAPI.getCurrentName(toRemove)
                    + " from " + g.getName() + " via the gui");
            g.removeMember(toRemove);
            CivUI.success(p, "Removed ", NLText.player(toRemove), " from ", NLText.group(g));
        } else {
            CivUI.error(p, "You no longer have permission to do that on ", NLText.group(g));
        }
    }

    private void changePlayerRank(UUID toChange, PlayerType newRank) {
        if (gm.hasAccess(g, p.getUniqueId(),
            getAccordingPermission(g.getCurrentRank(toChange)))) {
            if (!g.isMember(toChange)) {
                CivUI.error(p, NLText.player(toChange), " isn't on ", NLText.group(g), " any more");
                return;
            }
            if (g.isOwner(toChange)) {
                CivUI.error(p, NLText.player(toChange), " owns ", NLText.group(g), ", so you can't change their rank");
            }
            OfflinePlayer prom = Bukkit.getOfflinePlayer(toChange);
            NameLayerPlugin.log(
                Level.INFO,
                p.getName() + " changed player rank for "
                    + NameLayerAPI.getCurrentName(toChange) + " from "
                    + g.getCurrentRank(toChange).toString() + " to "
                    + newRank.toString() + " for group " + g.getName()
                    + " via the gui");
            if (prom.isOnline()) {
                Player oProm = (Player) prom;
                PromotePlayerEvent event = new PromotePlayerEvent(oProm, g,
                    g.getCurrentRank(toChange), newRank);
                Bukkit.getPluginManager().callEvent(event);
                if (event.isCancelled()) {
                    CivUI.error(p, "Couldn't change the rank of ", NLText.player(toChange), ". Tell an admin");
                    return;
                }
                g.removeMember(toChange);
                g.addMember(toChange, newRank);
                CivUI.info(oProm, "You're now ", CivText.value(getRankName(toChange)), " on ", NLText.group(g));
            } else {
                // player is offline change their perms
                g.removeMember(toChange);
                g.addMember(toChange, newRank);
            }
            CivUI.success(p, NLText.player(toChange), " is now ", CivText.value(getRankName(toChange)), " on ", NLText.group(g));
        } else {
            CivUI.error(p, "You no longer have permission to do that on ", NLText.group(g));
        }
    }

    private Clickable createBlacklistToggle() {
        ItemStack is = MenuUtils.toggleButton(
            showBlacklist,
            "Show blacklisted players",
            gm.hasAccess(g, p.getUniqueId(),
                PermissionType.getPermission("GROUPSTATS")));
        Clickable c;
        if (gm.hasAccess(g, p.getUniqueId(),
            PermissionType.getPermission("GROUPSTATS"))) {
            c = new Clickable(is) {

                @Override
                public void clicked(Player arg0) {
                    if (!showBlacklist) {
                        // currently showing members, so save state
                        savedToggleState = new boolean[5];
                        savedToggleState[0] = showInvites;
                        savedToggleState[1] = showMembers;
                        savedToggleState[2] = showMods;
                        savedToggleState[3] = showAdmins;
                        savedToggleState[4] = showOwners;
                        showInvites = false;
                        showMembers = false;
                        showMods = false;
                        showAdmins = false;
                        showOwners = false;
                        showBlacklist = true;
                    } else {
                        // load state
                        showInvites = savedToggleState[0];
                        showMembers = savedToggleState[1];
                        showMods = savedToggleState[2];
                        showAdmins = savedToggleState[3];
                        showOwners = savedToggleState[4];
                        showBlacklist = false;
                    }
                    showScreen();
                }
            };
        } else {
            c = new DecorationStack(is);
        }
        return c;
    }

    private Clickable createInheritedMemberToggle() {
        boolean canToggle = gm.hasAccess(g, p.getUniqueId(),
            PermissionType.getPermission("GROUPSTATS"));
        ItemStack is = MenuUtils.toggleButton(showInheritedMembers,
            "Show inherited members", canToggle);
        Clickable c;
        if (canToggle) {
            c = new Clickable(is) {

                @Override
                public void clicked(Player p) {
                    showInheritedMembers = !showInheritedMembers;
                    showScreen();

                }
            };
        } else {
            c = new DecorationStack(is);
        }
        return c;
    }

    private Clickable createInviteToggle() {
        ItemStack is = MenuUtils.toggleButton(showInvites, "Show invited players", true);
        return new Clickable(is) {

            @Override
            public void clicked(Player arg0) {
                showInvites = !showInvites;
                showScreen();
            }
        };
    }

    private Clickable getAddBlackListClickable() {
        Clickable c;
        ItemStack is = blacklistStack();
        MenuUtils.name(is, Component.text("Add player to blacklist", CivText.VALUE));
        if (gm.hasAccess(g, p.getUniqueId(),
            PermissionType.getPermission("BLACKLIST"))) {
            c = new Clickable(is) {

                @Override
                public void clicked(final Player p) {
                    CivUI.prompt("Blacklist on ", NLText.group(g))
                        .body("Separate several names with spaces to blacklist them all")
                        .label("Players")
                        .validate(names -> !names.isBlank(), "Type at least one name")
                        .onSubmit((who, names) -> {
                            if (gm.hasAccess(g, p.getUniqueId(),
                                PermissionType.getPermission("BLACKLIST"))) {
                                for (String playerName : names.trim().split("\\s+")) {
                                    UUID blackUUID = NameLayerAPI
                                        .getUUID(playerName);
                                    if (blackUUID == null) {
                                        CivUI.error(p, "There's no player called ", CivText.subject(playerName));
                                        continue;
                                    }
                                    if (g.isMember(blackUUID)) {
                                        CivUI.error(p, "You can't blacklist ", NLText.player(blackUUID), " - they're a member of ", NLText.group(g));
                                        continue;
                                    }
                                    BlackList bl = NameLayerPlugin
                                        .getBlackList();
                                    if (bl.isBlacklisted(g, blackUUID)) {
                                        CivUI.error(p, NLText.player(blackUUID), " is already blacklisted on ", NLText.group(g));
                                        continue;
                                    }
                                    NameLayerPlugin
                                        .log(Level.INFO,
                                            p.getName()
                                                + " blacklisted "
                                                + NameLayerAPI.getCurrentName(blackUUID)
                                                + " for group "
                                                + g.getName()
                                                + " via the gui");
                                    bl.addBlacklistMember(g, blackUUID, true);
                                    CivUI.success(p, "Blacklisted ", NLText.player(blackUUID), " on ", NLText.group(g));
                                }
                            } else {
                                CivUI.error(p, "You no longer have permission to do that on ", NLText.group(g));
                            }
                            showScreen();
                        })
                        .onCancel(who -> showScreen())
                        .open(p);
                }
            };
        } else {
            MenuUtils.lore(is, Component.text("You don't have permission to do this", CivText.ERROR));
            c = new DecorationStack(is);
        }
        return c;
    }

    private Clickable getPasswordClickable() {
        Clickable c;
        ItemStack is = new ItemStack(Material.OAK_SIGN);
        MenuUtils.name(is, Component.text("Add or change password", CivText.VALUE));
        if (gm.hasAccess(g, p.getUniqueId(),
            PermissionType.getPermission("PASSWORD"))) {
            String pass = g.getPassword();
            if (pass == null) {
                MenuUtils.lore(is, Component.text("This group doesn't have a password currently", CivText.INFO));
            } else {
                MenuUtils.lore(is, Component.text().append(Component.text("The current password is: ", CivText.INFO)).append(Component.text(String.valueOf(pass), CivText.VALUE)).build());
            }
            c = new Clickable(is) {

                @Override
                public void clicked(final Player p) {
                    if (gm.hasAccess(g, p.getUniqueId(),
                        PermissionType.getPermission("PASSWORD"))) {
                        CivUI.prompt("Password of ", NLText.group(g))
                            .body("Leave it empty to remove the password")
                            .label("Password")
                            .initial(g.getPassword())
                            .validate(password -> !password.trim().contains(" "), "Passwords can't contain spaces")
                            .onSubmit((who, input) -> {
                                String newPassword = input.trim();
                                if (newPassword.isEmpty()) {
                                    if (g.getPassword() == null) {
                                        CivUI.info(p, "Left the password unchanged");
                                        showScreen();
                                        return;
                                    }
                                    g.setPassword(null);
                                    CivUI.success(p, "Removed the password of ", NLText.group(g));
                                    NameLayerPlugin.log(Level.INFO, p.getName()
                                        + " removed password "
                                        + " for group " + g.getName()
                                        + " via the gui");
                                } else {
                                    NameLayerPlugin.log(Level.INFO, p.getName()
                                        + " set password to " + newPassword
                                        + " for group " + g.getName()
                                        + " via the gui");
                                    g.setPassword(newPassword);
                                    CivUI.success(p, "Set the password of ", NLText.group(g), " to ", CivText.value(String.valueOf(newPassword)));
                                }
                                showScreen();
                            })
                            .onCancel(who -> showScreen())
                            .open(p);
                    } else {
                        CivUI.error(p, "You no longer have permission to do that on ", NLText.group(g));
                        showScreen();
                    }
                }
            };
        } else {
            MenuUtils.lore(is, Component.text("You don't have permission to do this", CivText.ERROR));
            c = new DecorationStack(is);
        }
        return c;
    }

    private Clickable getPermOptionClickable() {
        ItemStack permStack = permsStack();
        MenuUtils.name(permStack, Component.text("View and manage group permissions", CivText.VALUE));
        Clickable permClickable;
        if (gm.hasAccess(g, p.getUniqueId(),
            PermissionType.getPermission("LIST_PERMS"))) {
            permClickable = new Clickable(permStack) {
                @Override
                public void clicked(Player arg0) {
                    PermissionManageGUI pmgui = new PermissionManageGUI(g, p,
                        MainGroupGUI.this);
                    pmgui.showScreen();
                }
            };
        } else {
            MenuUtils.lore(permStack, Component.text("You don't have permission", CivText.ERROR), Component.text("to do this", CivText.ERROR));
            permClickable = new DecorationStack(permStack);
        }
        return permClickable;
    }

    private Clickable getInvitePlayerClickable() {
        ItemStack inviteStack = new ItemStack(Material.COOKIE);
        MenuUtils.name(inviteStack, Component.text("Invite new member", CivText.VALUE));
        return new Clickable(inviteStack) {

            @Override
            public void clicked(Player arg0) {
                new InvitationGUI(g, p, MainGroupGUI.this);
            }
        };
    }

    private Clickable getDefaultGroupStack() {
        Clickable c;
        ItemStack is = defaultStack();
        MenuUtils.name(is, Component.text("Default group", CivText.VALUE));
        final String defGroup = gm.getDefaultGroup(p.getUniqueId());
        if (defGroup != null && defGroup.equals(g.getName())) {
            MenuUtils.lore(is, Component.text("This group is your current default group", CivText.INFO));
            c = new DecorationStack(is);
        } else {
            MenuUtils.lore(is, MenuUtils.hint("Click to make this group your default group"));
            if (defGroup != null) {
                MenuUtils.lore(is, Component.text().append(CivText.info("Your default group is ")).append(CivText.group(defGroup)).build());
            }
            c = new Clickable(is) {

                @Override
                public void clicked(Player p) {
                    NameLayerPlugin.log(Level.INFO, p.getName()
                        + " set default group to " + g.getName()
                        + " via the gui");
                    if (defGroup == null) {
                        g.setDefaultGroup(p.getUniqueId());
                        CivUI.success(p, "Your default group is now ", NLText.group(g));
                    } else {
                        g.changeDefaultGroup(p.getUniqueId());
                        CivUI.success(p, "Changed your default group from ", NLText.group(defGroup), " to ", NLText.group(g));
                    }
                    showScreen();
                }
            };
        }
        return c;
    }

    private Clickable getSuperMenuClickable() {
        ItemStack is = new ItemStack(Material.DIAMOND);
        MenuUtils.name(is, Component.text("Return to overview for all your groups", CivText.VALUE));
        return new Clickable(is) {

            @Override
            public void clicked(Player p) {
                GUIGroupOverview gui = new GUIGroupOverview(p);
                gui.showScreen();
            }
        };
    }

    private Clickable getAdminStuffClickable() {
        ItemStack is = new ItemStack(Material.DIAMOND);
        MenuUtils.name(is, Component.text("Admin/Owner functions", CivText.VALUE));
        return new Clickable(is) {

            @Override
            public void clicked(Player p) {
                AdminFunctionsGUI subGui = new AdminFunctionsGUI(p, g,
                    MainGroupGUI.this);
                subGui.showScreen();
            }
        };
    }

    /**
     * Constructs the icon used in the gui for leaving a group
     */
    private Clickable getLeaveGroupClickable() {
        Clickable c;
        ItemStack is = new ItemStack(Material.IRON_DOOR);
        MenuUtils.name(is, Component.text("Leave group", CivText.VALUE));
        if (g.isOwner(p.getUniqueId())) {
            MenuUtils.lore(is, Component.text("You can't leave this group,", CivText.ERROR), Component.text("because you own it", CivText.ERROR));
            c = new DecorationStack(is);
        } else {
            c = new Clickable(is) {

                @Override
                public void clicked(Player p) {
                    CivUI.confirm("Leave ", NLText.group(g))
                        .warning("You can't undo this")
                        .yesLabel("Leave")
                        .noLabel("Stay")
                        .onYes(who -> {
                            if (!g.isMember(who.getUniqueId())) {
                                CivUI.error(who, "You're not a member of ", NLText.group(g));
                                showScreen();
                                return;
                            }
                            if (g.isDisciplined()) {
                                CivUI.error(who, NLText.group(g), " is disciplined");
                                showScreen();
                                return;
                            }
                            NameLayerPlugin.log(Level.INFO, who.getName()
                                + " left " + g.getName() + " via the gui");
                            g.removeMember(who.getUniqueId());
                            CivUI.success(who, "Left the group ", NLText.group(g));
                        })
                        .onNo(who -> showScreen())
                        .open(p);
                }
            };
        }
        return c;
    }

    private Clickable getInfoStack() {
        Clickable c;
        ItemStack is = new ItemStack(Material.PAPER);
        MenuUtils.name(is, Component.text("Stats for " + g.getName(), CivText.VALUE));
        MenuUtils.lore(is, Component.text().append(Component.text("Your current rank: ", CivText.INFO)).append(Component.text(String.valueOf(PlayerType.getNiceRankName(g.getPlayerType(p.getUniqueId()))), CivText.VALUE)).build());
        boolean hasGroupStatsPerm = gm.hasAccess(g, p.getUniqueId(),
            PermissionType.getPermission("GROUPSTATS"));
        if (gm.hasAccess(g, p.getUniqueId(),
            PermissionType.getPermission("MEMBERS"))
            || hasGroupStatsPerm) {
            MenuUtils.lore(is, Component.text(String.valueOf(g .getAllMembers(PlayerType.MEMBERS).size()) + " members", CivText.INFO));
        }
        if (gm.hasAccess(g, p.getUniqueId(),
            PermissionType.getPermission("MODS"))
            || hasGroupStatsPerm) {
            MenuUtils.lore(is, Component.text(String.valueOf(g.getAllMembers(PlayerType.MODS) .size()) + " mods", CivText.INFO));
        }
        if (gm.hasAccess(g, p.getUniqueId(),
            PermissionType.getPermission("ADMINS"))
            || hasGroupStatsPerm) {
            MenuUtils.lore(is, Component.text(String.valueOf(g.getAllMembers(PlayerType.ADMINS) .size()) + " admins", CivText.INFO));
        }
        if (gm.hasAccess(g, p.getUniqueId(),
            PermissionType.getPermission("OWNER"))
            || hasGroupStatsPerm) {
            MenuUtils.lore(is, Component.text(String.valueOf(g.getAllMembers(PlayerType.OWNER) .size()) + " owner", CivText.INFO));
        }
        if (hasGroupStatsPerm) {
            MenuUtils.lore(is, Component.text(String.valueOf(g.getAllMembers().size()) + " total group members", CivText.INFO));
            MenuUtils.lore(is, Component.text().append(Component.text("Group owner: ", CivText.INFO)).append(Component.text(String.valueOf(NameLayerAPI.getCurrentName(g.getOwner())), CivText.VALUE)).build());
        }
        c = new DecorationStack(is);
        return c;
    }

    /**
     * Utility to get a "nice" version of the rank the given player has in the
     * group this gui is based on
     *
     * @param uuid Player whose rank name should be checked
     * @return Rankname of the given player ready to be put into the gui or null
     * if the given UUID was null or the player didn't have an explicit
     * rank in the group
     */
    private String getRankName(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        PlayerType pType = g.getPlayerType(uuid);
        if (pType == null) {
            return null;
        }
        return PlayerType.getNiceRankName(pType);
    }

    /**
     * Gets the permission needed to make changes to the given PlayerType
     *
     * @param pt PlayerType to get permission for
     * @return Permission belonging to the given PlayerType or null if the
     * permission was NOT_BLACKLISTED or null
     */
    public static PermissionType getAccordingPermission(PlayerType pt) {
        switch (pt) {
            case MEMBERS:
                return PermissionType.getPermission("MEMBERS");
            case MODS:
                return PermissionType.getPermission("MODS");
            case ADMINS:
                return PermissionType.getPermission("ADMINS");
            case OWNER:
                return PermissionType.getPermission("OWNER");
        }
        return null;
    }

    /**
     * Utility to determine whether the player is being promoted or demoted
     */
    private static String demoteOrPromote(PlayerType oldRank,
                                          PlayerType newRank, boolean upperCaseFirstLetter) {
        String res = PlayerType.getID(oldRank) <= PlayerType.getID(newRank) ? "promote"
            : "demote";
        if (upperCaseFirstLetter) {
            return res.substring(0, 1).toUpperCase() + res.substring(1);
        }
        return res;
    }

    private List<Clickable> getRecursiveInheritedMembers(Group g) {
        List<Clickable> clicks = new ArrayList<>();
        if (g.hasSuperGroup()) {
            clicks.addAll(getRecursiveInheritedMembers(g.getSuperGroup()));
        }
        for (UUID uuid : g.getAllMembers()) {
            ItemStack is;
            switch (g.getPlayerType(uuid)) {
                case MEMBERS:
                    is = new ItemStack(Material.LEATHER_CHESTPLATE);
                    break;
                case MODS:
                    is = modStack();
                    break;
                case ADMINS:
                    is = new ItemStack(Material.IRON_CHESTPLATE);
                    break;
                case OWNER:
                    is = new ItemStack(Material.DIAMOND_CHESTPLATE);
                    break;
                default:
                    continue;
            }
            MenuUtils.name(is, Component.text(String.valueOf(NameLayerAPI.getCurrentName(uuid)), CivText.VALUE));
            MenuUtils.lore(is, Component.text("Inherited " + getRankName(uuid) + " from " + g.getName(), CivText.INFO));
            clicks.add(new DecorationStack(is));
        }
        return clicks;
    }

    private boolean hasPermissionToViewAnything() {
        return gm.hasAccess(g, p.getUniqueId(),
            PermissionType.getPermission("MEMBERS"))
            || gm.hasAccess(g, p.getUniqueId(),
            PermissionType.getPermission("MODS"))
            || gm.hasAccess(g, p.getUniqueId(),
            PermissionType.getPermission("ADMINS"))
            || gm.hasAccess(g, p.getUniqueId(),
            PermissionType.getPermission("OWNER"))
            || gm.hasAccess(g, p.getUniqueId(),
            PermissionType.getPermission("GROUPSTATS"));
    }
}
