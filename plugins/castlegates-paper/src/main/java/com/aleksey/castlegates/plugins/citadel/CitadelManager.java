/**
 * @author Aleksey Terzi
 */

package com.aleksey.castlegates.plugins.citadel;

import java.util.List;
import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import vg.civcraft.mc.citadel.Citadel;
import vg.civcraft.mc.citadel.CitadelPermissionHandler;
import vg.civcraft.mc.citadel.model.Reinforcement;

import com.aleksey.castlegates.CastleGates;
import com.aleksey.castlegates.database.ReinforcementInfo;
import vg.civcraft.mc.citadel.playerstate.AbstractPlayerState;
import vg.civcraft.mc.citadel.playerstate.PlayerStateManager;
import vg.civcraft.mc.citadel.playerstate.ReinforcingState;
public class CitadelManager implements ICitadelManager {

    @Override
    public void init() {
    }

    @Override
    public void close() {
    }

    @Override
    public double getMaxRedstoneDistance() {
        return CastleGates.getConfigManager().getMaxRedstoneDistance();
    }

    @Override
    public ICitadel getCitadel(List<Player> players, Location loc) {
        Reinforcement rein = Citadel.getInstance().getReinforcementManager().getReinforcement(loc);

        boolean hasAccess = rein == null;
        boolean useJukeAlert = false;

        if (!hasAccess) {
            var doorsPermission = CitadelPermissionHandler.getDoors();

            if (players != null && players.size() > 0) {
                for (Player player : players) {
                    if (rein.hasPermission(player, doorsPermission) || player.hasPermission("citadel.admin")) {
                        hasAccess = true;
                        break;
                    }
                }
            }

            if (!hasAccess) {
                if (CastleGates.getJukeAlertManager().hasJukeAlertAccess(loc, rein.getGroup().getName())) {
                    hasAccess = true;
                    useJukeAlert = true;
                }
            }
        }

        return new com.aleksey.castlegates.plugins.citadel.Citadel(rein, hasAccess, useJukeAlert);
    }

    @Override
    public boolean isReinforced(Location loc) {
        Reinforcement rein = Citadel.getInstance().getReinforcementManager().getReinforcement(loc);
        return rein != null;
    }

    @Override
    public boolean canBypass(Player player, Location loc) {
        Reinforcement rein = Citadel.getInstance().getReinforcementManager().getReinforcement(loc);

        if (rein == null)
            return true;

        if (player == null)
            return false;

        return rein.hasPermission(player, CitadelPermissionHandler.getBypass())
            || player.hasPermission("citadel.admin");
    }

    @Override
    public boolean canViewInformation(Player player, Location loc) {
        Reinforcement rein = Citadel.getInstance().getReinforcementManager().getReinforcement(loc);

        if (rein == null)
            return true;

        if (player == null)
            return false;

        return rein.hasPermission(player, CitadelPermissionHandler.getInfo())
            || player.hasPermission("citadel.admin");
    }

    @Override
    public ReinforcementInfo removeReinforcement(Location loc) {
        Reinforcement rein = Citadel.getInstance().getReinforcementManager().getReinforcement(loc);

        if (rein == null)
            return null;

        ReinforcementInfo info = new ReinforcementInfo();
        info.CreationTime = rein.getCreationTime();
        info.TypeId = rein.getType().getID();
        info.Health = rein.getHealth();
        info.GroupId = rein.getGroupId();
        info.Insecure = rein.isInsecure();

        rein.setHealth(-1);

        return info;
    }

    @Override
    public boolean createReinforcement(ReinforcementInfo info, Location loc) {
        if (info == null)
            return false;

        var type = Citadel.getInstance().getReinforcementTypeManager().getById(info.TypeId);

        if (type == null) {
            CastleGates.getPluginLogger().log(Level.WARNING,
                "Could not restore reinforcement at {0}: reinforcement type {1} no longer exists",
                new Object[]{loc, info.TypeId});
            return false;
        }

        // Anything already here would be overwritten in the cache but left in the database, so remove it first.
        Reinforcement existing = Citadel.getInstance().getReinforcementManager().getReinforcement(loc);

        if (existing != null) {
            existing.setHealth(-1);
        }

        var rein = new Reinforcement(loc, type, info.GroupId, info.CreationTime, info.Health, info.Insecure, true);
        Citadel.getInstance().getReinforcementManager().putReinforcement(rein);

        return true;
    }

    @Override
    public boolean isReinforcingStateActive(Player player) {
        PlayerStateManager stateManager = Citadel.getInstance().getStateManager();
        AbstractPlayerState currentState = stateManager.getState(player);

        return currentState instanceof ReinforcingState;
    }
}
