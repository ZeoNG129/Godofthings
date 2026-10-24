package com.godofthings.mept.client;

import com.godofthings.mept.client.MeptKeyBindings;
import com.godofthings.mept.MeptItems;
import com.godofthings.mept.network.UndoPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * 撤销键处理（照抄上游 UndoKeyHandler）：
 * 按住撤销修饰键 + 左键 = 撤销上次放置。
 */
public class MeptUndoKeyHandler
{
    @SubscribeEvent
    public void onMouseButton(InputEvent.MouseButton.Pre event)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null)
        {
            return;
        }

        // 只有多方块 / 线缆工具可撤销
        var mainHandItem = mc.player.getMainHandItem();
        boolean holdingUndoableTool = mainHandItem.getItem() == MeptItems.MULTIBLOCK_PLACEMENT_TOOL.get() ||
                mainHandItem.getItem() == MeptItems.ME_CABLE_PLACEMENT_TOOL.get();
        if (mainHandItem.isEmpty() || !holdingUndoableTool)
        {
            return;
        }

        // 左键 + 撤销修饰键
        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT && event.getAction() == GLFW.GLFW_PRESS)
        {
            if (MeptKeyBindings.UNDO_MODIFIER.isDown())
            {
                HitResult hitResult = mc.hitResult;
                if (hitResult != null && hitResult.getType() == HitResult.Type.BLOCK)
                {
                    BlockHitResult blockHitResult = (BlockHitResult) hitResult;
                    PacketDistributor.sendToServer(new UndoPayload(blockHitResult.getBlockPos()));
                    event.setCanceled(true);
                }
            }
        }
    }
}
