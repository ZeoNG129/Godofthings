package com.godofthings.aalink;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * 持久化自动连接配置（移植自 AALink 的 AALinkSavedData）：
 * 开关状态 + 已桥接设备位置。
 *
 * <p>开关：玩家 {@code /aalink off} 后退出存档重进，开关状态保持关闭。</p>
 *
 * <p>已桥接设备：记录「曾经成功桥接过的设备」位置。区块加载重连时只恢复这些设备，
 * 因此开关关闭期间放置的设备（从未桥接）在重进存档后不会被自动连接。</p>
 */
public class AaLinkSavedData extends SavedData
{
    private static final String NAME = "godofthings_aalink_config";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_LINKED = "linked";

    private boolean enabled = false;
    private final Set<BlockPos> linked = new HashSet<>();

    public static AaLinkSavedData get(ServerLevel level)
    {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(AaLinkSavedData::new, AaLinkSavedData::load), NAME);
    }

    public AaLinkSavedData()
    {
    }

    private static AaLinkSavedData load(CompoundTag tag, HolderLookup.Provider registries)
    {
        AaLinkSavedData data = new AaLinkSavedData();
        data.enabled = tag.getBoolean(KEY_ENABLED);
        ListTag list = tag.getList(KEY_LINKED, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++)
        {
            CompoundTag entry = list.getCompound(i);
            data.linked.add(new BlockPos(entry.getInt("x"), entry.getInt("y"), entry.getInt("z")));
        }
        return data;
    }

    public boolean isEnabled()
    {
        return this.enabled;
    }

    public void setEnabled(boolean value)
    {
        this.enabled = value;
        this.setDirty();
    }

    public boolean isLinked(BlockPos pos)
    {
        return this.linked.contains(pos);
    }

    public void link(BlockPos pos)
    {
        if (this.linked.add(pos))
        {
            this.setDirty();
        }
    }

    /** 无线连接器卡手动断开时移除记录（v7.11.0）。 */
    public void unlink(BlockPos pos)
    {
        if (this.linked.remove(pos))
        {
            this.setDirty();
        }
    }

    /** 返回本维度已桥接设备位置的只读副本（供中枢节点重建后批量重连）。 */
    public Set<BlockPos> getLinkedPositions()
    {
        return new HashSet<>(this.linked);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries)
    {
        tag.putBoolean(KEY_ENABLED, this.enabled);
        ListTag list = new ListTag();
        for (BlockPos pos : this.linked)
        {
            CompoundTag entry = new CompoundTag();
            entry.putInt("x", pos.getX());
            entry.putInt("y", pos.getY());
            entry.putInt("z", pos.getZ());
            list.add(entry);
        }
        tag.put(KEY_LINKED, list);
        return tag;
    }
}
