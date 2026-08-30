package com.direwolf20.justdirethings.common.blockentities;

import com.direwolf20.justdirethings.common.blockentities.basebe.BaseMachineBE;
import com.direwolf20.justdirethings.common.blockentities.basebe.RedstoneControlledBE;
import com.direwolf20.justdirethings.setup.Registration;
import com.direwolf20.justdirethings.util.MiscHelpers;
import com.direwolf20.justdirethings.util.interfacehelpers.RedstoneControlData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.level.BlockEvent;

import java.util.*;

public class BlockBreakerT1BE extends BaseMachineBE implements RedstoneControlledBE {
	// BlockState we're breaking, ticks since we started, lastpacket progress we
	// sent, and an iterator, because each packet needs a unique breaker ID
	private record BlockBreakingProgress(BlockState blockState, int ticks, int lastSentProgress, int iterator,
			float destroyProgress) {
		public BlockBreakingProgress(BlockState blockState, int ticks, int iterator, float destroyProgress) {
			this(blockState, ticks, -1, iterator, destroyProgress); // Initialize with -1 to indicate no progress sent
																	// yet
		}
	}

	LinkedHashMap<BlockPos, BlockBreakingProgress> blockBreakingTracker = new LinkedHashMap<>();
	public RedstoneControlData redstoneControlData = new RedstoneControlData();
	Map.Entry<BlockPos, BlockBreakingProgress> currentBlock;
	public boolean sneaking = false;

	public BlockBreakerT1BE(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
		super(pType, pPos, pBlockState);
		MACHINE_SLOTS = 1; // Slot for a pickaxe
	}

	public BlockBreakerT1BE(BlockPos pPos, BlockState pBlockState) {
		this(Registration.BlockBreakerT1BE.get(), pPos, pBlockState);
	}

	public void setBreakerSettings(boolean sneaking) {
		this.sneaking = sneaking;
		markDirtyClient();
	}

	@Override
	public RedstoneControlData getRedstoneControlData() {
		return redstoneControlData;
	}

	@Override
	public BlockEntity getBlockEntity() {
		return this;
	}

	@Override
	public void tickClient() {
	}

	public void clearTracker(FakePlayer fakePlayer) {
		Iterator<Map.Entry<BlockPos, BlockBreakingProgress>> iterator = blockBreakingTracker.entrySet().iterator();

		while (iterator.hasNext()) {
			Map.Entry<BlockPos, BlockBreakingProgress> entry = iterator.next();
			sendClearPacket(entry.getKey(), fakePlayer.getId() + entry.getValue().iterator);
			iterator.remove();
		}
	}

	public void sendClearPacket(BlockPos blockPos, int pBreakerId) {
		sendPackets(pBreakerId, blockPos, -1);
	}

	public void removePosFromTracker(BlockPos blockPos, int pBreakerId) {
		sendClearPacket(blockPos, pBreakerId);
		blockBreakingTracker.remove(blockPos);
	}

	public void clearTrackerIfNeeded(ItemStack tool, FakePlayer fakePlayer) {
		if (blockBreakingTracker.isEmpty())
			return;
		if (tool.isEmpty())
			clearTracker(fakePlayer); // If we were breaking blocks before, and removed the tool, clear the progress
		if (!isActiveRedstone() && !redstoneControlData.redstoneMode.equals(MiscHelpers.RedstoneMode.PULSE))
			clearTracker(fakePlayer); // If we are in Pulse Mode, don't clear, but otherwise, do clear on redstone
										// signal turned off
	}

	@Override
	public void tickServer() {
		super.tickServer();
		// FakePlayerFactory caches per profile; reset sneak after so it can't
		// leak to other machines sharing the profile.
		FakePlayer fakePlayer = getFakePlayer((ServerLevel) level);
		fakePlayer.setShiftKeyDown(sneaking);
		doBlockBreak();
		fakePlayer.setShiftKeyDown(false);
	}

	public boolean canMine() {
		return true;
	}

