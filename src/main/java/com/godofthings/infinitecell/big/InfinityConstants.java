package com.godofthings.infinitecell.big;

/**
 * 「吞噬万籁的寂静」的存档字段名与格式版本常量。
 *
 * <p>移植自 ExtendedAE+ 1.6.3 的 {@code com.extendedae_plus.util.storage.InfinityConstants}
 * （作者 glodblock 等，LGPL-3.0）。字段名<b>刻意保持与上游一致</b>：这样同一份
 * {@code world/data/infinity_biginteger_cells.dat} 在两边可以互相读取，
 * 万一玩家从 ExtendedAE+ 换过来（或换回去）不会丢数据。</p>
 */
public interface InfinityConstants
{
    /** 当前磁盘数据格式版本号 */
    int FORMAT_VERSION = 2;
    /** 格式版本字段名 */
    String FORMAT_VERSION_FIELD = "infinity_format_version";

    /** SavedData 文件名（{@code world/data/<这个名字>.dat}） */
    String SAVE_FILE_NAME = "infinity_biginteger_cells";

    /** 磁盘唯一标识符（同时写在 ItemStack 与 StorageManager 里） */
    String INFINITY_CELL_UUID = "infinity_cell_uuid";
    /** 单个磁盘的数据块键名 */
    String INFINITY_CELL_DATA = "infinity_cell_data";
    /** 所有磁盘数据的列表键名 */
    String INFINITY_CELL_LIST = "infinity_cell_list";

    /** 磁盘内所有 key 的列表（ListTag of CompoundTag） */
    String INFINITY_CELL_KEYS = "infinity_cell_keys";
    /** 磁盘内每种 key 的数量（ListTag of CompoundTag，含 "value" 字节数组） */
    String INFINITY_CELL_AMOUNTS = "infinity_cell_amounts";
    /** 磁盘内物品总数（ListTag，含一个 "value"） */
    String INFINITY_CELL_ITEM_COUNT = "infinity_cell_item_count";

    /** ItemStack 上记录的总数量键名（用于 tooltip 显示） */
    String INFINITY_ITEM_TOTAL = "infinity_item_total";
    /** ItemStack 上记录的种类数键名 */
    String INFINITY_ITEM_TYPES = "infinity_item_types";
}
