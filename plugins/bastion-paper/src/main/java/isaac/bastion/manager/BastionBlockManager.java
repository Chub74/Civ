package isaac.bastion.manager;

import isaac.bastion.Bastion;
import isaac.bastion.BastionBlock;
import isaac.bastion.BastionType;
import isaac.bastion.Permissions;
import isaac.bastion.event.BastionDamageEvent;
import isaac.bastion.event.BastionDamageEvent.Cause;
import isaac.bastion.storage.BastionBlockStorage;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import org.apache.commons.collections4.CollectionUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import vg.civcraft.mc.citadel.CitadelUtility;
import vg.civcraft.mc.citadel.model.Reinforcement;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.namelayer.GroupManager;
import vg.civcraft.mc.namelayer.NameLayerAPI;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.permission.PermissionType;

public class BastionBlockManager {

    private static Random rng = new Random();

    private HashMap<UUID, HashMap<String, Long>> cooldowns = new HashMap<>();
    private BastionBlockStorage storage;

    public BastionBlockManager() {
        storage = Bastion.getBastionStorage();
    }

    // For block places
    public void erodeFromPlace(Player player, Set<BastionBlock> blocking) {
        erodeFromAction(player, blocking, Cause.BLOCK_PLACED, PermissionType.getPermission(Permissions.BASTION_PLACE));
    }

    // For pearls
    public void erodeFromTeleport(Player player, Set<BastionBlock> blocking) {
        erodeFromAction(player, blocking, Cause.PEARL, PermissionType.getPermission(Permissions.BASTION_PEARL));
    }

    /**
     * Common handler for erosion.
     */
    private void erodeFromAction(Player player, Set<BastionBlock> blocking, Cause cause, PermissionType perm) {
        HashMap<BastionType, Set<BastionBlock>> typeMap = new HashMap<>();
        for (BastionBlock block : blocking) {
            if (cause == Cause.PEARL && !block.getType().isBlockPearls()) {
                continue;
            }
            Set<BastionBlock> set = typeMap.get(block.getType());
            if (set == null) {
                set = new HashSet<>();
                typeMap.put(block.getType(), set);
            }
            set.add(block);
        }

        for (BastionType type : typeMap.keySet()) {
            if (onCooldown(player.getUniqueId(), type)) {
                continue;
            }
            Set<BastionBlock> bastions = typeMap.get(type);
            Set<BastionBlock> otherNearby = getBlockingBastionsWithoutPermission(
                bastions.stream().map(b -> b.getLocation()).collect(Collectors.toSet()),
                player.getUniqueId(), perm);
            // dont damage those already directly damaged
            otherNearby.removeAll(bastions);
            for (BastionBlock bastion : otherNearby) {
                if (bastion.getType() != type) {
                    continue;
                }
                boolean closeEnough = false;
                for (BastionBlock comparesBas : bastions) {
                    if (Math.abs(comparesBas.getLocation().getBlockX() - bastion.getLocation().getBlockX()) > type
                        .getProximityDamageRange()) {
                        continue;
                    }
                    if (Math.abs(comparesBas.getLocation().getBlockZ() - bastion.getLocation().getBlockZ()) > type
                        .getProximityDamageRange()) {
                        continue;
                    }
                    closeEnough = true;
                    break;
                }
                if (closeEnough) {
                    double damage = cause == Cause.BLOCK_PLACED ? bastion.getErosionFromBlock()
                        : bastion.getErosionFromPearl();
                    damage *= type.getProximityDamageFactor();
                    BastionDamageEvent event = new BastionDamageEvent(bastion, player, cause, damage);
                    Bukkit.getPluginManager().callEvent(event);
                    if (event.isCancelled()) {
                        continue;
                    }
                    bastion.erode(damage);
                }
            }

            if (type.getBlocksToErode() < 0) { // erode all
                for (BastionBlock bastion : bastions) {
                    double damage = cause == Cause.BLOCK_PLACED ? bastion.getErosionFromBlock()
                        : bastion.getErosionFromPearl();
                    BastionDamageEvent event = new BastionDamageEvent(bastion, player, cause, damage);
                    Bukkit.getPluginManager().callEvent(event);
                    if (event.isCancelled()) {
                        continue;
                    }
                    bastion.erode(damage);
                }
            } else if (type.getBlocksToErode() > 0) { // erode some
                List<BastionBlock> ordered = new LinkedList<>(bastions);
                for (int i = 0; i < ordered.size() && i < type.getBlocksToErode(); i++) {
                    int erode = rng.nextInt(ordered.size());
                    BastionBlock bastion = ordered.get(erode);
                    double damage = cause == Cause.BLOCK_PLACED ? bastion.getErosionFromBlock()
                        : bastion.getErosionFromPearl();
                    BastionDamageEvent event = new BastionDamageEvent(bastion, player, cause, damage);
                    Bukkit.getPluginManager().callEvent(event);
                    if (event.isCancelled()) {
                        continue;
                    }
                    bastion.erode(damage);
                    ordered.remove(erode);
                }
            }
        }
    }

