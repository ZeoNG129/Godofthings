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
import com.godofthings.item.GodSwordItem;
import com.godofthings.menu.GodSlaughterMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
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
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 神之砍杀方块实体：
 * <ul>
 *   <li>功能：开关、范围（0-300）、抢夺开关与强度（0-300）、秒杀开关。击杀范围内生物，
 *       掉落物直接进内部存储（无堆叠上限，同类恒单堆），不在世界生成。</li>
 *   <li>存储：{@link InfiniteItemHandler}（无堆叠上限），UI 显示 27 格。</li>
 *   <li>输入输出：六面 FaceMode 配置（NONE/INPUT/OUTPUT/BOTH），自动抽入/推出。</li>
 * </ul>
 */
public class GodSlaughterBlockEntity extends BlockEntity implements MenuProvider, IGridConnectedBlockEntity
{
    /** UI 显示的存储槽位数量（内部为无限存储，前 27 个堆叠映射到槽位）。 */
    public static final int STORAGE_SLOTS = 27;
    public static final int MAX_RANGE = 1600;

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
    private final InfiniteItemHandler storage = new InfiniteItemHandler();
    private final StorageView storageView = new StorageView();

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
        this.storage.setOnChange(this::setChanged);
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
        this.looting = Math.max(0, Math.min(MAX_RANGE, value));
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
            return (mode() == FaceMode.INPUT || mode() == FaceMode.BOTH)
                    ? storage.insertItem(slot, stack, simulate)
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
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack)
        {
            return true;
        }
    }

    /** 27 格固定视图（菜单槽位用），映射无限存储前 27 个堆叠。 */
    public class StorageView implements IItemHandlerModifiable
    {
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
        public void setStackInSlot(int slot, ItemStack stack)
        {
            storage.setStackInSlot(slot, stack);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
        {
            return storage.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate)
        {
            return storage.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot)
        {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack)
        {
            return true;
        }
    }

    public IItemHandlerModifiable getStorageView()
    {
        return storageView;
    }

    public int getStorageCount()
    {
        return storage.getStacks().size();
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

    // ---- AE 网格节点（线缆直连并网，存储内容自动输出进 AE） ----

    @Override
    public IManagedGridNode getMainNode() { return aeNode.getMainNode(); }

    @Override
    public void saveChanges() { setChanged(); }

    /** 把内部存储物品推入 AE 网络（节流由 tick 控制）。 */
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
        for (int slot = 0; slot < getStorageView().getSlots(); slot++)
        {
            ItemStack stack = getStorageView().getStackInSlot(slot);
            if (stack.isEmpty())
            {
                continue;
            }
            long inserted = inv.insert(AEItemKey.of(stack), stack.getCount(), Actionable.MODULATE, source);
            if (inserted > 0)
            {
                getStorageView().extractItem(slot, (int) inserted, false);
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
        ItemStack leftover = storage.insertItem(-1, stack, false);
        if (!leftover.isEmpty())
        {
            // 类型数达上限，剩余掉落到机器上方
            InfiniteItemHandler.dropRemainder(level, worldPosition, leftover);
        }
        setChanged();
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
        if (be.enabled)
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
            // 球形半径：只杀距离中心 ≤ range 的生物（避免 AABB 对角方向超出半径）
            if (mob.distanceToSqr(cx, cy, cz) > rangeSq)
            {
                continue;
            }
            killTarget(serverLevel, mob);
        }
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
            fakePlayer = FakePlayerFactory.getMinecraft(level);
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
                GodSwordItem.applyLooting(sword, level, power);
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

    /** 从邻居抽入物品到存储。 */
    private void pullFrom(IItemHandler neighbor)
    {
        for (int s = 0; s < neighbor.getSlots(); s++)
        {
            ItemStack src = neighbor.getStackInSlot(s);
            if (src.isEmpty())
            {
                continue;
            }
            ItemStack toInsert = src.copyWithCount(1);
            ItemStack leftover = storage.insertItem(-1, toInsert, true);
            if (leftover.isEmpty())
            {
                storage.insertItem(-1, neighbor.extractItem(s, 1, false), false);
                setChanged();
                break;
            }
        }
    }

    /** 把存储物品推出给邻居。 */
    private void pushTo(IItemHandler neighbor)
    {
        List<ItemStack> stacks = storage.getStacks();
        for (int s = 0; s < stacks.size() && s < STORAGE_SLOTS; s++)
        {
            ItemStack stack = stacks.get(s);
            if (stack.isEmpty())
            {
                continue;
            }
            ItemStack toPush = stack.copy();
            for (int ns = 0; ns < neighbor.getSlots() && !toPush.isEmpty(); ns++)
            {
                ItemStack leftover = neighbor.insertItem(ns, toPush, false);
                int inserted = toPush.getCount() - leftover.getCount();
                if (inserted > 0)
                {
                    storage.extractItem(s, inserted, false);
                    toPush = leftover;
                }
            }
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
        this.enabled = tag.getBoolean("Enabled");
        this.range = tag.contains("Range") ? tag.getInt("Range") : 16;
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
        if (tag.contains("Storage"))
        {
            storage.deserializeNBT(registries, tag.getCompound("Storage"));
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries)
    {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Enabled", this.enabled);
        tag.putInt("Range", this.range);
        tag.putBoolean("LootingEnabled", this.lootingEnabled);
        tag.putInt("Looting", this.looting);
        tag.putBoolean("InstantKill", this.instantKill);
        tag.putInt("ExperiencePoints", this.experiencePoints);
        tag.putIntArray("FaceModes", faceModes);
        tag.putBoolean("AeEnabled", aeEnabled);
        tag.put("Storage", storage.serializeNBT(registries));
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
