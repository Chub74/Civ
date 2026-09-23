package vg.civcraft.mc.citadel.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Description;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import vg.civcraft.mc.citadel.Citadel;
import vg.civcraft.mc.citadel.CitadelUtility;
import vg.civcraft.mc.civmodcore.players.settings.impl.BooleanSetting;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.civmodcore.ui.Kind;

public class EasyMode extends BaseCommand {

    @CommandAlias("cte|easymode")
    @Description("Reinforces to your default group using materials from your offhand.")
    public void execute(Player player) {
        BooleanSetting setting = Citadel.getInstance().getSettingManager().getEasyMode();
        boolean enabled = setting.getValue(player);
        if (enabled) {
            CitadelUtility.sendAndLog(player, ChatColor.GREEN, "Easy reinforcing off");
        } else {
            CivUI.notice(Kind.SUCCESS, "Easy reinforcing on")
                .to(player)
                .hover(CivText.info("Place blocks with a reinforcement material in your off hand"
                    + " and they reinforce to your default group"))
                .send();
        }
        setting.toggleValue(player.getUniqueId());
    }
}
