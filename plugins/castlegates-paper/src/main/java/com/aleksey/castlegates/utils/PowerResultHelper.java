package com.aleksey.castlegates.utils;

import java.time.Duration;
import java.util.List;

import com.aleksey.castlegates.CastleGates;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import com.aleksey.castlegates.types.PowerResult;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.civmodcore.ui.Kind;

public class PowerResultHelper {

    /** Redstone clocks can retrigger a failing gate every tick. */
    private static final Duration STATUS_WINDOW = Duration.ofSeconds(2);

    public static void showStatus(Location location, List<Player> players, PowerResult result) {
        Component message = switch (result.status) {
            case Blocked -> getBlockedMessage(result);
            case Broken -> getBrokenMessage(result);
            case CannotDrawGear -> getCannotDrawGearMessage(result);
            case NotInCitadelGroup -> Component.text("Nobody nearby has access to this bridge or gate");
            case DifferentCitadelGroup -> Component.text("Every block of a bridge or gate must be reinforced to the same group");
            case BastionBlocked -> Component.text("A bastion stopped the bridge or gate from undrawing");
            case Locked -> Component.text("A locked gearblock stopped the bridge or gate");
            default -> null;
        };

        if (message != null) {
            if (!players.isEmpty()) {
                CivUI.notice(Kind.ERROR, message)
                    .from(CastleGates.getInstance())
                    .to(players.toArray(new Player[0]))
                    .atMostEvery(STATUS_WINDOW)
                    .send();
            }
        } else if (result.status == PowerResult.Status.Drawn || result.status == PowerResult.Status.Undrawn) {
            playSound(location, result.status);
        }

        if (result.block != null) {
            ParticleHelper.spawn(result.block, ParticleHelper.Type.Warning);
        }
    }

    public static void playSound(Location location, PowerResult.Status status) {
        Sound sound = status == PowerResult.Status.Drawn
            ? Sound.BLOCK_PISTON_CONTRACT
            : Sound.BLOCK_PISTON_EXTEND;

        location.getWorld().playSound(location, sound, 0.7f, 1);
    }

    private static Component getBlockedMessage(PowerResult result) {
        return Component.text()
            .append(Component.text("Something at "))
            .append(Text.coords(result.block.getLocation()))
            .append(Component.text(" is in the way of the bridge or gate"))
            .build();
    }

    private static Component getBrokenMessage(PowerResult result) {
        Location location = result.block.getLocation();

        if (result.block.getType() == Material.AIR) {
            return Component.text()
                .append(Component.text("The bridge or gate is broken at "))
                .append(Text.coords(location))
                .build();
        }

        return Component.text()
            .append(Text.block(result.block.getType()))
            .append(Component.text(" at "))
            .append(Text.coords(location))
            .append(Component.text(" can't be part of a bridge or gate"))
            .build();
    }

    private static Component getCannotDrawGearMessage(PowerResult result) {
        return Component.text()
            .append(Component.text("The gearblock at "))
            .append(Text.coords(result.block.getLocation()))
            .append(Component.text(" can't be part of a bridge or gate"))
            .build();
    }
}
