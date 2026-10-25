package com.godofthings.ae.wt;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import appeng.api.config.Actionable;

import de.mari_023.ae2wtlib.api.terminal.ItemWUT;

/**
 * 无耗能无线通用终端（移植自 ae2wtlib 的 wireless_universal_terminal，[本地改动]
 * 不消耗任何能量）。
 *
 * <p><b>上游架构</b>：ae2wtlib 把 WUT 做成「终端注册表驱动的容器物品」——每装一个终端
 * （无线合成 / 样板编码 / 样板访问，由合成配方写入 {@code ae2wtlib:has_*_terminal} 组件），
 * 就可以通过右键打开对应终端、再按终端循环键切换。ae2wtlib 所有终端判定
 * （打开 / 切换 / 菜单 host / 磁卡宿主 / 合成配方）都基于
 * {@code item instanceof de.mari_023.ae2wtlib.api.terminal.ItemWUT}，因此本物品
 * 直接继承上游 API 类 {@link ItemWUT}——终端选择、打开、循环、菜单宿主、升级槽
 * 与磁力拾取全部走上游原生代码，与 ae2wtlib 生态完全互通。</p>
 *
 * <p><b>无耗能（[本地改动]）</b>：五连覆写——{@link #hasPower} / {@link #usePower}
 * 恒 true（终端内所有操作不扣电）、{@link #getChargeRate} 0（不进充电队列）、
 * {@link #getAECurrentPower} 恒等于容量（GUI 电量条恒满）、
 * {@link #extractAEPower} 恒返回请求量（菜单宿主闲置耗电 / 能量模拟永远满足）。
 * 上游能量组件（STORED_ENERGY / ENERGY_CAPACITY）照常存在但不会被消耗。</p>
 */
public class ItemWUTFree extends ItemWUT
{
    public ItemWUTFree()
    {
        super();
    }

    // ---- 无耗能五连（[本地改动]）----

    @Override
    public boolean hasPower(net.minecraft.world.entity.player.Player player, double amt, ItemStack stack)
    {
        return true;
    }

    @Override
    public boolean usePower(net.minecraft.world.entity.player.Player player, double amt, ItemStack stack)
    {
        return true;
    }

    @Override
    public double getChargeRate(ItemStack stack)
    {
        return 0;
    }

    @Override
    public double getAECurrentPower(ItemStack stack)
    {
        return getAEMaxPower(stack);
    }

    @Override
    public double extractAEPower(ItemStack stack, double amt, Actionable mode)
    {
        return amt;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag)
    {
        super.appendHoverText(stack, context, lines, flag);
        lines.add(Component.translatable("tooltip.godofthings.wut.no_energy"));
    }
}
