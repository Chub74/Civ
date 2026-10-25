package com.devotedmc.ExilePearl.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.Kind;

class PearlTextTest {

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    @Test
    void kindFollowsTheOpeningTag() {
        Assertions.assertEquals(Kind.ERROR, PearlText.kind("<b>No"));
        Assertions.assertEquals(Kind.SUCCESS, PearlText.kind("<g>Yes"));
        Assertions.assertEquals(Kind.WARN, PearlText.kind("<w>Careful"));
        Assertions.assertEquals(Kind.INFO, PearlText.kind("<i>Hello"));
        Assertions.assertEquals(Kind.INFO, PearlText.kind("<c>Bob <i>did something"));
    }

    @Test
    void tagsBecomeColoursAndArgumentsFillIn() {
        Component body = PearlText.body("<g>You freed <c>%s <m>[<p>%d %d %d %s<m>]", "Bob", 1, -2, 3, "world");
        Assertions.assertEquals("You freed Bob [1 -2 3 world]", plain(body));

        TextComponent name = (TextComponent) body.children().get(1);
        Assertions.assertEquals("Bob ", name.content());
        Assertions.assertEquals(CivText.SUBJECT, name.color());
        // Body text stays uncoloured so the archetype colours it
        Assertions.assertNull(body.children().get(0).color());
    }

    @Test
    void argumentsAreNotParsedForTags() {
        Component body = PearlText.body("<i>Hi %s", "<b>Mallory");
        Assertions.assertEquals("Hi <b>Mallory", plain(body));
    }

    @Test
    void unknownTagsAreKeptAsText() {
        Assertions.assertEquals("a <banned> b", plain(PearlText.body("a <banned> b")));
    }
}
