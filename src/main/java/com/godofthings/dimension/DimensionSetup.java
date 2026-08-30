package com.godofthings.dimension;

import com.godofthings.Godofthings;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 维度初始化处理。
 */
@Mod.EventBusSubscriber(modid = Godofthings.MODID)
public class DimensionSetup
{
    // 神之平坦平台顶部 Y=5（基岩 -64 + 69 层填充），站立在 Y=6
    private static final BlockPos FLAT_SPAWN = new BlockPos(8, 6, 8);
    // 神之虚空重生点（脚下放置传送方块，便于重生后传送回主世界）
    private static final BlockPos VOID_SPAWN = new BlockPos(8, 70, 8);

    @SubscribeEvent
    public static void onServerLevelLoad(LevelEvent.Load event)
    {
        if (event.getLevel() instanceof ServerLevel serverLevel)
        {
            if (serverLevel.dimension() == Godofthings.SUPERFLAT_DIMENSION)
            {
                serverLevel.setDefaultSpawnPos(FLAT_SPAWN, 0.0F);
            }
            if (serverLevel.dimension() == Godofthings.VOID_DIMENSION)
            {
                serverLevel.setDefaultSpawnPos(VOID_SPAWN, 0.0F);
                serverLevel.setBlockAndUpdate(VOID_SPAWN.below(), Godofthings.VOID_TELEPORTER.get().defaultBlockState());
            }
        }
    }
}
