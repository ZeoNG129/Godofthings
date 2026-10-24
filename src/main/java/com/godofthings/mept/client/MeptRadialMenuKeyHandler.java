package com.godofthings.mept.client;

import com.godofthings.mept.client.MeptKeyBindings;
import com.godofthings.mept.BasePlacementToolItem;
import com.godofthings.mept.ItemMECablePlacementTool;
import com.godofthings.mept.ItemMultiblockPlacementTool;
import com.godofthings.mept.network.OpenCableToolGuiPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 轮盘菜单 / 线缆工具界面键处理（照抄上游 RadialMenuKeyHandler）：
 * 手持 ME 放置工具或多方块工具按 G 打开轮盘；手持线缆工具按 G 直接开配置界面。
 * 用按键输入事件而非每 tick 轮询，才能正确处理「同键开关」。
 */
public class MeptRadialMenuKeyHandler
{
    @SubscribeEvent
    public void onKeyInput(InputEvent.Key event)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // 只响应按下（不响应释放 / 重复）
        if (event.getAction() != InputConstants.PRESS) return;

        // 已有界面开着就不开
        if (mc.screen != null) return;

        // 先主手后副手找放置工具
        var held = BasePlacementToolItem.findHeldTool(mc.player, BasePlacementToolItem.class);
        if (held.isEmpty()) return;

        // 线缆工具——直接开 GUI
        if (held.getItem() instanceof ItemMECablePlacementTool)
        {
            if (MeptKeyBindings.OPEN_CABLE_TOOL_GUI.matches(event.getKey(), event.getScanCode()))
            {
                // 发包让服务端开菜单
                PacketDistributor.sendToServer(new OpenCableToolGuiPayload());
                return;
            }
            // 线缆工具有自己的界面；不为它开轮盘
            return;
        }

        if (!MeptKeyBindings.OPEN_RADIAL_MENU.matches(event.getKey(), event.getScanCode()))
        {
            return;
        }

        // 按工具类型开轮盘
        if (held.getItem() instanceof ItemMultiblockPlacementTool)
        {
            mc.setScreen(new MeptDualLayerRadialMenuScreen());
        }
        else
        {
            mc.setScreen(new MeptRadialMenuScreen());
        }
    }
}
