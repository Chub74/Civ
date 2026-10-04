package com.programmerdan.minecraft.simpleadminhacks.hacks.basic;

import vg.civcraft.mc.civmodcore.ui.CivUI;
import com.programmerdan.minecraft.simpleadminhacks.framework.utilities.SAHText;
import com.programmerdan.minecraft.simpleadminhacks.SimpleAdminHacks;
import com.programmerdan.minecraft.simpleadminhacks.framework.BasicHack;
import com.programmerdan.minecraft.simpleadminhacks.framework.BasicHackConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.inventory.CraftInventoryCrafting;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.CartographyInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MapCopyProtection extends BasicHack implements CommandExecutor {

    final NamespacedKey copyKey = new NamespacedKey(plugin, "copy-protected");

    public MapCopyProtection(SimpleAdminHacks plugin, BasicHackConfig config) {
        super(plugin, config);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        plugin().registerCommand("copyprotect", this);
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    @EventHandler
    public void onMapCopyCartography(InventoryClickEvent event) {
        if (!(event.getInventory() instanceof CartographyInventory) || event.getSlotType() != InventoryType.SlotType.RESULT)
            return;

        ItemStack item = event.getInventory().getItem(1); // paper, glass pane, blank map slot

        if (item == null) return;

        if (item.getType() == Material.GLASS_PANE || item.getType() == Material.PAPER)
            return; // Allow locking and scaling of copy protected maps

        handleCopyProtection(event);
    }

    @EventHandler
    public void onMapCopyCrafting(InventoryClickEvent event) {
        if (!(event.getInventory() instanceof CraftInventoryCrafting) || event.getSlotType() != InventoryType.SlotType.RESULT)
            return;

        handleCopyProtection(event);
    }

    private void handleCopyProtection(InventoryClickEvent event) {
        ItemStack item = event.getCurrentItem();

        if (item == null || item.getType() == Material.AIR) return;

        if (isCopy(item)) {
            event.setCancelled(true);
            SAHText.deny(event.getWhoClicked(), "You can't copy a copy protected map");
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(
                Component.text()
                    .color(NamedTextColor.RED)
                    .content("This command can only be run by players")
            );
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType() != Material.FILLED_MAP) {
            CivUI.error(sender, "Hold a filled map to copy protect it");
            return true; // Command failed, but was syntactically correct, so no need to tell the player how to use it
        }

        if (isCopy(item)) {
            CivUI.error(sender, "That map is already copy protected");
            return true; // Command failed, but was syntactically correct, so no need to tell the player how to use it
        }

        ItemMeta itemMeta = item.getItemMeta();

        itemMeta.getPersistentDataContainer().set(copyKey, PersistentDataType.INTEGER, 1);

        itemMeta.lore(List.of(Component.text(String.format("Copy protected by %s", sender.getName()))));

        player.getInventory().getItemInMainHand().setItemMeta(itemMeta);

        return true;
    }

    private boolean isCopy(ItemStack item) {
        return item.getItemMeta().getPersistentDataContainer().has(copyKey, PersistentDataType.INTEGER);
    }
}
