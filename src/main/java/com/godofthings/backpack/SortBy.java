package com.godofthings.backpack;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/**
 * 神之背包的整理方式（名称 / 模组 / 数量 / 标签）。
 *
 * <p>{@link #getSerializedName()} 返回的字符串同时是界面语言键
 * {@code gui.godofthings.backpack.sort.<name>} 的后缀，改名会让按钮显示成原始键。</p>
 *
 * <p><b>枚举顺序不可改动</b>：网络同步用的是序号（{@link #STREAM_CODEC}），
 * 只能在末尾追加新档位。</p>
 */
public enum SortBy implements StringRepresentable
{
    NAME("name"),
    MOD("mod"),
    COUNT("count"),
    TAG("tag");

    /** 数据组件持久化用（存档里存字符串，比序号安全：加档位不会让老存档错位） */
    public static final Codec<SortBy> CODEC = StringRepresentable.fromEnum(SortBy::values);

    /** 网络同步用：序号（一个字节） */
    public static final StreamCodec<ByteBuf, SortBy> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> buf.writeByte(value.ordinal()),
            buf -> byOrdinal(buf.readUnsignedByte()));

    private static final SortBy[] VALUES = values();

    private final String name;

    SortBy(String name)
    {
        this.name = name;
    }

    @Override
    public String getSerializedName()
    {
        return this.name;
    }

    /** 循环到下一个：NAME → MOD → COUNT → TAG → NAME（界面「排序方式」按钮） */
    public SortBy next()
    {
        return VALUES[(this.ordinal() + 1) % VALUES.length];
    }

    /** 序号 → 枚举；越界回落到 NAME（网络数据不可信，不抛异常） */
    public static SortBy byOrdinal(int ordinal)
    {
        return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : NAME;
    }
}
