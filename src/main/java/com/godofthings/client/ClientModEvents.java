package com.godofthings.client;

import com.godofthings.Godofthings;
import com.godofthings.belt.BeltRegistration;
import com.godofthings.belt.client.ClientEvents;
import com.godofthings.belt.client.ToolBeltLayer;
import com.godofthings.belt.common.BeltScreen;
import com.godofthings.belt.slot.BeltSlotScreen;
import com.godofthings.client.screen.GodBlackBoxScreen;
import com.godofthings.client.screen.GodBackpackScreen;
import com.godofthings.client.screen.GodFurnaceConfigScreen;
import com.godofthings.energy.CreativeEnergyCubeScreen;
import com.godofthings.client.screen.GodFurnaceScreen;
import com.godofthings.client.screen.GodChangeScreen;
import com.godofthings.client.screen.GodCraftScreen;
import com.godofthings.client.screen.GodCraftConfigScreen;
import com.godofthings.client.screen.GodCraftTemplateScreen;
import com.godofthings.client.screen.GodDevourerScreen;
import com.godofthings.client.screen.GodDropScreen;
import com.godofthings.client.screen.GodEnchantScreen;
import com.godofthings.client.screen.GodMinerScreen;
import com.godofthings.client.screen.GodMinerConfigScreen;
import com.godofthings.client.screen.GodPeelerScreen;
import com.godofthings.client.screen.GodPeelerConfigScreen;
import com.godofthings.client.screen.GodResourceScreen;
import com.godofthings.client.screen.GodSpawnEggScreen;
import com.godofthings.client.screen.GodAbsorberConfigScreen;
import com.godofthings.client.screen.GodAbsorberScreen;
import com.godofthings.client.screen.GodSlaughterConfigScreen;
import com.godofthings.client.screen.GodSlaughterScreen;
import com.godofthings.client.screen.GodTransmitterScreen;
import com.godofthings.client.screen.WaypointScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.function.Function;

