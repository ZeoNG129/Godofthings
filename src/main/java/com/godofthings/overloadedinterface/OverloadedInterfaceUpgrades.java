package com.godofthings.overloadedinterface;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;

import appeng.api.upgrades.Upgrades;
import appeng.core.definitions.AEItems;

/**
 * 过载接口可安装的升级卡注册（照抄上游 AE2LT 的 Upgrades.add 列表）。
 *
 * <p>AE2 的升级槽白名单完全由 {@link Upgrades#add} 驱动——不注册的卡
 * 一张都放不进去。上游给过载接口注册了：</p>
 * <ul>
 *   <li>合成卡（CRAFTING_CARD）×1</li>
 *   <li>模糊卡（FUZZY_CARD）×1</li>
 *   <li>appflux 感应卡 ×1（软依赖：装了 applied flux 才注册）</li>
 * </ul>
 */
public final class OverloadedInterfaceUpgrades
{
    private OverloadedInterfaceUpgrades()
    {
    }

    /** commonSetup 里调用（enqueueWork 内，注册表已就绪）。 */
    public static void register()
    {
        ItemLike block = OverloadedInterfaceRegistry.OVERLOADED_INTERFACE.get();
        Upgrades.add(AEItems.CRAFTING_CARD, block, 1);
        Upgrades.add(AEItems.FUZZY_CARD, block, 1);

        // applied flux 的感应卡（FE 传输）：软依赖，没装就跳过（照抄上游）
        var inductionId = ResourceLocation.fromNamespaceAndPath("appflux", "induction_card");
        Item inductionCard = BuiltInRegistries.ITEM.get(inductionId);
        if (inductionCard != null && inductionCard != net.minecraft.world.item.Items.AIR)
        {
            Upgrades.add(inductionCard, block, 1);
        }
    }
}
