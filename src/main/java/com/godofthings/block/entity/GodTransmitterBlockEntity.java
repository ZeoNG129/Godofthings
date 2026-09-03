package com.godofthings.block.entity;

import com.godofthings.Godofthings;
import com.godofthings.menu.GodTransmitterMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * 神之传输（能量传输器）方块实体：
 * <ul>
 *   <li>无线连接（机器充能）：绑定器手动绑定 FE 机器（记录方块 ID），按机器速率（或无上限）逐设备充能；
 *       每 20 tick 校验绑定方块是否仍存在，被打掉自动清理。</li>
 *   <li>玩家充能：权限界面选择绑定的玩家，按其速率（或无上限）给物品栏可充能物品充能。</li>
 *   <li>跨维度开关独立（机器 / 玩家），开启后无视距离（跨维度）充能。</li>
 * </ul>
 */
public class GodTransmitterBlockEntity extends BlockEntity implements MenuProvider
{
    public static final int RANGE = 64;
    public static final int MIN_RATE = 1;
    public static final int MAX_RATE = 9999999;
    /** 机器速率滑块预设档位。 */
    public static final int[] RATE_PRESETS = {100, 1000, 5000, 10000, 100000};

    /** 失效绑定清理间隔（tick）。 */
    private static final int PRUNE_INTERVAL = 20;

    /** 已加载的神之传输注册表（供绑定器查找最近传输器）。 */
    private static final Set<GodTransmitterBlockEntity> LOADED =
            Collections.newSetFromMap(new WeakHashMap<>());

    // ---- 无线连接（机器充能）----
    private int machineRate = 100;
    private boolean machineUnlimited = false;
    private boolean machineCrossDimension = false;
    /** 绑定机器：维度 -> (坐标 -> 绑定时的方块 ID，用于打掉后清理)。 */
    private final Map<ResourceKey<Level>, Map<BlockPos, String>> boundMachines = new HashMap<>();
    private int pruneTimer = 0;
    /** 最近探测到的接收 storage 缓存（同维度坐标），每 PRUNE_INTERVAL 随清理一起失效重建，避免每 tick 重复探测。 */
    private final Map<BlockPos, IEnergyStorage> storageCache = new HashMap<>();

