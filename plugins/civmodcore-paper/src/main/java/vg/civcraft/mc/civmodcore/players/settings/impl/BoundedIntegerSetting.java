package vg.civcraft.mc.civmodcore.players.settings.impl;

import net.kyori.adventure.text.Component;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivMenu;
import java.util.UUID;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class BoundedIntegerSetting extends IntegerSetting {

    private int lowerBound;
    private int upperBound;

    public BoundedIntegerSetting(JavaPlugin owningPlugin, int defaultValue, String name, String identifier,
                                 ItemStack gui, String description, boolean showAmountInGui, int lowerBound, int upperBound) {
        super(owningPlugin, defaultValue, name, identifier, gui, description, showAmountInGui);
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        if (lowerBound > upperBound) {
            throw new IllegalArgumentException("Lower bound can not be bigger than upper bound");
        }
    }

    @Override
    public boolean isValidValue(String input) {
        try {
            int number = Integer.parseInt(input);
            return number >= lowerBound && number <= upperBound;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public ItemStack getGuiRepresentation(UUID player) {
        ItemStack stack = super.getGuiRepresentation(player);
        CivMenu.lore(stack, Component.text()
            .append(CivText.info("Between "))
            .append(CivText.value(String.valueOf(lowerBound)))
            .append(CivText.info(" and "))
            .append(CivText.value(String.valueOf(upperBound)))
            .build());
        return stack;
    }

}
