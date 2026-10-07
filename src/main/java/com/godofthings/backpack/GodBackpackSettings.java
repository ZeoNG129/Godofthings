package com.godofthings.backpack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 神之背包的设置（数据组件 {@code godofthings:god_backpack_settings}）：记忆格 / 忽略整理格 /
 * 排序方式 / 两个「忽略」开关 / 搜索词 / 保留搜索词。
 *
 * <p>构造时统一兜底：槽位下标越界丢掉、空堆叠不算记忆格、字段 null 回落默认值，
 * 所以记录里的集合永远是只读且干净的。</p>
 *
 * <p><b>记忆格与忽略整理格互斥</b>：一个格子要么「记住某个物品」，要么「整理时原地不动」。
 * {@link #withMemory(int, ItemStack)} 会把该格从忽略整理集合里摘掉，
 * {@link #toggleNoSort(int)} 打开时会把该格从记忆集合里摘掉 —— 与界面上「忽略整理格不作为记忆格」一致。</p>
 *
 * <p><b>注意 equals</b>：1.21.1 的 {@link ItemStack} 没有重写 {@code equals}（是引用比较），
 * 记忆格里的堆叠一编解码就是新对象，所以本记录的 {@code equals} 判内容会假阴性；
 * 要比两份设置，逐字段比、堆叠用 {@link ItemStack#matches}。</p>
 */
public record GodBackpackSettings(
        Map<Integer, ItemStack> memory,
        Set<Integer> noSort,
        SortBy sortBy,
        boolean ignoreDurability,
        boolean ignoreNbt,
        String searchPhrase,
        boolean keepSearch)
{
    /** 搜索词长度上限（与界面输入框一致，服务端再挡一道，防止客户端塞超长字符串进物品组件） */
    public static final int MAX_SEARCH_LENGTH = 50;

    /**
     * 记忆格的 NBT 条目。
     *
     * <p>用「槽位 + 物品」的列表而不是 map：NBT 的 map 键只能是字符串，非字符串键的 map 编解码
     * 在不同 ops 下行为不一致，列表最稳（键顺序按槽位排好，存档也是确定的）。</p>
     */
    private record MemoryEntry(int slot, ItemStack stack)
    {
        static final Codec<MemoryEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("slot").forGetter(MemoryEntry::slot),
                ItemStack.OPTIONAL_CODEC.fieldOf("item").forGetter(MemoryEntry::stack)
        ).apply(instance, MemoryEntry::new));
    }

    public static final Codec<GodBackpackSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            MemoryEntry.CODEC.listOf().optionalFieldOf("memory", List.of())
                    .forGetter(GodBackpackSettings::memoryEntries),
            Codec.INT.listOf().optionalFieldOf("no_sort", List.of())
                    .forGetter(GodBackpackSettings::noSortList),
            SortBy.CODEC.optionalFieldOf("sort_by", SortBy.NAME).forGetter(GodBackpackSettings::sortBy),
            Codec.BOOL.optionalFieldOf("ignore_durability", false).forGetter(GodBackpackSettings::ignoreDurability),
            Codec.BOOL.optionalFieldOf("ignore_nbt", false).forGetter(GodBackpackSettings::ignoreNbt),
            Codec.STRING.optionalFieldOf("search", "").forGetter(GodBackpackSettings::searchPhrase),
            Codec.BOOL.optionalFieldOf("keep_search", false).forGetter(GodBackpackSettings::keepSearch)
    ).apply(instance, GodBackpackSettings::fromParts));

    /** 网络同步用：手写编解码（设置本身不大，一个包发全量，客户端不做增量合并） */
    public static final StreamCodec<RegistryFriendlyByteBuf, GodBackpackSettings> STREAM_CODEC = StreamCodec.of(
            (buf, value) ->
            {
                List<MemoryEntry> entries = value.memoryEntries();
                buf.writeVarInt(entries.size());
                for (MemoryEntry entry : entries)
                {
                    buf.writeVarInt(entry.slot());
                    ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, entry.stack());
                }
                List<Integer> noSort = value.noSortList();
                buf.writeVarInt(noSort.size());
                for (int slot : noSort)
                {
                    buf.writeVarInt(slot);
                }
                SortBy.STREAM_CODEC.encode(buf, value.sortBy());
                buf.writeBoolean(value.ignoreDurability());
                buf.writeBoolean(value.ignoreNbt());
                buf.writeUtf(value.searchPhrase());
                buf.writeBoolean(value.keepSearch());
            },
            buf ->
            {
                int memoryCount = buf.readVarInt();
                Map<Integer, ItemStack> memory = new LinkedHashMap<>();
                for (int i = 0; i < memoryCount; i++)
                {
                    int slot = buf.readVarInt();
                    memory.put(slot, ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
                }
                int noSortCount = buf.readVarInt();
                Set<Integer> noSort = new LinkedHashSet<>();
                for (int i = 0; i < noSortCount; i++)
                {
                    noSort.add(buf.readVarInt());
                }
                return new GodBackpackSettings(memory, noSort,
                        SortBy.STREAM_CODEC.decode(buf),
                        buf.readBoolean(), buf.readBoolean(), buf.readUtf(), buf.readBoolean());
            });

    public GodBackpackSettings
    {
        Map<Integer, ItemStack> cleanMemory = new LinkedHashMap<>();
        if (memory != null)
        {
            memory.entrySet().stream()
                    .filter(entry -> entry.getKey() != null && valid(entry.getKey()))
                    .filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty())
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> cleanMemory.put(entry.getKey(), entry.getValue()));
        }
        memory = Collections.unmodifiableMap(cleanMemory);

        Set<Integer> cleanNoSort = new LinkedHashSet<>();
        if (noSort != null)
        {
            for (Integer slot : noSort)
            {
                if (slot != null && valid(slot))
                {
                    cleanNoSort.add(slot);
                }
            }
        }
        noSort = Collections.unmodifiableSet(cleanNoSort);

        sortBy = sortBy == null ? SortBy.NAME : sortBy;
        searchPhrase = searchPhrase == null ? "" : searchPhrase;
    }

    /** 槽位下标是否落在 0..119 */
    private static boolean valid(int slot)
    {
        return slot >= 0 && slot < GodBackpackItem.SIZE;
    }

    /** 默认设置：不记忆、不忽略、按名称、三个开关都关、无搜索词 */
    public static GodBackpackSettings defaults()
    {
        return new GodBackpackSettings(Map.of(), Set.of(), SortBy.NAME, false, false, "", false);
    }

    /** 存档 codec 的组装回调：列表 → 记录 */
    private static GodBackpackSettings fromParts(List<MemoryEntry> memory, List<Integer> noSort, SortBy sortBy,
                                                 boolean ignoreDurability, boolean ignoreNbt,
                                                 String searchPhrase, boolean keepSearch)
    {
        Map<Integer, ItemStack> map = new LinkedHashMap<>();
        for (MemoryEntry entry : memory)
        {
            map.put(entry.slot(), entry.stack());
        }
        return new GodBackpackSettings(map, new LinkedHashSet<>(noSort), sortBy,
                ignoreDurability, ignoreNbt, searchPhrase, keepSearch);
    }

    /** 存档用：按槽位排好序的记忆条目 */
    private List<MemoryEntry> memoryEntries()
    {
        List<MemoryEntry> list = new ArrayList<>(memory.size());
        for (Map.Entry<Integer, ItemStack> entry : memory.entrySet())
        {
            list.add(new MemoryEntry(entry.getKey(), entry.getValue()));
        }
        return list;
    }

    /** 存档 / 网络用：按槽位排好序的忽略整理槽位（HashSet 的迭代顺序不稳定，落盘要确定） */
    private List<Integer> noSortList()
    {
        List<Integer> list = new ArrayList<>(noSort);
        Collections.sort(list);
        return list;
    }

    /** 该格是不是记忆格 */
    public boolean isMemory(int slot)
    {
        return memory.containsKey(slot);
    }

    /** 该格是不是忽略整理格 */
    public boolean isNoSort(int slot)
    {
        return noSort.contains(slot);
    }

    /** 该格记住的模板物品（不是记忆格就是空堆叠） */
    public ItemStack memoryTemplate(int slot)
    {
        ItemStack template = memory.get(slot);
        return template == null ? ItemStack.EMPTY : template;
    }

    /** 记住 / 取消记住某格（传空堆叠 = 取消记忆；记住时会摘掉该格的忽略整理标记） */
    public GodBackpackSettings withMemory(int slot, ItemStack remembered)
    {
        if (!valid(slot))
        {
            return this;
        }
        Map<Integer, ItemStack> map = new LinkedHashMap<>(memory);
        Set<Integer> noSortCopy = new LinkedHashSet<>(noSort);
        if (remembered == null || remembered.isEmpty())
        {
            map.remove(slot);
        }
        else
        {
            // 只记「一个」，影子渲染与匹配判定都不关心数量
            map.put(slot, remembered.copyWithCount(1));
            noSortCopy.remove(slot);
        }
        return new GodBackpackSettings(map, noSortCopy, sortBy, ignoreDurability, ignoreNbt, searchPhrase, keepSearch);
    }

    /** 切换某格「整理时原地不动」（打开时摘掉该格的记忆标记） */
    public GodBackpackSettings toggleNoSort(int slot)
    {
        if (!valid(slot))
        {
            return this;
        }
        Set<Integer> set = new LinkedHashSet<>(noSort);
        Map<Integer, ItemStack> map = new LinkedHashMap<>(memory);
        if (!set.remove(slot))
        {
            set.add(slot);
            map.remove(slot);
        }
        return new GodBackpackSettings(map, set, sortBy, ignoreDurability, ignoreNbt, searchPhrase, keepSearch);
    }

    public GodBackpackSettings withSortBy(SortBy sortBy)
    {
        return new GodBackpackSettings(memory, noSort, sortBy, ignoreDurability, ignoreNbt, searchPhrase, keepSearch);
    }

    public GodBackpackSettings withSearch(String phrase)
    {
        String text = phrase == null ? "" : phrase;
        if (text.length() > MAX_SEARCH_LENGTH)
        {
            text = text.substring(0, MAX_SEARCH_LENGTH);
        }
        return new GodBackpackSettings(memory, noSort, sortBy, ignoreDurability, ignoreNbt, text, keepSearch);
    }

    public GodBackpackSettings toggledIgnoreDurability()
    {
        return new GodBackpackSettings(memory, noSort, sortBy, !ignoreDurability, ignoreNbt, searchPhrase, keepSearch);
    }

    public GodBackpackSettings toggledIgnoreNbt()
    {
        return new GodBackpackSettings(memory, noSort, sortBy, ignoreDurability, !ignoreNbt, searchPhrase, keepSearch);
    }

    public GodBackpackSettings toggledKeepSearch()
    {
        return new GodBackpackSettings(memory, noSort, sortBy, ignoreDurability, ignoreNbt, searchPhrase, !keepSearch);
    }
}
