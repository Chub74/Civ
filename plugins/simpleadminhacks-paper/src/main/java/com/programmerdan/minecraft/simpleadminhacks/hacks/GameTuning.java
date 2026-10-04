package com.programmerdan.minecraft.simpleadminhacks.hacks;

import com.programmerdan.minecraft.simpleadminhacks.framework.utilities.SAHText;
import com.programmerdan.minecraft.simpleadminhacks.SimpleAdminHacks;
import com.programmerdan.minecraft.simpleadminhacks.configs.GameTuningConfig;
import com.programmerdan.minecraft.simpleadminhacks.framework.SimpleHack;
import com.programmerdan.minecraft.simpleadminhacks.framework.utilities.TeleportUtil;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.PortalType;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.SkeletonHorse;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedEnterEvent.BedEnterResult;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SpawnEggMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import vg.civcraft.mc.civmodcore.inventory.gui.DecorationStack;
import vg.civcraft.mc.civmodcore.inventory.gui.IClickable;
import vg.civcraft.mc.civmodcore.inventory.gui.MultiPageView;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.civmodcore.ui.Kind;

/**
 * This is a grab-bag class to hold any _tuning_ related configurations that impact the
 * game, server-wide.
 * <p>
 * It's part of a series of focused hacks.
 * <p>
 * {@link GameFixes} is focused on things that are broken or don't work, and attempts to fix them.
 * {@link GameFeatures} focuses on enabling and disabling features, like elytra, various potion states.
 * {@link GameTuning} neither fixes nor disables, but rather adjusts and reconfigures.
 * <p>
 * Currently you can control the following:
 * - BlockEntity limits per chunk
 * - Setting bed during the day instead of just at night
 */
public class GameTuning extends SimpleHack<GameTuningConfig> implements Listener, CommandExecutor {

    public static final String NAME = "GameTuning";

    private static final MiniMessage MM = MiniMessage.miniMessage();

    /** A denied placement fires on every click, so one message per second at most. */
    private static final Duration DENY_WINDOW = Duration.ofSeconds(1);

    /** MultiPageView only takes a String title. */
    private static final String LIMITS_TITLE = LegacyComponentSerializer.legacySection()
        .serialize(CivText.heading("Chunk Limits"));

    public GameTuning(SimpleAdminHacks plugin, GameTuningConfig config) {
        super(plugin, config);
    }

    @Override
    public void registerListeners() {
        if (config != null && config.isEnabled()) {
            plugin().log("Registering GameTuning listeners");
            plugin().registerListener(this);
        }
    }

    @Override
    public void unregisterListeners() {
        // Bukkit does this for us.
    }

    @Override
    public void registerCommands() {
        if (config.isEnabled()) {
            plugin.registerCommand("chunklimits", this);
        }
    }

    @Override
    public void unregisterCommands() {
    }

    @Override
    public void dataBootstrap() {
    }

    @Override
    public void dataCleanup() {
        // NO-OP
    }

    @Override
    public String status() {
        StringBuilder genStatus = new StringBuilder();
        genStatus.append("GameTuning is ");
        if (config != null && config.isEnabled()) {
            genStatus.append("active\n");
            if (config.areChunkLimitsEnabled()) {
                genStatus.append("  Chunk Limits are enabled\n");
            } else {
                genStatus.append("  Chunk Limits are disabled\n");
            }
            if (config.areDaytimeBedsEnabled()) {
                genStatus.append("  Daytime Beds are enabled\n");
            } else {
                genStatus.append("  Daytime Beds are disabled\n");
            }

            genStatus.append("  One To One Nether is ");
            if (config.isOneToOneNether()) {
                genStatus.append("enabled\n");
            } else {
                genStatus.append("disabled\n");
            }
            genStatus.append("  Stop trap horses is ");
            if (config.stopTrapHorses()) {
                genStatus.append("enabled\n");
            } else {
                genStatus.append("disabled\n");
            }

            genStatus.append("  Kill trap horses is ");
            if (config.killTrapHorses()) {
                genStatus.append("enabled\n");
            } else {
                genStatus.append("disabled\n");
            }

            genStatus.append("  Changing spawner type is ");
            if (config.canChangeSpawnerType()) {
                genStatus.append("enabled\n");
            } else {
                genStatus.append("disabled\n");
            }

            genStatus.append("  Villager trading is ");
            if (config.allowVillagerTrading()) {
                genStatus.append("enabled\n");
            } else {
                genStatus.append("disabled\n");
            }

            genStatus.append("  Ender grief is ");
            if (config.isEnderGrief()) {
                genStatus.append("enabled\n");
            } else {
                genStatus.append("disabled\n");
            }

            genStatus.append("  Wither grief is ");
            if (config.isWitherGrief()) {
                genStatus.append("enabled\n");
            } else {
                genStatus.append("disabled\n");
            }

            genStatus.append("  Dragon grief is ");
            if (config.isDragonGrief()) {
                genStatus.append("enabled\n");
            } else {
                genStatus.append("disabled\n");
            }

            genStatus.append("  Prevent falling through bedrock is ");
            if (config.isPreventFallingThroughBedrock()) {
                genStatus.append("enabled\n");
            } else {
                genStatus.append("disabled\n");
            }

            genStatus.append("  Bad Omen is ");
            if (config.isBadOmenEnabled()) {
                genStatus.append("enabled\n");
            } else {
                genStatus.append("disabled\n");
            }

            genStatus.append("  Rain reduction is ");
            if (config.isRainReductionEnabled()) {
                genStatus.append("enabled\n");
            } else {
                genStatus.append("disabled\n");
            }

        } else {
            genStatus.append("inactive");
        }
        return genStatus.toString();
    }

