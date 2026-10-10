package com.github.maxopoly.essenceglue;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.entity.Player;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.utilities.TextUtil;

/** The nouns EssenceGlue messages are built from */
public final class EssenceText {

    public static final String WITHDRAW_COMMAND = "/essence withdraw";
    public static final String VOTE_COMMAND = "/vote";

    private EssenceText() {
    }

    public static Component amount(int amount) {
        return CivText.value(Integer.toString(amount));
    }

    public static Component duration(long millis) {
        return CivText.value(TextUtil.formatDuration(millis));
    }

    public static Component withdrawCommand() {
        return command(WITHDRAW_COMMAND);
    }

    public static Component voteCommand() {
        return command(VOTE_COMMAND);
    }

    /** A command in the SimpleAdminHacks announcement style: white, and typed into chat on click. */
    private static Component command(String command) {
        return Component.text(command, CivText.SUBJECT)
            .clickEvent(ClickEvent.suggestCommand(command))
            .hoverEvent(HoverEvent.showText(CivText.info("Click to type " + command)));
    }

    /** A voting site's name in the announcement link style: VALUE with ↗, opening its voting page. */
    public static Component site(VotingSite site, Player player) {
        String url = site.getVotingUrl().replace("%PLAYER%", player.getName());
        return Component.text(site.getName() + " ↗", CivText.VALUE)
            .clickEvent(ClickEvent.openUrl(url))
            .hoverEvent(HoverEvent.showText(CivText.info("Click to open " + url)));
    }

    /** "a", "a and b", "a, b and c" */
    public static Component list(List<? extends Component> parts) {
        TextComponent.Builder builder = Component.text();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                builder.append(Component.text(i == parts.size() - 1 ? " and " : ", "));
            }
            builder.append(parts.get(i));
        }
        return builder.build();
    }

    public static List<Component> sites(List<VotingSite> sites, Player player) {
        List<Component> links = new ArrayList<>(sites.size());
        for (VotingSite site : sites) {
            links.add(site(site, player));
        }
        return links;
    }
}
