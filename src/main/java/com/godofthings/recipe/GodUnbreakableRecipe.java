package com.godofthings.recipe;

import com.godofthings.Godofthings;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * 神之不毁合成：任意有耐久的物品 + 神之不毁 → 该物品带无限耐久（保留原有附魔/名字等 NBT）。
 * 无固定配方表（isSpecial），在合成台手工摆放即可。
 * 实现 CraftingRecipe 接口，供 JEI 等正常识别。
 */
public class GodUnbreakableRecipe extends CustomRecipe
{
    public GodUnbreakableRecipe(CraftingBookCategory category)
    {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level)
    {
        boolean hasUnbreakable = false;
        boolean hasTool = false;
        for (int i = 0; i < input.size(); i++)
        {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty())
            {
                continue;
            }
            if (stack.getItem() == Godofthings.GOD_UNBREAKABLE.get())
            {
                if (hasUnbreakable)
                {
                    return false; // 只能放一个神之不毁
                }
                hasUnbreakable = true;
            }
            else
            {
                // 任意物品都可以（含格雷科技等模组的工具/物品），不再强制 isDamageable
                if (hasTool)
                {
                    return false; // 只能放一个其他物品
                }
                hasTool = true;
            }
        }
        return hasUnbreakable && hasTool;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries)
    {
        ItemStack tool = ItemStack.EMPTY;
        for (int i = 0; i < input.size(); i++)
        {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty() && stack.getItem() != Godofthings.GOD_UNBREAKABLE.get())
            {
                tool = stack;
                break;
            }
        }
        if (tool.isEmpty())
        {
            return ItemStack.EMPTY;
        }
        ItemStack result = tool.copy();
        // 1.21.1：NBT "Unbreakable" 标签改为 DataComponents.UNBREAKABLE 组件（其余组件随 copy 保留）
        result.set(DataComponents.UNBREAKABLE, new Unbreakable(true)); // 无限耐久
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height)
    {
        return width * height >= 2;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries)
    {
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<?> getSerializer()
    {
        return Godofthings.GOD_UNBREAKABLE_SERIALIZER.get();
    }

    @Override
    public boolean isSpecial()
    {
        return true; // 不在配方书显示
    }

    public static class Serializer implements RecipeSerializer<GodUnbreakableRecipe>
    {
        // 参考原版 SimpleCraftingRecipeSerializer 的写法（1.21.1 RecipeSerializer = MapCodec + StreamCodec）
        public static final MapCodec<GodUnbreakableRecipe> CODEC = RecordCodecBuilder.mapCodec(
                instance -> instance.group(
                                CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(CraftingRecipe::category)
                        )
                        .apply(instance, GodUnbreakableRecipe::new)
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, GodUnbreakableRecipe> STREAM_CODEC = StreamCodec.composite(
                CraftingBookCategory.STREAM_CODEC, CraftingRecipe::category, GodUnbreakableRecipe::new
        );

        @Override
        public MapCodec<GodUnbreakableRecipe> codec()
        {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, GodUnbreakableRecipe> streamCodec()
        {
            return STREAM_CODEC;
        }
    }
}
