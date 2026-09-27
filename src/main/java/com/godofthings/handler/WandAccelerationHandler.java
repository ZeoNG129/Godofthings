package com.godofthings.handler;

import com.godofthings.Godofthings;
import com.godofthings.item.GodFavorWandItem;
import com.godofthings.item.WandAcceleration;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 神之工具的「时间加速」交互与驱动。
 *
 * <h3>操作</h3>
 * <ul>
 *   <li><b>潜行 + 右键方块</b>：给该方块挂加速（默认 2×，持续 30 秒）；<b>对同一方块再按一次倍率翻倍</b>
 *       （2→4→8→…→1024→回到 2）</li>
 *   <li><b>潜行 + 右键避雷针</b>：直接引雷（倍率 = 每次落雷数量，上限 8）</li>
 *   <li><b>潜行 + 右键空气（看向天空）</b>：推进时间 {@link WandAcceleration#TIME_SKIP} tick</li>
 * </ul>
 * 只在<b>主手或副手拿着神之工具</b>时生效。
 */
@EventBusSubscriber(modid = Godofthings.MODID)
public final class WandAccelerationHandler
{
    private WandAccelerationHandler() {}

    /** 手上是否拿着神之工具（主手或副手） */
    private static boolean holdingWand(ServerPlayer player)
    {
        return player.getMainHandItem().getItem() instanceof GodFavorWandItem
                || player.getOffhandItem().getItem() instanceof GodFavorWandItem;
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !player.isShiftKeyDown()
                || !holdingWand(player))
        {
            return;
        }
        if (!(player.level() instanceof ServerLevel level))
        {
            return;
        }
        BlockPos pos = event.getPos().immutable();
        int speed = WandAcceleration.mark(level, pos, false);
        player.displayClientMessage(Component.translatable("chat.godofthings.wand.accel",
                pos.getX(), pos.getY(), pos.getZ(), speed,
                WandAcceleration.DEFAULT_DURATION_TICKS / 20).withStyle(ChatFormatting.AQUA), true);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRightClickEmpty(PlayerInteractEvent.RightClickEmpty event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !player.isShiftKeyDown()
                || !holdingWand(player)
                || !(player.level() instanceof ServerLevel level))
        {
            return;
        }
        // 看向天空时才推进时间（避免误触）
        if (player.getXRot() > -40.0F)
        {
            return;
        }
        level.setDayTime(level.getDayTime() + WandAcceleration.TIME_SKIP);
        player.displayClientMessage(Component.translatable("chat.godofthings.wand.time",
                WandAcceleration.TIME_SKIP).withStyle(ChatFormatting.GOLD), true);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event)
    {
        for (ServerLevel level : event.getServer().getAllLevels())
        {
            WandAcceleration.tickAll(level);
        }
    }
}