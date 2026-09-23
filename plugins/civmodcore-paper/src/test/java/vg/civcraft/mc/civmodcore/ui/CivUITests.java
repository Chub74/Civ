package vg.civcraft.mc.civmodcore.ui;

import java.util.logging.Logger;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ObjectComponent;
import net.kyori.adventure.text.object.PlayerHeadObjectContents;
import net.kyori.adventure.text.object.SpriteObjectContents;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Rendering tests. No server needed - assertions run against the legacy serialization of CivUI.render. */
public class CivUITests {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    @BeforeAll
    public static void setBrand() {
        CivUI.init("Eden", Logger.getLogger("CivUITests"));
    }

    private static String render(final Kind kind, final boolean withIcon, final Object... parts) {
        return LEGACY.serialize(CivUI.render(kind, true, withIcon, parts));
    }

    /** Brand in HEADING, separator in MUTED, body in the archetype colour. */
    @Test
    public void testBrandAndSeparator() {
        Assertions.assertEquals(
            "&2&lEden&8 » &cYou can't reinforce that block.",
            render(Kind.ERROR, false, "You can't reinforce that block."));
    }

    /** Each archetype colours its body and nothing else. */
    @Test
    public void testArchetypeColours() {
        Assertions.assertEquals("&2&lEden&8 » &aDone.", render(Kind.SUCCESS, false, "Done."));
        Assertions.assertEquals("&2&lEden&8 » &cNope.", render(Kind.ERROR, false, "Nope."));
        Assertions.assertEquals("&2&lEden&8 » &eCareful.", render(Kind.WARN, false, "Careful."));
        Assertions.assertEquals("&2&lEden&8 » &7Noted.", render(Kind.INFO, false, "Noted."));
    }

    /** A token child overrides the archetype colour; plain text after it inherits it again. */
    @Test
    public void testTokensOverrideAndInherit() {
        Assertions.assertEquals(
            "&2&lEden&8 » &aReinforced with &bStone&a.",
            render(Kind.SUCCESS, false, "Reinforced with ", CivText.value("Stone"), "."));
    }

    /** No glyph unless asked for. */
    @Test
    public void testNoIconByDefault() {
        Assertions.assertFalse(render(Kind.ERROR, false, "Nope.").contains("✖"));
        Assertions.assertEquals(
            "&2&lEden&8 » &c✖ Nope.",
            render(Kind.ERROR, true, "Nope."));
    }

    /** INFO has no glyph, so asking for an icon must not insert a stray space. */
    @Test
    public void testInfoHasNoGlyph() {
        Assertions.assertEquals(
            render(Kind.INFO, false, "Noted."),
            render(Kind.INFO, true, "Noted."));
    }

    /** Non-player audiences get the ASCII stand-in. */
    @Test
    public void testAsciiFallbackForNonPlayers() {
        final String console = LEGACY.serialize(CivUI.render(Kind.ERROR, false, true, "Nope."));
        Assertions.assertEquals("&2&lEden&8 » &c[x] Nope.", console);
    }

    /** init() changes every subsequent message. */
    @Test
    public void testBrandIsConfigurable() {
        try {
            CivUI.init("Test", Logger.getLogger("CivUITests"));
            Assertions.assertEquals("&2&lTest&8 » &7Noted.", render(Kind.INFO, false, "Noted."));
        } finally {
            CivUI.init("Eden", Logger.getLogger("CivUITests"));
        }
    }

    /** Component parts keep their own styling. */
    @Test
    public void testComponentPartsSurvive() {
        Assertions.assertEquals(
            "&2&lEden&8 » &cYou don't have permission on &fAurora&c.",
            render(Kind.ERROR, false, "You don't have permission on ", CivText.subject("Aurora"), "."));
    }

    /** A notice with no recipients throws rather than silently doing nothing. */
    @Test
    public void testNoticeRequiresRecipients() {
        Assertions.assertThrows(IllegalStateException.class,
            () -> CivUI.notice(Kind.INFO, "Noted.").send());
    }

    /** Null parts render as nothing, not as the string "null". */
    @Test
    public void testNullPartRendersEmpty() {
        Assertions.assertEquals(
            "&2&lEden&8 » &7Noted.",
            render(Kind.INFO, false, "Noted.", (Object) null));
    }

    /** Icon and sound are both off unless asked for. */
    @Test
    public void testSoundAndIconAreOptIn() {
        final Notice plain = CivUI.notice(Kind.ERROR, "Nope.");
        Assertions.assertFalse(plain.hasSound(), "sound must be off unless asked for");
        Assertions.assertFalse(plain.hasIcon(), "icon must be off unless asked for");

        final Notice loud = CivUI.notice(Kind.ERROR, "Nope.").sound().icon();
        Assertions.assertTrue(loud.hasSound());
        Assertions.assertTrue(loud.hasIcon());
    }

    /** sound(Sound) must opt in too, not arm an override that never plays. */
    @Test
    public void testExplicitSoundOptsIn() {
        final Notice notice = CivUI.notice(Kind.INFO, "Noted.")
            .sound(Sound.sound(Key.key("ui.button.click"), Sound.Source.UI, 0.3F, 1.0F));
        Assertions.assertTrue(notice.hasSound());
        Assertions.assertNotNull(notice.soundOverride());
    }

