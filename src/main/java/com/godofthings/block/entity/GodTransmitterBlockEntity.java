package com.godofthings.block.entity;

import com.godofthings.Godofthings;
import com.godofthings.menu.GodTransmitterMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * 神之传输（能量传输器）方块实体：
 * <ul>
 *   <li>无线连接（机器充能）：维护按维度分组的绑定设备集合，范围内 FE 机器自动绑定，
 *       绑定器也可手动绑定 / 取消绑定；按机器速率（或无上限）逐设备充能。</li>
 *   <li>玩家充能：权限界面选择绑定的玩家，按其速率（或无上限）给物品栏可充能物品充能。</li>
 *   <li>跨维度开关独立（机器 / 玩家），开启后无视距离（跨维度）充能。</li>
 * </ul>
 */
public class GodTransmitterBlockEntity extends BlockEntity implements MenuProvider
{
    public static final int RANGE = 64;
    public static final int TICKS_PER_SCAN = 10;
    public static final int MIN_RATE = 1;
    public static final int MAX_RATE = 9999999;
    /** 机器速率滑块预设档位。 */
    public static final int[] RATE_PRESETS = {100, 1000, 5000, 10000, 100000};

    /** 已加载的神之传输注册表（供绑定器查找最近传输器）。 */
    private static final Set<GodTransmitterBlockEntity> LOADED =
            Collections.newSetFromMap(new WeakHashMap<>());

    // ---- 无线连接（机器充能）----
    private int machineRate = 100;
    private boolean machineUnlimited = false;
    private boolean machineCrossDimension = false;
    private final Map<ResourceKey<Level>, Set<BlockPos>> boundMachines = new HashMap<>();
    private final Map<ResourceKey<Level>, Set<BlockPos>> excludedMachines = new HashMap<>();

    // ---- 玩家充能 ----
    private boolean playerEnabled = true;
    private boolean playerCrossDimension = false;
    private int playerRate = 100;
    private boolean playerUnlimited = true;
    private final Set<UUID> boundPlayers = new HashSet<>();

    private int scanTimer = 0;

    public GodTransmitterBlockEntity(BlockPos pos, BlockState state)
    {
        super(Godofthings.GOD_TRANSMITTER_BE.get(), pos, state);
    }

    // ---- 机器充能访问 ----

    public int getMachineRate()
    {
        return machineRate;
    }

    public void setMachineRate(int rate)
    {
        this.machineRate = Math.max(MIN_RATE, Math.min(MAX_RATE, rate));
        setChanged();
    }

    public boolean isMachineUnlimited()
    {
        return machineUnlimited;
    }

    public void toggleMachineUnlimited()
    {
        this.machineUnlimited = !this.machineUnlimited;
        setChanged();
    }

    public boolean isMachineCrossDimension()
    {
        return machineCrossDimension;
    }

    public void toggleMachineCrossDimension()
    {
        this.machineCrossDimension = !this.machineCrossDimension;
        setChanged();
    }

    // ---- 玩家充能访问 ----

    public boolean isPlayerEnabled()
    {
        return playerEnabled;
    }

    public void togglePlayerEnabled()
    {
        this.playerEnabled = !this.playerEnabled;
        setChanged();
    }

    public boolean isPlayerCrossDimension()
    {
        return playerCrossDimension;
    }

    public void togglePlayerCrossDimension()
    {
        this.playerCrossDimension = !this.playerCrossDimension;
        setChanged();
    }

    public int getPlayerRate()
    {
        return playerRate;
    }

    public void setPlayerRate(int rate)
    {
        this.playerRate = Math.max(MIN_RATE, Math.min(MAX_RATE, rate));
        setChanged();
    }

    public boolean isPlayerUnlimited()
    {
        return playerUnlimited;
    }

    public void togglePlayerUnlimited()
    {
        this.playerUnlimited = !this.playerUnlimited;
        setChanged();
    }

