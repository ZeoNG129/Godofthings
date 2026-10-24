package com.godofthings.advancedmemorycard;

import com.godofthings.Godofthings;

/**
 * 高级内存卡（移植自 suntide_20210418 的 Advanced Memory Card）的常量入口。
 *
 * <p>上游是独立模组，这里并入 godofthings：MOD_ID 直接指向本模组，
 * 注册表由 {@link AmcRegistry} / {@link menu.AmcMenus} 持有并在
 * {@link Godofthings} 主类统一注册。</p>
 */
public final class AdvancedMemoryCardMod {

    /** 并入后所有资源都落在 godofthings 命名空间下。 */
    public static final String MOD_ID = Godofthings.MODID;

    private AdvancedMemoryCardMod() {
    }
}
