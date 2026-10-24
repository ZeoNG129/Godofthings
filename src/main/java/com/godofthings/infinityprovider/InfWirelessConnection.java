package com.godofthings.infinityprovider;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * 一条无线连接记录（维度 + 坐标 + 绑定面）。
 * 纯数据载体；面向机器的目标解析与缓存由 {@link InfProviderLogic} 持有
 * （那里才有供应器自身的 IActionSource）。移植自 AE2LT 的 WirelessConnection 裁剪版。
 */
public final class InfWirelessConnection {

    private final ResourceKey<Level> dimension;
    private final BlockPos pos;
    private final Direction boundFace;

    public InfWirelessConnection(ResourceKey<Level> dimension, BlockPos pos, Direction boundFace) {
        this.dimension = dimension;
        this.pos = pos;
        this.boundFace = boundFace;
    }

    public ResourceKey<Level> dimension() {
        return this.dimension;
    }

    public BlockPos pos() {
        return this.pos;
    }

    public Direction boundFace() {
        return this.boundFace;
    }

    /**
     * 派发 / 回收时传给机器的 capability 面。
     * 照抄上游 ProviderTarget.resolveStorageTarget：直接用 boundFace，
     * 不取反——boundFace 记录的就是「点中的那一面」，AE2 的
     * PatternProviderTarget.get 也按这一面去查目标机器能力。
     */
    public Direction machineSide() {
        return this.boundFace;
    }

    public boolean isAlive(ServerLevel level) {
        return level.dimension() == this.dimension && level.isLoaded(this.pos);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof InfWirelessConnection other)) {
            return false;
        }
        return this.dimension.equals(other.dimension)
                && this.pos.equals(other.pos)
                && this.boundFace == other.boundFace;
    }

    @Override
    public int hashCode() {
        int result = this.dimension.hashCode();
        result = 31 * result + this.pos.hashCode();
        result = 31 * result + this.boundFace.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "[" + this.dimension.location() + " " + this.pos.toShortString() + " " + this.boundFace + "]";
    }

    // ---- NBT ----

    private static final String TAG_DIM = "Dim";
    private static final String TAG_POS = "Pos";
    private static final String TAG_FACE = "Face";

    public CompoundTag toTag() {
        var tag = new CompoundTag();
        tag.putString(TAG_DIM, this.dimension.location().toString());
        tag.putLong(TAG_POS, this.pos.asLong());
        tag.putInt(TAG_FACE, this.boundFace.get3DDataValue());
        return tag;
    }

    public static InfWirelessConnection fromTag(CompoundTag tag) {
        var dim = ResourceKey.create(Registries.DIMENSION,
                ResourceLocation.parse(tag.getString(TAG_DIM)));
        var pos = BlockPos.of(tag.getLong(TAG_POS));
        var face = Direction.from3DDataValue(tag.getInt(TAG_FACE));
        return new InfWirelessConnection(dim, pos, face);
    }

    /** 按（维度, 坐标）查找；同一台机器只保留一条连接，绑面可更新。 */
    public static int indexOf(List<InfWirelessConnection> list, ResourceKey<Level> dim, BlockPos pos) {
        for (int i = 0; i < list.size(); i++) {
            var conn = list.get(i);
            if (conn.dimension.equals(dim) && conn.pos.equals(pos)) {
                return i;
            }
        }
        return -1;
    }

    public static List<InfWirelessConnection> readTagList(CompoundTag tag, String key, int max) {
        var result = new ArrayList<InfWirelessConnection>();
        var list = tag.getList(key, net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size() && result.size() < max; i++) {
            result.add(fromTag(list.getCompound(i)));
        }
        return result;
    }

    public static void writeTagList(CompoundTag tag, String key, List<InfWirelessConnection> connections) {
        var list = new net.minecraft.nbt.ListTag();
        for (var conn : connections) {
            list.add(conn.toTag());
        }
        tag.put(key, list);
    }
}
