package com.github.maxopoly.finale.listeners;

import com.github.maxopoly.finale.misc.HudText;
import java.time.Duration;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.civmodcore.ui.Kind;
import com.github.maxopoly.finale.Finale;
import com.github.maxopoly.finale.external.CombatTagPlusManager;
import com.github.maxopoly.finale.external.FinaleSettingManager;

import java.util.UUID;
import java.util.function.BiFunction;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import vg.civcraft.mc.civmodcore.players.scoreboard.bottom.BottomLine;
import vg.civcraft.mc.civmodcore.players.scoreboard.bottom.BottomLineAPI;
import vg.civcraft.mc.civmodcore.players.scoreboard.side.CivScoreBoard;
import vg.civcraft.mc.civmodcore.players.scoreboard.side.ScoreBoardAPI;
import vg.civcraft.mc.civmodcore.utilities.cooldowns.ICoolDownHandler;
import vg.civcraft.mc.civmodcore.utilities.cooldowns.TickCoolDownHandler;

public class PearlCoolDownListener implements Listener {

    private static PearlCoolDownListener instance;

    public static long getPearlCoolDown(UUID uuid) {
        if (instance == null) {
            return -1;
        }
        return instance.cds.getRemainingCoolDown(uuid);
    }

    private ICoolDownHandler<UUID> cds;
    private CombatTagPlusManager ctpManager;
    private boolean combatTag;

    public PearlCoolDownListener(long cooldown, boolean combatTag, CombatTagPlusManager ctpManager) {
        instance = this;
        this.cds = new TickCoolDownHandler<>(Finale.getPlugin(), cooldown / 50);
        this.ctpManager = ctpManager;
        this.combatTag = combatTag;
    }

    public long getCoolDown() {
        return cds.getTotalCoolDown();
    }

    private BottomLine cooldownBottomLine;

    public BottomLine getCooldownBottomLine() {
        if (cooldownBottomLine == null) {
            cooldownBottomLine = BottomLineAPI.createBottomLine("pearlCooldown", 1);
            cooldownBottomLine.updatePeriodically(getCooldownBiFunction(), 1L);
        }
        return cooldownBottomLine;
    }

    private CivScoreBoard cooldownBoard;

    public CivScoreBoard getCooldownBoard() {
        if (cooldownBoard == null) {
            cooldownBoard = ScoreBoardAPI.createBoard("pearlCooldown");
            cooldownBoard.updatePeriodically(getCooldownBiFunction(), 1L);
        }
        return cooldownBoard;
    }

    public String getCooldownText(Player shooter) {
        return HudText.cooldown("Ender pearl", cds.getRemainingCoolDown(shooter.getUniqueId()));
    }

    public void putOnCooldown(Player shooter) {
        cds.putOnCoolDown(shooter.getUniqueId());
        FinaleSettingManager settings = Finale.getPlugin().getSettingsManager();
        if (settings.setVanillaPearlCooldown(shooter.getUniqueId())) {
            Bukkit.getScheduler().runTaskLater(Finale.getPlugin(), () -> {
                // -1, because this is delayed by one tick
                shooter.setCooldown(Material.ENDER_PEARL, (int) cds.getTotalCoolDown() - 1);
            }, 1);
        }

        if (settings.actionBarPearlCooldown(shooter.getUniqueId())) {
            BottomLine bottomLine = getCooldownBottomLine();
            bottomLine.updatePlayer(shooter, getCooldownText(shooter));
        }
        if (settings.sideBarPearlCooldown(shooter.getUniqueId())) {
            CivScoreBoard board = getCooldownBoard();
            board.set(shooter, getCooldownText(shooter));
        }
    }

    public BiFunction<Player, String, String> getCooldownBiFunction() {
        return (shooter, oldText) -> {
            if (!cds.onCoolDown(shooter.getUniqueId())) {
                return null;
            }

            return getCooldownText(shooter);
        };
    }

    @EventHandler
    public void pearlThrow(ProjectileLaunchEvent e) {
        // ensure it's a pearl
        if (e.getEntityType() != EntityType.ENDER_PEARL) {
            return;
        }
        // ensure a player threw it
        if (!(e.getEntity().getShooter() instanceof Player)) {
            return;
        }
        Player shooter = (Player) e.getEntity().getShooter();
        // check whether on cooldown
        if (cds.onCoolDown(shooter.getUniqueId())) {
            e.setCancelled(true);
            CivUI.notice(Kind.ERROR, "You can pearl again in ",
                    CivText.value(HudText.seconds(cds.getRemainingCoolDown(shooter.getUniqueId()))))
                .from(Finale.getPlugin())
                .to(shooter)
                .atMostEvery(Duration.ofSeconds(1))
                .send();
            return;
        }
        // tag player if desired
        if (combatTag && ctpManager != null) {
            ctpManager.tag((Player) e.getEntity().getShooter(), null);
        }

        // put pearl on cooldown
        putOnCooldown(shooter);
    }

}
