package com.github.igotyou.FactoryMod.interactionManager;

import com.github.igotyou.FactoryMod.FactoryMod;
import com.github.igotyou.FactoryMod.eggs.FurnCraftChestEgg;
import com.github.igotyou.FactoryMod.factories.FurnCraftChestFactory;
import com.github.igotyou.FactoryMod.powerManager.FurnacePowerManager;
import com.github.igotyou.FactoryMod.recipes.IRecipe;
import com.github.igotyou.FactoryMod.recipes.InputRecipe;
import com.github.igotyou.FactoryMod.recipes.ProductionRecipe;
import com.github.igotyou.FactoryMod.recipes.upgrade.CharcoalConsumptionUpgradeRecipe;
import com.github.igotyou.FactoryMod.recipes.upgrade.SpeedUpgradeRecipe;
import com.github.igotyou.FactoryMod.repairManager.PercentageHealthRepairManager;
import com.github.igotyou.FactoryMod.structures.FurnCraftChestStructure;
import com.github.igotyou.FactoryMod.structures.MultiBlockStructure;
import com.github.igotyou.FactoryMod.utility.FactoryModGUI;
import com.github.igotyou.FactoryMod.utility.IOConfigSection;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Furnace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import vg.civcraft.mc.citadel.ReinforcementLogic;
import vg.civcraft.mc.citadel.model.Reinforcement;
import vg.civcraft.mc.civmodcore.inventory.gui.Clickable;
import vg.civcraft.mc.civmodcore.inventory.gui.ClickableInventory;
import vg.civcraft.mc.civmodcore.inventory.gui.IClickable;
import vg.civcraft.mc.civmodcore.inventory.gui.components.ComponableInventory;
import vg.civcraft.mc.civmodcore.inventory.gui.components.Scrollbar;
import vg.civcraft.mc.civmodcore.inventory.gui.components.SlotPredicates;
import vg.civcraft.mc.civmodcore.inventory.gui.components.StaticDisplaySection;
import vg.civcraft.mc.civmodcore.inventory.items.ItemMap;
import vg.civcraft.mc.civmodcore.inventory.items.ItemUtils;
import vg.civcraft.mc.namelayer.NameLayerAPI;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.permission.PermissionType;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.Kind;
import com.github.igotyou.FactoryMod.utility.FMText;

public class FurnCraftChestInteractionManager implements IInteractionManager {

    private FurnCraftChestFactory fccf;
    private final DecimalFormat decimalFormatting;

    public FurnCraftChestInteractionManager(FurnCraftChestFactory fccf) {
        this();
        this.fccf = fccf;
    }

    public FurnCraftChestInteractionManager() {
        this.decimalFormatting = new DecimalFormat("#.#####");
    }

    public void setFactory(FurnCraftChestFactory fccf) {
        this.fccf = fccf;
    }

    @Override
    public void redStoneEvent(BlockRedstoneEvent e, Block factoryBlock) {
        int threshold = FactoryMod.getInstance().getManager().getRedstonePowerOn();
        if (!(factoryBlock.getLocation().equals(fccf.getFurnace().getLocation()) && e.getOldCurrent() >= threshold
            && e.getNewCurrent() < threshold)) {
            return;
        }
        if (FactoryMod.getInstance().getManager().isCitadelEnabled()) {
            if (!MultiBlockStructure.citadelRedstoneChecks(e.getBlock())) {
                return;
            }
        }
        if (fccf.isActive()) {
            fccf.deactivate();
        } else {
            fccf.attemptToActivate(null, false);
        }
    }

    @Override
    public void blockBreak(Player p, Block b) {
        if (p != null && !fccf.getRepairManager().inDisrepair()) {
            CivUI.notice(Kind.WARN, "You broke ", FMText.factory(fccf.getName()), ". It's in disrepair until repaired").icon().to(p).send();
        }
        if (fccf.isActive()) {
            fccf.deactivate();
        }
        fccf.getRepairManager().breakIt();
    }

