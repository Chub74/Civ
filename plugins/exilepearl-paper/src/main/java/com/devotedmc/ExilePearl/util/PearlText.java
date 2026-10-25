package com.devotedmc.ExilePearl.util;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.civmodcore.ui.Kind;

/**
 * Sends ExilePearl's message templates through CivUI.
 *
 * <p>A template may open with {@code <b>} (error), {@code <g>} (success) or {@code <w>} (warning), otherwise it is
 * info. Inside it, {@code <c>} starts a name or command, {@code <p>} a value, {@code <grp>} a group, {@code <m>}
 * muted text, and {@code <i>} goes back to the body colour. {@code %s} and {@code %d} take the arguments in order,
 * as plain text, so a name can't carry tags of its own.
 */
public final class PearlText {

    private PearlText() {
    }

    public static void send(Audience to, String template, Object... args) {
        if (to == null || template == null || template.isEmpty()) {
            return;
        }
        CivUI.notice(kind(template), body(template, args)).to(to).send();
    }

    /** The archetype a template opens with. */
    public static Kind kind(String template) {
        if (template.startsWith("<b>")) {
            return Kind.ERROR;
        }
        if (template.startsWith("<g>")) {
            return Kind.SUCCESS;
        }
        if (template.startsWith("<w>")) {
            return Kind.WARN;
        }
        return Kind.INFO;
    }

    /** The template as a component, uncoloured where it uses the body colour so the archetype can fill it in. */
    public static Component body(String template, Object... args) {
        TextComponent.Builder out = Component.text();
        StringBuilder run = new StringBuilder();
        TextColor colour = null;
        int arg = 0;
        int i = 0;
        while (i < template.length()) {
            char ch = template.charAt(i);
            if (ch == '<') {
                int end = template.indexOf('>', i);
                if (end > i) {
                    TextColor next = tagColour(template.substring(i + 1, end));
                    if (next != null || isBodyTag(template.substring(i + 1, end))) {
                        flush(out, run, colour);
                        colour = next;
                        i = end + 1;
                        continue;
                    }
                }
            } else if (ch == '%' && i + 1 < template.length()) {
                char spec = template.charAt(i + 1);
                if (spec == 's' || spec == 'd') {
                    run.append(arg < args.length ? String.valueOf(args[arg++]) : "");
                    i += 2;
                    continue;
                }
                if (spec == '%') {
                    run.append('%');
                    i += 2;
                    continue;
                }
            }
            run.append(ch);
            i++;
        }
        flush(out, run, colour);
        return out.build();
    }

    /** Block coordinates and world as an attached value: {@code [x y z world]}. */
    public static Component coords(Location location) {
        String text = location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ() + " "
            + location.getWorld().getName();
        return Component.text()
            .append(Component.text("[", CivText.MUTED))
            .append(CivText.value(text))
            .append(Component.text("]", CivText.MUTED))
            .build();
    }

    private static void flush(TextComponent.Builder out, StringBuilder run, TextColor colour) {
        if (run.isEmpty()) {
            return;
        }
        out.append(colour == null ? Component.text(run.toString()) : Component.text(run.toString(), colour));
        run.setLength(0);
    }

    // n, a, h and l are older tags the admin commands still use
    private static boolean isBodyTag(String tag) {
        return switch (tag) {
            case "i", "b", "g", "w", "n" -> true;
            default -> false;
        };
    }

    private static TextColor tagColour(String tag) {
        return switch (tag) {
            case "c", "a" -> CivText.SUBJECT;
            case "p", "h" -> CivText.VALUE;
            case "grp" -> CivText.GROUP;
            case "m", "gray" -> CivText.MUTED;
            case "l" -> CivText.SUCCESS;
            default -> null;
        };
    }
}
