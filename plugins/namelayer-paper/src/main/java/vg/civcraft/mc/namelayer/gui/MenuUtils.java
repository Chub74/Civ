package vg.civcraft.mc.namelayer.gui;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.inventory.ItemStack;
import vg.civcraft.mc.civmodcore.ui.CivText;

public class MenuUtils {

    /** A toggle: its state as a toggle value, what a click does as a hint */
    public static ItemStack toggleButton(boolean initState, String name, boolean canModify) {
        ItemStack is = initState ? AbstractGroupGUI.yesStack() : AbstractGroupGUI.noStack();
        name(is, CivText.value(name));
        if (canModify) {
            lore(is, CivText.toggle(initState), Component.empty(), hint(initState ? "Click to turn off" : "Click to turn on"));
        } else {
            lore(is, CivText.toggle(initState), Component.empty(),
                Component.text("You don't have permission to change this", CivText.ERROR));
        }
        return is;
    }

    /** Sets the item's base name, which never renders italic and replaces any custom name */
    public static void name(ItemStack is, Component name) {
        is.unsetData(DataComponentTypes.CUSTOM_NAME);
        is.setData(DataComponentTypes.ITEM_NAME, name.colorIfAbsent(CivText.VALUE));
    }

    /** Appends lore lines, with the italics lore otherwise gets turned off */
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

    /** The last lore line: what a click does */
    public static Component hint(String text) {
        return Component.text(text, CivText.MUTED);
    }

    /** ClickableInventory only takes a String title */
    public static String title(String text) {
        return LegacyComponentSerializer.legacySection().serialize(CivText.heading(text));
    }

    public static ItemStack getPlayerSkull(UUID uuid) {
        return null; // TODO?
    }

    /**
     * Doesn't work
     *
     * @param lore the lore to split
     * @return a split list of lores
     */
    public static List<String> splitLore(String lore) {
        System.out.println("Splitting " + lore);
        List<String> splitLore = new ArrayList<>();
        int maxLineLength = 50;
        StringBuilder sb = new StringBuilder();
        String[] split = lore.split(" ");
        for (int i = 0; i < split.length; i++) {
            String word = split[i];
            if ((sb.length() + word.length()) > maxLineLength) {
                //max line length reached
                if (sb.length() == 0) {
                    //if empty, the word alone fills the line length so put it in anyway
                    sb.append(word);
                } else {
                    //include word in next run
                    i--;
                }
                //add finished line
                splitLore.add(sb.toString());
                sb = new StringBuilder();
            } else {
                //just append, line not full yet
                sb.append(" ");
                sb.append(word);
            }
        }
        return splitLore;
    }
}
