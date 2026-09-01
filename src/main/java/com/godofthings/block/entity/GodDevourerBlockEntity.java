package com.godofthings.block.entity;

import com.godofthings.Godofthings;
import com.godofthings.handler.DevourerItemHandler;
import com.godofthings.menu.GodDevourerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

/**
 * 神之吞噬方块实体：物品暂存于 {@link DevourerItemHandler}，退出界面时销毁；
 * 液体经 {@link FluidTank} 每 tick 排空销毁；物品/液体能力在六面全部开放。
 */
public class GodDevourerBlockEntity extends BlockEntity implements MenuProvider
{
    /** 吞噬槽数量（大箱子 9×6） */
    public static final int SLOT_COUNT = 54;

    private final DevourerItemHandler itemHandler = new DevourerItemHandler(SLOT_COUNT)
    {
        @Override
        protected void onContentsChanged(int slot)
        {
            setChanged();
        }
    };
    private final FluidTank fluidTank = new FluidTank(1_000_000_000);
    /** 当前打开菜单的玩家数（服务端计数，用于「退出界面再销毁」） */
    private int viewers = 0;

    public GodDevourerBlockEntity(BlockPos pos, BlockState state)
    {
        super(Godofthings.GOD_DEVOURER_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, GodDevourerBlockEntity be)
    {
        if (level.isClientSide)
        {
            return;
        }
        // 液体每 tick 销毁
        if (be.fluidTank.getFluidAmount() > 0)
        {
            be.fluidTank.drain(be.fluidTank.getFluidAmount(), IFluidHandler.FluidAction.EXECUTE);
        }
        // 物品：无玩家打开菜单时销毁（兜底六面管道输入）
        if (be.viewers <= 0 && !be.itemHandler.isEmpty())
        {
            be.itemHandler.clear();
        }
    }

    public DevourerItemHandler getItemHandler()
    {
        return itemHandler;
    }

    public FluidTank getFluidTank()
    {
        return fluidTank;
    }

    /** 玩家打开菜单（服务端） */
    public void onMenuOpened()
    {
        viewers++;
    }

    /** 玩家关闭菜单（服务端），最后一个玩家关闭时销毁物品 */
    public void onMenuClosed()
    {
        viewers = Math.max(0, viewers - 1);
        if (viewers == 0)
        {
            itemHandler.clear();
        }
    }

    @Override
    public Component getDisplayName()
    {
        return Component.translatable("block.godofthings.god_devourer");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInv, Player player)
    {
        return new GodDevourerMenu(containerId, playerInv, this);
    }

    @EventBusSubscriber(modid = Godofthings.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static class CapabilityRegistration
    {
        @SubscribeEvent
        public static void onRegisterCapabilities(RegisterCapabilitiesEvent event)
        {
            // 六面全部开放：物品暂存，退出界面销毁
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, Godofthings.GOD_DEVOURER_BE.get(),
                    (be, side) -> be.itemHandler);
            // 六面全部开放：液体每 tick 销毁
            event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, Godofthings.GOD_DEVOURER_BE.get(),
                    (be, side) -> be.fluidTank);
        }
    }
}
