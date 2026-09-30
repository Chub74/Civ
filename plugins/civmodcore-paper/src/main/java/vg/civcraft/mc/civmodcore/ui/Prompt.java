package vg.civcraft.mc.civmodcore.ui;

import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.TextDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Asks a player for one line of text in a dialog. Nothing is shown until open(). Replaces typing the answer into chat,
 * so there's no "type cancel" and no hijacked chat line.
 */
public final class Prompt {

    private static final String INPUT_KEY = "value";

    /** Vanilla's own default is 32, too short for most settings. */
    private static final int DEFAULT_MAX_LENGTH = 256;

    private final Component title;
    private final List<Component> body = new ArrayList<>(1);

    private Component label;
    private String initial = "";
    private int maxLength = DEFAULT_MAX_LENGTH;
    private TextDialogInput.MultilineOptions multiline;
    private Component submitLabel = Component.text("Done");
    private Component cancelLabel = Component.text("Cancel");
    private Function<String, Component> check;
    private BiConsumer<Player, String> onSubmit;
    private Consumer<Player> onCancel;

    Prompt(@NotNull final Object... title) {
        this.title = CivUI.compose(null, Objects.requireNonNull(title, "title cannot be null"));
    }

    /** A line of text above the input. Parts are String or ComponentLike; bare strings are INFO. */
    @NotNull
    public Prompt body(@NotNull final Object... parts) {
        this.body.add(CivUI.compose(CivText.INFO, parts));
        return this;
    }

    /** Shown beside the input. Hidden unless set. */
    @NotNull
    public Prompt label(@NotNull final Object... parts) {
        this.label = CivUI.compose(null, parts);
        return this;
    }

    /** Text already in the box, such as the current value. */
    @NotNull
    public Prompt initial(@Nullable final String initial) {
        this.initial = initial == null ? "" : initial;
        return this;
    }

    /** Enforced by the client as the player types. Defaults to 256. */
    @NotNull
    public Prompt maxLength(final int maxLength) {
        if (maxLength < 1) {
            throw new IllegalArgumentException("maxLength must be at least 1");
        }
        this.maxLength = maxLength;
        return this;
    }

    /** A multi-line box. Either may be null to leave it to the client. */
    @NotNull
    public Prompt multiline(@Nullable final Integer maxLines, @Nullable final Integer height) {
        this.multiline = TextDialogInput.MultilineOptions.create(maxLines, height);
        return this;
    }

    @NotNull
    public Prompt submitLabel(@NotNull final Object... parts) {
        this.submitLabel = CivUI.compose(null, parts);
        return this;
    }

    @NotNull
    public Prompt cancelLabel(@NotNull final Object... parts) {
        this.cancelLabel = CivUI.compose(null, parts);
        return this;
    }

    /**
     * Checked before onSubmit. Input that fails reopens the prompt with the message above it and the player's text
     * still in the box, rather than throwing them out to start again.
     */
    @NotNull
    public Prompt validate(@NotNull final Predicate<String> validator, @NotNull final Object... message) {
        Objects.requireNonNull(validator, "validator cannot be null");
        final Component error = CivUI.compose(CivText.ERROR, message);
        this.check = input -> validator.test(input) ? null : error;
        return this;
    }

    /**
     * Like validate, for input that can be wrong in more than one way: the check returns what's wrong, as a String or
     * ComponentLike, or null if the input is fine. Replaces any earlier validate or check.
     */
    @NotNull
    public Prompt check(@NotNull final Function<String, Object> check) {
        Objects.requireNonNull(check, "check cannot be null");
        this.check = input -> {
            final Object problem = check.apply(input);
            return problem == null ? null : CivUI.compose(CivText.ERROR, problem);
        };
        return this;
    }

    /** Receives the text as typed; it may be empty. Runs on the main thread. */
    @NotNull
    public Prompt onSubmit(@NotNull final BiConsumer<Player, String> onSubmit) {
        this.onSubmit = Objects.requireNonNull(onSubmit, "onSubmit cannot be null");
        return this;
    }

    /** Runs on Cancel or Escape, on the main thread. */
    @NotNull
    public Prompt onCancel(@Nullable final Consumer<Player> onCancel) {
        this.onCancel = onCancel;
        return this;
    }

    /** Throws IllegalStateException if onSubmit was not given. */
    public void open(@NotNull final Player player) {
        if (this.onSubmit == null) {
            throw new IllegalStateException("A prompt needs somewhere to send its answer; call onSubmit(...)");
        }
        show(player, this.initial, null);
    }

    private void show(@NotNull final Player player, @NotNull final String text, @Nullable final Component error) {
        final List<DialogBody> lines = new ArrayList<>(this.body.size() + 1);
        if (error != null) {
            lines.add(DialogBody.plainMessage(error));
        }
        for (final Component line : this.body) {
            lines.add(DialogBody.plainMessage(line));
        }

        // The client rejects a dialog whose initial text is longer than its limit
        final String shown = text.length() > this.maxLength ? text.substring(0, this.maxLength) : text;
        final TextDialogInput.Builder input = DialogInput.text(INPUT_KEY, this.label == null ? Component.empty() : this.label)
            .labelVisible(this.label != null)
            .initial(shown)
            .maxLength(this.maxLength);
        if (this.multiline != null) {
            input.multiline(this.multiline);
        }

        final DialogBase base = DialogBase.builder(this.title)
            .body(lines)
            .inputs(List.of(input.build()))
            .build();

        final AtomicBoolean answered = new AtomicBoolean();
        final ActionButton submit = DialogSupport.button(this.submitLabel, answered, (who, view) -> {
            final String answer = Objects.requireNonNullElse(view.getText(INPUT_KEY), "");
            final Component problem = this.check == null ? null : this.check.apply(answer);
            if (problem != null) {
                show(who, answer, problem);
                return;
            }
            this.onSubmit.accept(who, answer);
        });
        final ActionButton cancel = DialogSupport.button(this.cancelLabel, answered, (who, view) -> {
            if (this.onCancel != null) {
                this.onCancel.accept(who);
            }
        });

        DialogSupport.show(player, DialogSupport.dialog(base, DialogType.confirmation(submit, cancel)));
    }

}
