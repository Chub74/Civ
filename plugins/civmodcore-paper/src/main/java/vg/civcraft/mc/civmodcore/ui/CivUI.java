package vg.civcraft.mc.civmodcore.ui;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Entry point for everything a plugin says to a player. */
public final class CivUI {

    private static final String DEFAULT_BRAND = "Eden";
    private static final String SEPARATOR = " » ";

    /** Size at which stale rate-limit entries are swept. */
    private static final int RATE_LIMIT_SWEEP_AT = 4096;

    /** Minimum age of a swept rate-limit entry. */
    private static final long RATE_LIMIT_MIN_AGE_MS = Duration.ofMinutes(10L).toMillis();

    private static final Map<String, Long> LAST_SENT = new ConcurrentHashMap<>();

    private static Component prefix = buildPrefix(DEFAULT_BRAND, null);
    private static Logger fallbackLogger = Logger.getLogger("CivUI");

    private CivUI() {
    }

    /** Sets a text brand. */
    public static void init(@NotNull final String brand, @NotNull final Logger logger) {
        init(brand, null, logger);
    }

    /** Sets the brand and caches the rendered prefix. Called once by CivModCore on enable. */
    public static void init(@NotNull final String brand, @Nullable final Key brandSprite,
                            @NotNull final Logger logger) {
        prefix = buildPrefix(Objects.requireNonNull(brand, "brand cannot be null"), brandSprite);
        fallbackLogger = Objects.requireNonNull(logger, "logger cannot be null");
        LAST_SENT.clear();
    }

    @NotNull
    private static Component buildPrefix(@NotNull final String brand, @Nullable final Key brandSprite) {
        final Component word = Component.text(brand, CivText.BRAND, TextDecoration.BOLD);
        final Component mark = brandSprite == null
            ? word
            : CivText.sprite(CivText.ITEM_ATLAS, brandSprite, word);
        return Component.text()
            .append(mark)
            .append(Component.text(SEPARATOR, CivText.MUTED))
            .build();
    }


    /** Parts are String or ComponentLike. Bare strings inherit the archetype colour. */
    public static void success(@NotNull final Audience to, @NotNull final Object... parts) {
        notice(Kind.SUCCESS, parts).to(to).send();
    }

    public static void error(@NotNull final Audience to, @NotNull final Object... parts) {
        notice(Kind.ERROR, parts).to(to).send();
    }

    public static void warn(@NotNull final Audience to, @NotNull final Object... parts) {
        notice(Kind.WARN, parts).to(to).send();
    }

    public static void info(@NotNull final Audience to, @NotNull final Object... parts) {
        notice(Kind.INFO, parts).to(to).send();
    }

    /** Returns a builder; nothing is sent until send(). */
    @NotNull
    public static Notice notice(@NotNull final Kind kind, @NotNull final Object... parts) {
        return new Notice(kind, parts);
    }

    /** A dialog asking for a line of text; nothing is shown until open(). Parts are String or ComponentLike. */
    @NotNull
    public static Prompt prompt(@NotNull final Object... title) {
        return new Prompt(title);
    }

    /** A yes/no dialog; nothing is shown until open(). Parts are String or ComponentLike. */
    @NotNull
    public static Confirm confirm(@NotNull final Object... title) {
        return new Confirm(title);
    }

    /** Renders without sending. forPlayer picks glyphs over their ASCII stand-ins. */
    @NotNull
    public static Component render(@NotNull final Kind kind,
                                   final boolean forPlayer,
                                   final boolean withIcon,
                                   @NotNull final Object... parts) {
        return Component.text().append(prefix).append(body(kind, forPlayer, withIcon, parts)).build();
    }

    /** The message without the brand: archetype colour, optional icon, parts */
    @NotNull
    private static Component body(@NotNull final Kind kind,
                                  final boolean forPlayer,
                                  final boolean withIcon,
                                  @NotNull final Object... parts) {
        final TextComponent.Builder body = Component.text().color(kind.colour());
        if (withIcon) {
            final String mark = forPlayer ? kind.glyph() : kind.ascii();
            if (mark != null) {
                body.append(Component.text(mark)).append(Component.space());
            }
        }
        for (final Object part : parts) {
            body.append(toComponent(part));
        }
        return body.build();
    }

