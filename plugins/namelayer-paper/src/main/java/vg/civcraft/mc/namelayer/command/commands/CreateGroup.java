package vg.civcraft.mc.namelayer.command.commands;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Syntax;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.civmodcore.ui.Kind;
import vg.civcraft.mc.namelayer.GroupManager;
import vg.civcraft.mc.namelayer.NameLayerAPI;
import vg.civcraft.mc.namelayer.NameLayerPlugin;
import vg.civcraft.mc.namelayer.RunnableOnGroup;
import vg.civcraft.mc.namelayer.command.BaseCommandMiddle;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.misc.NLText;

public class CreateGroup extends BaseCommandMiddle {

    @CommandAlias("nlcg|nlcreategroup|creategroup|create|ctcreate|creategroup")
    @Syntax("<group> [password]")
    @Description("Create a group (Public or Private). Password is optional.")
    public void execute(Player sender, String groupName, @Optional String userPassword) {
        Player p = (Player) sender;
        String name = groupName;
        int currentGroupCount = gm.countGroups(p.getUniqueId());

        if (NameLayerPlugin.getInstance().getGroupLimit() < currentGroupCount + 1 && !(p.isOp() || p.hasPermission("namelayer.admin"))) {
            CivUI.error(p, "You're at the limit of ", CivText.value(String.valueOf(NameLayerPlugin.getInstance().getGroupLimit())),
                " groups. Delete one before making another");
            return;
        }

        //enforce regulations on the name
        if (name.length() > 32) {
            CivUI.error(p, "Group names can be at most ", CivText.value("32"), " characters");
            return;
        }
        Charset latin1 = StandardCharsets.ISO_8859_1;
        boolean invalidChars = false;
        if (!latin1.newEncoder().canEncode(name)) {
            invalidChars = true;
        }
        //cant allow them to hurt mercury :(
        if (name.contains("|")) {
            invalidChars = true;
        }

        for (char c : name.toCharArray()) {
            if (Character.isISOControl(c)) {
                invalidChars = true;
            }
        }

        if (invalidChars) {
            CivUI.error(p, "Group names can't contain that character");
            return;
        }

        if (GroupManager.getGroup(name) != null) {
            CivUI.error(p, NLText.group(name), " is already taken. Try another name");
            return;
        }
        String password = "";
        if (userPassword != null) {
            password = userPassword;
        } else {
            password = null;
        }
        final UUID uuid = NameLayerAPI.getUUID(p.getName());
        Group g = new Group(name, uuid, false, password, -1, System.currentTimeMillis(), "GRAY");
        gm.createGroupAsync(g, new RunnableOnGroup() {
            @Override
            public void run() {
                Player p = null;
                p = Bukkit.getPlayer(uuid);
                Group g = getGroup();
                if (p != null) {
                    if (g.getGroupId() == -1) { // failure
                        CivUI.error(p, "Couldn't create ", NLText.group(g), ". The name may already be taken");
                        return;
                    }
                    CivUI.notice(Kind.SUCCESS, "Created the group ", NLText.group(g), ". Open it with ", NLText.open(g))
                        .hover(CivText.info("Invite people with /nlip " + g.getName() + " <player>"))
                        .to(p)
                        .send();
                } else {
                    NameLayerPlugin.getInstance().getLogger().log(Level.INFO, "Group {0} creation complete resulting in group id: {1}",
                        new Object[]{g.getName(), g.getGroupId()});
                }
            }
        }, false);
        if (NameLayerPlugin.getInstance().getGroupLimit() == (currentGroupCount + 1)) {
            CivUI.warn(p, "That's your last group - you've reached the limit of ",
                CivText.value(String.valueOf(NameLayerPlugin.getInstance().getGroupLimit())));
        }
    }
}
