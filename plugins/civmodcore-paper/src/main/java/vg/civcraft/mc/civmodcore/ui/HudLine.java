package vg.civcraft.mc.civmodcore.ui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;

/**
 * One sidebar or action bar line, like "Reinforcing Aurora". Parts are joined by a space, a bare String is an
 * INFO label and a component keeps its colour. of() gives legacy text, component() keeps sprites.
 */
public final class HudLine {

    private HudLine() {
    }

    @NotNull
    public static String of(@NotNull final Object... parts) {
        return LegacyComponentSerializer.legacySection().serialize(component(parts));
    }

    @NotNull
    public static Component component(@NotNull final Object... parts) {
        final TextComponent.Builder line = Component.text();
        boolean first = true;
        for (final Object part : parts) {
            if (part == null) {
                continue;
            }
            if (!first) {
                line.append(Component.space());
            }
            first = false;
            if (part instanceof final ComponentLike componentLike) {
                line.append(componentLike.asComponent());
            } else {
                line.append(CivText.info(String.valueOf(part)));
            }
        }
        return line.build();
    }
}
