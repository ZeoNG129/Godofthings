package com.godofthings.infinitecell.big;

import java.math.BigInteger;
import java.util.List;

import appeng.api.config.FuzzyMode;
import appeng.api.ids.AEComponents;
import appeng.api.storage.cells.ICellWorkbenchItem;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.UpgradeInventories;
import appeng.items.contents.CellConfig;
import appeng.util.ConfigInventory;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

/**
 * 「吞噬万籁的寂静」—— 无限种类 + 无限容量的 ME 存储元件。
 *
 * <p>移植自 ExtendedAE+ 1.6.3 的 {@code com.extendedae_plus.items.InfinityBigIntegerCellItem}
 * （作者 glodblock 等，LGPL-3.0）。名称与 tooltip 的彩色文案全部来自语言文件里的
 * {@code §} 格式码，Java 侧不做任何渲染处理（见 lang 的 {@code item.godofthings.infinity_cell}）。</p>
 *
 * <p>物品本身<b>不存内容</b>：真实数据在世界存档里（{@link InfinityStorageManager}），
 * 物品只带一个 UUID 以及供 tooltip 显示的「种类数 / 总数量」摘要。</p>
 */
public class InfinityCellItem extends Item implements ICellWorkbenchItem
{
    public InfinityCellItem()
    {
        super(new Properties().stacksTo(1).fireResistant());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
            List<Component> tooltip, TooltipFlag tooltipFlag)
    {
        // 题词 / 基础介绍 / Shift 明细统一由 GodTooltipHandler 注入（键名按注册名 infinity_cell），
        // 这里只负责「带数据的实例」才有的动态摘要（UUID / 类型数 / 总量）。
        // 只在 ItemStack 自身带 UUID 时才显示摘要，避免触发持久化或加载逻辑
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (customData.isEmpty())
        {
            return;
        }

        CompoundTag tag = customData.copyTag();
        if (tag.contains(InfinityConstants.INFINITY_CELL_UUID))
        {
            String uuidStr = tag.getUUID(InfinityConstants.INFINITY_CELL_UUID).toString();
            tooltip.add(Component.translatable("tooltip.godofthings.infinity_cell.uuid", uuidStr)
                    .withStyle(ChatFormatting.GRAY));
        }

        if (tag.contains(InfinityConstants.INFINITY_ITEM_TYPES))
        {
            int types = tag.getInt(InfinityConstants.INFINITY_ITEM_TYPES);
            tooltip.add(Component.translatable("tooltip.godofthings.infinity_cell.types",
                    String.format("%,d", types)).withStyle(ChatFormatting.GRAY));
        }

        // 新字段优先，旧字段兜底
        String totalKey = tag.contains(InfinityConstants.INFINITY_ITEM_TOTAL)
                ? InfinityConstants.INFINITY_ITEM_TOTAL
                : (tag.contains(InfinityConstants.INFINITY_CELL_ITEM_COUNT)
                        ? InfinityConstants.INFINITY_CELL_ITEM_COUNT
                        : null);
        if (totalKey != null)
        {
            try
            {
                BigInteger total = new BigInteger(tag.getByteArray(totalKey));
                tooltip.add(Component.translatable("tooltip.godofthings.infinity_cell.total",
                        InfinityCellInventory.formatBigInteger(total)).withStyle(ChatFormatting.GRAY));
            }
            catch (RuntimeException ignored)
            {
                // 坏数据不显示，不让 tooltip 崩掉
            }
        }
    }

    @Override
    public IUpgradeInventory getUpgrades(ItemStack stack)
    {
        // 与上游一致：4 个升级槽（可装反向卡 / 模糊卡 / 容量卡等）
        return UpgradeInventories.forItem(stack, 4);
    }

    @Override
    public ConfigInventory getConfigInventory(ItemStack stack)
    {
        return CellConfig.create(stack);
    }

    @Override
    public FuzzyMode getFuzzyMode(ItemStack stack)
    {
        return stack.getOrDefault(AEComponents.STORAGE_CELL_FUZZY_MODE, FuzzyMode.IGNORE_ALL);
    }

    @Override
    public void setFuzzyMode(ItemStack stack, FuzzyMode fuzzyMode)
    {
        stack.set(AEComponents.STORAGE_CELL_FUZZY_MODE, fuzzyMode);
    }
}
