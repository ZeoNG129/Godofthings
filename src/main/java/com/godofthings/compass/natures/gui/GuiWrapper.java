package com.godofthings.compass.natures.gui;

import com.godofthings.compass.natures.NaturesCompassData;
import com.godofthings.compass.natures.items.NaturesCompassItem;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 打开自然罗盘界面（移植自 Natures Compass 3.4.0，作者 ChaosTheDude，CC BY-NC-SA 4.0）。
 *
 * <p><b>本类是「客户端专用」的入口</b>：只在 {@code NaturesCompassItem#use} 的
 * {@code level.isClientSide()} 分支里被调用，服务端路径永远碰不到它，因此不需要
 * {@code @EventBusSubscriber}（见 AGENTS.md：没有 {@code @SubscribeEvent} 的类不许挂那个注解）。</p>
 */
public final class GuiWrapper
{
    private GuiWrapper()
    {
    }

    public static void openGUI(Level level, Player player, ItemStack stack)
    {
        Minecraft.getInstance().setScreen(new NaturesCompassScreen(level, player, stack, (NaturesCompassItem) stack.getItem(),
                NaturesCompassData.allowedBiomes));
    }
}