    @Override
    public void leftClick(Player p, Block b, BlockFace bf) {
        if (p.getInventory().getItemInMainHand().getType() != FactoryMod.getInstance().getManager().getFactoryInteractionMaterial()) {
            return;
        }
        if (FactoryMod.getInstance().getManager().isCitadelEnabled()) {
            Reinforcement rein = ReinforcementLogic.getReinforcementProtecting(b);
            if (rein != null) {
                Group g = rein.getGroup();
                if (!NameLayerAPI.getGroupManager().hasAccess(g.getName(), p.getUniqueId(),
                    PermissionType.getPermission("USE_FACTORY"))) {
                    FMText.deny(p, "You don't have permission to use ", FMText.factory(fccf.getName()));
                    return;
                }
            }
        }
        if (b.equals(((FurnCraftChestStructure) fccf.getMultiBlockStructure()).getChest())) { // chest interaction
            if (p.isSneaking() && fccf.getCurrentRecipe() instanceof InputRecipe) { // sneaking, so showing detailed recipe stuff
                ClickableInventory ci = new ClickableInventory(54, fccf.getCurrentRecipe().getName());
                int index = 4;
                List<ItemStack> inp = ((InputRecipe) fccf.getCurrentRecipe())
                    .getInputRepresentation(fccf.getInputInventory(), fccf);
                if (inp.size() > 18) {
                    inp = new ItemMap(inp).getLoredItemCountRepresentation();
                }
                for (ItemStack is : inp) {
                    Clickable c = new Clickable(is) {
                        @Override
                        public void clicked(Player arg0) {
                            // nothing, just supposed to look nice
                        }
                    };
                    ci.setSlot(c, index);
                    // weird math to fill up the gui nicely
                    if ((index % 9) == 4) {
                        index++;
                        continue;
                    }
                    if ((index % 9) > 4) {
                        index -= (((index % 9) - 4) * 2);
                    } else {
                        if ((index % 9) == 0) {
                            index += 13;
                        } else {
                            index += (((4 - (index % 9)) * 2) + 1);
                        }
                    }

                }
                index = 49;
                List<ItemStack> outp = ((InputRecipe) fccf.getCurrentRecipe())
                    .getOutputRepresentation(fccf.getOutputInventory(), fccf);
                if (outp.size() > 18) {
                    outp = new ItemMap(outp).getLoredItemCountRepresentation();
                }
                for (ItemStack is : outp) {
                    Clickable c = new Clickable(is) {
                        @Override
                        public void clicked(Player arg0) {
                            // nothing, just supposed to look nice
                        }
                    };
                    ci.setSlot(c, index);
                    if ((index % 9) == 4) {
                        index++;
                        continue;
                    }
                    if ((index % 9) > 4) {
                        index -= (((index % 9) - 4) * 2);
                    } else {
                        if ((index % 9) == 0) {
                            index -= 13;
                        } else {
                            index += (((4 - (index % 9)) * 2) + 1);
                        }
                    }

                }
                ci.showInventory(p);

            } else { // not sneaking, so just a short sumup
                List<Component> lines = new ArrayList<>();
                lines.add(Component.text().append(CivText.info("Running ")).append(CivText.toggle(fccf.isActive())).build());
                if (fccf.isActive()) {
                    long ticksLeft = fccf.getCurrentRecipe().getProductionTime() - fccf.getRunningTime();
                    lines.add(Component.text().append(CivText.info("Run finishes in "))
                        .append(CivText.value(formatDuration(ticksLeft * 50L))).build());
                }
                lines.add(Component.text().append(CivText.info("Recipe "))
                    .append(FMText.recipe(fccf.getCurrentRecipe().getName())).build());
                lines.add(Component.text().append(CivText.info("Health "))
                    .append(CivText.value(String.valueOf(fccf.getRepairManager().getHealth()))).build());
                if (fccf.getRepairManager() instanceof PercentageHealthRepairManager) {
                    PercentageHealthRepairManager rm = (PercentageHealthRepairManager) fccf.getRepairManager();
                    if (rm.inDisrepair()) {
                        long leftTime = rm.getGracePeriod() - (System.currentTimeMillis() - rm.getBreakTime());
                        lines.add(Component.text().append(Component.text("Breaks for good in ", CivText.WARN))
                            .append(CivText.value(formatDuration(leftTime))).build());
                    } else {
                        long timeUntilBreak = rm.getEstimatedMillisUntilBreak();
                        if (timeUntilBreak < 0) {
                            lines.add(CivText.info("Doesn't decay"));
                        } else {
                            double healthFraction = (double) rm.getRawHealth() / rm.getMaximumHealth();
                            TextColor timeColor = healthFraction >= (2.0 / 3.0) ? CivText.SUCCESS
                                : healthFraction >= (1.0 / 3.0) ? CivText.WARN : CivText.ERROR;
                            lines.add(Component.text().append(CivText.info("Breaks in about "))
                                .append(Component.text(formatDuration(timeUntilBreak), timeColor))
                                .append(CivText.info(" at the current decay rate")).build());
                        }
                    }
                }
                p.sendMessage(FMText.list(CivText.heading(fccf.getName()), lines));
            }

            return;
        }
        if (b.equals(((FurnCraftChestStructure) fccf.getMultiBlockStructure()).getCraftingTable())) { // crafting table
            // interaction
            ComponableInventory compInv = buildRecipeInventory(p);
            compInv.update();
            compInv.updatePlayerView();
            return;
        }
        if (b.equals(fccf.getFurnace())) { // furnace interaction
            if (fccf.isActive()) {
                fccf.deactivate();
                FMText.bar(p, Kind.SUCCESS, "Stopped ", FMText.factory(fccf.getName()));
            } else {
                fccf.attemptToActivate(p, false);
            }
        }
    }

