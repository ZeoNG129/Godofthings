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
import com.godofthings.menu.GodResourceMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

import java.util.List;

/**
 * 神之资源系列方块实体（v5.13.0：原单一「神之资源」拆分为矿物 / 作物 / 方块三台）。
 *
 * <p>三台共用本类，仅 {@link GodResourceVariant} 不同（构造时传入）：
 * 各自的输入过滤与产出计算见 {@link GodResourceVariant#accepts} / {@link GodResourceVariant#produce}。</p>
 *
 * <p>其余行为与原神之资源一致：无需能源，9 个输入槽（3×3）并行生产且<b>不消耗模板</b>；
 * 快配方每 tick、慢配方每 20 tick；神之加速并行倍率；向下自动输出；内置无限储存；
 * AE2 并网（产物自动输出进网络）；打掉不掉落。</p>
 */
public class GodResourceBlockEntity extends BlockEntity implements MenuProvider, IGridConnectedBlockEntity, MachineOwner
{
    /** 主人（放置者）：神之共鸣判定用（主人在线 + 穿齐全套 + 共鸣开关开才生效）。 */
    @org.jetbrains.annotations.Nullable
    private java.util.UUID owner;

    @Override
    public void setOwner(@org.jetbrains.annotations.Nullable java.util.UUID owner)
    {
        this.owner = owner;
        setChanged();
    }

    @org.jetbrains.annotations.Nullable
    @Override
    public java.util.UUID getOwner()
    {
        return owner;
    }

    /** 可放置输入槽数量（3×3 共 9 个） */
    public static final int INPUT_SLOTS = 9;

    /**
     * 本台机器的变体（矿物 / 作物 / 方块）——决定输入过滤与产出表。
     * <p><b>非 final</b>：新放置时由 {@code newBlockEntity} 按方块实例传入；
     * 旧存档读回时 BlockEntityType 工厂只能给 (pos, state)，故在工厂里按 state 判定（见 Godofthings 注册处）。</p>
     */
    private GodResourceVariant variant;

