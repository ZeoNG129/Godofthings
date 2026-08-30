package com.godofthings.recipe;

import com.godofthings.Godofthings;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * 神之不毁合成：任意有耐久的物品 + 神之不毁 → 该物品带无限耐久（保留原有附魔/名字等 NBT）。
 * 无固定配方表（isSpecial），在合成台手工摆放即可。
 * 实现 CraftingRecipe 接口，供 JEI 等正常识别。
 */
public class GodUnbreakableRecipe implements CraftingRecipe
{
    private final ResourceLocation id;

    public GodUnbreakableRecipe(ResourceLocation id)
    {
        this.id = id;
    }

    @Override
    public CraftingBookCategory category()
    {
        return CraftingBookCategory.MISC;
    }

    @Override
    public boolean matches(CraftingContainer container, Level level)
    {
        boolean hasUnbreakable = false;
        boolean hasTool = false;
        for (int i = 0; i < container.getContainerSize(); i++)
        {
            ItemStack stack = container.getItem(i);
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
    public ItemStack assemble(CraftingContainer container, RegistryAccess registryAccess)
    {
        ItemStack tool = ItemStack.EMPTY;
        for (int i = 0; i < container.getContainerSize(); i++)
        {
            ItemStack stack = container.getItem(i);
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
        CompoundTag tag = result.getOrCreateTag();
        tag.putBoolean("Unbreakable", true); // 无限耐久
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height)
    {
        return width * height >= 2;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess)
    {
        return ItemStack.EMPTY;
    }

    @Override
    public ResourceLocation getId()
    {
        return id;
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
        @Override
        public GodUnbreakableRecipe fromJson(ResourceLocation id, com.google.gson.JsonObject json)
        {
            return new GodUnbreakableRecipe(id);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, GodUnbreakableRecipe recipe)
        {
        }

        @Override
        public GodUnbreakableRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer)
        {
            return new GodUnbreakableRecipe(id);
        }
    }
}
