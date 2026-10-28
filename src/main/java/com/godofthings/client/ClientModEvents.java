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
import com.godofthings.client.screen.GodDropConfigScreen;
import com.godofthings.client.screen.GodEnchantScreen;
import com.godofthings.client.screen.GodMinerScreen;
import com.godofthings.client.screen.GodMinerConfigScreen;
import com.godofthings.client.screen.GodPeelerScreen;
import com.godofthings.client.screen.GodPeelerConfigScreen;
import com.godofthings.client.screen.GodResourceScreen;
import com.godofthings.client.screen.GodResourceConfigScreen;
import com.godofthings.client.screen.GodSpawnEggScreen;
import com.godofthings.client.screen.GodSpawnEggConfigScreen;
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
        event.register(Godofthings.GOD_RESOURCE_CONFIG_MENU.get(), GodResourceConfigScreen::new);
        event.register(Godofthings.GOD_PEELER_MENU.get(), GodPeelerScreen::new);
        event.register(Godofthings.GOD_PEELER_CONFIG_MENU.get(), GodPeelerConfigScreen::new);
        event.register(Godofthings.GOD_DEVOURER_MENU.get(), GodDevourerScreen::new);
        event.register(Godofthings.PORTABLE_DEVOURER_MENU.get(), GodDevourerScreen::new);
        event.register(Godofthings.GOD_DROP_MENU.get(), GodDropScreen::new);
        event.register(Godofthings.GOD_DROP_CONFIG_MENU.get(), GodDropConfigScreen::new);
        event.register(Godofthings.GOD_SPAWN_EGG_MENU.get(), GodSpawnEggScreen::new);
        event.register(Godofthings.GOD_SPAWN_EGG_CONFIG_MENU.get(), GodSpawnEggConfigScreen::new);
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
        // ME 放置工具（移植自 ME-Placement-Tool）：法杖配置界面 + 线缆工具界面
        event.register(com.godofthings.mept.MeptMenus.WAND_MENU.get(),
                com.godofthings.mept.client.MeptWandScreen::new);
        event.register(com.godofthings.mept.MeptMenus.CABLE_TOOL_MENU.get(),
                com.godofthings.mept.client.MeptCableToolScreen::new);

    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event)
    {
        WandKeyBindings.register(event);
        // 神之皮带按键：默认全部不绑定（到「按键控制」里自己设）；未绑定时用方向键选中
        event.register(ClientEvents.OPEN_TOOL_MENU_KEYBIND =
                new KeyMapping("key.godofthings.belt_open", InputConstants.UNKNOWN.getValue(), "key.godofthings.belt_category"));
        event.register(ClientEvents.CYCLE_TOOL_MENU_LEFT_KEYBIND =
                new KeyMapping("key.godofthings.belt_cycle_left", InputConstants.UNKNOWN.getValue(), "key.godofthings.belt_category"));
        event.register(ClientEvents.CYCLE_TOOL_MENU_RIGHT_KEYBIND =
                new KeyMapping("key.godofthings.belt_cycle_right", InputConstants.UNKNOWN.getValue(), "key.godofthings.belt_category"));
        event.register(ClientEvents.OPEN_BELT_SLOT_KEYBIND =
                new KeyMapping("key.godofthings.belt_slot", InputConstants.UNKNOWN.getValue(), "key.godofthings.belt_category"));
        // ME 放置工具四键（分类「众神之物：ME 放置工具」；G/G/Ctrl/A 默认值照抄上游）
        event.register(com.godofthings.mept.client.MeptKeyBindings.OPEN_RADIAL_MENU);
        event.register(com.godofthings.mept.client.MeptKeyBindings.UNDO_MODIFIER);
        event.register(com.godofthings.mept.client.MeptKeyBindings.OPEN_CABLE_TOOL_GUI);
        event.register(com.godofthings.mept.client.MeptKeyBindings.MARK_COLOR_SHORTCUT);
        // ===== 权杖按键（分类「众神之物：权杖」）。排序即注册顺序：
        // 原版 useless（造化杖）13 键在上，荒辰附属 4 键在下。=====
        // 附魔切换
        event.register(com.godofthings.beef.core.common.KeyBindings.SWITCH_SILK_TOUCH_KEY.get());
        event.register(com.godofthings.beef.core.common.KeyBindings.SWITCH_FORTUNE_KEY.get());
        // 模式开关
        event.register(com.godofthings.beef.core.common.KeyBindings.TOGGLE_CHAIN_MODE_KEY.get());
        event.register(com.godofthings.beef.core.common.KeyBindings.SWITCH_FORCE_MINING_KEY.get());
        event.register(com.godofthings.beef.core.common.KeyBindings.SWITCH_FARMLAND_MODE_KEY.get());
        event.register(com.godofthings.beef.core.common.KeyBindings.TOGGLE_CROP_HARVEST_KEY.get());
        event.register(com.godofthings.beef.core.common.KeyBindings.TOGGLE_SHEARS_KEY.get());
        event.register(com.godofthings.beef.core.common.KeyBindings.TOGGLE_FLINT_AND_STEEL_KEY.get());
        event.register(com.godofthings.beef.core.common.KeyBindings.TOGGLE_AUTO_CLICK_KEY.get());
        // 触发按键
        event.register(com.godofthings.beef.core.common.KeyBindings.TRIGGER_CHAIN_MINING_KEY.get());
        event.register(com.godofthings.beef.core.common.KeyBindings.TRIGGER_FORCE_MINING_KEY.get());
        // 短距传送（造化杖）
        event.register(com.godofthings.beef.core.common.KeyBindings.SHORT_TELEPORT_KEY.get());
        // 模式轮盘
        event.register(com.godofthings.beef.core.common.KeyBindings.SWITCH_MODE_WHEEL_KEY.get());
        // ---- 荒辰移晷之杖附属四键 ----
        event.register(com.godofthings.wondrous.client.StretcherKeyBindings.STANDALONE_TELEPORT);
        event.register(com.godofthings.wondrous.client.StretcherKeyBindings.WONDROUS_STAFF_MODE);
        event.register(com.godofthings.wondrous.client.StretcherKeyBindings.WONDROUS_STAFF_TIME_STOP);
        event.register(com.godofthings.wondrous.client.StretcherKeyBindings.TWO_I_REMOTE_RECLAIM);
    }

    /** 荒辰移晷之杖：杖加速标记 / 时间流逝时钟 / 树叶奖励三个实体的渲染器（照抄上游 ClientSetup）。 */
    @SubscribeEvent
    public static void onWondrousRenderers(EntityRenderersEvent.RegisterRenderers event)
    {
        event.registerEntityRenderer(com.godofthings.wondrous.init.WondrousEntities.WONDROUS_STAFF_ACCELERATION.get(),
                com.godofthings.wondrous.client.render.WondrousStaffAccelerationRenderer::new);
        event.registerEntityRenderer(com.godofthings.wondrous.init.WondrousEntities.STAFF_LEAF_REWARD.get(),
                com.godofthings.wondrous.client.render.StaffLeafRewardRenderer::new);
        event.registerEntityRenderer(com.godofthings.wondrous.init.WondrousEntities.TIME_FLOW.get(),
                com.godofthings.wondrous.client.render.TimeFlowRenderer::new);
    }

    /** 神之皮带：染色皮带用带谓词的模型覆盖（与 models/item/belt.json 里的 has_custom_color 对应）。 */
    @SubscribeEvent
    public static void onClientSetupWork(FMLClientSetupEvent event)
    {
        event.enqueueWork(() -> ItemProperties.register(Godofthings.GOD_BELT.get(), BeltRegistration.location("has_custom_color"),
                (ItemStack pStack, @Nullable ClientLevel pLevel, @Nullable LivingEntity pEntity, int pSeed) ->
                        pStack.has(DataComponents.DYED_COLOR) ? 1 : 0));
        // 超级融合卡：客户端事件接线（软依赖 Mekanism，没装就不碰这些类）
        if (net.neoforged.fml.ModList.get().isLoaded("mekanism"))
        {
            com.godofthings.superfusion.client.SuperFusionClientSetup.init();
        }
        // ME 放置工具客户端接线（照抄上游 ClientModEvents.onClientSetupComplete）：
        // 多方块预览渲染器 / 撤销键 / 轮盘键 / 工具信息 HUD / 部件与线缆预览
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(new com.godofthings.mept.client.MeptMultiblockPreviewRenderer());
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(new com.godofthings.mept.client.MeptUndoKeyHandler());
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(new com.godofthings.mept.client.MeptRadialMenuKeyHandler());
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(new com.godofthings.mept.client.MeptToolInfoHudRenderer());
        com.godofthings.mept.client.MeptPartPreviewRenderer.install();
        com.godofthings.mept.client.MeptCablePreviewRenderer.install();
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

        // 玩偶：头顶玩偶渲染层（构造器直接吃 PlayerRenderer，不走 HumanoidModel 泛型）
        addDollHeadLayer(event, PlayerSkin.Model.WIDE);
        addDollHeadLayer(event, PlayerSkin.Model.SLIM);
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

    /** 给指定皮肤模型的玩家渲染器挂上玩偶头顶层（构造器直接吃 PlayerRenderer） */
    private static void addDollHeadLayer(EntityRenderersEvent.AddLayers event, PlayerSkin.Model skin)
    {
        if (event.getSkin(skin) instanceof net.minecraft.client.renderer.entity.player.PlayerRenderer renderer)
        {
            renderer.addLayer(new com.godofthings.doll.render.TwoIDollHeadLayer(renderer));
        }
    }
}
