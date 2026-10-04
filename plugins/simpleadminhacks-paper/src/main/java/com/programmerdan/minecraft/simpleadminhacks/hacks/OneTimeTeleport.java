package com.programmerdan.minecraft.simpleadminhacks.hacks;

import vg.civcraft.mc.civmodcore.ui.Kind;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.BukkitCommandCompletionContext;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Flags;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import com.devotedmc.ExilePearl.ExilePearlPlugin;
import com.google.common.collect.Lists;
import com.programmerdan.minecraft.simpleadminhacks.SimpleAdminHacks;
import com.programmerdan.minecraft.simpleadminhacks.configs.OneTimeTeleportConfig;
import com.programmerdan.minecraft.simpleadminhacks.framework.SimpleHack;
import isaac.bastion.Bastion;
import isaac.bastion.BastionBlock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minelink.ctplus.CombatTagPlus;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import vg.civcraft.mc.civmodcore.commands.TabComplete;
import vg.civcraft.mc.civmodcore.players.settings.PlayerSetting;
import vg.civcraft.mc.civmodcore.players.settings.PlayerSettingAPI;
import vg.civcraft.mc.civmodcore.players.settings.impl.LongSetting;
import vg.civcraft.mc.civmodcore.utilities.TextUtil;
import vg.civcraft.mc.civmodcore.world.WorldUtils;

public final class OneTimeTeleport extends SimpleHack<OneTimeTeleportConfig> implements Listener {

    public OneTimeTeleport(
        final @NotNull SimpleAdminHacks plugin,
        final @NotNull OneTimeTeleportConfig config
    ) {
        super(plugin, config);
    }

    public static @NotNull OneTimeTeleportConfig generate(
        final @NotNull SimpleAdminHacks plugin,
        final @NotNull ConfigurationSection config
    ) {
        return new OneTimeTeleportConfig(plugin, config);
    }

    @Override
    public void onEnable() {
        // Since there's no way to unregister settings without some gnarly reflection, this will only register settings
        // that aren't already registered. Also, MoreCollectionUtils.getMissing() when?
        final Collection<PlayerSetting<?>> allSettings = PlayerSettingAPI.getAllSettings();
        final Collection<PlayerSetting<?>> ourSettings = Lists.newArrayList(this.grantedTimestamps);
        ourSettings.removeIf(allSettings::contains);
        ourSettings.forEach((setting) -> PlayerSettingAPI.registerSetting(setting, null));

        plugin().registerListener(this);
        plugin().getCommands().registerCommand(this.commands);
    }

    @Override
    public void onDisable() {
        plugin().getCommands().unregisterCommand(this.commands);
        HandlerList.unregisterAll(this);
    }

    private final Map<UUID, UUID> senderToReceiver = new HashMap<>();

    private static final long OTT_AVAILABLE = -1L;
    private static final long OTT_UNAVAILABLE = -2L;
    private final LongSetting grantedTimestamps = new LongSetting(
        plugin(),
        OTT_AVAILABLE,
        "Time since OTT granted",
        "timeSinceOTTGrant"
    );

    private final OTTCommand commands = new OTTCommand();

    @CommandAlias("ott")
    private final class OTTCommand extends BaseCommand {

        @Default
        public void defaultCommand(
            final Player sender
        ) {
            if (checkOTT(sender.getUniqueId())) {
                final long expiresIn = config().getTimeLimitOnUsageInMillis() - (System.currentTimeMillis() - OneTimeTeleport.this.grantedTimestamps.getValue(sender.getUniqueId()));
                CivUI.notice(Kind.INFO, "Your one-time teleport expires in ", CivText.value(TextUtil.formatDuration(expiresIn)),
                        ". Click to request one")
                    .to(sender)
                    .hover(CivText.info("Suggests /ott to <player>"))
                    .click(ClickEvent.suggestCommand("/ott to "))
                    .send();
                return;
            }
            CivUI.error(sender, "You don't have a one-time teleport");
        }

