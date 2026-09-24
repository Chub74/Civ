package vg.civcraft.mc.civchat2.prefix;

import com.programmerdan.minecraft.banstick.data.BSPlayer;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.event.HoverEventSource;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import vg.civcraft.mc.civchat2.CivChat2;

public class StarManager {

    private static final String STAR = "⋆";
    private static final String ADMIN_STAR = "✧";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("d MMM uuuu");

    private final boolean playtimeStars;

    public StarManager(boolean playtimeStars) {
        this.playtimeStars = playtimeStars;
    }

    private long getJoined(Player player) {
        if (!Bukkit.getPluginManager().isPluginEnabled("BanStick")) {
            return player.getFirstPlayed();
        }

        BSPlayer bsPlayer = BSPlayer.byUUID(player.getUniqueId());
        if (bsPlayer == null) {
            return 0;
        }
        return bsPlayer.getFirstAdd().toInstant().toEpochMilli();
    }

    public HoverEventSource<?> hover(Player player) {
        long firstPlayed = getJoined(player);
        String joined = firstPlayed == 0 ? "unknown" : FORMATTER.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(firstPlayed), ZoneId.systemDefault()));

        String rank;
        if (player.hasPermission("civchat.admin")) {
            rank = "Admin";
        } else if (player.hasPermission("civchat.superfriend")) {
            rank = "Superfriend";
        } else if (player.hasPermission("civchat.mod")) {
            rank = "Moderator";
        } else if (player.hasPermission("civchat.powerplayer")) {
            rank = "Civ Power Player";
        } else if (player.hasPermission("civchat.grinder")) {
            rank = "Civ Grinder";
        } else if (player.hasPermission("civchat.bigdonator")) {
            rank = "Loyal Patron";
        } else if (player.hasPermission("civchat.donator")) {
            rank = "Namecolor Patron";
        } else {
            rank = "None";
        }

        return HoverEvent.showText(Component.text("Rank: " + rank + "\nJoined: " + joined));
    }

    /** The rank/playtime star prefix */
    public Component getPrefix(Player player) {
        if (!CivChat2.getInstance().getCivChat2SettingsManager().isShowPrefix(player.getUniqueId())) {
            return Component.empty();
        }
        Component stars = getStars(player);
        // One unstyled space after any stars, so every rank sits the same distance from the name
        return stars == null ? Component.empty() : Component.text().append(stars).append(Component.space()).build();
    }

    /** Null if the player has no stars. */
    private Component getStars(Player player) {
        if (player.hasPermission("civchat.admin")) {
            return Component.text(ADMIN_STAR, NamedTextColor.RED, TextDecoration.BOLD);
        } else if (player.hasPermission("civchat.superfriend")) {
            return Component.text(STAR.repeat(2), NamedTextColor.RED);
        } else if (player.hasPermission("civchat.mod")) {
            return Component.text(STAR, NamedTextColor.RED);
        }

        int greenStars = 0;
        if (player.hasPermission("civchat.powerplayer")) {
            greenStars = 4;
        } else if (player.hasPermission("civchat.grinder")) {
            greenStars = 3;
        } else if (player.hasPermission("civchat.bigdonator")) {
            greenStars = 2;
        } else if (player.hasPermission("civchat.donator")) {
            greenStars = 1;
        }

        Component stars = null;
        if (playtimeStars) {
            long firstPlayed = getJoined(player);
            int yellowStars = firstPlayed == 0 ? 0 : (int) LocalDateTime.ofInstant(Instant.ofEpochMilli(firstPlayed), ZoneId.systemDefault()).until(LocalDateTime.now(), ChronoUnit.YEARS);
            yellowStars = Math.max(0, yellowStars - greenStars);
            if (yellowStars > 0) {
                stars = Component.text(STAR.repeat(yellowStars), NamedTextColor.YELLOW);
            }
        }
        if (greenStars > 0) {
            Component green = Component.text(STAR.repeat(greenStars), NamedTextColor.GREEN);
            stars = stars == null ? green : stars.append(green);
        }

        return stars;
    }

    /** Legacy-serialized prefix for PlaceholderAPI, which can only return a String. */
    public String getLegacyPrefix(Player player) {
        return LegacyComponentSerializer.legacySection().serialize(getPrefix(player));
    }
}
