package com.godofthings.block.entity;

import com.godofthings.Godofthings;
import com.godofthings.block.entity.machine.LogStripper;
import com.godofthings.config.MachinesConfig;
import com.godofthings.menu.GodPeelerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * 神之去皮方块实体：1 输入槽 + 1 输出槽，无需能源。
 *
 * <p>每 {@link MachinesConfig#PEELER_WORK_INTERVAL} tick 把 1 个原木去皮成对应去皮原木
 * （映射见 {@link LogStripper}）。漏斗 / 管道可从任意面塞入原木（只进不出）、取走去皮原木（只出不进）。
 * 打掉时物品一并消失，不掉落（同神之资源系列做法）。</p>
 */
public class GodPeelerBlockEntity extends BlockEntity implements MenuProvider
{
    /** 输入槽：只收可去皮原木 */
    private final ItemStackHandler inputSlot = new ItemStackHandler(1)
    {
        @Override
        protected void onContentsChanged(int slot)
        {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack)
        {
            return LogStripper.isStrippable(stack);
        }
    };
    /** 输出槽：只出不进（对外 insert 恒拒绝，GUI 里 Shift 点击也塞不进去） */
    private final ItemStackHandler outputSlot = new ItemStackHandler(1)
    {
        @Override
        protected void onContentsChanged(int slot)
        {
            setChanged();
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
        {
            return stack;
        }
    };

    private int tickCounter = 0;

    /** 漏斗 / 管道用的组合视图：槽 0 = 输入（只进），槽 1 = 输出（只出）。 */
    private final IItemHandler automationHandler = new IItemHandler()
    {
        @Override
        public int getSlots()
        {
            return 2;
        }

        @Override
        public ItemStack getStackInSlot(int slot)
        {
            return slot == 0 ? inputSlot.getStackInSlot(0) : outputSlot.getStackInSlot(0);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
        {
            if (slot != 0 || stack.isEmpty())
            {
                return stack;
            }
            return inputSlot.insertItem(0, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate)
        {
            if (slot != 1 || amount <= 0)
            {
                return ItemStack.EMPTY;
            }
            return outputSlot.extractItem(0, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot)
        {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack)
        {
            return slot == 0 && inputSlot.isItemValid(0, stack);
        }
    };

    public GodPeelerBlockEntity(BlockPos pos, BlockState state)
    {
        super(Godofthings.GOD_PEELER_BE.get(), pos, state);
    }

    public ItemStackHandler getInputSlot()
    {
        return inputSlot;
    }

    public ItemStackHandler getOutputSlot()
    {
        return outputSlot;
    }

    public IItemHandler getAutomationHandler()
    {
        return automationHandler;
    }

    // ---- 每 tick 逻辑 ----

    public static void tick(Level level, BlockPos pos, BlockState state, GodPeelerBlockEntity be)
    {
        if (!level.isClientSide)
        {
            be.tickServer();
        }
    }

    private void tickServer()
    {
        if (inputSlot.getStackInSlot(0).isEmpty())
        {
            tickCounter = 0;
            return;
        }
        // 每 workInterval tick 去皮 1 个（实时读配置，改 toml 重进世界即生效）
        tickCounter++;
        if (tickCounter >= MachinesConfig.PEELER_WORK_INTERVAL.get())
        {
            tickCounter = 0;
            convertOne(inputSlot, outputSlot);
        }
    }

    /**
     * 纯逻辑：把输入槽 1 个原木去皮并放入输出槽（输出为同物品且未满才动；供 GameTest 直接调用）。
     *
     * @return 是否发生了转化
     */
    public static boolean convertOne(ItemStackHandler input, ItemStackHandler output)
    {
        ItemStack result = LogStripper.strip(input.getStackInSlot(0));
        if (result.isEmpty())
        {
            return false;
        }
        ItemStack out = output.getStackInSlot(0);
        if (!out.isEmpty() && (!ItemStack.isSameItem(out, result) || out.getCount() >= out.getMaxStackSize()))
        {
            return false;
        }
        input.extractItem(0, 1, false);
        if (out.isEmpty())
        {
            output.setStackInSlot(0, result.copyWithCount(1));
        }
        else
        {
            out.grow(1);
            output.setStackInSlot(0, out);
        }
        return true;
    }

    // ---- capability：任意面塞入原木 / 取走成品 ----

    @EventBusSubscriber(modid = Godofthings.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static class CapabilityRegistration
    {
        @SubscribeEvent
        public static void registerCapabilities(RegisterCapabilitiesEvent event)
        {
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, Godofthings.GOD_PEELER_BE.get(),
                    (be, side) -> be.getAutomationHandler());
        }
    }

    // ---- NBT ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider)
    {
        super.saveAdditional(tag, provider);
        tag.put("InputSlot", inputSlot.serializeNBT(provider));
        tag.put("OutputSlot", outputSlot.serializeNBT(provider));
        tag.putInt("TickCounter", tickCounter);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider)
    {
        super.loadAdditional(tag, provider);
        if (tag.contains("InputSlot"))
        {
            inputSlot.deserializeNBT(provider, tag.getCompound("InputSlot"));
        }
        if (tag.contains("OutputSlot"))
        {
            outputSlot.deserializeNBT(provider, tag.getCompound("OutputSlot"));
        }
        tickCounter = tag.getInt("TickCounter");
    }

    // ---- MenuProvider ----

    @Override
    public Component getDisplayName()
    {
        return Component.translatable("block.godofthings.god_peeler");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player)
    {
        return new GodPeelerMenu(containerId, inventory, this);
    }
}
