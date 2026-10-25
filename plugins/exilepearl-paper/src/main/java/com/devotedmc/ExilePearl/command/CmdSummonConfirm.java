package com.devotedmc.ExilePearl.command;

import com.devotedmc.ExilePearl.ExilePearl;
import com.devotedmc.ExilePearl.ExilePearlApi;
import com.devotedmc.ExilePearl.Lang;
import com.devotedmc.ExilePearl.PearlType;
import com.devotedmc.ExilePearl.holder.PlayerHolder;
import com.devotedmc.ExilePearl.util.Permission;

public class CmdSummonConfirm extends PearlCommand {

    public CmdSummonConfirm(ExilePearlApi pearlApi) {
        super(pearlApi);
        this.aliases.add("confirm");

        this.senderMustBePlayer = true;
        this.setHelpShort("Accepts a summon request");
        this.permission = Permission.SUMMON_CONFIRM.node;
        this.visibility = CommandVisibility.SECRET;
    }

    @Override
    protected void perform() {
        if (!plugin.getPearlConfig().allowSummoning()) {
            msg(Lang.summoningNotEnabled);
            return;
        }

        ExilePearl pearl = plugin.getPearl(player().getUniqueId());
        if (pearl == null || pearl.getPearlType() != PearlType.PRISON) {
            msg(Lang.onlyPrisonedPlayers);
            return;
        }
        if (!(pearl.getHolder() instanceof PlayerHolder)) {
            msg("<b>Someone has to be holding your pearl to summon you");
        } else {
            PlayerHolder holder = (PlayerHolder) pearl.getHolder();
            if (plugin.getPearlManager().summonPearl(pearl, holder.getPlayer())) {
                msg(Lang.pearlYouWereSummoned, holder.getName());
                msg(holder.getPlayer(), Lang.pearlSummoned, pearl.getPlayerName());
            } else {
                msg(holder.getPlayer(), Lang.pearlCantSummon);
                msg("<b>The summon didn't work");
            }
        }
    }

}