    /**
     * Formats a duration in milliseconds as "X months, Y days, Z hours", omitting any unit that's zero.
     */
    private static String formatDuration(long millis) {
        long months = millis / (60L * 60L * 24L * 30L * 1000L);
        long days = (millis - (months * 60L * 60L * 24L * 30L * 1000L)) / (60L * 60L * 24L * 1000L);
        long hours = (millis - (months * 60L * 60L * 24L * 30L * 1000L)
            - (days * 60L * 60L * 24L * 1000L)) / (60L * 60L * 1000L);
        String time = (months != 0 ? months + " months, " : "") + (days != 0 ? days + " days, " : "")
            + (hours != 0 ? hours + " hours" : "");
        return time.isEmpty() ? "less than an hour" : time;
    }

    private ComponableInventory buildRecipeInventory(Player p) {
        ComponableInventory compInv = new ComponableInventory("Select a recipe", 6, p);

        boolean upgrades = FactoryMod.getInstance().getManager().canUpgrade();
        Clickable upgradeSpeed = upgrades ? buildUpgradeSpeed() : null;
        Clickable upgradeCharcoalConsumption = upgrades ? buildUpgradeCharcoalConsumption() : null;
        Clickable autoClick = buildAutoSelectToggle();
        Clickable menuC = buildMenuClickable();
        Clickable menuModeButton = buildMenuModeCycleButton(p);
        Clickable[] buddons = new Clickable[]{upgradeSpeed, upgradeCharcoalConsumption, autoClick, menuC, menuModeButton};
        StaticDisplaySection lowerSection = new StaticDisplaySection(buddons);

        Block fblock = fccf.getFurnace();
        switch (fccf.getUiMenuMode()) {
            case IOCONFIG: {
                Scrollbar recipeScroller = buildRecipeScrollbar(3);
                compInv.addComponent(recipeScroller, SlotPredicates.offsetRectangle(3, 9, 0, 0));
                if (fblock.getType() == Material.FURNACE) {
                    Furnace fstate = (Furnace) fblock.getState();
                    org.bukkit.block.data.type.Furnace fdata = (org.bukkit.block.data.type.Furnace) fstate.getBlockData();
                    BlockFace facing = fdata.getFacing();
                    IOConfigSection furnaceConfigSection = new IOConfigSection(
                        p,
                        fccf.getFurnaceIOSelector(),
                        Material.FURNACE,
                        fblock,
                        facing,
                        fccf);
                    IOConfigSection tableConfigSection = new IOConfigSection(
                        p,
                        fccf.getTableIOSelector(),
                        Material.CRAFTING_TABLE,
                        ((FurnCraftChestStructure) fccf.getMultiBlockStructure()).getCraftingTable(),
                        facing,
                        fccf);
                    compInv.addComponent(furnaceConfigSection, SlotPredicates.offsetRectangle(3, 3, 3, 0));
                    compInv.addComponent(tableConfigSection, SlotPredicates.offsetRectangle(3, 3, 3, 4));
                }
                compInv.addComponent(lowerSection, SlotPredicates.offsetRectangle(5, 2, 3, 7));
                break;
            }
            default: {
                Scrollbar recipeScroller = buildRecipeScrollbar(5);
                compInv.addComponent(recipeScroller, SlotPredicates.offsetRectangle(5, 9, 0, 0));
                compInv.addComponent(lowerSection, SlotPredicates.offsetRectangle(1, 5, 5, 4));
            }
        }
        return compInv;
    }

