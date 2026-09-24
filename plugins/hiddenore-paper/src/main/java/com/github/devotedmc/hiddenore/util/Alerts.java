package com.github.devotedmc.hiddenore.util;

import com.github.devotedmc.hiddenore.Config;
import java.util.List;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;

/** Find alerts */
public final class Alerts {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private Alerts() {
    }

    /** An empty prefix and no finds sends nothing */
    public static void found(Audience to, @Nullable String prefix, List<Component> finds) {
        boolean noPrefix = prefix == null || prefix.isEmpty();
        if (noPrefix && finds.isEmpty()) {
            return;
        }
        Component body = noPrefix ? Component.empty() : MM.deserialize(prefix);
        if (finds.isEmpty()) {
            CivUI.success(to, body);
        } else {
            CivUI.success(to, body, noPrefix ? "" : " ", Component.join(JoinConfiguration.commas(true), finds));
        }
    }

    /** One find: count, texture and name, with the item's own tooltip on hover */
    public static Component find(ItemStack item, int amount, boolean nearby) {
        Component icon = CivText.icon(item);
        Component name = name(item).colorIfAbsent(CivText.VALUE).hoverEvent(item.asHoverEvent());
        return Component.text()
            .append(CivText.value(String.valueOf(amount)))
            .append(Component.space())
            .append(icon == null ? Component.empty() : icon.append(Component.space()))
            .append(name)
            .append(nearby ? Component.text(" nearby") : Component.empty())
            .build();
    }

    /** A custom item name, else the configured pretty name, else the client's own name for the material */
    private static Component name(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().displayName();
        }
        String pretty = Config.getConfiguredPrettyName(item.getType().name());
        return pretty == null ? Component.translatable(item.getType()) : Component.text(pretty);
    }
}
