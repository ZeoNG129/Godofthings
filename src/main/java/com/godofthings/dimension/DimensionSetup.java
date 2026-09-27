package com.godofthings.dimension;

import com.godofthings.Godofthings;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;

/**
 * 维度初始化处理。
 * 1.21.1 NeoForge：没有维度注册事件——void 维度由数据包 JSON 定义
 * （data/godofthings/dimension/void.json）；代码侧仅保留虚空维度的出生点设置
 * 与出生点下方那块传送方块的放置。
 * <p>超平坦维度（superflat）与其传送方块「神之平坦」已在 v5.1.2 按用户要求整体删除，
 * 连同区块生成器 godofthings:superflat_gen 一起移除。</p>
 */
@EventBusSubscriber(modid = Godofthings.MODID)
public class DimensionSetup
{
    // 神之虚空重生点（脚下放置传送方块，便于重生后传送回主世界）
    private static final BlockPos VOID_SPAWN = new BlockPos(8, 70, 8);

    @SubscribeEvent
    public static void onServerLevelLoad(LevelEvent.Load event)
    {
        if (event.getLevel() instanceof ServerLevel serverLevel)
        {
            if (serverLevel.dimension() == Godofthings.VOID_DIMENSION)
            {
                serverLevel.setDefaultSpawnPos(VOID_SPAWN, 0.0F);
                serverLevel.setBlockAndUpdate(VOID_SPAWN.below(), Godofthings.VOID_TELEPORTER.get().defaultBlockState());
            }
        }
    }
}
