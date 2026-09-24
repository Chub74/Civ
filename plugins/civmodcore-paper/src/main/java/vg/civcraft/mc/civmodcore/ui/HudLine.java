package vg.civcraft.mc.civmodcore.ui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;

/**
 * One sidebar or action bar line: "Reinforcing Aurora", "Destination Spawn". Parts are joined by a space;
 * a bare String is an INFO label, a component keeps its token colour. No brand - these are ambient.
 * Returns a legacy String because ScoreBoardAPI and BottomLineAPI only take Strings.
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
