package com.direwolf20.justdirethings.util;

import com.direwolf20.justdirethings.datagen.JustDireBlockTags;
import com.mojang.math.Axis;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class MiscTools {
	// Thanks Soaryn!
	@NotNull
	public static BlockHitResult getHitResult(Player player) {
		var playerLook = new Vec3(player.getX(), player.getY() + player.getEyeHeight(), player.getZ());
		var lookVec = player.getViewVector(1.0F);
		var reach = player.getBlockReach();
		var endLook = playerLook.add(lookVec.x * reach, lookVec.y * reach, lookVec.z * reach);
		BlockHitResult hitResult = player.level()
				.clip(new ClipContext(playerLook, endLook, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		return hitResult;
	}

	public static boolean inBounds(int x, int y, int w, int h, double ox, double oy) {
		return ox >= x && ox <= x + w && oy >= y && oy <= y + h;
	}

	public static Vector3f findOffset(Direction direction, int slot, Vector3f[] offsets) {
		Vector3f offsetVector = new Vector3f(offsets[slot]);
		switch (direction) {
			case UP -> {
				Quaternionf quaternionf = Axis.XP.rotationDegrees(-270);
				offsetVector = quaternionf.transform(offsetVector);
				// offsetVector.transform(Vector3f.XP.rotationDegrees(-270));
				offsetVector.add(0, 1, 0);
			}
			case DOWN -> {
				Quaternionf quaternionf = Axis.XP.rotationDegrees(-90);
				offsetVector = quaternionf.transform(offsetVector);
				// offsetVector.transform(Vector3f.XP.rotationDegrees(-90));
				offsetVector.add(0, 0, 1);
				// reverse = false;
			}
			// case NORTH -> offsetVector;
			case EAST -> {
				Quaternionf quaternionf = Axis.YP.rotationDegrees(-90);
				offsetVector = quaternionf.transform(offsetVector);
				// offsetVector.transform(Vector3f.YP.rotationDegrees(-90));
				offsetVector.add(1, 0, 0);
			}
			case SOUTH -> {
				Quaternionf quaternionf = Axis.YP.rotationDegrees(-180);
				offsetVector = quaternionf.transform(offsetVector);
				// offsetVector.transform(Vector3f.YP.rotationDegrees(-180));
				offsetVector.add(1, 0, 1);
			}
			case WEST -> {
				Quaternionf quaternionf = Axis.YP.rotationDegrees(-270);
				offsetVector = quaternionf.transform(offsetVector);
				// offsetVector.transform(Vector3f.YP.rotationDegrees(-270));
				offsetVector.add(0, 0, 1);
			}
		}
		return offsetVector;
	}

	public static ListTag stringListToNBT(List<String> list) {
		ListTag nbtList = new ListTag();
		for (String string : list) {
			CompoundTag tag = new CompoundTag();
			tag.putString("list", string);
			nbtList.add(tag);
		}
		return nbtList;
	}

	public static List<String> NBTToStringList(ListTag nbtList) {
		List<String> list = new ArrayList<>();
		for (int i = 0; i < nbtList.size(); i++) {
			CompoundTag tag = nbtList.getCompound(i);
			list.add(tag.getString("list"));
		}
		return list;
	}

	public static MutableComponent tooltipMaker(String string, int color) {
		Style style = Style.EMPTY;
		style = style.withColor(color);
		MutableComponent current = Component.translatable(string);
		current.setStyle(style);
		return current;
	}

	public static boolean isValidTickAccelBlock(ServerLevel level, BlockState state, @Nullable BlockEntity be) {
		if (be == null)
			return false;
		if (state.is(JustDireBlockTags.TICK_SPEED_DENY))
			return false;
		if (isTimeWandBlacklisted(state))
			return false;
		return true;
	}

	private static boolean isTimeWandBlacklisted(BlockState state) {
		ResourceLocation key = ForgeRegistries.BLOCKS.getKey(state.getBlock());
		if (key == null)
			return false;
		return matchesRegexBlacklist(List.of(), key.toString());
	}

	private static final Map<String, Pattern> BLACKLIST_PATTERN_CACHE = new ConcurrentHashMap<>();

	public static boolean matchesRegexBlacklist(List<? extends String> blacklist, String fullId) {
		for (String regex : blacklist) {
			Pattern pattern = BLACKLIST_PATTERN_CACHE.computeIfAbsent(regex, Pattern::compile);
			if (pattern.matcher(fullId).find())
				return true;
		}
		return false;
	}

	public static void doExtraTicks(ServerLevel level, BlockPos pos, int extraTicks) {
		BlockState state = level.getBlockState(pos);
		BlockEntity be = level.getBlockEntity(pos);
		if (!isValidTickAccelBlock(level, state, be))
			return;
		for (int i = 0; i < extraTicks; i++) {
			tickBlockEntity(level, pos, state, be);
		}
	}

	@SuppressWarnings("unchecked")
	private static <T extends BlockEntity> void tickBlockEntity(Level level, BlockPos pos, BlockState state, T be) {
		if (!(state.getBlock() instanceof EntityBlock entityBlock))
			return;
		BlockEntityTicker<T> ticker = entityBlock.getTicker(level, state, (BlockEntityType<T>) be.getType());
		if (ticker != null) {
			ticker.tick(level, pos, state, be);
		}
	}

	@Nullable
	public static Entity getEntityLookedAt(Player player, double distance) {
		Vec3 eyePos = player.getEyePosition();
		Vec3 lookVec = player.getViewVector(1.0F);
		Vec3 endPos = eyePos.add(lookVec.x * distance, lookVec.y * distance, lookVec.z * distance);
		AABB searchBox = player.getBoundingBox().expandTowards(lookVec.scale(distance)).inflate(1.0);
		List<Entity> entities = player.level().getEntities(player, searchBox, e -> !e.isSpectator() && e.isPickable());
		Entity closest = null;
		double closestDist = distance;
		for (Entity entity : entities) {
			AABB entityBB = entity.getBoundingBox().inflate(0.3);
			Optional<Vec3> hit = entityBB.clip(eyePos, endPos);
			if (hit.isPresent()) {
				double dist = eyePos.distanceTo(hit.get());
				if (dist < closestDist) {
					closestDist = dist;
					closest = entity;
				}
			} else if (entityBB.contains(eyePos)) {
				// Eye is inside the entity's hitbox (very close to a large mob).
				// AABB.clip requires the ray to start outside, so we check containment
				// directly.
				double dist = eyePos.distanceTo(entity.position());
				if (dist < closestDist) {
					closestDist = dist;
					closest = entity;
				}
			}
		}
		return closest;
	}
}
