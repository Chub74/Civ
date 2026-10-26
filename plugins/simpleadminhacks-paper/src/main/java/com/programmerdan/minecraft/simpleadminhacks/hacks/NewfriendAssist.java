package com.programmerdan.minecraft.simpleadminhacks.hacks;

import com.programmerdan.minecraft.simpleadminhacks.SimpleAdminHacks;
import com.programmerdan.minecraft.simpleadminhacks.configs.NewfriendAssistConfig;
import com.programmerdan.minecraft.simpleadminhacks.framework.SimpleHack;
import com.programmerdan.minecraft.simpleadminhacks.framework.utilities.BroadcastLevel;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import vg.civcraft.mc.civmodcore.ui.Bedrock;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.civmodcore.ui.Kind;

/**
 * A simple Newfriend tracker and assist module. Keeps track of newfriends so far.
 *
 * @author ProgrammerDan
 */
public class NewfriendAssist extends SimpleHack<NewfriendAssistConfig> implements Listener, CommandExecutor {

    public static final String NAME = "NewfriendAssist";
    private static long newfriendCount = 0l;
    /**
     * We could use the various Bukkit methods every time we want to peak at our data, but the cost of storing it is low
     * and this is a hack, after all..
     */
    private HashMap<UUID, String> newfriendNames;
    private HashMap<UUID, SessionTime> newfriendSessionTime;

    public NewfriendAssist(SimpleAdminHacks plugin, NewfriendAssistConfig config) {
        super(plugin, config);
    }

    /**
     * Track standard quit events to monitor newfriend playtime on the day of join.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void newLeaveEvent(PlayerQuitEvent exit) {
        if (!config.isEnabled()) return;
        doLeave(exit.getPlayer());
    }

    /**
     * Track standard kick events to monitor newfriend playtime on the day of join.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void newKickEvent(PlayerKickEvent exit) {
        if (!config.isEnabled()) return;
        doLeave(exit.getPlayer());
    }

    private void doLeave(Player departed) {
        if (departed == null) return;

        UUID depUUID = departed.getUniqueId();
        if (newfriendSessionTime.containsKey(depUUID)) {
            newfriendSessionTime.get(depUUID).endSession(System.currentTimeMillis());
        }
    }

    /**
     * Track join events to keep track of session time for previous newfriends and
     * to announce the presence of new newfriends, if configured.
     * <p>
     * Monitoring is always on and is not configurable.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void NewJoinEvent(PlayerJoinEvent join) {
        if (!config.isEnabled()) return;
        Player newfriend = join.getPlayer();
        if (newfriend == null) return;

        UUID newUUID = newfriend.getUniqueId();
        if (newfriendSessionTime.containsKey(newUUID)) {
            newfriendSessionTime.get(newUUID).startSession(System.currentTimeMillis());
        }

        if (newfriend.hasPlayedBefore()) return;

        NewfriendAssist.newfriendCount++;

        newfriendNames.put(newUUID, newfriend.getName());
        newfriendSessionTime.put(newUUID, new SessionTime(System.currentTimeMillis()));

        if (!config.getAnnounceBroadcast().isEmpty()) {
            announceNewfriend(newfriend);
        }

        if (config.isIntroKitEnabled()) {
            ItemStack[] introKit = config.getIntroKit();
            if (introKit != null && introKit.length > 0) {
                Inventory inv = newfriend.getInventory();
                inv.addItem(introKit);
                plugin().log(Level.INFO, "  Gave newbit kit to {0}", newfriend.getDisplayName());
            }
        }

    }

    /**
     * Expanding the usage of this class to allow us to give RandomSpawning players a kit on each respawn.
     * This method checks if the respawn event is a bed or anchor respawn and if not, gives them the introkit.
     *
     * @param event PlayerRespawnEvent
     */

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onRespawn(PlayerRespawnEvent event) {
        if (!config.giveToRandomSpawners()) {
            return;
        }
        if (event.isAnchorSpawn()) {
            return;
        }
        if (event.isBedSpawn()) {
            return;
        }
        //Checking here if the player hasn't played before so we don't double spawn a starter kit
        if (!event.getPlayer().hasPlayedBefore()) {
            return;
        }
        if (config.isIntroKitEnabled()) {
            ItemStack[] introKit = config.getIntroKit();
            if (introKit != null && introKit.length > 0) {
                Inventory inv = event.getPlayer().getInventory();
                inv.addItem(introKit);
                plugin().log(Level.INFO, "  Gave randomspawner kit to {0}", event.getPlayer().getDisplayName());
            }
        }
    }

