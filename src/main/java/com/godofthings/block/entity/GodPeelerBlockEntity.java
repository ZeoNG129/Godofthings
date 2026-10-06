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
import com.godofthings.block.entity.machine.LogStripper;
import com.godofthings.config.MachinesConfig;
import com.godofthings.item.GodAcceleratorItem;
import com.godofthings.menu.GodPeelerMenu;
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

/**
 * 神之去皮方块实体（v5.15.3：加入与神之熔炉相同的六面输入/输出配置）。
 *
 * <p>9 个输入槽（0-8）+ 9 个输出槽（9-17，一一对应：输入 i → 输出 {@link #OUTPUT_SLOT_START} + i）+
 * 1 个神之加速槽（提升并行数量与槽位容量）。无需能源。</p>
 *
 * <p>每 {@link MachinesConfig#PEELER_WORK_INTERVAL} tick 处理一轮：<b>每个</b>非空输入槽把最多
 * 「并行倍率」个原木去皮塞进对应输出槽（同物品可叠、容量够才动）。</p>
 *
 * <p><b>面配置（与神之熔炉同一套）</b>：六个面各自可配置
 * {@link FaceMode#NONE}（不启用）/ {@link FaceMode#INPUT}（自动从相邻容器抽入原木）/
 * {@link FaceMode#OUTPUT}（自动把成品推给相邻容器）/ {@link FaceMode#BOTH}（同一面既抽入又推出），
 * 默认全部 NONE——漏斗 / 管道要在界面右上角齿轮的面配置里开启对应面才会互动。
 * 面配置界面右上角齿轮按钮打开（菜单按钮 6），见 {@link GodPeelerMenu}。</p>
 *
 * <p>打掉时物品一并消失，不掉落（同神之资源系列做法）。</p>
 */
public class GodPeelerBlockEntity extends BlockEntity implements MenuProvider, IGridConnectedBlockEntity
{
    public static final int INPUT_SLOT_COUNT = 9;
    public static final int OUTPUT_SLOT_COUNT = 9;
    public static final int OUTPUT_SLOT_START = INPUT_SLOT_COUNT;
    public static final int TOTAL_SLOTS = INPUT_SLOT_COUNT + OUTPUT_SLOT_COUNT;

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

    // 每个面一个模式，索引 = Direction.get3DDataValue()，取值见 FaceMode.getId()
    private final int[] faceModes = new int[6];

    private final IItemHandler[] sideHandlers = new IItemHandler[6];

    /** 是否接入 AE（v5.15.9 加入：并网后去皮产物自动输出进 AE 网络，占一个频道）。 */
    private boolean aeEnabled = true;

    /** AE 网格节点（线缆直连并网，照抄神之熔炉）。 */
    private final AeGridNode aeNode = new AeGridNode(this);
    private int aeTick = 0;

    private int tickCounter = 0;

    public GodPeelerBlockEntity(BlockPos pos, BlockState state)
    {
        super(Godofthings.GOD_PEELER_BE.get(), pos, state);
        for (int i = 0; i < 6; i++)
        {
            final int idx = i;
            sideHandlers[idx] = new SideHandler(idx);
        }
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

    /** 并行倍率：每个神之加速 16 倍，最多一组（64 个）= 1024 倍。无加速时为 1。（倍率权威值见 {@link GodAcceleratorItem#PARALLEL_PER_ITEM}） */
    public int getParallelMultiplier()
    {
        return GodAcceleratorItem.multiplierFor(accelSlot.getStackInSlot(0).getCount());
    }

    // ---- 面模式（与神之熔炉同一套）----

    public int getFaceMode(Direction dir)
    {
        return faceModes[dir.get3DDataValue()];
    }

    public void setFaceMode(Direction dir, int mode)
    {
        faceModes[dir.get3DDataValue()] = ((mode % 4) + 4) % 4;
        setChanged();
    }

    public void cycleFaceMode(Direction dir)
    {
        setFaceMode(dir, getFaceMode(dir) + 1);
    }

    // ---- capability：每个面按模式暴露受限的 IItemHandler ----

    // NeoForge 1.21.1：BlockEntity 不可覆写 getCapability（LazyOptional 机制已移除），
    // 能力统一在 RegisterCapabilitiesEvent（MOD 总线）注册，见下方 CapabilityRegistration。
    // 注意：faceModes 在运行时可改，SideHandler 每次调用动态读取当前模式，故无需失效缓存。

    /** NeoForge 能力查询入口：按面模式返回受限 handler；side == null 或 NONE 面返回 null（与神之熔炉逻辑一致）。 */
    @Nullable
    IItemHandler getSideCapability(@Nullable Direction side)
    {
        if (side == null)
        {
            return null;
        }
        int idx = side.get3DDataValue();
        return faceModes[idx] != FaceMode.NONE.getId() ? sideHandlers[idx] : null;
    }

    @EventBusSubscriber(modid = Godofthings.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static class CapabilityRegistration
    {
        @SubscribeEvent
        public static void registerCapabilities(RegisterCapabilitiesEvent event)
        {
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, Godofthings.GOD_PEELER_BE.get(),
                    (be, side) -> be.getSideCapability(side));
            // v5.15.9：加入 AE 并网（照抄神之熔炉）——线缆直连、占一个频道、产物自动输出进网络。
            event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, Godofthings.GOD_PEELER_BE.get(),
                    (be, side) -> be);
        }
    }

