package com.biggestnerd.namecolors;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import static net.kyori.adventure.text.format.NamedTextColor.NAMES;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import vg.civcraft.mc.civmodcore.inventory.gui.Clickable;
import vg.civcraft.mc.civmodcore.inventory.gui.IClickable;
import vg.civcraft.mc.civmodcore.inventory.gui.MultiPageView;
import vg.civcraft.mc.civmodcore.players.settings.PlayerSetting;
import vg.civcraft.mc.civmodcore.players.settings.gui.MenuSection;
import vg.civcraft.mc.civmodcore.ui.CivMenu;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;

public class NameColorSetting extends PlayerSetting<String> {

    public static final String RAINBOW_PERMISSION = "namecolor.rainbow";
    public static final String COLOR_PERMISSION = "namecolor.use";
    private static final String RAINBOW = "rainbow";

    /** value is what gets stored: a named colour's name or a hex code */
    private record Preset(String value, String label, TextColor color, Material material) {
    }

    private static Preset named(NamedTextColor color, Material material) {
        return new Preset(color.toString(), label(color), color, material);
    }

    private static Preset hex(String label, String hex, Material material) {
        return new Preset(hex, label, TextColor.fromHexString(hex), material);
    }

    /**
     * Menu order, around the colour wheel. Black and white are left out - white is the default name colour and
     * black is unreadable on the chat background. Kept under 42 so the menu stays one page.
     */
    private static final List<Preset> PRESETS = List.of(
        named(NamedTextColor.RED, Material.RED_CONCRETE),
        named(NamedTextColor.DARK_RED, Material.RED_WOOL),
        hex("Crimson", "#dc143c", Material.CRIMSON_NYLIUM),
        hex("Rose", "#ff007f", Material.ROSE_BUSH),
        hex("Coral", "#ff7f50", Material.FIRE_CORAL_BLOCK),
        hex("Salmon", "#fa8072", Material.SALMON),
        hex("Orange", "#ff8c00", Material.ORANGE_WOOL),
        hex("Copper", "#b87333", Material.COPPER_BLOCK),
        hex("Brown", "#a0522d", Material.BROWN_WOOL),
        hex("Tan", "#d2b48c", Material.BIRCH_PLANKS),
        named(NamedTextColor.GOLD, Material.YELLOW_GLAZED_TERRACOTTA),
        hex("Amber", "#ffbf00", Material.HONEYCOMB_BLOCK),
        named(NamedTextColor.YELLOW, Material.YELLOW_WOOL),
        hex("Lime", "#bfff00", Material.SLIME_BLOCK),
        named(NamedTextColor.GREEN, Material.LIME_WOOL),
        hex("Mint", "#98ff98", Material.LIME_STAINED_GLASS),
        hex("Emerald", "#50c878", Material.EMERALD_BLOCK),
        named(NamedTextColor.DARK_GREEN, Material.GREEN_WOOL),
        hex("Olive", "#808000", Material.MOSS_BLOCK),
        hex("Teal", "#008080", Material.WARPED_WART_BLOCK),
        named(NamedTextColor.DARK_AQUA, Material.CYAN_WOOL),
        hex("Turquoise", "#40e0d0", Material.DIAMOND_BLOCK),
        named(NamedTextColor.AQUA, Material.LIGHT_BLUE_WOOL),
        hex("Sky", "#87ceeb", Material.LIGHT_BLUE_CONCRETE),
        hex("Cornflower", "#6495ed", Material.BLUE_ORCHID),
        named(NamedTextColor.BLUE, Material.BLUE_WOOL),
        named(NamedTextColor.DARK_BLUE, Material.BLUE_CONCRETE),
        hex("Indigo", "#7b68ee", Material.LAPIS_BLOCK),
        hex("Violet", "#8f00ff", Material.PURPLE_CONCRETE),
        named(NamedTextColor.DARK_PURPLE, Material.PURPLE_WOOL),
        hex("Lavender", "#b57edc", Material.LILAC),
        named(NamedTextColor.LIGHT_PURPLE, Material.MAGENTA_WOOL),
        hex("Hot pink", "#ff69b4", Material.PINK_CONCRETE),
        hex("Pink", "#ffb6c1", Material.PINK_WOOL),
        named(NamedTextColor.GRAY, Material.LIGHT_GRAY_WOOL),
        named(NamedTextColor.DARK_GRAY, Material.GRAY_WOOL));

    private static Preset preset(String value) {
        for (Preset preset : PRESETS) {
            if (preset.value.equalsIgnoreCase(value)) {
                return preset;
            }
        }
        return null;
    }

    public NameColorSetting(JavaPlugin owningPlugin) {
        super(owningPlugin, "", "Name color", "namecolors.format", new ItemStack(Material.RED_WOOL),
            "The color of your name in chat and the tab list", true);
    }

    @Override
    public String deserialize(String serial) {
        return serial;
    }

    @Override
    public ItemStack getGuiRepresentation(UUID player) {
        ItemStack item = new ItemStack(Material.WHITE_WOOL);
        applyInfoToItemStack(item, player);
        Player play = Bukkit.getPlayer(player);
        if (play != null && !play.hasPermission(COLOR_PERMISSION) && !play.hasPermission(RAINBOW_PERMISSION)) {
            CivMenu.lore(item, Component.text("You don't have permission to change this", CivText.ERROR));
        }
        return item;
    }

