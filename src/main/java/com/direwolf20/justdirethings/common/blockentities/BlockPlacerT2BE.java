package com.direwolf20.justdirethings.common.blockentities;

import com.direwolf20.justdirethings.common.blockentities.basebe.AreaAffectingBE;
import com.direwolf20.justdirethings.common.blockentities.basebe.FilterableBE;
import com.direwolf20.justdirethings.common.containers.handlers.FilterBasicHandler;
import com.direwolf20.justdirethings.setup.Registration;
import com.direwolf20.justdirethings.util.interfacehelpers.AreaAffectingData;
import com.direwolf20.justdirethings.util.interfacehelpers.FilterData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class BlockPlacerT2BE extends BlockPlacerT1BE implements AreaAffectingBE, FilterableBE {
	public FilterData filterData = new FilterData();
	public AreaAffectingData areaAffectingData = new AreaAffectingData();
	private final FilterBasicHandler filterHandler;

	public BlockPlacerT2BE(BlockPos pPos, BlockState pBlockState) {
		super(Registration.BlockPlacerT2BE.get(), pPos, pBlockState);
		areaAffectingData = new AreaAffectingData(pBlockState.getValue(BlockStateProperties.FACING));
		filterHandler = new FilterBasicHandler(9);
	}

	@Override
	public AreaAffectingData getAreaAffectingData() {
		return areaAffectingData;
	}

	@Override
	public FilterBasicHandler getFilterHandler() {
		return filterHandler;
	}

	@Override
	public FilterData getFilterData() {
		return filterData;
	}

	@Override
	public List<BlockPos> findSpotsToPlace(FakePlayer fakePlayer) {
		if (isEmptyScanOnCooldown())
			return List.of();
		AABB area = getAABB(getBlockPos());
		List<BlockPos> result = BlockPos
				.betweenClosedStream((int) area.minX, (int) area.minY, (int) area.minZ, (int) area.maxX - 1,
						(int) area.maxY - 1, (int) area.maxZ - 1)
				.filter(blockPos -> isBlockPosValid(fakePlayer, blockPos)).map(BlockPos::immutable)
				.sorted(Comparator.comparingDouble(x -> x.distSqr(getBlockPos()))).collect(Collectors.toList());
		if (result.isEmpty())
			setEmptyScanCooldown(20);
		return result;
	}

	@Override
	public boolean isBlockPosValid(FakePlayer fakePlayer, BlockPos blockPos) {
		if (!super.isBlockPosValid(fakePlayer, blockPos))
			return false; // Do the same checks as normal, then check the filters
		BlockPos adjacentPos = blockPos.relative(getDirectionValue());
		ItemStack blockItemStack = level.getBlockState(adjacentPos).getCloneItemStack(
				new BlockHitResult(Vec3.ZERO, getDirectionValue(), adjacentPos, false), level, adjacentPos, fakePlayer);
		return isStackValidFilter(blockItemStack);
	}

}
