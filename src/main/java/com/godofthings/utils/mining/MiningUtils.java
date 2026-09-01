package com.godofthings.utils.mining;

import com.godofthings.item.WandItemUtils;
import com.godofthings.item.WandConfig;
import com.godofthings.item.WandModes;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 挖掘工具类：方块查找、破坏、掉落处理。
 * 移植自 useless_mod 的 MiningUtils（1.20.1）。
 */
public class MiningUtils
{
    // ==================== 方块查找 ====================

    public static List<BlockPos> findBlocksToMine(BlockPos originPos, BlockState originState, Level level,
                                                   ItemStack stack, boolean forceMining)
    {
        int maxBlocks = WandConfig.getChainMiningMaxBlocks();
        int rangeX = WandConfig.getChainMiningRangeX();
        int rangeY = WandConfig.getChainMiningRangeY();
        int rangeZ = WandConfig.getChainMiningRangeZ();

        Block originBlock = originState.getBlock();
        List<BlockPos> blocksToMine = new ArrayList<>(maxBlocks);

        if (!forceMining && !stack.isCorrectToolForDrops(originState))
        {
            return blocksToMine;
        }

        Queue<BlockPos> queue = new LinkedList<>();
        LongOpenHashSet visited = new LongOpenHashSet(maxBlocks * 2);
        queue.add(originPos);
        visited.add(originPos.asLong());

        while (!queue.isEmpty() && blocksToMine.size() < maxBlocks)
        {
            BlockPos currentPos = queue.poll();
            blocksToMine.add(currentPos);
            int cx = currentPos.getX();
            int cy = currentPos.getY();
            int cz = currentPos.getZ();

            for (int x = -1; x <= 1; x++)
            {
                for (int y = -1; y <= 1; y++)
                {
                    for (int z = -1; z <= 1; z++)
                    {
                        if (x == 0 && y == 0 && z == 0) continue;
                        int nx = cx + x;
                        int ny = cy + y;
                        int nz = cz + z;

                        if (Math.abs(nx - originPos.getX()) > rangeX ||
                                Math.abs(ny - originPos.getY()) > rangeY ||
                                Math.abs(nz - originPos.getZ()) > rangeZ) continue;

                        long nLong = BlockPos.asLong(nx, ny, nz);
                        if (visited.contains(nLong)) continue;

                        BlockPos neighborPos = new BlockPos(nx, ny, nz);
                        BlockState nextState = level.getBlockState(neighborPos);
                        if (nextState.getBlock() == originBlock)
                        {
                            if (forceMining || stack.isCorrectToolForDrops(nextState))
                            {
                                visited.add(nLong);
                                queue.add(neighborPos);
                            }
                        }
                    }
                }
            }
        }

        blocksToMine.sort(Comparator.comparingDouble(pos -> pos.distSqr(originPos)));
        return blocksToMine;
    }

    public static List<BlockPos> findBlocksToMineEnhanced(BlockPos originPos, BlockState originState, Level level,
                                                           ItemStack stack, boolean forceMining)
    {
        int maxBlocks = WandConfig.getChainMiningMaxBlocks();
        int rangeX = WandConfig.getChainMiningRangeX();
        int rangeY = WandConfig.getChainMiningRangeY();
        int rangeZ = WandConfig.getChainMiningRangeZ();

        Block originBlock = originState.getBlock();
        List<BlockPos> blocksToMine = new ArrayList<>(maxBlocks);

        for (int x = -rangeX; x <= rangeX && blocksToMine.size() < maxBlocks; x++)
        {
            for (int y = -rangeY; y <= rangeY && blocksToMine.size() < maxBlocks; y++)
            {
                for (int z = -rangeZ; z <= rangeZ && blocksToMine.size() < maxBlocks; z++)
                {
                    int nx = originPos.getX() + x;
                    int ny = originPos.getY() + y;
                    int nz = originPos.getZ() + z;
                    BlockPos targetPos = new BlockPos(nx, ny, nz);
                    BlockState nextState = level.getBlockState(targetPos);
                    if (nextState.getBlock() == originBlock)
                    {
                        if (forceMining || stack.isCorrectToolForDrops(nextState))
                        {
                            blocksToMine.add(targetPos);
                        }
                    }
                }
            }
        }

        blocksToMine.sort(Comparator.comparingDouble(pos -> pos.distSqr(originPos)));
        return blocksToMine;
    }