    private Scrollbar buildRecipeScrollbar(int rows) {
        rows = Math.max(1, Math.min(5, rows));
        List<IRecipe> recipeList = fccf.getRecipes();
        List<IClickable> recipeClickList = new ArrayList<>(recipeList.size());
        for (IRecipe rec : fccf.getRecipes()) {
            InputRecipe recipe = (InputRecipe) (rec);
            ItemStack recStack = recipe.getRecipeRepresentation(fccf.getInputInventory());
            int runcount = fccf.getRunCount(recipe);
            FMText.lore(recStack, Component.empty(), Component.text().append(CivText.info("Ran ")).append(CivText.value(String.valueOf(runcount))).append(CivText.info(" times")).build());
            if (rec == fccf.getCurrentRecipe()) {
                FMText.lore(recStack, Component.text("Currently selected", CivText.SUCCESS));
                recStack.editMeta(itemMeta -> itemMeta.setEnchantmentGlintOverride(true));
            }
            if (recipe instanceof ProductionRecipe) {
                ProductionRecipe prod = (ProductionRecipe) recipe;
                if (prod.getModifier() != null) {
                    FMText.lore(recStack, Component.text().append(CivText.info("Level ")).append(CivText.value(fccf.getRecipeLevel(recipe) + " ★")).build());
                    FMText.lore(recStack, Component.text().append(CivText.info("Output multiplier ")).append(CivText.value(decimalFormatting.format(prod.getModifier().getFactor(fccf.getRecipeLevel(recipe), runcount)))).build());
                }
            }
            Clickable c = new Clickable(recStack) {

                @Override
                public void clicked(Player p) {
                    if (fccf.isActive()) {
                        FMText.bar(p, Kind.ERROR, "You can't switch recipes while ", FMText.factory(fccf.getName()), " is running");
                    } else {
                        fccf.setRecipe(recipe);
                        FMText.bar(p, Kind.SUCCESS, "Switched ", FMText.factory(fccf.getName()), " to ", FMText.recipe(recipe.getName()));
                        ComponableInventory compInv = buildRecipeInventory(p);
                        compInv.update();
                        compInv.updatePlayerView();
                    }
                }
            };
            recipeClickList.add(c);
        }
        Scrollbar recipeScroller = new Scrollbar(recipeClickList, rows * 9);
        recipeScroller.setBackwardsClickSlot(rows == 1 ? 0 : 8);
        return recipeScroller;
    }

    private static final String[] CHARCOAL_STAGES = new String[] { "1", "1/4", "1/8", "1/12", "1/16"};

