package net.minelink.ctplus.util;

import java.time.Duration;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.plugin.Plugin;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.civmodcore.ui.Kind;

public final class Messages {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    /** Denials fire on every click, so one per second per player at most */
    private static final Duration DENY_WINDOW = Duration.ofSeconds(1);

    private Messages() {
    }

    public static void send(Audience to, Kind kind, String template, TagResolver... resolvers) {
        if (isEmpty(template)) {
            return;
        }
        CivUI.notice(kind, MM.deserialize(template, resolvers)).to(to).send();
    }

    public static void deny(Plugin plugin, Audience to, String template, TagResolver... resolvers) {
        if (isEmpty(template)) {
            return;
        }
        CivUI.notice(Kind.ERROR, MM.deserialize(template, resolvers))
            .from(plugin)
            .to(to)
            .atMostEvery(DENY_WINDOW)
            .send();
    }

    public static Component body(TextColor colour, String template, TagResolver... resolvers) {
        return Component.text().color(colour).append(MM.deserialize(template, resolvers)).build();
    }

    public static boolean isEmpty(String template) {
        return template == null || template.isEmpty();
    }

    public static TagResolver name(String key, String name) {
        return Placeholder.component(key, CivText.subject(name));
    }

    public static TagResolver value(String key, String value) {
        return Placeholder.component(key, CivText.value(value));
    }
}
