package com.godofthings.block.entity;

import appeng.api.AECapabilities;
import appeng.api.config.Actionable;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageService;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.MEStorage;
import appeng.me.helpers.IGridConnectedBlockEntity;
import com.godofthings.Godofthings;
import com.godofthings.ae2.AeGridNode;
import com.godofthings.config.MachinesConfig;
import com.godofthings.item.GodAcceleratorItem;
import com.godofthings.menu.GodSpawnEggMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
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

import java.util.List;

/**
 * 神之怪蛋方块实体：刷怪蛋复制机。
 * - 无需能源，9 个输入槽（3×3）并行复制；每 20 tick 处理一轮所有非空刷怪蛋
 * - 放入刷怪蛋 → 产出**同种**刷怪蛋（保留原物品的全部组件，所以模组刷怪蛋的实体数据一并复制）
 * - **兼容所有刷怪蛋**：原版 / NeoForge 系直接认；其它模组自建的刷怪蛋只要按原版约定写了
 *   ENTITY_DATA 组件也认（见 {@link com.godofthings.block.entity.machine.SpawnEggHelper}）
 * - 每种产物每周期 64 个（神之加速按并行倍率乘）
 * - 不消耗刷怪蛋（生产模板，按时间持续产出）
 * - 向下自动输出，内置无限储存；打掉不掉落
 */
public class GodSpawnEggBlockEntity extends BlockEntity implements MenuProvider, IGridConnectedBlockEntity
{
    /** 可放置输入槽数量（3×3 共 9 个） */
    public static final int INPUT_SLOTS = 9;

    /** 可放置输入槽：3×3 共 9 个（最多同时放置 9 种刷怪蛋并行生产） */
    private final ItemStackHandler inputSlot = new ItemStackHandler(INPUT_SLOTS)
    {
        @Override
        protected void onContentsChanged(int slot)
        {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack)
        {
            return com.godofthings.block.entity.machine.SpawnEggHelper.isSpawnEgg(stack);
        }

        @Override
        public void setSize(int size)
        {
            // 槽数固定为 INPUT_SLOTS：旧世界（2.0.5）保存的 NBT 里 Size=1，
            // deserializeNBT 会调 setSize(1) 缩槽，导致 tickServer 遍历 9 槽时越界崩溃
            // （Slot 1 not in valid range - [0,1)）。忽略非 INPUT_SLOTS 的 setSize，保持 9 槽。
            if (size != INPUT_SLOTS)
            {
                return;
            }
            super.setSize(size);
        }
    };
    private final InfiniteItemHandler itemHandler = new InfiniteItemHandler();

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

    /** 是否接入 AE（并网后产物自动输出进 AE 网络，占一个频道）。 */
    private boolean aeEnabled = true;

    /** AE 网格节点（线缆直连并网）。 */
    private final AeGridNode aeNode = new AeGridNode(this);
    private int aeTick = 0;

    private int tickCounter = 0;

    public GodSpawnEggBlockEntity(BlockPos pos, BlockState state)
    {
        super(Godofthings.GOD_SPAWN_EGG_BE.get(), pos, state);
        itemHandler.setOnChange(this::setChanged);
    }

    public ItemStackHandler getInputSlot()
    {
        return inputSlot;
    }

    public InfiniteItemHandler getItemHandler()
    {
        return itemHandler;
    }

    public boolean isAeEnabled()
    {
        return aeEnabled;
    }

    public void toggleAeEnabled()
    {
        this.aeEnabled = !this.aeEnabled;
        setChanged();
    }

    // ---- AE 网格节点（线缆直连并网，产物自动输出进 AE） ----

    @Override
    public IManagedGridNode getMainNode() { return aeNode.getMainNode(); }

    @Override
    public void saveChanges() { setChanged(); }

    /** 把产物推入 AE 网络（节流由 tick 控制）。 */
    private void pushOutputToAe()
    {
        if (!aeEnabled || !aeNode.isActive())
        {
            return;
        }
        IStorageService storage = aeNode.getStorage();
        if (storage == null)
        {
            return;
        }
        MEStorage inv = storage.getInventory();
        IActionSource source = aeNode.actionSource();
        for (int slot = 0; slot < getItemHandler().getSlots(); slot++)
        {
            ItemStack stack = getItemHandler().getStackInSlot(slot);
            if (stack.isEmpty())
            {
                continue;
            }
            long inserted = inv.insert(AEItemKey.of(stack), stack.getCount(), Actionable.MODULATE, source);
            if (inserted > 0)
            {
                getItemHandler().extractItem(slot, (int) inserted, false);
            }
        }
    }

    /** 神之加速槽（只接受神之加速，最多 64 个） */
    public ItemStackHandler getAccelSlot()
    {
        return accelSlot;
    }

    /** 并行倍率：每个神之加速 16 倍，最多一组（64 个）= 1024 倍。无加速时为 1。（倍率权威值见 {@link GodAcceleratorItem#PARALLEL_PER_ITEM}） */
    public int getParallelMultiplier()
    {
        return GodAcceleratorItem.multiplierFor(accelSlot.getStackInSlot(0).getCount());
    }

    public int getStorageCount()
    {
        return itemHandler.getStacks().size();
    }

    // ---- 生命周期：创建/销毁 AE 网格节点 ----

    @Override
    public void onLoad()
    {
        super.onLoad();
        aeNode.create(level, worldPosition);
    }

    @Override
    public void setRemoved()
    {
        aeNode.destroy();
        super.setRemoved();
    }

