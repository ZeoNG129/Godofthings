package com.godofthings.backpack;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 神之背包的内容（数据组件 {@code godofthings:god_backpack_contents}）。
 *
 * <p>永远 {@link GodBackpackItem#SIZE} 项、空格用 {@link ItemStack#EMPTY}；构造时自动补齐 / 截断，
 * 所以 {@link #items()} 一定是长度 120 的只读列表，调用方不需要再判空。</p>
 *
 * <p><b>序列化</b>：NBT 用 {@link ItemStack#OPTIONAL_CODEC}（空格编码成空 map，而不是 {@code {id:"minecraft:air"}}），
 * 网络用 {@link ItemStack#OPTIONAL_LIST_STREAM_CODEC}（带上 registry 上下文，附魔 / 药水这类组件才编得动）。</p>
 */
public record GodBackpackContents(List<ItemStack> items)
{
    /** 存档 / 网络同步的编解码器（物品数据组件用） */
    public static final Codec<GodBackpackContents> CODEC =
            ItemStack.OPTIONAL_CODEC.listOf().xmap(GodBackpackContents::new, GodBackpackContents::items);

    public static final StreamCodec<RegistryFriendlyByteBuf, GodBackpackContents> STREAM_CODEC =
            ItemStack.OPTIONAL_LIST_STREAM_CODEC.map(GodBackpackContents::new, GodBackpackContents::items);

    /** 共享的空背包（记录不可变，可以安全复用，省掉每次读空格都新建 120 项列表） */
    private static final GodBackpackContents EMPTY = new GodBackpackContents(List.of());

    public GodBackpackContents
    {
        items = normalize(items);
    }

    /** 补齐到 120 项 / 截断多余项 / 把 null 换成空堆叠，并包成只读列表 */
    private static List<ItemStack> normalize(List<ItemStack> raw)
    {
        List<ItemStack> list = new ArrayList<>(GodBackpackItem.SIZE);
        for (int i = 0; i < GodBackpackItem.SIZE; i++)
        {
            ItemStack stack = raw != null && i < raw.size() ? raw.get(i) : null;
            list.add(stack == null ? ItemStack.EMPTY : stack);
        }
        return Collections.unmodifiableList(list);
    }

    public static GodBackpackContents empty()
    {
        return EMPTY;
    }

    /** 取第 index 格（越界返回空堆叠，不抛异常） */
    public ItemStack get(int index)
    {
        return index >= 0 && index < GodBackpackItem.SIZE ? items.get(index) : ItemStack.EMPTY;
    }

    /** 替换第 index 格并返回新内容（记录不可变，原对象不动；越界返回自身） */
    public GodBackpackContents with(int index, ItemStack stack)
    {
        if (index < 0 || index >= GodBackpackItem.SIZE)
        {
            return this;
        }
        List<ItemStack> copy = new ArrayList<>(items);
        copy.set(index, stack == null ? ItemStack.EMPTY : stack);
        return new GodBackpackContents(copy);
    }

    /** 是不是一格都没放（转移 / 整理前的空判） */
    public boolean isEmpty()
    {
        for (ItemStack stack : items)
        {
            if (!stack.isEmpty())
            {
                return false;
            }
        }
        return true;
    }

    /** 非空格数量（测试与调试用） */
    public int countNonEmpty()
    {
        int count = 0;
        for (ItemStack stack : items)
        {
            if (!stack.isEmpty())
            {
                count++;
            }
        }
        return count;
    }
}