    // ---- 玩家绑定 ----

    public boolean isPlayerBound(UUID uuid)
    {
        return boundPlayers.contains(uuid);
    }

    public void bindPlayer(UUID uuid)
    {
        boundPlayers.add(uuid);
        setChanged();
    }

    public void unbindPlayer(UUID uuid)
    {
        boundPlayers.remove(uuid);
        setChanged();
    }

    public Set<UUID> getBoundPlayers()
    {
        return boundPlayers;
    }

    public int getBoundPlayerCount()
    {
        return boundPlayers.size();
    }

    // ---- 机器绑定 ----

    public int getBoundCount()
    {
        int n = 0;
        for (Set<BlockPos> set : boundMachines.values())
        {
            n += set.size();
        }
        return n;
    }

    public void clearAllBindings()
    {
        boundMachines.clear();
        excludedMachines.clear();
        setChanged();
    }

    public boolean isBound(ResourceKey<Level> dim, BlockPos pos)
    {
        Set<BlockPos> set = boundMachines.get(dim);
        return set != null && set.contains(pos);
    }

    /** 手动绑定机器（加入绑定集合，移除排除）。 */
    public void bindMachine(ResourceKey<Level> dim, BlockPos pos)
    {
        boundMachines.computeIfAbsent(dim, k -> new HashSet<>()).add(pos);
        Set<BlockPos> ex = excludedMachines.get(dim);
        if (ex != null)
        {
            ex.remove(pos);
        }
        setChanged();
    }

    /** 取消绑定（移出绑定集合，加入排除，阻止范围扫描重新自动绑定）。 */
    public void unbindMachine(ResourceKey<Level> dim, BlockPos pos)
    {
        Set<BlockPos> set = boundMachines.get(dim);
        if (set != null)
        {
            set.remove(pos);
        }
        excludedMachines.computeIfAbsent(dim, k -> new HashSet<>()).add(pos);
        setChanged();
    }

    /** 已绑定机器坐标文本列表（供 UI 显示），格式 "维度 x y z"。 */
    public List<String> getBoundMachineTexts()
    {
        List<String> list = new ArrayList<>();
        for (Map.Entry<ResourceKey<Level>, Set<BlockPos>> entry : boundMachines.entrySet())
        {
            String dim = entry.getKey().location().toString();
            for (BlockPos p : entry.getValue())
            {
                list.add(dim + " " + p.getX() + " " + p.getY() + " " + p.getZ());
            }
        }
        return list;
    }

    // ---- 注册表 ----

    @Override
    public void onLoad()
    {
        super.onLoad();
        if (level != null && !level.isClientSide)
        {
            LOADED.add(this);
        }
    }

    @Override
    public void setRemoved()
    {
        LOADED.remove(this);
        super.setRemoved();
    }

    /** 找同维度距离最近的已加载神之传输。 */
    @Nullable
    public static GodTransmitterBlockEntity findNearest(Level level, BlockPos pos)
    {
        GodTransmitterBlockEntity nearest = null;
        double best = Double.MAX_VALUE;
        for (GodTransmitterBlockEntity be : LOADED)
        {
            if (be.getLevel() == null || be.getLevel() != level || be.isRemoved())
            {
                continue;
            }
            double d = be.getBlockPos().distSqr(pos);
            if (d < best)
            {
                best = d;
                nearest = be;
            }
        }
        return nearest;
    }

    // ---- tick ----

    public static void serverTick(Level level, BlockPos pos, BlockState state, GodTransmitterBlockEntity be)
    {
        if (be.playerEnabled)
        {
            be.chargePlayers(level);
        }
        be.chargeBoundMachines(level);
        be.scanTimer++;
        if (be.scanTimer >= TICKS_PER_SCAN)
        {
            be.scanTimer = 0;
            be.scanMachines(level, pos);
        }
    }

