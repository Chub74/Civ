package com.devotedmc.ExilePearl.command;

import com.devotedmc.ExilePearl.ExilePearl;
import com.devotedmc.ExilePearl.ExilePearlApi;
import com.programmerdan.minecraft.banstick.handler.BanHandler;
import io.papermc.paper.datacomponent.DataComponentTypes;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.apache.commons.collections4.ComparatorUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import vg.civcraft.mc.civmodcore.CivModCorePlugin;
import vg.civcraft.mc.civmodcore.inventory.gui.Clickable;
import vg.civcraft.mc.civmodcore.inventory.gui.ClickableInventory;
import vg.civcraft.mc.civmodcore.inventory.gui.DecorationStack;
import vg.civcraft.mc.civmodcore.inventory.gui.IClickable;
import vg.civcraft.mc.civmodcore.inventory.gui.MultiPageView;
import vg.civcraft.mc.civmodcore.inventory.items.ItemUtils;
import vg.civcraft.mc.civmodcore.ui.Channel;
import vg.civcraft.mc.civmodcore.ui.CivMenu;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.civmodcore.ui.Kind;
import vg.civcraft.mc.civmodcore.utilities.MoreCollectionUtils;
import vg.civcraft.mc.civmodcore.world.WorldUtils;

public class CmdShowAllPearls extends PearlCommand {

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd MMM yyyy");
    private static final Map<UUID, Long> COOLDOWNS = new HashMap<>();
    private static final long COOLDOWN = 2_000;
    private static final Set<UUID> TOGGLES = new HashSet<>();

    public CmdShowAllPearls(final ExilePearlApi pearlApi) {
        super(pearlApi);
        this.aliases.add("showall");
        setHelpShort("Lists all pearled players");

        this.senderMustBePlayer = true;
    }

    @Override
    public void perform() {
        final Player sender = player();
        if (onCoolDown(sender)) return;
        generateOpenPearlsMenu(sender);
    }

    private boolean onCoolDown(Player sender) {
        final long now = System.currentTimeMillis();
        final long previousUseTime = COOLDOWNS.compute(sender.getUniqueId(),
            (uuid, value) -> value == null ? 0L : value); // This is better than getOrDefault()
        if (previousUseTime > (now - COOLDOWN)) {
            CivUI.notice(Kind.ERROR, "Wait a moment before doing that again").to(sender).on(Channel.ACTION_BAR).send();
            return true;
        }
        COOLDOWNS.put(sender.getUniqueId(), now);
        return false;
    }