    public static GameTuningConfig generate(SimpleAdminHacks plugin, ConfigurationSection config) {
        return new GameTuningConfig(plugin, config);
    }

    /* From here on, the actual meat of the hack. Above is basically boilerplate for micro-plugins.*/

    /**
     * Many thanks to BlackXNT for his work on this event in Humbug, which I have largely copied and expanded.
     * <p>
     * This tracks block placements, and if a limit is configured and the block is a TileEntity w/ state,
     * will reject the placement if otherwise it would exceed limits for the Chunk.
     *
     * @param event the Placement event.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void chunkLimits(BlockPlaceEvent event) {
        if (!config.isEnabled() || !config.areChunkLimitsEnabled()) return;
        try {
            Player player = event.getPlayer();
            Block block = event.getBlock();

            if (!config.applyChunkLimits(player.getUniqueId())) return;

            Material mat = block.getType();

            Integer limit = config.getChunkLimit(mat);
            if (limit == null) return;

            int current = 0;
            for (BlockState state : block.getChunk().getTileEntities()) {
                if (state != null && mat.equals(state.getType())) {
                    if (++current > limit) {
                        event.setCancelled(true);
                        sendLimitExceeded(player, mat, limit);
                        return;
                    }
                }
            }
        } catch (Exception e) {
            plugin().log(Level.WARNING, "Failed to measure chunk limit", e);
        }
    }

    private void sendLimitExceeded(Player player, Material mat, int limit) {
        String template = config.getChunkLimitsExceededMessage();
        if (template == null || template.isEmpty()) {
            return;
        }
        CivUI.notice(Kind.ERROR, MM.deserialize(template,
                Placeholder.component("limit", CivText.value(Integer.toString(limit))),
                Placeholder.component("material", materialWithIcon(mat))))
            .from(plugin())
            .to(player)
            .atMostEvery(DENY_WINDOW)
            .send();
    }

    /** The material's icon then its name; CivUI drops the icon for Bedrock players. */
    private static Component materialWithIcon(Material mat) {
        Component name = Component.translatable(mat, CivText.VALUE);
        Component icon = CivText.icon(mat);
        return icon == null ? name : Component.text().append(icon, Component.space(), name).build();
    }

    // If any limit at all, cancel the piston event.
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void chunkLimitsExploitExtend(BlockPistonExtendEvent event) {
        if (!config.isEnabled() || !config.areChunkLimitsEnabled()) return;
        List<Block> blocks = event.getBlocks();
        if (!blocks.isEmpty()) {
            for (Block b : blocks) {
                if (b != null && config.getChunkLimit(b.getType()) != null) {
                    event.setCancelled(true);
                    return; // TODO send message to nearby player warning of reason for stopping.
                }
            }
        }
    }

    // Yes, this is identical ...
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void chunkLimitsExploitRetract(BlockPistonRetractEvent event) {
        if (!config.isEnabled() || !config.areChunkLimitsEnabled()) return;
        List<Block> blocks = event.getBlocks();
        if (!blocks.isEmpty()) {
            for (Block b : blocks) {
                if (b != null && config.getChunkLimit(b.getType()) != null) {
                    event.setCancelled(true);
                    return; // TODO send message to nearby player warning of reason for stopping.
                }
            }
        }
    }

