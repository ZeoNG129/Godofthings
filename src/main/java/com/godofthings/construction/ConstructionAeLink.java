package com.godofthings.construction;

import appeng.api.features.GridLinkables;
import appeng.api.features.IGridLinkableHandler;
import appeng.api.ids.AEComponents;
import com.godofthings.Godofthings;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.item.ItemStack;

/**
 * 神之建筑的 AE 无线连接注册。
 *
 * <p>AE2 的「无线终端」是靠 {@link GridLinkables} 登记「哪些物品能绑到无线访问点」的：
 * 拿本物品右键访问点，AE2 调 {@link IGridLinkableHandler#link} 把 {@link GlobalPos} 写进
 * {@code ae2:wireless_link_target} 组件；再右键就是 {@code unlink} 解绑。
 * 绑定后 {@link ConstructionAeSource} 才能顺着这个坐标取物。</p>
 *
 * <p>注册入口在 {@code Godofthings#commonSetup}（FMLCommonSetupEvent）。外面包了
 * {@code try/catch (Throwable)}：本模组虽然硬依赖 AE2，但万一以后把 AE2 改成可选依赖，
 * 这里退化掉也只是「不能从网络取材料」，不会让模组加载阶段直接崩。</p>
 */
public final class ConstructionAeLink
{
    private ConstructionAeLink()
    {
    }

    /** 只认本物品；绑定坐标写在 AE2 自己的组件里（自动存盘 + 自动同步） */
    private static final IGridLinkableHandler HANDLER = new IGridLinkableHandler()
    {
        @Override
        public boolean canLink(ItemStack stack)
        {
            return stack.is(Godofthings.GOD_CONSTRUCTION.get());
        }

        @Override
        public void link(ItemStack stack, GlobalPos pos)
        {
            stack.set(AEComponents.WIRELESS_LINK_TARGET, pos);
        }

        @Override
        public void unlink(ItemStack stack)
        {
            stack.remove(AEComponents.WIRELESS_LINK_TARGET);
        }
    };

    /** 在 FMLCommonSetupEvent 里调用一次 */
    public static void register()
    {
        try
        {
            GridLinkables.register(Godofthings.GOD_CONSTRUCTION.get(), HANDLER);
        }
        catch (Throwable ignored)
        {
            // AE2 缺席时静默退化：材料只能从玩家背包出
        }
    }
}
