package com.untamedears.jukealert.model.appender;

import com.untamedears.jukealert.JukeAlert;
import com.untamedears.jukealert.model.Snitch;
import com.untamedears.jukealert.model.actions.abstr.LoggablePlayerAction;
import com.untamedears.jukealert.model.actions.abstr.SnitchAction;
import com.untamedears.jukealert.model.appender.config.LimitedActionTriggerConfig;
import com.untamedears.jukealert.util.JASettingsManager;
import com.untamedears.jukealert.util.JAText;
import com.untamedears.jukealert.util.JukeAlertPermissionHandler;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import vg.civcraft.mc.civmodcore.ui.CivText;

public class BroadcastEntryAppender extends ConfigurableSnitchAppender<LimitedActionTriggerConfig> {

    public static final String ID = "broadcast";

    public BroadcastEntryAppender(Snitch snitch, ConfigurationSection config) {
        super(snitch, config);
    }

    @Override
    public void acceptAction(SnitchAction action) {
        if (action.isLifeCycleEvent() || !action.hasPlayer()) {
            return;
        }
        LoggablePlayerAction log = (LoggablePlayerAction) action;
        if (snitch.hasPermission(log.getPlayer(), JukeAlertPermissionHandler.getSnitchImmune())) {
            return;
        }
        if (!config.isTrigger(action.getIdentifier())) {
            return;
        }
        for (UUID uuid : snitch.getGroup().getAllMembers()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                continue;
            }
            JASettingsManager settings = JukeAlert.getInstance().getSettingsManager();
            if (settings.doesIgnoreAllAlerts(uuid)) {
                continue;
            }
            if (settings.doesIgnoreAlert(snitch.getGroup().getName(), uuid)) {
                continue;
            }
            if (snitch.hasPermission(uuid, JukeAlertPermissionHandler.getSnitchAlerts())) {
                Component alert = log.getChatRepresentation(player.getLocation(), true, false);

                if (settings.shouldShowDirections(uuid)) {
                    Component directions = JAText.directions(snitch, player);
                    alert = alert.append(Component.text("  "))
                        .append(directions == null ? Component.empty() : directions);
                }
                if (settings.monocolorAlerts(uuid)) {
                    alert = Component.text(PlainTextComponentSerializer.plainText().serialize(alert), CivText.VALUE);
                }
                player.sendMessage(alert);
            }
        }
    }

    @Override
    public boolean runWhenSnitchInactive() {
        return false;
    }

    @Override
    public Class<LimitedActionTriggerConfig> getConfigClass() {
        return LimitedActionTriggerConfig.class;
    }

}