    /**
     * The player's name is resolved as a MiniMessage placeholder component, not a string replacement - a
     * display name containing markup can't inject formatting into the announcement this way.
     */
    private void announceNewfriend(Player newfriend) {
        Component body = MiniMessage.miniMessage().deserialize(config.getAnnounceMessage(),
            Placeholder.component("player", Component.text()
                .append(CivText.head(newfriend))
                .append(Component.space())
                .append(CivText.subject(newfriend.getName()))
                .build()));
        Component message = CivUI.render(Kind.INFO, true, false, body);

        // Overlap is possible. Some people might get double-notified
        // Sent one player at a time, Bedrock players get the message without the head
        for (BroadcastLevel level : config.getAnnounceBroadcast()) {
            plugin().debug("  Broadcast to {0}", level);
            switch (level) {
                case OP -> {
                    for (Player op : Bukkit.getOnlinePlayers()) {
                        if (op.isOp()) {
                            op.sendMessage(Bedrock.adapt(op, message));
                        }
                    }
                }
                case PERM -> {
                    String permission = plugin().config().getBroadcastPermission();
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (player.hasPermission(permission)) {
                            player.sendMessage(Bedrock.adapt(player, message));
                        }
                    }
                    Bukkit.getConsoleSender().sendMessage(message);
                }
                case CONSOLE -> Bukkit.getConsoleSender().sendMessage(message);
                case ALL -> {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.sendMessage(Bedrock.adapt(player, message));
                    }
                    Bukkit.getConsoleSender().sendMessage(message);
                }
            }
        }
    }

    @Override
    public void registerListeners() {
        if (config.isEnabled()) {
            plugin().log("Registering PlayerJoin/Quit/KickEvent listener");
            plugin().registerListener(this);
        }
    }

    @Override
    public void registerCommands() {
        if (config.isEnabled()) {
            plugin().log("Registering introkit command");
            plugin().registerCommand("introkit", this);
        }
    }

    @Override
    public void dataBootstrap() {
        this.newfriendNames = new HashMap<>();
        this.newfriendSessionTime = new HashMap<>();
    }

    @Override
    public void unregisterListeners() {
    }

    @Override
    public void unregisterCommands() {
    }

    @Override
    public void dataCleanup() {
        // Before we dump the data, lets dump a status to the log.
        if (plugin() != null) {
            plugin().log(Level.INFO, status());
        }

        this.newfriendNames.clear();
        this.newfriendNames = null;
        this.newfriendSessionTime.clear();
        this.newfriendSessionTime = null;
    }

    /**
     * Shows all tracked data on new friends.
     */
    @Override
    public String status() {
        StringBuilder sb = new StringBuilder();
        if (config != null && config.isEnabled()) {
            sb.append("NewfriendAssist.PlayerJoin/Quit/KickEvent monitoring active");
        } else {
            sb.append("NewfriendAssist.PlayerJoin/Quit/KickEvent monitoring not active");
        }

        sb.append("\n  Since server start, ").append(NewfriendAssist.newfriendCount)
            .append(" new players have joined.");

        if (this.newfriendNames != null && !this.newfriendNames.isEmpty()) {
            sb.append("\n  New players since this hack was last enabled:");
            for (HashMap.Entry<UUID, String> entry : this.newfriendNames.entrySet()) {
                sb.append("\n    ").append(entry.getKey()).append(": ").append(entry.getValue());
                SessionTime soFar = newfriendSessionTime.get(entry.getKey());
                if (soFar != null) {
                    sb.append(" online ").append(soFar.totalTime() / 1000l).append(" seconds");
                }
            }
        }

        if (config.isIntroKitEnabled()) {
            sb.append("\n  Introkit gifting is enabled. Current Introkit:");
            if (config.getIntroKit() != null && config.getIntroKit().length > 0) {
                for (ItemStack item : config.getIntroKit()) {
                    sb.append("\n    ").append(item);
                }
            } else {
                sb.append("\n    -- in error --");
            }
        } else {
            sb.append("\n  Introkit gifting is disabled.");
        }

        return sb.toString();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!config.isIntroKitEnabled()) {
            CivUI.error(sender, "Introkit gifting is disabled");
            return true;
        }

        if (args.length < 1) return false;

        Player p = plugin().getServer().getPlayer(args[0]);

        if (p == null) {
            try {
                UUID pu = UUID.fromString(args[0]);
                p = plugin().getServer().getPlayer(pu);
            } catch (IllegalArgumentException iae) {
                p = null;
            }
        }

        if (p == null) {
            CivUI.error(sender, "Unable to find ", CivText.subject(args[0]));
        } else {
            plugin().log(Level.INFO, "Sent introkit to {0}", args[0]);
            CivUI.success(p, "You've been given an introductory kit!");
            Inventory inv = p.getInventory();
            inv.addItem(config.getIntroKit());
        }

        return true;
    }

    public static NewfriendAssistConfig generate(SimpleAdminHacks plugin, ConfigurationSection config) {
        return new NewfriendAssistConfig(plugin, config);
    }

    /**
     * Simple data storage and processing class for NewfriendAssist and elsewhere to track playtime.
     *
     * @author ProgrammerDan programmerdan@gmail.com
     */
    public static class SessionTime {

        private long playtimeTotal;
        private long lastJoin;

        public SessionTime(long joinTime) {
            playtimeTotal = 0l;
            lastJoin = joinTime;
        }

        public long endSession(long leaveTime) {
            if (lastJoin > 0l) {
                playtimeTotal = leaveTime - lastJoin;
                lastJoin = 0;
            }

            return playtimeTotal;
        }

        public void startSession(long startTime) {
            if (lastJoin > 0l) {
                endSession(startTime);
            }
            lastJoin = startTime;
        }

        public long totalTime() {
            long toReturn = playtimeTotal;
            if (lastJoin > 0l && System.currentTimeMillis() > lastJoin) {
                toReturn += System.currentTimeMillis() - lastJoin;
            }
            return toReturn;
        }
    }

}