        @Subcommand("to|request")
        @Description("Makes an OTT request to a player. If accepted, you'll teleport to them.")
        @Syntax("<player>")
        public void requestOTT(
            final Player sender,
            @Flags("other") final Player targetPlayer
        ) {
            if (targetPlayer == sender) {
                CivUI.error(sender, "You can't teleport to yourself");
                return;
            }

            switch (testPermissibility(sender, targetPlayer)) {
                case OK -> {
                }
                case FAIL_NO_OTT -> {
                    CivUI.error(sender, "Your one-time teleport has expired");
                    return;
                }
                case FAIL_IN_COMBAT -> {
                    CivUI.error(sender, "You can't teleport while in combat");
                    return;
                }
                case FAIL_IS_PEARLED -> {
                    CivUI.error(sender, "You can't teleport while pearled");
                    return;
                }
                case FAIL_DIFFERENT_WORLD -> {
                    CivUI.error(sender, "You can't teleport to another world");
                    return;
                }
                case FAIL_TARGET_IS_PEARLED -> {
                    CivUI.error(sender, "You can't teleport to a pearled player");
                    return;
                }
            }

            final UUID previousRequest = OneTimeTeleport.this.senderToReceiver.put(
                sender.getUniqueId(),
                targetPlayer.getUniqueId()
            );

            if (previousRequest != null) {
                final Player previousTargetPlayer = Bukkit.getPlayer(previousRequest);
                if (previousTargetPlayer != null) {
                    CivUI.info(previousTargetPlayer, CivText.subject(sender.getName()), " withdrew their teleport request");
                }
            }

            CivUI.success(sender, "Asked to teleport to ", CivText.subject(targetPlayer.getName()));

            final String commandStr = "/ott accept " + sender.getName();
            CivUI.notice(Kind.INFO, CivText.subject(sender.getName()), " wants to teleport to you. Click or type ",
                    CivText.value(commandStr), " to accept")
                .to(targetPlayer)
                .hover(CivText.info("Suggests " + commandStr))
                .click(ClickEvent.suggestCommand(commandStr))
                .send();
        }

        @Subcommand("revoke|rescind|stop")
        @Description("Revokes your OTT request.")
        public void rescindRequest(
            final Player sender
        ) {
            final UUID targetUUID = OneTimeTeleport.this.senderToReceiver.remove(sender.getUniqueId());
            if (targetUUID == null) {
                CivUI.error(sender, "You have no teleport request open");
                return;
            }
            CivUI.success(sender, "Withdrew your teleport request");
            final Player targetPlayer = Bukkit.getPlayer(targetUUID);
            if (targetPlayer != null) {
                CivUI.info(targetPlayer, CivText.subject(sender.getName()), " withdrew their teleport request");
            }
        }

        @TabComplete("sah_ott_requests")
        public List<String> getRequestsToMe(
            final @NotNull BukkitCommandCompletionContext context
        ) {
            final UUID targetPlayerUUID = context.getPlayer().getUniqueId();
            return OneTimeTeleport.this.senderToReceiver.entrySet()
                .stream()
                .filter((entry) -> Objects.equals(targetPlayerUUID, entry.getValue()))
                .map((entry) -> Bukkit.getPlayer(entry.getKey()))
                .filter(Objects::nonNull)
                .map(Player::getName)
                .toList();
        }

