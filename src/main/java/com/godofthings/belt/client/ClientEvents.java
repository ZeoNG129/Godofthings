package com.godofthings.belt.client;

import com.godofthings.Godofthings;
import com.godofthings.belt.BeltFinder;
import com.godofthings.belt.BeltRegistration;
import com.godofthings.belt.ConfigData;
import com.godofthings.belt.network.OpenBeltSlotInventory;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.player.Input;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * 神之皮带的客户端事件（移植自 ToolBelt 2.2.10 的 {@code dev.gigaherz.toolbelt.client.ClientEvents}，BSD-3-Clause）。
 *
 * <p>[本地改动] 上游把按键注册 / 渲染层 / 物品属性都写在内部的 {@code ModBusEvents} 里；
 * 本移植按项目约定把这些**模态总线**注册项搬到 {@code com.godofthings.client.ClientModEvents}，
 * 这里只留游戏总线（GAME bus）的每 tick 按键处理与移动输入覆盖。</p>
 */
@EventBusSubscriber(value = Dist.CLIENT, modid = Godofthings.MODID, bus = EventBusSubscriber.Bus.GAME)
public class ClientEvents
{
    @Nullable public static KeyMapping OPEN_TOOL_MENU_KEYBIND;
    @Nullable public static KeyMapping CYCLE_TOOL_MENU_LEFT_KEYBIND;
    @Nullable public static KeyMapping CYCLE_TOOL_MENU_RIGHT_KEYBIND;
    @Nullable public static KeyMapping OPEN_BELT_SLOT_KEYBIND;

    public static void wipeOpen()
    {
        //noinspection StatementWithEmptyBody
        while (OPEN_TOOL_MENU_KEYBIND != null && OPEN_TOOL_MENU_KEYBIND.consumeClick())
        {
        }
    }

    private static boolean toolMenuKeyWasDown = false;

    @SubscribeEvent
    public static void handleKeys(ClientTickEvent.Pre ev)
    {
        Minecraft mc = Minecraft.getInstance();

        if (mc.screen == null)
        {
            boolean toolMenuKeyIsDown = OPEN_TOOL_MENU_KEYBIND != null && OPEN_TOOL_MENU_KEYBIND.isDown();
            if (toolMenuKeyIsDown && !toolMenuKeyWasDown)
            {
                while (OPEN_TOOL_MENU_KEYBIND != null && OPEN_TOOL_MENU_KEYBIND.consumeClick())
                {
                    if (mc.screen == null)
                    {
                        ItemStack inHand = mc.player.getMainHandItem();
                        if (ConfigData.isItemStackAllowed(inHand))
                        {
                            BeltFinder.findBelt(mc.player).ifPresent((getter) -> mc.setScreen(new RadialMenuScreen(getter)));
                        }
                    }
                }
            }
            toolMenuKeyWasDown = toolMenuKeyIsDown;
        }
        else
        {
            toolMenuKeyWasDown = true;
        }

        while (OPEN_BELT_SLOT_KEYBIND != null && OPEN_BELT_SLOT_KEYBIND.consumeClick())
        {
            if (mc.screen == null)
            {
                PacketDistributor.sendToServer(OpenBeltSlotInventory.INSTANCE);
            }
        }
    }

    @SubscribeEvent
    public static void updateInputEvent(MovementInputUpdateEvent event)
    {
        if (Minecraft.getInstance().screen instanceof RadialMenuScreen)
        {
            Options settings = Minecraft.getInstance().options;
            Input eInput = event.getInput();
            eInput.up = isKeyDown0(settings.keyUp);
            eInput.down = isKeyDown0(settings.keyDown);
            eInput.left = isKeyDown0(settings.keyLeft);
            eInput.right = isKeyDown0(settings.keyRight);

            eInput.forwardImpulse = eInput.up == eInput.down ? 0.0F : (eInput.up ? 1.0F : -1.0F);
            eInput.leftImpulse = eInput.left == eInput.right ? 0.0F : (eInput.left ? 1.0F : -1.0F);
            eInput.jumping = isKeyDown0(settings.keyJump);
            eInput.shiftKeyDown = isKeyDown0(settings.keyShift);
            if (Minecraft.getInstance().player.isMovingSlowly())
            {
                eInput.leftImpulse = (float) ((double) eInput.leftImpulse * 0.3D);
                eInput.forwardImpulse = (float) ((double) eInput.forwardImpulse * 0.3D);
            }
        }
    }

    public static boolean isKeyDown0(KeyMapping keybind)
    {
        if (keybind.isUnbound())
            return false;

        return switch (keybind.getKey().getType())
        {
            case KEYSYM ->
                    InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), keybind.getKey().getValue());
            case MOUSE ->
                    GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().getWindow(), keybind.getKey().getValue()) == GLFW.GLFW_PRESS;
            default -> keybind.isDown();
        };
    }

    public static boolean isKeyDown(KeyMapping keybind)
    {
        return isKeyDown0(keybind) && keybind.getKeyConflictContext().isActive() && keybind.getKeyModifier().isActive(keybind.getKeyConflictContext());
    }

    public static ModelLayerLocation BELT_LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("minecraft", "player"), "godofthings_belt");
}
