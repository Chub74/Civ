package com.devotedmc.ExilePearl;

/** Message templates, sent with PearlText. */
public class Lang {

    public static final String pearlCantHold = "<b>You can't pick up pearls while you're pearled";
    public static final String pearlMotd2 = "<i>Type <c>/ep locate <i>to find your pearl";
    public static final String pearlNotExiled = "<i>You aren't pearled";
    public static final String pearlPearlIsHeld = "<i>Your pearl is held by <c>%s  <m>[<p>%d %d %d %s<m>]";
    public static final String pearlYouBound = "<g>You pearled <c>%s";
    public static final String pearlYouWereBound = "<b>You were pearled by <c>%s";
    public static final String pearlYouWereFreed = "<g>You've been freed";
    public static final String pearlYouFreed = "<g>You freed <c>%s";
    public static final String pearlYouDamagedNotAwarded = "<c>%s <i>was pearled by <c>%s";
    public static final String pearlMustBeHoldingPearl = "<b>Hold a pearl to do that";
    public static final String pearlAlreadyPearled = "<b>You can't pearl <c>%s<b>, they're already pearled";
    public static final String pearlAlreadyPearledAtLocation = "<b>You can't pearl <c>%s<b>, they're already <i>%s";
    public static final String pearlPlayerNotExiled = "<b>That player isn't pearled";
    public static final String pearlPlayerNotExiledOrBcasting = "<b>That player isn't pearled or broadcasting to you";
    public static final String pearlCantDoThat = "<b>You can't do that while you're pearled";
    public static final String pearlNoPlayer = "<b>There's no online player by that name";
    public static final String pearlBcastRequestSent = "<g>Asked them to accept your pearl broadcasts";
    public static final String pearlBcastRequest = "<c>%s <i>wants to broadcast their pearl location to you. Type <c>/ep accept <i>to accept";
    public static final String pearlNoBcastRequest = "<b>You have no broadcast requests";
    public static final String pearlGettingBcasts = "<g>You're now getting pearl broadcasts from <c>%s";
    public static final String pearlNotGettingBcasts = "<b>You aren't getting broadcasts from <c>%s";
    public static final String pearlCantBcastSelf = "<b>You can't broadcast to yourself";
    public static final String pearlAlreadyBcasting = "<b>You're already broadcasting to that player";
    public static final String pearlSilencedBcast = "<g>You silenced pearl broadcasts from <c>%s";
    public static final String pearlBroadcast = "<i>The pearl of <c>%s <i>is held by <c>%s  <m>[<p>%d %d %d %s<m>]";
    public static final String pearlCantThrow = "<b>You can't throw pearls";
    public static final String pearlCantSummon = "<b>You can't summon that pearl";
    public static final String pearlSummoned = "<g>You summoned <c>%s";
    public static final String pearlYouWereSummoned = "<g>You were summoned by <c>%s";
    public static final String pearlCantReturn = "<b>You can't return that pearl";
    public static final String pearlReturned = "<g>You returned <c>%s";
    public static final String pearlPlayerIsCombatTagged = "<b>You can't summon them while they're in combat";
    public static final String pearlKillerIsPearled = "<b>You can't pearl others while you're pearled";

    public static final String groupUnknown = "<b>There's no group by that name";
    public static final String groupNoChatPermission = "<b>You don't have permission to chat in that group";
    public static final String groupNoExileBroadcastPermission = "<b>You don't have permission to broadcast in that group";
    public static final String groupStoppedBcasting = "<g>You stopped broadcasting your pearl location to <grp>%s";
    public static final String groupNowBcasting = "<g>You're now broadcasting your pearl location to <grp>%s";
    public static final String groupPearlBroadcast = "<grp>%s <m>| <i>The pearl of <c>%s <i>is held by <c>%s  <m>[<p>%d %d %d %s<m>]";

    public static final String permForbidden = "<b>You don't have permission to %s";
    public static final String permDoThat = "do that";
    public static final String commandSenderMustBePlayer = "<b>Only players can use this command";
    public static final String commandToFewArgs = "<b>That needs more arguments. Use it like this:";
    public static final String commandToManyArgs = "<b>There's an argument too many, <p>%s<b>. Use it like this:";
    public static final String unknownCommand = "<b>There's no command by that name";
    public static final String unknownPlayer = "<b>There's no player by that name";
    public static final String locNotInventory = "<b>That location isn't an inventory";
    public static final String ruleCantDoThat = "<b>You can't %s while you're exiled";
    public static final String onlyExiledPlayers = "<b>Only exile pearled players can do that";
    public static final String onlyPrisonedPlayers = "<b>Only prison pearled players can do that";
    public static final String suicideInSeconds = "<w>You'll die in <p>%d seconds <w>unless you move";
    public static final String suicideCancelled = "<i>You moved, so you won't die";

    public static final String summoningNotEnabled = "<b>Pearl summoning is turned off";
    public static final String summonRequested = "<g>Asked <c>%s <g>to accept your summon";
    public static final String hasRequestedToSummon = "<c>%s <i>wants to summon you. Type <c>/ep confirm <i>to accept";
    public static final String noSummonRequested = "<b>Nobody asked to summon you";

}
