package com.godofthings.doll.render;

import com.godofthings.doll.init.DollRegistry;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * 玩偶在「头顶槽」与「饰品栏」的显示判定。
 *
 * <p><b>移植说明</b>：原 mod 的 {@code TwoIDollCurioRenderer} 直接
 * {@code implements ICurioRenderer} 并调 {@code CuriosRendererRegistry.register(...)}，
 * 这要求对 Curios 有<b>编译期依赖</b>。godofthings 的硬规则是「Curios 只能反射访问」，
 * 否则没装 Curios 的整合包会在类加载阶段 {@code NoClassDefFoundError}。</p>
 *
 * <p>因此这里<b>不再实现 Curios 的渲染接口</b>，只保留不依赖 Curios 的那部分：
 * 「头盔槽里已经有玩偶时，饰品栏那只不要重复画」的判定。饰品栏本身的渲染
 * 交给 {@link TwoIDollHeadLayer}（头顶）+ {@link TwoIDollHandRenderer}（手持），
 * 这两条路径都不碰 Curios。</p>
 *
 * <p>代价：把玩偶放进 Curios 饰品槽时<b>不会</b>在人物模型上额外显示一只玩偶。
 * 玩偶的核心体验（放出实体、头顶、手持、对话、故事、好感度）不受影响。</p>
 */
public final class TwoIDollCurioRenderer
{
    private TwoIDollCurioRenderer()
    {
    }

    /**
     * 保留给注册入口调用 —— 现在只做空操作。
     *
     * <p>之所以留这个方法而不是删掉调用点：{@code DollClientSetup} 里仍然会调它，
     * 以后真要做饰品栏渲染时把实现填回来即可，不用再改调用方。</p>
     */
    public static void register()
    {
        // 反射模式下无法注册 Curios 渲染器（那需要实现它的接口），故意留空。
    }

    /**
     * 这只玩偶是否应该被画出来。
     *
     * <p>规则：头盔槽已经戴了玩偶时，饰品栏里的那只不重复渲染（否则头上会叠两只）。
     * 不装 Curios 时这个方法仍然有意义 —— 头顶渲染层会用它判断要不要画。</p>
     */
    public static boolean isDisplayed(LivingEntity wearer, ItemStack stack)
    {
        if (wearer == null || stack == null || stack.isEmpty())
        {
            return false;
        }
        return !wearer.getItemBySlot(EquipmentSlot.HEAD).is(DollRegistry.TWO_I_DOLL_ITEM.get());
    }
}
