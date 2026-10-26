package com.github.maxopoly.finale.overlay;

import com.github.maxopoly.finale.Finale;
import com.github.maxopoly.finale.external.FinaleSettingManager;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import vg.civcraft.mc.civmodcore.chat.ChatUtils;
import vg.civcraft.mc.civmodcore.inventory.items.ItemUtils;
import vg.civcraft.mc.civmodcore.players.scoreboard.bottom.BottomLine;
import vg.civcraft.mc.civmodcore.players.scoreboard.bottom.BottomLineAPI;
import vg.civcraft.mc.civmodcore.players.scoreboard.side.CivScoreBoard;
import vg.civcraft.mc.civmodcore.players.scoreboard.side.ScoreBoardAPI;
import vg.civcraft.mc.civmodcore.players.settings.impl.DisplayLocationSetting;
import vg.civcraft.mc.civmodcore.ui.Bedrock;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.HudLine;

public class ScoreboardHUD implements Listener {

    //TODO Make this a configuration item so it can be changed at runtime.
    private final static long UPDATE_PERIOD_IN_TICKS = 20L * 5L; // Every 5 seconds.

    private List<CivScoreBoard> scoreBoards;
    private FinaleSettingManager settingsMan;
    private BottomLine coordsBottomLine;

    public ScoreboardHUD(FinaleSettingManager settingsMan) {
        scoreBoards = new ArrayList<>();
        this.coordsBottomLine = BottomLineAPI.createBottomLine("Location", 4);
        this.settingsMan = settingsMan;
        for (int i = 0; i < 12; i++) {
            scoreBoards.add(ScoreBoardAPI.createBoard("finaleArmor" + i));
        }
        Bukkit.getScheduler().runTaskTimer(Finale.getPlugin(), () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                updateDurabilities(p);
                updateCoordinates(p, settingsMan.getCoordsLocation());
                if (settingsMan.showPotionEffects(p.getUniqueId())) {
                    updateAllPotionEffects(p);
                }
            }
        }, UPDATE_PERIOD_IN_TICKS, UPDATE_PERIOD_IN_TICKS);
        settingsMan.getArmorSetting().registerListener((player, setting, oldValue, newValue) -> {
            Player p = Bukkit.getPlayer(player);
            if (p == null) {
                return;
            }
            if (newValue) {
                updateDurabilities(p);
            } else {
                for (int i = 1; i < 5; i++) {
                    scoreBoards.get(i).set(p, null);
                }
            }
        });
        settingsMan.getToolSetting().registerListener((player, setting, oldValue, newValue) -> {
            Player p = Bukkit.getPlayer(player);
            if (p == null) {
                return;
            }
            if (newValue) {
                updateDurabilities(p);
            } else {
                scoreBoards.get(0).set(p, null);
            }
        });
        settingsMan.getGammaBrightSetting().registerListener((player, setting, oldValue, newValue) -> {
            Player p = Bukkit.getPlayer(player);
            if (p == null) {
                return;
            }
            if (newValue) {
                updateGammaBright(p);
            } else {
                p.removePotionEffect(PotionEffectType.NIGHT_VISION);
            }
        });
        settingsMan.getPotionSetting().registerListener((player, setting, oldValue, newValue) -> {
            Player p = Bukkit.getPlayer(player);
            if (p == null) {
                return;
            }
            if (newValue) {
                updateAllPotionEffects(p);
            } else {
                for (int i = 5; i < 11; i++) {
                    scoreBoards.get(i).set(p, null);
                }
            }
        });

        settingsMan.getCoordsLocation().registerListener((player, playerSetting, s, t1) -> {
            Player p = Bukkit.getPlayer(player);
            if (p == null) {
                return;
            }
            updateCoordinates(Bukkit.getPlayer(player), settingsMan.getCoordsLocation());
        });
    }

    private void updateGammaBright(Player player) {
        player.removePotionEffect(PotionEffectType.NIGHT_VISION);
        //after being logged in for 14 days straight, players may have to relog to refresh the effect
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 20 * 60 * 60 * 24 * 14, 0, false, false, false));
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void playerJoin(PlayerJoinEvent e) {
        updateDurabilities(e.getPlayer());
        if (settingsMan.showPotionEffects(e.getPlayer().getUniqueId())) {
            updateAllPotionEffects(e.getPlayer());
        }
        if (settingsMan.getGammaBrightSetting().getValue(e.getPlayer())) {
            updateGammaBright(e.getPlayer());
        }
        updateCoordinates(e.getPlayer(), settingsMan.getCoordsLocation());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void playerRespawn(PlayerRespawnEvent e) {
        Bukkit.getScheduler().runTask(Finale.getPlugin(), () -> {
            if (settingsMan.getGammaBrightSetting().getValue(e.getPlayer())) {
                updateGammaBright(e.getPlayer());
            }
        });
    }

    private void updateAllPotionEffects(Player p) {
        int boardIndex = 5;
        for (PotionEffect pot : p.getActivePotionEffects()) {
            if (boardIndex >= 11) {
                break;
            }
            if (pot.getType().equals(PotionEffectType.NIGHT_VISION)
                && settingsMan.getGammaBrightSetting().getValue(p)) {
                continue;
            }
            showEffect(p, scoreBoards.get(boardIndex), boardIndex, pot);
            boardIndex++;
        }
        while (boardIndex < 11) {
            scoreBoards.get(boardIndex).set(p, null);
            boardIndex++;
        }
    }

    /** Invisible: a colour code that renders nothing but keeps lines in order, since every score is 0. */
    private static String sortingPrefix(int order) {
        char code = LegacyComponentSerializer.SECTION_CHAR;
        return "" + code + Character.forDigit(order, 16) + code + 'r';
    }

    private static String roman(int level) {
        String[] numerals = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return level >= 1 && level <= numerals.length ? numerals[level - 1] : String.valueOf(level);
    }

    private void updateDurabilities(Player p) {
        if (settingsMan.showToolDurability(p.getUniqueId())) {
            showDurability(p, 0, "Tool", p.getInventory().getItemInMainHand());
        }
        if (settingsMan.showArmorDurability(p.getUniqueId())) {
            ItemStack[] armor = p.getInventory().getArmorContents();
            showDurability(p, 1, "Helmet", armor[3]);
            showDurability(p, 2, "Chestplate", armor[2]);
            showDurability(p, 3, "Leggings", armor[1]);
            showDurability(p, 4, "Boots", armor[0]);
        }
    }

    private void showDurability(Player p, int order, String slot, ItemStack item) {
        CivScoreBoard board = scoreBoards.get(order);
        Damageable damageable = item == null ? null : ItemUtils.getDamageable(item);
        int max = damageable == null ? 0
            : damageable.hasMaxDamage() ? damageable.getMaxDamage() : item.getType().getMaxDurability();
        if (max <= 0) {
            board.set(p, null);
            return;
        }
        int remaining = max - damageable.getDamage();
        float ratio = (float) remaining / max;
        TextColor colour = ratio > 0.5F ? CivText.SUCCESS : ratio > 0.25F ? CivText.WARN : CivText.ERROR;
        // the remaining points carry the colour the percentage used to
        Component points = Component.text()
            .append(Component.text(remaining, colour))
            .append(CivText.info("/" + max))
            .build();

        // Bedrock can't draw the icon, so it gets the slot name the icon stands in for
        Component icon = Bedrock.isBedrock(p) ? null : CivText.icon(item);
        Component display = HudLine.component(icon == null ? slot : icon, points);
        board.set(p, sortingPrefix(order) + HudLine.of(slot, points), display);
    }

    private void showEffect(Player p, CivScoreBoard board, int order, PotionEffect effect) {
        String name = ChatUtils.stringify(Component.translatable(effect.getType()));
        int level = effect.getAmplifier() + 1;
        if (level > 1) {
            name += " " + roman(level);
        }
        Component time = null;
        if (!effect.isInfinite()) {
            int seconds = effect.getDuration() / 20;
            time = Component.text(String.format("%d:%02d", seconds / 60, seconds % 60),
                seconds <= 30 ? CivText.WARN : CivText.VALUE);
        }
        Component display = HudLine.component(CivText.icon(effect.getType()), CivText.subject(name), time);
        board.set(p, sortingPrefix(order) + HudLine.of(name, time), Bedrock.adapt(p, display));
    }

    private void updateCoordinates(Player p, DisplayLocationSetting setting) {

    }
}