    // ---- 玩家充能 ----
    private boolean playerEnabled = true;
    private boolean playerCrossDimension = false;
    private int playerRate = 100;
    private boolean playerUnlimited = true;
    private final Set<UUID> boundPlayers = new HashSet<>();

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
        for (Map<BlockPos, String> map : boundMachines.values())
        {
            n += map.size();
        }
        return n;
    }

    public void clearAllBindings()
    {
        boundMachines.clear();
        setChanged();
    }

    public boolean isBound(ResourceKey<Level> dim, BlockPos pos)
    {
        Map<BlockPos, String> map = boundMachines.get(dim);
        return map != null && map.containsKey(pos);
    }

    /** 手动绑定机器（记录当前方块 ID，供打掉后自动清理）。 */
    public void bindMachine(Level level, BlockPos pos)
    {
        ResourceKey<Level> dim = level.dimension();
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock());
        String blockId = key == null ? "" : key.toString();
        boundMachines.computeIfAbsent(dim, k -> new HashMap<>()).put(pos, blockId);
        setChanged();
    }

    /** 取消绑定。 */
    public void unbindMachine(ResourceKey<Level> dim, BlockPos pos)
    {
        Map<BlockPos, String> map = boundMachines.get(dim);
        if (map != null)
        {
            map.remove(pos);
            if (map.isEmpty())
            {
                boundMachines.remove(dim);
            }
        }
        setChanged();
    }

    /** 已绑定机器坐标文本列表（供 UI 显示），格式 "维度 x y z"。 */
    public List<String> getBoundMachineTexts()
    {
        List<String> list = new ArrayList<>();
        for (Map.Entry<ResourceKey<Level>, Map<BlockPos, String>> entry : boundMachines.entrySet())
        {
            String dim = entry.getKey().location().toString();
            for (BlockPos p : entry.getValue().keySet())
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
        be.pruneTimer++;
        if (be.pruneTimer >= PRUNE_INTERVAL)
        {
            be.pruneTimer = 0;
            be.pruneBrokenTargets(level);
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
        for (Map.Entry<ResourceKey<Level>, Map<BlockPos, String>> entry : boundMachines.entrySet())
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
            for (BlockPos target : entry.getValue().keySet())
            {
                if (!machineCrossDimension && target.distSqr(worldPosition) > (long) RANGE * RANGE)
                {
                    continue;
                }
                chargeMachineAt(targetLevel, target, amount);
            }
        }
    }

    /**
     * 探测式充能（带缓存）：缓存命中直接用上次探测到的最佳 storage，否则遍历 side=null 与六面
     * 用 simulate 探测实际可接收量选接收量最大的（兼容 Mekanism 等 canReceive 行为特殊 / 特定面暴露能力的机器）。
     */
    private int chargeMachineAt(Level level, BlockPos target, int amount)
    {
        IEnergyStorage best = storageCache.get(target);
        if (best == null)
        {
            best = findBestStorage(level, target, amount);
            if (best != null)
            {
                storageCache.put(target, best);
            }
            else
            {
                return 0;
            }
        }
        int r = probeReceive(best, amount);
        return r > 0 ? best.receiveEnergy(amount, false) : 0;
    }

    /** 探测式找接收量最大的 storage（side=null + 六面）。 */
    private static IEnergyStorage findBestStorage(Level level, BlockPos target, int amount)
    {
        IEnergyStorage best = null;
        int bestReceive = -1;
        IEnergyStorage s = level.getCapability(Capabilities.EnergyStorage.BLOCK, target, null);
        if (s != null)
        {
            int r = probeReceive(s, amount);
            if (r > bestReceive)
            {
                bestReceive = r;
                best = s;
            }
        }
        for (Direction side : Direction.values())
        {
            s = level.getCapability(Capabilities.EnergyStorage.BLOCK, target, side);
            if (s != null)
            {
                int r = probeReceive(s, amount);
                if (r > bestReceive)
                {
                    bestReceive = r;
                    best = s;
                }
            }
        }
        return bestReceive > 0 ? best : null;
    }

    /** simulate 探测 storage 实际可接收量（异常返回 0）。 */
    private static int probeReceive(IEnergyStorage storage, int amount)
    {
        try
        {
            if (!storage.canReceive())
            {
                return 0;
            }
            return Math.max(0, storage.receiveEnergy(amount, true));
        }
        catch (RuntimeException e)
        {
            return 0;
        }
    }

    public static boolean hasEnergyStorage(Level level, BlockPos pos)
    {
        IEnergyStorage s = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
        if (accepts(s))
        {
            return true;
        }
        for (Direction side : Direction.values())
        {
            s = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, side);
            if (accepts(s))
            {
                return true;
            }
        }
        return false;
    }

    private static boolean accepts(IEnergyStorage storage)
    {
        if (storage == null)
        {
            return false;
        }
        try
        {
            return storage.canReceive() || probeReceive(storage, 1000) > 0;
        }
        catch (RuntimeException e)
        {
            return false;
        }
    }

    /** 定期清理失效绑定：方块被打掉（或替换）后，其方块 ID 与绑定记录不一致则移除。 */
    private void pruneBrokenTargets(Level level)
    {
        if (level.isClientSide || level.getServer() == null)
        {
            return;
        }
        boolean changed = false;
        Iterator<Map.Entry<ResourceKey<Level>, Map<BlockPos, String>>> dimIt = boundMachines.entrySet().iterator();
        while (dimIt.hasNext())
        {
            Map.Entry<ResourceKey<Level>, Map<BlockPos, String>> dimEntry = dimIt.next();
            ResourceKey<Level> dim = dimEntry.getKey();
            Level targetLevel = dim.equals(level.dimension()) ? level : level.getServer().getLevel(dim);
            if (targetLevel == null)
            {
                continue;
            }
            Iterator<Map.Entry<BlockPos, String>> posIt = dimEntry.getValue().entrySet().iterator();
            while (posIt.hasNext())
            {
                Map.Entry<BlockPos, String> posEntry = posIt.next();
                if (!blockMatches(targetLevel, posEntry.getKey(), posEntry.getValue()))
                {
                    posIt.remove();
                    changed = true;
                }
            }
            if (dimEntry.getValue().isEmpty())
            {
                dimIt.remove();
            }
        }
        if (changed)
        {
            setChanged();
        }
        // 每 PRUNE_INTERVAL 重建 storage 缓存（机器可能更换面能力/被替换）
        storageCache.clear();
    }

    private static boolean blockMatches(Level level, BlockPos pos, String blockId)
    {
        if (blockId == null || blockId.isEmpty())
        {
            return true; // 旧存档无方块 ID，不清理
        }
        ResourceLocation current = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock());
        return current != null && current.toString().equals(blockId);
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
        ListTag players = new ListTag();
        for (UUID uuid : boundPlayers)
        {
            CompoundTag t = new CompoundTag();
            t.putUUID("U", uuid);
            players.add(t);
        }
        tag.put("BoundPlayers", players);
    }

    private static ListTag serializeBound(Map<ResourceKey<Level>, Map<BlockPos, String>> map)
    {
        ListTag list = new ListTag();
        for (Map.Entry<ResourceKey<Level>, Map<BlockPos, String>> entry : map.entrySet())
        {
            for (Map.Entry<BlockPos, String> posEntry : entry.getValue().entrySet())
            {
                CompoundTag t = new CompoundTag();
                t.putString("Dim", entry.getKey().location().toString());
                t.putLong("Pos", posEntry.getKey().asLong());
                t.putString("Block", posEntry.getValue());
                list.add(t);
            }
        }
        return list;
    }

    private static void deserializeBound(Map<ResourceKey<Level>, Map<BlockPos, String>> map, ListTag list)
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
            BlockPos pos = BlockPos.of(t.getLong("Pos"));
            String blockId = t.getString("Block");
            map.computeIfAbsent(dim, k -> new HashMap<>()).put(pos, blockId);
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