    public boolean onCooldown(UUID player, BastionType type) {
        if (!cooldowns.containsKey(player)) {
            cooldowns.put(player, new HashMap<String, Long>());
            return false;
        }
        if (!cooldowns.get(player).containsKey(type.getName())) {
            cooldowns.get(player).put(type.getName(), System.currentTimeMillis());
            return false;
        }
        long lastPlaced = cooldowns.get(player).get(type.getName());

        if ((System.currentTimeMillis() - lastPlaced) < type.getPlacementCooldown()) {
            return true;
        } else {
            cooldowns.get(player).put(type.getName(), System.currentTimeMillis());
        }

        return false;
    }

    public Set<BastionBlock> getBlockingBastions(Location loc) {
        return getBlockingBastions(loc, null);
    }

    public Set<Group> getEnteredGroupFields(Location source, Collection<Location> target) {
        Set<Group> result = new HashSet<>();
        for (Location loc : target) {
            for (BastionBlock b : getBlockingBastions(loc)) {
                result.add(b.getGroup());
            }
        }
        for (BastionBlock b : getBlockingBastions(source)) {
            result.remove(b.getGroup());
        }

        return result;
    }

    public Set<BastionBlock> getBlockingBastions(Set<Location> locs) {
        Set<BastionBlock> result = new HashSet<>();
        for (Location loc : locs) {
            result.addAll(getBlockingBastions(loc));
        }
        return result;
    }

    public Set<BastionBlock> getBlockingBastionsWithoutPermission(Set<Location> locs, UUID player,
                                                                  PermissionType permission) {
        Set<BastionBlock> result = new HashSet<>();
        for (Location loc : locs) {
            result.addAll(getBlockingBastionsWithoutPermission(loc, player, permission));
        }
        return result;
    }

    public Set<BastionBlock> getBlockingBastionsWithoutPermission(Location loc, UUID player,
                                                                  PermissionType permission) {
        return getBlockingBastions(loc, b -> !NameLayerAPI.getGroupManager().hasAccess(b.getGroup(), player, permission));
    }

    public Set<BastionBlock> getBlockingBastions(Location loc, Predicate<BastionBlock> filter) {
        Set<BastionBlock> boxes = storage.forLocation(loc);
        if (CollectionUtils.isEmpty(boxes)) {
            return Collections.emptySet();
        }
        Set<BastionBlock> result = new HashSet<>();
        for (BastionBlock bastion : boxes) {
            if (filter != null && !filter.test(bastion)) {
                continue;
            }
            if (bastion.inField(loc)) {
                result.add(bastion);
            }

        }
        return result;
    }

    public boolean canListBastionsForGroup(Player player, Integer groupId) {
        if (player == null || groupId == null) {
            return false;
        }
        Group group = GroupManager.getGroup(groupId);
        if (group == null) {
            return false;
        }
        PermissionType permission = PermissionType.getPermission(Permissions.BASTION_LIST);
        return NameLayerAPI.getGroupManager().hasAccess(group, player.getUniqueId(), permission);
    }

