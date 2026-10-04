package com.programmerdan.minecraft.simpleadminhacks.framework.utilities;

import com.programmerdan.minecraft.simpleadminhacks.SimpleAdminHacks;
import java.time.Duration;
import net.kyori.adventure.audience.Audience;
import vg.civcraft.mc.civmodcore.ui.Channel;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.civmodcore.ui.Kind;

/** Output shared by the hacks */
public final class SAHText {

    /** Denials fire on every repeat of the action, so one per second per player at most */
    private static final Duration DENY_WINDOW = Duration.ofSeconds(1);

    private SAHText() {
    }

    /** A denial of something the player can repeat. On the action bar, so it doesn't pile up in chat */
    public static void deny(Audience to, Object... parts) {
        bar(to, Kind.ERROR, parts);
    }

    /** Short feedback on the action bar, at most once a second */
    public static void bar(Audience to, Kind kind, Object... parts) {
        CivUI.notice(kind, parts).from(SimpleAdminHacks.instance()).to(to).on(Channel.ACTION_BAR)
            .atMostEvery(DENY_WINDOW).send();
    }
}