        @Subcommand("accept|approve|allow|yes")
        @CommandCompletion("@sah_ott_requests")
        @Description("Accepts an OTT request to your location.")
        @Syntax("<requester>")
        public void acceptOTT(
            final Player sender,
            @Flags("other") final Player requestingPlayer
        ) {
            if (requestingPlayer == sender) {
                CivUI.error(sender, "You can't accept your own request");
                return;
            }

            if (!OneTimeTeleport.this.senderToReceiver.remove(
                requestingPlayer.getUniqueId(),
                sender.getUniqueId()
            )) {
                CivUI.error(sender, CivText.subject(requestingPlayer.getName()), " hasn't asked to teleport to you");
                return;
            }

            switch (testPermissibility(requestingPlayer, sender)) {
                case OK -> {
                }
                case FAIL_NO_OTT -> {
                    CivUI.error(sender, CivText.subject(requestingPlayer.getName()), "'s one-time teleport has expired");
                    CivUI.error(requestingPlayer, "Your one-time teleport has expired");
                    return;
                }
                case FAIL_IN_COMBAT -> {
                    CivUI.error(sender, CivText.subject(requestingPlayer.getName()), " can't teleport while in combat");
                    CivUI.error(requestingPlayer, CivText.subject(sender.getName()), " accepted, but you're in combat");
                    OneTimeTeleport.this.senderToReceiver.put(requestingPlayer.getUniqueId(), sender.getUniqueId()); // Be kind and put the request back!
                    return;
                }
                case FAIL_IS_PEARLED -> {
                    CivUI.error(sender, CivText.subject(requestingPlayer.getName()), " can't teleport while pearled");
                    CivUI.error(requestingPlayer, CivText.subject(sender.getName()), " accepted, but you're pearled");
                    return;
                }
                case FAIL_DIFFERENT_WORLD -> {
                    CivUI.error(sender, CivText.subject(requestingPlayer.getName()), " is in another world");
                    CivUI.error(requestingPlayer, CivText.subject(sender.getName()), " accepted, but you're in another world");
                    return;
                }
                case FAIL_TARGET_IS_PEARLED -> {
                    CivUI.error(sender, CivText.subject(requestingPlayer.getName()), " can't teleport to you while you're pearled");
                    CivUI.error(requestingPlayer, CivText.subject(sender.getName()), " accepted, but they're pearled");
                    return;
                }
            }

            if (Bukkit.getPluginManager().isPluginEnabled("Bastion")) {
                for (final BastionBlock bastion : Bastion.getBastionManager().getBlockingBastions(sender.getLocation())) {
                    if (!bastion.canPlace(sender) || !bastion.canPlace(requestingPlayer)) {
                        CivUI.error(sender, CivText.subject(requestingPlayer.getName()), " can't teleport into a hostile bastion");
                        CivUI.error(requestingPlayer, CivText.subject(sender.getName()), " accepted, but they're in a hostile bastion");
                        OneTimeTeleport.this.senderToReceiver.put(requestingPlayer.getUniqueId(), sender.getUniqueId()); // Be kind and put the request back!
                        return;
                    }
                }
            }

            for (final Block block : getNearbyBlocks(sender.getLocation().getBlock(), 8)) {
                if (config().getUnsafeMaterials().contains(block.getType())) {
                    CivUI.error(sender, CivText.subject(requestingPlayer.getName()), " can't teleport here, there are unsafe blocks nearby");
                    CivUI.error(requestingPlayer, CivText.subject(sender.getName()), " accepted, but there are unsafe blocks near them");
                    OneTimeTeleport.this.senderToReceiver.put(requestingPlayer.getUniqueId(), sender.getUniqueId()); // Be kind and put the request back!
                    return;
                }
            }

            // Remove any blacklisted materials from the player's inventory
            CivUI.warn(requestingPlayer, "Items that can't be teleported were removed from your inventory");
            final Inventory requestingPlayerInventory = requestingPlayer.getInventory();
            config().getMaterialBlacklist().forEach(requestingPlayerInventory::remove);

            OneTimeTeleport.this.grantedTimestamps.setValue(requestingPlayer.getUniqueId(), OTT_UNAVAILABLE);

            OneTimeTeleport.this.logger.info("Player[" + requestingPlayer.getName() + "] has OTT-teleported from [" + WorldUtils.getBlockLocation(requestingPlayer.getLocation()) + "] to [" + sender.getName() + "] at [" + WorldUtils.getBlockLocation(sender.getLocation()) + "]");
            requestingPlayer.teleport(sender.getLocation());
            CivUI.success(sender, CivText.subject(requestingPlayer.getName()), " teleported to you");
            CivUI.success(requestingPlayer, "Teleported to ", CivText.subject(sender.getName()));
        }

        @Subcommand("reject|refuse|deny|no")
        @CommandCompletion("@sah_ott_requests")
        @Description("Rejects an OTT to your location.")
        @Syntax("<requester>")
        public void rejectOTT(
            final Player sender,
            @Flags("other") final Player requestingPlayer
        ) {
            if (requestingPlayer == sender) {
                CivUI.error(sender, "You can't reject your own request");
                return;
            }

            if (!OneTimeTeleport.this.senderToReceiver.remove(
                requestingPlayer.getUniqueId(),
                sender.getUniqueId()
            )) {
                CivUI.error(sender, CivText.subject(requestingPlayer.getName()), " hasn't asked to teleport to you");
                return;
            }
            CivUI.success(sender, "Rejected ", CivText.subject(requestingPlayer.getName()), "'s teleport request");
            CivUI.error(requestingPlayer, CivText.subject(sender.getName()), " rejected your teleport request");
        }

        @Subcommand("grant|bestow|give")
        @CommandPermission("simpleadmin.grantott")
        @CommandCompletion("@allplayers")
        @Description("Gives a player a use of OTT as if they first joined.")
        @Syntax("<recipient>")
        public void grantOTT(
            final CommandSender sender,
            final String receivingPlayerName
        ) {
            final OfflinePlayer receivingPlayer = Bukkit.getOfflinePlayerIfCached(receivingPlayerName);
            if (receivingPlayer == null) {
                sender.sendMessage(Component.text("Could not find player " + receivingPlayerName + "!", NamedTextColor.RED));
                return;
            }
            OneTimeTeleport.this.grantedTimestamps.setValue(receivingPlayer.getUniqueId(), System.currentTimeMillis());
            sender.sendMessage(Component.text("You have granted " + receivingPlayer.getName() + " an OTT!", NamedTextColor.GREEN));
            final Player onlineReceivingPlayer = receivingPlayer.getPlayer();
            if (onlineReceivingPlayer != null) {
                CivUI.success(onlineReceivingPlayer, "You've been given a one-time teleport. Use ", CivText.value("/ott to <player>"));
            }
            OneTimeTeleport.this.logger.info(sender.getName() + " has granted " + receivingPlayer.getName() + " an OTT!");
        }
    }

