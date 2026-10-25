package com.devotedmc.ExilePearl.command;

import com.devotedmc.ExilePearl.ExilePearl;
import com.devotedmc.ExilePearl.ExilePearlApi;
import com.devotedmc.ExilePearl.Lang;
import com.devotedmc.ExilePearl.PearlType;
import com.devotedmc.ExilePearl.util.Permission;

public class CmdSummon extends PearlCommand {

    public CmdSummon(ExilePearlApi pearlApi) {
        super(pearlApi);
        this.aliases.add("summon");

        this.senderMustBePlayer = true;
        this.setHelpShort("Summons a prisoner");
        this.permission = Permission.SUMMON.node;
        this.visibility = CommandVisibility.SECRET;
    }

    @Override
    public void perform() {
        if (!plugin.getPearlConfig().allowSummoning()) {
            msg(Lang.summoningNotEnabled);
            return;
        }
        ExilePearl pearl = plugin.getPearlFromItemStack(player().getInventory().getItemInMainHand());
        if (pearl == null) {
            msg(Lang.pearlMustBeHoldingPearl);
            return;
        }
        if (plugin.isPlayerTagged(pearl.getPlayerId())) {
            msg(Lang.pearlPlayerIsCombatTagged);
            return;
        }

        if (pearl.getPearlType() != PearlType.PRISON) {
            msg("<b>You can only summon with prison pearls");
        } else if (!pearl.getPlayer().isOnline()) {
            msg("<b>You can't summon offline players");
        } else if (!plugin.getPearlManager().requestSummon(pearl)) {
            msg("<b>You already asked to summon them. Wait a bit before asking again");
        } else {
            msg(Lang.summonRequested, pearl.getPlayerName());
            msg(pearl.getPlayer(), Lang.hasRequestedToSummon, player().getName());
        }
    }
}
