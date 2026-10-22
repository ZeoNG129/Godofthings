package com.godofthings.infinitecell.big;

import javax.annotation.Nullable;

import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import com.godofthings.Godofthings;

/**
 * 持有当前服务器的 {@link InfinityStorageManager}（「吞噬万籁的寂静」的存档容器）。
 *
 * <p>上游 ExtendedAE+ 把这份状态放在它自己的 {@code @Mod} 主类的两个静态字段里，
 * 用 {@code ServerStartedEvent} / {@code ServerStoppingEvent} 维护。本模组不建第二个
 * {@code @Mod}，所以单独抽成这个类，逻辑一一对应。</p>
 *
 * <p>注意：AE2 的 {@code ICellHandler} 是全局单例，取库存时拿不到 ServerLevel，
 * 只能靠这里的静态引用拿到存档；服务器没开时返回 {@code null}，
 * {@link InfinityCellInventory} 会把所有操作安全地降级为「什么都不做」。</p>
 */
@EventBusSubscriber(modid = Godofthings.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class InfinityCellStorage
{
    @Nullable
    private static InfinityStorageManager storageManager;
    @Nullable
    private static MinecraftServer ownerServer;

    private InfinityCellStorage()
    {
    }

    /** 当前服务器的存档容器（服务器未开时为 null） */
    @Nullable
    public static InfinityStorageManager get()
    {
        return storageManager;
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event)
    {
        ownerServer = event.getServer();
        storageManager = InfinityStorageManager.getInstance(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event)
    {
        // 只有当前服务器自己停下来才清空，避免「先开 A 再开 B、B 停时把 A 的引用清掉」
        if (ownerServer == event.getServer())
        {
            ownerServer = null;
            storageManager = null;
        }
    }
}
