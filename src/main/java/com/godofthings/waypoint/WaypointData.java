package com.godofthings.waypoint;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 传送点全局存储（World SavedData，持久化到存档）。
 * 挂载在主世界（overworld）的 DimensionDataStorage 下，跨维度共享同一份点位表。
 */
public class WaypointData extends SavedData
{
    private static final String NAME = "godofthings_waypoints";

    /** 名称 → 点位（LinkedHashMap 保插入顺序） */
    private final Map<String, Waypoint> waypoints = new LinkedHashMap<>();

    public WaypointData()
    {
    }

    /** 反序列化构造器（SavedData.Factory 的 BiFunction 入参） */
    public WaypointData(CompoundTag tag, HolderLookup.Provider provider)
    {
        ListTag list = tag.getList("Waypoints", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++)
        {
            Waypoint wp = Waypoint.load(list.getCompound(i));
            waypoints.put(wp.name, wp);
        }
    }

    public static WaypointData get(MinecraftServer server)
    {
        ServerLevel overworld = server.overworld();
        return overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(WaypointData::new, WaypointData::new), NAME);
    }

    public void set(Waypoint wp)
    {
        waypoints.put(wp.name, wp);
        setDirty();
    }

    public Waypoint get(String name)
    {
        return waypoints.get(name);
    }

    public void remove(String name)
    {
        if (waypoints.remove(name) != null)
        {
            setDirty();
        }
    }

    public void togglePin(String name)
    {
        Waypoint wp = waypoints.get(name);
        if (wp != null)
        {
            wp.pinned = !wp.pinned;
            setDirty();
        }
    }

    /** 置顶优先，其余保持插入顺序 */
    public List<Waypoint> list()
    {
        List<Waypoint> result = new ArrayList<>(waypoints.values());
        result.sort(Comparator.comparing((Waypoint w) -> !w.pinned));
        return result;
    }

    /** 跨维度传送（含恢复面对方向），失败返回 false */
    public static boolean teleport(ServerPlayer player, Waypoint wp)
    {
        ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(wp.dimension));
        ServerLevel target = player.server.getLevel(dimKey);
        if (target == null)
        {
            return false;
        }
        player.teleportTo(target, wp.x, wp.y, wp.z, wp.yaw, wp.pitch);
        return true;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider)
    {
        ListTag list = new ListTag();
        for (Waypoint wp : waypoints.values())
        {
            list.add(wp.save(new CompoundTag()));
        }
        tag.put("Waypoints", list);
        return tag;
    }
}
