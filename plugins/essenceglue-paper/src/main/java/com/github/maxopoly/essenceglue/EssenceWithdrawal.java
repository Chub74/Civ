package com.github.maxopoly.essenceglue;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import vg.civcraft.mc.civmodcore.inventory.items.ItemMap;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;

/** Turns stored essence into items, directly or through a dialog with an amount slider. */
public class EssenceWithdrawal {

    private static final String AMOUNT_KEY = "amount";

    private static final ClickCallback.Options ONE_SHOT = ClickCallback.Options.builder()
        .uses(1)
        .lifetime(Duration.ofMinutes(30L))
        .build();

    private final EssenceGluePlugin plugin;
    private final VirtualEssenceManager manager;
    private final ItemMap essence;

    public EssenceWithdrawal(EssenceGluePlugin plugin, VirtualEssenceManager manager, ItemMap essence) {
        this.plugin = plugin;
        this.manager = manager;
        this.essence = essence;
    }

    /** Shows the withdraw dialog, or says in chat why there's nothing to withdraw. */
    public void openDialog(Player player) {
        int stored = manager.getEssence(player.getUniqueId());
        if (stored <= 0) {
            CivUI.error(player, "You don't have any essence to withdraw");
            return;
        }
        int room = mostThatFits(player.getInventory(), stored);
        if (room == 0) {
            CivUI.error(player, "Your inventory is full. Make room to withdraw essence");
            return;
        }

        List<DialogBody> body = new ArrayList<>(2);
        Component summary = Component.text()
            .append(CivText.info("You have "))
            .append(EssenceText.amount(stored))
            .append(CivText.info(" of "))
            .append(EssenceText.amount(manager.getCap()))
            .append(CivText.info(" essence stored"))
            .build();
        ItemStack icon = icon();
        body.add(icon == null
            ? DialogBody.plainMessage(summary)
            : DialogBody.item(icon).description(DialogBody.plainMessage(summary)).showDecorations(false).build());
        if (room < stored) {
            body.add(DialogBody.plainMessage(Component.text()
                .append(Component.text("Your inventory only has room for ", CivText.WARN))
                .append(EssenceText.amount(room))
                .build()));
        }

        DialogBase.Builder base = DialogBase.builder(CivText.heading("Withdraw Essence"))
            .canCloseWithEscape(true)
            .body(body);
        // A slider needs two ends, so with room for just one there's nothing to choose
        if (room > 1) {
            base.inputs(List.of(DialogInput.numberRange(AMOUNT_KEY, CivText.info("Amount"), 1f, room)
                .step(1f)
                .initial((float) room)
                .build()));
        }

        ActionButton withdraw = ActionButton.builder(Component.text("Withdraw", CivText.SUCCESS))
            .action(DialogAction.customClick((view, audience) -> {
                if (!(audience instanceof Player who)) {
                    return;
                }
                Float chosen = view.getFloat(AMOUNT_KEY);
                int amount = chosen == null ? room : Math.round(chosen);
                onMain(() -> withdraw(who, amount));
            }, ONE_SHOT))
            .build();
        ActionButton cancel = ActionButton.builder(Component.text("Cancel")).build();

        player.showDialog(Dialog.create(factory -> factory.empty()
            .base(base.build())
            .type(DialogType.confirmation(withdraw, cancel))));
    }

    /**
     * Withdraws up to the amount, as much as fits, and tells the player how it went. Checks everything again, since
     * the balance or inventory may have changed while a dialog was open.
     */
    public void withdraw(Player player, int amount) {
        UUID playerId = player.getUniqueId();
        int stored = manager.getEssence(playerId);
        if (stored <= 0) {
            CivUI.error(player, "You don't have any essence to withdraw");
            return;
        }
        if (amount <= 0) {
            CivUI.error(player, "You can only withdraw a positive amount of essence");
            return;
        }
        if (amount > stored) {
            CivUI.error(player, "You only have ", EssenceText.amount(stored), " essence stored");
            return;
        }
        int withdrawn = mostThatFits(player.getInventory(), amount);
        if (withdrawn == 0) {
            CivUI.error(player, "Your inventory is full. Make room to withdraw essence");
            return;
        }

        manager.removeEssence(playerId, withdrawn);
        for (ItemStack item : scaled(withdrawn).getItemStackRepresentation()) {
            // fitsIn already said there's room, so this only catches it being wrong
            for (ItemStack leftover : player.getInventory().addItem(item).values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }
        }

        int left = stored - withdrawn;
        if (withdrawn < amount) {
            CivUI.success(player, "You withdrew ", EssenceText.amount(withdrawn), " essence. ",
                EssenceText.amount(amount - withdrawn), " didn't fit in your inventory, so it's still stored");
        } else if (left > 0) {
            CivUI.success(player, "You withdrew ", EssenceText.amount(withdrawn), " essence, ",
                EssenceText.amount(left), " is still stored");
        } else {
            CivUI.success(player, "You withdrew ", EssenceText.amount(withdrawn), " essence");
        }
    }

    /** The most essence, up to wanted, whose items fit in the inventory. */
    private int mostThatFits(Inventory inventory, int wanted) {
        int low = 0;
        int high = wanted;
        while (low < high) {
            int mid = (low + high + 1) >>> 1;
            if (scaled(mid).fitsIn(inventory)) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }
        return low;
    }

    private ItemMap scaled(int amount) {
        ItemMap items = essence.clone();
        items.multiplyContent(amount);
        return items;
    }

    /** One essence item, to show in the dialog. */
    private ItemStack icon() {
        List<ItemStack> items = essence.getItemStackRepresentation();
        if (items.isEmpty()) {
            return null;
        }
        ItemStack icon = items.getFirst().clone();
        icon.setAmount(1);
        return icon;
    }

    private void onMain(Runnable task) {
        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }
}
