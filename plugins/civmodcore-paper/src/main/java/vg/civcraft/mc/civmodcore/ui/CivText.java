package vg.civcraft.mc.civmodcore.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.ObjectComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.object.ObjectContents;
import net.kyori.adventure.text.object.PlayerHeadObjectContents;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class CivText {

    public static final TextColor SUCCESS = NamedTextColor.GREEN;

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
        Map.entry(Material.FLOWERING_AZALEA, "block/flowering_azalea_top"),
        Map.entry(Material.HOPPER, "item/hopper"),
        Map.entry(Material.PISTON, "block/piston_side"),
        Map.entry(Material.STICKY_PISTON, "block/piston_top_sticky"),
        Map.entry(Material.OBSERVER, "block/observer_front"),
        Map.entry(Material.DISPENSER, "block/dispenser_front"),
        Map.entry(Material.DROPPER, "block/dropper_front"),
        Map.entry(Material.COMPARATOR, "item/comparator"),
        Map.entry(Material.REPEATER, "item/repeater"),
        Map.entry(Material.FURNACE, "block/furnace_front"),
        Map.entry(Material.BLAST_FURNACE, "block/blast_furnace_front"),
        Map.entry(Material.SMOKER, "block/smoker_front"),
        Map.entry(Material.BREWING_STAND, "item/brewing_stand"),
        Map.entry(Material.CAULDRON, "item/cauldron"),
        Map.entry(Material.COMPASS, "item/compass_16"),
        Map.entry(Material.RECOVERY_COMPASS, "item/recovery_compass_16"),
        Map.entry(Material.CLOCK, "item/clock_00"),
        Map.entry(Material.MAGMA_BLOCK, "block/magma"),
        Map.entry(Material.SMOOTH_QUARTZ, "block/quartz_block_bottom"),
        Map.entry(Material.SMOOTH_SANDSTONE, "block/sandstone_top"),
        Map.entry(Material.SMOOTH_RED_SANDSTONE, "block/red_sandstone_top"),
        Map.entry(Material.BIG_DRIPLEAF, "block/big_dripleaf_top"),
        Map.entry(Material.SMALL_DRIPLEAF, "block/small_dripleaf_top"));

    /** Faces tried, in order, for a block with no plain block/<name> sprite. */
    private static final List<String> FACE_SUFFIXES = List.of("_front", "_side", "_top");

    /** Rendered from a 3D model, with no flat texture to show. */
    private static final Set<Material> NO_ICON = Set.of(Material.SHIELD);

    /**
     * Every block/ and item/ texture in the client, from sprites.txt. A sprite the client lacks renders as the
     * missing texture, so icon() only names sprites listed here. Regenerate the file on a Minecraft update:
     * list assets/minecraft/textures/{block,item}/*.png in the client jar, without the prefix and extension.
     */
    private static final Set<String> KNOWN_SPRITES = loadKnownSprites();

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
     * A player's head from a skin the server already holds
     */
    @NotNull
    public static Component head(@NotNull final PlayerHeadObjectContents.SkinSource skin) {
        return untinted(Component.object(ObjectContents.playerHead(skin)));
    }

    /**
     * An item's texture, coloured the way the game colours it
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
        if (KNOWN_SPRITES.isEmpty()) {
            return material.isBlock()
                ? sprite(Key.key("block/" + name))
                : sprite(ITEM_ATLAS, Key.key("item/" + name));
        }
        final String sprite = findSprite(name);
        if (sprite == null) {
            return null;
        }
        return sprite.startsWith("item/") ? sprite(ITEM_ATLAS, Key.key(sprite)) : sprite(Key.key(sprite));
    }

    /** The sprite that best shows a material, or null if the client has none for it. */
    @Nullable
    private static String findSprite(@NotNull final String name) {
        // Waxing doesn't change the texture
        if (name.startsWith("waxed_")) {
            return findSprite(name.substring("waxed_".length()));
        }
        // The flat item texture reads best; blocks whose item is the block model only have block/
        if (KNOWN_SPRITES.contains("item/" + name)) {
            return "item/" + name;
        }
        if (KNOWN_SPRITES.contains("block/" + name)) {
            return "block/" + name;
        }
        for (final String face : FACE_SUFFIXES) {
            if (KNOWN_SPRITES.contains("block/" + name + face)) {
                return "block/" + name + face;
            }
        }
        // Fences and gates are drawn from the planks or bricks they're made of
        for (final String suffix : List.of("_fence_gate", "_fence")) {
            if (name.endsWith(suffix)) {
                final String base = name.substring(0, name.length() - suffix.length());
                for (final String material : List.of(base + "_planks", base + "s", base)) {
                    if (KNOWN_SPRITES.contains("block/" + material)) {
                        return "block/" + material;
                    }
                }
            }
        }
        return null;
    }

    @NotNull
    private static Set<String> loadKnownSprites() {
        try (InputStream in = CivText.class.getResourceAsStream("/sprites.txt")) {
            if (in == null) {
                return Set.of();
            }
            return new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)).lines()
                .map(String::strip)
                .filter(line -> !line.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
        } catch (final IOException e) {
            return Set.of();
        }
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
