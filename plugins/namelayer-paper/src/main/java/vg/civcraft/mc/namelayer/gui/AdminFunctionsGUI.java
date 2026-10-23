package vg.civcraft.mc.namelayer.gui;

import java.util.Locale;
import java.util.UUID;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import vg.civcraft.mc.civmodcore.inventory.gui.Clickable;
import vg.civcraft.mc.civmodcore.inventory.gui.ClickableInventory;
import vg.civcraft.mc.civmodcore.inventory.gui.DecorationStack;
import vg.civcraft.mc.civmodcore.inventory.gui.LClickable;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.namelayer.NameLayerAPI;
import vg.civcraft.mc.namelayer.NameLayerPlugin;
import vg.civcraft.mc.namelayer.command.commands.TransferGroup;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.misc.NLText;
import vg.civcraft.mc.civmodcore.ui.CivMenu;
import vg.civcraft.mc.namelayer.permission.PermissionType;

public class AdminFunctionsGUI extends AbstractGroupGUI {

    private MainGroupGUI parent;

    public AdminFunctionsGUI(Player p, Group g, MainGroupGUI parent) {
        super(g, p);
        this.parent = parent;
    }

    public void showScreen() {
        ClickableInventory ci = new ClickableInventory(27, MenuUtils.title(g.getName()));
        // linking
        ItemStack linkStack = new ItemStack(Material.GOLD_INGOT);
        MenuUtils.name(linkStack, Component.text("Link group", CivText.VALUE));
        Clickable linkClick;

        MenuUtils.lore(linkStack, Component.text("Group linking isn't supported yet", CivText.ERROR));
        linkClick = new DecorationStack(linkStack);

//        ci.setSlot(linkClick, 10);
        // merging
        ItemStack mergeStack = new ItemStack(Material.SPONGE);
        MenuUtils.name(mergeStack, Component.text("Merge group", CivText.VALUE));
        Clickable mergeClick;

        MenuUtils.lore(mergeStack, Component.text("Group merging isn't supported yet", CivText.ERROR));
        mergeClick = new DecorationStack(mergeStack);

//		if (gm.hasAccess(g, p.getUniqueId(),
//				PermissionType.getPermission("MERGE"))) {
//			mergeClick = new Clickable(mergeStack) {
//				@Override
//				public void clicked(Player p) {
//					showMergingMenu();
//				}
//			};
//		} else {
//			MenuUtils.lore(mergeStack, Component.text("You don't have permission to do this", CivText.ERROR));
//			mergeClick = new DecorationStack(mergeStack);
//		}
//        ci.setSlot(mergeClick, 12);
        ItemStack colorChangeStack = new ItemStack(Material.WHITE_DYE);
        MenuUtils.name(colorChangeStack, Component.text("Change the colour of ", CivText.VALUE).append(g.getGroupNameColored()));
        Clickable colorChangeClick;
        if (gm.hasAccess(g, p.getUniqueId(), PermissionType.getPermission("EDIT_COLOR"))) {
            colorChangeClick = getGroupColorChangeButton();
        } else {
            MenuUtils.lore(colorChangeStack, Component.text("You don't have permission to change the colour", CivText.ERROR));
            colorChangeClick = new DecorationStack(colorChangeStack);
        }
        ci.setSlot(colorChangeClick, 11);
        // transferring group
        ItemStack transferStack = new ItemStack(Material.PACKED_ICE);
        MenuUtils.name(transferStack, Component.text("Transfer group to new primary owner", CivText.VALUE));
        Clickable transferClick;
        if (g.isOwner(p.getUniqueId())) {
            transferClick = new Clickable(transferStack) {
                @Override
                public void clicked(Player p) {
                    showTransferingMenu();
                }
            };
        } else {
            MenuUtils.lore(transferStack, Component.text("You don't have permission to do this", CivText.ERROR));
            transferClick = new DecorationStack(transferStack);
        }
        ci.setSlot(transferClick, 13);
        // deleting group
        ItemStack deletionStack = new ItemStack(Material.BARRIER);
        MenuUtils.name(deletionStack, Component.text("Delete group", CivText.VALUE));
        Clickable deletionClick;
        if (gm.hasAccess(g, p.getUniqueId(),
            PermissionType.getPermission("DELETE"))) {
            deletionClick = new Clickable(deletionStack) {
                @Override
                public void clicked(Player p) {
                    showDeletionMenu();
                }
            };
        } else {
            MenuUtils.lore(deletionStack, Component.text("You don't have permission to do this", CivText.ERROR));
            deletionClick = new DecorationStack(deletionStack);
        }
        ci.setSlot(deletionClick, 15);

        // back button
        ItemStack backToOverview = CivMenu.back("overview");
        ci.setSlot(new Clickable(backToOverview) {

            @Override
            public void clicked(Player arg0) {
                parent.showScreen();
            }
        }, 22);
        ci.showInventory(p);
    }

