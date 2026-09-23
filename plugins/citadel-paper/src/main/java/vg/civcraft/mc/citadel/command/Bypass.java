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

public class Bypass extends BaseCommand {

    @CommandAlias("ctb|bypass")
    @Description("Toggles bypass state. In bypass state you can break blocks reinforced on groups you have access to in a single break")
    public void execute(Player player) {
        BooleanSetting setting = Citadel.getInstance().getSettingManager().getBypass();
        boolean enabled = setting.getValue(player);
        if (enabled) {
            CitadelUtility.sendAndLog(player, ChatColor.GREEN, "Bypass off");
        } else {
            CivUI.notice(Kind.SUCCESS, "Bypass on")
                .to(player)
                .hover(CivText.info("You break reinforced blocks in one hit"
                    + " on groups you have access to"))
                .send();
        }
        setting.toggleValue(player.getUniqueId());
    }
}
