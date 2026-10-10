package vg.civcraft.mc.civmodcore.ui;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Builds one message. Nothing is sent until send(). */
public final class Notice {

    private final Kind kind;
    private final Object[] parts;
    private final List<Audience> recipients = new ArrayList<>(1);

    private Plugin plugin;
    private Channel channel = Channel.CHAT;
    private Duration rateLimit;
    private boolean icon;
    private boolean sound;
    private Sound soundOverride;
    private Component hover;
    private ClickEvent<?> click;
    private Level logLevel;

    Notice(@NotNull final Kind kind, @NotNull final Object... parts) {
        this.kind = Objects.requireNonNull(kind, "kind cannot be null");
        this.parts = Objects.requireNonNull(parts, "parts cannot be null");
    }

    /** Adds recipients, ignoring nulls. At least one is required. */
    @NotNull
    public Notice to(@NotNull final Audience... audiences) {
        for (final Audience audience : audiences) {
            if (audience != null) {
                this.recipients.add(audience);
            }
        }
        return this;
    }

    /** Names the sending plugin. Renders nothing - it keys atMostEvery and picks the logger for log. */
    @NotNull
    public Notice from(@Nullable final Plugin plugin) {
        this.plugin = plugin;
        return this;
    }

    /** Defaults to CHAT. */
    @NotNull
    public Notice on(@NotNull final Channel channel) {
        this.channel = Objects.requireNonNull(channel, "channel cannot be null");
        return this;
    }

    /** Drops this if one with the same recipient, plugin, kind and channel was sent within the window. */
    @NotNull
    public Notice atMostEvery(@NotNull final Duration window) {
        this.rateLimit = Objects.requireNonNull(window, "window cannot be null");
        return this;
    }

    /** Prepends the archetype's glyph. Off unless called. */
    @NotNull
    public Notice icon() {
        this.icon = true;
        return this;
    }

    /** Plays the archetype's sound. Off unless called. No-op for non-players and for INFO. */
    @NotNull
    public Notice sound() {
        this.sound = true;
        return this;
    }

    /** Plays this instead of the archetype's sound. Implies sound(); null disables both. */
    @NotNull
    public Notice sound(@Nullable final Sound sound) {
        this.soundOverride = sound;
        this.sound = sound != null;
        return this;
    }

    @NotNull
    public Notice hover(@Nullable final ComponentLike hover) {
        this.hover = hover == null ? null : hover.asComponent();
        return this;
    }

    @NotNull
    public Notice click(@Nullable final ClickEvent<?> click) {
        this.click = click;
        return this;
    }

    /** Also writes the message, as plain text, to the sending plugin's logger. */
    @NotNull
    public Notice log(@NotNull final Level level) {
        this.logLevel = Objects.requireNonNull(level, "level cannot be null");
        return this;
    }

    /** Throws IllegalStateException if no recipients were given. */
    public void send() {
        if (this.recipients.isEmpty()) {
            throw new IllegalStateException("A notice needs at least one recipient; call to(...)");
        }
        if (this.channel != Channel.CHAT && (this.hover != null || this.click != null)) {
            throw new IllegalStateException("hover and click only work on CHAT");
        }
        CivUI.dispatch(this);
    }

    @NotNull
    Kind kind() {
        return this.kind;
    }

    @NotNull
    Object[] parts() {
        return this.parts;
    }

    @NotNull
    List<Audience> recipients() {
        return this.recipients;
    }

    @Nullable
    Plugin plugin() {
        return this.plugin;
    }

    @NotNull
    Channel channel() {
        return this.channel;
    }

    @Nullable
    Duration rateLimit() {
        return this.rateLimit;
    }

    boolean hasIcon() {
        return this.icon;
    }

    boolean hasSound() {
        return this.sound;
    }

    @Nullable
    Sound soundOverride() {
        return this.soundOverride;
    }

    @Nullable
    Component hoverText() {
        return this.hover;
    }

    @Nullable
    ClickEvent<?> clickEvent() {
        return this.click;
    }

    @Nullable
    Level logLevel() {
        return this.logLevel;
    }

}