    /** Joins parts into one component. Bare strings take the colour, or stay uncoloured if it's null. */
    @NotNull
    static Component compose(@Nullable final TextColor colour, @NotNull final Object... parts) {
        final TextComponent.Builder builder = Component.text().color(colour);
        for (final Object part : parts) {
            builder.append(toComponent(part));
        }
        return builder.build();
    }

    @NotNull
    private static Component toComponent(@Nullable final Object part) {
        if (part == null) {
            return Component.empty();
        }
        if (part instanceof final ComponentLike componentLike) {
            return componentLike.asComponent();
        }
        if (part instanceof final String string) {
            // Left uncoloured so it inherits the archetype colour from the body builder.
            return Component.text(string);
        }
        return Component.text(String.valueOf(part));
    }


    static void dispatch(@NotNull final Notice notice) {
        for (final Audience recipient : notice.recipients()) {
            if (isRateLimited(notice, recipient)) {
                continue;
            }
            final boolean isPlayer = recipient instanceof Player;
            if (notice.channel() == Channel.ACTION_BAR) {
                if (recipient instanceof final Player player) {
                    ActionBarNotices.show(player, body(notice.kind(), true, notice.hasIcon(), notice.parts()));
                    playSound(notice, recipient, true);
                }
                // Console and other non-player audiences have no action bar
                continue;
            }
            Component message = render(notice.kind(), isPlayer, notice.hasIcon(), notice.parts());
            if (notice.hoverText() != null) {
                message = message.hoverEvent(HoverEvent.showText(notice.hoverText()));
            }
            if (notice.clickEvent() != null) {
                message = message.clickEvent(notice.clickEvent());
            }
            if (isPlayer) {
                message = Bedrock.adapt(recipient, message);
            }
            recipient.sendMessage(message);
            playSound(notice, recipient, isPlayer);
        }
        writeLog(notice);
    }

    private static void playSound(@NotNull final Notice notice,
                                  @NotNull final Audience recipient,
                                  final boolean isPlayer) {
        if (!notice.hasSound() || !isPlayer) {
            return;
        }
        final Sound sound = notice.soundOverride() != null ? notice.soundOverride() : notice.kind().sound();
        if (sound != null) {
            recipient.playSound(sound);
        }
    }

    private static void writeLog(@NotNull final Notice notice) {
        final Level level = notice.logLevel();
        if (level == null) {
            return;
        }
        final Plugin plugin = notice.plugin();
        final Logger logger = plugin == null ? fallbackLogger : plugin.getLogger();
        logger.log(level, plain(notice));
    }

    @NotNull
    private static String plain(@NotNull final Notice notice) {
        final StringBuilder builder = new StringBuilder();
        for (final Object part : notice.parts()) {
            if (part instanceof final ComponentLike componentLike) {
                builder.append(PlainTextComponentSerializer.plainText().serialize(componentLike.asComponent()));
            } else if (part != null) {
                builder.append(part);
            }
        }
        return builder.toString();
    }


    private static boolean isRateLimited(@NotNull final Notice notice, @NotNull final Audience recipient) {
        final Duration window = notice.rateLimit();
        if (window == null) {
            return false;
        }
        final UUID uuid = recipient.get(Identity.UUID).orElse(null);
        if (uuid == null) {
            // No identity to key on; console and similar audiences are never limited.
            return false;
        }
        final Plugin plugin = notice.plugin();
        final String key = uuid + "|" + (plugin == null ? "?" : plugin.getName())
            + "|" + notice.kind().name() + "|" + notice.channel().name();

        final long now = System.currentTimeMillis();
        final long windowMillis = window.toMillis();
        sweepIfLarge(now, windowMillis);

        final Long previous = LAST_SENT.get(key);
        if (previous != null && now - previous < windowMillis) {
            return true;
        }
        LAST_SENT.put(key, now);
        return false;
    }

    /** Drops entries for players who left, so the map does not grow across a long uptime. */
    private static void sweepIfLarge(final long now, final long windowMillis) {
        if (LAST_SENT.size() < RATE_LIMIT_SWEEP_AT) {
            return;
        }
        final long cutoff = now - Math.max(windowMillis, RATE_LIMIT_MIN_AGE_MS);
        LAST_SENT.entrySet().removeIf(entry -> entry.getValue() < cutoff);
    }

}
