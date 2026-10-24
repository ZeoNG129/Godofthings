package com.godofthings.wondrous;

import com.godofthings.Godofthings;

/**
 * 上游 UselessStretcherMod 的本地替身（[本地改动]）。
 * <p>
 * 上游杖模组是独立 mod，转换后的代码通过 {@code UselessStretcherMod.MODID} 取命名空间。
 * 本模组所有注册都在 {@code godofthings} 命名空间下，这里只保留 MODID 常量并指向主模组，
 * 不再注册任何内容（注册统一走 {@code WondrousItems/WondrousEntities/StaffComponents}）。
 */
public final class UselessStretcherMod
{
    public static final String MODID = Godofthings.MODID;

    private UselessStretcherMod()
    {
    }
}
