package com.godofthings.client;

import com.godofthings.Godofthings;
import com.godofthings.network.WandMessages;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;

/**
 * 神之工具的按键处理（客户端）。
 */
@EventBusSubscriber(modid = Godofthings.MODID, value = Dist.CLIENT)
public class WandKeyInputHandler
{
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event)
    {
        int key = event.getKey();
        int action = event.getAction();

        // Tab 键：按住连锁挖掘（按下/松开都处理）。
        // 必须在 screen != null 检查之前处理，否则打开界面（物品栏等）时 Tab 的 RELEASE
        // 事件被吞掉，SWITCH_CHAIN_MINING_KEY_WAS_DOWN 与服务端 isTabPressed 卡在 true，
        // 连锁挖掘就再也关不掉了。
        if (key == WandKeyBindings.SWITCH_CHAIN_MINING_KEY.get().getKey().getValue())
        {
            boolean isDown = action == InputConstants.PRESS || action == InputConstants.REPEAT;
            if (isDown != WandKeyBindings.SWITCH_CHAIN_MINING_KEY_WAS_DOWN)
            {
                WandKeyBindings.SWITCH_CHAIN_MINING_KEY_WAS_DOWN = isDown;
                WandMessages.sendMiningControl(WandMessages.MiningControl.TAB_PRESSED, isDown);
            }
            return;
        }

        if (Minecraft.getInstance().screen != null)
        {
            return;
        }

        // R 键：触发强制破坏（携带 Tab 按住状态）
        if (key == WandKeyBindings.TRIGGER_FORCE_MINING_KEY.get().getKey().getValue())
        {
            if (action == InputConstants.PRESS)
            {
                WandMessages.sendMiningControl(WandMessages.MiningControl.TRIGGER_FORCE,
                        WandKeyBindings.SWITCH_CHAIN_MINING_KEY_WAS_DOWN);
            }
            return;
        }

        if (action != InputConstants.PRESS)
        {
            return;
        }

        WandMessages.WandAction wandAction = null;

        if (key == WandKeyBindings.SWITCH_SILK_TOUCH_KEY.get().getKey().getValue())
        {
            wandAction = WandMessages.WandAction.SILK_TOUCH;
        }
        else if (key == WandKeyBindings.SWITCH_FORTUNE_KEY.get().getKey().getValue())
        {
            wandAction = WandMessages.WandAction.FORTUNE;
        }
        else if (key == WandKeyBindings.TOGGLE_CHAIN_MODE_KEY.get().getKey().getValue())
        {
            wandAction = WandMessages.WandAction.TOGGLE_CHAIN_MINING;
        }
        else if (key == WandKeyBindings.SWITCH_ENHANCED_CHAIN_MINING_KEY.get().getKey().getValue())
        {
            wandAction = WandMessages.WandAction.TOGGLE_ENHANCED_CHAIN;
        }
        else if (key == WandKeyBindings.SWITCH_FORCE_MINING_KEY.get().getKey().getValue())
        {
            wandAction = WandMessages.WandAction.TOGGLE_FORCE_MINING;
        }
        else if (key == WandKeyBindings.TOGGLE_FORCE_KILL_KEY.get().getKey().getValue())
        {
            wandAction = WandMessages.WandAction.TOGGLE_FORCE_KILL;
        }
        else if (key == WandKeyBindings.TOGGLE_CAPTURE_KEY.get().getKey().getValue())
        {
            wandAction = WandMessages.WandAction.TOGGLE_CAPTURE;
        }
        else if (key == WandKeyBindings.TOGGLE_INVULNERABILITY_KEY.get().getKey().getValue())
        {
            wandAction = WandMessages.WandAction.TOGGLE_INVULNERABILITY;
        }

        if (wandAction != null)
        {
            WandMessages.send(wandAction);
        }
    }
}