	public void doBlockBreak() {
		ItemStack tool = getTool();
		FakePlayer fakePlayer = getFakePlayer((ServerLevel) level);
		clearTrackerIfNeeded(tool, fakePlayer);
		if (tool.isEmpty())
			return;
		if (isActiveRedstone() && canRun() && blockBreakingTracker.isEmpty()) {
			List<BlockPos> blocksToMine = findBlocksToMine(fakePlayer);
			for (BlockPos blockPos : blocksToMine) {
				BlockState blockState = level.getBlockState(blockPos);
				if (!blockState.isAir() && !blockBreakingTracker.containsKey(blockPos)) // Start tracking the mine!
					startMining(fakePlayer, blockPos, blockState, tool);
			}
		}
		if (blockBreakingTracker.isEmpty() || !canMine())
			return;
		if (currentBlock == null && canRun())
			currentBlock = blockBreakingTracker.entrySet().iterator().next();
		if (currentBlock != null && (mineBlock(currentBlock.getKey(), tool, fakePlayer))) {
			removePosFromTracker(currentBlock.getKey(), fakePlayer.getId() + currentBlock.getValue().iterator);
			currentBlock = null;
		}
	}

	public boolean isBlockValid(FakePlayer fakePlayer, BlockPos blockPos) {
		if (level.getBlockState(blockPos).isAir())
			return false;
		if (blockPos.equals(getBlockPos()))
			return false;
		if (blockBreakingTracker.containsKey(blockPos))
			return false;
		if (level.getBlockState(blockPos).getDestroySpeed(level, blockPos) < 0)
			return false;
		if (!level.mayInteract(fakePlayer, blockPos))
			return false;
		return true;
	}

	public List<BlockPos> findBlocksToMine(FakePlayer fakePlayer) {
		// Only the faced block is tracked for mining progress; ability extras
		// (hammer AOE, ore veins, etc.) break all at once when it completes -
		// see breakBlock.
		List<BlockPos> returnList = new ArrayList<>();
		BlockPos targetPos = getBlockPos().relative(getBlockState().getValue(BlockStateProperties.FACING));
		if (isBlockValid(fakePlayer, targetPos))
			returnList.add(targetPos);
		return returnList;
	}

	public ItemStack getTool() {
		return getMachineHandler().getStackInSlot(0);
	}

	public int generatePosHash() {
		BlockPos blockPos = getBlockPos();
		return blockPos.getX() + blockPos.getY() + blockPos.getZ(); // For now this is probably good enough, will add
																	// more randomness if needed
	}

	public Direction getFacing() {
		return getBlockState().getValue(BlockStateProperties.FACING);
	}

	public void startMining(FakePlayer fakePlayer, BlockPos blockPos, BlockState blockState, ItemStack tool) {
		// if (!tool.isCorrectToolForDrops(blockState)) return;
		setFakePlayerData(tool, fakePlayer, blockPos, getFacing());
		blockBreakingTracker.put(blockPos,
				new BlockBreakingProgress(blockState, 0, blockBreakingTracker.size() + generatePosHash(),
						getDestroyProgress(blockPos, tool, fakePlayer, blockState)));
	}

	/**
	 * @return true if we should remove the block from the map (Via the iterator -
	 *         above)
	 */
	public boolean mineBlock(BlockPos blockPos, ItemStack tool, FakePlayer player) {
		BlockState blockState = level.getBlockState(blockPos);
		if (blockState.isAir()) { // If we got here, and the block is air, it means we had a block there before
									// and it has since been removed
			return true;
		}
		if (blockBreakingTracker.containsKey(blockPos)
				&& !level.getBlockState(blockPos).equals(blockBreakingTracker.get(blockPos).blockState)) {
			return true;
		}
		BlockBreakingProgress progress = blockBreakingTracker.compute(blockPos, (pos, oldProgress) -> {
			int updatedTicks = oldProgress == null ? 1 : oldProgress.ticks + 1;
			return new BlockBreakingProgress(blockState, updatedTicks,
					oldProgress == null ? -1 : oldProgress.lastSentProgress, oldProgress.iterator,
					oldProgress.destroyProgress);
		});
		float destroyProgress = progress.destroyProgress * progress.ticks;
		int currentProgress = (int) (destroyProgress * 10.0F);
		if (currentProgress != progress.lastSentProgress && currentProgress < 10) {
			sendPackets(player.getId() + progress.iterator, blockPos, currentProgress);
			blockBreakingTracker.put(blockPos, new BlockBreakingProgress(blockState, progress.ticks, currentProgress,
					progress.iterator, progress.destroyProgress));
		}
		if (destroyProgress >= 1.0f) {
			tryBreakBlock(tool, player, blockPos, blockState);
			return true;
		}
		return false;
	}

