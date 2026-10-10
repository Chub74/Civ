package vg.civcraft.mc.citadel.listener;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import vg.civcraft.mc.citadel.Citadel;
import vg.civcraft.mc.citadel.CitadelPermissionHandler;
import vg.civcraft.mc.citadel.CitadelUtility;
import vg.civcraft.mc.citadel.ReinforcementLogic;
import vg.civcraft.mc.citadel.events.ReinforcementModeSwitchEvent;
import vg.civcraft.mc.citadel.model.AcidManager;
import vg.civcraft.mc.citadel.model.CitadelSettingManager;
import vg.civcraft.mc.citadel.model.HologramManager;
import vg.civcraft.mc.citadel.model.Reinforcement;
import vg.civcraft.mc.citadel.playerstate.AbstractPlayerState;
import vg.civcraft.mc.citadel.playerstate.PlayerStateManager;
import vg.civcraft.mc.civmodcore.players.scoreboard.bottom.BottomLine;
import vg.civcraft.mc.civmodcore.players.scoreboard.bottom.BottomLineAPI;
import vg.civcraft.mc.civmodcore.players.scoreboard.side.CivScoreBoard;
import vg.civcraft.mc.civmodcore.players.scoreboard.side.ScoreBoardAPI;
import vg.civcraft.mc.civmodcore.players.settings.PlayerSetting;
import vg.civcraft.mc.civmodcore.players.settings.SettingChangeListener;
import vg.civcraft.mc.civmodcore.players.settings.impl.DisplayLocationSetting;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.HudLine;
import vg.civcraft.mc.civmodcore.utilities.DoubleInteractFixer;
import vg.civcraft.mc.civmodcore.utilities.TextUtil;

public class ModeListener implements Listener {

    private static final DecimalFormat commaFormat = new DecimalFormat("#.##");
    private static final DecimalFormat roundingFormat = new DecimalFormat("0");

    private DoubleInteractFixer interactFixer;
    private BottomLine ctiBottomLine;
    private CivScoreBoard ctiBoard;
    private BottomLine ctbBottomLine;
    private CivScoreBoard ctbBoard;
    private BottomLine reinBottomLine;
    private CivScoreBoard reinBoard;
    private CitadelSettingManager settingMan;
    private PlayerStateManager stateMan;

