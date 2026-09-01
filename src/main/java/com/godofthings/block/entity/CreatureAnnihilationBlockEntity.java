package com.godofthings.block.entity;

import com.godofthings.Godofthings;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 生物覆灭方块实体：作为标记方块，抑制以自身为中心 512 格内的自然生物生成。
 * - onLoad 时登记位置到全局缓存，onChunkUnloaded/setRemoved 时移除（仅服务端维护）
 * - SpawnHandler 监听 MobSpawnEvent.PositionCheck，自然生成落入抑制范围则拒绝
 */
public class CreatureAnnihilationBlockEntity extends BlockEntity
{
    /** 抑制半径（格） */
    public static final int RANGE = 512;

    /** 各维度已加载的生物覆灭方块位置（仅服务端维护） */
    private static final Map<ResourceKey<Level>, Set<BlockPos>> ACTIVE = new ConcurrentHashMap<>();

    public CreatureAnnihilationBlockEntity(BlockPos pos, BlockState state)
    {
        super(Godofthings.CREATURE_ANNIHILATION_BE.get(), pos, state);
    }

    @Override
    public void onLoad()
    {
        super.onLoad();
        if (level != null && !level.isClientSide)
        {
            ACTIVE.computeIfAbsent(level.dimension(), k -> ConcurrentHashMap.newKeySet()).add(getBlockPos());
        }
    }

    @Override
    public void onChunkUnloaded()
    {
        unregister();
        super.onChunkUnloaded();
    }

    @Override
    public void setRemoved()
    {
        unregister();
        super.setRemoved();
    }

    private void unregister()
    {
        if (level != null && !level.isClientSide)
        {
            Set<BlockPos> set = ACTIVE.get(level.dimension());
            if (set != null)
            {
                set.remove(getBlockPos());
                if (set.isEmpty())
                {
                    ACTIVE.remove(level.dimension());
                }
            }
        }
    }

    /** 判断某生成点是否落入任一生物覆灭方块的抑制范围 */
    public static boolean isSuppressed(ResourceKey<Level> dimension, BlockPos spawnPos)
    {
        Set<BlockPos> set = ACTIVE.get(dimension);
        if (set == null || set.isEmpty())
        {
            return false;
        }
        long rangeSq = (long) RANGE * RANGE;
        for (BlockPos p : set)
        {
            if (p.distSqr(spawnPos) < rangeSq)
            {
                return true;
            }
        }
        return false;
    }

    /** 生物生成位置检查事件：自然生成落入抑制范围时拒绝 */
    @EventBusSubscriber(modid = Godofthings.MODID)
    public static class SpawnHandler
    {
        @SubscribeEvent
        public static void onPositionCheck(MobSpawnEvent.PositionCheck event)
        {
            if (event.getSpawnType() != MobSpawnType.NATURAL)
            {
                return;
            }
            if (isSuppressed(event.getLevel().getLevel().dimension(), event.getEntity().blockPosition()))
            {
                event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
            }
        }
    }
}
