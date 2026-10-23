package com.untamedears.jukealert.gui;

import com.untamedears.jukealert.model.Snitch;
import com.untamedears.jukealert.model.actions.abstr.LoggableAction;
import com.untamedears.jukealert.model.appender.LeverToggleAppender;
import com.untamedears.jukealert.model.appender.SnitchLogAppender;
import com.untamedears.jukealert.util.JAText;
import com.untamedears.jukealert.util.JAUtility;
import com.untamedears.jukealert.util.JukeAlertPermissionHandler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import vg.civcraft.mc.civmodcore.inventory.gui.Clickable;
import vg.civcraft.mc.civmodcore.inventory.gui.ClickableInventory;
import vg.civcraft.mc.civmodcore.inventory.gui.DecorationStack;
import vg.civcraft.mc.civmodcore.inventory.gui.IClickable;
import vg.civcraft.mc.civmodcore.inventory.gui.MultiPageView;
import vg.civcraft.mc.civmodcore.ui.CivMenu;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.civmodcore.utilities.MoreCollectionUtils;
import vg.civcraft.mc.namelayer.permission.PermissionType;

public class SnitchLogGUI {

    private final Player player;
    private final Snitch snitch;
    private final SnitchLogAppender logAppender;
    private List<IClickable> buttonCache;

    public SnitchLogGUI(@NotNull final Player player,
                        @NotNull final Snitch snitch) {
        this.player = Objects.requireNonNull(player);
        this.snitch = Objects.requireNonNull(snitch);
        this.logAppender = snitch.getAppender(SnitchLogAppender.class);
    }

    private boolean INTERNAL_hasPermission(@NotNull final PermissionType permission) {
        return this.snitch.hasPermission(this.player.getUniqueId(), permission);
    }

    private static Component noPermission() {
        return Component.text("You don't have permission to do this", CivText.ERROR);
    }

    private List<IClickable> INTERNAL_constructContent() {
        if (this.logAppender == null) {
            final var item = new ItemStack(Material.BARRIER);
            CivMenu.name(item, "No logs");
            CivMenu.lore(item, Component.text("This snitch doesn't keep logs"));
            return MoreCollectionUtils.asLazyList(Collections.singletonList(() -> new DecorationStack(item)));
        }
        final var actions = this.logAppender.getFullLogs();
        if (actions.isEmpty()) {
            final var item = new ItemStack(Material.BARRIER);
            CivMenu.name(item, "No logs");
            CivMenu.lore(item, Component.text("Nothing has happened near this snitch yet"));
            return MoreCollectionUtils.asLazyList(Collections.singletonList(() -> new DecorationStack(item)));
        }
        final var buttons = new ArrayList<Supplier<IClickable>>(actions.size());
        for (final LoggableAction action : actions) {
            buttons.add(action::getGUIRepresentation);
        }
        return MoreCollectionUtils.asLazyList(buttons);
    }

    private IClickable INTERNAL_constructClearClick() {
        final var item = new ItemStack(Material.TNT);
        CivMenu.name(item, "Clear logs");
        if (!INTERNAL_hasPermission(JukeAlertPermissionHandler.getClearLogs())) {
            CivMenu.lore(item, noPermission());
            return new DecorationStack(item);
        }
        CivMenu.lore(item, Component.text("✖ Click to delete every log", CivText.ERROR));
        return new Clickable(item) {
            @Override
            public void clicked(@NotNull final Player clicker) {
                CivUI.confirm("Clear the logs of ", JAText.snitch(SnitchLogGUI.this.snitch), "?")
                    .warning("You can't undo this")
                    .yesLabel("Clear")
                    .noLabel("Keep them")
                    .onYes(who -> {
                        // Checked again: permissions may have changed while the dialog was open
                        if (INTERNAL_hasPermission(JukeAlertPermissionHandler.getClearLogs())) {
                            SnitchLogGUI.this.logAppender.deleteLogs();
                            if (SnitchLogGUI.this.buttonCache != null) {
                                SnitchLogGUI.this.buttonCache.clear();
                                SnitchLogGUI.this.buttonCache = null;
                            }
                            CivUI.success(who, "Cleared the logs of ", JAText.snitch(SnitchLogGUI.this.snitch));
                        }
                        showScreen();
                    })
                    .onNo(who -> showScreen())
                    .open(clicker);
            }
        };
    }

