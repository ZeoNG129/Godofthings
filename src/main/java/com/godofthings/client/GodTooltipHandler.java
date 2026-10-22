package com.godofthings.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import com.godofthings.Godofthings;

/**
 * 全局 Tooltip 注入器（v5.26.0 起）。
 *
 * <p>本模组绝大多数方块 / 物品都是「无自定义类」的（{@code BlockItem} 或简单 {@code Items.registerItem}），
 * 一个个去写 {@code appendHoverText} 既啰嗦又容易漏。这里改用 NeoForge 的
 * {@link ItemTooltipEvent} 一次性接管：凡是 {@code godofthings:} 命名空间的物品，都按
 * 「题词 + 基础介绍 / Shift 展开明细」的双层规范追加。</p>
 *
 * <p>样式规范与键名见 {@code GodTooltips}：{@code tooltip.godofthings.<注册名>.lore / .desc / .shift_N}。
 * 文案全部放语言文件、用 {@code §} 上色，Java 侧不做颜色处理。</p>
 *
 * <p><b>为什么只挂客户端</b>：{@code ItemTooltipEvent} 只在客户端渲染 tooltip 时触发，
 * 且本类引用了 {@code net.minecraft.client.*}，所以必须 {@code Dist.CLIENT}。
 * 注意本类有真实的 {@code @SubscribeEvent} 方法，符合项目里「有注解就必须有订阅方法」的硬规则。</p>
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = Godofthings.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class GodTooltipHandler
{
    private static final String NS = Godofthings.MODID + ":";
    private static final String KEY_ROOT = "tooltip.godofthings.";
    private static final String SHIFT_HINT = KEY_ROOT + "shift_hint";

    private GodTooltipHandler()
    {
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event)
    {
        ResourceLocation key = net.minecraft.core.registries.BuiltInRegistries.ITEM
                .getKey(event.getItemStack().getItem());
        if (key == null || !key.getNamespace().equals(Godofthings.MODID))
        {
            return;
        }

        String id = key.getPath();
        boolean shift = Screen.hasShiftDown();

        // 题词（金色斜体）+ 基础介绍
        appendIfPresent(event, id + ".lore");
        appendIfPresent(event, id + ".desc");

        if (shift)
        {
            for (int i = 1; i <= 8; i++)
            {
                if (!appendIfPresent(event, id + ".shift_" + i))
                {
                    break;
                }
            }
        }
        else if (I18n.exists(KEY_ROOT + id + ".shift_1"))
        {
            event.getToolTip().add(Component.translatable(SHIFT_HINT)
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    /** 追加一条；键不存在返回 false */
    private static boolean appendIfPresent(ItemTooltipEvent event, String suffix)
    {
        String full = KEY_ROOT + suffix;
        if (!I18n.exists(full))
        {
            return false;
        }
        event.getToolTip().add(Component.translatable(full));
        return true;
    }
}
