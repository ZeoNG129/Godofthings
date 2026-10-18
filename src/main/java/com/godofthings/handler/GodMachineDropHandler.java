package com.godofthings.handler;

import com.godofthings.godgrid.GodGridHost;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * 神之机器被玩家挖掉时，<b>只把加速槽里的神之加速掉出来</b>，其余内置物品照旧随方块一起消失。
 *
 * <p>为什么用全局事件而不是逐台机器改方块类：六台机器（熔炉 / 矿机 / 资源三机 / 去皮 / 掉落 / 怪蛋）
 * 都实现了 {@link GodGridHost}，一个事件就能统一处理，新增机器也自动生效。</p>
 *
 * <p>判定时机是 {@link BlockEvent.BreakEvent}（方块被玩家破坏、尚未移除）：此时方块实体还在，
 * 能读到加速槽；弹出后立即从槽里扣掉，避免随后的掉落表/清空逻辑重复产出。
 * 创造模式不弹（与原版"创造破坏不掉落内容物"一致），活塞/爆炸等非玩家破坏也不弹。</p>
 */
@EventBusSubscriber(modid = com.godofthings.Godofthings.MODID)
public final class GodMachineDropHandler
{
    private GodMachineDropHandler()
    {
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event)
    {
        if (!(event.getLevel() instanceof ServerLevel level))
        {
            return;
        }
        if (event.getPlayer() == null || event.getPlayer().isCreative())
        {
            return;
        }
        BlockEntity be = level.getBlockEntity(event.getPos());
        if (!(be instanceof GodGridHost host))
        {
            return;
        }
        IItemHandler accel = host.gridAccel();
        if (accel == null || accel.getSlots() <= 0)
        {
            return;
        }
        ItemStack stack = accel.getStackInSlot(0);
        if (stack.isEmpty())
        {
            return;
        }
        ItemStack drop = stack.copy();
        accel.extractItem(0, drop.getCount(), false);
        Block.popResource(level, event.getPos(), drop);
    }
}
