package vg.civcraft.mc.namelayer.gui;

import java.util.List;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import vg.civcraft.mc.civmodcore.inventory.gui.Clickable;
import vg.civcraft.mc.civmodcore.inventory.gui.ClickableInventory;
import vg.civcraft.mc.civmodcore.inventory.items.ItemUtils;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.namelayer.GroupManager;
import vg.civcraft.mc.namelayer.NameLayerPlugin;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.misc.NLText;
import vg.civcraft.mc.namelayer.permission.PermissionType;

public class MergeGUI extends AbstractGroupGUI {

    private AdminFunctionsGUI parent;
    private boolean mergeIntoThisGroup;
    private int currentPage;

    public MergeGUI(Group g, Player p, AdminFunctionsGUI parent) {
        super(g, p);
        this.parent = parent;
        this.currentPage = 0;
    }

    public void showScreen() {
        ClickableInventory ci = new ClickableInventory(27, MenuUtils.title(g.getName()));
        ItemStack mergeThisIntoOtherStack = new ItemStack(Material.MINECART);
        MenuUtils.name(mergeThisIntoOtherStack, Component.text("Merge this group into a different one", CivText.VALUE));
        MenuUtils.lore(mergeThisIntoOtherStack, Component.text("This action will transfer all members, reinforcements, snitches of this group to the one you chose next. " + "This group will be deleted in the process", CivText.INFO));
        ItemStack mergeOtherIntoThisStack = mergeStack();
        MenuUtils.name(mergeOtherIntoThisStack, Component.text("Merge a different group into this one", CivText.VALUE));
        MenuUtils.lore(mergeOtherIntoThisStack, Component.text("This action will transfer all members, reinforcements and snitches of the group you chose next to this group. " + "The group chosen will be deleted in the process", CivText.INFO));
        ci.setSlot(new Clickable(mergeOtherIntoThisStack) {

            @Override
            public void clicked(Player arg0) {
                mergeIntoThisGroup = true;
                showMergeGroupSelector();
            }
        }, 11);
        ci.setSlot(new Clickable(mergeThisIntoOtherStack) {

            @Override
            public void clicked(Player arg0) {
                mergeIntoThisGroup = false;
                showMergeGroupSelector();
            }
        }, 15);
        // exit button
        ItemStack backToOverview = goBackStack();
        MenuUtils.name(backToOverview, Component.text("Go back to previous menu", CivText.VALUE));
        ci.setSlot(new Clickable(backToOverview) {

            @Override
            public void clicked(Player arg0) {
                parent.showScreen();
            }
        }, 22);
        ci.showInventory(p);
    }

    private void showMergeGroupSelector() {
        ClickableInventory ci = new ClickableInventory(54, MenuUtils.title(g.getName()));
        final List<String> gName = gm.getAllGroupNames(p.getUniqueId());
        if (gName.size() < 45 * currentPage) {
            // would show an empty page, so go to previous
            currentPage--;
            showScreen();
        }
        for (int i = 45 * currentPage; i < 45 * (currentPage + 1) && i < gName.size(); i++) {
            final String currentName = gName.get(i);
            if (!gm.hasAccess(currentName, p.getUniqueId(), PermissionType.getPermission("MERGE"))) {
                // dont show groups player cant merge
                continue;
            }
            if (currentName.equals(g.getName())) {
                // cant merge with itself
                continue;
            }
            ItemStack is = new ItemStack(Material.MAGMA_CREAM);
            MenuUtils.name(is, Component.text(String.valueOf(currentName), CivText.VALUE));
            ci.setSlot(new Clickable(is) {

                @Override
                public void clicked(Player arg0) {
                    requestConfirmation(currentName);
                }
            }, 9 + i - (45 * currentPage));
        }
        // back button
        if (currentPage > 0) {
            ItemStack back = new ItemStack(Material.ARROW);
            MenuUtils.name(back, Component.text("Go to previous page", CivText.VALUE));
            Clickable baCl = new Clickable(back) {

                @Override
                public void clicked(Player arg0) {
                    if (currentPage > 0) {
                        currentPage--;
                    }
                    showMergeGroupSelector();
                }
            };
            ci.setSlot(baCl, 45);
        }
        // next button
        if ((45 * (currentPage + 1)) <= gName.size()) {
            ItemStack forward = new ItemStack(Material.ARROW);
            MenuUtils.name(forward, Component.text("Go to next page", CivText.VALUE));
            Clickable forCl = new Clickable(forward) {

                @Override
                public void clicked(Player arg0) {
                    if ((45 * (currentPage + 1)) <= gName.size()) {
                        currentPage++;
                    }
                    showMergeGroupSelector();
                }
            };
            ci.setSlot(forCl, 53);
        }

        // exit button
        ItemStack backToOverview = goBackStack();
        MenuUtils.name(backToOverview, Component.text("Exit selection", CivText.VALUE));
        ci.setSlot(new Clickable(backToOverview) {

            @Override
            public void clicked(Player arg0) {
                parent.showScreen();
            }
        }, 49);
        ci.showInventory(p);
    }