    @EventHandler
    public void enterBed(PlayerBedEnterEvent e) {
        if (!config.isEnabled() || !config.areDaytimeBedsEnabled()) {
            return;
        }
        if (BedEnterResult.NOT_POSSIBLE_NOW.equals(e.getBedEnterResult()) || BedEnterResult.NOT_SAFE.equals(e.getBedEnterResult())) {
            e.getPlayer().setBedSpawnLocation(e.getBed().getLocation(), false);
            e.getPlayer().sendTitle("", config.getDaytimeBedSpawnSetMessage());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPortalTravel(EntityPortalEvent event) {
        if (!config.isEnabled()) {
            return;
        }
        if (!config.allowNetherTravel() && event.getPortalType() == PortalType.NETHER) {
            event.setCancelled(true);
            return;
        }
        if (config.isOneToOneNether()) {
            Location newLoc = event.getFrom();
            newLoc.setWorld(event.getTo().getWorld());
            event.setTo(newLoc);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerPortalTravel(PlayerPortalEvent event) {
        if (config.isEnabled() && PlayerTeleportEvent.TeleportCause.NETHER_PORTAL == event.getCause()) {
            if (!config.allowNetherTravel()) {
                event.setCancelled(true);
            } else if (config.isOneToOneNether()) {
                Location newLoc = event.getFrom();
                newLoc.setWorld(event.getTo().getWorld());
                event.setTo(newLoc);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPortalCreate(PortalCreateEvent event) {
        if (config.isEnabled() && !config.isReturnNetherPortal()) {
            if (PortalCreateEvent.CreateReason.FIRE.equals(event.getReason()) && Environment.NETHER.equals(event.getWorld().getEnvironment())) {
                event.setCancelled(true);
            }
        }
    }

    //Trying to stop dupe bugs via minecart inventories
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityRightClick(PlayerInteractEntityEvent event) {
        if (!config.isEnabled()) return;
        if (!config.isChestedMinecartInventories() || !config.isHopperMinecartInventories()) {
            Entity target = event.getRightClicked();

            if (target.getType().equals(EntityType.CHEST_MINECART) && !config.isChestedMinecartInventories()) {
                event.setCancelled(true);
            }

            if (target.getType().equals(EntityType.HOPPER_MINECART) && !config.isHopperMinecartInventories()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryMoveItemEvent(InventoryMoveItemEvent event) {
        if (!config.isEnabled()) return;
        if (!config.isChestedMinecartInventories() || !config.isHopperMinecartInventories()) {
            InventoryHolder holder = event.getDestination().getHolder();
            if (holder instanceof StorageMinecart && !config.isChestedMinecartInventories()) {
                event.setCancelled(true);
            }

            if (holder instanceof HopperMinecart && !config.isHopperMinecartInventories()) {
                event.setCancelled(true);
            }

        }
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        if (config.isEnabled() && config.stopTrapHorses()) {
            Entity entity = event.getEntity();
            if (entity instanceof Skeleton && entity.isInsideVehicle() && entity.getVehicle() instanceof SkeletonHorse) {
                if (config.killTrapHorses()) {
                    entity.getVehicle().remove();
                }
                entity.remove();
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!config.isEnabled() || !Action.RIGHT_CLICK_BLOCK.equals(event.getAction())) {
            return;
        }
        boolean cancel = !config.canChangeSpawnerType() && Material.SPAWNER.equals(event.getClickedBlock().getType())
            && event.getItem() != null && event.getItem().getItemMeta() instanceof SpawnEggMeta;
        if (cancel) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (config.isEnabled() && !config.canPlace(event.getBlock().getType())) {
            event.setCancelled(true);
            SAHText.deny(event.getPlayer(), "You can't place that");
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (config.isEnabled() && !config.allowVillagerTrading()) {
            Entity npc = event.getRightClicked();
            // consistency, preserving null check
            if (npc != null && EntityType.VILLAGER.equals(npc.getType())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (config.isEnabled() &&
            (!config.isEnderGrief() && EntityType.ENDERMAN.equals(event.getEntityType())) ||
            (!config.isWitherGrief() && EntityType.WITHER.equals(event.getEntityType())) ||
            (!config.isDragonGrief() && EntityType.ENDER_DRAGON.equals(event.getEntityType()))) {
            event.setCancelled(true);
        }
    }

    /**
     * Speculative handler for dragon fireballs and exploding wither skulls
     * <p>
     * Some examples online prefer event.getEntity() instanceof DragonFireball and WitherSkull, could try that
     * if this does not work
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityExplodeEvent(EntityExplodeEvent event) {
        if (config.isEnabled() &&
            (!config.isDragonGrief() && EntityType.DRAGON_FIREBALL.equals(event.getEntityType())) ||
            (!config.isWitherGrief() && EntityType.WITHER_SKULL.equals(event.getEntityType()))) {
            event.setCancelled(true);
            // note this might not prevent block breaks, check on that (1.14)
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (config.isEnabled() && config.isPreventFallingThroughBedrock() && event.getTo().getY() < event.getTo().getWorld().getMinHeight()
            && GameMode.SURVIVAL.equals(event.getPlayer().getGameMode())) {
            TeleportUtil.tryToTeleportVertically(event.getPlayer(), event.getTo(), "falling into the void");
        }
    }

    @EventHandler
    public void onBadOmenEffect(EntityPotionEffectEvent event) {
        if (!config.isEnabled() || config.isBadOmenEnabled()) {
            return;
        }
        PotionEffect effect = event.getNewEffect();
        if (effect != null && effect.getType().equals(PotionEffectType.BAD_OMEN)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onRain(WeatherChangeEvent event) {
        if (!config.isEnabled() || !config.isRainReductionEnabled()) {
            return;
        }
        if (event.toWeatherState() && Math.random() >= config.getRainOccurrenceChance()) {
            event.setCancelled(true);
            plugin().getLogger().info("Rain event was cancelled");
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            CivUI.error(sender, "Only players can check chunk limits.");
            return true;
        }
        Map<Material, Integer> limits = config.getBlockEntityLimits();
        if (!config.areChunkLimitsEnabled() || limits.isEmpty()) {
            CivUI.info(player, "There are no chunk limits on this server.");
            return true;
        }
        Map<Material, Integer> used = countInChunk(player.getChunk(), limits.keySet());
        List<IClickable> items = limits.entrySet().stream()
            .filter(entry -> entry.getKey().isItem())
            .sorted(Comparator.comparing(entry -> entry.getKey().getKey().getKey()))
            .map(entry -> (IClickable) new DecorationStack(
                limitItem(entry.getKey(), entry.getValue(), used.getOrDefault(entry.getKey(), 0))))
            .toList();
        new MultiPageView(player, new ArrayList<>(items), LIMITS_TITLE, true).showScreen();
        return true;
    }

    /** The limited item, with how much of its limit this chunk has used. Full limits glint so they stand out. */
    private static ItemStack limitItem(Material material, int limit, int count) {
        boolean full = count >= limit;
        ItemStack item = new ItemStack(material);
        item.setData(DataComponentTypes.ITEM_NAME, Component.translatable(material, full ? CivText.ERROR : CivText.VALUE));
        item.setData(DataComponentTypes.LORE, ItemLore.lore(List.of(
            loreLine(Component.text("In this chunk "),
                Component.text(count, full ? CivText.ERROR : CivText.VALUE),
                Component.text(" / " + limit)),
            full
                ? loreLine(Component.text("Limit reached", CivText.ERROR))
                : loreLine(Component.text(limit - count, CivText.SUCCESS), Component.text(" more can be placed")))));
        if (full) {
            item.setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        }
        return item;
    }

    /** Lore renders italic unless told otherwise. */
    private static Component loreLine(Component... parts) {
        return Component.text().color(CivText.INFO).decoration(TextDecoration.ITALIC, false).append(parts).build();
    }

    /**
     * Counts the limited blocks and entities in a chunk. Blocks are counted by scanning the whole chunk, since
     * not every limited block (pistons, observers) has a block entity to find them by.
     */
    private static Map<Material, Integer> countInChunk(Chunk chunk, Set<Material> limited) {
        Map<Material, Integer> counts = new EnumMap<>(Material.class);
        if (limited.stream().anyMatch(Material::isBlock)) {
            ChunkSnapshot snapshot = chunk.getChunkSnapshot(false, false, false);
            int minY = chunk.getWorld().getMinHeight();
            int maxY = chunk.getWorld().getMaxHeight();
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = minY; y < maxY; y++) {
                        Material type = snapshot.getBlockType(x, y, z);
                        if (limited.contains(type)) {
                            counts.merge(type, 1, Integer::sum);
                        }
                    }
                }
            }
        }
        // Item frames, armour stands and paintings are entities named the same as their item
        for (Entity entity : chunk.getEntities()) {
            Material type = Material.getMaterial(entity.getType().name());
            if (type != null && limited.contains(type)) {
                counts.merge(type, 1, Integer::sum);
            }
        }
        return counts;
    }

}