@EventBusSubscriber(modid = Godofthings.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents
{
    // 1.20.1 为 FMLClientSetupEvent + MenuScreens.register，1.21.1 改为 MOD 总线 RegisterMenuScreensEvent
    @SubscribeEvent
    public static void onClientSetup(RegisterMenuScreensEvent event)
    {
        event.register(Godofthings.GOD_FURNACE_MENU.get(), GodFurnaceScreen::new);
        event.register(Godofthings.GOD_FURNACE_CONFIG_MENU.get(), GodFurnaceConfigScreen::new);
        event.register(Godofthings.GOD_MINER_MENU.get(), GodMinerScreen::new);
        event.register(Godofthings.GOD_MINER_CONFIG_MENU.get(), GodMinerConfigScreen::new);
        event.register(Godofthings.GOD_RESOURCE_MENU.get(), GodResourceScreen::new);
        event.register(Godofthings.GOD_PEELER_MENU.get(), GodPeelerScreen::new);
        event.register(Godofthings.GOD_PEELER_CONFIG_MENU.get(), GodPeelerConfigScreen::new);
        event.register(Godofthings.GOD_DEVOURER_MENU.get(), GodDevourerScreen::new);
        event.register(Godofthings.PORTABLE_DEVOURER_MENU.get(), GodDevourerScreen::new);
        event.register(Godofthings.GOD_DROP_MENU.get(), GodDropScreen::new);
        event.register(Godofthings.GOD_SPAWN_EGG_MENU.get(), GodSpawnEggScreen::new);
        event.register(Godofthings.GOD_ENCHANT_MENU.get(), GodEnchantScreen::new);
        event.register(Godofthings.GOD_CHANGE_MENU.get(), GodChangeScreen::new);
        event.register(Godofthings.GOD_CRAFT_MENU.get(), GodCraftScreen::new);
        event.register(Godofthings.GOD_CRAFT_CONFIG_MENU.get(), GodCraftConfigScreen::new);
        event.register(Godofthings.GOD_CRAFT_TEMPLATE_MENU.get(), GodCraftTemplateScreen::new);
        event.register(Godofthings.CREATIVE_ENERGY_CUBE_MENU.get(), CreativeEnergyCubeScreen::new);
        event.register(Godofthings.WAYPOINT_MENU.get(), WaypointScreen::new);
        event.register(Godofthings.GOD_BLACK_BOX_MENU.get(), GodBlackBoxScreen::new);
        event.register(Godofthings.GOD_TRANSMITTER_MENU.get(), GodTransmitterScreen::new);
        event.register(Godofthings.GOD_SLAUGHTER_MENU.get(), GodSlaughterScreen::new);
        event.register(Godofthings.GOD_SLAUGHTER_CONFIG_MENU.get(), GodSlaughterConfigScreen::new);
        event.register(Godofthings.GOD_ABSORBER_MENU.get(), GodAbsorberScreen::new);
        event.register(Godofthings.GOD_ABSORBER_CONFIG_MENU.get(), GodAbsorberConfigScreen::new);
        // 神之背包（120 格随身仓库，右键打开）
        event.register(Godofthings.GOD_BACKPACK_MENU.get(), GodBackpackScreen::new);
        // 神之皮带：皮带内容界面 + 皮带槽位界面（移植自 ToolBelt）
        event.register(Godofthings.GOD_BELT_MENU.get(), BeltScreen::new);
        event.register(Godofthings.GOD_BELT_SLOT_MENU.get(), BeltSlotScreen::new);
        // 无用维度配置界面（潜行右键传送方块打开；照抄 useless_mod 的 DimensionConfigScreen）
        event.register(com.godofthings.beef.init.ModMenuType.DIMENSION_CONFIG_MENU.get(),
                com.godofthings.beef.client.gui.DimensionConfigScreen::new);

    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event)
    {
        WandKeyBindings.register(event);
        // 神之皮带按键：R 打开工具轮盘 / V 打开皮带槽位界面 / 左右循环切换（未绑定时用方向键选中）
        event.register(ClientEvents.OPEN_TOOL_MENU_KEYBIND =
                new KeyMapping("key.godofthings.belt_open", GLFW.GLFW_KEY_R, "key.godofthings.belt_category"));
        event.register(ClientEvents.CYCLE_TOOL_MENU_LEFT_KEYBIND =
                new KeyMapping("key.godofthings.belt_cycle_left", InputConstants.UNKNOWN.getValue(), "key.godofthings.belt_category"));
        event.register(ClientEvents.CYCLE_TOOL_MENU_RIGHT_KEYBIND =
                new KeyMapping("key.godofthings.belt_cycle_right", InputConstants.UNKNOWN.getValue(), "key.godofthings.belt_category"));
        event.register(ClientEvents.OPEN_BELT_SLOT_KEYBIND =
                new KeyMapping("key.godofthings.belt_slot", GLFW.GLFW_KEY_V, "key.godofthings.belt_category"));
    }

    /** 神之皮带：染色皮带用带谓词的模型覆盖（与 models/item/god_belt.json 里的 has_custom_color 对应）。 */
    @SubscribeEvent
    public static void onClientSetupWork(FMLClientSetupEvent event)
    {
        event.enqueueWork(() -> ItemProperties.register(Godofthings.GOD_BELT.get(), BeltRegistration.location("has_custom_color"),
                (ItemStack pStack, @Nullable ClientLevel pLevel, @Nullable LivingEntity pEntity, int pSeed) ->
                        pStack.has(DataComponents.DYED_COLOR) ? 1 : 0));
    }

    /** 神之皮带：物品染色（只染 layer0，扣针保持原色）。 */
    @SubscribeEvent
    public static void onRegisterItemColors(RegisterColorHandlersEvent.Item event)
    {
        event.register(
                (ItemStack pStack, int pTintIndex) ->
                        pTintIndex == 0 && pStack.has(DataComponents.DYED_COLOR)
                                ? (0xFF000000 | pStack.get(DataComponents.DYED_COLOR).rgb()) : -1,
                Godofthings.GOD_BELT.get()
        );
    }

    /** 神之皮带：腰带的穿戴模型图层。 */
    @SubscribeEvent
    public static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event)
    {
        event.registerLayerDefinition(ClientEvents.BELT_LAYER, ToolBeltLayer.BeltModel::createBodyLayer);
    }

    /** 神之皮带：给玩家 / 僵尸系 / 盔甲架挂上腰带图层（有皮带才渲染）。 */
    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event)
    {
        addLayerToHumanoid(event, EntityType.ARMOR_STAND, ToolBeltLayer::new);
        addLayerToHumanoid(event, EntityType.ZOMBIE, ToolBeltLayer::new);
        addLayerToHumanoid(event, EntityType.SKELETON, ToolBeltLayer::new);
        addLayerToHumanoid(event, EntityType.HUSK, ToolBeltLayer::new);
        addLayerToHumanoid(event, EntityType.DROWNED, ToolBeltLayer::new);
        addLayerToHumanoid(event, EntityType.STRAY, ToolBeltLayer::new);

        addLayerToPlayerSkin(event, PlayerSkin.Model.WIDE, ToolBeltLayer::new);
        addLayerToPlayerSkin(event, PlayerSkin.Model.SLIM, ToolBeltLayer::new);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static <E extends Player, M extends HumanoidModel<E>>
    void addLayerToPlayerSkin(EntityRenderersEvent.AddLayers event, PlayerSkin.Model skinName, Function<LivingEntityRenderer<E, M>, ? extends RenderLayer<E, M>> factory)
    {
        EntityRenderer renderer = event.getSkin(skinName);
        if (renderer instanceof LivingEntityRenderer ler) ler.addLayer(factory.apply(ler));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static <E extends LivingEntity, M extends HumanoidModel<E>>
    void addLayerToHumanoid(EntityRenderersEvent.AddLayers event, EntityType<E> entityType, Function<LivingEntityRenderer<E, M>, ? extends RenderLayer<E, M>> factory)
    {
        EntityRenderer<E> renderer = event.getRenderer(entityType);
        if (renderer instanceof LivingEntityRenderer ler) ler.addLayer(factory.apply(ler));
    }
}
