package com.direwolf20.justdirethings.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;

import java.util.Random;

public class MiscHelpers {
	public enum RedstoneMode {
		IGNORED, LOW, HIGH, PULSE;

		public RedstoneMode next() {
			RedstoneMode[] values = values();
			int nextOrdinal = (this.ordinal() + 1) % values.length;
			return values[nextOrdinal];
		}
	}
	private static final Random rand = new Random();

	public static double nextDouble(double min, double max) {
		return min + (max - min) * rand.nextDouble();
	}

	public static IItemHandler getAttachedInventory(Level level, BlockPos blockPos, Direction side) {
		if (level == null)
			return null;
		BlockEntity be = level.getBlockEntity(blockPos);
		// if we have a TE and its an item handler, try extracting from that
		if (be != null) {
			return be.getCapability(ForgeCapabilities.ITEM_HANDLER, side).orElse(null);
		}
		return null;
	}

	public static Direction getPrimaryDirection(Vec3 vec) {
		Direction best = Direction.NORTH;
		double bestDot = Double.NEGATIVE_INFINITY;
		for (Direction dir : Direction.values()) {
			double dot = vec.dot(new Vec3(dir.getStepX(), dir.getStepY(), dir.getStepZ()));
			if (dot > bestDot) {
				bestDot = dot;
				best = dir;
			}
		}
		return best;
	}

	public static Direction getFacingDirection(Player player) {
		return player.getDirection();
	}
}
