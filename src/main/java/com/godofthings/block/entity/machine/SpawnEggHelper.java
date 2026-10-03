package com.godofthings.block.entity.machine;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.CustomData;

/**
 * 刷怪蛋识别：把「原版刷怪蛋」和「各种模组的刷怪蛋」统一成一件事。
 *
 * <p>以前神之掉落机只认 {@link SpawnEggItem} 的实例，所以任何自己实现刷怪蛋物品的模组（没继承原版类）
 * 都放不进去。现在两条路都走：</p>
 * <ol>
 *   <li><b>原版 / NeoForge 系</b>：{@code SpawnEggItem}（含 {@code DeferredSpawnEggItem} 与各种子类，
 *       模组用官方 API 注册的刷怪蛋都在这一类）→ 直接用 {@code getType(stack)}；</li>
 *   <li><b>其它模组自建物品</b>：只要按原版约定把实体数据写进了 {@code ENTITY_DATA} 组件
 *       （实体 SNBT，类型在 {@code "id"} 字段 —— 原版 {@code SpawnEggItem} 自己也是这么读的），
 *       就按这个组件解析出实体类型。</li>
 * </ol>
 *
 * <p>解析不出实体类型的一律当作「不是刷怪蛋」，所以拿石头之类的东西塞机器不会被误当成模板。</p>
 */
public final class SpawnEggHelper
{
    private SpawnEggHelper() {}

    /** 是不是刷怪蛋（能解析出实体类型就算） */
    public static boolean isSpawnEgg(ItemStack stack)
    {
        return typeOf(stack) != null;
    }

    /**
     * 复制刷怪蛋（神之怪蛋的产物）：不是刷怪蛋返回空表，否则返回「同种蛋 × 64」。
     *
     * <p>用 {@code copyWithCount} 复制整份物品栈，所以输入蛋上的组件（实体数据、自定义名等）
     * 一并带过去 —— 模组刷怪蛋的实体类型也能原样复制。</p>
     */
    public static java.util.List<ItemStack> duplicate(ItemStack input)
    {
        if (typeOf(input) == null)
        {
            return java.util.List.of();
        }
        return java.util.List.of(input.copyWithCount(64));
    }

    /** 解析这个物品会生成哪种实体；不是刷怪蛋 / 解析不出返回 null */
    public static EntityType<?> typeOf(ItemStack stack)
    {
        if (stack == null || stack.isEmpty())
        {
            return null;
        }
        // ① 原版与 NeoForge DeferredSpawnEggItem（以及任何继承 SpawnEggItem 的模组刷怪蛋）
        if (stack.getItem() instanceof SpawnEggItem egg)
        {
            EntityType<?> type = egg.getType(stack);
            if (type != null)
            {
                return type;
            }
        }
        // ② 其它模组自建的刷怪蛋：按原版约定读 ENTITY_DATA 组件里的实体 id
        CustomData data = stack.get(DataComponents.ENTITY_DATA);
        if (data != null && !data.isEmpty() && data.contains("id"))
        {
            CompoundTag tag = data.copyTag();
            ResourceLocation id = ResourceLocation.tryParse(tag.getString("id"));
            // 注意：ENTITY_TYPE 是「带默认值」的注册表（默认 pig），直接 get() 一个不存在的 id 会拿到猪，
            // 所以这里必须先 containsKey —— 否则任何写了垃圾 id 的物品都会被当成猪刷怪蛋。
            if (id != null && BuiltInRegistries.ENTITY_TYPE.containsKey(id))
            {
                return BuiltInRegistries.ENTITY_TYPE.get(id);
            }
        }
        return null;
    }
}