    // ==================== 方块破坏 ====================

    public static void processBlockBreak(ServerLevel level, BlockPos pos, BlockState state, Player player,
                                          ItemStack tool, boolean forceMining)
    {
        List<ItemStack> fallbackDrops = forceMining ? getForcedFallbackDrops(state, level, pos, tool) : Collections.emptyList();
        List<ItemStack> drops = destroyBlockAndCollectDrops(level, pos, state, player, tool);
        if (forceMining && hasNoValidDrops(drops) && !hasNoValidDrops(fallbackDrops))
        {
            drops = fallbackDrops;
        }
        handleDrops(player, drops, tool);

        if (!isSilkTouchMode(tool))
        {
            state.getBlock().popExperience(level, pos, state.getBlock().getExpDrop(state, level, pos, null, null, tool));
        }
    }

    public static List<ItemStack> getForcedFallbackDrops(BlockState state, ServerLevel level, BlockPos pos, ItemStack tool)
    {
        BlockEntity be = level.getBlockEntity(pos);
        ItemStack stack = new ItemStack(state.getBlock().asItem());
        if (stack.isEmpty() || stack.getItem() == Items.AIR)
        {
            return Collections.emptyList();
        }

        boolean isSilk = isSilkTouchMode(tool);
        if (isSilk && be != null)
        {
            // 1.21.1：容器内容走数据组件（原 NBT "BlockEntityTag" 方案已废弃）
            stack.applyComponents(be.collectComponents());
        }
        return Collections.singletonList(stack);
    }

    public static boolean hasNoValidDrops(List<ItemStack> drops)
    {
        return drops.isEmpty() || drops.stream().allMatch(stack -> stack.isEmpty() || stack.getItem() == Items.AIR);
    }

    public static List<ItemStack> destroyBlockAndCollectDrops(ServerLevel level, BlockPos pos, BlockState state,
                                                               Player player, ItemStack tool)
    {
        AABB area = new AABB(pos).inflate(1.0);
        Set<UUID> before = level.getEntitiesOfClass(ItemEntity.class, area)
                .stream()
                .map(Entity::getUUID)
                .collect(Collectors.toSet());

        destroyBlockWithoutDrops(level, pos, state, player, tool);

        List<ItemStack> drops = new ArrayList<>();
        level.getEntitiesOfClass(ItemEntity.class, area).stream()
                .filter(entity -> !before.contains(entity.getUUID()))
                .forEach(entity ->
                {
                    ItemStack drop = entity.getItem().copy();
                    if (!drop.isEmpty())
                    {
                        drops.add(drop);
                    }
                    entity.discard();
                });
        return drops;
    }

    public static void destroyBlockWithoutDrops(ServerLevel level, BlockPos pos, BlockState state,
                                                 Player player, ItemStack tool)
    {
        BlockEntity be = level.getBlockEntity(pos);
        state.getBlock().playerWillDestroy(level, pos, state, player);
        state.getBlock().playerDestroy(level, player, pos, state, be, tool);
        level.removeBlock(pos, false);
    }

    // ==================== 掉落物处理 ====================

