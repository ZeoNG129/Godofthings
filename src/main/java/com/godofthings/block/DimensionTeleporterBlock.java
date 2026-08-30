package com.godofthings.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 维度传送方块：右键在主世界与目标维度之间往返。
 * 移植自 useless_mod 的 teleport_block 逻辑（1.20.1）。
 */
public class DimensionTeleporterBlock extends Block
{
    private final ResourceKey<Level> targetDimension;
    private final String enterMessageKey;

    public DimensionTeleporterBlock(ResourceKey<Level> targetDimension, String enterMessageKey, Properties properties)
    {
        super(properties);
        this.targetDimension = targetDimension;
        this.enterMessageKey = enterMessageKey;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit)
    {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer)
        {
            // 异步处理传送，避免阻塞主线程
            level.getServer().execute(() -> handleTeleport(serverPlayer, pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    private void handleTeleport(ServerPlayer player, BlockPos sourcePos)
    {
        ResourceKey<Level> currentDimension = player.level().dimension();
        boolean inTarget = currentDimension.equals(targetDimension);
        ResourceKey<Level> destination = inTarget ? Level.OVERWORLD : targetDimension;

        ServerLevel targetLevel = player.server.getLevel(destination);
        if (targetLevel == null)
        {
            player.displayClientMessage(Component.translatable("message.godofthings.dimension_missing"), false);
            return;
        }

        // 预加载目标区块
        targetLevel.getChunk(sourcePos.getX() >> 4, sourcePos.getZ() >> 4);

        // 查找或创建目标传送方块
        BlockPos targetPos = findOrCreateTeleportBlock(targetLevel, sourcePos);
        if (targetPos == null)
        {
            player.displayClientMessage(Component.translatable("message.godofthings.tp_failed"), false);
            return;
        }

        player.teleportTo(targetLevel,
                targetPos.getX() + 0.5,
                targetPos.getY() + 1.0,
                targetPos.getZ() + 0.5,
                player.getYRot(),
                player.getXRot());

        if (inTarget)
        {
            player.displayClientMessage(Component.translatable("message.godofthings.tp_return",
                    Component.translatable("dimension.minecraft.overworld")), false);
        }
        else
        {
            player.displayClientMessage(Component.translatable(enterMessageKey), false);
        }
    }

    private BlockPos findOrCreateTeleportBlock(ServerLevel level, BlockPos sourcePos)
    {
        int searchRadius = 16;
        BlockPos.MutableBlockPos searchPos = new BlockPos.MutableBlockPos();

        // 优先搜索同一 Y 层和相邻层
        int[] priorityYLevels = {
                sourcePos.getY(),      // 同一层（最高优先级）
                sourcePos.getY() - 1,  // 下一层（高优先级）
                sourcePos.getY() + 1,  // 上一层（高优先级）
                sourcePos.getY() - 2,
                sourcePos.getY() + 2,
                sourcePos.getY() - 3,
                sourcePos.getY() + 3
        };

        for (int y : priorityYLevels)
        {
            if (y < level.getMinBuildHeight() || y >= level.getMaxBuildHeight())
            {
                continue;
            }

            BlockPos found = searchAtYLevel(level, sourcePos, y, searchRadius);
            if (found != null)
            {
                return found;
            }
        }

        // 向下扩展搜索
        for (int y = sourcePos.getY() - 4; y >= level.getMinBuildHeight() + 1; y--)
        {
            BlockPos found = searchAtYLevel(level, sourcePos, y, searchRadius);
            if (found != null)
            {
                return found;
            }
        }

        // 向上扩展搜索
        for (int y = sourcePos.getY() + 4; y < level.getMaxBuildHeight() - 10; y++)
        {
            BlockPos found = searchAtYLevel(level, sourcePos, y, searchRadius);
            if (found != null)
            {
                return found;
            }
        }

        return createTeleportBlockFast(level, sourcePos);
    }

    private BlockPos searchAtYLevel(ServerLevel level, BlockPos center, int y, int radius)
    {
        BlockPos.MutableBlockPos searchPos = new BlockPos.MutableBlockPos();

        for (int currentRadius = 0; currentRadius <= radius; currentRadius++)
        {
            for (int x = -currentRadius; x <= currentRadius; x++)
            {
                for (int z = -currentRadius; z <= currentRadius; z++)
                {
                    // 只搜索边界，避免重复搜索内部
                    if (Math.abs(x) != currentRadius && Math.abs(z) != currentRadius)
                    {
                        continue;
                    }

                    searchPos.set(center.getX() + x, y, center.getZ() + z);

                    if (isValidTeleportBlock(level, searchPos))
                    {
                        return searchPos.immutable();
                    }
                }
            }
        }

        return null;
    }

    private boolean isValidTeleportBlock(ServerLevel level, BlockPos pos)
    {
        return level.isLoaded(pos) && level.getBlockState(pos).getBlock() == this;
    }

    private BlockPos createTeleportBlockFast(ServerLevel level, BlockPos sourcePos)
    {
        int[] safeHeights = {64, 80, 96, 112, 128};

        for (int height : safeHeights)
        {
            BlockPos testPos = new BlockPos(sourcePos.getX(), height, sourcePos.getZ());

            if (canPlaceTeleportBlockFast(level, testPos))
            {
                level.setBlock(testPos, this.defaultBlockState(), 3);
                return testPos;
            }
        }

        // 使用原版世界生成高度
        BlockPos worldSurfacePos = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, sourcePos);
        BlockPos placementPos = worldSurfacePos.above(2);

        if (canPlaceTeleportBlockFast(level, placementPos))
        {
            level.setBlock(placementPos, this.defaultBlockState(), 3);
            return placementPos;
        }

        // 最后兜底：直接在地表创建，并在下方垫一块石头避免掉落
        level.setBlock(placementPos, this.defaultBlockState(), 3);
        level.setBlock(placementPos.below(), Blocks.STONE.defaultBlockState(), 3);

        return placementPos;
    }

    private boolean canPlaceTeleportBlockFast(ServerLevel level, BlockPos pos)
    {
        if (pos.getY() < level.getMinBuildHeight() || pos.getY() >= level.getMaxBuildHeight())
        {
            return false;
        }

        return level.getBlockState(pos).canBeReplaced() &&
                level.getBlockState(pos.below()).isSolid();
    }
}
