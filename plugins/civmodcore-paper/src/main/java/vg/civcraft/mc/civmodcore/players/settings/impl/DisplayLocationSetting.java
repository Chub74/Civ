package vg.civcraft.mc.civmodcore.players.settings.impl;

import net.kyori.adventure.text.Component;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivMenu;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import vg.civcraft.mc.civmodcore.inventory.gui.IClickable;
import vg.civcraft.mc.civmodcore.inventory.gui.LClickable;
import vg.civcraft.mc.civmodcore.inventory.gui.MultiPageView;
import vg.civcraft.mc.civmodcore.players.settings.gui.MenuSection;

public class DisplayLocationSetting extends LimitedStringSetting {

    private String displayName;

    public DisplayLocationSetting(JavaPlugin plugin, DisplayLocation defaultValue, String name, String identifier,
                                  ItemStack gui, String displayName) {
        super(plugin, defaultValue.toString(), name, identifier, gui, "Set where to display " + displayName,
            Arrays.stream(DisplayLocation.values()).map(DisplayLocation::toString).collect(Collectors.toList()),
            false);
        this.displayName = displayName;
    }

    public boolean showOnSidebar(UUID uuid) {
        DisplayLocation location = DisplayLocation.valueOf(getValue(uuid));
        return location == DisplayLocation.SIDEBAR || location == DisplayLocation.BOTH;
    }

    public boolean showOnActionbar(UUID uuid) {
        DisplayLocation location = DisplayLocation.valueOf(getValue(uuid));
        return location == DisplayLocation.ACTIONBAR || location == DisplayLocation.BOTH;
    }

    public DisplayLocation getDisplayLocation(UUID uuid) {
        return DisplayLocation.valueOf(getValue(uuid));
    }

    @Override
    public void handleMenuClick(Player player, MenuSection menu) {
        DisplayLocation currentValue = DisplayLocation.fromString(getValue(player));
        IClickable sideClick = genLocationClick(Material.YELLOW_BANNER, "Show %s only on the sidebar",
            DisplayLocation.SIDEBAR, menu, currentValue);
        IClickable actionClick = genLocationClick(Material.STONE_PRESSURE_PLATE, "Show %s only on the action bar",
            DisplayLocation.ACTIONBAR, menu, currentValue);
        IClickable bothClick = genLocationClick(Material.PAINTING, "Show %s on the sidebar and action bar",
            DisplayLocation.BOTH, menu, currentValue);
        IClickable noneClick = genLocationClick(Material.BARRIER, "Don't show %s",
            DisplayLocation.NONE, menu, currentValue);
        MultiPageView selector = new MultiPageView(player, Arrays.asList(sideClick, actionClick, bothClick, noneClick),
            "Select where to show " + displayName, true);
        selector.showScreen();
    }

    private IClickable genLocationClick(Material mat, String infoText, DisplayLocation location, MenuSection menu, DisplayLocation currentlySelect) {
        ItemStack sideStack = new ItemStack(mat);
        CivMenu.name(sideStack, String.format(infoText, displayName));
        if (location == currentlySelect) {
            sideStack.editMeta(itemMeta -> itemMeta.setEnchantmentGlintOverride(true));
            CivMenu.lore(sideStack, Component.text("Selected", CivText.SUCCESS));
        } else {
            CivMenu.lore(sideStack, CivMenu.hint("Click to select"));
        }
        return new LClickable(sideStack, p -> {
            setValue(p, location.toString());
            menu.showScreen(p);
        });
    }

    public enum DisplayLocation {
        SIDEBAR, ACTIONBAR, BOTH, NONE;

        public static DisplayLocation fromString(String s) {
            switch (s.toUpperCase()) {
                case "SIDEBAR":
                    return SIDEBAR;
                case "ACTIONBAR":
                    return ACTIONBAR;
                case "BOTH":
                    return BOTH;
                case "NONE":
                case "NEITHER":
                    return NONE;
                default:
                    return null;
            }
        }
    }

}
