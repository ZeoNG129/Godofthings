package com.godofthings.handler;

import com.godofthings.Godofthings;
import com.godofthings.item.ConfigurableMachine;
import com.godofthings.item.GodConfigData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

/**
 * 机器的「拆卸 / 放置」状态搬运（对齐 Mekanism 的拆机手感）：
 * <ul>
 *   <li><b>挖掉</b>：把六面 I/O 模式、AE 并网开关、加速槽里的神之加速数量写进掉落的方块物品
 *       （数据组件 {@code godofthings:machine_state}），<b>加速不再单独掉成物品</b>，
 *       而是「跟着机器走」——放回去还在里面，所以既不会丢也不会多出来。</li>
 *   <li><b>放置</b>：方块类在自己的 {@code setPlacedBy} 里调 {@link #applyOnPlace}，
 *       把存下的配置与加速数量写回新的方块实体。</li>
 * </ul>
 *
 * <p>用 {@link BlockDropsEvent}（能拿到方块实体 + 掉落列表）而不是逐台机器改掉落表：
 * 一个事件覆盖所有实现 {@link ConfigurableMachine} 的机器，以后新增机器也自动生效。</p>
 */
@EventBusSubscriber(modid = Godofthings.MODID)
public final class MachineStateHandler
{
    private MachineStateHandler() {}

    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event)
    {
        if (!(event.getBlockEntity() instanceof ConfigurableMachine machine))
        {
            return;
        }
        String blockId = String.valueOf(BuiltInRegistries.BLOCK.getKey(event.getState().getBlock()));
        GodConfigData data = new GodConfigData(blockId,
                machine.getFaceModesSnapshot(), machine.isAeEnabled(), machine.acceleratorCount());
        var machineBlock = event.getState().getBlock();
        for (ItemEntity drop : event.getDrops())
        {
            ItemStack stack = drop.getItem();
            if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)
                    || blockItem.getBlock() != machineBlock)
            {
                continue;
            }
            ItemStack carried = stack.copy();
            carried.set(Godofthings.MACHINE_STATE_DATA.get(), data);
            drop.setItem(carried);
        }
    }

    /**
     * 放置时还原配置：六面模式 / AE 开关 / 加速数量。
     * 组装机（{@code GodMinerItem} 自己放方块）也走这里，所以矿机同样是「拆了再放，配置还在」。
     */
    public static void applyOnPlace(Level level, BlockPos pos, ItemStack stack)
    {
        if (level.isClientSide || stack == null || stack.isEmpty())
        {
            return;
        }
        GodConfigData data = stack.get(Godofthings.MACHINE_STATE_DATA.get());
        if (data == null || !data.hasData())
        {
            return;
        }
        if (!(level.getBlockEntity(pos) instanceof ConfigurableMachine machine))
        {
            return;
        }
        machine.applyFaceModes(data.copyFaceModes());
        machine.setAeEnabled(data.aeEnabled());
        if (data.accelCount() > 0)
        {
            machine.addAccelerators(data.accelCount());
        }
    }
}