    /**
     * 某个面的包装 handler：INPUT 面只能插入输入槽，OUTPUT 面只能从输出槽提取（照抄神之熔炉）。
     */
    private class SideHandler implements IItemHandler
    {
        private final int dirIndex;

        SideHandler(int dirIndex)
        {
            this.dirIndex = dirIndex;
        }

        private FaceMode mode()
        {
            return FaceMode.fromId(faceModes[dirIndex]);
        }

        @Override
        public int getSlots()
        {
            return TOTAL_SLOTS;
        }

        @Override
        public ItemStack getStackInSlot(int slot)
        {
            return itemHandler.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
        {
            if (slot < 0 || slot >= INPUT_SLOT_COUNT)
            {
                return stack; // 只能插入输入槽
            }
            return (mode() == FaceMode.INPUT || mode() == FaceMode.BOTH)
                    ? itemHandler.insertItem(slot, stack, simulate)
                    : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate)
        {
            if (slot < OUTPUT_SLOT_START || slot >= TOTAL_SLOTS)
            {
                return ItemStack.EMPTY; // 只能从输出槽提取
            }
            return (mode() == FaceMode.OUTPUT || mode() == FaceMode.BOTH)
                    ? itemHandler.extractItem(slot, amount, simulate)
                    : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot)
        {
            return itemHandler.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack)
        {
            return slot < INPUT_SLOT_COUNT
                    && (mode() == FaceMode.INPUT || mode() == FaceMode.BOTH)
                    && LogStripper.isStrippable(stack);
        }
    }

    // ---- 每 tick 逻辑：先自动抽推，再去皮 ----

    // ---- AE 网格节点（v5.15.9 加入：线缆直连并网，产物自动输出进 AE，占一个频道） ----

    @Override
    public IManagedGridNode getMainNode()
    {
        return aeNode.getMainNode();
    }

    @Override
    public void saveChanges()
    {
        setChanged();
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

    /** 是否接入 AE（并网后去皮产物自动输出进 AE 网络）。 */
    public boolean isAeEnabled()
    {
        return aeEnabled;
    }

    public void toggleAeEnabled()
    {
        this.aeEnabled = !this.aeEnabled;
        setChanged();
    }

    /** 把输出槽产物推入 AE 网络（只推输出槽 9-17；输入槽是待去皮队列不推）。 */
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
        for (int slot = OUTPUT_SLOT_START; slot < TOTAL_SLOTS; slot++)
        {
            ItemStack stack = itemHandler.getStackInSlot(slot);
            if (stack.isEmpty())
            {
                continue;
            }
            long inserted = inv.insert(AEItemKey.of(stack), stack.getCount(), Actionable.MODULATE, source);
            if (inserted > 0)
            {
                itemHandler.extractItem(slot, (int) inserted, false);
            }
        }
    }