    public static List<ItemStack> mergeItemStacks(List<ItemStack> items)
    {
        List<ItemStack> merged = new ArrayList<>();
        for (ItemStack item : items)
        {
            if (item.isEmpty()) continue;
            boolean mergedFlag = false;
            for (ItemStack mergedItem : merged)
            {
                if (ItemStack.isSameItemSameComponents(item, mergedItem))
                {
                    int remaining = mergedItem.getMaxStackSize() - mergedItem.getCount();
                    if (remaining > 0)
                    {
                        int addCount = Math.min(remaining, item.getCount());
                        mergedItem.grow(addCount);
                        item.shrink(addCount);
                        if (item.isEmpty())
                        {
                            mergedFlag = true;
                            break;
                        }
                    }
                }
            }
            if (!mergedFlag && !item.isEmpty())
            {
                merged.add(item.copy());
            }
        }
        return merged;
    }

    public static void handleDrops(Player player, List<ItemStack> drops)
    {
        handleDrops(player, drops, player.getMainHandItem());
    }

    /** 处理掉落物：AE2 优先存储 -> 玩家背包 -> 掉落。 */
    public static void handleDrops(Player player, List<ItemStack> drops, ItemStack tool)
    {
        for (ItemStack drop : drops)
        {
            if (drop.isEmpty()) continue;
            if (!WandItemUtils.addToInventoryOrAE(player, drop, tool))
            {
                player.drop(drop, false);
            }
        }
    }

    // ==================== 工具方法 ====================

    public static BlockPos getTargetBlockPos(Player player)
    {
        double reach = 4.5D;
        HitResult hitResult = player.pick(reach, 0.0f, false);
        if (hitResult.getType() == HitResult.Type.BLOCK)
        {
            return ((BlockHitResult) hitResult).getBlockPos();
        }
        return null;
    }

    public static boolean isSilkTouchMode(ItemStack stack)
    {
        return WandModes.getBoolean(stack, WandModes.SILK_TOUCH_MODE);
    }

    public static boolean isEnhancedChainMiningMode(ItemStack stack)
    {
        return WandModes.getBoolean(stack, WandModes.ENHANCED_CHAIN_MINING);
    }

    public static boolean isChainMiningPressed(ItemStack stack)
    {
        CompoundTag tag = WandModes.getData(stack);
        if (tag.contains(WandModes.CHAIN_MINING_PRESSED))
        {
            return tag.getBoolean(WandModes.CHAIN_MINING_PRESSED);
        }
        if (tag.contains(WandModes.TOOL_MODES))
        {
            CompoundTag toolModes = tag.getCompound(WandModes.TOOL_MODES);
            // 键名沿用 1.20.1 原代码字面量 "CHAIN_MINING"（保存端写入的是小写 mode name，此分支为兼容保留）
            if (toolModes.contains("CHAIN_MINING"))
            {
                return toolModes.getBoolean("CHAIN_MINING");
            }
        }
        return false;
    }

    public static boolean isForceMiningMode(ItemStack stack)
    {
        CompoundTag tag = WandModes.getData(stack);
        if (tag.contains(WandModes.TOOL_MODES))
        {
            CompoundTag toolModes = tag.getCompound(WandModes.TOOL_MODES);
            if (toolModes.contains("force_mining"))
            {
                return toolModes.getBoolean("force_mining");
            }
            if (toolModes.contains("FORCE_MINING"))
            {
                return toolModes.getBoolean("FORCE_MINING");
            }
        }
        return false;
    }

    public static void quickBreakBlock(Level world, BlockPos pos, BlockState state, Player player, ItemStack tool)
    {
        if (world.isClientSide())
        {
            world.playSound(player, pos, state.getSoundType().getBreakSound(), SoundSource.BLOCKS, 0.7F, 1.0F);
            return;
        }
        ServerLevel serverLevel = (ServerLevel) world;
        BlockEntity blockEntity = world.getBlockEntity(pos);
        List<ItemStack> drops = Block.getDrops(state, serverLevel, pos, blockEntity, player, tool);
        handleDrops(player, drops, tool);
        world.destroyBlock(pos, false, player);
    }
}
