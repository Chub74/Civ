package vg.civcraft.mc.civmodcore.ui;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;


public final class CivMenu {

    private CivMenu() {
    }

    /** Sets a menu item's base name, which never renders italic and replaces any custom name */
    public static void name(@NotNull final ItemStack item, @NotNull final Component name) {
        item.unsetData(DataComponentTypes.CUSTOM_NAME);
        item.setData(DataComponentTypes.ITEM_NAME, name.colorIfAbsent(CivText.VALUE));
    }

    public static void name(@NotNull final ItemStack item, @NotNull final String name) {
        name(item, Component.text(name));
    }

    /** Appends lore lines, INFO unless coloured, with the italics lore otherwise gets turned off */
    public static void lore(@NotNull final ItemStack item, @NotNull final Component... lines) {
        final List<Component> lore = new ArrayList<>();
        final ItemLore current = item.getData(DataComponentTypes.LORE);
        if (current != null) {
            lore.addAll(current.lines());
        }
        for (final Component line : lines) {
            lore.add(line.colorIfAbsent(CivText.INFO).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        }
        item.setData(DataComponentTypes.LORE, ItemLore.lore(lore));
    }

    /** "Label: value", for a stat on an item */
    @NotNull
    public static Component stat(@NotNull final String label, @NotNull final Component value) {
        return Component.text().append(CivText.info(label + ": ")).append(value).build();
    }

    @NotNull
    public static Component stat(@NotNull final String label, @NotNull final String value) {
        return stat(label, CivText.value(value));
    }

    /** The last lore line: what a click does */
    @NotNull
    public static Component hint(@NotNull final String text) {
        return Component.text(text, CivText.MUTED);
    }

    /** A heading for inventories, which only take a String title */
    @NotNull
    public static String title(@NotNull final String text) {
        return LegacyComponentSerializer.legacySection().serialize(CivText.heading(text));
    }
}