    /**
     * Legacy-deserialized bodies keep their inline colours, and uncoloured segments still inherit the archetype.
     * Citadel concatenates ChatColor into message strings, so this is what stops them rendering as raw section
     * signs.
     */
    @Test
    public void testLegacyBodyInheritsAndOverrides() {
        final Component body = LegacyComponentSerializer.legacySection()
            .deserialize("You are still in §emode");
        Assertions.assertEquals(
            "&2&lEden&8 » &7You are still in &emode",
            LEGACY.serialize(CivUI.render(Kind.INFO, true, false, body)));
    }

    /** Groups have their own token, continuing the LIGHT_PURPLE convention already in the codebase. */
    @Test
    public void testGroupToken() {
        Assertions.assertEquals(
            "&2&lEden&8 » &cYou can't reinforce on &dAurora&c.",
            render(Kind.ERROR, false, "You can't reinforce on ", CivText.group("Aurora"), "."));
    }

    /** Mode names get their own token, continuing the YELLOW convention already in the codebase. */
    @Test
    public void testModeToken() {
        Assertions.assertEquals(
            "&2&lEden&8 » &eFortifying&a with &bStone&a on &dAurora",
            render(Kind.SUCCESS, false, CivText.mode("Fortifying"), " with ",
                CivText.value("Stone"), " on ", CivText.group("Aurora")));
    }

    /** Sprites render inline from an atlas the client already has - no resource pack. */
    @Test
    public void testSprite() {
        final ObjectComponent c = (ObjectComponent) CivText.sprite(Key.key("minecraft:block/stone"));
        final SpriteObjectContents sprite = (SpriteObjectContents) c.contents();
        Assertions.assertEquals(Key.key("minecraft:blocks"), sprite.atlas());
        Assertions.assertEquals(Key.key("minecraft:block/stone"), sprite.sprite());
        Assertions.assertEquals(NamedTextColor.WHITE, c.color(),
            "a sprite must be white, or the client tints the texture with the text colour");
    }

    /** A sprite composes into a message like any other part. */
    @Test
    public void testSpriteInMessage() {
        final Component msg = CivUI.render(Kind.INFO, true, false,
            "Reinforced with ", CivText.sprite(Key.key("minecraft:block/stone")), " Stone");
        Assertions.assertTrue(msg.children().stream()
            .flatMap(c -> c.children().stream())
            .anyMatch(c -> c instanceof ObjectComponent),
            "the sprite must survive composition");
    }

    /** A sprite inside a coloured message must not pick up that colour. */
    @Test
    public void testSpriteIsNotTintedByArchetype() {
        final Component msg = CivUI.render(Kind.ERROR, true, false,
            "No ", CivText.sprite(Key.key("minecraft:block/stone")), " left");
        final ObjectComponent sprite = msg.children().stream()
            .flatMap(c -> c.children().stream())
            .filter(c -> c instanceof ObjectComponent)
            .map(c -> (ObjectComponent) c)
            .findFirst().orElseThrow();
        Assertions.assertEquals(NamedTextColor.WHITE, sprite.color());
    }

    /** Clients too old for object components must degrade quietly, not show a raw placeholder. */
    @Test
    public void testSpriteHasFallback() {
        final ObjectComponent c = (ObjectComponent) CivText.sprite(Key.key("minecraft:block/stone"));
        Assertions.assertEquals(Component.empty(), c.fallback());
    }

    /** Player heads render inline too. */
    @Test
    public void testPlayerHead() {
        final ObjectComponent c = (ObjectComponent) CivText.head("Notch");
        final PlayerHeadObjectContents head = (PlayerHeadObjectContents) c.contents();
        Assertions.assertEquals("Notch", head.name());
    }

    /** Symbol and word are one value, in the VALUE token. */
    @Test
    public void testToggleMarker() {
        Assertions.assertEquals("&b⏽ on", LEGACY.serialize(CivText.toggle(true)));
        Assertions.assertEquals("&b⭘ off", LEGACY.serialize(CivText.toggle(false)));
    }

    /** A sprite brand renders as the texture, with the word kept as the fallback for old clients. */
    @Test
    public void testSpriteBrand() {
        try {
            CivUI.init("Eden", Key.key("item/golden_apple"), Logger.getLogger("CivUITests"));
            final Component msg = CivUI.render(Kind.INFO, true, false, "Noted");
            final ObjectComponent brand = (ObjectComponent) msg.children().get(0).children().get(0);
            final SpriteObjectContents sprite = (SpriteObjectContents) brand.contents();
            Assertions.assertEquals(Key.key("minecraft:items"), sprite.atlas());
            Assertions.assertEquals(Key.key("minecraft:item/golden_apple"), sprite.sprite());
            Assertions.assertEquals("Eden",
                net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                    .serialize(brand.fallback()),
                "old clients must still see the brand word");
        } finally {
            CivUI.init("Eden", Logger.getLogger("CivUITests"));
        }
    }

    /** ERROR is &c, not &4. */
    @Test
    public void testErrorIsLightRed() {
        Assertions.assertEquals("&cx", LEGACY.serialize(Component.text("x", CivText.ERROR)));
        Assertions.assertEquals("&ax", LEGACY.serialize(Component.text("x", CivText.SUCCESS)));
    }

}
