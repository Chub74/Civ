package vg.civcraft.mc.civmodcore.players.settings.impl;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import vg.civcraft.mc.civmodcore.config.ConfigHelper;
import vg.civcraft.mc.civmodcore.players.settings.PlayerSetting;

/**
 * A length of time, written the way {@link ConfigHelper#parseTime(String)} reads it, such as "20h", "1d12h",
 * "30 minutes" or "week". Values are saved in the shortest form, such as "1d12h"
 */
public class DurationSetting extends PlayerSetting<Duration> {

    private static final Pattern PART = Pattern.compile("(\\d*)([a-z]+)");
    // Every unit ConfigHelper.parseTime understands, it silently ignores anything else
    private static final Set<String> UNITS = Set.of("ms", "milli", "millis", "s", "sec", "second", "seconds", "m",
        "min", "minute", "minutes", "h", "hour", "hours", "d", "day", "days", "w", "week", "weeks", "month", "months",
        "y", "year", "years", "never", "inf", "infinite", "perm", "perma", "forever");

    public DurationSetting(JavaPlugin owningPlugin, Duration defaultValue, String name, String identifier,
                           ItemStack gui, String description) {
        super(owningPlugin, defaultValue, name, identifier, gui, description, true);
    }

    @Override
    public Duration deserialize(String serial) {
        return Duration.ofMillis(ConfigHelper.parseTime(serial));
    }

    @Override
    public boolean isValidValue(String input) {
        if (input == null) {
            return false;
        }
        String normalized = input.replace(" ", "").replace(",", "").toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return false;
        }
        // A plain number is milliseconds
        if (normalized.chars().allMatch(Character::isDigit)) {
            return normalized.length() <= 18;
        }
        Matcher matcher = PART.matcher(normalized);
        int end = 0;
        while (matcher.find()) {
            if (matcher.start() != end || !UNITS.contains(matcher.group(2)) || matcher.group(1).length() > 18) {
                return false;
            }
            end = matcher.end();
        }
        return end == normalized.length();
    }

    @Override
    public String serialize(Duration value) {
        return format(value, "");
    }

    @Override
    public String toText(Duration value) {
        return format(value, " ");
    }

    private static String format(Duration value, String separator) {
        long millis = value.toMillis();
        if (millis == 0) {
            return "0s";
        }
        List<String> parts = new ArrayList<>();
        millis = append(parts, millis, TimeUnit.DAYS.toMillis(1), "d");
        millis = append(parts, millis, TimeUnit.HOURS.toMillis(1), "h");
        millis = append(parts, millis, TimeUnit.MINUTES.toMillis(1), "m");
        millis = append(parts, millis, TimeUnit.SECONDS.toMillis(1), "s");
        append(parts, millis, 1, "ms");
        return String.join(separator, parts);
    }

    private static long append(List<String> parts, long millis, long unitMillis, String unit) {
        long amount = millis / unitMillis;
        if (amount > 0) {
            parts.add(amount + unit);
        }
        return millis % unitMillis;
    }

}
