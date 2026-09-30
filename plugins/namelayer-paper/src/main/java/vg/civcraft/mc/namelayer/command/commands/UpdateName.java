package vg.civcraft.mc.namelayer.command.commands;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Syntax;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.civmodcore.ui.Kind;
import vg.civcraft.mc.namelayer.NameLayerAPI;
import vg.civcraft.mc.namelayer.NameLayerPlugin;
import vg.civcraft.mc.namelayer.command.BaseCommandMiddle;
import vg.civcraft.mc.namelayer.misc.NLText;
import vg.civcraft.mc.namelayer.misc.NameFetcher;

public class UpdateName extends BaseCommandMiddle {

    private Map<UUID, String> newNames = Collections.synchronizedSortedMap(new TreeMap<UUID, String>());

    @CommandAlias("nlun|updatename|")
    @Syntax("[confirm]")
    @Description("Updates your name on this server to the one your minecraft account currently has")
    public void execute(Player sender, String newNameOrConfirm) {
        final Player p = (Player) sender;
        final UUID uuid = p.getUniqueId();
        final String oldName = NameLayerAPI.getCurrentName(uuid);

        if (NameLayerPlugin.getGroupManagerDao().hasChangedNameBefore(uuid)) {
            CivUI.error(p, "You've already changed your name");
            return;
        }

        if (newNameOrConfirm.isEmpty()) {
            Bukkit.getScheduler().runTaskAsynchronously(
                NameLayerPlugin.getInstance(), new Runnable() {
                    @Override
                    public void run() {
                        NameFetcher fetcher = new NameFetcher(Collections.singletonList(uuid));
                        Map<UUID, String> fetchedNames = null;
                        try {
                            fetchedNames = fetcher.call();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        if (fetchedNames == null) {
                            CivUI.error(p, "Couldn't look up your account name. Try again later");
                            return;
                        }
                        String newName = fetchedNames.get(uuid);
                        if (newName == null) {
                            CivUI.error(p, "Couldn't look up your account name. Try again later");
                            return;
                        }
                        UUID existingNameUUID = NameLayerAPI.getUUID(newName);
                        if (existingNameUUID != null) {
                            if (!uuid.equals(existingNameUUID)) {
                                // different person has the name
                                CivUI.error(p, "Someone here already uses the name ", CivText.subject(newName), ", so you can't take it");
                                return;
                            }
                            if (oldName.equals(newName)) {
                                // name hasnt changed
                                CivUI.error(p, "Your account name is already ", CivText.subject(newName));
                                return;
                            }
                            // Player wants to change name to one which is
                            // the same one as his current name, but with
                            // different capitalization. We'll allow it
                        }

                        CivUI.notice(Kind.WARN, "Your account is now called ", CivText.subject(newName),
                                ". Run ", NLText.command("/nlun CONFIRM"), " to take that name here - you can't undo it")
                            .icon()
                            .to(p)
                            .send();
                        newNames.put(uuid, newName);
                    }
                });
            return;
        } else {
            String newName = newNames.get(uuid);
            if (newName == null) {
                CivUI.error(sender, "Run ", NLText.command("/nlun"), " first to start a name change");
                return;
            }
            if (!newNameOrConfirm.equals("CONFIRM")) {
                CivUI.error(sender, "Run ", NLText.command("/nlun CONFIRM"), " to confirm your name change to ", CivText.subject(newName));
                return;
            }
            NameLayerPlugin.getGroupManagerDao().logNameChange(uuid, oldName,
                newName);
            // uncomment following to directly change name
            // NameAPI.getAssociationList().changePlayer(newName, uuid);
            // NameAPI.resetCache(uuid);
            CivUI.success(sender, "Your name will change to ", CivText.subject(newName), " with the next scheduled batch of name changes");
        }
    }
}
