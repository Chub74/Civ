package vg.civcraft.mc.civmodcore.ui;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import vg.civcraft.mc.civmodcore.CivModCorePlugin;

/** Shared plumbing for Prompt and Confirm. */
final class DialogSupport {

    /** How long an unanswered dialog's buttons keep working. */
    private static final Duration LIFETIME = Duration.ofMinutes(30L);

    private static final ClickCallback.Options ONE_SHOT = ClickCallback.Options.builder()
        .uses(1)
        .lifetime(LIFETIME)
        .build();

    private DialogSupport() {
    }

    @NotNull
    static Dialog dialog(@NotNull final DialogBase base, @NotNull final DialogType type) {
        return Dialog.create(factory -> factory.empty().base(base).type(type));
    }

    /**
     * A button whose handler runs at most once across every button sharing the same flag, so a dialog can't be
     * answered twice. Escape runs the dialog's exit button, which is how cancel handlers see it being closed.
     */
    @NotNull
    static ActionButton button(@NotNull final Component label,
                               @NotNull final AtomicBoolean answered,
                               @NotNull final BiConsumer<Player, DialogResponseView> handler) {
        final DialogAction action = DialogAction.customClick((view, audience) -> {
            if (!(audience instanceof final Player player) || !answered.compareAndSet(false, true)) {
                return;
            }
            onMain(() -> handler.accept(player, view));
        }, ONE_SHOT);
        return ActionButton.builder(label).action(action).build();
    }

    /**
     * Shows on the next tick, after closing whatever inventory is open. Opening straight from an inventory click
     * would leave the server thinking the container is still open behind the dialog.
     */
    static void show(@NotNull final Player player, @NotNull final Dialog dialog) {
        Bukkit.getScheduler().runTask(CivModCorePlugin.getInstance(), () -> {
            if (!player.isOnline()) {
                return;
            }
            player.closeInventory();
            player.showDialog(dialog);
        });
    }

    private static void onMain(@NotNull final Runnable task) {
        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            Bukkit.getScheduler().runTask(CivModCorePlugin.getInstance(), task);
        }
    }

}