    // ---- 每 tick 逻辑 ----

    public static void tick(Level level, BlockPos pos, BlockState state, GodSpawnEggBlockEntity be)
    {
        if (level.isClientSide)
        {
            return;
        }
        be.tickServer();
    }

    private void tickServer()
    {
        tickCounter++;
        if (tickCounter >= MachinesConfig.SPAWN_EGG_WORK_INTERVAL.get())
        {
            tickCounter = 0;
            for (int i = 0; i < INPUT_SLOTS; i++)
            {
                process(i);
            }
        }
        pushDown();
        // AE 产物输出节流：每 20 tick（1 秒）推一次
        aeTick++;
        if (aeTick >= 20)
        {
            aeTick = 0;
            pushOutputToAe();
        }
    }

    private void process(int slot)
    {
        ItemStack input = inputSlot.getStackInSlot(slot);
        if (input.isEmpty())
        {
            return;
        }
        List<ItemStack> outputs = produce(input);
        if (outputs.isEmpty())
        {
            return;
        }
        int mult = getParallelMultiplier();
        // 不消耗刷怪蛋：生产模板，按时间持续产出（神之加速提升并行数量）
        for (ItemStack out : outputs)
        {
            if (!out.isEmpty())
            {
                ItemStack toInsert = out.copyWithCount(out.getCount() * mult);
                ItemStack leftover = itemHandler.insertItem(-1, toInsert, false);
                if (!leftover.isEmpty())
                {
                    InfiniteItemHandler.dropRemainder(level, worldPosition, leftover);
                }
            }
        }
    }

    /**
     * 复制刷怪蛋：产出与输入**同种**的刷怪蛋（每周期 64 个，神之加速按倍率乘）。
     *
     * <p>用 {@code copyWithCount} 复制整份物品栈，所以输入蛋上挂的组件（实体数据等）一并带过去 ——
     * 模组刷怪蛋的实体类型、甚至被改过名字/加了数据的蛋都能原样复制。
     * 输入不是刷怪蛋时（正常插不进来，兜底）返回空表。</p>
     */
    private List<ItemStack> produce(ItemStack input)
    {
        return com.godofthings.block.entity.machine.SpawnEggHelper.duplicate(input);
    }

    /** 向下自动输出到下方容器 */
    private void pushDown()
    {
        BlockPos below = worldPosition.below();
        if (!level.isLoaded(below))
        {
            return;
        }
        BlockEntity neighbor = level.getBlockEntity(below);
        if (neighbor == null)
        {
            return;
        }
        // 1.21.1：BE 不再覆盖 getCapability；Level.getCapability(BlockCapability, BlockPos, side) 直接返回能力对象（null = 无能力）
        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, below, Direction.UP);
        if (handler == null)
        {
            return;
        }
        while (true)
        {
            ItemStack stack = itemHandler.getStackInSlot(0);
            if (stack.isEmpty())
            {
                break;
            }
            ItemStack toPush = stack.copy();
            boolean anyMoved = false;
            for (int s = 0; s < handler.getSlots(); s++)
            {
                ItemStack leftover = handler.insertItem(s, toPush, false);
                int moved = toPush.getCount() - leftover.getCount();
                if (moved > 0)
                {
                    itemHandler.extractItem(0, moved, false);
                    anyMoved = true;
                }
                toPush = leftover;
                if (toPush.isEmpty())
                {
                    break;
                }
            }
            if (!anyMoved)
            {
                break;
            }
        }
    }

    // ---- capability 注册：任意面都能取走产物 ----
    // 1.21.1：BE 不再覆盖 getCapability/LazyOptional，能力经 RegisterCapabilitiesEvent（MOD 总线）集中注册

    @EventBusSubscriber(modid = Godofthings.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static class CapabilityRegistrar
    {
        @SubscribeEvent
        public static void onRegisterCapabilities(RegisterCapabilitiesEvent event)
        {
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, Godofthings.GOD_SPAWN_EGG_BE.get(),
                    (be, side) -> be.getItemHandler());
            event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, Godofthings.GOD_SPAWN_EGG_BE.get(),
                    (be, side) -> be);
        }
    }

    // ---- NBT（1.20.5+：save/load 需 HolderLookup.Provider） ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.saveAdditional(tag, registries);
        tag.put("InputSlot", inputSlot.serializeNBT(registries));
        tag.put("AccelSlot", accelSlot.serializeNBT(registries));
        tag.put("Inventory", itemHandler.serializeNBT(registries));
        tag.putInt("TickCounter", tickCounter);
        tag.putBoolean("AeEnabled", aeEnabled);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.loadAdditional(tag, registries);
        this.aeEnabled = tag.contains("AeEnabled") ? tag.getBoolean("AeEnabled") : true;
        if (tag.contains("InputSlot"))
        {
            inputSlot.deserializeNBT(registries, tag.getCompound("InputSlot"));
        }
        if (tag.contains("AccelSlot"))
        {
            accelSlot.deserializeNBT(registries, tag.getCompound("AccelSlot"));
        }
        if (tag.contains("Inventory"))
        {
            itemHandler.deserializeNBT(registries, tag.getCompound("Inventory"));
        }
        tickCounter = tag.getInt("TickCounter");
    }

    // ---- MenuProvider ----

    @Override
    public Component getDisplayName()
    {
        return Component.translatable("block.godofthings.god_spawn_egg");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player)
    {
        return new GodSpawnEggMenu(containerId, inventory, this);
    }
}
