package com.godofthings;

import com.godofthings.block.DimensionTeleporterBlock;
import com.godofthings.block.GodDropBlock;
import com.godofthings.block.GodCraftBlock;
import com.godofthings.block.GodEnchantBlock;
import com.godofthings.block.GodFurnaceBlock;
import com.godofthings.block.GodHeavenEnchantBlock;
import com.godofthings.block.GodMinerBlock;
import com.godofthings.block.GodResourceBlock;
import com.godofthings.block.entity.GodDropBlockEntity;
import com.godofthings.block.entity.GodCraftBlockEntity;
import com.godofthings.block.entity.GodEnchantBlockEntity;
import com.godofthings.block.entity.GodFurnaceBlockEntity;
import com.godofthings.block.entity.GodMinerBlockEntity;
import com.godofthings.block.entity.GodResourceBlockEntity;
import com.godofthings.dimension.GodFlatDimension;
import com.godofthings.generator.EnergyGeneratorBlock;
import com.godofthings.generator.EnergyGeneratorEntity;
import com.godofthings.generator.EnergyGeneratorItem;
import com.godofthings.generator.EnergyGeneratorMenu;
import com.godofthings.generator.EnergyRelayBlock;
import com.godofthings.generator.EnergyRelayEntity;
import com.godofthings.generator.EnergyRelayMenu;
import com.godofthings.handler.AdAstraCompat;
import com.godofthings.handler.GodFavorWandAe2Helper;
import com.godofthings.item.GodArmorItem;
import com.godofthings.item.GodChangeItem;
import com.godofthings.item.EnchantScrollItem;
import com.godofthings.item.GodFavorWandItem;
import com.godofthings.item.GodMinerItem;
import com.godofthings.item.GodUnbreakableItem;
import com.godofthings.menu.GodChangeMenu;
import com.godofthings.menu.GodDropMenu;
import com.godofthings.network.WandMessages;
import com.godofthings.menu.GodCraftMenu;
import com.godofthings.menu.GodCraftConfigMenu;
import com.godofthings.menu.GodCraftTemplateMenu;
import com.godofthings.menu.GodEnchantMenu;
import com.godofthings.recipe.GodUnbreakableRecipe;
import com.godofthings.menu.GodFurnaceConfigMenu;
import com.godofthings.menu.GodFurnaceMenu;
import com.godofthings.menu.GodMinerMenu;
import com.godofthings.menu.GodResourceMenu;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Godofthings.MODID)
public class Godofthings
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "godofthings";
    /** 天神附魔的附魔等级上限（突破原版，类似 give 指令） */
    public static final int HEAVENLY_ENCHANT_MAX_LEVEL = 255;
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    // ---- 维度 ----
    public static final ResourceKey<Level> SUPERFLAT_DIMENSION = ResourceKey.create(Registries.DIMENSION, ResourceLocation.tryBuild(MODID, "superflat"));
    public static final ResourceKey<Level> VOID_DIMENSION = ResourceKey.create(Registries.DIMENSION, ResourceLocation.tryBuild(MODID, "void"));

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, MODID);

    // ---- 方块 ----
    // 注意：不能用 Properties.copy(Blocks.FURNACE) —— Forge 1.20.1 下会连带原版熔炉的
    // `lit` 状态属性，而本方块未定义该属性，注册时直接崩溃。因此手动构造属性。
    public static final RegistryObject<Block> GOD_FURNACE = BLOCKS.register("god_furnace",
            () -> new GodFurnaceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.5F, 3.5F)
                    .sound(SoundType.STONE)
            ));
    public static final RegistryObject<Item> GOD_FURNACE_ITEM = ITEMS.register("god_furnace",
            () -> new BlockItem(GOD_FURNACE.get(), new Item.Properties()));

    // ---- 神之矿机 ----
    public static final RegistryObject<Block> GOD_MINER = BLOCKS.register("god_miner",
            () -> new GodMinerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(5.0F, 6.0F)
                    .sound(SoundType.METAL)
            ));
    public static final RegistryObject<Item> GOD_MINER_ITEM = ITEMS.register("god_miner",
            () -> new GodMinerItem(GOD_MINER.get(), new Item.Properties().stacksTo(1)));

    // ---- 神之资源 ----
    public static final RegistryObject<Block> GOD_RESOURCE = BLOCKS.register("god_resource",
            () -> new GodResourceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_YELLOW)
                    .strength(5.0F, 6.0F)
                    .sound(SoundType.METAL)
            ));
    public static final RegistryObject<Item> GOD_RESOURCE_ITEM = ITEMS.register("god_resource",
            () -> new BlockItem(GOD_RESOURCE.get(), new Item.Properties()));

    // ---- 神之掉落 ----
    public static final RegistryObject<Block> GOD_DROP = BLOCKS.register("god_drop",
            () -> new GodDropBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(5.0F, 6.0F)
                    .sound(SoundType.METAL)
            ));
    public static final RegistryObject<Item> GOD_DROP_ITEM = ITEMS.register("god_drop",
            () -> new BlockItem(GOD_DROP.get(), new Item.Properties()));

    // ---- 神之附魔 ----
    public static final RegistryObject<Block> GOD_ENCHANT = BLOCKS.register("god_enchant",
            () -> new GodEnchantBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(3.0F, 6.0F)
                    .sound(SoundType.STONE)));
    public static final RegistryObject<Item> GOD_ENCHANT_ITEM = ITEMS.register("god_enchant",
            () -> new BlockItem(GOD_ENCHANT.get(), new Item.Properties()));
    public static final RegistryObject<Block> GOD_HEAVEN_ENCHANT = BLOCKS.register("god_heaven_enchant",
            () -> new GodHeavenEnchantBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(3.0F, 6.0F)
                    .sound(SoundType.STONE)));
    public static final RegistryObject<Item> GOD_HEAVEN_ENCHANT_ITEM = ITEMS.register("god_heaven_enchant",
            () -> new BlockItem(GOD_HEAVEN_ENCHANT.get(), new Item.Properties()));

    // ---- 神装 ----
    public static final RegistryObject<Item> GOD_HELMET = ITEMS.register("god_helmet",
            () -> new GodArmorItem(net.minecraft.world.item.ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistryObject<Item> GOD_CHESTPLATE = ITEMS.register("god_chestplate",
            () -> new GodArmorItem(net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistryObject<Item> GOD_LEGGINGS = ITEMS.register("god_leggings",
            () -> new GodArmorItem(net.minecraft.world.item.ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistryObject<Item> GOD_BOOTS = ITEMS.register("god_boots",
            () -> new GodArmorItem(net.minecraft.world.item.ArmorItem.Type.BOOTS, new Item.Properties()));

    // ---- 神之不毁 ----
    public static final RegistryObject<Item> GOD_UNBREAKABLE = ITEMS.register("god_unbreakable",
            () -> new GodUnbreakableItem(new Item.Properties()));
    public static final RegistryObject<RecipeSerializer<?>> GOD_UNBREAKABLE_SERIALIZER =
            RECIPE_SERIALIZERS.register("god_unbreakable", GodUnbreakableRecipe.Serializer::new);

    // ---- 附魔卷轴 ----
    public static final RegistryObject<Item> ENCHANT_SCROLL = ITEMS.register("enchant_scroll",
            () -> new EnchantScrollItem(new Item.Properties().stacksTo(64)));

    // ---- 造化垂青之杖 ----
    public static final RegistryObject<Item> GOD_FAVOR_WAND = ITEMS.register("god_favor_wand",
            GodFavorWandItem::new);
    // GT 扳手模式子类（通过模式轮盘切换，不直接出现在创造标签）
    public static final RegistryObject<Item> GOD_FAVOR_WAND_WRENCH = ITEMS.register("god_favor_wand_wrench",
            GodFavorWandItem::new);
    public static final RegistryObject<Item> GOD_FAVOR_WAND_SCREWDRIVER = ITEMS.register("god_favor_wand_screwdriver",
            GodFavorWandItem::new);
    public static final RegistryObject<Item> GOD_FAVOR_WAND_MALLET = ITEMS.register("god_favor_wand_mallet",
            GodFavorWandItem::new);
    public static final RegistryObject<Item> GOD_FAVOR_WAND_CROWBAR = ITEMS.register("god_favor_wand_crowbar",
            GodFavorWandItem::new);
    public static final RegistryObject<Item> GOD_FAVOR_WAND_HAMMER = ITEMS.register("god_favor_wand_hammer",
            GodFavorWandItem::new);

    // ---- 维度传送器（方块）----
    public static final RegistryObject<Block> SUPERFLAT_TELEPORTER = BLOCKS.register("superflat_teleporter",
            () -> new DimensionTeleporterBlock(SUPERFLAT_DIMENSION, "message.godofthings.tp_superflat",
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .strength(2.0F, 65536.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.STONE)));
    public static final RegistryObject<Item> SUPERFLAT_TELEPORTER_ITEM = ITEMS.register("superflat_teleporter",
            () -> new BlockItem(SUPERFLAT_TELEPORTER.get(), new Item.Properties()));

    public static final RegistryObject<Block> VOID_TELEPORTER = BLOCKS.register("void_teleporter",
            () -> new DimensionTeleporterBlock(VOID_DIMENSION, "message.godofthings.tp_void",
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .strength(2.0F, 65536.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.STONE)));
    public static final RegistryObject<Item> VOID_TELEPORTER_ITEM = ITEMS.register("void_teleporter",
            () -> new BlockItem(VOID_TELEPORTER.get(), new Item.Properties()));

    // ---- 能量发电机（移植自 auto-resource）----
    public static final RegistryObject<Block> ENERGY_GENERATOR = BLOCKS.register("energy_generator",
            () -> new EnergyGeneratorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .pushReaction(PushReaction.DESTROY)
                    .strength(0.5F, 3.0F)
                    .lightLevel(state -> 7)
                    .sound(SoundType.METAL)
            ));
    public static final RegistryObject<Item> ENERGY_GENERATOR_ITEM = ITEMS.register("energy_generator",
            () -> new EnergyGeneratorItem(ENERGY_GENERATOR.get()));
    public static final RegistryObject<BlockEntityType<EnergyGeneratorEntity>> ENERGY_GENERATOR_BE =
            BLOCK_ENTITIES.register("energy_generator",
                    () -> BlockEntityType.Builder.of(EnergyGeneratorEntity::new, ENERGY_GENERATOR.get()).build(null));
    public static final RegistryObject<MenuType<EnergyGeneratorMenu>> ENERGY_GENERATOR_MENU =
            MENUS.register("energy_generator", () -> IForgeMenuType.create((id, inv, buf) -> new EnergyGeneratorMenu(id, inv, buf.readBlockPos())));

    // ---- 能量传输器（无线 FE 中继/电池）----
    public static final RegistryObject<Block> ENERGY_RELAY = BLOCKS.register("energy_relay",
            () -> new EnergyRelayBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .pushReaction(PushReaction.DESTROY)
                    .strength(5.0F, 6.0F)
                    .lightLevel(state -> 7)
                    .sound(SoundType.METAL)
            ));
    public static final RegistryObject<Item> ENERGY_RELAY_ITEM = ITEMS.register("energy_relay",
            () -> new BlockItem(ENERGY_RELAY.get(), new Item.Properties()));
    public static final RegistryObject<BlockEntityType<EnergyRelayEntity>> ENERGY_RELAY_BE =
            BLOCK_ENTITIES.register("energy_relay",
                    () -> BlockEntityType.Builder.of(EnergyRelayEntity::new, ENERGY_RELAY.get()).build(null));
    public static final RegistryObject<MenuType<EnergyRelayMenu>> ENERGY_RELAY_MENU =
            MENUS.register("energy_relay", () -> IForgeMenuType.create((id, inv, buf) -> new EnergyRelayMenu(id, inv, buf.readBlockPos())));

    // ---- 神之更改 ----
    public static final RegistryObject<Item> GOD_CHANGE = ITEMS.register("god_change",
            () -> new GodChangeItem(new Item.Properties().stacksTo(1)));

    // ---- 神之合成 ----
    public static final RegistryObject<Block> GOD_CRAFT = BLOCKS.register("god_craft",
            () -> new GodCraftBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(3.0F, 6.0F)
                    .sound(SoundType.WOOD)
            ));
    public static final RegistryObject<Item> GOD_CRAFT_ITEM = ITEMS.register("god_craft",
            () -> new BlockItem(GOD_CRAFT.get(), new Item.Properties()));
    public static final RegistryObject<BlockEntityType<GodCraftBlockEntity>> GOD_CRAFT_BE =
            BLOCK_ENTITIES.register("god_craft",
                    () -> BlockEntityType.Builder.of(GodCraftBlockEntity::new, GOD_CRAFT.get()).build(null));
    public static final RegistryObject<MenuType<GodCraftMenu>> GOD_CRAFT_MENU =
            MENUS.register("god_craft", () -> IForgeMenuType.create(GodCraftMenu::new));
    public static final RegistryObject<MenuType<GodCraftConfigMenu>> GOD_CRAFT_CONFIG_MENU =
            MENUS.register("god_craft_config", () -> IForgeMenuType.create(GodCraftConfigMenu::new));
    public static final RegistryObject<MenuType<GodCraftTemplateMenu>> GOD_CRAFT_TEMPLATE_MENU =
            MENUS.register("god_craft_templates", () -> IForgeMenuType.create(GodCraftTemplateMenu::new));

    // ---- 方块实体 ----
    public static final RegistryObject<BlockEntityType<GodFurnaceBlockEntity>> GOD_FURNACE_BE =
            BLOCK_ENTITIES.register("god_furnace",
                    () -> BlockEntityType.Builder.of(GodFurnaceBlockEntity::new, GOD_FURNACE.get()).build(null));
    public static final RegistryObject<BlockEntityType<GodMinerBlockEntity>> GOD_MINER_BE =
            BLOCK_ENTITIES.register("god_miner",
                    () -> BlockEntityType.Builder.of(GodMinerBlockEntity::new, GOD_MINER.get()).build(null));
    public static final RegistryObject<BlockEntityType<GodResourceBlockEntity>> GOD_RESOURCE_BE =
            BLOCK_ENTITIES.register("god_resource",
                    () -> BlockEntityType.Builder.of(GodResourceBlockEntity::new, GOD_RESOURCE.get()).build(null));
    public static final RegistryObject<BlockEntityType<GodDropBlockEntity>> GOD_DROP_BE =
            BLOCK_ENTITIES.register("god_drop",
                    () -> BlockEntityType.Builder.of(GodDropBlockEntity::new, GOD_DROP.get()).build(null));
    public static final RegistryObject<BlockEntityType<GodEnchantBlockEntity>> GOD_ENCHANT_BE =
            BLOCK_ENTITIES.register("god_enchant",
                    () -> BlockEntityType.Builder.of(GodEnchantBlockEntity::new,
                            GOD_ENCHANT.get(), GOD_HEAVEN_ENCHANT.get()).build(null));

    // ---- 菜单 ----
    public static final RegistryObject<MenuType<GodFurnaceMenu>> GOD_FURNACE_MENU =
            MENUS.register("god_furnace", () -> IForgeMenuType.create(GodFurnaceMenu::new));
    public static final RegistryObject<MenuType<GodFurnaceConfigMenu>> GOD_FURNACE_CONFIG_MENU =
            MENUS.register("god_furnace_config", () -> IForgeMenuType.create(GodFurnaceConfigMenu::new));
    public static final RegistryObject<MenuType<GodMinerMenu>> GOD_MINER_MENU =
            MENUS.register("god_miner", () -> IForgeMenuType.create(GodMinerMenu::new));
    public static final RegistryObject<MenuType<GodResourceMenu>> GOD_RESOURCE_MENU =
            MENUS.register("god_resource", () -> IForgeMenuType.create(GodResourceMenu::new));
    public static final RegistryObject<MenuType<GodDropMenu>> GOD_DROP_MENU =
            MENUS.register("god_drop", () -> IForgeMenuType.create(GodDropMenu::new));
    public static final RegistryObject<MenuType<GodEnchantMenu>> GOD_ENCHANT_MENU =
            MENUS.register("god_enchant", () -> IForgeMenuType.create(GodEnchantMenu::new));
    public static final RegistryObject<MenuType<GodChangeMenu>> GOD_CHANGE_MENU =
            MENUS.register("god_change", () -> IForgeMenuType.create(GodChangeMenu::new));

    // ---- 创造标签 ----
    public static final RegistryObject<CreativeModeTab> GOD_OF_THINGS_TAB =
            CREATIVE_MODE_TABS.register("god_of_things_tab", () -> CreativeModeTab.builder()
                    .icon(() -> GOD_FURNACE_ITEM.get().getDefaultInstance())
                    .title(Component.translatable("itemGroup.godofthings.god_of_things_tab"))
                    .displayItems((parameters, output) ->
                    {
                        output.accept(GOD_FURNACE_ITEM.get());
                        output.accept(GOD_MINER_ITEM.get());
                        output.accept(GOD_RESOURCE_ITEM.get());
                        output.accept(GOD_DROP_ITEM.get());
                        output.accept(GOD_ENCHANT_ITEM.get());
                        output.accept(GOD_HEAVEN_ENCHANT_ITEM.get());
                        output.accept(GOD_HELMET.get());
                        output.accept(GOD_CHESTPLATE.get());
                        output.accept(GOD_LEGGINGS.get());
                        output.accept(GOD_BOOTS.get());
                        output.accept(GOD_FAVOR_WAND.get());
                        output.accept(GOD_UNBREAKABLE.get());
                        output.accept(ENCHANT_SCROLL.get());
                        output.accept(GOD_CHANGE.get());
                        output.accept(GOD_CRAFT_ITEM.get());
                        output.accept(SUPERFLAT_TELEPORTER_ITEM.get());
                        output.accept(VOID_TELEPORTER_ITEM.get());
                        output.accept(ENERGY_GENERATOR_ITEM.get());
                        output.accept(ENERGY_RELAY_ITEM.get());
                    })
                    .build());

    public Godofthings(FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        MENUS.register(modEventBus);
        RECIPE_SERIALIZERS.register(modEventBus);
        GodFlatDimension.CHUNK_GENERATORS.register(modEventBus);

        // 神之机器参数配置（矿机/资源机/掉落机，godofthings-machines.toml）
        // 显式指定文件名，避免依赖默认命名规则（默认 godofthings-server.toml）。
        context.registerConfig(ModConfig.Type.SERVER, com.godofthings.config.MachinesConfig.SPEC, "godofthings-machines.toml");

        modEventBus.addListener(this::commonSetup);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        event.enqueueWork(WandMessages::register);
        event.enqueueWork(this::registerGridLinkables);
        AdAstraCompat.init(); // Ad Astra 未安装时自动跳过
        LOGGER.info("Godofthings loaded");
    }

    /** 注册造化垂青之杖到 AE2 无线访问点链接（AE2 未安装时跳过）。 */
    private void registerGridLinkables()
    {
        if (!net.minecraftforge.fml.ModList.get().isLoaded("ae2"))
        {
            return;
        }
        try
        {
            appeng.api.features.GridLinkables.register(GOD_FAVOR_WAND.get(), GodFavorWandAe2Helper.LINKABLE_HANDLER);
            appeng.api.features.GridLinkables.register(GOD_FAVOR_WAND_WRENCH.get(), GodFavorWandAe2Helper.LINKABLE_HANDLER);
            appeng.api.features.GridLinkables.register(GOD_FAVOR_WAND_SCREWDRIVER.get(), GodFavorWandAe2Helper.LINKABLE_HANDLER);
            appeng.api.features.GridLinkables.register(GOD_FAVOR_WAND_MALLET.get(), GodFavorWandAe2Helper.LINKABLE_HANDLER);
            appeng.api.features.GridLinkables.register(GOD_FAVOR_WAND_CROWBAR.get(), GodFavorWandAe2Helper.LINKABLE_HANDLER);
            appeng.api.features.GridLinkables.register(GOD_FAVOR_WAND_HAMMER.get(), GodFavorWandAe2Helper.LINKABLE_HANDLER);
        }
        catch (Exception e)
        {
            // AE2 未正确加载时忽略，避免影响模组加载
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        LOGGER.info("Godofthings server starting");
    }
}
