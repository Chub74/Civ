package vg.civcraft.mc.citadel.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Description;
import org.bukkit.entity.Player;
import vg.civcraft.mc.citadel.Citadel;
import vg.civcraft.mc.civmodcore.players.settings.impl.BooleanSetting;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;

public class Information extends BaseCommand {

    @CommandAlias("cti|ctinfo|info")
    @Description("Enters information mode. Interacting with blocks in information mode will show information on their reinforcement")
    public void execute(Player player) {
        BooleanSetting ctiSetting = Citadel.getInstance().getSettingManager().getInformationMode();
        ctiSetting.toggleValue(player.getUniqueId());
        boolean on = ctiSetting.getValue(player.getUniqueId());
        CivUI.success(player, "Reinforcement info ", CivText.toggle(on));
    }
}
