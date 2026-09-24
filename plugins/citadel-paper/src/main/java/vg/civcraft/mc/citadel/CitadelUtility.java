package vg.civcraft.mc.citadel;

import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import vg.civcraft.mc.citadel.acidtypes.AcidType;
import vg.civcraft.mc.citadel.model.AcidManager;
import vg.civcraft.mc.citadel.model.Reinforcement;
import vg.civcraft.mc.citadel.reinforcementtypes.ReinforcementType;
import vg.civcraft.mc.civmodcore.inventory.items.ItemMap;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.civmodcore.ui.Kind;
import vg.civcraft.mc.namelayer.NameLayerAPI;
import vg.civcraft.mc.namelayer.group.Group;

public class CitadelUtility {

    private CitadelUtility() {
    }

    public static boolean isPlant(Block plant) {
        switch (plant.getType()) {
            case DANDELION:
            case POPPY:
            case BLUE_ORCHID:
            case ALLIUM:
            case AZURE_BLUET:
            case ORANGE_TULIP:
            case RED_TULIP:
            case PINK_TULIP:
            case WHITE_TULIP:
            case OXEYE_DAISY:
            case PINK_PETALS:
            case ACACIA_SAPLING:
            case BIRCH_SAPLING:
            case DARK_OAK_SAPLING:
            case JUNGLE_SAPLING:
            case OAK_SAPLING:
            case SPRUCE_SAPLING:
            case FLOWERING_AZALEA:
            case CHERRY_SAPLING:
            case MANGROVE_PROPAGULE:
            case PALE_OAK_SAPLING:
            case WHEAT:
            case CARROTS:
            case POTATOES:
            case BEETROOTS:
            case MELON_STEM:
            case PUMPKIN_STEM:
            case ATTACHED_MELON_STEM:
            case ATTACHED_PUMPKIN_STEM:
            case NETHER_WART:
            case SUGAR_CANE:
            case CACTUS:
            case SUNFLOWER:
            case LILAC:
            case PEONY:
            case ROSE_BUSH:
            case TORCHFLOWER:
            case PITCHER_PLANT:
            case TALL_GRASS:
            case SHORT_GRASS:
            case TWISTING_VINES:
            case TWISTING_VINES_PLANT:
            case WEEPING_VINES:
            case WEEPING_VINES_PLANT:
            case CRIMSON_FUNGUS:
            case CRIMSON_ROOTS:
            case WARPED_FUNGUS:
            case WARPED_ROOTS:
            case NETHER_SPROUTS:
            case KELP:
            case KELP_PLANT:
            case SPORE_BLOSSOM:
            case BIG_DRIPLEAF:
            case BIG_DRIPLEAF_STEM:
            case CAVE_VINES:
            case CAVE_VINES_PLANT:
            case SMALL_DRIPLEAF:
            case SWEET_BERRY_BUSH:
            case BAMBOO:
            case BAMBOO_SAPLING:
            case SEA_PICKLE:
            case POINTED_DRIPSTONE:
                return true;
            default:
                return false;
        }
    }

    /** Call sites still pass a colour; the mapping decides what that colour means. */
    private static Kind kindOf(ChatColor color) {
        return switch (color) {
            case RED, DARK_RED -> Kind.ERROR;
            case GREEN, DARK_GREEN -> Kind.SUCCESS;
            case YELLOW -> Kind.WARN;
            default -> Kind.INFO;
        };
    }

    /** Some messages concatenate colour codes into the string, so it is parsed rather than taken literally. */
    private static Component body(String message) {
        return LegacyComponentSerializer.legacySection().deserialize(message);
    }

    /** The texture for a material. Empty if it has none. */
    public static Component sprite(Material material) {
        Component icon = CivText.icon(material);
        return icon == null ? Component.empty() : icon;
    }

    /** The type's texture and name, with the real item tooltip on hover. */
    public static Component item(ReinforcementType type) {
        ItemStack stack = type.getItem();
        Component label = Component.text(type.getName(), CivText.VALUE)
            .hoverEvent(HoverEvent.showItem(stack.getType(), stack.getAmount()));
        return Component.text()
            .append(sprite(stack.getType()))
            .append(Component.space())
            .append(label)
            .build();
    }

    public static Component group(Group group) {
        return CivText.group(group.getName());
    }

    private static Component coordsHover(Location location) {
        return Component.text("Location: ", CivText.INFO)
            .append(Component.text(
                location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ(),
                CivText.VALUE));
    }

    private static void logReply(CommandSender receiver, String message) {
        if (Citadel.getInstance().getConfigManager().logMessages()) {
            Citadel.getInstance().getLogger().log(Level.INFO, "Sent {0} reply {1}",
                new Object[]{receiver.getName(), message});
        }
    }

    public static void sendAndLog(CommandSender receiver, ChatColor color, String message) {
        CivUI.notice(kindOf(color), body(message)).to(receiver).send();
        logReply(receiver, message);
    }

    /** A success with the block's coordinates on hover. */
    public static void successAt(CommandSender receiver, Location location, Object... parts) {
        CivUI.notice(Kind.SUCCESS, parts).to(receiver).hover(coordsHover(location)).send();
        logReply(receiver, PlainTextComponentSerializer.plainText()
            .serialize(CivUI.render(Kind.SUCCESS, false, false, parts)));
    }

    /** An inspection result with the block's coordinates on hover. */
    public static void infoAt(CommandSender receiver, Location location, Object... parts) {
        CivUI.notice(Kind.INFO, parts).to(receiver).hover(coordsHover(location)).send();
    }

