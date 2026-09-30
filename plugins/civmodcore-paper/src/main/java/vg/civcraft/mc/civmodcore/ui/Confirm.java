package vg.civcraft.mc.civmodcore.ui;

import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Asks a player yes or no in a dialog. Nothing is shown until open(). */
public final class Confirm {

    private final Component title;
    private final List<Component> body = new ArrayList<>(1);

    private Component yesLabel = Component.text("Yes");
    private Component noLabel = Component.text("No");
    private Consumer<Player> onYes;
    private Consumer<Player> onNo;

    Confirm(@NotNull final Object... title) {
        this.title = CivUI.compose(null, Objects.requireNonNull(title, "title cannot be null"));
    }

    /** A line of text above the buttons. Parts are String or ComponentLike; bare strings are INFO. */
    @NotNull
    public Confirm body(@NotNull final Object... parts) {
        this.body.add(CivUI.compose(CivText.INFO, parts));
        return this;
    }

    /** A line in WARN, for what can't be undone. */
    @NotNull
    public Confirm warning(@NotNull final Object... parts) {
        this.body.add(CivUI.compose(CivText.WARN, parts));
        return this;
    }

    @NotNull
    public Confirm yesLabel(@NotNull final Object... parts) {
        this.yesLabel = CivUI.compose(null, parts);
        return this;
    }

    @NotNull
    public Confirm noLabel(@NotNull final Object... parts) {
        this.noLabel = CivUI.compose(null, parts);
        return this;
    }

    /** Runs on the main thread. */
    @NotNull
    public Confirm onYes(@NotNull final Consumer<Player> onYes) {
        this.onYes = Objects.requireNonNull(onYes, "onYes cannot be null");
        return this;
    }

    /** Runs on No or Escape, on the main thread. */
    @NotNull
    public Confirm onNo(@Nullable final Consumer<Player> onNo) {
        this.onNo = onNo;
        return this;
    }

    /** Throws IllegalStateException if onYes was not given. */
    public void open(@NotNull final Player player) {
        if (this.onYes == null) {
            throw new IllegalStateException("A confirmation needs something to confirm; call onYes(...)");
        }
        final List<DialogBody> lines = new ArrayList<>(this.body.size());
        for (final Component line : this.body) {
            lines.add(DialogBody.plainMessage(line));
        }
        final DialogBase base = DialogBase.builder(this.title)
            .body(lines)
            .build();

        final AtomicBoolean answered = new AtomicBoolean();
        final ActionButton yes = DialogSupport.button(this.yesLabel, answered, (who, view) -> this.onYes.accept(who));
        final ActionButton no = DialogSupport.button(this.noLabel, answered, (who, view) -> {
            if (this.onNo != null) {
                this.onNo.accept(who);
            }
        });

        DialogSupport.show(player, DialogSupport.dialog(base, DialogType.confirmation(yes, no)));
    }

}