    private void requestConfirmation(final String groupName) {
        String fromGroup = mergeIntoThisGroup ? groupName : g.getName();
        String targetGroup = mergeIntoThisGroup ? g.getName() : groupName;
        CivUI.confirm("Merge ", NLText.group(fromGroup), " into ", NLText.group(targetGroup))
            .body("This will transfer all members, reinforcements, snitches etc. from ", NLText.group(fromGroup),
                " to ", NLText.group(targetGroup), ". ", NLText.group(fromGroup), " will be deleted in the process")
            .warning("You can't undo this")
            .yesLabel("Merge")
            .noLabel("Don't merge")
            .onYes(who -> requestMerge(groupName))
            .onNo(who -> showScreen())
            .open(p);
    }

    private void requestMerge(String mergeGroupName) {
        final Group otherGroup = GroupManager.getGroup(mergeGroupName);
        if (otherGroup == null) {
            CivUI.error(p, "Couldn't start the merge. Try again");
            showScreen();
            return;
        }
        if (!gm.hasAccess(otherGroup, p.getUniqueId(), PermissionType.getPermission("MERGE"))) {
            CivUI.error(p, "You don't have permission to merge ", NLText.group(otherGroup));
            showScreen();
            return;
        }
        if (!gm.hasAccess(g, p.getUniqueId(), PermissionType.getPermission("MERGE"))) {
            CivUI.error(p, "You don't have permission to merge ", NLText.group(g));
            showScreen();
            return;
        }
        if (g.isDisciplined() || otherGroup.isDisciplined()) {
            CivUI.error(p, "You can't merge a disciplined group");
            showScreen();
            return;
        }
        if (mergeGroupName.equals(g.getName())) {
            CivUI.error(p, "You can't merge a group into itself");
            showScreen();
            return;
        }
        NameLayerPlugin.log(Level.INFO, p.getName() + " merged " + g.getName() + " and " + mergeGroupName + "via gui, "
            + (mergeIntoThisGroup ? g.getName() : mergeGroupName) + " was the group merged into");
        try {
            if (mergeIntoThisGroup) {
                gm.mergeGroup(g, otherGroup);
                CivUI.success(p, "Merged the group ", NLText.group(otherGroup), " into ", NLText.group(g));
                parent.showScreen();
            } else {
                gm.mergeGroup(otherGroup, g);
                CivUI.success(p, "Merged the group ", NLText.group(g), " into ", NLText.group(otherGroup));
            }
        } catch (Exception e) {
            NameLayerPlugin.getInstance().getLogger().log(Level.SEVERE, "Group merging failed", e);
            CivUI.error(p, "The merge may have failed. Ask an admin to check");
        }
    }

}