    private void generateOpenPearlsMenu(Player sender) {
        final boolean bannedPearlToggle = TOGGLES.contains(sender.getUniqueId());
        final Location senderLocation = sender.getLocation();
        final double pearlExclusionRadius = this.plugin.getPearlConfig().getRulePearlRadius() * 1.2;
        final boolean isBanStickEnabled = this.plugin.isBanStickEnabled();

        final List<Supplier<IClickable>> contentSuppliers = this.plugin.getPearls().stream()
            // Sort pearls from newest to oldest
            .sorted(ComparatorUtils.reversedComparator(Comparator.comparing(ExilePearl::getPearledOn)))
            .filter((pearl) -> {
                if (!bannedPearlToggle) {
                    final boolean isPlayerBanned = isBanStickEnabled
                        && BanHandler.isPlayerBanned(pearl.getPlayerId());

                    return !isPlayerBanned;
                } else return true;
            })
            .<Supplier<IClickable>>map((pearl) -> () -> {
                final Location pearlLocation = pearl.getLocation();
                final boolean isPlayerBanned = isBanStickEnabled
                    && BanHandler.isPlayerBanned(pearl.getPlayerId());

                final int distanceToPearl = WorldUtils.blockDistance(senderLocation, pearlLocation, true);
                // distance is -1 if the pearl is in a different world
                final boolean showLocation = distanceToPearl != -1 && distanceToPearl <= pearlExclusionRadius;

                CompletableFuture<ItemStack> itemReadyFuture = new CompletableFuture<>();
                final ItemStack item = isPlayerBanned
                    ? new ItemStack(Material.ARMOR_STAND)
                    : CivModCorePlugin.getInstance().getSkinCache().getHeadItem(
                    pearl.getPlayerId(),
                    () -> new ItemStack(Material.ENDER_PEARL),
                    itemReadyFuture::complete);

                Consumer<ItemStack> itemMetaMod = itemToMod ->
                    ItemUtils.handleItemMeta(itemToMod, (ItemMeta meta) -> {
                        // A custom name, not ITEM_NAME: player heads name themselves, and that beats ITEM_NAME
                        meta.displayName(Component.text()
                            .decoration(TextDecoration.ITALIC, false)
                            .append(CivText.subject(pearl.getPlayerName()))
                            .append(isPlayerBanned ? Component.text(" (banned)", CivText.ERROR) : Component.empty())
                            .build());
                        return true;
                    });
                Consumer<ItemStack> itemLoreMod = itemToMod -> {
                    itemToMod.unsetData(DataComponentTypes.LORE);
                    CivMenu.lore(itemToMod,
                        CivMenu.stat("Type", pearl.getItemName()),
                        CivMenu.stat("Pearl", Component.text(Integer.toString(pearl.getPearlId(), 36).toUpperCase(), CivText.MUTED)),
                        CivMenu.stat("Pearled", DATE_FORMAT.format(pearl.getPearledOn())),
                        CivMenu.stat("Killed by", CivText.subject(pearl.getKillerName())));
                    if (showLocation) {
                        CivMenu.lore(itemToMod,
                            CivMenu.stat("Location", pearlLocation.getBlockX() + " " + pearlLocation.getBlockY() + " "
                                + pearlLocation.getBlockZ() + " " + pearlLocation.getWorld().getName()),
                            Component.empty(),
                            CivMenu.hint("Click to get a waypoint"));
                    }
                };

                itemMetaMod.accept(item);
                itemLoreMod.accept(item);

                return new Clickable(item) {
                    @Override
                    protected void clicked(final Player clicker) {
                        if (showLocation) {
                            final var location = pearl.getLocation();
                            if (!Objects.equals(location.getWorld(), clicker.getWorld())) {
                                CivUI.error(clicker, "That pearl is in another world");
                                return;
                            }
                            // Not through CivUI: waypoint mods only pick this up as a bare line
                            clicker.sendMessage('['
                                + "name:" + pearl.getPlayerName() + "'s pearl,"
                                + "x:" + location.getBlockX() + ','
                                + "y:" + location.getBlockY() + ','
                                + "z:" + location.getBlockZ()
                                + ']');
                        }
                    }

                    @Override
                    public void addedToInventory(ClickableInventory inv, int slot) {
                        itemReadyFuture.thenAccept(item -> {
                            itemMetaMod.accept(item);
                            itemLoreMod.accept(item);
                            this.item = item;
                            inv.setItem(item, slot);
                        });
                        super.addedToInventory(inv, slot);
                    }
                };
            })
            .collect(Collectors.toCollection(ArrayList::new));

        if (contentSuppliers.isEmpty()) {
            final var item = new ItemStack(Material.BARRIER);
            CivMenu.name(item, "No pearls");
            CivMenu.lore(item, Component.text("Nobody is pearled right now"));
            contentSuppliers.add(() -> new DecorationStack(item));
        }

        List<IClickable> lazyContents = MoreCollectionUtils.asLazyList(contentSuppliers);
        final var pageView = new MultiPageView(sender, lazyContents, CivMenu.title("All Pearls"), true);

        pageView.setMenuSlot(constructBannedPearlsToggleClick(bannedPearlToggle), 4);
        pageView.showScreen();
    }

    private IClickable constructBannedPearlsToggleClick(final boolean bannedPearlToggle) {
        final var item = new ItemStack(Material.BARRIER);
        CivMenu.name(item, "Banned pearls");
        CivMenu.lore(item,
            CivMenu.stat("Shown", CivText.toggle(bannedPearlToggle)),
            CivMenu.hint("Click to " + (bannedPearlToggle ? "hide" : "show") + " pearls of banned players"));
        return new Clickable(item) {
            @Override
            public void clicked(final Player clicker) {
                if (onCoolDown(clicker)) return;
                if (!bannedPearlToggle) {
                    TOGGLES.add(clicker.getUniqueId());
                } else {
                    TOGGLES.remove(clicker.getUniqueId());
                }
                CivUI.success(clicker, "Banned pearls ", CivText.toggle(!bannedPearlToggle));
                generateOpenPearlsMenu(clicker);
            }
        };
    }
}
