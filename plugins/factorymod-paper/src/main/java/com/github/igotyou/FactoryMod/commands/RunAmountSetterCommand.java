package com.github.igotyou.FactoryMod.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Syntax;
import com.github.igotyou.FactoryMod.FactoryMod;
import com.github.igotyou.FactoryMod.FactoryModManager;
import com.github.igotyou.FactoryMod.factories.Factory;
import com.github.igotyou.FactoryMod.factories.FurnCraftChestFactory;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.civmodcore.ui.CivText;
import com.github.igotyou.FactoryMod.utility.FMText;

public class RunAmountSetterCommand extends BaseCommand {

    @CommandAlias("fmsrc")
    @Syntax("<amount>")
    @Description("Sets the amount of runs for the currently selected recipe in the factory you are looking at")
    @CommandPermission("fm.op")
    public void execute(Player sender, String runCount) {
        int newAmount;
        try {
            newAmount = Integer.parseInt(runCount);
        } catch (NumberFormatException e) {
            CivUI.error(sender, CivText.value(runCount), " isn't a number");
            return;
        }
        FactoryModManager manager = FactoryMod.getInstance().getManager();
        for (Block b : sender.getLineOfSight((Set<Material>) null, 15)) {
            Factory f = manager.getFactoryAt(b);
            if (f instanceof FurnCraftChestFactory) {
                FurnCraftChestFactory fccf = (FurnCraftChestFactory) f;
                fccf.setRunCount(fccf.getCurrentRecipe(), newAmount);
                CivUI.success(sender, "Set the run count of ", FMText.recipe(fccf.getCurrentRecipe().getName()), " in ",
                    FMText.factory(fccf.getName()), " to ", CivText.value(String.valueOf(newAmount)));
            }
        }
    }
}
