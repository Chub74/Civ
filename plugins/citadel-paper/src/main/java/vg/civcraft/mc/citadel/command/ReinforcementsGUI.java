package vg.civcraft.mc.citadel.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Description;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.citadel.Citadel;
import vg.civcraft.mc.citadel.acidtypes.AcidType;
import vg.civcraft.mc.citadel.reinforcementtypes.ReinforcementType;
import vg.civcraft.mc.civmodcore.inventory.gui.DecorationStack;
import vg.civcraft.mc.civmodcore.inventory.gui.IClickable;
import vg.civcraft.mc.civmodcore.inventory.gui.MultiPageView;
import vg.civcraft.mc.civmodcore.utilities.TextUtil;

import static java.util.stream.Collectors.partitioningBy;

public class ReinforcementsGUI extends BaseCommand {

    /** MultiPageView only takes a String title. */
    private static final String TITLE = LegacyComponentSerializer.legacySection()
        .serialize(CivText.heading("Reinforcements"));

    private DecimalFormat format = new DecimalFormat("##.##");

    @CommandAlias("ctdl|reinforcements")
    @Description("Opens a GUI displaying all reinforcement materials")
    public void execute(Player sender) {

        Map<Boolean, List<ReinforcementType>> allowedOrNot = Citadel.getInstance().getReinforcementTypeManager().getAllTypes()
            .stream().collect(partitioningBy(i ->
                i.isAllowedInWorld(sender.getWorld().getName())));

        // sort ascending by health
        List<ReinforcementType> allowedTypes = new ArrayList<>(allowedOrNot.get(true));
        List<ReinforcementType> disallowedTypes = new ArrayList<>(allowedOrNot.get(false));
        allowedTypes.sort(Comparator.comparingDouble(ReinforcementType::getHealth));
        disallowedTypes.sort(Comparator.comparingDouble(ReinforcementType::getHealth));

        List<IClickable> clicks = new LinkedList<>();
        clicks.addAll(getMaterialClicks(allowedTypes, true));

        if (!disallowedTypes.isEmpty()) {
            addDivider(clicks, "Not usable in this world");
            clicks.addAll(getMaterialClicks(disallowedTypes, false));
        }

        List<AcidType> acidTypes = Citadel.getInstance().getAcidManager().getAcidTypes();
        if (!acidTypes.isEmpty()) {
            addDivider(clicks, "Acid blocks");
            clicks.addAll(getAcidTypeClicks(acidTypes));
        }

        MultiPageView pageView = new MultiPageView(sender, clicks, TITLE, true);
        pageView.showScreen();
    }

    /** Finishes the current row with empty slots, then a row of labelled panes that names the next section. */
    private void addDivider(List<IClickable> clicks, String label) {
        while (clicks.size() % 9 != 0) {
            clicks.add(new DecorationStack(Material.AIR));
        }
        for (int i = 0; i < 9; i++) {
            ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            pane.setData(DataComponentTypes.ITEM_NAME, CivText.heading(label));
            clicks.add(new DecorationStack(pane));
        }
    }

    private List<IClickable> getMaterialClicks(List<ReinforcementType> types, boolean allowed) {
        List<IClickable> clickables = new LinkedList<>();

        for (ReinforcementType type : types) {
            ItemStack is = type.getItem().clone();
            // A custom name would override ITEM_NAME and render italic.
            is.unsetData(DataComponentTypes.CUSTOM_NAME);
            is.setData(DataComponentTypes.ITEM_NAME, CivText.value(type.getName()));

            List<Component> lore = existingLore(is);
            lore.add(line("Health ", CivText.value(format.format(type.getHealth()))));
            lore.add(line("Matures in ",
                CivText.value(TextUtil.formatDuration(type.getMaturationTime(), TimeUnit.MILLISECONDS))));
            if (type.getAcidTime() > 0) {
                lore.add(line("Acid matures in ",
                    CivText.value(TextUtil.formatDuration(type.getAcidTime(), TimeUnit.MILLISECONDS))));
            } else {
                lore.add(line("Can't be used for acid"));
            }
            lore.add(line("Return chance ", CivText.value(format.format(type.getReturnChance() * 100.0) + "%")));
            lore.add(line("Allowed in ", CivText.value(type.getAllowedWorlds().isEmpty()
                ? "every world"
                : String.join(", ", type.getAllowedWorlds()))));
            lore.add(Component.empty());
            lore.add(allowed
                ? line(CivText.SUCCESS, "Usable in this world")
                : line(CivText.ERROR, "Not usable in this world"));
            is.setData(DataComponentTypes.LORE, ItemLore.lore(lore));

            clickables.add(new DecorationStack(is));
        }

        return clickables;
    }

    private List<IClickable> getAcidTypeClicks(List<AcidType> acidTypes) {
        List<IClickable> clickables = new ArrayList<>();

        for (AcidType acidType : acidTypes) {
            ItemStack is = new ItemStack(acidType.material());
            is.setData(DataComponentTypes.ITEM_NAME,
                Component.translatable(acidType.material()).color(CivText.VALUE));

            String faces = acidType.blockFaces().stream()
                .map(face -> face.name().toLowerCase(Locale.ROOT))
                .collect(Collectors.joining(", "));
            is.setData(DataComponentTypes.LORE, ItemLore.lore(List.of(
                line("Acid faces ", CivText.value(faces)),
                line("Maturation modifier ", CivText.value(format.format(acidType.modifier()) + "x")))));

            clickables.add(new DecorationStack(is));
        }

        return clickables;
    }

    /** The item's own lore, followed by a blank line, so ours reads as a separate block. */
    private static List<Component> existingLore(ItemStack is) {
        List<Component> lore = new ArrayList<>();
        ItemLore current = is.getData(DataComponentTypes.LORE);
        if (current != null && !current.lines().isEmpty()) {
            lore.addAll(current.lines());
            lore.add(Component.empty());
        }
        return lore;
    }

    private static Component line(Object... parts) {
        return line(CivText.INFO, parts);
    }

    /** Lore renders italic unless told otherwise. */
    private static Component line(TextColor colour, Object... parts) {
        TextComponent.Builder line = Component.text().color(colour).decoration(TextDecoration.ITALIC, false);
        for (Object part : parts) {
            line.append(part instanceof Component component ? component : Component.text(String.valueOf(part)));
        }
        return line.build();
    }
}
