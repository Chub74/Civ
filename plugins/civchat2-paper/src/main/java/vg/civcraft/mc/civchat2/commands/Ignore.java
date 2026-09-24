package vg.civcraft.mc.civchat2.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Syntax;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import vg.civcraft.mc.civchat2.CivChat2;
import vg.civcraft.mc.civchat2.database.CivChatDAO;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;

public class Ignore extends BaseCommand {

    @CommandAlias("ignore")
    @Syntax("<player>")
    @Description("Toggles ignoring a player")
    @CommandCompletion("@allplayers")
    public void execute(Player player, String targetPlayer) {
        Bukkit.getScheduler().runTaskAsynchronously(CivChat2.getInstance(), () -> {
            OfflinePlayer ignoredPlayerNonFinal = Bukkit.getServer().getPlayer(targetPlayer);
            if (ignoredPlayerNonFinal == null) {
                ignoredPlayerNonFinal = Bukkit.getServer().getOfflinePlayer(targetPlayer);
            }
            if (!ignoredPlayerNonFinal.hasPlayedBefore() && !ignoredPlayerNonFinal.isOnline()) {
                CivUI.error(player, "No player exists with that name");
                return;
            }
            OfflinePlayer ignoredPlayer = ignoredPlayerNonFinal;
            Bukkit.getScheduler().runTask(CivChat2.getInstance(), () -> {
                if (player.equals(ignoredPlayer)) {
                    CivUI.error(player, "You can't ignore yourself");
                    return;
                }
                CivChatDAO db = CivChat2.getInstance().getDatabaseManager();
                if (!db.isIgnoringPlayer(player.getUniqueId(), ignoredPlayer.getUniqueId())) {
                    // Player added to the list
                    db.addIgnoredPlayer(player.getUniqueId(), ignoredPlayer.getUniqueId());
                    CivUI.success(player, "You're now ignoring ", CivText.subject(ignoredPlayer.getName()));
                } else {
                    // Player removed from the list
                    db.removeIgnoredPlayer(player.getUniqueId(), ignoredPlayer.getUniqueId());
                    CivUI.success(player, "You stopped ignoring ", CivText.subject(ignoredPlayer.getName()));
                }
            });
        });
    }
}
