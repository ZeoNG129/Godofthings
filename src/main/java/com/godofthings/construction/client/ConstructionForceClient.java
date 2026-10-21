package com.godofthings.construction.client;

import com.godofthings.Godofthings;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * 神之建筑的客户端侧输入判定：<b>只放客户端类</b>，避免把 {@link Screen} 拖进服务端会加载的
 * {@code GodConstructionItem} 里。
 *
 * <p>为什么要单独一个类：服务端<b>没有</b> Ctrl 状态（原版只同步 Shift），Ctrl 只能在客户端问，
 * 所以 {@code GodConstructionItem#onItemUseFirst} 的 {@code level.isClientSide} 分支会调这里，
 * 拿到结果后再发 {@code ConstructionForcePayload}。</p>
 *
 * <p>本类用 {@code @EventBusSubscriber(value = Dist.CLIENT)} 标出来：NeoForge 是用 ASM 读注解元数据
 * 来扫订阅者的，专用服务器上不会加载这个类；调用点又被 {@code level.isClientSide} 挡着，
 * 双重保险（与同包的 {@code ConstructionPreviewRenderer} 是同一套做法）。</p>
 */
@EventBusSubscriber(modid = Godofthings.MODID, value = Dist.CLIENT)
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
