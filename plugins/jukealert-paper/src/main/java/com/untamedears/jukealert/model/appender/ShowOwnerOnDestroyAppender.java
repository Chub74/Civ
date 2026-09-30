package com.untamedears.jukealert.model.appender;

import com.untamedears.jukealert.model.Snitch;
import com.untamedears.jukealert.model.actions.abstr.SnitchAction;
import com.untamedears.jukealert.model.actions.internal.DestroySnitchAction;
import com.untamedears.jukealert.util.JAText;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.namelayer.group.Group;

public class ShowOwnerOnDestroyAppender extends AbstractSnitchAppender {

    public static final String ID = "showownerondestroy";

    public ShowOwnerOnDestroyAppender(Snitch snitch) {
        super(snitch);
    }

    @Override
    public boolean runWhenSnitchInactive() {
        return true;
    }

    @Override
    public void acceptAction(SnitchAction action) {
        if (!action.isLifeCycleEvent()) {
            return;
        }
        if (!(action instanceof DestroySnitchAction dsa)) {
            return;
        }
        UUID destroyerUUID = dsa.getPlayer();
        if (destroyerUUID == null) {
            return;
        }
        Player player = Bukkit.getPlayer(destroyerUUID);
        if (player == null) {
            return;
        }
        Group group = snitch.getGroup();
        Component type = CivText.subject(snitch.getType().getName());
        if (group == null) {
            CivUI.info(player, "You broke a ", type, " on an unknown group");
            return;
        }

        UUID placer = action.getSnitch().getPlacer();
        Component owner = JAText.player(group.getOwner());

        if (placer == null) {
            CivUI.info(player, "You broke a ", type, " on ", JAText.group(group), ", owned by ", owner);
        } else {
            CivUI.info(player, "You broke a ", type, " placed by ", JAText.player(placer), " on ",
                JAText.group(group), ", owned by ", owner);
        }
    }

}
