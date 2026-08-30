package com.godofthings.toolbelt.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.godofthings.toolbelt.BeltFinder;
import com.godofthings.toolbelt.ToolBelt;
import com.godofthings.toolbelt.ToolBeltConfig;
import com.godofthings.toolbelt.common.BeltScreen;
import com.godofthings.toolbelt.common.BeltSlotScreen;
import com.godofthings.toolbelt.network.OpenBeltSlotInventory;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.function.Function;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = ToolBelt.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientEvents
{
    public static KeyMapping OPEN_TOOL_MENU_KEYBIND;
    public static KeyMapping CYCLE_TOOL_MENU_LEFT_KEYBIND;
    public static KeyMapping CYCLE_TOOL_MENU_RIGHT_KEYBIND;

    public static KeyMapping OPEN_BELT_SLOT_KEYBIND;

    public static void wipeOpen()
    {
        while (OPEN_TOOL_MENU_KEYBIND.consumeClick())
        {
        }
    }

    private static boolean toolMenuKeyWasDown = false;

    @SubscribeEvent
    public static void handleKeys(TickEvent.ClientTickEvent ev)
    {
        if (ev.phase != TickEvent.Phase.START)
            return;

        Minecraft mc = Minecraft.getInstance();

        if (mc.screen == null)
        {
            boolean toolMenuKeyIsDown = OPEN_TOOL_MENU_KEYBIND.isDown();
            if (toolMenuKeyIsDown && !toolMenuKeyWasDown)
            {
                while (OPEN_TOOL_MENU_KEYBIND.consumeClick())
                {
                    if (mc.screen == null && mc.player != null)
                    {
                        ItemStack inHand = mc.player.getMainHandItem();
                        if (ToolBeltConfig.isItemStackAllowed(inHand))
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

        if (ToolBeltConfig.customBeltSlotEnabled)
        {
            while (OPEN_BELT_SLOT_KEYBIND.consumeClick())
            {
                if (mc.screen == null)
                {
                    ToolBelt.channel.sendToServer(new OpenBeltSlotInventory());
                }
            }
        }
    }

    public static boolean isKeyDown(KeyMapping keybind)
    {
        if (keybind.isUnbound())
            return false;

        boolean isDown = switch (keybind.getKey().getType())
                {
                    case KEYSYM -> InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), keybind.getKey().getValue());
                    case MOUSE -> GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().getWindow(), keybind.getKey().getValue()) == GLFW.GLFW_PRESS;
                    default -> keybind.isDown();
                };
        return isDown && keybind.getKeyConflictContext().isActive() && keybind.getKeyModifier().isActive(keybind.getKeyConflictContext());
    }

    public static ModelLayerLocation BELT_LAYER = new ModelLayerLocation(ResourceLocation.tryBuild("minecraft", "player"), "toolbelt_belt");

    @Mod.EventBusSubscriber(modid = ToolBelt.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModBusEvents
    {
        @SubscribeEvent
        public static void initKeybinds(RegisterKeyMappingsEvent event)
        {
            event.register(OPEN_TOOL_MENU_KEYBIND =
                    new KeyMapping("key.godofthings.toolbelt.open", GLFW.GLFW_KEY_R, "key.godofthings.toolbelt.category"));

            event.register(CYCLE_TOOL_MENU_LEFT_KEYBIND =
                    new KeyMapping("key.godofthings.toolbelt.cycle.left", InputConstants.UNKNOWN.getValue(), "key.godofthings.toolbelt.category"));

            event.register(CYCLE_TOOL_MENU_RIGHT_KEYBIND =
                    new KeyMapping("key.godofthings.toolbelt.cycle.right", InputConstants.UNKNOWN.getValue(), "key.godofthings.toolbelt.category"));

            event.register(OPEN_BELT_SLOT_KEYBIND =
                    new KeyMapping("key.godofthings.toolbelt.slot", GLFW.GLFW_KEY_V, "key.godofthings.toolbelt.category"));
        }

        @SubscribeEvent
        public static void clientSetup(FMLClientSetupEvent event)
        {
            event.enqueueWork(() -> {
                ItemProperties.register(ToolBelt.BELT.get(), ToolBelt.location("has_custom_color"),
                        (ItemStack pStack, @Nullable ClientLevel pLevel, @Nullable LivingEntity pEntity, int pSeed) ->
                                (pStack.getItem() instanceof DyeableLeatherItem dyeable) && dyeable.hasCustomColor(pStack)
                                        ? 1 : 0
                );
                MenuScreens.register(ToolBelt.BELT_MENU.get(), BeltScreen::new);
                MenuScreens.register(ToolBelt.BELT_SLOT_MENU.get(), BeltSlotScreen::new);
            });
        }

        @SubscribeEvent
        public static void colors(RegisterColorHandlersEvent.Item event)
        {
            event.register(
                    (ItemStack pStack, int pTintIndex) ->
                            pTintIndex == 0 && (pStack.getItem() instanceof DyeableLeatherItem dyeable) && dyeable.hasCustomColor(pStack)
                                    ? dyeable.getColor(pStack) : -1,
                    ToolBelt.BELT.get()
            );
        }

        @SubscribeEvent
        public static void registerLayer(EntityRenderersEvent.RegisterLayerDefinitions event)
        {
            event.registerLayerDefinition(BELT_LAYER, ToolBeltLayer.BeltModel::createBodyLayer);
        }

        @SubscribeEvent
        public static void construct(EntityRenderersEvent.AddLayers event)
        {
            addLayerToHumanoid(event, EntityType.ARMOR_STAND, ToolBeltLayer::new);
            addLayerToHumanoid(event, EntityType.ZOMBIE, ToolBeltLayer::new);
            addLayerToHumanoid(event, EntityType.SKELETON, ToolBeltLayer::new);
            addLayerToHumanoid(event, EntityType.HUSK, ToolBeltLayer::new);
            addLayerToHumanoid(event, EntityType.DROWNED, ToolBeltLayer::new);
            addLayerToHumanoid(event, EntityType.STRAY, ToolBeltLayer::new);

            addLayerToPlayerSkin(event, "default", ToolBeltLayer::new);
            addLayerToPlayerSkin(event, "slim", ToolBeltLayer::new);
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        private static <E extends Player, M extends HumanoidModel<E>>
        void addLayerToPlayerSkin(EntityRenderersEvent.AddLayers event, String skinName, Function<LivingEntityRenderer<E, M>, ? extends RenderLayer<E, M>> factory)
        {
            LivingEntityRenderer renderer = event.getSkin(skinName);
            if (renderer != null) renderer.addLayer(factory.apply(renderer));
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        private static <E extends LivingEntity, M extends HumanoidModel<E>>
        void addLayerToHumanoid(EntityRenderersEvent.AddLayers event, EntityType<E> entityType, Function<LivingEntityRenderer<E, M>, ? extends RenderLayer<E, M>> factory)
        {
            LivingEntityRenderer<E, M> renderer = event.getRenderer(entityType);
            if (renderer != null) renderer.addLayer(factory.apply(renderer));
        }
    }
}
