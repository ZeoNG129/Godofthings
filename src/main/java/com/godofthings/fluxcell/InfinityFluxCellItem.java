package com.godofthings.fluxcell;

import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.UpgradeInventories;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * 无限 FE 存储元件（软依赖 AppFlux）。
 *
 * <p>与项目里其他无限元件（原初万象圣晶 / 吞噬万籁的寂静）同档：容量与吞吐都无上限，
 * 对外报告 9.2E。之所以不用普通 FE 容器：NeoForge 的 {@code IEnergyStorage} 是 int，
 * 装不下 9.2E（详见 {@link InfinityFluxCellInventory} 的类注释）。</p>
 *
 * <p>仅当安装了 AppFlux 时才会被注册，因此不会在纯净环境里留下一个“摆设物品”。</p>
 */
public class InfinityFluxCellItem extends Item
{
    /** 空闲耗电（AE/t）：与同级无限元件保持一致。 */
    private static final double IDLE_DRAIN = 8.0;

    public InfinityFluxCellItem()
    {
        super(new Properties().stacksTo(1));
    }

    public double getIdleDrain()
    {
        return IDLE_DRAIN;
    }

    /** 元件可装 1 张升级卡（与 AppFlux 的 FE 元件一致）。 */
    public IUpgradeInventory getUpgrades(ItemStack stack)
    {
        return UpgradeInventories.forItem(stack, 1);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag)
    {
        lines.add(Component.translatable("item.godofthings.infinity_flux_cell.desc.capacity")
                .withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("item.godofthings.infinity_flux_cell.desc.require_appflux")
                .withStyle(FluxBridge.isAvailable() ? ChatFormatting.DARK_GRAY : ChatFormatting.RED));
    }
}
