package com.godofthings.item;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

/**
 * 神之护甲（神之头/神之甲/神之腿/神之鞋）。
 * 效果由 {@link com.godofthings.handler.GodArmorHandler} 处理。
 *
 * <p>材质用本模组自定义的 {@link GodArmorMaterials#GOD}：数值与下界合金一致，
 * 但穿戴图层指向本模组的 {@code godofthings_layer_1/2.png}（由原版下界合金图层
 * 按本模组「金/紫/青」色板重着色而来），解决「物品图标金紫、穿上身却是原版黑紫」的不一致。</p>
 *
 * <p>1.21.1 移植说明：ArmorItem 构造器需 Holder&lt;ArmorMaterial&gt;（{@code ARMOR_MATERIALS}
 * 注册表返回的 DeferredHolder 即是）；Item#isDamageable(ItemStack) 已被移除，
 * 此处通过不设置 MAX_DAMAGE 组件（不给 durability）达到与旧版 isDamageable=false
 * 相同的"永不消耗耐久"效果。</p>
 */
public class GodArmorItem extends ArmorItem
{
    public GodArmorItem(Type type, Properties properties)
    {
        super(GodArmorMaterials.GOD, type, properties.setNoRepair());
    }
}
