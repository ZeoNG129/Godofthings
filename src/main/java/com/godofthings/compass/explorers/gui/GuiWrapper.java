package com.godofthings.compass.explorers.gui;

import com.godofthings.compass.explorers.ExplorersCompassData;
import com.godofthings.compass.explorers.items.ExplorersCompassItem;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 打开结构选择界面（移植自 Explorer's Compass 1.20.x，作者 ChaosTheDude，CC BY-NC-SA 4.0）。
 *
 * <p>只在客户端调用；服务端路径永远碰不到这个类（物品的 {@code use} 里用
 * {@code level.isClientSide()} 挡着）。</p>
 */
@OnlyIn(Dist.CLIENT)
public final class GuiWrapper
{
    private GuiWrapper()
    {
    }

    public static void openGUI(Level level, Player player, ItemStack stack)
    {
        Minecraft.getInstance().setScreen(new ExplorersCompassScreen(level, player, stack,
                (ExplorersCompassItem) stack.getItem(), ExplorersCompassData.allowedStructureKeys));
    }
}