    /** "Dark aqua" for DARK_AQUA */
    private static String label(NamedTextColor color) {
        String name = color.toString().replace('_', ' ');
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    /** A value as it looks: the colour's name in that colour, "Rainbow" in rainbow */
    private static Component swatch(String value) {
        if (value == null || value.isEmpty()) {
            return CivText.info("None");
        }
        if (RAINBOW.equals(value)) {
            return NameColors.rainbowify("Rainbow");
        }
        Preset preset = preset(value);
        if (preset != null) {
            return Component.text(preset.label, preset.color);
        }
        NamedTextColor named = NAMES.value(value);
        if (named != null) {
            return Component.text(label(named), named);
        }
        TextColor hex = TextColor.fromHexString(value);
        return hex == null ? CivText.value(value) : Component.text(value, hex);
    }

    /** The player's own name as it would look with this value */
    private static Component preview(Player player, String value) {
        if (RAINBOW.equals(value)) {
            return NameColors.rainbowify(player.getName());
        }
        TextColor color = NAMES.value(value);
        if (color == null) {
            color = TextColor.fromHexString(value);
        }
        return Component.text(player.getName(), color == null ? CivText.SUBJECT : color);
    }

    @Override
    protected Component valueComponent(String value) {
        return swatch(value);
    }

    private Clickable choice(Player player, MenuSection menu, ItemStack item, Component name, String value) {
        boolean selected = value.equalsIgnoreCase(getValue(player));
        CivMenu.name(item, name);
        CivMenu.lore(item, CivMenu.stat("Preview", preview(player, value)));
        CivMenu.lore(item, selected
            ? Component.text("Selected", CivText.SUCCESS)
            : CivMenu.hint("Click to select"));
        return new Clickable(item) {
            @Override
            protected void clicked(@NotNull Player clicker) {
                setValue(player, value);
                CivUI.success(player, "Your name is now ", preview(player, value));
                handleMenuClick(player, menu);
            }
        };
    }

    private void openHexPrompt(Player player, MenuSection menu) {
        String current = getValue(player);
        CivUI.prompt(getNiceName())
            .label("Hex color")
            .initial(RGB.matcher(current).matches() ? current : "#")
            .maxLength(7)
            .validate(input -> RGB.matcher(input).matches(), "Use a hex color like ", CivText.value("#55ffaa"))
            .onSubmit((p, value) -> {
                setValue(p, value);
                CivUI.success(p, "Your name is now ", preview(p, value));
                handleMenuClick(p, menu);
            })
            .onCancel(p -> handleMenuClick(p, menu))
            .open(player);
    }

    @Override
    public void handleMenuClick(Player player, MenuSection menu) {
        List<IClickable> clicks = new ArrayList<>();
        if (player.hasPermission(COLOR_PERMISSION)) {
            for (Preset preset : PRESETS) {
                clicks.add(choice(player, menu, new ItemStack(preset.material),
                    Component.text(preset.label, preset.color), preset.value));
            }

            String current = getValue(player);
            // A hex code that happens to match a preset shows as that preset instead
            boolean customSelected = RGB.matcher(current).matches() && preset(current) == null;
            ItemStack custom = new ItemStack(Material.AMETHYST_BLOCK);
            CivMenu.name(custom, "Custom color");
            if (customSelected) {
                CivMenu.lore(custom, CivMenu.stat("Current", swatch(current)),
                    Component.text("Selected", CivText.SUCCESS));
            } else {
                CivMenu.lore(custom, CivText.info("Any hex color, like #55ffaa"));
            }
            CivMenu.lore(custom, CivMenu.hint("Click to enter a color"));
            clicks.add(new Clickable(custom) {
                @Override
                protected void clicked(@NotNull Player clicker) {
                    openHexPrompt(player, menu);
                }
            });
        }
        if (player.hasPermission(RAINBOW_PERMISSION)) {
            clicks.add(choice(player, menu, new ItemStack(Material.YELLOW_STAINED_GLASS),
                NameColors.rainbowify("Rainbow"), RAINBOW));
        }
        if (clicks.isEmpty()) {
            CivUI.error(player, "You don't have permission to change your name color");
            return;
        }

        boolean noneSelected = getValue(player).isEmpty();
        ItemStack none = new ItemStack(Material.GLASS);
        CivMenu.name(none, "No color");
        CivMenu.lore(none, CivMenu.stat("Preview", CivText.subject(player.getName())));
        CivMenu.lore(none, noneSelected
            ? Component.text("Selected", CivText.SUCCESS)
            : CivMenu.hint("Click to remove your name color"));
        clicks.add(new Clickable(none) {
            @Override
            public void clicked(@NotNull Player clicker) {
                if (!noneSelected) {
                    setValue(player, "");
                    CivUI.success(player, "Removed your name color");
                }
                handleMenuClick(player, menu);
            }
        });

        MultiPageView view = new MultiPageView(player, clicks, getNiceName(), true);
        ItemStack back = CivMenu.back(menu.getName());
        view.setMenuSlot(new Clickable(back) {
            @Override
            public void clicked(@NotNull Player clicker) {
                menu.showScreen(player);
            }
        }, 0);
        view.showScreen();
    }

    private final static Pattern RGB = Pattern.compile("#[A-Fa-f0-9]{6}");

    @Override
    public boolean isValidValue(String input) {
        return NAMES.value(input) != null || RAINBOW.equals(input) || RGB.matcher(input).matches();
    }

    @Override
    public String serialize(String value) {
        return value;
    }

    @Override
    public void setValue(UUID player, String value) {
        super.setValue(player, value);
        Player play = Bukkit.getPlayer(player);
        if (play != null) {
            NameColors.getInstance().updatePlayerName(play, value);
        }
    }

    @Override
    public String toText(String value) {
        return value;
    }

}
