package com.godofthings.item;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;

/**
 * 神之护甲（神之头/神之甲/神之腿/神之鞋）。
 * 效果由 {@link com.godofthings.handler.GodArmorHandler} 处理。
 *
 * 1.21.1 移植说明：ArmorItem 构造器需 Holder&lt;ArmorMaterial&gt;（ArmorMaterials.NETHERITE
 * 在 1.21.1 已是 Holder，直接沿用）；Item#isDamageable(ItemStack) 已被移除，
 * 此处通过不设置 MAX_DAMAGE 组件（不给 durability）达到与旧版 isDamageable=false
 * 相同的"永不消耗耐久"效果。
 */
public class GodArmorItem extends ArmorItem
{
    public GodArmorItem(Type type, Properties properties)
    {
        super(ArmorMaterials.NETHERITE, type, properties.setNoRepair());
    }
}
