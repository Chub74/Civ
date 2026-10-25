package com.devotedmc.ExilePearl.command;

import com.devotedmc.ExilePearl.ExilePearlApi;
import com.devotedmc.ExilePearl.util.PearlText;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import vg.civcraft.mc.civmodcore.ui.CivText;

public class CmdAutoHelp extends PearlCommand {

    private static final int PAGE_SIZE = 9;

    public CmdAutoHelp(ExilePearlApi pearlApi) {
        super(pearlApi);

        this.aliases.add("help");

        this.setHelpShort("");

        this.commandArgs.add(optional("page", "1"));
    }

    @Override
    public void perform() {
        if (this.commandChain.size() == 0) return;
        BaseCommand<?> cmd = this.commandChain.get(this.commandChain.size() - 1);

        List<Component> lines = new ArrayList<>();

        for (String line : cmd.getLongHelp()) {
            lines.add(PearlText.body(line).colorIfAbsent(CivText.INFO));
        }

        for (BaseCommand<?> c : cmd.getSubCommands()) {
            if (!c.validSenderType(sender, false)) {
                continue;
            }

            // Only list help for commands that are visible or the sender has permission for
            if (c.getVisibility() == CommandVisibility.VISIBLE || (c.getVisibility() == CommandVisibility.SECRET && c.validSenderPermissions(sender, false))) {
                lines.add(usage(c));
            }
        }

        int pages = Math.max(1, (lines.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int page = this.argAsInt(0, 1);
        if (page < 1 || page > pages) {
            msg("<b>There's no page <p>%d<b>. Pick one from <p>1 <b>to <p>%d", page, pages);
            return;
        }

        Component heading = Component.text()
            .append(CivText.heading("/" + cmd.getAliases().get(0) + " help"))
            .append(Component.text(" " + page + "/" + pages, CivText.MUTED))
            .build();
        TextComponent.Builder out = Component.text().append(heading);
        for (Component line : lines.subList((page - 1) * PAGE_SIZE, Math.min(lines.size(), page * PAGE_SIZE))) {
            out.append(Component.newline()).append(Component.text("  ")).append(line);
        }
        sender.sendMessage(out.build());
    }

    /** The command, which fills itself into chat on click, then its arguments and what it does. */
    private Component usage(BaseCommand<?> c) {
        StringBuilder command = new StringBuilder("/");
        for (BaseCommand<?> parent : this.commandChain) {
            command.append(parent.getAliases().get(0)).append(' ');
        }
        command.append(c.getAliases().get(0));

        StringBuilder args = new StringBuilder();
        for (CommandArg arg : c.commandArgs) {
            args.append(' ').append(arg);
        }

        return Component.text()
            .append(CivText.subject(command.toString())
                .clickEvent(ClickEvent.suggestCommand(command + " "))
                .hoverEvent(HoverEvent.showText(CivText.info("Click to type " + command))))
            .append(CivText.value(args.toString()))
            .append(CivText.info(" " + c.getHelpShort()))
            .build();
    }
}
