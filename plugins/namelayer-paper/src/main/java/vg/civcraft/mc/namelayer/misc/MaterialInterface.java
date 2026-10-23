package vg.civcraft.mc.namelayer.misc;


import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public interface MaterialInterface {

    public enum Specific {
        GREEN, RED, MOD, BLACKLIST, PERMS, MERGE, DEFAULT;
    }

    Material getMaterial(Specific specific);

    ItemStack getItemStack(Specific specific);
}

