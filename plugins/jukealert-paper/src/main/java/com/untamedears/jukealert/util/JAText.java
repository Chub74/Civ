package com.untamedears.jukealert.util;

import com.untamedears.jukealert.model.Snitch;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import vg.civcraft.mc.civmodcore.ui.CivMenu;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.namelayer.NameLayerAPI;
import vg.civcraft.mc.namelayer.group.Group;

/**
 * The nouns JukeAlert messages are built from, coloured by CivText role.
 */
public final class JAText {

    private JAText() {
    }

    /** The snitch's name, or its type if it has none, with its location, name and group on hover */
    public static Component snitch(Snitch snitch) {
        return CivText.value(name(snitch)).hoverEvent(HoverEvent.showText(details(snitch)));
    }

    public static String name(Snitch snitch) {
        return snitch.getName().isEmpty() ? snitch.getType().getName() : snitch.getName();
    }

    private static Component details(Snitch snitch) {
        TextComponent.Builder details = Component.text()
            .append(CivMenu.stat("Location", location(snitch.getLocation(), true)));
        if (!snitch.getName().isEmpty()) {
            details.append(Component.newline()).append(CivMenu.stat("Name", CivText.subject(snitch.getName())));
        }
        return details.append(Component.newline()).append(CivMenu.stat("Group", group(snitch.getGroup()))).build();
    }

    public static Component group(@Nullable Group group) {
        return group == null ? CivText.value("an unknown group") : CivText.group(group.getName());
    }

    public static Component player(@Nullable UUID uuid) {
        if (uuid == null) {
            return CivText.subject("someone unknown");
        }
        return CivText.subject(Objects.requireNonNullElse(NameLayerAPI.getCurrentName(uuid), uuid.toString()));
    }

    /** "[x y z]", or "[world x y z]", copied as "x y z" on click. INFO, so it doesn't run into the snitch name */
    public static Component location(Location location, boolean includeWorld) {
        String coords = location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ();
        return CivText.info(JAUtility.formatLocation(location, includeWorld))
            .clickEvent(ClickEvent.copyToClipboard(coords))
            .hoverEvent(HoverEvent.showText(Component.text("Click to copy", CivText.MUTED)));
    }

    /** "[12m North]" from the player to the snitch, or null if they're in different worlds */
    @Nullable
    public static Component directions(Snitch snitch, Player player) {
        Location from = player.getLocation();
        Location to = snitch.getLocation();
        if (!to.getWorld().equals(from.getWorld())) {
            return null;
        }
        return Component.text()
            .append(Component.text("[", CivText.MUTED))
            .append(CivText.value(Math.round(from.distance(to)) + "m " + JAUtility.getCardinal(from, to)))
            .append(Component.text("]", CivText.MUTED))
            .build();
    }
}
