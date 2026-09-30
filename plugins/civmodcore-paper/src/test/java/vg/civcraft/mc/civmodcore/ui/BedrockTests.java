package vg.civcraft.mc.civmodcore.ui;

import java.util.logging.Logger;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** What a Bedrock player sees once sprites and heads are stripped. */
public class BedrockTests {

    private static String plain(final Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    private static final Component STONE = CivText.sprite(Key.key("minecraft:block/stone"));

    /** A sprite goes, and so does the space that separated it from the next word. */
    @Test
    public void testSpriteAndItsSpaceDropped() {
        final Component message = Component.text()
            .append(Component.text("Reinforced with "))
            .append(STONE)
            .append(Component.space())
            .append(CivText.value("Stone"))
            .build();
        Assertions.assertEquals("Reinforced with Stone", plain(Bedrock.strip(message)));
    }

    /** A head in front of a name, as in chat. */
    @Test
    public void testHeadDropped() {
        final Component tag = Component.text()
            .append(Component.text("★ "))
            .append(CivText.head("Bob"))
            .append(Component.space())
            .append(CivText.subject("Bob"))
            .build();
        Assertions.assertEquals("★ Bob", plain(Bedrock.strip(tag)));
    }

    /** A space kept as the sprite's own child goes with it. */
    @Test
    public void testSpriteWithChildSpace() {
        final Component message = Component.text()
            .append(CivText.value("3"))
            .append(Component.space())
            .append(STONE.append(Component.space()))
            .append(CivText.value("Stone"))
            .build();
        Assertions.assertEquals("3 Stone", plain(Bedrock.strip(message)));
    }

    /** A sprite with a fallback, like the brand, shows the fallback instead. */
    @Test
    public void testSpriteBrandBecomesWord() {
        try {
            CivUI.init("Eden", Key.key("item/golden_apple"), Logger.getLogger("BedrockTests"));
            final Component message = CivUI.render(Kind.INFO, true, false, "Noted");
            Assertions.assertEquals("Eden » Noted", plain(Bedrock.strip(message)));
        } finally {
            CivUI.init("Eden", Logger.getLogger("BedrockTests"));
        }
    }

    /** Messages without sprites come back as the same instance. */
    @Test
    public void testUntouchedMessageIsSameInstance() {
        final Component message = Component.text().append(Component.text("Hello ")).append(CivText.value("Bob")).build();
        Assertions.assertSame(message, Bedrock.strip(message));
    }
}