    /** AE 产物输出节流：每 20 tick（1 秒）推一次。 */
    private void pushOutputToAeThrottled()
    {
        aeTick++;
        if (aeTick >= 20)
        {
            aeTick = 0;
            pushOutputToAe();
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, GodPeelerBlockEntity be)
    {
        if (!level.isClientSide)
        {
            be.autoTransfer();
            be.tickServer();
            // AE 产物输出节流：每 20 tick（1 秒）推一次（v5.15.9 加入并网）
            be.pushOutputToAeThrottled();
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

    /** 每 tick 按各面模式自动抽入原木 / 推出成品（照抄神之熔炉 autoTransfer，过滤条件换成可去皮原木）。 */
    private void autoTransfer()
    {
        for (Direction dir : Direction.values())
        {
            int mode = getFaceMode(dir);
            if (mode == FaceMode.NONE.getId())
            {
                continue;
            }

            BlockPos neighborPos = worldPosition.relative(dir);
            if (!level.isLoaded(neighborPos))
            {
                continue;
            }

            // NeoForge 1.21.1：邻居能力查询改为 Level.getCapability(BlockCapability, BlockPos, side)，null = 无能力
            IItemHandler neighborCap = level.getCapability(Capabilities.ItemHandler.BLOCK, neighborPos, dir.getOpposite());
            if (neighborCap == null)
            {
                continue;
            }

            if (mode == FaceMode.INPUT.getId())
            {
                pullFrom(neighborCap);
            }
            else if (mode == FaceMode.OUTPUT.getId())
            {
                pushTo(neighborCap);
            }
            else if (mode == FaceMode.BOTH.getId())
            {
                // 同一个面既自动抽入原木，又自动推出成品
                pullFrom(neighborCap);
                pushTo(neighborCap);
            }
        }
    }

    /** 从邻居抽取可去皮原木到任意有空位的输入槽（同物品优先，否则找空槽；照抄神之熔炉 pullFrom）。 */
    private void pullFrom(IItemHandler neighbor)
    {
        for (int s = 0; s < neighbor.getSlots(); s++)
        {
            ItemStack src = neighbor.getStackInSlot(s);
            if (src.isEmpty() || !LogStripper.isStrippable(src))
            {
                continue;
            }

            int targetSlot = findInputSlotFor(src);
            if (targetSlot < 0)
            {
                continue;
            }

            ItemStack leftoverSim = itemHandler.insertItem(targetSlot, src, true);
            int canMove = src.getCount() - leftoverSim.getCount();
            if (canMove <= 0)
            {
                continue;
            }

            ItemStack extracted = neighbor.extractItem(s, canMove, true);
            if (extracted.isEmpty())
            {
                continue;
            }
            int toMove = Math.min(extracted.getCount(), canMove);
            if (toMove <= 0)
            {
                continue;
            }

            ItemStack remaining = itemHandler.insertItem(targetSlot, extracted, false);
            int placed = toMove - remaining.getCount();
            if (placed > 0)
            {
                neighbor.extractItem(s, placed, false);
                setChanged();
            }
        }
    }

    /** 找到可接受该原木的输入槽（同物品优先，否则找空槽）；没有则 -1（照抄神之熔炉）。 */
    private int findInputSlotFor(ItemStack stack)
    {
        int emptySlot = -1;
        for (int i = 0; i < INPUT_SLOT_COUNT; i++)
        {
            ItemStack cur = itemHandler.getStackInSlot(i);
            if (cur.isEmpty())
            {
                if (emptySlot < 0)
                {
                    emptySlot = i;
                }
            }
            // 1.21.1：isSameItemSameTags → isSameItemSameComponents
            else if (ItemStack.isSameItemSameComponents(cur, stack))
            {
                return i;
            }
        }
        return emptySlot;
    }

    /** 把输出槽的去皮原木推送给邻居，直到清空或插不下（照抄神之熔炉 pushTo）。 */
    private void pushTo(IItemHandler neighbor)
    {
        for (int out = OUTPUT_SLOT_START; out < TOTAL_SLOTS; out++)
        {
            ItemStack output = itemHandler.getStackInSlot(out);
            if (output.isEmpty())
            {
                continue;
            }
            ItemStack toPush = output.copy();
            for (int s = 0; s < neighbor.getSlots(); s++)
            {
                ItemStack leftover = neighbor.insertItem(s, toPush, false);
                int moved = toPush.getCount() - leftover.getCount();
                if (moved > 0)
                {
                    itemHandler.extractItem(out, moved, false);
                }
                toPush = leftover;
                if (toPush.isEmpty())
                {
                    break;
                }
            }
        }
        setChanged();
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

    // ---- NBT ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider)
    {
        super.saveAdditional(tag, provider);
        tag.put("Inventory", itemHandler.serializeNBT(provider));
        tag.put("AccelSlot", accelSlot.serializeNBT(provider));
        tag.putIntArray("FaceModes", faceModes);
        tag.putBoolean("AeEnabled", aeEnabled);
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
        if (tag.contains("FaceModes"))
        {
            int[] modes = tag.getIntArray("FaceModes");
            System.arraycopy(modes, 0, faceModes, 0, Math.min(6, modes.length));
        }
        // AE 接入开关（v5.15.9 加入；旧存档缺键默认开）
        this.aeEnabled = !tag.contains("AeEnabled") || tag.getBoolean("AeEnabled");
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
