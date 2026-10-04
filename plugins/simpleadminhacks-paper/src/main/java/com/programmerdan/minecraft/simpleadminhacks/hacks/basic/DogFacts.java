package com.programmerdan.minecraft.simpleadminhacks.hacks.basic;

import com.programmerdan.minecraft.simpleadminhacks.SimpleAdminHacks;
import com.programmerdan.minecraft.simpleadminhacks.framework.BasicHack;
import com.programmerdan.minecraft.simpleadminhacks.framework.BasicHackConfig;
import com.programmerdan.minecraft.simpleadminhacks.framework.autoload.AutoLoad;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ObjectComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import vg.civcraft.mc.civmodcore.config.ConfigHelper;
import vg.civcraft.mc.civmodcore.players.settings.PlayerSettingAPI;
import vg.civcraft.mc.civmodcore.players.settings.gui.MenuSection;
import vg.civcraft.mc.civmodcore.players.settings.impl.BooleanSetting;
import vg.civcraft.mc.civmodcore.ui.Bedrock;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.civmodcore.ui.Kind;

public class DogFacts extends BasicHack {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    @AutoLoad
    private List<String> announcements;
    @AutoLoad
    private String intervalTime;
    private BooleanSetting disableAnnouncements;
    private final List<Component> messages = new ArrayList<>();
    private BukkitTask task;
    private int counter = 0;

    public DogFacts(SimpleAdminHacks plugin, BasicHackConfig config) {
        super(plugin, config);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        parseAnnouncements();
        startRunnable();
        initSettings();
    }

    @Override
    public void onDisable() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        super.onDisable();
    }

    /** Parsed once up front, so a broken line is reported at startup rather than every broadcast. */
    private void parseAnnouncements() {
        messages.clear();
        for (int i = 0; i < announcements.size(); i++) {
            try {
                Component body = untintSprites(MM.deserialize(announcements.get(i)));
                messages.add(CivUI.render(Kind.INFO, true, false, body));
            } catch (RuntimeException e) {
                plugin.warning("Skipping DogFact #" + i + ", it isn't valid MiniMessage: " + e.getMessage());
            }
        }
    }

    /** The client multiplies a sprite by its text colour, so one left uncoloured would take the grey body colour. */
    private static Component untintSprites(Component component) {
        List<Component> children = new ArrayList<>(component.children().size());
        for (Component child : component.children()) {
            children.add(untintSprites(child));
        }
        Component result = component.children(children);
        if (result instanceof ObjectComponent && result.color() == null) {
            result = result.color(NamedTextColor.WHITE);
        }
        return result;
    }

    private void startRunnable() {
        long interval = ConfigHelper.parseTimeAsTicks(intervalTime);
        long tickOffset = (long) (Math.random() * interval);
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (messages.isEmpty()) {
                return;
            }
            if (counter >= messages.size()) {
                counter = 0;
            }
            Component message = messages.get(counter);
            plugin.info("Broadcasting DogFact #" + counter);
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (getDisableAnnouncements(player.getUniqueId())) {
                    continue;
                }
                player.sendMessage(Bedrock.adapt(player, message));
            }
            counter++;
        }, tickOffset, interval);
    }

    private void initSettings() {
        MenuSection menu = plugin.getSettingManager().getMainMenu();
        disableAnnouncements =
            new BooleanSetting(plugin, false, "Disable Announcements", "disableAnnouncements",
                "Disable Announcements?");
        PlayerSettingAPI.registerSetting(disableAnnouncements, menu);
    }

    public boolean getDisableAnnouncements(UUID uuid) {
        return disableAnnouncements.getValue(uuid);
    }
}
