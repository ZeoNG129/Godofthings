package com.godofthings.ae.wt.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.item.ItemStack;

import appeng.api.upgrades.IUpgradeableItem;

import de.mari_023.ae2wtlib.AE2wtlibAdditionalComponents;
import de.mari_023.ae2wtlib.AE2wtlibItems;
import de.mari_023.ae2wtlib.wct.magnet_card.MagnetHandler;
import de.mari_023.ae2wtlib.wct.magnet_card.MagnetMode;

/**
 * 让 ae2wtlib 的磁力系统认可本模组的磁卡（[本地改动]）。
 *
 * <p>上游 {@code MagnetHandler} 在保存 / 读取磁力模式时硬编码检查
 * {@code isInstalled(AE2wtlibItems.MAGNET_CARD)}——本模组磁卡
 * （godofthings:magnet_card）装进终端后会被当成「没装卡」（NO_CARD），磁力拾取
 * 静默失效。这里在上游两处判定后追加本地磁卡的等价逻辑：装着本地磁卡时同样
 * 保存 / 返回 {@code MAGNET_SETTINGS} 组件值。组件键沿用上游的，两种卡互通。</p>
 */
@Mixin(MagnetHandler.class)
public abstract class MagnetHandlerCardMixin
{
    @Inject(method = "saveMagnetMode", at = @At("HEAD"), cancellable = true)
    private static void godofthings$saveMagnetMode(ItemStack terminal, MagnetMode magnetSettings, CallbackInfo cir)
    {
        if (magnetSettings == MagnetMode.INVALID || magnetSettings == MagnetMode.NO_CARD)
        {
            return;
        }
        if (!(terminal.getItem() instanceof IUpgradeableItem upgradeableItem))
        {
            return;
        }
        if (upgradeableItem.getUpgrades(terminal).isInstalled(
                com.godofthings.Godofthings.MAGNET_CARD.get()))
        {
            terminal.set(AE2wtlibAdditionalComponents.MAGNET_SETTINGS, magnetSettings);
            cir.cancel();
        }
    }

    @Inject(method = "getMagnetMode", at = @At("HEAD"), cancellable = true)
    private static void godofthings$getMagnetMode(ItemStack terminal, CallbackInfoReturnable<MagnetMode> cir)
    {
        if (!(terminal.getItem() instanceof IUpgradeableItem upgradeableItem))
        {
            return;
        }
        if (upgradeableItem.getUpgrades(terminal).isInstalled(
                com.godofthings.Godofthings.MAGNET_CARD.get()))
        {
            cir.setReturnValue(terminal.getOrDefault(AE2wtlibAdditionalComponents.MAGNET_SETTINGS,
                    MagnetMode.OFF));
        }
    }
}