    ;

    @EventHandler
    public void onFirstJoin(final PlayerJoinEvent event) {
        checkOTT(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(final PlayerQuitEvent event) {
        final Player whoQuit = event.getPlayer();
        // Remove all pending OTT's involving this player
        this.senderToReceiver.entrySet().removeIf((entry) -> {
            final UUID requesterUUID = entry.getKey(), targetUUID = entry.getValue();

            // The player was the target of OTT requests
            if (Objects.equals(whoQuit.getUniqueId(), targetUUID)) {
                final Player requesterPlayer = Bukkit.getPlayer(requesterUUID);
                if (requesterPlayer != null) { // Just in case
                    CivUI.info(requesterPlayer, "Your teleport request to ", CivText.subject(whoQuit.getName()), " expired because they logged off");
                }
                return true;
            }

            // The player had made an OTT request
            if (Objects.equals(whoQuit.getUniqueId(), requesterUUID)) {
                final Player targetPlayer = Bukkit.getPlayer(targetUUID);
                if (targetPlayer != null) { // Just in case
                    CivUI.info(targetPlayer, CivText.subject(whoQuit.getName()), "'s teleport request expired because they logged off");
                }
            }
            return false;
        });
    }

    /**
     * All possible permissible states that requesting and accepting share!
     */
    private enum OttPermissible {OK, FAIL_NO_OTT, FAIL_IN_COMBAT, FAIL_IS_PEARLED, FAIL_DIFFERENT_WORLD, FAIL_TARGET_IS_PEARLED}

    private @NotNull OttPermissible testPermissibility(
        final @NotNull Player requestingPlayer,
        final @NotNull Player destinationPlayer
    ) {
        if (!checkOTT(requestingPlayer.getUniqueId())) {
            return OttPermissible.FAIL_NO_OTT;
        }
        if (Bukkit.getPluginManager().isPluginEnabled("CombatTagPlus")) {
            final CombatTagPlus combatTagPlus = JavaPlugin.getPlugin(CombatTagPlus.class);
            if (combatTagPlus.getTagManager().isTagged(requestingPlayer.getUniqueId())) {
                return OttPermissible.FAIL_IN_COMBAT;
            }
        }
        if (Bukkit.getPluginManager().isPluginEnabled("ExilePearl")) {
            if (ExilePearlPlugin.getApi().getPearlManager().getPearl(requestingPlayer.getUniqueId()) != null) {
                return OttPermissible.FAIL_IS_PEARLED;
            }

            if(ExilePearlPlugin.getApi().getPearlManager().getPearl(destinationPlayer.getUniqueId()) != null) {
                return OttPermissible.FAIL_TARGET_IS_PEARLED;
            }
        }
        if (config().isLimitingToSameWorld()) {
            if (!Objects.equals(requestingPlayer.getWorld(), destinationPlayer.getWorld())) {
                return OttPermissible.FAIL_DIFFERENT_WORLD;
            }
        }
        return OttPermissible.OK;
    }

    private @NotNull List<Block> getNearbyBlocks(
        final @NotNull Block start,
        final int radius
    ) {
        if (radius <= 0) {
            return new ArrayList<>(0);
        }
        final int iterations = (radius * 2) + 1;
        final var blocks = new ArrayList<Block>(iterations * iterations * iterations);
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    blocks.add(start.getRelative(x, y, z));
                }
            }
        }
        return blocks;
    }

    private boolean checkOTT(
        final @NotNull UUID uuid
    ) {
        final long grantedTimestamp = this.grantedTimestamps.getValue(uuid);
        if (grantedTimestamp == OTT_UNAVAILABLE) {
            return false;
        }
        if (grantedTimestamp == OTT_AVAILABLE) {
            this.grantedTimestamps.setValue(uuid, System.currentTimeMillis());
            return true;
        }
        if (System.currentTimeMillis() >= (grantedTimestamp + config().getTimeLimitOnUsageInMillis())) {
            this.grantedTimestamps.setValue(uuid, OTT_UNAVAILABLE);
            return false;
        }
        return true;
    }
}
