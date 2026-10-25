package vg.civcraft.mc.civmodcore.players.settings.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import vg.civcraft.mc.civmodcore.commands.TabComplete;
import vg.civcraft.mc.civmodcore.players.settings.AdminSetting;
import vg.civcraft.mc.civmodcore.players.settings.AdminSettingAPI;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;

@CommandAlias("adminconfig")
@CommandPermission("cmc.adminconfig")
public final class AdminConfigCommand extends BaseCommand {

    @Default
    @Description("Opens the GUI for plugin settings which can be changed at runtime")
    public void openConfig(final Player sender) {
        AdminSettingAPI.getMainMenu().showScreen(sender);
    }

    @Subcommand("list")
    @Description("Lists admin settings, optionally only those of one plugin")
    @Syntax("[plugin]")
    @CommandCompletion("@adminsettingplugins")
    public void list(final CommandSender sender, @Optional final String plugin) {
        final List<AdminSetting<?>> settings = new ArrayList<>(INTERNAL_settingsOf(plugin));
        if (settings.isEmpty()) {
            CivUI.error(sender, "No admin settings found");
            return;
        }
        settings.sort((a, b) -> a.getIdentifier().compareToIgnoreCase(b.getIdentifier()));
        sender.sendMessage(CivText.heading("Admin settings"));
        for (final AdminSetting<?> setting : settings) {
            CivUI.info(sender, CivText.subject(setting.getIdentifier()), " = ", CivText.value(setting.getValueText()),
                INTERNAL_unflushedNote(setting));
        }
    }

    @Subcommand("get")
    @Description("Shows the current value of an admin setting")
    @Syntax("<setting>")
    @CommandCompletion("@adminsettings")
    public void get(final CommandSender sender, final String identifier) {
        final AdminSetting<?> setting = INTERNAL_resolveSetting(sender, identifier);
        if (setting == null) {
            return;
        }
        CivUI.info(sender, CivText.subject(setting.getNiceName()), " is ", CivText.value(setting.getValueText()),
            INTERNAL_unflushedNote(setting));
        CivUI.info(sender, "Default ", CivText.value(setting.getDefaultValueText()), ", stored at ",
            CivText.value(setting.getPath()), " in ", CivText.value(setting.getFileName()));
        if (setting.getDescription() != null) {
            CivUI.info(sender, setting.getDescription());
        }
    }

    @Subcommand("set")
    @Description("Changes the live value of an admin setting, without writing it to the config")
    @Syntax("<setting> <value>")
    @CommandCompletion("@adminsettings")
    public void set(final CommandSender sender, final String identifier, final String value) {
        final AdminSetting<?> setting = INTERNAL_resolveSetting(sender, identifier);
        if (setting == null) {
            return;
        }
        if (!setting.isValidValue(value)) {
            CivUI.error(sender, CivText.value(value), " is not a valid value for ", CivText.subject(identifier));
            return;
        }
        setting.setValueFromString(value);
        CivUI.success(sender, "Set ", CivText.subject(setting.getNiceName()), " to ",
            CivText.value(setting.getValueText()), INTERNAL_unflushedNote(setting));
    }

    @Subcommand("reset")
    @Description("Sets the live value of an admin setting back to its default")
    @Syntax("<setting>")
    @CommandCompletion("@adminsettings")
    public void reset(final CommandSender sender, final String identifier) {
        final AdminSetting<?> setting = INTERNAL_resolveSetting(sender, identifier);
        if (setting == null) {
            return;
        }
        setting.reset();
        CivUI.success(sender, "Reset ", CivText.subject(setting.getNiceName()), " to ",
            CivText.value(setting.getValueText()), INTERNAL_unflushedNote(setting));
    }

    @Subcommand("flush")
    @Description("Writes unflushed changes to the config files, optionally only those of one plugin")
    @Syntax("[plugin]")
    @CommandCompletion("@adminsettingplugins")
    public void flush(final CommandSender sender, @Optional final String plugin) {
        final int flushed = AdminSettingAPI.flush(INTERNAL_settingsOf(plugin));
        CivUI.success(sender, "Flushed ", CivText.value(String.valueOf(flushed)), " admin settings to config");
    }

    @Subcommand("discard")
    @Description("Sets unflushed changes back to their config values, optionally only those of one plugin")
    @Syntax("[plugin]")
    @CommandCompletion("@adminsettingplugins")
    public void discard(final CommandSender sender, @Optional final String plugin) {
        final int discarded = AdminSettingAPI.discard(INTERNAL_settingsOf(plugin));
        CivUI.success(sender, "Discarded ", CivText.value(String.valueOf(discarded)), " admin setting changes");
    }

    @Subcommand("reload")
    @Description("Re-reads a plugin's admin settings from its config files, discarding unflushed changes")
    @Syntax("<plugin>")
    @CommandCompletion("@adminsettingplugins")
    public void reload(final CommandSender sender, final String plugin) {
        final Plugin found = Bukkit.getPluginManager().getPlugin(plugin);
        if (found == null || AdminSettingAPI.getSettings(found.getName()).isEmpty()) {
            CivUI.error(sender, "No admin settings found for ", CivText.subject(plugin));
            return;
        }
        AdminSettingAPI.reload(found);
        CivUI.success(sender, "Reloaded admin settings of ", CivText.subject(found.getName()));
    }

    @TabComplete("adminsettings")
    public List<String> completeSettings() {
        return AdminSettingAPI.getAllSettings().stream().map(AdminSetting::getIdentifier).sorted().toList();
    }

    @TabComplete("adminsettingplugins")
    public List<String> completePlugins() {
        return AdminSettingAPI.getPluginNames().stream().sorted().toList();
    }

    private static Collection<AdminSetting<?>> INTERNAL_settingsOf(final String plugin) {
        return plugin == null ? AdminSettingAPI.getAllSettings() : AdminSettingAPI.getSettings(plugin);
    }

    private static String INTERNAL_unflushedNote(final AdminSetting<?> setting) {
        return setting.hasUnflushedChange() ? " (unflushed, config has " + setting.getFlushedValueText() + ")" : "";
    }

    private static AdminSetting<?> INTERNAL_resolveSetting(final CommandSender sender, final String identifier) {
        final AdminSetting<?> setting = AdminSettingAPI.getSetting(identifier);
        if (setting == null) {
            CivUI.error(sender, "Could not find admin setting ", CivText.subject(identifier));
        }
        return setting;
    }

}
