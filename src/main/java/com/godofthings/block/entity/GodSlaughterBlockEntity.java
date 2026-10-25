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
import com.godofthings.block.entity.machine.PendingOutputQueue;
import com.godofthings.item.ConfigurableMachine;
import com.godofthings.item.LootingHelper;
import com.godofthings.godgrid.LongSlotHandler;
import com.godofthings.godgrid.LongSlotTransfer;
import com.godofthings.menu.GodSlaughterMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 神之砍杀方块实体：
 * <ul>
 *   <li>功能：开关、范围（0-{@link #MAX_RANGE}）、抢夺开关与强度（0-{@link #MAX_LOOTING}）、秒杀开关。击杀范围内生物，
 *       掉落物直接进内部存储（单格容量 int 上限，同类恒单堆），不在世界生成。</li>
 *   <li>存储：{@link LongSlotHandler}（自管 long 计数，单格容量 int 上限），UI 显示前 27 格。</li>
 *   <li>输入输出：六面 FaceMode 配置（NONE/INPUT/OUTPUT/BOTH），自动抽入/推出。</li>
 * </ul>
 * <p>v5.18.10：产物装不下内部存储时进内存待处理队列并暂停击杀，不再掉到世界里（见 {@code pendingOutputs} 注释）。
 */
public class GodSlaughterBlockEntity extends BlockEntity implements MenuProvider, IGridConnectedBlockEntity, MachineOwner, ConfigurableMachine
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
    /** 击杀范围上限（方块）。v5.17.2 按用户要求从 1600 降到 480（注意 {@link #MAX_LOOTING} 是另一个量，不受影响）。 */
    public static final int MAX_RANGE = 480;
    /** 抢夺强度上限（面板值；经 {@link LootingHelper#lootingLevel} 指数映射到掠夺附魔等级 0-255）。
     *  <p>此前误复用 {@link #MAX_RANGE} 作为 clamp 上限 —— 两个语义不同的量共用一个常量，
     *  改范围上限会连带改抢夺上限，故拆开。</p> */
    public static final int MAX_LOOTING = 1600;

    private static final int SCAN_INTERVAL = 10;

    // ---- 功能 ----
    private boolean enabled = false;
    private int range = 16;
    private boolean lootingEnabled = false;
    private int looting = 100;
    private boolean instantKill = true;

    /** 内部存储的经验点数（击杀生物吸收，不生成经验球）。 */
    private int experiencePoints = 0;

    // ---- 存储 ----
    /**
     * v5.18.7：改用自管 long 计数的 {@link LongSlotHandler}（单格容量 = int 上限 2,147,483,647）。
     *
     * <p>原先的 {@link InfiniteItemHandler} 虽然存档把数量单独存，但内存里的 {@code ItemStack}
     * count 会是 &gt;99 的非法堆，一旦经界面同步 / 别的模组读取就可能崩，也没有「对外视图 ≤99」的概念。
     * 现在对外视图恒 ≤99，真实数量走 long。</p>
     */
    /** 存储内容是否变过（变了才需要压紧，避免空机器每 tick 空扫全表） */
    private boolean storageDirty = false;
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
     * <b>队列非空期间暂停击杀</b>，队列清空后自动恢复。
     *
     * <p>取舍：队列是纯内存态、<b>不进存档</b> —— 拆机器 / 掉电 / 区块卸载时队列里的余量会消失；
     * 另一条路只有「掉到世界里」，按要求宁可丢这一点余量也不把产物撒进世界。
     * 实际上只有内部存储的「不同类型数」（{@link #MAX_STORAGE_TYPES}）被塞满时才会用到它。</p>
     */
    private final PendingOutputQueue pendingOutputs = new PendingOutputQueue();

    // ---- 面模式（输入输出配置） ----
    private final int[] faceModes = new int[6];
    private final SideHandler[] sideHandlers = new SideHandler[6];

    /** 是否接入 AE（并网后存储内容自动输出进 AE 网络，占一个频道）。 */
    private boolean aeEnabled = true;

    /** AE 网格节点（线缆直连并网）。 */
    private final AeGridNode aeNode = new AeGridNode(this);
    private int aeTick = 0;

    // ---- 击杀用 ----
    private FakePlayer fakePlayer;
    private ItemStack sword = ItemStack.EMPTY;
    private int swordPower = -1;
    private int scanTimer = 0;

    public GodSlaughterBlockEntity(BlockPos pos, BlockState state)
    {
        super(Godofthings.GOD_SLAUGHTER_BE.get(), pos, state);
        for (int i = 0; i < 6; i++)
        {
            this.sideHandlers[i] = new SideHandler(i);
        }
    }

    // ---- 功能访问 ----

    public boolean isEnabled()
    {
        return enabled;
    }

    public void toggleEnabled()
    {
        this.enabled = !this.enabled;
        setChanged();
    }

    public int getRange()
    {
        return range;
    }

    public void setRange(int value)
    {
        this.range = Math.max(0, Math.min(MAX_RANGE, value));
        setChanged();
    }

    public boolean isLootingEnabled()
    {
        return lootingEnabled;
    }

    public void toggleLootingEnabled()
    {
        this.lootingEnabled = !this.lootingEnabled;
        this.swordPower = -1; // 抢夺开关变化，重建剑
        setChanged();
    }

    public int getLooting()
    {
        return looting;
    }

    public void setLooting(int value)
    {
        this.looting = Math.max(0, Math.min(MAX_LOOTING, value));
        this.swordPower = -1; // 抢夺强度变化，重建剑
        setChanged();
    }

    public boolean isInstantKill()
    {
        return instantKill;
    }

    public void toggleInstantKill()
    {
        this.instantKill = !this.instantKill;
        setChanged();
    }

    // ---- 面模式 ----

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
        public static void onRegisterCapabilities(RegisterCapabilitiesEvent event)
        {
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, Godofthings.GOD_SLAUGHTER_BE.get(),
                    (be, side) -> be.getSideCapability(side));
            event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, Godofthings.GOD_SLAUGHTER_BE.get(),
                    (be, side) -> be);
        }
    }

    /** 某个面的包装 handler：INPUT 面可插入，OUTPUT 面可提取。 */
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
            return STORAGE_SLOTS;
        }

        @Override
        public ItemStack getStackInSlot(int slot)
        {
            return storage.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
        {
            // 插入不看槽位索引：同类叠到已有槽，否则占第一个空槽（与旧 InfiniteItemHandler 一致），
            // 所以界面 27 格之外的隐藏槽也能经管道补料
            return (mode() == FaceMode.INPUT || mode() == FaceMode.BOTH)
                    ? insertAny(stack, simulate)
                    : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate)
        {
            return (mode() == FaceMode.OUTPUT || mode() == FaceMode.BOTH)
                    ? storage.extractItem(slot, amount, simulate)
                    : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot)
        {
            // 单格容量 = int 上限（不再有 64 / 99 的假上限）
            return (int) Math.min(Integer.MAX_VALUE, storage.capacity());
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack)
        {
            return true;
        }
    }

    /** 存储视图（菜单槽位用）：直接暴露 long 存储本身，界面只注册前 {@link #STORAGE_SLOTS} 格。 */
    public LongSlotHandler getStorageView()
    {
        return storage;
    }

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

    // ---- AE 接入 ----

    public boolean isAeEnabled()
    {
        return aeEnabled;
    }

    public void toggleAeEnabled()
    {
        this.aeEnabled = !this.aeEnabled;
        setChanged();
    }

    // ---- ConfigurableMachine：给「神之配置」物品读写（六面模式 / AE 开关；本机没有加速槽）----

    @Override
    public int[] getFaceModesSnapshot()
    {
        return faceModes.clone();
    }

    @Override
    public void applyFaceModes(int[] modes)
    {
        if (modes == null)
        {
            return;
        }
        for (int i = 0; i < faceModes.length && i < modes.length; i++)
        {
            faceModes[i] = ((modes[i] % 4) + 4) % 4;
        }
        setChanged();
    }

    @Override
    public void setAeEnabled(boolean enabled)
    {
        if (this.aeEnabled != enabled)
        {
            this.aeEnabled = enabled;
            setChanged();
        }
    }

    @Override
    public int acceleratorCount()
    {
        // 砍杀机没有加速槽
        return 0;
    }

    @Override
    public int addAccelerators(int want)
    {
        // 砍杀机没有加速槽，启动数由斧头决定
        return 0;
    }

    // ---- AE 网格节点（线缆直连并网，存储内容自动输出进 AE） ----

    @Override
    public IManagedGridNode getMainNode() { return aeNode.getMainNode(); }

    @Override
    public void saveChanges() { setChanged(); }

    /** 把内部存储的物品推入 AE 网络（节流由 tick 控制；按 long 全量推，网络收下多少扣多少）。 */
    private void pushOutputToAe()
    {
        if (!aeEnabled || !aeNode.isActive())
        {
            return;
        }
        IStorageService storageService = aeNode.getStorage();
        if (storageService == null)
        {
            return;
        }
        MEStorage inv = storageService.getInventory();
        IActionSource source = aeNode.actionSource();
        int used = usedSlots();
        for (int slot = 0; slot < used; slot++)
        {
            long available = storage.amount(slot);
            ItemStack key = storage.key(slot);
            if (available <= 0L || key.isEmpty())
            {
                continue;
            }
            AEItemKey aeKey = AEItemKey.of(key);
            if (aeKey == null)
            {
                continue;
            }
            long inserted = inv.insert(aeKey, available, Actionable.MODULATE, source);
            if (inserted > 0L)
            {
                storage.extractLong(slot, inserted, false);
            }
        }
    }

    // ---- 经验 ----

    public int getExperiencePoints()
    {
        return experiencePoints;
    }

    /** 击杀生物吸收经验（LivingExperienceDropEvent 调用）。 */
    public void addExperience(int points)
    {
        if (points <= 0)
        {
            return;
        }
        this.experiencePoints += points;
        setChanged();
    }

    /** 取出指定数量的经验点（返回实际取出量）。 */
    public int takeExperience(int amount)
    {
        int actual = Math.max(0, Math.min(amount, experiencePoints));
        this.experiencePoints -= actual;
        setChanged();
        return actual;
    }

    public int takeAllExperience()
    {
        int all = experiencePoints;
        this.experiencePoints = 0;
        setChanged();
        return all;
    }

    /** 存储经验点数换算成的等级（原版累计经验公式）。 */
    public int getExperienceLevel()
    {
        return xpToLevel(experiencePoints);
    }

    /** 升到指定等级需要的累计经验点数（原版公式）。 */
    public static int xpToReach(int level)
    {
        if (level <= 0)
        {
            return 0;
        }
        if (level <= 16)
        {
            return level * level + 6 * level;
        }
        if (level <= 31)
        {
            return (int) (2.5 * level * level - 40.5 * level + 360);
        }
        return (int) (4.5 * level * level - 162.5 * level + 2220);
    }

    /** 累计经验点数 → 等级（原版公式反解）。 */
    public static int xpToLevel(int points)
    {
        if (points <= 0)
        {
            return 0;
        }
        int level = 0;
        while (level < 30000 && xpToReach(level + 1) <= points)
        {
            level++;
        }
        return level;
    }

    /** 掉落物进存储（掉落拦截调用）。 */
    public void insertIntoStorage(ItemStack stack)
    {
        if (stack.isEmpty())
        {
            return;
        }
        ItemStack leftover = insertAny(stack, false);
        if (!leftover.isEmpty())
        {
            // v5.18.10：物品类型数达上限（MAX_STORAGE_TYPES）也绝不掉地 —— 余量进内存待处理队列，下一 tick 重试写入
            pendingOutputs.offer(leftover);
        }
        setChanged();
    }

    /** 把待处理队列里的产物重试写进内部存储；写空后击杀自动恢复。 */
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

    // ---- tick ----

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

    public static void serverTick(Level level, BlockPos pos, BlockState state, GodSlaughterBlockEntity be)
    {
        if (level.isClientSide)
        {
            return;
        }
        be.autoTransfer(level);
        be.flushPendingStorage();
        // 待处理队列没写空 = 产物塞不下：暂停击杀（autoTransfer / AE 仍照常跑，抽走存储腾出空间后自动恢复）
        if (be.enabled && be.pendingOutputs.isEmpty())
        {
            be.scanAndKill(level);
        }
        // AE 产物输出节流：每 20 tick（1 秒）推一次
        be.aeTick++;
        if (be.aeTick >= 20)
        {
            be.aeTick = 0;
            be.pushOutputToAe();
        }
        // 存储压紧：界面只显示前 27 格，压紧后隐藏区的物品自动补进可视区（内容变过才扫）
        if (be.storageDirty)
        {
            be.compact();
            be.storageDirty = false;
        }
    }

    /** 扫描范围内生物并一次性全部击杀（每 SCAN_INTERVAL tick 扫一次，范围内全部瞬间死亡）。 */
    private void scanAndKill(Level level)
    {
        if (!(level instanceof ServerLevel serverLevel) || range <= 0)
        {
            return;
        }
        scanTimer++;
        if (scanTimer < SCAN_INTERVAL)
        {
            return;
        }
        scanTimer = 0;

        AABB aabb = new AABB(worldPosition).inflate(range);
        List<LivingEntity> mobs = level.getEntitiesOfClass(LivingEntity.class, aabb);
        double cx = worldPosition.getX() + 0.5;
        double cy = worldPosition.getY() + 0.5;
        double cz = worldPosition.getZ() + 0.5;
        double rangeSq = (double) range * range;
        for (LivingEntity mob : mobs)
        {
            if (mob instanceof Player || mob.isDeadOrDying() || mob.isRemoved())
            {
                continue;
            }
            // 保护名单（盔甲架 / 村民 / 流浪商人 / 已驯服宠物 / 起过名字的生物）：一律不杀
            if (isProtected(mob))
            {
                continue;
            }
            // 球形半径：只杀距离中心 ≤ range 的生物（避免 AABB 对角方向超出半径）
            if (mob.distanceToSqr(cx, cy, cz) > rangeSq)
            {
                continue;
            }
            killTarget(serverLevel, mob);
        }
    }

    /**
     * 保护名单：这些生物**永远不会**被本机击杀（硬保护，界面里不可关闭）。
     *
     * <p>理由：砍杀机是「范围内全部秒杀」，范围一开大误伤代价极高 —— 盔甲架是玩家的展示/装备架，
     * 村民与流浪商人是交易资源，已驯服宠物和起过名字的生物基本都算玩家自己的东西。
     * 玩家的正常用途（刷牛羊猪、刷怪塔）都不受影响。</p>
     *
     * <p>公开是为了让回归测试直接断言（{@code GodSlaughterGameTest}）。</p>
     */
    public static boolean isProtected(LivingEntity mob)
    {
        if (mob instanceof ArmorStand)
        {
            return true;                                   // 盔甲架（含摆在上面的装备）
        }
        if (mob instanceof Villager || mob instanceof WanderingTrader)
        {
            return true;                                   // 村民 / 流浪商人：交易资源
        }
        if (mob instanceof TamableAnimal tameable && tameable.isTame())
        {
            return true;                                   // 已驯服的狼 / 猫 / 鹦鹉 等
        }
        // 马 / 驴 / 骡 / 羊驼：**{@code AbstractHorse} 继承的是 {@code Animal} 而不是 {@code TamableAnimal}**，
        // 驯服状态存在它自己的 {@code isTamed()} 里 —— 只判 TamableAnimal 会漏掉它们（用户实测报过）。
        if (mob instanceof AbstractHorse horse && horse.isTamed())
        {
            return true;
        }
        // 再兜一层：任何「有主人」的生物（OwnableEntity）都算玩家的东西
        if (mob instanceof OwnableEntity ownable && ownable.getOwnerUUID() != null)
        {
            return true;
        }
        return mob.hasCustomName();                        // 起过名字的（命名牌 / 铁砧改名）
    }

    /** 击杀单个生物：用带抢夺附魔的假玩家剑作攻击者，标记生物供掉落拦截进存储。 */
    private void killTarget(ServerLevel level, LivingEntity target)
    {
        FakePlayer fp = getFakePlayer(level);
        ItemStack sword = getSword(level);
        fp.setItemInHand(InteractionHand.MAIN_HAND, sword);
        // 标记：掉落拦截（LivingDropsEvent）据此把掉落物转入本机存储
        target.getPersistentData().putLong("GodSlaughterMachine", worldPosition.asLong());
        if (instantKill)
        {
            target.hurt(level.damageSources().playerAttack(fp), Float.MAX_VALUE);
        }
        else
        {
            target.hurt(level.damageSources().playerAttack(fp), 20.0F);
        }
    }

    private FakePlayer getFakePlayer(ServerLevel level)
    {
        if (fakePlayer == null)
        {
            // 神之共鸣（v5.15.4 修复）：假玩家以主人 UUID 归属——事件侧 ownerOf 按 UUID 反查在线主人判共鸣
            // （生物掉落 / 头颅 / 怪蛋 / 经验四条共鸣全经此生效）。主人未记录时退回共享假玩家（同旧版）。
            fakePlayer = owner != null
                    ? FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(owner, "GodofThings"))
                    : FakePlayerFactory.getMinecraft(level);
        }
        return fakePlayer;
    }

    private ItemStack getSword(ServerLevel level)
    {
        int power = lootingEnabled ? looting : 0;
        if (sword.isEmpty() || swordPower != power)
        {
            sword = new ItemStack(Items.NETHERITE_SWORD);
            if (power > 0)
            {
                LootingHelper.applyLooting(sword, level, power);
            }
            swordPower = power;
        }
        return sword;
    }

    // ---- 自动传输（面配置输出） ----

    private void autoTransfer(Level level)
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
                pullFrom(neighborCap);
                pushTo(neighborCap);
            }
        }
    }

    /** 从邻居抽整堆进存储（v5.18.7：按堆搬运，整堆一次搬完，塞不下才把余量退回源槽）。 */
    private void pullFrom(IItemHandler neighbor)
    {
        for (int s = 0; s < neighbor.getSlots(); s++)
        {
            ItemStack src = neighbor.getStackInSlot(s);
            if (src.isEmpty())
            {
                continue;
            }
            ItemStack pulled = neighbor.extractItem(s, src.getCount(), false);
            if (pulled.isEmpty())
            {
                continue;
            }
            ItemStack leftover = insertAny(pulled, false);
            if (!leftover.isEmpty())
            {
                // 存储塞不下：余量退回源槽（抽多了才回退，而不是抽 1 个试错）
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

    // ---- 掉落拦截（机器击杀的掉落物进存储，不在世界生成） ----

    @EventBusSubscriber(modid = Godofthings.MODID)
    public static class SlaughterEvents
    {
        /** 经验吸收：机器击杀的生物经验点进内部存储，不生成经验球（不清理标记，掉落事件随后清理）。 */
        @SubscribeEvent
        public static void onLivingExperienceDrop(LivingExperienceDropEvent event)
        {
            LivingEntity entity = event.getEntity();
            CompoundTag data = entity.getPersistentData();
            if (!data.contains("GodSlaughterMachine"))
            {
                return;
            }
            long posLong = data.getLong("GodSlaughterMachine");
            if (!(entity.level() instanceof ServerLevel level))
            {
                return;
            }
            BlockPos pos = BlockPos.of(posLong);
            if (level.getBlockEntity(pos) instanceof GodSlaughterBlockEntity be)
            {
                int xp = event.getDroppedExperience();
                event.setDroppedExperience(0);
                be.addExperience(xp);
            }
        }

        @SubscribeEvent
        public static void onLivingDrops(LivingDropsEvent event)
        {
            LivingEntity entity = event.getEntity();
            CompoundTag data = entity.getPersistentData();
            if (!data.contains("GodSlaughterMachine"))
            {
                return;
            }
            long posLong = data.getLong("GodSlaughterMachine");
            data.remove("GodSlaughterMachine");
            if (!(entity.level() instanceof ServerLevel level))
            {
                return;
            }
            BlockPos pos = BlockPos.of(posLong);
            if (level.getBlockEntity(pos) instanceof GodSlaughterBlockEntity be)
            {
                for (ItemEntity drop : event.getDrops())
                {
                    be.insertIntoStorage(drop.getItem());
                }
                event.getDrops().clear();
                event.setCanceled(true);
            }
        }
    }

    // ------------------------------------------------------------------ NBT

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
        this.lootingEnabled = tag.getBoolean("LootingEnabled");
        this.looting = tag.contains("Looting") ? tag.getInt("Looting") : 100;
        this.instantKill = tag.contains("InstantKill") ? tag.getBoolean("InstantKill") : true;
        this.aeEnabled = tag.contains("AeEnabled") ? tag.getBoolean("AeEnabled") : true;
        this.experiencePoints = tag.contains("ExperiencePoints") ? tag.getInt("ExperiencePoints") : 0;
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
        tag.putBoolean("LootingEnabled", this.lootingEnabled);
        tag.putInt("Looting", this.looting);
        tag.putBoolean("InstantKill", this.instantKill);
        tag.putInt("ExperiencePoints", this.experiencePoints);
        tag.putIntArray("FaceModes", faceModes);
        tag.putBoolean("AeEnabled", aeEnabled);
        // v5.18.7：存储改成「自管 long」格式（单格可超 99），每条 {Slot:int, Item:count=1, Amount:long}
        tag.put("StorageLong", storage.saveList(registries));
    }

    // ------------------------------------------------------------------ 菜单

    @Override
    @NotNull
    public Component getDisplayName()
    {
        return Component.translatable("container.godofthings.god_slaughter");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, @NotNull Inventory playerInventory, @NotNull Player player)
    {
        return new GodSlaughterMenu(id, playerInventory, this);
    }
}