    /** 可放置输入槽：3×3 共 9 个（最多同时放置 9 种模板并行生产） */
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
            return variant.accepts(stack);
        }

        @Override
        public void setSize(int size)
        {
            // 槽数固定为 INPUT_SLOTS：旧存档反序列化会调 setSize 缩槽导致越界崩溃，忽略之
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

    private int tickCounter = 0;

    /** 是否接入 AE（并网后产物自动输出进 AE 网络，占一个频道）。 */
    private boolean aeEnabled = true;

    /** AE 网格节点（线缆直连并网）。 */
    private final AeGridNode aeNode = new AeGridNode(this);
    private int aeTick = 0;

    public GodResourceBlockEntity(GodResourceVariant variant, BlockPos pos, BlockState state)
    {
        super(Godofthings.GOD_RESOURCE_BE.get(), pos, state);
        this.variant = variant;
        itemHandler.setOnChange(this::setChanged);
    }

    /** 供 BlockEntityType 工厂用：先占位 DUPLICATE，真实变体由工厂按 state 判定后立即 setVariant（见 Godofthings 注册处） */
    public GodResourceBlockEntity(BlockPos pos, BlockState state)
    {
        this(GodResourceVariant.DUPLICATE, pos, state);
    }

    /** 工厂按 state 判定变体后回填。 */
    public void setVariant(GodResourceVariant variant)
    {
        this.variant = variant;
    }

    /** 本台机器的变体（矿物 / 作物 / 复制）。 */
    public GodResourceVariant getVariant()
    {
        return variant;
    }

    public ItemStackHandler getInputSlot()
    {
        return inputSlot;
    }

    public InfiniteItemHandler getItemHandler()
    {
        return itemHandler;
    }

    /** 神之加速槽（只接受神之加速，最多 64 个） */
    public ItemStackHandler getAccelSlot()
    {
        return accelSlot;
    }

    /** 并行倍率：每个神之加速 16 倍，最多一组（64 个）= 1024 倍。（倍率权威值见 {@link GodAcceleratorItem#PARALLEL_PER_ITEM}） */
    public int getParallelMultiplier()
    {
        return GodAcceleratorItem.multiplierFor(accelSlot.getStackInSlot(0).getCount());
    }

    public int getStorageCount()
    {
        return itemHandler.getStacks().size();
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

    public static void tick(Level level, BlockPos pos, BlockState state, GodResourceBlockEntity be)
    {
        if (level.isClientSide)
        {
            return;
        }
        be.tickServer();
    }

    private void tickServer()
    {
        // 快配方：每 tick 处理所有非空输入槽（变体各自的快配方资格见 GodResourceVariant.isFastRecipe）
        for (int i = 0; i < INPUT_SLOTS; i++)
        {
            ItemStack input = inputSlot.getStackInSlot(i);
            if (!input.isEmpty() && variant.isFastRecipe(input))
            {
                process(i); // 复制配方：每 tick 产 64 个
            }
        }
        // 慢配方：每 workInterval tick 处理一轮所有非空输入槽（实时读配置，改 toml 重进世界即生效）
        tickCounter++;
        if (tickCounter >= MachinesConfig.RESOURCE_WORK_INTERVAL.get())
        {
            tickCounter = 0;
            for (int i = 0; i < INPUT_SLOTS; i++)
            {
                ItemStack input = inputSlot.getStackInSlot(i);
                if (!input.isEmpty() && !variant.isFastRecipe(input))
                {
                    process(i);
                }
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
        List<ItemStack> outputs = variant.produce(input.getItem(), (net.minecraft.server.level.ServerLevel) level, worldPosition);
        if (outputs.isEmpty())
        {
            return; // 无效输入
        }
        int mult = getParallelMultiplier();
        // 神之共鸣·掉落（v5.15.4）：战利品爆炸倍率（主人在线 + 穿齐全套 + 共鸣开关开）
        double bomb = level instanceof net.minecraft.server.level.ServerLevel serverLevel
                ? com.godofthings.handler.ArmorSkillHandler.lootBombBoost(serverLevel, owner) : 1.0;
        // 不消耗原材料：输入只是生产模板，按时间持续产出（神之加速提升并行数量）
        for (ItemStack out : outputs)
        {
            if (!out.isEmpty())
            {
                ItemStack toInsert = out.copyWithCount((int) Math.min(Integer.MAX_VALUE / 2L,
                        Math.round(out.getCount() * mult * bomb)));
                ItemStack leftover = itemHandler.insertItem(-1, toInsert, false);
                if (!leftover.isEmpty())
                {
                    InfiniteItemHandler.dropRemainder(level, worldPosition, leftover);
                }
            }
        }
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

    // ---- capability：任意面都能取走产物 ----

    @EventBusSubscriber(modid = Godofthings.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static class CapabilityRegistration
    {
        @SubscribeEvent
        public static void registerCapabilities(RegisterCapabilitiesEvent event)
        {
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, Godofthings.GOD_RESOURCE_BE.get(),
                    (be, side) -> be.itemHandler);
            event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST,
                    Godofthings.GOD_RESOURCE_BE.get(),
                    (be, side) -> be);
        }
    }

    // ---- NBT ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider)
    {
        super.saveAdditional(tag, provider);
        if (owner != null)
        {
            tag.putUUID("Owner", owner);
        }
        tag.put("InputSlot", inputSlot.serializeNBT(provider));
        tag.put("AccelSlot", accelSlot.serializeNBT(provider));
        tag.put("Inventory", itemHandler.serializeNBT(provider));
        tag.putInt("TickCounter", tickCounter);
        tag.putBoolean("AeEnabled", aeEnabled);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider)
    {
        super.loadAdditional(tag, provider);
        if (tag.hasUUID("Owner"))
        {
            owner = tag.getUUID("Owner");
        }
        if (tag.contains("InputSlot"))
        {
            inputSlot.deserializeNBT(provider, tag.getCompound("InputSlot"));
        }
        if (tag.contains("AccelSlot"))
        {
            accelSlot.deserializeNBT(provider, tag.getCompound("AccelSlot"));
        }
        if (tag.contains("Inventory"))
        {
            itemHandler.deserializeNBT(provider, tag.getCompound("Inventory"));
        }
        tickCounter = tag.getInt("TickCounter");
        this.aeEnabled = tag.contains("AeEnabled") ? tag.getBoolean("AeEnabled") : true;
    }

    // ---- MenuProvider ----

    @Override
    public Component getDisplayName()
    {
        // 三台共用 BE 类型：按变体取对应方块的注册名显示（check-lang 校验该字面量）
        return switch (variant)
        {
            case ORE -> Component.translatable("block.godofthings.god_ore_machine");
            case CROP -> Component.translatable("block.godofthings.god_crop_machine");
            case DUPLICATE -> Component.translatable("block.godofthings.god_duplicate_machine");
        };
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player)
    {
        return new com.godofthings.menu.GodResourceMenu(containerId, inventory, this);
    }
}