	public float getDestroyProgress(BlockPos blockPos, ItemStack tool, FakePlayer player, BlockState blockState) {
		float hardness = blockState.getDestroySpeed(level, blockPos);
		if (hardness == -1.0F) {
			return -1.0F;
		}
		float modifier = tool.isCorrectToolForDrops(blockState) ? 30 : 100;
		return getDestroySpeed(blockPos, tool, player, blockState) / hardness / modifier;
	}

	public float getDestroySpeed(BlockPos blockPos, ItemStack tool, FakePlayer player, BlockState blockState) {
		float toolDestroySpeed = tool.getDestroySpeed(blockState);
		if (toolDestroySpeed > 1.0F) {
			int efficiency = EnchantmentHelper.getTagEnchantmentLevel(Enchantments.BLOCK_EFFICIENCY, tool);
			if (efficiency > 0) {
				toolDestroySpeed += (float) (efficiency * efficiency + 1);
			}
		}

		// Fire the Forge BreakSpeed event manually, equivalent to
		// ForgeHooks.getBreakSpeed
		net.minecraftforge.event.entity.player.PlayerEvent.BreakSpeed event = new net.minecraftforge.event.entity.player.PlayerEvent.BreakSpeed(
				player, blockState, toolDestroySpeed, blockPos);
		net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
		if (!event.isCanceled()) {
			toolDestroySpeed = event.getNewSpeed();
		}

		return toolDestroySpeed;
	}

	public boolean tryBreakBlock(ItemStack tool, FakePlayer fakePlayer, BlockPos breakPos, BlockState blockState) {
		setFakePlayerData(tool, fakePlayer, breakPos, getFacing());
		BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(level, breakPos, level.getBlockState(breakPos),
				fakePlayer);
		if (MinecraftForge.EVENT_BUS.post(event))
			return false;
		breakBlock(fakePlayer, breakPos, tool, blockState);
		return true;
	}

	public void breakBlock(FakePlayer player, BlockPos breakPos, ItemStack itemStack, BlockState state) {
		itemStack.onBlockStartBreak(breakPos, player);
		BlockEntity blockEntity = level.getBlockEntity(breakPos);
		if (level.destroyBlock(breakPos, false, player)) {
			Block.dropResources(state, level, breakPos, blockEntity, player, itemStack);
			if (state.getDestroySpeed(level, breakPos) != 0.0F)
				itemStack.hurtAndBreak(1, player, pOnBroken -> {
				});
		}
	}

	public void sendPackets(int pBreakerId, BlockPos pPos, int pProgress) {
		for (ServerPlayer serverplayer : level.getServer().getPlayerList().getPlayers()) {
			if (serverplayer != null && serverplayer.level() == level && serverplayer.getId() != pBreakerId) {
				double d0 = (double) pPos.getX() - serverplayer.getX();
				double d1 = (double) pPos.getY() - serverplayer.getY();
				double d2 = (double) pPos.getZ() - serverplayer.getZ();
				if (d0 * d0 + d1 * d1 + d2 * d2 < 1024.0) {
					serverplayer.connection.send(new ClientboundBlockDestructionPacket(pBreakerId, pPos, pProgress));
				}
			}
		}
	}

	@Override
	public boolean isDefaultSettings() {
		if (!super.isDefaultSettings())
			return false;
		if (tickSpeed != 20)
			return false;
		if (!getRedstoneControlData().equals(getDefaultRedstoneData()))
			return false;
		if (sneaking)
			return false;
		return true;
	}

	@Override
	public void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		tag.putBoolean("sneaking", sneaking);
	}

	@Override
	public void load(CompoundTag tag) {
		this.sneaking = tag.getBoolean("sneaking");
		super.load(tag);
	}
}