    private Clickable buildUpgradeCharcoalConsumption() {
        ItemStack upgradeCharcoal = new ItemStack(Material.REDSTONE_TORCH);
        ItemMeta meta = upgradeCharcoal.getItemMeta();
        meta.displayName(FMText.label("Upgrade charcoal consumption"));
        if (fccf.getSpeedLevel() > 0) {
            meta.lore(List.of(Component.text("Cannot be upgraded", CivText.ERROR).decoration(TextDecoration.ITALIC, false)));
        } else if (fccf.getCharcoalLevel() == CHARCOAL_STAGES.length - 1) {
            meta.lore(List.of(Component.empty().decoration(TextDecoration.ITALIC, false)
                .append(Component.text(CHARCOAL_STAGES[fccf.getCharcoalLevel()], CivText.VALUE))
                .append(Component.text(" Max level!", CivText.INFO))));
        } else if (((FurnacePowerManager) fccf.getPowerManager()).getFuel().getType() != Material.CHARCOAL) {
            meta.lore(List.of(Component.text("This factory does not use charcoal", CivText.INFO).decoration(TextDecoration.ITALIC, false)));
        } else {
            meta.lore(List.of(
                Component.empty().decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(CHARCOAL_STAGES[fccf.getCharcoalLevel()], CivText.VALUE))
                    .append(Component.text(" -> ", CivText.INFO))
                    .append(Component.text(CHARCOAL_STAGES[fccf.getCharcoalLevel() + 1], CivText.VALUE)),
                Component.text("Requires 1 Factory Upgrade", CivText.INFO).decoration(TextDecoration.ITALIC, false),
                Component.text("Mutually exclusive with speed upgrade", CivText.WARN).decoration(TextDecoration.ITALIC, false),
                Component.text("Not refundable", CivText.WARN).decoration(TextDecoration.ITALIC, false)
            ));
        }
        meta.setEnchantmentGlintOverride(true);
        upgradeCharcoal.setItemMeta(meta);

