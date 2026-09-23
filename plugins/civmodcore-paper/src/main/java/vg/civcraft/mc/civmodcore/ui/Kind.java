package vg.civcraft.mc.civmodcore.ui;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Colour, glyph and sound per archetype. Only the colour applies by default. */
public enum Kind {

    SUCCESS(CivText.SUCCESS, "✔", "[ok]", sound("entity.experience_orb.pickup", 1.2F)),
    ERROR(CivText.ERROR, "✖", "[x]", sound("block.note_block.bass", 0.8F)),
    WARN(CivText.WARN, "⚠", "[!]", sound("block.note_block.pling", 0.7F)),

    /** Neutral body text. No glyph, no sound. */
    INFO(CivText.INFO, null, null, null);

    private final TextColor colour;
    private final String glyph;
    private final String ascii;
    private final Sound sound;

    Kind(@NotNull final TextColor colour,
         @Nullable final String glyph,
         @Nullable final String ascii,
         @Nullable final Sound sound) {
        this.colour = colour;
        this.glyph = glyph;
        this.ascii = ascii;
        this.sound = sound;
    }

    @NotNull
    private static Sound sound(@NotNull final String key, final float pitch) {
        // Source.UI so it follows the player's UI volume slider, not the ambience mix.
        return Sound.sound(Key.key(key), Sound.Source.UI, 0.5F, pitch);
    }

    @NotNull
    public TextColor colour() {
        return this.colour;
    }

    /** Null if this archetype has no glyph. */
    @Nullable
    public String glyph() {
        return this.glyph;
    }

    /** Stand-in for audiences whose encoding is not guaranteed. Null if there is no glyph. */
    @Nullable
    public String ascii() {
        return this.ascii;
    }

    /** Null if this archetype has no sound. */
    @Nullable
    public Sound sound() {
        return this.sound;
    }

}
