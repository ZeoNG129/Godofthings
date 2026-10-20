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
import com.godofthings.block.entity.machine.PendingOutputQueue;
import com.godofthings.godgrid.LongSlotHandler;
import com.godofthings.godgrid.LongSlotTransfer;
import com.godofthings.menu.GodAbsorberMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * 神之吸收方块实体：大范围吸收掉落物与经验。
 * <ul>
 *   <li>功能：开关、范围（0-1600）。</li>
 *   <li>存储：掉落物进 {@link LongSlotHandler}（自管 long 计数，单格容量 int 上限，UI 显示前 27 格）。</li>
 *   <li>经验：经验球进经验点存储（经验面板可取出）。</li>
 *   <li>面配置（六面输入输出）+ AE 并网（产物自动输出进 AE）。</li>
 * </ul>
 * <p>v5.18.10：产物装不下内部存储时进内存待处理队列并暂停吸收，不再掉到世界里（见 {@code pendingOutputs} 注释）。
 */
public class GodAbsorberBlockEntity extends BlockEntity implements MenuProvider, IGridConnectedBlockEntity, MachineOwner
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

    /** UI 显示的存储槽位数量（内部存储更大，界面上只暴露前 27 格）。 */
    public static final int STORAGE_SLOTS = 27;
    /**
     * 内部存储槽位数（= 旧 {@link InfiniteItemHandler#MAX_STACKS}，即「不同物品类型数」上限）。
     * 界面只显示前 {@link #STORAGE_SLOTS} 格，被挤出可视区的物品由 {@link #compact()} 压回可视区，
     * 并照常由面输出 / AE 推出去。
     */
    public static final int MAX_STORAGE_TYPES = InfiniteItemHandler.MAX_STACKS;
    /** 吸收范围上限（方块）。v5.17.2 按用户要求从 1600 降到 480。 */
    public static final int MAX_RANGE = 480;
    private static final int SCAN_INTERVAL = 10;

    private boolean enabled = false;
    private int range = 16;
    private int experiencePoints = 0;

    /** 存储内容是否变过（变了才需要压紧，避免空机器每 tick 空扫全表） */
    private boolean storageDirty = false;
    /**
     * v5.18.7：改用自管 long 计数的 {@link LongSlotHandler}（单格容量 = int 上限 2,147,483,647）。
     *
     * <p>原先的 {@link InfiniteItemHandler} 单格上限是「内存态」的：真实数量塞在 {@code ItemStack}
     * count 里（&gt;99 的非法堆），一旦经界面同步 / 别的模组读取就可能崩。现在对外视图恒 ≤99，真实数量走 long。</p>
     */
    private final LongSlotHandler storage = new LongSlotHandler(MAX_STORAGE_TYPES)
    {
        @Override
        protected void onContentsChanged(int slot)
        {
            storageDirty = true;
            setChanged();
        }
    };

    /**
     * 产物溢出的内存待处理队列（v5.18.10）：产物装不进内部存储时暂存于此，下一 tick 重试写入；
     * <b>队列非空期间暂停吸收</b>，队列清空后自动恢复。
     *
     * <p>取舍：队列是纯内存态、<b>不进存档</b> —— 拆机器 / 掉电 / 区块卸载时队列里的余量会消失；
     * 另一条路只有「掉到世界里」，按要求宁可丢这一点余量也不把产物撒进世界。
     * 实际上只有内部存储的「不同类型数」（{@link #MAX_STORAGE_TYPES}）被塞满时才会用到它。</p>
     */
    private final PendingOutputQueue pendingOutputs = new PendingOutputQueue();

    private final int[] faceModes = new int[6];
    private final SideHandler[] sideHandlers = new SideHandler[6];

    private final AeGridNode aeNode = new AeGridNode(this);
    private boolean aeEnabled = true;
    private int aeTick = 0;
    private int scanTimer = 0;

    public GodAbsorberBlockEntity(BlockPos pos, BlockState state)
    {
        super(Godofthings.GOD_ABSORBER_BE.get(), pos, state);
        for (int i = 0; i < 6; i++)
        {
            this.sideHandlers[i] = new SideHandler(i);
        }
    }

    // ---- 功能 ----

    public boolean isEnabled() { return enabled; }
    public void toggleEnabled() { this.enabled = !this.enabled; setChanged(); }
    public int getRange() { return range; }
    public void setRange(int value) { this.range = Math.max(0, Math.min(MAX_RANGE, value)); setChanged(); }

    // ---- 面配置 ----

    public int getFaceMode(Direction dir) { return faceModes[dir.get3DDataValue()]; }
    public void setFaceMode(Direction dir, int mode) { faceModes[dir.get3DDataValue()] = ((mode % 4) + 4) % 4; setChanged(); }
    public void cycleFaceMode(Direction dir) { setFaceMode(dir, getFaceMode(dir) + 1); }

    @Nullable
    IItemHandler getSideCapability(@Nullable Direction side)
    {
        if (side == null) return null;
        return faceModes[side.get3DDataValue()] != FaceMode.NONE.getId() ? sideHandlers[side.get3DDataValue()] : null;
    }

    @EventBusSubscriber(modid = Godofthings.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static class CapabilityRegistration
    {
        @SubscribeEvent
        public static void onRegisterCapabilities(RegisterCapabilitiesEvent event)
        {
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, Godofthings.GOD_ABSORBER_BE.get(),
                    (be, side) -> be.getSideCapability(side));
            event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, Godofthings.GOD_ABSORBER_BE.get(),
                    (be, side) -> be);
        }
    }

    private class SideHandler implements IItemHandler
    {
        private final int dirIndex;
        SideHandler(int dirIndex) { this.dirIndex = dirIndex; }
        private FaceMode mode() { return FaceMode.fromId(faceModes[dirIndex]); }
        @Override public int getSlots() { return STORAGE_SLOTS; }
        @Override public ItemStack getStackInSlot(int slot) { return storage.getStackInSlot(slot); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
        {
            // 插入不看槽位索引：同类叠到已有槽，否则占第一个空槽（与旧 InfiniteItemHandler 一致）
            return (mode() == FaceMode.INPUT || mode() == FaceMode.BOTH) ? insertAny(stack, simulate) : stack;
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate)
        {
            return (mode() == FaceMode.OUTPUT || mode() == FaceMode.BOTH) ? storage.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }
        /** 单格容量 = int 上限（不再有 64 / 99 的假上限） */
        @Override public int getSlotLimit(int slot) { return (int) Math.min(Integer.MAX_VALUE, storage.capacity()); }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return true; }
    }

    /** 存储视图（菜单槽位用）：直接暴露 long 存储本身，界面只注册前 {@link #STORAGE_SLOTS} 格。 */
    public LongSlotHandler getStorageView() { return storage; }

    /** 存储里有几种物品（旧 getStorageCount 的语义）。 */
    public int getStorageCount()
    {
        int count = 0;
        for (int i = 0; i < MAX_STORAGE_TYPES; i++)
        {
            if (!storage.isEmpty(i))
            {
                count++;
            }
        }
        return count;
    }

    public LongSlotHandler getItemHandler() { return storage; }

    // ---- long 存储辅助 ----

    /** 压紧后的「已用槽数」：存储恒定前紧后空，第一个空槽就是下一个可用位。 */
    private int usedSlots()
    {
        int used = 0;
        while (used < MAX_STORAGE_TYPES && !storage.isEmpty(used))
        {
            used++;
        }
        return used;
    }

    /**
     * 把整堆物品塞进存储：先叠到同类槽，再占第一个空槽。
     * 返回塞不下的余量（数量可能 &gt;99）；调用方必须交给 {@code PendingOutputQueue} 待处理队列，禁止掉到世界。
     */
    private ItemStack insertAny(ItemStack stack, boolean simulate)
    {
        if (stack.isEmpty())
        {
            return ItemStack.EMPTY;
        }
        ItemStack rest = stack.copy();
        int used = usedSlots();
        for (int i = 0; i < used && !rest.isEmpty(); i++)
        {
            if (!ItemStack.isSameItemSameComponents(storage.key(i), rest))
            {
                continue;
            }
            long inserted = storage.insertLong(i, rest, rest.getCount(), simulate);
            if (inserted > 0L)
            {
                rest.shrink((int) inserted);
            }
        }
        if (!rest.isEmpty() && used < MAX_STORAGE_TYPES)
        {
            long inserted = storage.insertLong(used, rest, rest.getCount(), simulate);
            if (inserted > 0L)
            {
                rest.shrink((int) inserted);
            }
        }
        return rest;
    }

    /** 把待处理队列里的产物重试写进内部存储；写空后吸收自动恢复。 */
    private void flushPendingStorage()
    {
        int guard = pendingOutputs.size() + 1; // 防死循环：队列里每条最多试一轮
        while (!pendingOutputs.isEmpty() && guard-- > 0)
        {
            ItemStack head = pendingOutputs.poll();
            ItemStack leftover = insertAny(head, false);
            if (leftover.getCount() >= head.getCount())
            {
                pendingOutputs.offer(leftover); // 一点都写不进：放回队列，保持暂停
                return;
            }
            if (!leftover.isEmpty())
            {
                pendingOutputs.offer(leftover); // 部分写入：余量放回队尾，接着试下一条
            }
        }
    }

    /**
     * 存储压紧：把非空槽位整体前移。界面只显示前 {@link #STORAGE_SLOTS} 格，
     * 被挤出可视区的物品会在可视区腾空后自动补回来（旧 {@code InfiniteItemHandler} 动态列表的语义）。
     * 已经前紧后空时是 O(已用类型数) 的空转，无写入。
     */
    private void compact()
    {
        int write = 0;
        while (write < MAX_STORAGE_TYPES && !storage.isEmpty(write))
        {
            write++;
        }
        if (write >= MAX_STORAGE_TYPES)
        {
            return; // 已压紧
        }
        for (int read = write + 1; read < MAX_STORAGE_TYPES; read++)
        {
            if (storage.isEmpty(read))
            {
                continue;
            }
            ItemStack key = storage.key(read);
            long amount = storage.amount(read);
            storage.clear(read);
            storage.setEntry(write, key, amount);
            write++;
        }
    }

    // ---- 经验 ----

    public int getExperiencePoints() { return experiencePoints; }
    public int getExperienceLevel() { return xpToLevel(experiencePoints); }
    public void addExperience(int points) { if (points <= 0) return; this.experiencePoints += points; setChanged(); }
    public int takeExperience(int amount) { int a = Math.max(0, Math.min(amount, experiencePoints)); this.experiencePoints -= a; setChanged(); return a; }
    public int takeAllExperience() { int a = experiencePoints; this.experiencePoints = 0; setChanged(); return a; }

    public static int xpToReach(int level)
    {
        if (level <= 0) return 0;
        if (level <= 16) return level * level + 6 * level;
        if (level <= 31) return (int) (2.5 * level * level - 40.5 * level + 360);
        return (int) (4.5 * level * level - 162.5 * level + 2220);
    }

    public static int xpToLevel(int points)
    {
        if (points <= 0) return 0;
        int level = 0;
        while (level < 30000 && xpToReach(level + 1) <= points) level++;
        return level;
    }

    // ---- AE ----

    public boolean isAeEnabled() { return aeEnabled; }
    public void toggleAeEnabled() { this.aeEnabled = !this.aeEnabled; setChanged(); }

    @Override public IManagedGridNode getMainNode() { return aeNode.getMainNode(); }

    @Override public void saveChanges() { setChanged(); }

    private void pushOutputToAe()
    {
        if (!aeEnabled || !aeNode.isActive()) return;
        IStorageService storageService = aeNode.getStorage();
        if (storageService == null) return;
        MEStorage inv = storageService.getInventory();
        IActionSource source = aeNode.actionSource();
        int used = usedSlots();
        for (int slot = 0; slot < used; slot++)
        {
            long available = storage.amount(slot);
            ItemStack key = storage.key(slot);
            if (available <= 0L || key.isEmpty()) continue;
            AEItemKey aeKey = AEItemKey.of(key);
            if (aeKey == null) continue;
            long inserted = inv.insert(aeKey, available, Actionable.MODULATE, source);
            if (inserted > 0L) storage.extractLong(slot, inserted, false);
        }
    }

    // ---- 生命周期 ----

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

    // ---- tick ----

    public static void serverTick(Level level, BlockPos pos, BlockState state, GodAbsorberBlockEntity be)
    {
        if (level.isClientSide) return;
        be.autoTransfer(level);
        be.flushPendingStorage();
        // 待处理队列没写空 = 产物塞不下：暂停吸收（autoTransfer / AE 仍照常跑，抽走存储腾出空间后自动恢复）
        if (be.enabled && be.pendingOutputs.isEmpty()) be.scanAndAbsorb(level);
        be.aeTick++;
        if (be.aeTick >= 20) { be.aeTick = 0; be.pushOutputToAe(); }
        // 存储压紧：界面只显示前 27 格，压紧后隐藏区的物品自动补进可视区（内容变过才扫）
        if (be.storageDirty)
        {
            be.compact();
            be.storageDirty = false;
        }
    }

    /** 扫描范围内掉落物与经验，吸收进内部存储（每 SCAN_INTERVAL tick 扫一次，一次吸全部）。 */
    private void scanAndAbsorb(Level level)
    {
        if (!(level instanceof ServerLevel) || range <= 0) return;
        scanTimer++;
        if (scanTimer < SCAN_INTERVAL) return;
        scanTimer = 0;

        AABB aabb = new AABB(worldPosition).inflate(range);
        double cx = worldPosition.getX() + 0.5;
        double cy = worldPosition.getY() + 0.5;
        double cz = worldPosition.getZ() + 0.5;
        double rangeSq = (double) range * range;

        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, aabb))
        {
            if (item.isRemoved()) continue;
            if (item.distanceToSqr(cx, cy, cz) > rangeSq) continue;
            ItemStack stack = item.getItem();
            if (stack.isEmpty()) { item.discard(); continue; }
            ItemStack leftover = insertAny(stack, false);
            if (!leftover.isEmpty() && !pendingOutputs.offer(leftover))
            {
                // v5.18.10：连内存待处理队列都满了（理论上不会）——这次不吸，留在世界里等下一轮，绝不掉地也不吞物
                continue;
            }
            item.discard();
        }

        // 神之共鸣·经验（v5.15.4）：经验飞涨倍率（主人在线 + 穿齐全套 + 共鸣开关开）
        double xpMult = level instanceof ServerLevel serverLevel
                ? com.godofthings.handler.ArmorSkillHandler.xpBoost(serverLevel, owner) : 1.0;
        for (ExperienceOrb orb : level.getEntitiesOfClass(ExperienceOrb.class, aabb))
        {
            if (orb.isRemoved()) continue;
            if (orb.distanceToSqr(cx, cy, cz) > rangeSq) continue;
            addExperience((int) Math.min(Integer.MAX_VALUE, Math.round(orb.getValue() * xpMult)));
            orb.discard();
        }
    }

    private void autoTransfer(Level level)
    {
        for (Direction dir : Direction.values())
        {
            int mode = getFaceMode(dir);
            if (mode == FaceMode.NONE.getId()) continue;
            BlockPos np = worldPosition.relative(dir);
            if (!level.isLoaded(np)) continue;
            IItemHandler neighbor = level.getCapability(Capabilities.ItemHandler.BLOCK, np, dir.getOpposite());
            if (neighbor == null) continue;
            if (mode == FaceMode.INPUT.getId()) pullFrom(neighbor);
            else if (mode == FaceMode.OUTPUT.getId()) pushTo(neighbor);
            else if (mode == FaceMode.BOTH.getId()) { pullFrom(neighbor); pushTo(neighbor); }
        }
    }

    /**
     * 从相邻容器抽入物品，按堆搬运（整堆一次搬完，塞不下才把余量退回源槽）。
     * v5.12.1 修复吞吐 bug：原先每个面每 tick 只抽 1 个就 break。
     */
    private void pullFrom(IItemHandler neighbor)
    {
        for (int s = 0; s < neighbor.getSlots(); s++)
        {
            ItemStack src = neighbor.getStackInSlot(s);
            if (src.isEmpty()) continue;
            ItemStack pulled = neighbor.extractItem(s, src.getCount(), false);
            if (pulled.isEmpty()) continue;
            ItemStack leftover = insertAny(pulled, false);
            if (!leftover.isEmpty())
            {
                // 容器塞不下：余量塞回原槽（抽多了才回退，而不是抽 1 个试错）
                neighbor.insertItem(s, leftover, false);
                setChanged();
                break;
            }
            setChanged();
        }
    }

    /** 把存储物品整格推给邻居（全量搬运，搬不走的余量写回原格，不丢不复制）。 */
    private void pushTo(IItemHandler neighbor)
    {
        boolean moved = false;
        int used = usedSlots();
        for (int s = 0; s < used; s++)
        {
            if (LongSlotTransfer.pushSlot(storage, s, neighbor))
            {
                moved = true;
            }
        }
        if (moved)
        {
            compact();
        }
    }

    // ---- NBT ----

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries)
    {
        super.loadAdditional(tag, registries);
        if (tag.hasUUID("Owner"))
        {
            owner = tag.getUUID("Owner");
        }
        this.enabled = tag.getBoolean("Enabled");
        // 走 setRange 而不是直接赋值：老存档里可能存着超过上限的值（v5.17.2 上限由 1600 降到 480）
        setRange(tag.contains("Range") ? tag.getInt("Range") : 16);
        this.experiencePoints = tag.contains("ExperiencePoints") ? tag.getInt("ExperiencePoints") : 0;
        this.aeEnabled = tag.contains("AeEnabled") ? tag.getBoolean("AeEnabled") : true;
        if (tag.contains("FaceModes"))
        {
            int[] modes = tag.getIntArray("FaceModes");
            System.arraycopy(modes, 0, faceModes, 0, Math.min(6, modes.length));
        }
        // 新格式优先；老存档没有该键 → 读旧的 InfiniteItemHandler 格式，再把数量灌进 long 存储（amount = count）
        if (tag.contains("StorageLong", Tag.TAG_LIST))
        {
            storage.loadList(tag.getList("StorageLong", Tag.TAG_COMPOUND), registries);
        }
        else if (tag.contains("Storage"))
        {
            migrateLegacyStorage(tag.getCompound("Storage"), registries);
        }
    }

    /**
     * 老存档迁移：旧格式是 {@link InfiniteItemHandler} 的 {@code {Items:[{Item,Count}]}}
     * （更旧的存档是直接物品标签），整堆读出来后按「同类叠堆 + 占空槽」灌进 long 存储（amount = count）。
     */
    private void migrateLegacyStorage(CompoundTag legacyTag, HolderLookup.Provider registries)
    {
        InfiniteItemHandler legacy = new InfiniteItemHandler();
        legacy.deserializeNBT(registries, legacyTag);
        for (ItemStack stack : legacy.getStacks())
        {
            ItemStack leftover = insertAny(stack, false);
            if (!leftover.isEmpty())
            {
                // v5.18.10：老存档迁移也绝不掉地 —— 余量进内存待处理队列，下一 tick 重试写进 long 存储
                pendingOutputs.offer(leftover);
            }
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries)
    {
        super.saveAdditional(tag, registries);
        if (owner != null)
        {
            tag.putUUID("Owner", owner);
        }
        tag.putBoolean("Enabled", this.enabled);
        tag.putInt("Range", this.range);
        tag.putInt("ExperiencePoints", this.experiencePoints);
        tag.putBoolean("AeEnabled", this.aeEnabled);
        tag.putIntArray("FaceModes", faceModes);
        // v5.18.7：存储改成「自管 long」格式（单格可超 99），每条 {Slot:int, Item:count=1, Amount:long}
        tag.put("StorageLong", storage.saveList(registries));
    }

    // ---- 菜单 ----

    @Override
    @NotNull
    public Component getDisplayName()
    {
        return Component.translatable("container.godofthings.god_absorber");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, @NotNull Inventory playerInventory, @NotNull Player player)
    {
        return new GodAbsorberMenu(id, playerInventory, this);
    }
}
