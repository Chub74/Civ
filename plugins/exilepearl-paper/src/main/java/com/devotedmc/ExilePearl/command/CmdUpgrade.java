package com.devotedmc.ExilePearl.command;

import com.devotedmc.ExilePearl.ExilePearl;
import com.devotedmc.ExilePearl.ExilePearlApi;
import com.devotedmc.ExilePearl.Lang;
import com.devotedmc.ExilePearl.PearlType;
import com.devotedmc.ExilePearl.RepairMaterial;
import com.devotedmc.ExilePearl.util.Permission;
import com.devotedmc.ExilePearl.util.SpawnUtil;
import java.util.Set;
import vg.civcraft.mc.civmodcore.inventory.items.ItemMap;


/**
 * Upgrades the Exile Pearl in senders main hand to a Prison Pearl using upgrade_material held in players inventory
 */
public class CmdUpgrade extends PearlCommand {

    public CmdUpgrade(ExilePearlApi pearlApi) {
        super(pearlApi);
        this.aliases.add("upgrade");
        this.helpShort = "Upgrades an Exile Pearl to a Prison Pearl";
        this.senderMustBePlayer = true;
        this.permission = Permission.UPGRADE.node;
        this.visibility = CommandVisibility.SECRET;
    }

    @Override
    public void perform() {
        ExilePearl pearl = plugin.getPearlFromItemStack(player().getInventory().getItemInMainHand());
        if (pearl == null) {
            msg(Lang.pearlMustBeHoldingPearl);
            return;
        }
        Set<RepairMaterial> upgradeMaterials = plugin.getPearlConfig().getUpgradeMaterials();
        if (pearl.getPearlType() != PearlType.EXILE || upgradeMaterials == null) {
            msg("<b>You can only upgrade exile pearls");
            return;
        }
        ItemMap inv = new ItemMap(player().getInventory());
        for (RepairMaterial rm : upgradeMaterials) {
            if (inv.getAmount(rm.getStack()) >= rm.getRepairAmount()) {
                rm.getStack().setAmount(rm.getRepairAmount());
                ItemMap matches = new ItemMap(rm.getStack());
                boolean removed = new ItemMap(matches.getItemStackRepresentation()).removeSafelyFrom(player().getInventory());
                if (removed) {
                    upgradePearl(pearl);
                    return;
                }
            }
        }
        msg("<b>You don't have the materials to upgrade this pearl");
    }

    public void upgradePearl(ExilePearl pearl) {
        pearl.setPearlType(PearlType.PRISON);
        if (pearl.getPlayer() != null && pearl.getPlayer().isOnline()) {
            SpawnUtil.spawnPlayer(pearl.getPlayer(), pearl.getPearlType() == PearlType.PRISON ? plugin.getPearlConfig().getPrisonWorld() : plugin.getPearlConfig().getMainWorld());
            msg(pearl.getPlayer(), "<b>You were imprisoned in the end by <c>%s", player().getName());
        }
        plugin.log("The pearl for player %s was upgraded to a Prison Pearl.", pearl.getPlayerName());
        msg("<g>Upgraded <c>%s<g>'s pearl to a prison pearl", pearl.getPlayerName());
    }
}