        return new Clickable(upgradeCharcoal) {
            @Override
            protected void clicked(Player p) {
                if (fccf.getCharcoalLevel() == CHARCOAL_STAGES.length - 1 || fccf.getSpeedLevel() > 0 || ((FurnacePowerManager) fccf.getPowerManager()).getFuel().getType() != Material.CHARCOAL) {
                    return;
                }

                if (fccf.isActive()) {
                    FMText.bar(p, Kind.ERROR, "You can't switch recipes while ", FMText.factory(fccf.getName()), " is running");
                } else {
                    fccf.setRecipeForce(FactoryMod.getInstance().getManager().getRecipe(CharcoalConsumptionUpgradeRecipe.IDENTIFIER));
                    FMText.bar(p, Kind.SUCCESS, "Switched ", FMText.factory(fccf.getName()), " to ", FMText.recipe(fccf.getCurrentRecipe().getName()));
                }
            }
        };
    }

    private static final String[] SPEED_STAGES = new String[] { "x1", "x2", "x3", "x4", "x5"};

    private Clickable buildUpgradeSpeed() {
        ItemStack upgradeSpeed = new ItemStack(Material.SOUL_TORCH);
        ItemMeta meta = upgradeSpeed.getItemMeta();
        meta.displayName(FMText.label("Upgrade speed"));
        if (fccf.getCharcoalLevel() > 0) {
            meta.lore(List.of(Component.text("Cannot be upgraded", CivText.ERROR).decoration(TextDecoration.ITALIC, false)));
        } else if (fccf.getSpeedLevel() == SPEED_STAGES.length - 1) {
            meta.lore(List.of(Component.empty().decoration(TextDecoration.ITALIC, false)
                .append(Component.text(SPEED_STAGES[fccf.getSpeedLevel()], CivText.VALUE))
                .append(Component.text(" Max level!", CivText.INFO))));
        } else {
            meta.lore(List.of(
                Component.empty().decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(SPEED_STAGES[fccf.getSpeedLevel()], CivText.VALUE))
                    .append(Component.text(" -> ", CivText.INFO))
                    .append(Component.text(SPEED_STAGES[fccf.getSpeedLevel() + 1], CivText.VALUE)),
                Component.text("Requires 1 Factory Upgrade", CivText.INFO).decoration(TextDecoration.ITALIC, false),
                Component.text("Mutually exclusive with charcoal upgrade", CivText.WARN).decoration(TextDecoration.ITALIC, false),
                Component.text("Not refundable", CivText.WARN).decoration(TextDecoration.ITALIC, false)
            ));
        }
        meta.setEnchantmentGlintOverride(true);
        upgradeSpeed.setItemMeta(meta);

        return new Clickable(upgradeSpeed) {
            @Override
            protected void clicked(Player p) {
                if (fccf.getSpeedLevel() == SPEED_STAGES.length - 1 || fccf.getCharcoalLevel() > 0) {
                    return;
                }

                if (fccf.isActive()) {
                    FMText.bar(p, Kind.ERROR, "You can't switch recipes while ", FMText.factory(fccf.getName()), " is running");
                } else {
                    fccf.setRecipeForce(FactoryMod.getInstance().getManager().getRecipe(SpeedUpgradeRecipe.IDENTIFIER));
                    FMText.bar(p, Kind.SUCCESS, "Switched ", FMText.factory(fccf.getName()), " to ", FMText.recipe(fccf.getCurrentRecipe().getName()));
                }
            }
        };
    }

    private Clickable buildAutoSelectToggle() {
        ItemStack autoSelectStack = new ItemStack(Material.REDSTONE_BLOCK);
        FMText.name(autoSelectStack, Component.text("Auto-select"));
        FMText.lore(autoSelectStack, CivText.toggle(fccf.isAutoSelect()), Component.empty(), CivText.info("Picks a recipe the factory can run"), CivText.info("whenever you start it"), Component.empty(), FMText.hint("Click to turn it " + (fccf.isAutoSelect() ? "off" : "on")));
        Clickable autoClick = new Clickable(autoSelectStack) {

            @Override
            public void clicked(Player p) {
                fccf.setAutoSelect(!fccf.isAutoSelect());
                FMText.bar(p, Kind.SUCCESS, "Auto-select for ", FMText.factory(fccf.getName()), " ", CivText.toggle(fccf.isAutoSelect()));
            }
        };
        return autoClick;
    }

    private Clickable buildMenuClickable() {
        ItemStack menuStack = new ItemStack(Material.PAINTING);
        FMText.name(menuStack, Component.text("Menu"));
        FMText.lore(menuStack, FMText.hint("Click to open the factory menu"));
        Clickable menuC = new Clickable(menuStack) {
            @Override
            public void clicked(Player p) {
                FactoryModGUI gui = new FactoryModGUI(p);
                gui.showForFactory((FurnCraftChestEgg) FactoryMod.getInstance().getManager().getEgg(fccf.getName()));
            }
        };
        return menuC;
    }

    private Clickable buildMenuModeCycleButton(Player p) {
        FurnCraftChestFactory.UiMenuMode[] modes = FurnCraftChestFactory.UiMenuMode.values();
        FurnCraftChestFactory.UiMenuMode curMode = fccf.getUiMenuMode();
        FurnCraftChestFactory.UiMenuMode nextMode = modes[(curMode.ordinal() + 1) % modes.length];
        ItemStack display = new ItemStack(nextMode.uiMaterial);
        ItemUtils.setComponentDisplayName(display, Component.text(nextMode.uiDescription));

        Clickable menuModeButton = new Clickable(display) {
            private ClickableInventory inventory;
            private int slot;

            @Override
            protected void clicked(Player player) {
                cycleMenuMode();
                ComponableInventory compInv = buildRecipeInventory(p);
                compInv.update();
                compInv.updatePlayerView();
            }

            @Override
            public void addedToInventory(ClickableInventory inv, int slot) {
                this.inventory = inv;
                this.slot = slot;
            }

            private void cycleMenuMode() {
                FurnCraftChestFactory.UiMenuMode innerCurMode = fccf.getUiMenuMode();
                innerCurMode = modes[(innerCurMode.ordinal() + 1) % modes.length];
                fccf.setUiMenuMode(innerCurMode);

                ItemStack innerCurStack = getItemStack();
                FurnCraftChestFactory.UiMenuMode innerNextMode = modes[(innerCurMode.ordinal() + 1) % modes.length];
                innerCurStack.setType(innerNextMode.uiMaterial);
                ItemUtils.setComponentDisplayName(innerCurStack, Component.text(innerNextMode.uiDescription));

                if (inventory != null && inventory.getSlot(slot) == this) {
                    inventory.setSlot(this, slot);
                }
            }
        };
        return menuModeButton;
    }

    @Override
    public void rightClick(Player p, Block b, BlockFace bf) {
        // Nothing to do here, every block already has a right click
        // functionality
    }

}
