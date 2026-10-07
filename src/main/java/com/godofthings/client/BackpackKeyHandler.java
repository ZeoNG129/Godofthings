package com.godofthings.client;

import com.godofthings.Godofthings;
import com.godofthings.network.GodBackpackOpenPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 神之背包快捷键（B）：客户端只发一个空包，找背包 / 开界面都由服务端决定
 * （背包可能在 Curios 背部槽、主物品栏或副手，客户端不猜位置）。
 *
 * <p>写法与 {@link ArmorKeyHandler} 一致：{@code ClientTickEvent.Post} + {@code consumeClick()}
 * 取按下沿，每按一次发一次；界面开着（{@code mc.screen != null}）时不响应，
 * 免得在别的 GUI 里误触把界面顶掉。</p>
 */
// 游戏总线为默认值（Bus.GAME），省略 bus 属性
@EventBusSubscriber(modid = Godofthings.MODID, value = Dist.CLIENT)
public final class BackpackKeyHandler
{
    private BackpackKeyHandler() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.screen != null)
        {
            return;
        }
        while (WandKeyBindings.OPEN_BACKPACK_KEY.get().consumeClick())
        {
            PacketDistributor.sendToServer(new GodBackpackOpenPayload());
        }
    }
}
