package vg.civcraft.mc.namelayer.misc;

import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.namelayer.GroupManager.PlayerType;
import vg.civcraft.mc.namelayer.NameLayerAPI;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.permission.PermissionType;

/** The nouns NameLayer messages are built from, coloured by CivText role */
public final class NLText {

    private NLText() {
    }

    public static Component group(Group group) {
        return CivText.group(group.getName());
    }

    public static Component group(String name) {
        return CivText.group(name);
    }

    public static Component player(UUID uuid) {
        return CivText.subject(NameLayerAPI.getCurrentName(uuid));
    }

    /** Ranks are shown as typed in commands: MEMBERS, MODS, ADMINS, OWNER */
    public static Component rank(PlayerType type) {
        return CivText.value(type.name());
    }

    public static Component ranks() {
        return join(Arrays.stream(PlayerType.values()).map(NLText::rank).toList());
    }

    public static Component permissions() {
        return join(PermissionType.getAllPermissions().stream()
            .map(perm -> CivText.value(perm.getName()))
            .collect(Collectors.toList()));
    }

    public static Component players(Collection<UUID> uuids) {
        return join(uuids.stream().map(NLText::player).toList());
    }

    /** Opens the group's menu when clicked */
    public static Component open(Group group) {
        String command = "/nl " + group.getName();
        return CivText.value(command)
            .clickEvent(ClickEvent.runCommand(command))
            .hoverEvent(HoverEvent.showText(Component.text("Click to open", CivText.MUTED)));
    }

    /** A command the player can click to have typed into their chat box */
    public static Component command(String command) {
        return CivText.value(command)
            .clickEvent(ClickEvent.suggestCommand(command))
            .hoverEvent(HoverEvent.showText(Component.text("Click to fill in", CivText.MUTED)));
    }

    public static Component join(Collection<Component> parts) {
        return Component.join(JoinConfiguration.separator(Component.text(", ", CivText.INFO)), parts);
    }

    /** A bold heading line followed by one indented entry per line, for lists too long for one sentence */
    public static Component list(Component heading, Collection<Component> entries) {
        return Component.text()
            .append(heading)
            .append(entries.stream()
                .map(entry -> Component.text().append(Component.newline()).append(Component.text("  ")).append(entry).build())
                .toList())
            .build();
    }
}
