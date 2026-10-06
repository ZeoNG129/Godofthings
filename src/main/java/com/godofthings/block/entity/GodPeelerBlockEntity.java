package com.godofthings.block.entity;

import com.godofthings.Godofthings;
import com.godofthings.block.entity.machine.LogStripper;
import com.godofthings.config.MachinesConfig;
import com.godofthings.item.GodAcceleratorItem;
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
 * 神之去皮方块实体（v5.15.1：1+1 槽改为与神之熔炉相同的 9+9 布局）。
 *
 * <p>9 个输入槽（0-8）+ 9 个输出槽（9-17，一一对应：输入 i → 输出 {@link #OUTPUT_SLOT_START} + i）+
 * 1 个神之加速槽（提升并行数量与槽位容量）。无需能源。</p>
 *
 * <p>每 {@link MachinesConfig#PEELER_WORK_INTERVAL} tick 处理一轮：<b>每个</b>非空输入槽把最多
 * 「并行倍率」个原木去皮塞进对应输出槽（同物品可叠、容量够才动）。
 * 漏斗 / 管道可从任意面塞入原木（只进输入槽）、取走去皮原木（只出输出槽）；
 * 输出槽对外部插入恒拒绝（GUI 里 Shift 点击也塞不进去）。
 * 打掉时物品一并消失，不掉落（同神之资源系列做法）。</p>
 */
public class GodPeelerBlockEntity extends BlockEntity implements MenuProvider
{
    public static final int INPUT_SLOT_COUNT = 9;
    public static final int OUTPUT_SLOT_COUNT = 9;
    public static final int OUTPUT_SLOT_START = INPUT_SLOT_COUNT;
    public static final int TOTAL_SLOTS = INPUT_SLOT_COUNT + OUTPUT_SLOT_COUNT;
    /** 加速槽在自动化视图里的下标（总 19 槽：18 物品槽 + 1 加速槽，加速槽对外不可读写） */
    public static final int AUTOMATION_SLOT_COUNT = TOTAL_SLOTS + 1;

    /** 神之加速槽：放入神之加速提升并行数量（最多一组 64 个 = 1024 倍） */
    private final ItemStackHandler accelSlot = new ItemStackHandler(1)
    {
        @Override
        protected void onContentsChanged(int slot)
        {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack)
        {
            return stack.getItem() instanceof GodAcceleratorItem;
        }
    };

    private final ItemStackHandler itemHandler = new ItemStackHandler(TOTAL_SLOTS)
    {
        @Override
        protected void onContentsChanged(int slot)
        {
            setChanged();
        }

        // 规则：输入槽只收可去皮原木；输出槽对外部插入恒拒绝（内部转化走 setStackInSlot 直写）
        @Override
        public boolean isItemValid(int slot, ItemStack stack)
        {
            return slot < INPUT_SLOT_COUNT && LogStripper.isStrippable(stack);
        }

        // 神之加速：输入/输出槽容量随并行倍率提升，但单堆上限 99
        // （ItemStack 的 count 序列化硬上限是 99，ExtraCodecs.intRange(1,99)，超 99 存档/掉落时崩溃）
        @Override
        public int getSlotLimit(int slot)
        {
            return Math.min(99, 64 * getParallelMultiplier());
        }

        @Override
        protected int getStackLimit(int slot, ItemStack stack)
        {
            return getSlotLimit(slot);
        }
    };

    private int tickCounter = 0;

    /** 漏斗 / 管道用的组合视图：槽 0-8 = 输入（只进），槽 9-17 = 输出（只出），槽 18 = 加速槽（对外封闭）。 */
    private final IItemHandler automationHandler = new IItemHandler()
    {
        @Override
        public int getSlots()
        {
            return AUTOMATION_SLOT_COUNT;
        }

        @Override
        public ItemStack getStackInSlot(int slot)
        {
            return slot < TOTAL_SLOTS ? itemHandler.getStackInSlot(slot) : accelSlot.getStackInSlot(0);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
        {
            if (slot >= INPUT_SLOT_COUNT || stack.isEmpty())
            {
                return stack;
            }
            return itemHandler.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate)
        {
            if (slot < OUTPUT_SLOT_START || slot >= TOTAL_SLOTS || amount <= 0)
            {
                return ItemStack.EMPTY;
            }
            return itemHandler.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot)
        {
            return slot < TOTAL_SLOTS ? itemHandler.getSlotLimit(slot) : 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack)
        {
            return slot < INPUT_SLOT_COUNT && itemHandler.isItemValid(slot, stack);
        }
    };

    public GodPeelerBlockEntity(BlockPos pos, BlockState state)
    {
        super(Godofthings.GOD_PEELER_BE.get(), pos, state);
    }

    public ItemStackHandler getItemHandler()
    {
        return itemHandler;
    }

    /** 神之加速槽（只接受神之加速，最多 64 个） */
    public ItemStackHandler getAccelSlot()
    {
        return accelSlot;
    }

    public IItemHandler getAutomationHandler()
    {
        return automationHandler;
    }

    /** 并行倍率：每个神之加速 16 倍，最多一组（64 个）= 1024 倍。无加速时为 1。（倍率权威值见 {@link GodAcceleratorItem#PARALLEL_PER_ITEM}） */
    public int getParallelMultiplier()
    {
        return GodAcceleratorItem.multiplierFor(accelSlot.getStackInSlot(0).getCount());
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
        boolean hasWork = false;
        for (int i = 0; i < INPUT_SLOT_COUNT; i++)
        {
            if (!itemHandler.getStackInSlot(i).isEmpty())
            {
                hasWork = true;
                break;
            }
        }
        if (!hasWork)
        {
            tickCounter = 0;
            return;
        }
        // 每 workInterval tick 处理一轮：每个输入槽并行去皮（实时读配置，改 toml 重进世界即生效）
        tickCounter++;
        if (tickCounter >= MachinesConfig.PEELER_WORK_INTERVAL.get())
        {
            tickCounter = 0;
            int mult = getParallelMultiplier();
            for (int i = 0; i < INPUT_SLOT_COUNT; i++)
            {
                convertOne(itemHandler, i, OUTPUT_SLOT_START + i, mult);
            }
        }
    }

    /**
     * 纯逻辑：把输入槽最多 mult 个原木去皮放进对应输出槽（输出为空或同物品且未满才动；供 GameTest 直接调用）。
     *
     * @return 实际去皮的数量（0 = 没动）
     */
    public static int convertOne(ItemStackHandler handler, int inputSlot, int outputSlot, int mult)
    {
        if (mult <= 0)
        {
            return 0;
        }
        ItemStack in = handler.getStackInSlot(inputSlot);
        ItemStack result = LogStripper.strip(in);
        if (result.isEmpty())
        {
            return 0;
        }
        ItemStack out = handler.getStackInSlot(outputSlot);
        if (!out.isEmpty() && !ItemStack.isSameItem(out, result))
        {
            return 0;
        }
        int space = out.isEmpty() ? handler.getSlotLimit(outputSlot) : handler.getSlotLimit(outputSlot) - out.getCount();
        int n = Math.min(Math.min(mult, in.getCount()), space);
        if (n <= 0)
        {
            return 0;
        }
        handler.extractItem(inputSlot, n, false);
        if (out.isEmpty())
        {
            handler.setStackInSlot(outputSlot, result.copyWithCount(n));
        }
        else
        {
            out.grow(n);
            handler.setStackInSlot(outputSlot, out);
        }
        return n;
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
        tag.put("Inventory", itemHandler.serializeNBT(provider));
        tag.put("AccelSlot", accelSlot.serializeNBT(provider));
        tag.putInt("TickCounter", tickCounter);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider)
    {
        super.loadAdditional(tag, provider);
        if (tag.contains("Inventory"))
        {
            itemHandler.deserializeNBT(provider, tag.getCompound("Inventory"));
        }
        else if (tag.contains("InputSlot") || tag.contains("OutputSlot"))
        {
            // v5.15.0（1 输入 + 1 输出）旧存档迁移：单槽内容搬进新布局的槽 0 / 槽 9
            ItemStackHandler legacyIn = new ItemStackHandler(1);
            ItemStackHandler legacyOut = new ItemStackHandler(1);
            if (tag.contains("InputSlot"))
            {
                legacyIn.deserializeNBT(provider, tag.getCompound("InputSlot"));
            }
            if (tag.contains("OutputSlot"))
            {
                legacyOut.deserializeNBT(provider, tag.getCompound("OutputSlot"));
            }
            itemHandler.setStackInSlot(0, legacyIn.getStackInSlot(0));
            itemHandler.setStackInSlot(OUTPUT_SLOT_START, legacyOut.getStackInSlot(0));
        }
        if (tag.contains("AccelSlot"))
        {
            accelSlot.deserializeNBT(provider, tag.getCompound("AccelSlot"));
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
