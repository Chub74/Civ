package vg.civcraft.mc.citadel.playerstate;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import vg.civcraft.mc.citadel.Citadel;
import vg.civcraft.mc.citadel.CitadelPermissionHandler;
import vg.civcraft.mc.citadel.CitadelUtility;
import vg.civcraft.mc.citadel.ReinforcementLogic;
import vg.civcraft.mc.citadel.events.ReinforcementChangeTypeEvent;
import vg.civcraft.mc.citadel.events.ReinforcementGroupChangeEvent;
import vg.civcraft.mc.citadel.model.Reinforcement;
import vg.civcraft.mc.citadel.reinforcementtypes.ReinforcementType;
import vg.civcraft.mc.civmodcore.inventory.items.ItemMap;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.civmodcore.ui.HudLine;
import vg.civcraft.mc.namelayer.NameLayerAPI;
import vg.civcraft.mc.namelayer.group.Group;

public class ReinforcingState extends AbstractPlayerState {

    private Group group;

    public ReinforcingState(Player p, Group group) {
        super(p);
        this.group = group;
    }

    public Group getGroup() {
        return group;
    }

    @Override
    public Component getName() {
        return Component.text()
            .append(CivText.mode("Reinforcing")).append(Component.text(" on "))
            .append(CitadelUtility.group(group))
            .build();
    }

    @Override
    public void handleBlockPlace(BlockPlaceEvent e) {
    }

    @Override
    public void handleInteractBlock(PlayerInteractEvent e) {
        // always cancel
        e.setCancelled(true);
        // does group still exist?
        if (!group.isValid()) {
            CivUI.error(e.getPlayer(), "The group ", CitadelUtility.group(group), " was deleted");
            Citadel.getInstance().getStateManager().setState(e.getPlayer(), null);
            return;
        }
        Player player = e.getPlayer();
        // does the player have an item?
        if (e.getItem() == null) {
            CitadelUtility.sendAndLog(player, ChatColor.RED, "Nothing in your hand to reinforce with",
                e.getClickedBlock().getLocation());
            return;
        }
        ReinforcementType type = Citadel.getInstance().getReinforcementTypeManager().getByItemStack(e.getItem(), player.getWorld().getName());
        // is it a valid item to reinforce with
        if (type == null) {
            CitadelUtility.sendAndLog(player, ChatColor.RED, "You can't reinforce with that item",
                e.getClickedBlock().getLocation());
            return;
        }
        Block block = ReinforcementLogic.getResponsibleBlock(e.getClickedBlock());
        // can the item reinforce the clicked block
        if (!type.canBeReinforced(block.getType())) {
            CitadelUtility.errorAt(player, block.getLocation(),
                CitadelUtility.item(type), " can't reinforce ", CivText.value(block.getType().name()));
            return;
        }
        // is the reinforcement item allowed in the current world
        if (!type.isAllowedInWorld(block.getWorld().getName())) {
            CitadelUtility.errorAt(player, block.getLocation(),
                CitadelUtility.item(type), " can't reinforce in this world");
            return;
        }
        // does the player have permission to reinforce on that group
        if (!NameLayerAPI.getGroupManager().hasAccess(group, e.getPlayer().getUniqueId(),
            CitadelPermissionHandler.getReinforce())) {
            CitadelUtility.errorAt(e.getPlayer(), e.getClickedBlock().getLocation(),
                "You lost reinforce permission on ", CitadelUtility.group(group));
            Citadel.getInstance().getStateManager().setState(e.getPlayer(), null);
            return;
        }
        Reinforcement rein = ReinforcementLogic.getReinforcementProtecting(block);
        // if reinforcement exists, check if player has permission to edit it
        if (rein != null) {
            if (!rein.hasPermission(e.getPlayer(), CitadelPermissionHandler.getBypass())) {
                CitadelUtility.sendAndLog(e.getPlayer(), ChatColor.RED,
                    "You can't bypass this reinforcement",
                    e.getClickedBlock().getLocation());
                return;
            }
        }
        Reinforcement newRein = null;
        if (rein == null || rein.getType() != type) {
            // check inventory for reinforcement item
            ItemMap toConsume = new ItemMap(type.getItem());
            if (!toConsume.isContainedIn(player.getInventory())) {
                CitadelUtility.sendAndLog(e.getPlayer(), ChatColor.RED, "No reinforcing item found in your inventory?",
                    e.getClickedBlock().getLocation());
                return;
            }
            if (rein == null) {
                newRein = ReinforcementLogic.callReinforcementCreationEvent(player, block, type, group);
                if (newRein == null) {
                    //event was cancelled, error message is up to whoever cancelled it
                    return;
                }
            }
            // consume item from inventory
            if (!toConsume.removeSafelyFrom(player.getInventory())) {
                CitadelUtility.errorAt(e.getPlayer(), e.getClickedBlock().getLocation(),
                    "Could not take ", CitadelUtility.item(type), " from your inventory");
                return;
            }
        }

        if (rein == null) {
            if (Citadel.getInstance().getConfigManager().logCreation()) {
                Citadel.getInstance().getLogger()
                    .info(player.getName() + " created reinforcement with " + type.getName() + " for "
                        + block.getType().toString() + " at "
                        + block.getLocation().toString());
            }
            // just create new reinforcement
            ReinforcementLogic.createReinforcement(newRein);
        } else {
            // replace existing one
            boolean changedGroup = false;
            if (group.getGroupId() != rein.getGroup().getGroupId()) {
                // switch group
                ReinforcementGroupChangeEvent rgce = new ReinforcementGroupChangeEvent(player, rein, group);
                Bukkit.getPluginManager().callEvent(rgce);
                if (!rgce.isCancelled()) {
                    rein.setGroup(group);
                    changedGroup = true;
                }
                // informing the user is up to whoever cancelled it
            }
            if (type != rein.getType()) {
                // switch type
                ReinforcementChangeTypeEvent rcte = new ReinforcementChangeTypeEvent(player, rein, type);
                Bukkit.getPluginManager().callEvent(rcte);
                if (!rcte.isCancelled()) {
                    if (rein.rollForItemReturn()) {
                        giveReinforcement(rein.getLocation().clone().add(0.5, 0.5, 0.5), e.getPlayer(), rein.getType());
                    }
                    rein.setType(type);
                    rein.setHealth(type.getHealth());
                    rein.resetCreationTime();
                    CitadelUtility.successAt(player, e.getClickedBlock().getLocation(),
                        "Updated reinforcement to ", CitadelUtility.item(rein.getType()),
                        " on ", CitadelUtility.group(group));
                } else if (changedGroup) {
                    CitadelUtility.successAt(player, e.getClickedBlock().getLocation(),
                        "Updated group to ", CitadelUtility.group(group));
                }
            } else if (changedGroup) {
                CitadelUtility.successAt(player, e.getClickedBlock().getLocation(),
                    "Updated group to ", CitadelUtility.group(group));
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ReinforcingState)) {
            return false;
        }
        return ((ReinforcingState) o).group.getName().equals(this.getGroup().getName());
    }

    @Override
    public String getOverlayText() {
        return HudLine.of(CivText.mode("Reinforcing"), CivText.group(group.getName()));
    }
}
