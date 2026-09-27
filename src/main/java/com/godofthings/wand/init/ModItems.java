package com.godofthings.wand.init;

/**
 * 垫片：替代万象担架的物品注册表 {@code ModItems}。
 * <p>照抄的代码通过 {@code ModItems.WONDROUS_STAFF.get()} 之类取物品 —— 这里把这些字段
 * <b>全部指向本模组的「神之工具」</b>，于是抄来的逻辑作用对象正是神之工具本身。
 */
public final class ModItems
{
    /** 原 mod 的 OMNIVERSAL_MYRIAD，这里统一指向本模组的神之工具 */
    public static final java.util.function.Supplier<net.minecraft.world.item.Item> OMNIVERSAL_MYRIAD = () -> com.godofthings.Godofthings.GOD_FAVOR_WAND.get();

    /** 原 mod 的 RANGE_RECLAIMER，这里统一指向本模组的神之工具 */
    public static final java.util.function.Supplier<net.minecraft.world.item.Item> RANGE_RECLAIMER = () -> com.godofthings.Godofthings.GOD_FAVOR_WAND.get();

    /** 原 mod 的 USELESS_STRETCHER，这里统一指向本模组的神之工具 */
    public static final java.util.function.Supplier<net.minecraft.world.item.Item> USELESS_STRETCHER = () -> com.godofthings.Godofthings.GOD_FAVOR_WAND.get();

    /** 原 mod 的 WONDROUS_STAFF，这里统一指向本模组的神之工具 */
    public static final java.util.function.Supplier<net.minecraft.world.item.Item> WONDROUS_STAFF = () -> com.godofthings.Godofthings.GOD_FAVOR_WAND.get();

    private ModItems() {}
}
