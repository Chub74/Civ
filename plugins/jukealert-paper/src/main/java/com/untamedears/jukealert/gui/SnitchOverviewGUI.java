package com.untamedears.jukealert.gui;

import com.untamedears.jukealert.model.Snitch;
import com.untamedears.jukealert.model.appender.DormantCullingAppender;
import com.untamedears.jukealert.util.JAText;
import com.untamedears.jukealert.util.JAUtility;
import java.util.LinkedList;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import vg.civcraft.mc.civmodcore.inventory.gui.Clickable;
import vg.civcraft.mc.civmodcore.inventory.gui.FastMultiPageView;
import vg.civcraft.mc.civmodcore.inventory.gui.IClickable;
import vg.civcraft.mc.civmodcore.inventory.items.ItemUtils;
import vg.civcraft.mc.civmodcore.ui.CivMenu;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.civmodcore.utilities.TextUtil;
import vg.civcraft.mc.civmodcore.world.WorldUtils;

public class SnitchOverviewGUI {

    private final List<Snitch> snitches;
    private final Player player;
    private final String title;
    private final boolean canShowDetails;

    public SnitchOverviewGUI(Player player, List<Snitch> snitches, String title, boolean canShowDetails) {
        this.snitches = snitches;
        this.player = player;
        this.title = title;
        this.canShowDetails = canShowDetails;
    }

    private List<IClickable> constructSnitchClickables(int start, int offset) {
        final List<IClickable> clickables = new LinkedList<>();
        for (int i = start; i <= Math.min(snitches.size() - 1, start + offset); i++) {
            Snitch snitch = snitches.get(i);
            // Base the snitch icon on the snitch type
            final var icon = snitch.getType().getItem().clone();
            ItemUtils.clearLore(icon);
            CivMenu.name(icon, JAText.name(snitch));
            CivMenu.lore(icon,
                CivMenu.stat("Location", JAUtility.formatLocation(snitch.getLocation(), true)),
                CivMenu.stat("Group", JAText.group(snitch.getGroup())));
            if (snitch.hasAppender(DormantCullingAppender.class)) {
                final var cull = snitch.getAppender(DormantCullingAppender.class);
                if (cull.isActive()) {
                    CivMenu.lore(icon, CivMenu.stat("Goes dormant in", TextUtil.formatDuration(cull.getTimeUntilDormant())));
                    icon.editMeta(meta -> meta.setEnchantmentGlintOverride(true));
                } else if (cull.isDormant()) {
                    CivMenu.lore(icon, CivMenu.stat("Culled in", TextUtil.formatDuration(cull.getTimeUntilCulling())));
                }
            }
            CivMenu.lore(icon, Component.empty());
            if (this.canShowDetails) {
                CivMenu.lore(icon, CivMenu.hint("Click to show its logs"));
            }
            CivMenu.lore(icon, CivMenu.hint("Right click to send a waypoint"));
            clickables.add(new Clickable(icon) {
                @Override
                public void clicked(final Player clicker) {
                    if (canShowDetails) {
                        new SnitchLogGUI(clicker, snitch).showScreen();
                    }
                }

                @Override
                protected void onRightClick(final Player clicker) {
                    final var location = snitch.getLocation();
                    if (!WorldUtils.doLocationsHaveSameWorld(location, clicker.getLocation())) {
                        CivUI.error(clicker, JAText.snitch(snitch), " is in another world");
                        return;
                    }
                    // Not through CivUI: waypoint mods only pick this up as a bare line
                    clicker.sendMessage(Component.text("["
                        + "name:" + snitch.getName() + ","
                        + "x:" + location.getBlockX() + ","
                        + "y:" + location.getBlockY() + ","
                        + "z:" + location.getBlockZ()
                        + "]"));
                }
            });
        }
        return clickables;
    }

    public void showScreen() {
        final var view = new FastMultiPageView(this.player, this::constructSnitchClickables, CivMenu.title(this.title), 6);
        view.setMenuSlot(SnitchLogGUI.constructExitClick(), 3);
        view.showScreen();
    }

}
