package com.github.igotyou.FactoryMod.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import com.github.igotyou.FactoryMod.FactoryMod;
import com.github.igotyou.FactoryMod.FactoryModManager;
import com.github.igotyou.FactoryMod.factories.Factory;
import com.github.igotyou.FactoryMod.factories.FurnCraftChestFactory;
import com.github.igotyou.FactoryMod.recipes.IRecipe;
import com.github.igotyou.FactoryMod.recipes.ProductionRecipe;
import java.util.List;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import com.github.igotyou.FactoryMod.utility.FMText;

public class CheatOutput extends BaseCommand {

    @CommandAlias("fmco")
    @CommandPermission("fm.op")
    @Description("Gives you the output of the selected recipe in the factory you are looking at")
    public void execute(Player sender) {
        Set<Material> transparent = null;
        List<Block> view = ((Player) sender).getLineOfSight(transparent, 10);
        FactoryModManager manager = FactoryMod.getInstance().getManager();
        Factory exis = manager.getFactoryAt(view.get(view.size() - 1));
        if (exis != null && exis instanceof FurnCraftChestFactory) {
            FurnCraftChestFactory fcc = (FurnCraftChestFactory) exis;
            if (fcc.getCurrentRecipe() == null) {
                CivUI.error(sender, "That factory has no recipe selected");
                return;
            }
            IRecipe rec = fcc.getCurrentRecipe();
            if (!(rec instanceof ProductionRecipe)) {
                CivUI.error(sender, FMText.recipe(rec.getName()), " isn't a production recipe");
                return;
            }
            ProductionRecipe prod = (ProductionRecipe) rec;
            for (ItemStack is : prod.getOutput().getItemStackRepresentation()) {
                sender.getInventory().addItem(is);
            }
            CivUI.success(sender, "Gave you the output of ", FMText.recipe(prod.getName()));
        } else {
            CivUI.error(sender, "You're not looking at a factory");
        }
    }
}
