package vg.civcraft.mc.civchat2;

import io.papermc.paper.chat.ChatRenderer;
import net.kyori.adventure.text.Component;
import vg.civcraft.mc.civmodcore.ui.CivText;

public class ChatStrings {

    /** sourceDisplayName already carries its own stars - see CivChat2Manager.identityTag. */
    public final static ChatRenderer localChatFormat = (source, sourceDisplayName, message, viewer) -> Component.empty()
        .append(sourceDisplayName)
        .append(Component.text(" » ", CivText.MUTED))
        .append(message);

    /** Shown to whoever messages an AFK player who hasn't set their own away note. */
    public final static Component chatPlayerAfk = CivText.info("That player is currently AFK");
}
