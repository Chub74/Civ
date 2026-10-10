package com.github.maxopoly.essenceglue;

import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VotifierEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.audience.Audience;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.geysermc.floodgate.api.FloodgateApi;
import vg.civcraft.mc.civmodcore.players.settings.PlayerSettingAPI;
import vg.civcraft.mc.civmodcore.players.settings.impl.LongSetting;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.civmodcore.ui.Kind;

public class VotifyManager implements Listener {

    private final FloodgateApi floodgateApi;
    private final RewardManager rewardMan;
    private final Map<String, VotingSite> perSiteCooldowns;
    private final Map<String, LongSetting> perSiteSettings;

    public VotifyManager(RewardManager rewardMan, Map<String, VotingSite> perSiteCooldowns) {
        this.rewardMan = rewardMan;
        this.perSiteSettings = new HashMap<>();
        this.perSiteCooldowns = new LinkedHashMap<>(perSiteCooldowns);
        this.floodgateApi = FloodgateApi.getInstance();
        for (String s : perSiteCooldowns.keySet()) {
            LongSetting setting = new LongSetting(EssenceGluePlugin.instance(), 0L, "Last vote " + s,
                "essenceGlueVoteTime" + s);
            PlayerSettingAPI.registerSetting(setting, null);
            perSiteSettings.put(s, setting);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onVotifierEvent(VotifierEvent event) {
        Vote vote = event.getVote();
        String voteUsername = vote.getUsername();
        Player player = Bukkit.getPlayer(vote.getUsername());
        if (player != null) {
            handOutVotingReward(player, vote);
            return;
        }

        // Floodgate prefixes Bedrock names with a dot
        if(voteUsername.startsWith(".")){
            var floodgatePlayer = floodgateApi.getPlayers().stream().filter(fp -> fp.getJavaUsername().equalsIgnoreCase(vote.getUsername())).findFirst();
            if(floodgatePlayer.isPresent()){
                player = Bukkit.getPlayer(floodgatePlayer.get().getJavaUniqueId());
                if(player != null){
                    handOutVotingReward(player, vote);
                    return;
                }
            }
        }


        Bukkit.getScheduler().runTaskAsynchronously(EssenceGluePlugin.instance(), () -> {
            // Mojang API rate limit is 600 requests per 10 minutes, which is not a problem for now
            UUID uuid = NewNameResolver.getUUIDForMojangName(vote.getUsername());
            if (uuid == null) {
                EssenceGluePlugin.instance().getLogger().info("Could not resolve vote from " + vote.getUsername());
                return;
            }
            Player altPlayer = Bukkit.getPlayer(uuid);
            if (altPlayer == null) {
                EssenceGluePlugin.instance().getLogger()
                    .info("Found uuid " + uuid + " but could not find player for vote from " + vote.getUsername());
                return;
            }
            Bukkit.getScheduler().runTask(EssenceGluePlugin.instance(), () -> {
                handOutVotingReward(altPlayer, vote);
            });
        });
    }

    public void handOutVotingReward(Player player, Vote vote) {
        VotingSite site = perSiteCooldowns.get(vote.getServiceName());
        if (site == null) {
            EssenceGluePlugin.instance().getLogger()
                .warning("Received vote from unknown service " + vote.getServiceName());
            CivUI.error(player, "Your vote on ", CivText.value(vote.getServiceName()),
                " didn't earn a reward because that site isn't set up here");
            return;
        }
        LongSetting serverCooldown = perSiteSettings.get(vote.getServiceName());
        serverCooldown.setValue(trueId(player), System.currentTimeMillis());
        rewardMan.giveVoteReward(player, site.getName());
        announceToServer(player, site.getName());
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        if (!EssenceGluePlugin.instance().getStreakManager().getShowSitesOnLogin(p.getUniqueId())) {
            return;
        }
        List<VotingSite> open = getOpenSites(p);
        // FullEssenceReminder already tells a full player to withdraw before voting
        Integer room = essenceRoom(p);
        if (open.isEmpty() || (room != null && room == 0)) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(EssenceGluePlugin.instance(), () -> {
            if (p.isOnline()) {
                showOpenSites(p, open);
            }
        }, 20L);
    }

    /** Lists the sites to vote on, warning first if the player can't hold the essence voting on them would earn. */
    public void showOpenSites(Player p, List<VotingSite> open) {
        Integer room = essenceRoom(p);
        int earns = EssenceGluePlugin.instance().getConfigManager().getVotingReward() * open.size();
        if (room != null && room == 0) {
            CivUI.notice(Kind.WARN, "Your essence is full, so you'd lose your voting rewards. Take some out with ",
                    EssenceText.withdrawCommand(), " before you vote")
                .icon().to(p).send();
        } else if (room != null && room < earns) {
            CivUI.warn(p, "You only have room for ", EssenceText.amount(room), " more essence, but voting on "
                + (open.size() == 1 ? "it" : "every site") + " earns ", EssenceText.amount(earns),
                ". Take some out with ", EssenceText.withdrawCommand(), " before you vote");
        }
        CivUI.info(p, "Vote for rewards on ", EssenceText.list(EssenceText.sites(open, p)));
    }

    /** Essence the player has room for, or null when rewards are items and so never run out of room. */
    private static Integer essenceRoom(Player p) {
        VirtualEssenceManager essence = EssenceGluePlugin.instance().getEssenceManager();
        if (essence == null) {
            return null;
        }
        return Math.max(0, essence.getCap() - essence.getEssence(p.getUniqueId()));
    }

    private void announceToServer(Player player, String siteName) {
        // Per player rather than the server audience, so each one gets their own Bedrock adaptation
        CivUI.notice(Kind.INFO, "Thanks to ", CivText.subject(player.getName()), " for voting on ",
                CivText.value(siteName))
            .to(Bukkit.getOnlinePlayers().toArray(new Audience[0]))
            .send();
    }

    /** Sites the player can vote on for a reward right now. */
    public List<VotingSite> getOpenSites(Player player) {
        List<VotingSite> open = new ArrayList<>();
        for (VotingSite site : perSiteCooldowns.values()) {
            if (getRemainingCooldown(site, player) <= 0) {
                open.add(site);
            }
        }
        return open;
    }

    /** Millis until the player can vote on the site again, or 0 or less if they can now. */
    public long getRemainingCooldown(VotingSite site, Player player) {
        long lastVote = getLastVote(site.getInternalKey(), trueId(player));
        return site.getVotingCooldown() - (System.currentTimeMillis() - lastVote);
    }

    public long getLastVote(String internalSiteKey, UUID player) {
        return perSiteSettings.get(internalSiteKey).getValue(player);
    }

    /** The main account, or the player's own if BanStick doesn't know them. */
    private static UUID trueId(Player player) {
        return Objects.requireNonNullElse(StreakManager.getTrueUUID(player.getUniqueId()), player.getUniqueId());
    }

}
