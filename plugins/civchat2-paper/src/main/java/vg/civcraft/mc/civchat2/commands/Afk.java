package vg.civcraft.mc.civchat2.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Syntax;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import vg.civcraft.mc.civchat2.ChatStrings;
import vg.civcraft.mc.civchat2.CivChat2;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;

public class Afk extends BaseCommand {

    @CommandAlias("afk")
    @Description("Toggle afk status")
    @Syntax("[message]")
    public void execute(Player player, @Optional String afkMessageRaw) {
        Component afkMessage;
        if (afkMessageRaw == null) {
            afkMessage = ChatStrings.chatPlayerAfk;
        } else {
            afkMessage = CivText.info(afkMessageRaw);
        }

        boolean isAfk = CivChat2.getInstance().getCivChat2Manager().togglePlayerAfk(player, afkMessage);
        if (!isAfk) {
            CivUI.success(player, "You're no longer AFK");
            return;
        }

        CivUI.success(player, "You're now AFK - type /afk to remove AFK status");
    }
}