    private IClickable INTERNAL_constructNameChangeClick() {
        final var item = new ItemStack(Material.OAK_SIGN);
        CivMenu.name(item, "Rename");
        if (!INTERNAL_hasPermission(JukeAlertPermissionHandler.getRenameSnitch())) {
            CivMenu.lore(item, noPermission());
            return new DecorationStack(item);
        }
        CivMenu.lore(item, CivMenu.hint("Click to rename this snitch"));
        return new Clickable(item) {
            @Override
            public void clicked(@NotNull final Player clicker) {
                CivUI.prompt("Rename ", JAText.snitch(SnitchLogGUI.this.snitch))
                    .label("Name")
                    .initial(SnitchLogGUI.this.snitch.getName())
                    .maxLength(JAUtility.MAX_NAME_LENGTH)
                    .onSubmit((who, name) -> {
                        if (INTERNAL_hasPermission(JukeAlertPermissionHandler.getRenameSnitch())) {
                            JAUtility.renameSnitch(who, SnitchLogGUI.this.snitch, name);
                        }
                        showScreen();
                    })
                    .onCancel(who -> showScreen())
                    .open(clicker);
            }
        };
    }

    private IClickable INTERNAL_constructInfoStack() {
        final var item = new ItemStack(Material.PAPER);
        CivMenu.name(item, JAText.name(this.snitch));
        CivMenu.lore(item,
            CivMenu.stat("Location", JAUtility.formatLocation(this.snitch.getLocation(), true)),
            CivMenu.stat("Group", JAText.group(this.snitch.getGroup())),
            CivMenu.stat("Type", this.snitch.getType().getName()));
        return new DecorationStack(item);
    }

    private IClickable INTERNAL_constructLeverToggleClick() {
        final var leverAppender = this.snitch.getAppender(LeverToggleAppender.class);
        final var item = new ItemStack(Material.LEVER);
        CivMenu.name(item, "Lever toggling");
        CivMenu.lore(item,
            Component.text("Pulses levers on this snitch when it sees someone"),
            CivMenu.stat("Currently", CivText.toggle(leverAppender.shouldToggle())));
        if (!INTERNAL_hasPermission(JukeAlertPermissionHandler.getToggleLevers())) {
            CivMenu.lore(item, Component.empty(), noPermission());
            return new DecorationStack(item);
        }
        CivMenu.lore(item, Component.empty(), CivMenu.hint("Click to toggle"));
        return new Clickable(item) {
            @Override
            public void clicked(@NotNull final Player clicker) {
                if (INTERNAL_hasPermission(JukeAlertPermissionHandler.getToggleLevers())) {
                    leverAppender.switchState();
                    CivUI.success(clicker, "Lever toggling is now ", CivText.toggle(leverAppender.shouldToggle()),
                        " for ", JAText.snitch(SnitchLogGUI.this.snitch));
                    showScreen();
                }
            }
        };
    }

    public void showScreen() {
        if (this.buttonCache == null) {
            this.buttonCache = INTERNAL_constructContent();
        }
        final var view = new MultiPageView(this.player, this.buttonCache,
            CivMenu.title(StringUtils.substring(JAText.name(this.snitch), 0, 32)), true);
        if (this.logAppender != null) {
            view.setMenuSlot(INTERNAL_constructClearClick(), 1);
        }
        view.setMenuSlot(INTERNAL_constructNameChangeClick(), 2);
        view.setMenuSlot(INTERNAL_constructInfoStack(), 3);
        view.setMenuSlot(constructExitClick(), 4);
        if (this.snitch.hasAppender(LeverToggleAppender.class)) {
            view.setMenuSlot(INTERNAL_constructLeverToggleClick(), 5);
        }
        view.showScreen();
    }

    public static IClickable constructExitClick() {
        final var item = CivMenu.close();
        return new Clickable(item) {
            @Override
            public void clicked(@NotNull final Player clicker) {
                ClickableInventory.forceCloseInventory(clicker);
            }
        };
    }

}
