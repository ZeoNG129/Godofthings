package com.godofthings.block.entity;

import appeng.api.AECapabilities;
import appeng.api.config.Actionable;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageService;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.MEStorage;
import appeng.api.util.AECableType;
import com.godofthings.Godofthings;
import com.godofthings.ae2.AeGridNode;
import com.godofthings.menu.GodAbsorberMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
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
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 神之吸收方块实体：大范围吸收掉落物与经验。
 * <ul>
 *   <li>功能：开关、范围（0-1600）。</li>
 *   <li>存储：掉落物进 {@link InfiniteItemHandler}（无堆叠上限，UI 27 格）。</li>
 *   <li>经验：经验球进经验点存储（经验面板可取出）。</li>
 *   <li>面配置（六面输入输出）+ AE 并网（产物自动输出进 AE）。</li>
 * </ul>
 */
public class GodAbsorberBlockEntity extends BlockEntity implements MenuProvider, IInWorldGridNodeHost, IActionHost
{
    public static final int STORAGE_SLOTS = 27;
    public static final int MAX_RANGE = 1600;
    private static final int SCAN_INTERVAL = 10;

    private boolean enabled = false;
    private int range = 16;
    private int experiencePoints = 0;

    private final InfiniteItemHandler storage = new InfiniteItemHandler();
    private final StorageView storageView = new StorageView();

    private final int[] faceModes = new int[6];
    private final SideHandler[] sideHandlers = new SideHandler[6];

    private final AeGridNode aeNode = new AeGridNode(this);
    private boolean aeEnabled = true;
    private int aeTick = 0;
    private int scanTimer = 0;

    public GodAbsorberBlockEntity(BlockPos pos, BlockState state)
    {
        super(Godofthings.GOD_ABSORBER_BE.get(), pos, state);
        this.storage.setOnChange(this::setChanged);
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
            return (mode() == FaceMode.INPUT || mode() == FaceMode.BOTH) ? storage.insertItem(slot, stack, simulate) : stack;
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate)
        {
            return (mode() == FaceMode.OUTPUT || mode() == FaceMode.BOTH) ? storage.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }
        @Override public int getSlotLimit(int slot) { return Integer.MAX_VALUE; }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return true; }
    }

    public class StorageView implements IItemHandlerModifiable
    {
        @Override public int getSlots() { return STORAGE_SLOTS; }
        @Override public ItemStack getStackInSlot(int slot) { return storage.getStackInSlot(slot); }
        @Override public void setStackInSlot(int slot, ItemStack stack) { storage.setStackInSlot(slot, stack); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return storage.insertItem(slot, stack, simulate); }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return storage.extractItem(slot, amount, simulate); }
        @Override public int getSlotLimit(int slot) { return Integer.MAX_VALUE; }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return true; }
    }

    public IItemHandler getStorageView() { return storageView; }
    public int getStorageCount() { return storage.getStacks().size(); }
    public InfiniteItemHandler getItemHandler() { return storage; }

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

    @Override public IGridNode getGridNode(Direction side) { return aeNode.getGridNode(side); }
    @Override public AECableType getCableConnectionType(Direction side) { return aeNode.getCableConnectionType(side); }
    @Override public IGridNode getActionableNode() { return aeNode.getActionableNode(); }

    private void pushOutputToAe()
    {
        if (!aeEnabled || !aeNode.isActive()) return;
        IStorageService storage = aeNode.getStorage();
        if (storage == null) return;
        MEStorage inv = storage.getInventory();
        IActionSource source = aeNode.actionSource();
        for (int slot = 0; slot < this.storage.getSlots(); slot++)
        {
            ItemStack stack = this.storage.getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            long inserted = inv.insert(AEItemKey.of(stack), stack.getCount(), Actionable.MODULATE, source);
            if (inserted > 0) this.storage.extractItem(slot, (int) inserted, false);
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
        if (be.enabled) be.scanAndAbsorb(level);
        be.aeTick++;
        if (be.aeTick >= 20) { be.aeTick = 0; be.pushOutputToAe(); }
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
            ItemStack leftover = storage.insertItem(-1, stack, false);
            if (!leftover.isEmpty()) InfiniteItemHandler.dropRemainder(level, worldPosition, leftover);
            item.discard();
        }

        for (ExperienceOrb orb : level.getEntitiesOfClass(ExperienceOrb.class, aabb))
        {
            if (orb.isRemoved()) continue;
            if (orb.distanceToSqr(cx, cy, cz) > rangeSq) continue;
            addExperience(orb.getValue());
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

    private void pullFrom(IItemHandler neighbor)
    {
        for (int s = 0; s < neighbor.getSlots(); s++)
        {
            ItemStack src = neighbor.getStackInSlot(s);
            if (src.isEmpty()) continue;
            ItemStack leftover = storage.insertItem(-1, neighbor.extractItem(s, 1, false), false);
            if (!leftover.isEmpty()) InfiniteItemHandler.dropRemainder(level, worldPosition, leftover);
            setChanged();
            break;
        }
    }

    private void pushTo(IItemHandler neighbor)
    {
        List<ItemStack> stacks = storage.getStacks();
        for (int s = 0; s < stacks.size() && s < STORAGE_SLOTS; s++)
        {
            ItemStack stack = stacks.get(s);
            if (stack.isEmpty()) continue;
            ItemStack toPush = stack.copy();
            for (int ns = 0; ns < neighbor.getSlots() && !toPush.isEmpty(); ns++)
            {
                ItemStack leftover = neighbor.insertItem(ns, toPush, false);
                int inserted = toPush.getCount() - leftover.getCount();
                if (inserted > 0) { storage.extractItem(s, inserted, false); toPush = leftover; }
            }
        }
    }

    // ---- NBT ----

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries)
    {
        super.loadAdditional(tag, registries);
        this.enabled = tag.getBoolean("Enabled");
        this.range = tag.contains("Range") ? tag.getInt("Range") : 16;
        this.experiencePoints = tag.contains("ExperiencePoints") ? tag.getInt("ExperiencePoints") : 0;
        this.aeEnabled = tag.contains("AeEnabled") ? tag.getBoolean("AeEnabled") : true;
        if (tag.contains("FaceModes"))
        {
            int[] modes = tag.getIntArray("FaceModes");
            System.arraycopy(modes, 0, faceModes, 0, Math.min(6, modes.length));
        }
        if (tag.contains("Storage")) storage.deserializeNBT(registries, tag.getCompound("Storage"));
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries)
    {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Enabled", this.enabled);
        tag.putInt("Range", this.range);
        tag.putInt("ExperiencePoints", this.experiencePoints);
        tag.putBoolean("AeEnabled", this.aeEnabled);
        tag.putIntArray("FaceModes", faceModes);
        tag.put("Storage", storage.serializeNBT(registries));
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
