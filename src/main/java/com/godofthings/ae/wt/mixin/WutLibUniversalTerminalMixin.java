package com.godofthings.ae.wt.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.item.Item;

import de.mari_023.ae2wtlib.api.AE2wtlibAPIImpl;
import de.mari_023.ae2wtlib.api.terminal.ItemWUT;

/**
 * 让 ae2wtlib 承认本模组的无耗能通用终端也是「通用终端」（[本地改动]）。
 *
 * <p>上游 {@code AE2wtlibAPIImpl.isUniversalTerminal} 是
 * {@code item == AE2wtlibItems.UNIVERSAL_TERMINAL} 的实例比对，第三方 WUT 一律返回
 * false。该判定被 {@code ItemWT.checkPreconditions}（终端菜单放行）与
 * {@code WUTHandler.findTerminal}（Curios / 背包扫描优先选通用终端）使用。
 * 本模组无耗能 WUT 继承自上游 {@link ItemWUT}，这里把判定放宽为
 * {@code item instanceof ItemWUT}——上游本体与本地物品都命中，其他物品不受影响。</p>
 */
@Mixin(AE2wtlibAPIImpl.class)
public abstract class WutLibUniversalTerminalMixin
{
    @Inject(method = "isUniversalTerminal(Lnet/minecraft/world/item/Item;)Z",
            at = @At("HEAD"), cancellable = true)
    private void godofthings$acceptFreeWUT(Item item, CallbackInfoReturnable<Boolean> cir)
    {
        if (item instanceof ItemWUT)
        {
            cir.setReturnValue(true);
        }
    }
}