    /** 给绑定的玩家充能。 */
    private void chargePlayers(Level level)
    {
        if (level.isClientSide || level.getServer() == null || boundPlayers.isEmpty())
        {
            return;
        }
        int amount = playerUnlimited ? Integer.MAX_VALUE : playerRate;
        for (UUID uuid : boundPlayers)
        {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(uuid);
            if (player == null)
            {
                continue;
            }
            if (!playerCrossDimension)
            {
                if (player.level().dimension() != level.dimension())
                {
                    continue;
                }
                if (player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                        worldPosition.getZ() + 0.5) > (double) RANGE * RANGE)
                {
                    continue;
                }
            }
            chargePlayerInventory(player, amount);
        }
    }

    private static void chargePlayerInventory(ServerPlayer player, int amount)
    {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++)
        {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty())
            {
                continue;
            }
            IEnergyStorage storage = stack.getCapability(Capabilities.EnergyStorage.ITEM);
            if (storage != null && storage.canReceive())
            {
                storage.receiveEnergy(amount, false);
            }
        }
    }

    /** 给绑定设备充能（按维度，跨维度开关决定是否跨维度）。 */
    private void chargeBoundMachines(Level level)
    {
        if (level.isClientSide || level.getServer() == null)
        {
            return;
        }
        int amount = machineUnlimited ? Integer.MAX_VALUE : machineRate;
        for (Map.Entry<ResourceKey<Level>, Set<BlockPos>> entry : boundMachines.entrySet())
        {
            Level targetLevel = entry.getKey().equals(level.dimension())
                    ? level
                    : level.getServer().getLevel(entry.getKey());
            if (targetLevel == null)
            {
                continue;
            }
            if (!machineCrossDimension && !entry.getKey().equals(level.dimension()))
            {
                continue;
            }
            for (BlockPos target : entry.getValue())
            {
                if (!machineCrossDimension && target.distSqr(worldPosition) > (long) RANGE * RANGE)
                {
                    continue;
                }
                chargeMachineAt(targetLevel, target, amount);
            }
        }
    }

    /** 扫描范围内 FE 机器，自动加入绑定集合（排除集合里的跳过）。 */
    private void scanMachines(Level level, BlockPos pos)
    {
        if (level.isClientSide)
        {
            return;
        }
        ResourceKey<Level> dim = level.dimension();
        for (int dx = -RANGE; dx <= RANGE; dx++)
        {
            for (int dz = -RANGE; dz <= RANGE; dz++)
            {
                for (int dy = -2; dy <= 2; dy++)
                {
                    BlockPos target = pos.offset(dx, dy, dz);
                    if (target.equals(pos))
                    {
                        continue;
                    }
                    if (isBound(dim, target))
                    {
                        continue;
                    }
                    Set<BlockPos> ex = excludedMachines.get(dim);
                    if (ex != null && ex.contains(target))
                    {
                        continue;
                    }
                    if (!level.getBlockState(target).hasBlockEntity())
                    {
                        continue;
                    }
                    if (hasEnergyStorage(level, target))
                    {
                        boundMachines.computeIfAbsent(dim, k -> new HashSet<>()).add(target);
                    }
                }
            }
        }
    }

    private static void chargeMachineAt(Level level, BlockPos target, int amount)
    {
        IEnergyStorage storage = level.getCapability(Capabilities.EnergyStorage.BLOCK, target, null);
        if (storage != null && storage.canReceive())
        {
            storage.receiveEnergy(amount, false);
            return;
        }
        // 部分机器（如 Mekanism）仅在特定面暴露能力，遍历六面兜底
        for (Direction side : Direction.values())
        {
            storage = level.getCapability(Capabilities.EnergyStorage.BLOCK, target, side);
            if (storage != null && storage.canReceive())
            {
                storage.receiveEnergy(amount, false);
                return;
            }
        }
    }

    public static boolean hasEnergyStorage(Level level, BlockPos pos)
    {
        if (level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null) != null)
        {
            return true;
        }
        for (Direction side : Direction.values())
        {
            if (level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, side) != null)
            {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ NBT

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries)
    {
        super.loadAdditional(tag, registries);
        this.machineRate = tag.contains("MachineRate") ? tag.getInt("MachineRate") : 100;
        this.machineUnlimited = tag.getBoolean("MachineUnlimited");
        this.machineCrossDimension = tag.getBoolean("MachineCrossDimension");
        this.playerEnabled = tag.contains("PlayerEnabled") ? tag.getBoolean("PlayerEnabled") : true;
        this.playerCrossDimension = tag.getBoolean("PlayerCrossDimension");
        this.playerRate = tag.contains("PlayerRate") ? tag.getInt("PlayerRate") : 100;
        this.playerUnlimited = tag.contains("PlayerUnlimited") ? tag.getBoolean("PlayerUnlimited") : true;
        if (tag.contains("BoundMachines"))
        {
            deserializeBound(boundMachines, tag.getList("BoundMachines", 10));
        }
        if (tag.contains("ExcludedMachines"))
        {
            deserializeBound(excludedMachines, tag.getList("ExcludedMachines", 10));
        }
        if (tag.contains("BoundPlayers"))
        {
            boundPlayers.clear();
            ListTag list = tag.getList("BoundPlayers", 10);
            for (int i = 0; i < list.size(); i++)
            {
                CompoundTag t = list.getCompound(i);
                if (t.hasUUID("U"))
                {
                    boundPlayers.add(t.getUUID("U"));
                }
            }
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries)
    {
        super.saveAdditional(tag, registries);
        tag.putInt("MachineRate", this.machineRate);
        tag.putBoolean("MachineUnlimited", this.machineUnlimited);
        tag.putBoolean("MachineCrossDimension", this.machineCrossDimension);
        tag.putBoolean("PlayerEnabled", this.playerEnabled);
        tag.putBoolean("PlayerCrossDimension", this.playerCrossDimension);
        tag.putInt("PlayerRate", this.playerRate);
        tag.putBoolean("PlayerUnlimited", this.playerUnlimited);
        tag.put("BoundMachines", serializeBound(boundMachines));
        tag.put("ExcludedMachines", serializeBound(excludedMachines));
        ListTag players = new ListTag();
        for (UUID uuid : boundPlayers)
        {
            CompoundTag t = new CompoundTag();
            t.putUUID("U", uuid);
            players.add(t);
        }
        tag.put("BoundPlayers", players);
    }

    private static ListTag serializeBound(Map<ResourceKey<Level>, Set<BlockPos>> map)
    {
        ListTag list = new ListTag();
        for (Map.Entry<ResourceKey<Level>, Set<BlockPos>> entry : map.entrySet())
        {
            for (BlockPos p : entry.getValue())
            {
                CompoundTag t = new CompoundTag();
                t.putString("Dim", entry.getKey().location().toString());
                t.putLong("Pos", p.asLong());
                list.add(t);
            }
        }
        return list;
    }

    private static void deserializeBound(Map<ResourceKey<Level>, Set<BlockPos>> map, ListTag list)
    {
        map.clear();
        for (int i = 0; i < list.size(); i++)
        {
            CompoundTag t = list.getCompound(i);
            ResourceLocation loc = ResourceLocation.tryParse(t.getString("Dim"));
            if (loc == null)
            {
                continue;
            }
            ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, loc);
            map.computeIfAbsent(dim, k -> new HashSet<>()).add(BlockPos.of(t.getLong("Pos")));
        }
    }

    // ------------------------------------------------------------------ 菜单

    @Override
    @NotNull
    public Component getDisplayName()
    {
        return Component.translatable("container.godofthings.god_transmitter");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, @NotNull Inventory playerInventory, @NotNull Player player)
    {
        return new GodTransmitterMenu(id, playerInventory, this);
    }
}
