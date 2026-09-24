package vg.civcraft.mc.civmodcore.ui;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.ObjectComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.object.ObjectContents;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class CivText {

    public static final TextColor SUCCESS = NamedTextColor.GREEN;

    /** &c, the light red - not &4. */
    public static final TextColor ERROR = NamedTextColor.RED;

    public static final TextColor WARN = NamedTextColor.YELLOW;

    /** Body text, and the default. */
    public static final TextColor INFO = NamedTextColor.GRAY;

    /** A datum: coords, counts, durations, item names. */
    public static final TextColor VALUE = NamedTextColor.AQUA;

    /** A name: player, snitch, factory. */
    public static final TextColor SUBJECT = NamedTextColor.WHITE;

    /** A NameLayer group. Its own token because it is the noun Civ revolves around. */
    public static final TextColor GROUP = NamedTextColor.LIGHT_PURPLE;

    /** A mode the player is in, Normal included. Shares YELLOW with WARN; the two never share a line. */
    public static final TextColor MODE = NamedTextColor.YELLOW;

    /** The server brand that leads every message. */
    public static final TextColor BRAND = NamedTextColor.DARK_GREEN;

    /** Section titles, GUI titles, list headers. */
    public static final TextColor HEADING = NamedTextColor.GOLD;

    /** Brand separator, rules, pagination, hints. */
    public static final TextColor MUTED = NamedTextColor.DARK_GRAY;

    /** Item textures live in their own atlas; atlases/blocks.json only carries block/. */
    public static final Key ITEM_ATLAS = Key.key("minecraft:items");

    /** GUI sprites, which also carry the mob effect icons under mob_effect/. */
    public static final Key GUI_ATLAS = Key.key("minecraft:gui");

    /**
     * Materials whose texture is not named after them. A block with several faces has no plain
     * block/<name> sprite, so the face to show is named here rather than guessed.
     */
    private static final Map<Material, String> ICON_OVERRIDES = Map.ofEntries(
        Map.entry(Material.ENCHANTING_TABLE, "block/enchanting_table_top"),
        Map.entry(Material.GRASS_BLOCK, "block/grass_block_top"),
        Map.entry(Material.CROSSBOW, "item/crossbow_standby"),
        Map.entry(Material.CACTUS, "block/cactus_side"),
        Map.entry(Material.PUMPKIN, "block/pumpkin_side"),
        Map.entry(Material.MELON, "block/melon_side"),
        Map.entry(Material.BAMBOO, "item/bamboo"),
        Map.entry(Material.NETHER_WART, "item/nether_wart"),
        Map.entry(Material.AZALEA, "block/azalea_top"),
        Map.entry(Material.FLOWERING_AZALEA, "block/flowering_azalea_top"));

    /** Rendered from a 3D model, with no flat texture to show. */
    private static final Set<Material> NO_ICON = Set.of(Material.SHIELD);

    private CivText() {
    }

    /** Nearest of the sixteen named colours, for surfaces that cannot take hex. */
    @NotNull
    public static NamedTextColor named(@NotNull final TextColor colour) {
        return NamedTextColor.nearestTo(colour);
    }

    @NotNull
    public static Component value(@NotNull final String text) {
        return Component.text(text, VALUE);
    }

    @NotNull
    public static Component subject(@NotNull final String name) {
        return Component.text(name, SUBJECT);
    }

    @NotNull
    public static Component group(@NotNull final String name) {
        return Component.text(name, GROUP);
    }

    @NotNull
    public static Component mode(@NotNull final String name) {
        return Component.text(name, MODE);
    }

    @NotNull
    public static Component info(@NotNull final String text) {
        return Component.text(text, INFO);
    }

    /** A texture from a Minecraft atlas, rendered inline in chat. */
    @NotNull
    public static Component sprite(@NotNull final Key sprite) {
        return untinted(Component.object(ObjectContents.sprite(sprite)));
    }

    /** A texture from a named atlas, for sprites outside minecraft:blocks. */
    @NotNull
    public static Component sprite(@NotNull final Key atlas, @NotNull final Key sprite) {
        return untinted(Component.object(ObjectContents.sprite(atlas, sprite)));
    }

    @NotNull
    private static Component untinted(@NotNull final ObjectComponent sprite) {
        return sprite.fallback(Component.empty()).color(NamedTextColor.WHITE);
    }

    /** A player's head, rendered inline from their skin. */
    @NotNull
    public static Component head(@NotNull final String playerName) {
        return untinted(Component.object(ObjectContents.playerHead(playerName)));
    }

    /** A player's head by UUID, for offline players. */
    @NotNull
    public static Component head(@NotNull final UUID playerId) {
        return untinted(Component.object(ObjectContents.playerHead(playerId)));
    }

    /**
     * An item's texture, coloured the way the game colours it. Dyeable items (leather armour, leather
     * horse armour) ship a grey texture the client tints, so drawn plain they'd pass for iron.
     * The client multiplies a sprite by its text colour, which does the same tint here.
     */
    @Nullable
    public static Component icon(@NotNull final ItemStack item) {
        final Component icon = icon(item.getType());
        // Undyed leather reports the default leather brown here, so it needs no special case
        if (icon != null && item.getItemMeta() instanceof final LeatherArmorMeta dyed) {
            return icon.color(TextColor.color(dyed.getColor().asRGB()));
        }
        return icon;
    }

    /** A material's texture, or null if it has none to show. */
    @Nullable
    public static Component icon(@NotNull final Material material) {
        if (NO_ICON.contains(material)) {
            return null;
        }
        final String override = ICON_OVERRIDES.get(material);
        if (override != null) {
            return override.startsWith("item/")
                ? sprite(ITEM_ATLAS, Key.key(override))
                : sprite(Key.key(override));
        }
        final String name = material.getKey().getKey();
        return material.isBlock()
            ? sprite(Key.key("block/" + name))
            : sprite(ITEM_ATLAS, Key.key("item/" + name));
    }

    /** A potion effect's icon, as shown in the inventory. */
    @NotNull
    public static Component icon(@NotNull final PotionEffectType effect) {
        return sprite(GUI_ATLAS, Key.key("mob_effect/" + effect.getKey().getKey()));
    }

    /** A sprite with something for clients too old to render object components. */
    @NotNull
    public static Component sprite(@NotNull final Key atlas, @NotNull final Key sprite,
                                   @NotNull final ComponentLike fallback) {
        return Component.object(ObjectContents.sprite(atlas, sprite))
            .fallback(fallback)
            .color(NamedTextColor.WHITE);
    }

    private static final String ON = "⏽";

    private static final String OFF = "⭘";

    /** A toggle state as one value: the IEC power symbol and the word, together. */
    @NotNull
    public static Component toggle(final boolean on) {
        return Component.text((on ? ON : OFF) + (on ? " on" : " off"), VALUE);
    }

    /** Bold. */
    @NotNull
    public static Component heading(@NotNull final String text) {
        return Component.text(text, HEADING, TextDecoration.BOLD);
    }

}