    public Component bastionDeletedMessage(BastionBlock bastion) {
        return Component.text("Bastion deleted").hoverEvent(HoverEvent.showText(bastion.hoverComponent()));
    }

    public Component bastionCreatedMessage(Location location, Reinforcement reinforcement) {
        BastionBlock bastion = storage.getBastionBlock(location);
        return Component.text("Bastion created, reinforced with ")
            .append(CitadelUtility.item(reinforcement.getType()))
            .append(Component.text(" on "))
            .append(CitadelUtility.group(reinforcement.getGroup()))
            .hoverEvent(HoverEvent.showText(bastion.hoverComponent()));
    }

    /** An inspection result: the bastion at the clicked block, or a summary of what's blocking the target block. */
    public Component infoMessage(boolean dev, Block block, Block clicked, Player player) {
        BastionBlock bastion = storage.getBastionBlock(clicked.getLocation());
        if (bastion == null) {
            return blockingSummary(dev, block, player);
        }
        Component component = bastion.infoComponent(dev);
        if (canListBastionsForGroup(player, bastion.getListGroupId())) {
            component = component.hoverEvent(HoverEvent.showText(bastion.hoverComponent()));
        }
        return component;
    }

    private Component blockingSummary(boolean dev, Block block, Player player) {
        Set<BastionBlock> bastions = getBlockingBastions(block.getLocation());
        Component summary;
        if (bastions.isEmpty()) {
            summary = Component.text("No bastion here");
        } else {
            Set<BastionType> alliedBastions = new HashSet<>();
            Set<BastionType> enemyBastions = new HashSet<>();
            for (BastionBlock bas : bastions) {
                if (NameLayerAPI.getGroupManager().hasAccess(bas.getGroup(), player.getUniqueId(), PermissionType.getPermission(Permissions.BASTION_PLACE))) {
                    alliedBastions.add(bas.getType());
                } else {
                    enemyBastions.add(bas.getType());
                }
            }
            if (alliedBastions.isEmpty()) {
                summary = Component.text("Enemy ").append(bastionTypeList(enemyBastions))
                    .append(Component.text(" blocks you from building here"));
            } else if (enemyBastions.isEmpty()) {
                summary = Component.text("Your allied ").append(bastionTypeList(alliedBastions))
                    .append(Component.text(" protects this from others"));
            } else {
                summary = Component.text("Enemy ").append(bastionTypeList(enemyBastions))
                    .append(Component.text(" blocks you, allied ")).append(bastionTypeList(alliedBastions))
                    .append(Component.text(" protects others"));
            }
        }
        if (dev) {
            for (BastionBlock bas : bastions) {
                summary = summary.append(Component.newline()).append(bas.infoComponent(true));
            }
        }
        return summary;
    }

    private Component bastionTypeList(Set<BastionType> types) {
        List<String> names = types.stream().map(BastionType::getName).sorted().collect(Collectors.toList());
        return Component.text(String.join(", ", names), CivText.VALUE);
    }

    public Boolean changeBastionGroup(Player player, Reinforcement reinforcement, Location location) {
        BastionBlock bastion = storage.getBastionBlock(location);

        if (bastion == null) {
            return null;
        }

        Reinforcement oldReinf = bastion.getReinforcement();

        if (NameLayerAPI.getGroupManager().hasAccess(reinforcement.getGroup(), player.getUniqueId(),
            PermissionType.getPermission(Permissions.BASTION_PLACE))
            && (oldReinf == null || NameLayerAPI.getGroupManager().hasAccess(oldReinf.getGroup(), player.getUniqueId(),
            PermissionType.getPermission(Permissions.BASTION_PLACE)))) {
            // The new reinforcement isn't registered yet during the creation event
            storage.changeBastionGroup(bastion, reinforcement.getGroupId());
            return Boolean.TRUE;
        } else {
            return Boolean.FALSE;
        }
    }

    public void getBastionsByGroupIds(List<Integer> groupIds, List<BastionBlock> result) {
        storage.getBastionsByGroupIds(groupIds, result);
    }
}
