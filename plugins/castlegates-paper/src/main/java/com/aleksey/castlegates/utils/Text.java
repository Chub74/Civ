package com.aleksey.castlegates.utils;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import com.aleksey.castlegates.types.BlockCoord;
import com.aleksey.castlegates.types.CommandMode;
import com.aleksey.castlegates.types.TimerMode;
import com.aleksey.castlegates.types.TimerOperation;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.utilities.TextUtil;

/** The nouns CastleGates messages are built from, coloured by CivText role. */
public final class Text {

    private Text() {
    }

    /** Block coordinates as a value, copied to the clipboard on click. */
    public static Component coords(int x, int y, int z) {
        String text = x + " " + y + " " + z;
        return CivText.value(text)
            .clickEvent(ClickEvent.copyToClipboard(text))
            .hoverEvent(HoverEvent.showText(Component.text("Click to copy", CivText.MUTED)));
    }

    public static Component coords(BlockCoord coord) {
        return coords(coord.getX(), coord.getY(), coord.getZ());
    }

    public static Component coords(Location location) {
        return coords(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    /** The client's own name for the block, in its own language. */
    public static Component block(Material material) {
        return Component.translatable(material).color(CivText.VALUE);
    }

    public static Component seconds(long seconds) {
        return CivText.value(TextUtil.formatDuration(seconds, TimeUnit.SECONDS));
    }

    public static Component mode(CommandMode mode) {
        return CivText.mode(switch (mode) {
            case CREATE -> "Gearblock mode";
            case LINK -> "Link mode";
            case TIMER -> "Timer mode";
            case INFO -> "Info mode";
            default -> "Normal mode";
        });
    }

    /** "undraw it after 5s in door mode" */
    public static Component timer(int seconds, TimerOperation operation, TimerMode mode) {
        return Component.text()
            .append(CivText.value(operation.name().toLowerCase(Locale.ROOT)))
            .append(Component.text(" it after "))
            .append(seconds(seconds))
            .append(mode == TimerMode.DOOR ? Component.text(" in door mode") : Component.empty())
            .build();
    }
}
