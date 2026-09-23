package vg.civcraft.mc.citadel.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.command.CommandSender;
import vg.civcraft.mc.citadel.Citadel;
import vg.civcraft.mc.citadel.activity.ActivityMapStat;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;

@CommandAlias("am")
public class Activity extends BaseCommand {

    @CommandAlias("stat")
    @Description("Shows ActivityMap statistics.")
    @CommandPermission("citadel.admin")
    public void execute(CommandSender sender) {
        ActivityMapStat stat = Citadel.getInstance().getActivityMap().getStat();
        if (stat == null) {
            CivUI.error(sender, "ActivityMap is disabled");
            return;
        }

        TextComponent.Builder body = Component.text()
            .append(CivText.heading("ActivityMap"));
        row(body, "World count", longToStr(stat.worlds));
        row(body, "In-memory loaded regions", longToStr(stat.loadedRegions));
        row(body, "In-memory unloaded regions", longToStr(stat.notLoadedRegions));
        row(body, "In-memory groups", longToStr(stat.uniqueGroups));
        row(body, "In-memory activities", longToStr(stat.loadedActivities));
        row(body, "Regions loaded", longToStr(stat.regionLoadCount));
        row(body, "Regions un-loaded", longToStr(stat.regionUnloadCount));
        row(body, "Total load time", nanoToMsStr(stat.regionLoadSumNano));
        row(body, "Min load time", nanoToMsStr(stat.regionLoadMinTimeNano));
        row(body, "Max load time", nanoToMsStr(stat.regionLoadMaxTimeNano));
        row(body, "Avg load time", nanoToMsStr(stat.regionLoadSumNano / stat.regionLoadCount));

        CivUI.info(sender, body.build());
    }

    private static void row(TextComponent.Builder body, String label, String value) {
        body.append(Component.newline())
            .append(Component.text(label + ": "))
            .append(CivText.value(value));
    }

    private static String longToStr(long i) {
        return String.format("%,d", i);
    }

    private static String nanoToMsStr(long nano) {
        double ms = (double) Math.round(nano / 10000L) / 100.0;
        return String.format("%,.2f ms", ms);
    }
}