    /** An error with the block's coordinates on hover, taking components rather than a flat string. */
    public static void errorAt(CommandSender receiver, Location location, Object... parts) {
        CivUI.notice(Kind.ERROR, parts).to(receiver).hover(coordsHover(location)).send();
        logReply(receiver, PlainTextComponentSerializer.plainText()
            .serialize(CivUI.render(Kind.ERROR, false, false, parts)));
    }

    public static void sendAndLog(CommandSender receiver, ChatColor color, String message, Location location) {
        CivUI.notice(kindOf(color), body(message))
            .to(receiver)
            .hover(coordsHover(location))
            .send();
        logReply(receiver, message);
    }

    public static void debugLog(String msg) {
        if (Citadel.getInstance().getConfigManager().isDebugEnabled()) {
            Citadel.getInstance().getLogger().info(msg);
        }
    }

    public static boolean consumeReinforcementItems(Player player, ReinforcementType type, boolean consumeExtra) {
        ItemMap toRemove = new ItemMap(type.getItem());
        if (consumeExtra) {
            toRemove.addItemAmount(type.getItem(), 1);
        }
        if (!toRemove.removeSafelyFrom(player.getInventory())) {
            CivUI.error(player, "Could not take the ", item(type), " from your inventory");
            return false;
        }
        return true;
    }

    public static boolean attemptReinforcementCreation(Block block, ReinforcementType type, Group group,
                                                       Player player) {
        return attemptReinforcementCreation(block, type, group, player, null);
    }

    /**
     * onFailure is appended to every failure message, for callers whose mode drops as a result. Null appends
     * nothing - the mode did not change, so there is nothing to report.
     */
    public static boolean attemptReinforcementCreation(Block block, ReinforcementType type, Group group,
                                                       Player player, Component onFailure) {
        final Object tail = onFailure == null ? "" : onFailure;
        if (!group.isValid()) {
            errorAt(player, block.getLocation(), "The group ", group(group), " was deleted", tail);
            return true;
        }
        if (!NameLayerAPI.getGroupManager().hasAccess(group, player.getUniqueId(),
            CitadelPermissionHandler.getReinforce())) {
            errorAt(player, block.getLocation(), "You lost reinforce permission on ", group(group), tail);
            return true;
        }
        block = ReinforcementLogic.getResponsibleBlock(block);
        Reinforcement existingRein = Citadel.getInstance().getReinforcementManager().getReinforcement(block);
        if (existingRein != null) {
            // e.g. a slab whose other half is already reinforced - not an error, just nothing to do
            return false;
        }
        if (!type.canBeReinforced(block.getType())) {
            errorAt(player, block.getLocation(), item(type), " can't reinforce ", block.getType(), tail);
            return false;
        }
        if (!type.isAllowedInWorld(block.getWorld().getName())) {
            errorAt(player, block.getLocation(), item(type), " can't reinforce in this world", tail);
            return true;
        }
        ItemMap playerItems = new ItemMap(player.getInventory());
        int available = playerItems.getAmount(type.getItem());
        int required = type.getItem().getAmount();
        boolean consumeExtra = false;
        // Cloned rather than a material check so lore/name-restricted items still match.
        ItemStack offfhandStack = player.getInventory().getItemInOffHand().clone();
        offfhandStack.setAmount(1);

        boolean blockIsAlsoReinforcement = block.getType() == type.getItem().getType();
        if (blockIsAlsoReinforcement) {
            // The block being placed is itself the reinforcement item, so one more must be consumed
            // or it would count as reinforcing itself for free.
            boolean placingFromReinforcementStack = (player.getInventory().getHeldItemSlot() == player.getInventory().first(block.getType())) ||
                (offfhandStack.equals(type.getItem()));
            if (placingFromReinforcementStack) {
                consumeExtra = true;
                required++;
            }
        }
        if (available < required) {
            errorAt(player, block.getLocation(), "No ", item(type), " left to reinforce with", tail);
            return true;
        }
        Reinforcement newRein = ReinforcementLogic.callReinforcementCreationEvent(player, block, type, group);
        if (newRein == null) {
            return true;
        }
        if (!CitadelUtility.consumeReinforcementItems(player, type, consumeExtra)) {
            return true;
        }
        if (Citadel.getInstance().getConfigManager().logCreation()) {
            Citadel.getInstance().getLogger().info(player.getName() + " created reinforcement with " + type.getName()
                + " for " + block.getType().toString() + " at " + block.getLocation().toString());
        }
        if (Citadel.getInstance().getAcidManager().isPossibleAcidBlock(block)) {
            AcidManager acidMan = Citadel.getInstance().getAcidManager();
            AcidType acidType = acidMan.getAcidTypeFromMaterial(block.getType());
            for (BlockFace blockFace : acidType.blockFaces()) {
                Block relativeBlock = block.getRelative(blockFace);
                Reinforcement relativeReinforcement = ReinforcementLogic.getReinforcementProtecting(relativeBlock);
                if (
                    relativeReinforcement == null
                        || !relativeReinforcement.getType().canBeReinforced(relativeBlock.getType())
                        || (acidMan.isPossibleAcidBlock(relativeBlock) && acidMan.isAcidOnSameGroup(newRein, relativeReinforcement))
                ) {
                    continue;
                }
                if (!acidMan.canAcidBlock(type, relativeReinforcement.getType())) {
                    CitadelUtility.sendAndLog(player, ChatColor.RED,
                        "The " + blockFace + " acid will fail - that reinforcement is tougher");
                }
            }
        }
        ReinforcementLogic.createReinforcement(newRein);
        return false;
    }
}
