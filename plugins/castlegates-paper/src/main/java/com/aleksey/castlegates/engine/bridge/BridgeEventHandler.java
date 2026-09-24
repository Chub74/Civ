/**
 * Created by Aleksey Terzi
 */

package com.aleksey.castlegates.engine.bridge;

import com.aleksey.castlegates.CastleGates;
import com.aleksey.castlegates.config.ConfigManager;
import com.aleksey.castlegates.engine.PlayerStateManager;
import com.aleksey.castlegates.engine.StorageManager;
import com.aleksey.castlegates.types.*;
import com.aleksey.castlegates.utils.Helper;
import com.aleksey.castlegates.utils.ParticleHelper;
import com.aleksey.castlegates.utils.PowerResultHelper;
import com.aleksey.castlegates.utils.Text;
import vg.civcraft.mc.civmodcore.ui.CivText;
import vg.civcraft.mc.civmodcore.ui.CivUI;
import vg.civcraft.mc.civmodcore.ui.Kind;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public class BridgeEventHandler {

    private static class FindGearResult {

        public Gearblock gearblock;
        public int distance;

        public FindGearResult(Gearblock gearblock, int distance) {
            this.gearblock = gearblock;
            this.distance = distance;
        }
    }

    private final StorageManager _storage;
    private final BridgeManager _bridgeManager;
    private final HashSet<Block> _waitingBlocks = new HashSet<>();
    private final HashSet<Block> _processingBlocks = new HashSet<>();

    public BridgeEventHandler(StorageManager storage, BridgeManager bridgeManager) {
        _storage = storage;
        _bridgeManager = bridgeManager;
    }

    public boolean handleBlockClicked(PlayerInteractEvent event, PlayerStateManager.PlayerState state) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getHand() == EquipmentSlot.HAND) {
            simpleActivate(event);
            return false;
        }

        if (event.getAction() != Action.LEFT_CLICK_BLOCK)
            return false;

        boolean interacted = false;
        ConfigManager configManager = CastleGates.getConfigManager();
        CommandMode mode = state != null ? state.mode : CommandMode.OFF;

        if (configManager.getAllowAutoCreate() && configManager.isCreationConsumeItem(event.getItem())) {
            interacted = createGearblockAndLink(event);
        } else if (mode == CommandMode.INFO) {
            showGearInfo(event);
            interacted = true;
        } else if (mode == CommandMode.TIMER) {
            interacted = setGearblockTimer(event, state);
        } else if (configManager.isStickItem(event.getItem())) {
            interacted = switch (mode) {
                case CREATE -> createGearblock(event);
                case LINK -> linkGearblocks(event);
                default -> false;
            };
        }

        return interacted;
    }

    public void handleBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if(!CastleGates.getCitadelManager().canBypass(event.getPlayer(), block.getLocation())){
            return;
        }

        removeGearAndRefund(event.getPlayer(), block);
    }

    public void handleBlockRedstone(BlockRedstoneEvent event) {
        if ((event.getOldCurrent() != 0) == (event.getNewCurrent() != 0)) return;

        Block block = event.getBlock();

        for (BlockFace face : BridgeManager.faces) {
            Block faceBlock = block.getRelative(face);

            if (_storage.getGearblock(new BlockCoord(faceBlock)) != null) {
                _waitingBlocks.add(faceBlock);
            }
        }
    }

    public void handleBlockPhysics(BlockPhysicsEvent event) {
        Block block = event.getBlock();

        if (_waitingBlocks.remove(block)) {
            processBlock(block, block.isBlockPowered());
        }
    }

    public void handleEntityExplode(EntityExplodeEvent event) {
        for (Block block : event.blockList()) {
            _bridgeManager.removeGear(new BlockCoord(block));
        }
    }

    /** Hoeing, path-making, trampling, endermen and falling blocks all land here. */
    public void handleEntityChangeBlock(EntityChangeBlockEvent event) {
        Player player = event.getEntity() instanceof Player p ? p : null;
        removeGearAndRefund(player, event.getBlock());
    }

    public void handlePistonEvent(List<Block> blocks) {
        for (Block block : blocks) {
            removeGearAndRefund(null, block);
        }
    }

    /** Removes the gearblock here, if any, and returns its creation item to the player or drops it at the block. */
    private void removeGearAndRefund(Player player, Block block) {
        BridgeManager.RemoveResult result = _bridgeManager.removeGear(new BlockCoord(block));

        if (result == BridgeManager.RemoveResult.Removed || result == BridgeManager.RemoveResult.RemovedWithLink) {
            Helper.putItemToInventoryOrDrop(player, block.getLocation(), CastleGates.getConfigManager().getCreationConsumeItem());
        }
    }

    private boolean createGearblockAndLink(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null
            || CastleGates.getCitadelManager().isReinforcingStateActive(event.getPlayer())
            || !createGearblock(event)
        ) {
            return false;
        }

        Block block = event.getClickedBlock();
        Gearblock gearblock1 = _storage.getGearblock(new BlockCoord(block));

        if (gearblock1.getLink() != null)
            return true;

        for (BlockFace face : BridgeManager.faces) {
            FindGearResult result = findEndGear(block, face);

            if (result != null
                && result.gearblock.getLink() == null
                && linkGearblocks(event.getPlayer(), gearblock1, result, false)
            ) {
                break;
            }
        }

        return true;
    }

    private boolean createGearblock(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        if (block == null)
            return false;

        Location location = block.getLocation();

        if (CastleGates.getConfigManager().isGearblockMustBeReinforced()
            && !CastleGates.getCitadelManager().isReinforced(location)) {
            CivUI.error(player, "Reinforce this block before making it a gearblock");
            return false;
        }

        if (!CastleGates.getCitadelManager().canBypass(player, location)) {
            CivUI.error(player, "You need bypass on this block's reinforcement to make it a gearblock");
            return false;
        }

        ItemStack consumeItem = CastleGates.getConfigManager().getCreationConsumeItem();
        List<Integer> consumeSlots = Helper.getConsumeSlots(player, consumeItem);

        if (consumeSlots == null && consumeItem != null) {
            CivUI.error(player, "You need ", CivText.value(String.valueOf(consumeItem.getAmount())), " ",
                Text.block(consumeItem.getType()), " to make a gearblock");
            return false;
        }

        BridgeManager.CreateResult result = _bridgeManager.createGear(block);

        if (result == BridgeManager.CreateResult.NotCreated) {
            CivUI.error(player, Text.block(block.getType()), " can't be a gearblock");
            return false;
        } else if (result == BridgeManager.CreateResult.AlreadyExist) {
            CivUI.error(player, "That block is already a gearblock");
            return true;
        }

        Helper.consumeItem(player, consumeItem, consumeSlots);

        CivUI.success(player, "Made this block a gearblock");

        ParticleHelper.spawn(block, ParticleHelper.Type.Info);

        return true;
    }

    private boolean linkGearblocks(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        if (block == null)
            return false;

        Gearblock gearblock1 = _storage.getGearblock(new BlockCoord(block));

        if (gearblock1 == null)
            return false;

        if (!CastleGates.getCitadelManager().canBypass(player, block.getLocation())) {
            CivUI.error(player, "You need bypass on this gearblock to link it");
            return false;
        }

        if (gearblock1.getLink() != null) {
            if (gearblock1.getLink().isDrawn()) {
                CivUI.error(player, "You can't unlink gearblocks while the bridge or gate is drawn");
            } else {
                _bridgeManager.removeLink(gearblock1.getLink());
                CivUI.success(player, "Unlinked the gearblocks");
            }

            return true;
        }

        FindGearResult result = findEndGear(block, event.getBlockFace());

        if (result == null) {
            CivUI.error(player, "There's no gearblock to link to within ",
                CivText.value(String.valueOf(CastleGates.getConfigManager().getMaxBridgeLength())), " blocks of this face");
        } else {
            linkGearblocks(player, gearblock1, result, true);
        }

        return true;
    }

    private boolean linkGearblocks(Player player, Gearblock gearblock1, FindGearResult result, boolean showError) {
        Location loc = new Location(player.getWorld(), result.gearblock.getCoord().getX(), result.gearblock.getCoord().getY(), result.gearblock.getCoord().getZ());

        if (!CastleGates.getCitadelManager().canBypass(player, loc)) {
            if (showError) {
                CivUI.error(player, "You need bypass on the gearblock at ", Text.coords(result.gearblock.getCoord()), " to link to it");
            }

            return false;
        }

        if (result.gearblock.getLink() != null) {
            if (showError) {
                CivUI.error(player, "The gearblock at ", Text.coords(result.gearblock.getCoord()), " is already linked. Unlink it first");
                ParticleHelper.spawn(player, result.gearblock, ParticleHelper.Type.Warning);
            }

            return false;
        }

        if (_bridgeManager.createLink(gearblock1, result.gearblock, result.distance)) {
            CivUI.success(player, "Linked to the gearblock at ", Text.coords(result.gearblock.getCoord()));
            ParticleHelper.spawn(player, gearblock1, ParticleHelper.Type.Info);
            ParticleHelper.spawn(player, result.gearblock, ParticleHelper.Type.Info);
            return true;
        }

        if (showError) {
            CivUI.error(player, "The gearblock at ", Text.coords(result.gearblock.getCoord()),
                " has a broken link. Click where its old partner was to restore it");
            ParticleHelper.spawn(player, result.gearblock, ParticleHelper.Type.Warning);
        }

        return false;
    }

    private FindGearResult findEndGear(Block startGearBlock, BlockFace blockFace) {
        UUID worldUID = startGearBlock.getWorld().getUID();
        int x = startGearBlock.getX();
        int y = startGearBlock.getY();
        int z = startGearBlock.getZ();
        int maxBridgeLength = CastleGates.getConfigManager().getMaxBridgeLength();

        boolean isNonSimpleDisabled = CastleGates.getConfigManager().isHorizontalLinkDisabled()
            && blockFace != BlockFace.UP
            && blockFace != BlockFace.DOWN;

        for (int i = 0; i < maxBridgeLength; i++) {
            x += blockFace.getModX();
            y += blockFace.getModY();
            z += blockFace.getModZ();

            BlockCoord location = new BlockCoord(worldUID, x, y, z);
            Gearblock gearblock = _storage.getGearblock(location);

            if (gearblock != null) {
                if (i == 0 || i > 1 && isNonSimpleDisabled)
                    return null;

                if (isNonSimpleDisabled && (
                    hasAdjacentGearblocks(worldUID, startGearBlock.getX(), startGearBlock.getY(), startGearBlock.getZ())
                        || hasAdjacentGearblocks(worldUID, x, y, z)
                )
                ) {
                    return null;
                }

                return new FindGearResult(gearblock, i);
            }
        }

        return null;
    }

    private boolean hasAdjacentGearblocks(UUID worldUID, int x, int y, int z) {
        return _storage.hasGearblock(new BlockCoord(worldUID, x + 1, y, z))
            || _storage.hasGearblock(new BlockCoord(worldUID, x - 1, y, z))
            || _storage.hasGearblock(new BlockCoord(worldUID, x, y + 1, z))
            || _storage.hasGearblock(new BlockCoord(worldUID, x, y - 1, z))
            || _storage.hasGearblock(new BlockCoord(worldUID, x, y, z + 1))
            || _storage.hasGearblock(new BlockCoord(worldUID, x, y, z - 1));
    }

    private void showGearInfo(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        if (block == null)
            return;

        BlockCoord blockCoord = new BlockCoord(block);
        Gearblock gearblock = _storage.getGearblock(blockCoord);

        if (gearblock == null) {
            BridgeManager.SearchBridgeBlockResult searchResult = _bridgeManager.searchBridgeBlock(blockCoord);

            switch (searchResult) {
                case Bridge -> CivUI.info(player, "This block is part of a bridge");
                case Gates -> CivUI.info(player, "This block is part of a gate");
            }

            return;
        }

        if (!CastleGates.getCitadelManager().canViewInformation(player, block.getLocation())) {
            CivUI.notice(Kind.INFO, "This is a gearblock")
                .hover(CivText.info("You don't have info access on its reinforcement"))
                .to(player)
                .send();
        } else {
            if (gearblock.getLink() == null) {
                if (gearblock.getBrokenLink() != null) {
                    CivUI.info(player, "This gearblock isn't linked, but holds ",
                        CivText.value(String.valueOf(gearblock.getBrokenLink().getBlocks().size())), " drawn blocks");
                } else {
                    CivUI.info(player, "This gearblock isn't linked");
                }
            } else {
                Gearblock gearblock2 = gearblock.getLink().getGearblock1() == gearblock ? gearblock.getLink().getGearblock2() : gearblock.getLink().getGearblock1();
                CivUI.info(player, "This gearblock is linked to ", Text.coords(gearblock2.getCoord()),
                    gearblock.getLink().isDrawn() ? " and is drawn" : "");

                ParticleHelper.spawn(player, gearblock2, ParticleHelper.Type.Info);
            }

            if (gearblock.getTimer() != null) {
                CivUI.info(player, "Its timer will ",
                    Text.timer(gearblock.getTimer(), gearblock.getTimerOperation(), gearblock.getTimerMode()));
            }

            if (gearblock.getLockedGearblocks() != null || gearblock.getLockGearblock() != null) {
                CivUI.warn(player, "This gearblock is locked");
            }
        }
    }

    private void processBlock(Block block, boolean isPowered) {
        if (_processingBlocks.contains(block))
            return;

        Gearblock gearblock = _storage.getGearblock(new BlockCoord(block));

        if (gearblock == null || gearblock.isPowered() == isPowered)
            return;

        _processingBlocks.add(block);

        try {
            List<Player> players = Helper.getNearbyPlayers(block.getLocation());

            PowerResult result = _bridgeManager.processGearblock(
                block.getWorld(),
                gearblock,
                isPowered,
                players
            );

            if (result.status == PowerResult.Status.Locked && gearblock.getTimerMode() == TimerMode.DOOR) {
                result = PowerResult.Unchanged;
            }

            PowerResultHelper.showStatus(block.getLocation(), players, result);
        } finally {
            _processingBlocks.remove(block);
        }
    }

    private boolean setGearblockTimer(PlayerInteractEvent event, PlayerStateManager.PlayerState state) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        if (block == null)
            return true;

        if (!CastleGates.getConfigManager().isTimerEnabled()) {
            CivUI.error(player, "Timers are disabled on this server");
            return true;
        }

        BlockCoord blockCoord = new BlockCoord(block);
        Gearblock gearblock = _storage.getGearblock(blockCoord);

        if (gearblock == null) {
            CivUI.error(player, "That block isn't a gearblock");
            return true;
        }

        if (!CastleGates.getCitadelManager().canBypass(player, block.getLocation())) {
            CivUI.error(player, "You need bypass on this gearblock to change its timer");
            return true;
        }

        if (gearblock.getTimer() == null) {
            _storage.setGearblockTimer(gearblock, state.timer, state.timerOperation, state.timerMode);
            CivUI.success(player, "Set the timer to ", Text.timer(state.timer, state.timerOperation, state.timerMode));
        } else {
            _storage.clearGearblockTimer(gearblock);
            CivUI.success(player, "Removed the gearblock's timer");
        }

        ParticleHelper.spawn(player, gearblock, ParticleHelper.Type.Info);

        return true;
    }

    private void simpleActivate(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();

        if (block == null)
            return;

        BlockCoord blockCoord = new BlockCoord(block);
        Gearblock gearblock = _storage.getGearblock(blockCoord);

        if (gearblock != null)
            return;

        GearblockLink link;

        if ((link = getLink(blockCoord.getForward(), blockCoord.getBackward())) == null
            && (link = getLink(blockCoord.getRight(), blockCoord.getLeft())) == null
            && (link = getLink(blockCoord.getTop(), blockCoord.getBottom())) == null
        ) {
            return;
        }

        if (_bridgeManager.isNotSimpleGearblock(link.getGearblock1(), event.getPlayer())
            || _bridgeManager.isNotSimpleGearblock(link.getGearblock2(), event.getPlayer())
        ) {
            return;
        }

        BlockCoord processCoord = link.getGearblock1().getCoord();
        Block processBlock = block.getWorld().getBlockAt(processCoord.getX(), processCoord.getY(), processCoord.getZ());

        if (_waitingBlocks.contains(processBlock))
            return;

        processBlock(processBlock, true);
        processBlock(processBlock, false);
    }

    private GearblockLink getLink(BlockCoord start, BlockCoord end) {
        Gearblock gearblock = _storage.getGearblock(start);
        GearblockLink link = gearblock != null ? gearblock.getLink() : null;

        if (link != null &&
            (
                link.getGearblock1().getCoord().equals(start) && link.getGearblock2().getCoord().equals(end)
                    || link.getGearblock1().getCoord().equals(end) && link.getGearblock2().getCoord().equals(start)
            )
        ) {
            return link;
        }

        return null;
    }
}
