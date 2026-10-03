package com.godofthings.beef.event.client;

import com.godofthings.beef.UselessMod;
import com.godofthings.beef.event.EventHandler;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;

/**
 * 客户端事件接线。
 *
 * <p><b>本次死代码清理</b>：牛排工具框架已整套删除，本类原先承载的工具按键注册
 * （时运 / 精准 / 连锁 / 强制挖掘 / 模式轮盘 / 耕地 / 收菜 / 剪刀 / 打火石 / 连点 /
 * 短距传送）、模式轮盘按键分发、连点循环与挖掘状态 HUD 全部随之移除；
 * {@code com.godofthings.beef.core.common.KeyBindings} 已无任何键位需要注册。</p>
 *
 * <p>现在只剩「高级隐身」的客户端状态维护：隐身开启 / 关闭时把玩家实体 id 记进
 * {@link EventHandler} 的客户端名单，供 {@code EntityMixin} / {@code LevelMixin} 等
 * 玩家保护 Mixin 在客户端侧判定使用。</p>
 */
@EventBusSubscriber(modid = UselessMod.MODID, value = Dist.CLIENT)
public class ClientEventBusSubscriber {

    @SubscribeEvent
    public static void onClientLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        EventHandler.clearClientBeefAdvancedStealthStates();
    }

    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide() && event.getEntity() instanceof Player player) {
            EventHandler.setClientBeefAdvancedStealthState(player.getId(), false);
        }
    }
}
