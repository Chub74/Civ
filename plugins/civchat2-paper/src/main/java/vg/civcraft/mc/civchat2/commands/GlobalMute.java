package vg.civcraft.mc.civchat2.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Syntax;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.bukkit.command.CommandSender;
import vg.civcraft.mc.civchat2.CivChat2;
import vg.civcraft.mc.civmodcore.config.ConfigHelper;
import vg.civcraft.mc.civmodcore.players.settings.impl.LongSetting;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.civmodcore.utilities.TextUtil;
import vg.civcraft.mc.namelayer.NameLayerAPI;

public class GlobalMute extends BaseCommand {

    @CommandAlias("globalmute")
    @CommandPermission("civchat2.globalmute")
    @Syntax("<set|check|remove> <player> [time]")
    @Description("Applies, checks or removes a players global mute timer")
    @CommandCompletion("set|check|remove @allplayers @nothing")
    public void execute(CommandSender sender, String getSetOrCheck, String targetPlayer, @Optional String muteTime) {
        UUID player = NameLayerAPI.getUUID(targetPlayer);
        if (player == null) {
            CivUI.error(sender, "The player ", CivText.subject(targetPlayer), " doesn't exist");
            return;
        }
        LongSetting banSetting = CivChat2.getInstance().getCivChat2SettingsManager().getGlobalChatMuteSetting();
        switch (getSetOrCheck.toLowerCase()) {
            case "set":
                if (muteTime == null) {
                    CivUI.error(sender, "You need to supply a mute timer");
                    return;
                }
                long time = ConfigHelper.parseTime(muteTime);
                if (time <= 0) {
                    CivUI.error(sender, "That's not a valid time frame");
                    return;
                }
                banSetting.setValue(player, System.currentTimeMillis() + time);
                CivUI.success(sender, CivText.subject(targetPlayer), " will be muted for ",
                    CivText.value(TextUtil.formatDuration(time, TimeUnit.MILLISECONDS)));
                return;
            case "remove":
                banSetting.setValue(player, 0L);
                CivUI.success(sender, "You unmuted ", CivText.subject(targetPlayer));
                return;
            case "check":
            case "get":
                long timeForUnban = banSetting.getValue(player);
                timeForUnban -= System.currentTimeMillis();
                if (timeForUnban <= 0) {
                    CivUI.info(sender, CivText.subject(targetPlayer), " isn't chat muted");
                    return;
                }
                CivUI.info(sender, CivText.subject(targetPlayer), " is muted for another ",
                    CivText.value(TextUtil.formatDuration(timeForUnban, TimeUnit.MILLISECONDS)));
                return;
            default:
                CivUI.error(sender, CivText.value(getSetOrCheck), " isn't a valid action");
        }
    }
}
