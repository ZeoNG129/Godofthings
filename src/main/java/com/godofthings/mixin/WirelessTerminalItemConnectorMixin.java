package com.godofthings.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import appeng.api.networking.IGrid;
import appeng.items.tools.powered.WirelessTerminalItem;

import com.godofthings.aalink.ConnectorCardItem;

/**
 * 无线终端插着已绑定的无线连接器卡时，直接把终端连进卡所绑定的无线连接器所在
 * ME 网络（照抄上游 AE2LT WTMenuHostMixin 的语义——终端联网重定向到卡绑定的网络，
 * 跨维度 / 无线信号范围外均可访问，类似量子桥卡）。
 *
 * <p>[本地改动] 上游 mixin 的是 ae2wtlib 的 WTMenuHost（菜单宿主层）；本地没有
 * ae2wtlib 编译依赖，改挂 AE2 基类 {@code WirelessTerminalItem.getLinkedGrid}——
 * AE2 原版三件终端与 ae2wtlib 全部终端（ItemWT 未覆写此方法）都会经过这里。</p>
 *
 * <p>只在卡已绑定且绑定连接器可用时重定向；否则返回 null 交回原版
 * 无线访问点判定，不影响原版行为。仅服务端（getLinkedGrid 由 host 在服务端调用）。</p>
 */
@Mixin(WirelessTerminalItem.class)
public abstract class WirelessTerminalItemConnectorMixin
{
    @Inject(method = "getLinkedGrid", at = @At("HEAD"), cancellable = true)
    private void godofthings$redirectConnectorCard(ItemStack stack, Level level,
            java.util.function.Consumer<Component> errorCallback, CallbackInfoReturnable<IGrid> cir)
    {
        // 升级槽里插着已绑定的无线连接器卡 → 返回绑定连接器所在网络
        IGrid grid = ConnectorCardItem.resolveBoundGridForTerminal(stack, level);
        if (grid != null)
        {
            cir.setReturnValue(grid);
        }
        // 没有卡 / 卡未绑定 / 绑定连接器不可用：不设置返回值，走原版无线访问点逻辑
    }
}
