package com.github.igotyou.FactoryMod.utility;

import com.github.igotyou.FactoryMod.FactoryMod;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import vg.civcraft.mc.civmodcore.ui.Channel;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.civmodcore.ui.Kind;

public final class FMText {

    /** Denials fire on every click, so one per second per player at most */
    private static final Duration DENY_WINDOW = Duration.ofSeconds(1);

    private FMText() {
    }

    /** A factory, pipe or sorter */
    public static Component factory(String name) {
        return CivText.subject(name);
    }

    public static Component recipe(String name) {
        return CivText.value(name);
    }

    /** The client's own name for the material */
    public static Component item(Material material) {
        return Component.translatable(material).color(CivText.VALUE);
    }

    /** A custom name if the item has one, otherwise the material's */
    public static Component item(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().displayName().colorIfAbsent(CivText.VALUE);
        }
        if (item.hasItemMeta() && item.getItemMeta().hasItemName()) {
            return item.getItemMeta().itemName().colorIfAbsent(CivText.VALUE);
        }
        return item(item.getType());
    }

    /** The item's texture and a space, or nothing if it has none. Custom items show their base material's texture */
    public static Component icon(ItemStack item) {
        Component icon = item == null ? null : CivText.icon(item);
        return icon == null ? Component.empty() : icon.append(Component.space());
    }

    /** An error that can repeat with every click. On the action bar, so it doesn't pile up in chat */
    public static void deny(Audience to, Object... parts) {
        CivUI.notice(Kind.ERROR, parts).from(FactoryMod.getInstance()).to(to).on(Channel.ACTION_BAR)
            .atMostEvery(DENY_WINDOW).send();
    }

    /** Factory status and click feedback: on the action bar for a few seconds, not in chat */
    public static void bar(Audience to, Kind kind, Object... parts) {
        CivUI.notice(kind, parts).from(FactoryMod.getInstance()).to(to).on(Channel.ACTION_BAR).send();
    }

    /** A bold heading followed by one indented line per entry */
    public static Component list(Component heading, Collection<Component> entries) {
        return Component.text()
            .append(heading)
            .append(entries.stream()
                .map(entry -> Component.text().append(Component.newline()).append(Component.text("  ")).append(entry).build())
                .toList())
            .build();
    }

    /** Sets a menu item's base name, which never renders italic and replaces any custom name */
    public static void name(ItemStack is, Component name) {
        is.unsetData(DataComponentTypes.CUSTOM_NAME);
        is.setData(DataComponentTypes.ITEM_NAME, name.colorIfAbsent(CivText.VALUE));
    }

    /** A menu button's name, for constructors that take a Component name */
    public static Component label(String text) {
        return Component.text(text, CivText.VALUE).decoration(TextDecoration.ITALIC, false);
    }

    /** Appends lore lines to a menu item, with italics off */
    public static void lore(ItemStack is, Component... lines) {
        List<Component> lore = new ArrayList<>();
        ItemLore current = is.getData(DataComponentTypes.LORE);
        if (current != null) {
            lore.addAll(current.lines());
        }
        for (Component line : lines) {
            lore.add(line.colorIfAbsent(CivText.INFO).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        }
        is.setData(DataComponentTypes.LORE, ItemLore.lore(lore));
    }

    /** Replaces a menu item's lore */
    public static void setLore(ItemStack is, Component... lines) {
        is.unsetData(DataComponentTypes.LORE);
        lore(is, lines);
    }

    /** Text that still arrives as a legacy string, such as item names built elsewhere */
    public static Component legacy(String text) {
        return LegacyComponentSerializer.legacySection().deserialize(text);
    }

    /** A recipe card section title: "Input", "Output" */
    public static Component section(String title) {
        return Component.text(title, CivText.HEADING);
    }

    /** One entry in a recipe card */
    public static Component entry(Component content) {
        return Component.text().append(Component.text(" - ", CivText.MUTED)).append(content).build();
    }

    /** "3 Iron Ingot" */
    public static Component amount(int amount, Component name) {
        return Component.text().append(CivText.value(String.valueOf(amount))).append(Component.space()).append(name).build();
    }

    /** "2/3 Iron Ingot", green when there's enough and red when there isn't */
    public static Component have(int have, int needed, Component name) {
        return Component.text()
            .append(Component.text(have + "/" + needed, have >= needed ? CivText.SUCCESS : CivText.ERROR))
            .append(Component.space())
            .append(name)
            .build();
    }

    /** "Enough for 12 runs" under a recipe input */
    public static Component runs(Object count) {
        return Component.text()
            .append(CivText.info("Enough for "))
            .append(CivText.value(String.valueOf(count)))
            .append(CivText.info(" runs"))
            .build();
    }

    /** Replaces the first lore line whose plain text matches, keeping the rest */
    public static void replaceLine(ItemStack is, String plainTarget, Component replacement) {
        ItemLore current = is.getData(DataComponentTypes.LORE);
        if (current == null) {
            return;
        }
        List<Component> lines = new ArrayList<>(current.lines());
        for (int i = 0; i < lines.size(); i++) {
            if (PlainTextComponentSerializer.plainText().serialize(lines.get(i)).equals(plainTarget)) {
                lines.set(i, replacement.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
                break;
            }
        }
        is.setData(DataComponentTypes.LORE, ItemLore.lore(lines));
    }

    /** The last lore line: what a click does */
    public static Component hint(String text) {
        return Component.text(text, CivText.MUTED);
    }

    /** Menus that only take a String title */
    public static String title(String text) {
        return LegacyComponentSerializer.legacySection().serialize(CivText.heading(text));
    }
}