    private void showMergingMenu() {
        MergeGUI mGui = new MergeGUI(g, p, this);
        mGui.showScreen();
    }

    private void showTransferingMenu() {
        CivUI.prompt("Transfer ", NLText.group(g))
            .body("Who should become the new primary owner?")
            .label("Player")
            .maxLength(16)
            .validate(name -> NameLayerAPI.getUUID(name.trim()) != null, "There's no player with that name")
            .onSubmit((p, name) -> confirmTransfer(p, NameLayerAPI.getUUID(name.trim())))
            .onCancel(p -> showScreen())
            .open(p);
    }

    private void confirmTransfer(Player p, UUID transferUUID) {
        CivUI.confirm("Transfer ", NLText.group(g), " to ", NLText.player(transferUUID))
            .warning("You can't undo this")
            .yesLabel("Transfer")
            .noLabel("Don't transfer")
            .onYes(who -> {
                if (TransferGroup.attemptTransfer(g, who, transferUUID)) {
                    NameLayerPlugin.log(
                        Level.INFO,
                        who.getName()
                            + " transferred group to "
                            + NameLayerAPI.getCurrentName(transferUUID)
                            + " for group " + g.getName()
                            + " via the gui");
                } else {
                    showScreen();
                }
            })
            .onNo(who -> showScreen())
            .open(p);
    }

    private void showDeletionMenu() {
        CivUI.confirm("Delete ", NLText.group(g))
            .warning("You can't undo this")
            .yesLabel("Delete")
            .noLabel("Keep it")
            .onYes(p -> {
                if (!gm.hasAccess(g, p.getUniqueId(),
                    PermissionType.getPermission("DELETE"))) {
                    CivUI.error(p, "You don't have permission to delete ", NLText.group(g));
                    showScreen();
                    return;
                }
                if (g.isDisciplined()) {
                    CivUI.error(p, NLText.group(g), " is disciplined");
                    showScreen();
                    return;
                }
                NameLayerPlugin.log(Level.INFO,
                    p.getName() + " deleted " + g.getName() + " via the gui");
                if (gm.deleteGroup(g.getName())) {
                    CivUI.success(p, "Deleted the group ", NLText.group(g));
                } else {
                    CivUI.warn(p, NLText.group(g), " is disciplined while it's deleted. Check back later");
                }
            })
            .onNo(p -> showScreen())
            .open(p);
    }

    private LClickable getGroupColorChangeButton() {
        return new LClickable(Material.WHITE_DYE,
            Component.text("Change the colour of ", CivText.VALUE).append(g.getGroupNameColored()), p ->
            CivUI.prompt("Colour of ", NLText.group(g))
                .body("A colour name like ", CivText.value("gold"), " or a hex value like ", CivText.value("#ff8800"))
                .label("Colour")
                .initial(colourName(g.getGroupColor()))
                .maxLength(16)
                .validate(input -> parseColour(input) != null, "That isn't a colour name or hex value")
                .onSubmit((who, input) -> {
                    g.setGroupColor(parseColour(input));
                    CivUI.success(who, "Changed the colour of ", NLText.group(g), " to ", g.getGroupNameColored());
                })
                .onCancel(who -> {
                    CivUI.info(who, "Left the colour unchanged");
                    showScreen();
                })
                .open(p));
    }

    private static TextColor parseColour(String input) {
        String trimmed = input.trim();
        TextColor color = NamedTextColor.NAMES.value(trimmed.toLowerCase(Locale.ROOT));
        return color != null ? color : TextColor.fromHexString(trimmed);
    }

    private static String colourName(TextColor color) {
        if (color == null) {
            return "";
        }
        String name = color instanceof NamedTextColor named ? NamedTextColor.NAMES.key(named) : null;
        return name != null ? name : color.asHexString();
    }

}
