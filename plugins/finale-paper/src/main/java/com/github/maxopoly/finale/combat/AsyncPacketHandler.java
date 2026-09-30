package com.github.maxopoly.finale.combat;

import vg.civcraft.mc.civmodcore.ui.Kind;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.civmodcore.ui.CivText;
import java.time.Duration;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.EnumWrappers.EntityUseAction;
import com.comphenix.protocol.wrappers.EnumWrappers.Hand;
import com.comphenix.protocol.wrappers.WrappedEnumEntityUseAction;
import com.github.maxopoly.finale.Finale;
import com.google.common.collect.Sets;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class AsyncPacketHandler extends PacketAdapter implements Listener {

    private static final PacketType SWING_PACKET = PacketType.fromName("PUNCH").stream()
        .filter(type -> type.getProtocol() == PacketType.Protocol.PLAY && type.isClient())
        .findFirst()
        .orElse(PacketType.Play.Client.ARM_ANIMATION);

    private static final PacketType ATTACK_PACKET = findAttackPacket();

    private static PacketType findAttackPacket() {
        try {
            return (PacketType) PacketType.Play.Client.class.getField("ATTACK").get(null);
        } catch (ReflectiveOperationException e) {
            return PacketType.Play.Client.USE_ENTITY;
        }
    }

    private CombatConfig cc;

    public AsyncPacketHandler(CombatConfig cc) {
        super(Finale.getPlugin(), ListenerPriority.HIGH, ATTACK_PACKET, SWING_PACKET, PacketType.Play.Client.BLOCK_DIG, PacketType.Play.Client.ENTITY_ACTION);

        this.cc = cc;

        Bukkit.getPluginManager().registerEvents(this, Finale.getPlugin());
    }

    private Set<UUID> isDigging = Sets.newConcurrentHashSet();
    private Map<UUID, Long> lastRemovals = new ConcurrentHashMap<>();
    private Map<UUID, Long> lastStartBreaks = new ConcurrentHashMap<>();

    @Override
    public void onPacketReceiving(PacketEvent event) {
        PacketType packetType = event.getPacketType();

        CPSHandler cpsHandler = Finale.getPlugin().getManager().getCPSHandler();
        if (packetType == ATTACK_PACKET) {
            Player attacker = event.getPlayer();
            World world = attacker.getWorld();

            PacketContainer packet = event.getPacket();
            if (packetType == PacketType.Play.Client.USE_ENTITY) {
                StructureModifier<WrappedEnumEntityUseAction> actions = packet.getEnumEntityUseActions();
                if (actions.read(0).getAction() != EntityUseAction.ATTACK) {
                    return;
                }
            }
            event.setCancelled(true);
            new BukkitRunnable() {

                @Override
                public void run() {
                    Entity entity = packet.getEntityModifier(event).read(0);
                    Damageable target = entity instanceof Damageable ? (Damageable) entity : null;

                    if (target == null || target.isDead() || target.isInvulnerable() ||
                        !world.getUID().equals(target.getWorld().getUID()) || !(target instanceof LivingEntity)) {
                        if (entity instanceof CraftEntity craftEntity) {
                            DamageSources damageSources = ((CraftWorld) world).getHandle().damageSources();
                            craftEntity.getHandle().hurt(damageSources.playerAttack(((CraftPlayer) attacker).getHandle()), (float) ((CraftPlayer) attacker).getHandle().getAttribute(Attributes.ATTACK_DAMAGE).getValue());
                        }
                        return;
                    }

                    double distanceSquared = attacker.getLocation().distanceSquared(target.getLocation());

                    if (distanceSquared > (cc.getMaxReach() * cc.getMaxReach())) {
                        return;
                    }

                    if (cpsHandler.getCPS(attacker.getUniqueId()) >= cc.getCPSLimit()) {
                        CivUI.notice(Kind.ERROR, "You're clicking faster than the limit of ",
                                CivText.value(cc.getCPSLimit() + " CPS"))
                            .from(Finale.getPlugin())
                            .to(attacker)
                            .atMostEvery(Duration.ofSeconds(1))
                            .send();
                        return;
                    }

                    CombatUtil.attack(attacker, ((CraftEntity) target).getHandle());
                }
            }.runTask(Finale.getPlugin());
        } else if (packetType == PacketType.Play.Client.ENTITY_ACTION) {
            Player player = event.getPlayer();
            PacketContainer packet = event.getPacket();
            EnumWrappers.PlayerAction playerAction = packet.getPlayerActions().read(0);
            SprintHandler sprintHandler = Finale.getPlugin().getManager().getSprintHandler();
            if (playerAction == EnumWrappers.PlayerAction.START_SPRINTING) {
                sprintHandler.startSprinting(player);
            } else if (playerAction == EnumWrappers.PlayerAction.STOP_SPRINTING) {
                sprintHandler.stopSprinting(player);
            }
        } else if (packetType == SWING_PACKET) {
            Player attacker = event.getPlayer();
            PacketContainer packet = event.getPacket();
            Hand hand = packetType == PacketType.Play.Client.ARM_ANIMATION ? packet.getHands().getValues().get(0) : Hand.MAIN_HAND;
            if (hand == Hand.MAIN_HAND && !isDigging.contains(attacker.getUniqueId())) {
                Block targetBlock = attacker.getTargetBlockExact(4);
                if (targetBlock != null && targetBlock.getType() != Material.AIR) {
                    return;
                }
                cpsHandler.updateClicks(attacker);
            }
        } else if (packetType == PacketType.Play.Client.BLOCK_DIG) {
            Player attacker = event.getPlayer();

            if (attacker.getGameMode() != GameMode.SURVIVAL) {
                return;
            }

            if (!(event.getPacket().getHandle() instanceof ServerboundPlayerActionPacket actionPacket)) {
                return;
            }
            BlockPos blockPos = actionPacket.getPos();
            ServerboundPlayerActionPacket.Action digType = actionPacket.getAction();
            if (digType == ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK) {
                Block block = attacker.getWorld().getBlockAt(blockPos.getX(), blockPos.getY(), blockPos.getZ());
                if (block.getType() == Material.BEDROCK || block.getType() == Material.BARRIER && !isDigging.contains(attacker.getUniqueId())) {
                    isDigging.add(attacker.getUniqueId());
                    cpsHandler.updateClicks(attacker);
                    return;
                }

                ServerLevel nmsWorld = ((CraftWorld) block.getWorld()).getHandle();
                float strength = nmsWorld.getBlockState(blockPos).getDestroySpeed(nmsWorld, blockPos);

                long lastStartBreak = lastStartBreaks.getOrDefault(attacker.getUniqueId(), 0L);
                long timeSinceBreak = (System.currentTimeMillis() - lastStartBreak);
                lastStartBreaks.put(attacker.getUniqueId(), System.currentTimeMillis());
                if (strength > 0) {
                    long lastRemoval = lastRemovals.getOrDefault(attacker.getUniqueId(), 0L);
                    long timeSinceRemoval = (System.currentTimeMillis() - lastRemoval);

                    if (isDigging.contains(attacker.getUniqueId())) {
                        return;
                    }
                    isDigging.add(attacker.getUniqueId());
                    if (timeSinceRemoval >= 48 && timeSinceBreak > 51) {
                        cpsHandler.updateClicks(attacker);
                    }
                }
            } else if (digType == ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK || digType == ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK) {
                isDigging.remove(attacker.getUniqueId());
                lastRemovals.put(attacker.getUniqueId(), System.currentTimeMillis());
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();

        isDigging.remove(player.getUniqueId());
        lastRemovals.remove(player.getUniqueId());
        lastStartBreaks.remove(player.getUniqueId());
    }
}
