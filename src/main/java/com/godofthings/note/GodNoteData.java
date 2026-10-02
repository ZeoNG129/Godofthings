package com.godofthings.note;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 神之便签的全局存储（World SavedData，持久化到存档）：一个玩家一册（可含多本）。
 *
 * <p>与传送点（{@code WaypointData}）同一套做法，挂主世界维度存储下、跨维度共享。</p>
 *
 * <p><b>旧存档兼容</b>：v5.3.0 及以前每个玩家只有一本，NBT 顶层是 {@code Books:[{Id,Tasks,...}]}；
 * 读取时若没有 {@code Shelves} 标签，就把每个旧条目的内容当成「一册里的一本」（旧条目的 Hud 也一并接上）。</p>
 */
public class GodNoteData extends SavedData
{
    private static final String NAME = "godofthings_notes";

    private final Map<UUID, NoteShelf> shelves = new HashMap<>();

    public GodNoteData()
    {
    }

    /** 反序列化构造器（SavedData.Factory 的 BiFunction 入参） */
    public GodNoteData(CompoundTag tag, HolderLookup.Provider provider)
    {
        if (tag.contains("Shelves", Tag.TAG_LIST))
        {
            ListTag list = tag.getList("Shelves", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++)
            {
                CompoundTag entry = list.getCompound(i);
                if (!entry.hasUUID("Id"))
                {
                    continue;
                }
                NoteShelf shelf = new NoteShelf();
                shelf.load(entry);
                shelves.put(entry.getUUID("Id"), shelf);
            }
            return;
        }
        // v5.3.0 及以前的单本格式
        ListTag legacy = tag.getList("Books", Tag.TAG_COMPOUND);
        for (int i = 0; i < legacy.size(); i++)
        {
            CompoundTag entry = legacy.getCompound(i);
            if (!entry.hasUUID("Id"))
            {
                continue;
            }
            NoteShelf shelf = new NoteShelf();
            shelf.load(entry);
            shelves.put(entry.getUUID("Id"), shelf);
        }
    }

    public static GodNoteData get(MinecraftServer server)
    {
        ServerLevel overworld = server.overworld();
        return overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(GodNoteData::new, GodNoteData::new), NAME);
    }

    /** 取该玩家的便签册（没有就新建一册，但不立刻标脏） */
    public NoteShelf shelf(UUID id)
    {
        return shelves.computeIfAbsent(id, k -> new NoteShelf());
    }

    /** 整册替换（客户端上传的副本），先兜底再落库；开着自动更名的那几本以服务端当日日期为准 */
    public void put(UUID id, NoteShelf shelf)
    {
        shelf.clamp();
        shelf.applyAutoName();
        shelves.put(id, shelf);
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider)
    {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, NoteShelf> e : shelves.entrySet())
        {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Id", e.getKey());
            e.getValue().save(entry);
            list.add(entry);
        }
        tag.put("Shelves", list);
        return tag;
    }
}
