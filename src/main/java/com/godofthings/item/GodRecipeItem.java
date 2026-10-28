package com.godofthings.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 神之配方：「一切神之造物的合成底料」。
 *
 * <p>本身没有交互，是一个纯材料（数据 / 逻辑都不挂在物品上），这样以后把各个神之物品
 * 的配方改成「神之配方 + 若干材料」时，只需动 datapack 里的 recipe json，不用改代码。</p>
 *
 * <p>获取：有序合成 —— 铁锭 / 铜锭 / 金锭 / 钻石 / 下界合金锭 / 绿宝石 / 红石 /
 * 青金石 / 煤炭 各一个摆满 3×3（见 {@code data/godofthings/recipe/recipe.json}）。</p>
 */
public class GodRecipeItem extends Item
{
    public GodRecipeItem(Properties properties)
    {
        super(properties);
    }
}
