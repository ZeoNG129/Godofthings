package com.godofthings.block.entity;

import com.godofthings.Godofthings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 时空永恒方块实体：每 tick 将世界时间与天气锁定在放置时刻的状态。
 * - 时间锁定：首次 tick 记录当前 dayTime，之后每 tick 强制写回，时间永不流逝
 * - 天气锁定：首次 tick 记录是否下雨/雷暴，之后每 tick 强制写回
 */
public class SpaceTimeEternityBlockEntity extends BlockEntity
{
    /** 锁定值未初始化哨兵（dayTime 恒非负，-1 安全） */
    private long lockedDayTime = -1;
    private boolean lockedRaining;
    private boolean lockedThundering;

    public SpaceTimeEternityBlockEntity(BlockPos pos, BlockState state)
    {
        super(Godofthings.SPACE_TIME_ETERNITY_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SpaceTimeEternityBlockEntity be)
    {
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel))
        {
            return;
        }
        // 首次 tick 记录当前时间/天气作为锁定基准
        if (be.lockedDayTime < 0)
        {
            be.lockedDayTime = serverLevel.getDayTime();
            be.lockedRaining = serverLevel.isRaining();
            be.lockedThundering = serverLevel.isThundering();
            be.setChanged();
        }
        // 时间锁定
        serverLevel.setDayTime(be.lockedDayTime);
        // 天气锁定：晴天给足 clearWeatherTime 让它永不转雨；雨天给足 rainTime 让它永不转晴
        int clearTime = be.lockedRaining ? 0 : 1000000;
        int rainTime = be.lockedRaining ? 1000000 : 0;
        serverLevel.setWeatherParameters(clearTime, rainTime, be.lockedRaining, be.lockedThundering);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider)
    {
        super.saveAdditional(tag, provider);
        tag.putLong("LockedDayTime", lockedDayTime);
        tag.putBoolean("LockedRaining", lockedRaining);
        tag.putBoolean("LockedThundering", lockedThundering);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider)
    {
        super.loadAdditional(tag, provider);
        if (tag.contains("LockedDayTime"))
        {
            lockedDayTime = tag.getLong("LockedDayTime");
            lockedRaining = tag.getBoolean("LockedRaining");
            lockedThundering = tag.getBoolean("LockedThundering");
        }
    }
}
