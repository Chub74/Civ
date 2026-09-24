package com.github.maxopoly.finale.misc;

import java.text.DecimalFormat;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.HudLine;

public final class HudText {

    private static final DecimalFormat SECONDS = new DecimalFormat("0.0");

    private HudText() {
    }

    /** ENDER_PEARL -> "Ender pearl". */
    public static String label(Material material) {
        String words = material.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(words.charAt(0)) + words.substring(1);
    }

    public static String seconds(long ticks) {
        return SECONDS.format(ticks / 20.0) + "s";
    }

    public static String cooldown(String label, long remainingTicks) {
        Component value = remainingTicks <= 0
            ? Component.text("Ready", CivText.SUCCESS)
            : CivText.value(seconds(remainingTicks));
        return HudLine.of(label, value);
    }

    /** "CPS 12" */
    public static String stat(String label, Object value) {
        return HudLine.of(label, CivText.value(String.valueOf(value)));
    }
}
