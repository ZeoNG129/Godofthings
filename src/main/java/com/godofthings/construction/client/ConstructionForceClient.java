package com.godofthings.construction.client;

import net.minecraft.client.gui.screens.Screen;

/**
 * 神之建筑的客户端侧输入判定：<b>只放客户端类</b>，避免把 {@link Screen} 拖进服务端会加载的
 * {@code GodConstructionItem} 里。
 *
 * <p>为什么要单独一个类：服务端<b>没有</b> Ctrl 状态（原版只同步 Shift），Ctrl 只能在客户端问，
 * 所以 {@code GodConstructionItem#onItemUseFirst} 的 {@code level.isClientSide} 分支会调这里，
 * 拿到结果后再发 {@code ConstructionForcePayload}。</p>
 *
 * <p><b>注意：这里绝对不能加 {@code @EventBusSubscriber}。</b>本类没有任何
 * {@code @SubscribeEvent} 方法，而 NeoForge 在构造阶段扫到这种「标了订阅注解却没有订阅方法」的类会直接
 * 抛 {@code IllegalArgumentException} 让整个模组加载失败（v5.22.1 在客户端闪过一次，服务端 GameTest
 * 因为 {@code Dist.CLIENT} 被跳过所以没暴露）。客户端类隔离靠「调用点被 {@code level.isClientSide}
 * 挡着 + 本类不被任何服务端路径引用」就够了。</p>
 */
public final class ConstructionForceClient
{
    private ConstructionForceClient()
    {
    }

    /**
     * 强制施工的修饰键（Ctrl）是否按下。
     *
     * <p>只有按住 Ctrl 时，右键「会开界面的方块」才会被当成施工而不是开界面。</p>
     */
    public static boolean isForceModifierDown()
    {
        return Screen.hasControlDown();
    }
}
