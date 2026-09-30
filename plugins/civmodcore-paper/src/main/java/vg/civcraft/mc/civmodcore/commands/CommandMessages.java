package vg.civcraft.mc.civmodcore.commands;

import co.aikar.commands.ACFUtil;
import co.aikar.commands.CommandIssuer;
import co.aikar.commands.MessageKeys;
import co.aikar.commands.MessageType;
import co.aikar.locales.MessageKey;
import co.aikar.locales.MessageKeyProvider;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;
import vg.civcraft.mc.civmodcore.ui.Bedrock;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.civmodcore.ui.Kind;

final class CommandMessages {

    /** ACF marks its highlights with <c1>..</c1> to <c3>..</c3> */
    private static final Pattern HIGHLIGHT = Pattern.compile("<c(\\d)>(.*?)</c\\1>", Pattern.DOTALL);

    private static final Set<MessageKey> HEADERS = Set.of(
        MessageKeys.HELP_HEADER.getMessageKey(),
        MessageKeys.HELP_DETAILED_HEADER.getMessageKey(),
        MessageKeys.HELP_SEARCH_HEADER.getMessageKey());

    private CommandMessages() {
    }

    /** @return false if the issuer can't be sent components, so ACF should handle it */
    static boolean send(@NotNull final CommandManager manager, @NotNull final CommandIssuer issuer,
                        @NotNull final MessageType type, @NotNull final MessageKeyProvider keyProvider,
                        @NotNull final String... replacements) {
        if (!(issuer.getIssuer() instanceof final Audience to)) {
            return false;
        }
        final MessageKey key = keyProvider.getMessageKey();
        final Map<String, String> values = pairs(replacements);

        if (is(key, MessageKeys.INVALID_SYNTAX)) {
            final String command = values.getOrDefault("{command}", "");
            final String usage = (command + " " + values.getOrDefault("{syntax}", "")).trim();
            CivUI.notice(Kind.ERROR, "Usage: ", CivText.value(usage))
                .to(to)
                .hover(CivText.info("Click to fill in the command"))
                .click(ClickEvent.suggestCommand(command + " "))
                .send();
            return true;
        }
        if (is(key, MessageKeys.UNKNOWN_COMMAND)) {
            CivUI.error(to, "That command doesn't exist");
            return true;
        }
        if (is(key, MessageKeys.PERMISSION_DENIED) || is(key, MessageKeys.PERMISSION_DENIED_PARAMETER)) {
            CivUI.error(to, "You don't have permission to do that");
            return true;
        }
        if (is(key, MessageKeys.ERROR_GENERIC_LOGGED) || is(key, MessageKeys.ERROR_PERFORMING_COMMAND)) {
            CivUI.error(to, "Something went wrong running that command");
            return true;
        }
        if (is(key, MessageKeys.NOT_ALLOWED_ON_CONSOLE)) {
            CivUI.error(to, "Only players can use this command");
            return true;
        }

        final String text = ACFUtil.replaceStrings(manager.getLocales().getMessage(issuer, keyProvider), replacements);
        if (type == MessageType.HELP) {
            to.sendMessage(Bedrock.adapt(to, help(key, text, values)));
            return true;
        }
        final Kind kind = type == MessageType.ERROR || type == MessageType.SYNTAX ? Kind.ERROR : Kind.INFO;
        CivUI.notice(kind, highlighted(stripErrorPrefix(text), CivText.VALUE, CivText.VALUE, CivText.VALUE)).to(to).send();
        return true;
    }

    /** Help pages are a list, not a message, so they go without the brand */
    private static Component help(final MessageKey key, final String text, final Map<String, String> values) {
        if (HEADERS.contains(key)) {
            final String title = values.getOrDefault("{commandprefix}", "") + values.getOrDefault("{command}", "");
            return Component.text()
                .append(Component.text("─── ", CivText.MUTED))
                .append(CivText.heading(title))
                .append(Component.text(" ───", CivText.MUTED))
                .build();
        }
        final Component line = highlighted(text, CivText.VALUE, CivText.SUBJECT, CivText.MUTED).colorIfAbsent(CivText.INFO);
        if (is(key, MessageKeys.HELP_FORMAT) || is(key, MessageKeys.HELP_DETAILED_COMMAND_FORMAT)) {
            final String command = values.getOrDefault("{commandprefix}", "") + values.getOrDefault("{command}", "");
            return line.clickEvent(ClickEvent.suggestCommand(command + " "))
                .hoverEvent(CivText.info("Click to fill in the command"));
        }
        return line;
    }

    /** ACF's highlights as colours, and any legacy colour codes a plugin passed in as components */
    private static Component highlighted(final String text, final TextColor c1, final TextColor c2, final TextColor c3) {
        final TextComponent.Builder result = Component.text();
        final Matcher matcher = HIGHLIGHT.matcher(text);
        int last = 0;
        while (matcher.find()) {
            result.append(legacy(text.substring(last, matcher.start())));
            final TextColor colour = switch (matcher.group(1)) {
                case "1" -> c1;
                case "2" -> c2;
                default -> c3;
            };
            result.append(legacy(matcher.group(2)).colorIfAbsent(colour));
            last = matcher.end();
        }
        result.append(legacy(text.substring(last)));
        return result.build();
    }

    private static Component legacy(final String text) {
        return text.isEmpty() ? Component.empty() : LegacyComponentSerializer.legacySection().deserialize(text);
    }

    /** CivUI's error colour already says it's an error */
    private static String stripErrorPrefix(final String text) {
        return text.startsWith("Error: ") ? text.substring("Error: ".length()) : text;
    }

    private static Map<String, String> pairs(final String... replacements) {
        final Map<String, String> result = new HashMap<>();
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            result.put(replacements[i], replacements[i + 1]);
        }
        return result;
    }

    private static boolean is(final MessageKey key, final MessageKeys expected) {
        return key.equals(expected.getMessageKey());
    }
}