    public ModeListener(Citadel citadel) {
        interactFixer = new DoubleInteractFixer(citadel);
        this.stateMan = citadel.getStateManager();
        this.ctiBottomLine = BottomLineAPI.createBottomLine("ctiDisplay", 3);
        this.ctiBoard = ScoreBoardAPI.createBoard("ctiDisplay");
        this.ctbBottomLine = BottomLineAPI.createBottomLine("ctbDisplay", 3);
        this.ctbBoard = ScoreBoardAPI.createBoard("ctbDisplay");
        this.reinBottomLine = BottomLineAPI.createBottomLine("ctreinDisplay", 3);
        this.reinBoard = ScoreBoardAPI.createBoard("ctreinDisplay");
        this.settingMan = Citadel.getInstance().getSettingManager();
        settingMan.getInformationMode().registerListener(new SettingChangeListener<Boolean>() {
            @Override
            public void handle(UUID player, PlayerSetting<Boolean> setting, Boolean oldValue, Boolean newValue) {
                setCtiOverlay(Bukkit.getPlayer(player), newValue);
            }
        });
        settingMan.getInformationLocationSetting().registerListener(new SettingChangeListener<String>() {
            @Override
            public void handle(UUID player, PlayerSetting<String> setting, String oldValue, String newValue) {
                setCtiOverlay(Bukkit.getPlayer(player), settingMan.getInformationMode().getValue(player));
            }
        });
        settingMan.getBypass().registerListener(new SettingChangeListener<Boolean>() {
            @Override
            public void handle(UUID player, PlayerSetting<Boolean> setting, Boolean oldValue, Boolean newValue) {
                setCtbOverlay(Bukkit.getPlayer(player), newValue);
            }
        });
        settingMan.getBypassLocationSetting().registerListener(new SettingChangeListener<String>() {
            @Override
            public void handle(UUID player, PlayerSetting<String> setting, String oldValue, String newValue) {
                setCtbOverlay(Bukkit.getPlayer(player), settingMan.getBypass().getValue(player));
            }
        });
        settingMan.getModeLocationSetting().registerListener(new SettingChangeListener<String>() {

            @Override
            public void handle(UUID player, PlayerSetting<String> setting, String oldValue, String newValue) {
                setReinModeOverlay(Bukkit.getPlayer(player), stateMan.getState(Bukkit.getPlayer(player)));
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void modeChange(ReinforcementModeSwitchEvent event) {
        setReinModeOverlay(event.getPlayer(), event.getNewMode());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void playerLogin(PlayerJoinEvent e) {
        setCtiOverlay(e.getPlayer(), settingMan.getInformationMode().getValue(e.getPlayer()));
        setCtbOverlay(e.getPlayer(), settingMan.getBypass().getValue(e.getPlayer()));
        setReinModeOverlay(e.getPlayer(), stateMan.getState(e.getPlayer()));
    }

    private void setCtiOverlay(Player player, boolean state) {
        updateDisplaySetting(player, settingMan.getInformationLocationSetting(), state, HudLine.of(CivText.mode("Information mode")),
            null, ctiBottomLine, ctiBoard);
    }

    private void setCtbOverlay(Player player, boolean state) {
        updateDisplaySetting(player, settingMan.getBypassLocationSetting(), state, HudLine.of(CivText.mode("Bypass mode")),
            null, ctbBottomLine, ctbBoard);
    }

    private void setReinModeOverlay(Player player, AbstractPlayerState state) {
        if (state == null) {
            return;
        }
        updateDisplaySetting(player, settingMan.getModeLocationSetting(), true, state.getOverlayText(),
            state.getOverlayDisplay(), reinBottomLine, reinBoard);
    }

    /** display, if not null, replaces text wherever it shows, so sprites survive. */
    private static void updateDisplaySetting(Player player, DisplayLocationSetting locSetting, boolean state, String text,
                                             Component display, BottomLine bottomLine, CivScoreBoard scoreBoard) {
        if (player == null) {
            return;
        }
        if (text == null) {
            state = false;
        }
        if (!state) {
            // always clean up, value might have been changed
            bottomLine.removePlayer(player);
            scoreBoard.hide(player);
        } else {
            if (locSetting.showOnActionbar(player.getUniqueId())) {
                if (display != null) {
                    bottomLine.updatePlayer(player, display);
                } else {
                    bottomLine.updatePlayer(player, text);
                }
            }
            if (locSetting.showOnSidebar(player.getUniqueId())) {
                scoreBoard.set(player, text, display);
            }
        }
    }

    public static ChatColor getDamageColor(double relativeHealth) {
        if (relativeHealth >= 1.0) {
            return ChatColor.GREEN;
        } else if (relativeHealth >= 0.75) {
            return ChatColor.GREEN;
        } else if (relativeHealth >= 0.5) {
            return ChatColor.YELLOW;
        } else if (relativeHealth >= 0.25) {
            return ChatColor.RED;
        } else {
            return ChatColor.RED;
        }
    }

    public static String formatHealth(Reinforcement rein) {
        double broken = rein.getHealth() / rein.getType().getHealth();
        ChatColor color;
        if (broken >= 1.0) {
            color = ChatColor.GREEN;
        } else if (broken >= 0.75) {
            color = ChatColor.GREEN;
        } else if (broken >= 0.5) {
            color = ChatColor.YELLOW;
        } else if (broken >= 0.25) {
            color = ChatColor.RED;
        } else {
            color = ChatColor.RED;
        }
        return String.format("%s%s%% (%s/%s)", color.toString(),
            commaFormat.format(rein.getHealth() / rein.getType().getHealth() * 100),
            roundingFormat.format(rein.getHealth()), roundingFormat.format(rein.getType().getHealth()));
    }

    public static Component formatHealthRgb(Reinforcement rein) {
        return Component.text(String.format("%s%% (%s/%s)",
            commaFormat.format(rein.getHealth() / rein.getType().getHealth() * 100),
            roundingFormat.format(rein.getHealth()), roundingFormat.format(rein.getType().getHealth())), healthColor(rein));
    }

    /** Red through green as the reinforcement goes from broken to full health. */
    public static TextColor healthColor(Reinforcement rein) {
        double broken = rein.getHealth() / rein.getType().getHealth();
        int slot = Math.max(0, Math.min(12, (int) (broken * 12)));
        return new TextColor[] {
            TextColor.color(0xaa0000),
            TextColor.color(0xaa1c00),
            TextColor.color(0xaa3900),
            TextColor.color(0xaa5500),
            TextColor.color(0xaa7100),
            TextColor.color(0xaa8e00),
            TextColor.color(0xaaaa00),
            TextColor.color(0x8eaa00),
            TextColor.color(0x71aa00),
            TextColor.color(0x55aa00),
            TextColor.color(0x39aa00),
            TextColor.color(0x68f84c),
            TextColor.color(0x55ff00),
        }[slot];
    }

    public static String formatProgress(long start, long timeNeeded, String text) {
        long timeTaken = System.currentTimeMillis() - start;
        timeTaken = Math.min(timeTaken, timeNeeded);
        double progress = Math.min(1.0, ((double) timeTaken) / ((double) timeNeeded));
        return String.format("%s%% %s %s", commaFormat.format(progress * 100), text,
            TextUtil.formatDuration(timeNeeded - timeTaken, TimeUnit.MILLISECONDS));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void handleInteractBlock(PlayerInteractEvent e) {
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (interactFixer.checkInteracted(e.getPlayer(), e.getClickedBlock())) {
                return;
            }
        } else if (e.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        if (!settingMan.getInformationMode().getValue(e.getPlayer())) {
            return;
        }
        Reinforcement rein = ReinforcementLogic.getReinforcementProtecting(e.getClickedBlock());
        Player player = e.getPlayer();
        boolean showChat = settingMan.shouldShowChatInCti(player.getUniqueId());
        if (rein == null) {
            if (showChat) {
                CitadelUtility.infoAt(e.getPlayer(), e.getClickedBlock().getLocation(), "Not reinforced");
            }
            return;
        }
        if (player.getGameMode() == GameMode.CREATIVE && e.getAction() == Action.LEFT_CLICK_BLOCK) {
            e.setCancelled(true);
        }
        boolean showHolo = settingMan.shouldShowHologramInCti(player.getUniqueId());
        if (!rein.hasPermission(player, CitadelPermissionHandler.getInfo())) {
            if (showChat) {
                Citadel.getInstance().getSettingManager().sendCtiEnemyMessage(player, rein, e.getClickedBlock().getLocation());
            }
            if (showHolo) {
                showHolo(rein, player);
            }
            return;
        }
        if (showChat) {
            // An inspection result, so INFO rather than SUCCESS - nothing succeeded, the player asked.
            TextComponent.Builder sb = Component.text()
                .append(Component.text("Reinforced at "))
                .append(formatHealthRgb(rein))
                .append(Component.text(" health with "))
                .append(CitadelUtility.item(rein.getType()))
                .append(Component.text(" on "))
                .append(CitadelUtility.group(rein.getGroup()));
            if (!rein.isMature()) {
                sb.append(Component.space()).append(Component.text(
                    formatProgress(rein.getCreationTime(), rein.getType().getMaturationTime(), "mature"),
                    CivText.VALUE));
            }
            if (rein.isInsecure()) {
                sb.append(Component.space()).append(Component.text("(Insecure)", CivText.VALUE));
            }
            if (ReinforcementLogic.getDecayDamage(rein) != 1) {
                sb.append(Component.space()).append(Component.text(
                    "(Decayed x" + commaFormat.format(ReinforcementLogic.getDecayDamage(rein)) + ")",
                    CivText.VALUE));
            }
            AcidManager acidMan = Citadel.getInstance().getAcidManager();
            if (acidMan.isPossibleAcidBlock(e.getClickedBlock())) {
                Map<BlockFace, Long> times = acidMan.getRemainingAcidMaturationTime(rein);
                Block acidBlock = rein.getLocation().getBlock();
                for (Map.Entry<BlockFace, Long> entry : times.entrySet()) {
                    Reinforcement relativeReinforcement = ReinforcementLogic.getReinforcementProtecting(acidBlock.getRelative(entry.getKey()));
                    if (relativeReinforcement != null) {
                        if (!acidMan.canAcidBlock(rein.getType(), relativeReinforcement.getType())) {
                            sb.append(Component.newline()).append(Component.text(
                                entry.getKey() + " acid will fail", CivText.ERROR));
                            continue;
                        }
                        if (acidMan.isPossibleAcidBlock(relativeReinforcement.getLocation().getBlock()) && acidMan.isAcidOnSameGroup(rein, relativeReinforcement)) {
                            continue;
                        }
                        if (entry.getValue() == 0) {
                            sb.append(Component.newline()).append(Component.text(
                                entry.getKey() + " acid ready", CivText.WARN));
                            continue;
                        }
                        sb.append(Component.newline()).append(Component.text(
                            entry.getKey() + " acid ready in "
                                + TextUtil.formatDuration(entry.getValue(), TimeUnit.MILLISECONDS),
                            CivText.WARN));
                    }
                }
            }
            if (e.getPlayer().hasPermission("citadel.admin")) {
                sb.append(Component.newline())
                    .append(Component.text("ActivityMap.Activity = ", CivText.MUTED))
                    .append(Component.text(String.valueOf(Citadel.getInstance().getActivityMap()
                        .getLastActivityTime(rein.getGroup(), rein.getLocation())), CivText.VALUE));
            }
            CitadelUtility.infoAt(player, e.getClickedBlock().getLocation(), sb.build());
        }
        if (showHolo) {
            showHolo(rein, player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockBreak(BlockBreakEvent event) {
        Reinforcement rein = ReinforcementLogic.getReinforcementProtecting(event.getBlock());
        if (rein == null) {
            return;
        }
        Player player = event.getPlayer();
        boolean showHolo = Citadel.getInstance().getSettingManager().shouldShowHologramInCti(player.getUniqueId());
        if (!showHolo) {
            return;
        }
        showHolo(rein, player);
    }

    @EventHandler
    public void on(PlayerQuitEvent event) {
        HologramManager holoManager = Citadel.getInstance().getHologramManager();
        if (holoManager != null) {
            holoManager.deleteHolos(event.getPlayer());
        }
    }

    @EventHandler
    public void on(PlayerChangedWorldEvent event) {
        HologramManager holoManager = Citadel.getInstance().getHologramManager();
        if (holoManager != null) {
            holoManager.deleteHolos(event.getPlayer());
        }
    }

    private static void showHolo(Reinforcement rein, Player player) {
        HologramManager holoManager = Citadel.getInstance().getHologramManager();
        if (holoManager != null) {
            holoManager.showInfoHolo(rein, player);
        }
    }
}
