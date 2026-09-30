package com.untamedears.jukealert.model.actions.abstr;

import com.untamedears.jukealert.JukeAlert;
import com.untamedears.jukealert.model.Snitch;
import com.untamedears.jukealert.model.actions.ActionCacheState;
import com.untamedears.jukealert.model.actions.LoggedActionPersistence;
import com.untamedears.jukealert.util.JAText;
import com.untamedears.jukealert.util.JAUtility;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import vg.civcraft.mc.civmodcore.CivModCorePlugin;
import vg.civcraft.mc.civmodcore.inventory.gui.ClickableInventory;
import vg.civcraft.mc.civmodcore.inventory.gui.DecorationStack;
import vg.civcraft.mc.civmodcore.inventory.gui.IClickable;
import vg.civcraft.mc.civmodcore.ui.CivMenu;
import vg.civcraft.mc.civmodcore.ui.CivText;

public abstract class LoggablePlayerAction extends PlayerAction implements LoggableAction {

    private ActionCacheState state;
    private int id;

    public LoggablePlayerAction(long time, Snitch snitch, UUID player) {
        super(time, snitch, player);
        state = ActionCacheState.NEW;
    }


    @Override
    public LoggedActionPersistence getPersistence() {
        return new LoggedActionPersistence(player, null, time, null);
    }

    @Override
    public void setID(int id) {
        this.id = id;
        state = ActionCacheState.NORMAL;
    }

    @Override
    public int getID() {
        return id;
    }

    @Override
    public void setCacheState(ActionCacheState state) {
        this.state = state;
    }

    @Override
    public ActionCacheState getCacheState() {
        return state;
    }

    @Override
    public boolean isLifeCycleEvent() {
        return false;
    }

    @Override
    public Component getChatRepresentation(Location reference, boolean live, boolean censor) {
        Location referenceLoc = getLocationForStringRepresentation();
        boolean sameWorld = JAUtility.isSameWorld(referenceLoc, reference);
        Component referenceLocText = censor
            ? CivText.value("[*** *** ***]")
            : JAText.location(referenceLoc, !sameWorld);

        // Client mods parse these lines, so the plain text and its double spaces must stay as they are
        TextComponent.Builder line = Component.text().color(CivText.INFO)
            .append(getChatIdentifier())
            .append(Component.text("  "))
            .append(JAText.player(getPlayer()))
            .append(Component.text("  "));
        if (live) {
            line.append(JAText.snitch(snitch))
                .append(Component.text("  "))
                .append(referenceLocText);
        } else {
            //dont need to explicitly list location when retrieving logs and its the snitch location
            if (referenceLoc != snitch.getLocation()) {
                line.append(referenceLocText).append(Component.space());
            }
            // Example: 2011-12-03T10:15:30
            line.append(CivText.value(getFormattedTime(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
        }
        return line.build();
    }

    /** The action's name at the start of a chat line */
    protected Component getChatIdentifier() {
        return Component.text(getChatRepresentationIdentifier(), CivText.HEADING);
    }

    protected void enrichGUIItem(ItemStack item) {
        if (item.getType().isAir()) {
            JukeAlert.getInstance().getLogger().info("Tried to enrich air");
            item = new ItemStack(Material.STONE);
        }
        // A custom name, not ITEM_NAME: player heads and potions name themselves, and that beats ITEM_NAME
        item.setData(DataComponentTypes.CUSTOM_NAME, Component.text(getGUIName(), CivText.VALUE)
            .decoration(TextDecoration.ITALIC, false));
        CivMenu.lore(item,
            CivMenu.stat("Player", JAText.player(getPlayer())),
            // Example: Tue, 3 Jun 2008 11:05:30 GMT
            CivMenu.stat("Time", getFormattedTime(DateTimeFormatter.RFC_1123_DATE_TIME)));
    }

    protected String getGUIName() {
        return getChatRepresentationIdentifier();
    }

    protected Location getLocationForStringRepresentation() {
        return snitch.getLocation();
    }

    protected IClickable getEnrichedClickableSkullFor(UUID uuid) {
        CompletableFuture<ItemStack> itemReadyFuture = new CompletableFuture<>();
        ItemStack is = CivModCorePlugin.getInstance().getSkinCache().getHeadItem(getPlayer(),
            () -> new ItemStack(Material.PLAYER_HEAD),
            itemReadyFuture::complete);
        enrichGUIItem(is);
        return new DecorationStack(is) {
            @Override
            public void addedToInventory(ClickableInventory inv, int slot) {
                itemReadyFuture.thenAccept(newItem -> {
                    LoggablePlayerAction.this.enrichGUIItem(newItem);
                    this.item = newItem;
                    inv.setItem(newItem, slot);
                });
                super.addedToInventory(inv, slot);
            }
        };
    }
}
