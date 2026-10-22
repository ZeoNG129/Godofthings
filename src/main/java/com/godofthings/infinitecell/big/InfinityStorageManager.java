package com.godofthings.infinitecell.big;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * 所有「吞噬万籁的寂静」磁盘内容的存档容器（{@code world/data/infinity_biginteger_cells.dat}）。
 *
 * <p>移植自 ExtendedAE+ 1.6.3 的 {@code com.extendedae_plus.util.storage.InfinityStorageManager}
 * （作者 glodblock 等，LGPL-3.0；上游自述参考了 AE2Things，MIT，版权归 Technici4n）。</p>
 *
 * <p><b>为什么数据不放在物品上</b>：单个 key 的数量可以超过 {@code long}，而 ItemStack 的
 * NBT 会被大量复制/比较（AE2 网络每次规划都会复制），把大数塞进物品会拖垮性能；
 * 所以物品只带一个 UUID，真实内容存在这里，按 UUID 索引。</p>
 */
public class InfinityStorageManager extends SavedData
{
    private static final Factory<InfinityStorageManager> FACTORY =
            new Factory<>(InfinityStorageManager::new, InfinityStorageManager::readNbt);

    /** UUID → 该磁盘的数据 */
    private final Object2ObjectMap<UUID, InfinityDataStorage> cells;
    /** 只在磁盘条目被创建 / 替换 / 删除时变化，供库存实例安全缓存数据对象 */
    private long storageRevision;
    @Nullable
    private WeakReference<HolderLookup.Provider> registries;

    public InfinityStorageManager()
    {
        this.cells = new Object2ObjectOpenHashMap<>();
        this.setDirty();
    }

    private InfinityStorageManager(Object2ObjectMap<UUID, InfinityDataStorage> cells)
    {
        this.cells = cells;
        this.setDirty();
    }

    /** 从 NBT 反序列化 */
    public static InfinityStorageManager readNbt(CompoundTag nbt, HolderLookup.Provider registries)
    {
        // 缺省视为 1（兼容旧档）
        int version = nbt.contains(InfinityConstants.FORMAT_VERSION_FIELD)
                ? nbt.getInt(InfinityConstants.FORMAT_VERSION_FIELD)
                : 1;

        ListTag cellList = nbt.getList(InfinityConstants.INFINITY_CELL_LIST, CompoundTag.TAG_COMPOUND);
        Object2ObjectMap<UUID, InfinityDataStorage> cells =
                new Object2ObjectOpenHashMap<>(Math.max(2, cellList.size()));
        for (int i = 0; i < cellList.size(); i++)
        {
            CompoundTag cell = cellList.getCompound(i);
            cells.put(
                    cell.getUUID(InfinityConstants.INFINITY_CELL_UUID),
                    InfinityDataStorage.loadFromNBT(
                            cell.getCompound(InfinityConstants.INFINITY_CELL_DATA), registries));
        }
        return new InfinityStorageManager(cells);
    }

    @Override
    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider provider)
    {
        ListTag cellList = new ListTag();
        for (var entry : Object2ObjectMaps.fastIterable(cells))
        {
            CompoundTag cell = new CompoundTag();
            cell.putUUID(InfinityConstants.INFINITY_CELL_UUID, entry.getKey());
            cell.put(InfinityConstants.INFINITY_CELL_DATA, entry.getValue().serializeNBT(provider));
            cellList.add(cell);
        }
        nbt.put(InfinityConstants.INFINITY_CELL_LIST, cellList);
        nbt.putInt(InfinityConstants.FORMAT_VERSION_FIELD, InfinityConstants.FORMAT_VERSION);
        return nbt;
    }

    /** 已加载的所有磁盘 UUID（调试 / 恢复指令用） */
    public Set<UUID> getAllLoadedUUIDs()
    {
        return Collections.unmodifiableSet(cells.keySet());
    }

    /** 更新或添加某个 UUID 的数据并标脏 */
    public void updateCell(UUID uuid, InfinityDataStorage storage)
    {
        InfinityDataStorage previous = cells.put(uuid, storage);
        if (previous != storage)
        {
            storageRevision++;
        }
        setDirty();
    }

    /** 删除某个 UUID 的记录并标脏 */
    public void removeCell(UUID uuid)
    {
        if (cells.remove(uuid) != null)
        {
            storageRevision++;
            setDirty();
        }
    }

    public boolean hasUUID(UUID uuid)
    {
        return cells.containsKey(uuid);
    }

    @Nullable
    public InfinityDataStorage getCell(UUID uuid)
    {
        return cells.get(uuid);
    }

    public long getStorageRevision()
    {
        return storageRevision;
    }

    /** 获取或创建某个 UUID 的数据容器 */
    public InfinityDataStorage getOrCreateCell(UUID uuid)
    {
        InfinityDataStorage cell = cells.get(uuid);
        if (cell == null)
        {
            cell = new InfinityDataStorage();
            cells.put(uuid, cell);
            storageRevision++;
            setDirty();
        }
        return cell;
    }

    /** 取当前服务器的实例（主世界的 SavedData） */
    public static InfinityStorageManager getInstance(MinecraftServer server)
    {
        ServerLevel world = server.getLevel(ServerLevel.OVERWORLD);
        var manager = world.getDataStorage().computeIfAbsent(FACTORY, InfinityConstants.SAVE_FILE_NAME);
        manager.registries = new WeakReference<>(server.registryAccess());
        return manager;
    }

    public HolderLookup.Provider getRegistries()
    {
        var r = this.registries;
        if (r == null)
        {
            throw new IllegalStateException("StorageManager was not initialized properly.");
        }

        var registries = r.get();
        if (registries == null)
        {
            throw new IllegalStateException("Using a StorageManager whose server was already closed");
        }

        return registries;
    }
}
