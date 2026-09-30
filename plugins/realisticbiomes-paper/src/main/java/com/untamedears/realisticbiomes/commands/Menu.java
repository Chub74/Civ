package com.untamedears.realisticbiomes.commands;

import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Syntax;
import com.untamedears.realisticbiomes.utils.RealisticBiomesGUI;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;

public class Menu extends BaseCommand {

    @CommandAlias("rb|rbmenu|plants")
    @Syntax("[biome]")
    @Description("Opens a GUI allowing you to browse RealisticBiomes growth rates for current biome")
    @CommandCompletion("@RB_Biomes")
    public void onCommand(Player p, @Optional String biome) {
        if (p.isInsideVehicle()) {
            CivUI.error(p, "You can't open this in a vehicle");
            return;
        }
        if (biome == null) {
            RealisticBiomesGUI gui = new RealisticBiomesGUI(p);
            gui.showRBOverview(null);
        } else {
            if (!p.hasPermission("rb.pickBiome")) {
                CivUI.error(p, "You can only look at the biome you're in");
                return;
            }
            String concat = String.join(" ", biome);
            for (Biome b : RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME)) {
                if (b.getKey().value().equals(concat)) {
                    RealisticBiomesGUI gui = new RealisticBiomesGUI(p);
                    gui.showRBOverview(b);
                    return;
                }
            }
            CivUI.error(p, "There's no biome called ", CivText.value(concat));
        }
    }
}
