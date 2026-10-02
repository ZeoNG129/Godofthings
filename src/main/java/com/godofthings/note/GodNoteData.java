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
 * 神之便签的全局存储（World SavedData，持久化到存档）：一个玩家一本。
 *
 * <p>与传送点（{@code WaypointData}）同一套做法，挂主世界维度存储下、跨维度共享。
 * 便签属于「玩家个人资料」而不是物品，所以物品只是个入口：把便签丢给别人也不会把内容带走。</p>
 */
public class GodNoteData extends SavedData
{
    private static final String NAME = "godofthings_notes";

    private final Map<UUID, NoteBook> books = new HashMap<>();

    public GodNoteData()
    {
    }

    /** 反序列化构造器（SavedData.Factory 的 BiFunction 入参） */
    public GodNoteData(CompoundTag tag, HolderLookup.Provider provider)
    {
        ListTag list = tag.getList("Books", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++)
        {
            CompoundTag entry = list.getCompound(i);
            if (!entry.hasUUID("Id"))
            {
                continue;
            }
            NoteBook book = new NoteBook();
            book.load(entry);
            books.put(entry.getUUID("Id"), book);
        }
    }

    public static GodNoteData get(MinecraftServer server)
    {
        ServerLevel overworld = server.overworld();
        return overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(GodNoteData::new, GodNoteData::new), NAME);
    }

    /** 取该玩家的便签（没有就新建一份空的，但不立刻标脏） */
    public NoteBook book(UUID id)
    {
        return books.computeIfAbsent(id, k -> new NoteBook());
    }

    /** 整本替换（客户端上传的副本），先兜底再落库；开着自动更名时以服务端当日日期为准 */
    public void put(UUID id, NoteBook book)
    {
        book.clamp();
        book.applyAutoName();
        books.put(id, book);
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider)
    {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, NoteBook> e : books.entrySet())
        {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Id", e.getKey());
            e.getValue().save(entry);
            list.add(entry);
        }
        tag.put("Books", list);
        return tag;
    }
}
