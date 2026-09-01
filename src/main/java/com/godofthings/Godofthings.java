package com.godofthings;

import com.godofthings.block.CreatureAnnihilationBlock;
import com.godofthings.block.DimensionTeleporterBlock;
import com.godofthings.block.GodCraftBlock;
import com.godofthings.block.GodDropBlock;
import com.godofthings.block.GodEnchantBlock;
import com.godofthings.block.GodFurnaceBlock;
import com.godofthings.block.GodHeavenEnchantBlock;
import com.godofthings.block.GodMinerBlock;
import com.godofthings.block.GodRecordBlock;
import com.godofthings.block.GodResourceBlock;
import com.godofthings.block.SpaceTimeEternityBlock;
import com.godofthings.block.entity.CreatureAnnihilationBlockEntity;
import com.godofthings.block.entity.GodCraftBlockEntity;
import com.godofthings.block.entity.GodDropBlockEntity;
import com.godofthings.block.entity.GodEnchantBlockEntity;
import com.godofthings.block.entity.GodFurnaceBlockEntity;
import com.godofthings.block.entity.GodMinerBlockEntity;
import com.godofthings.block.entity.GodRecordBlockEntity;
import com.godofthings.block.entity.GodResourceBlockEntity;
import com.godofthings.block.entity.SpaceTimeEternityBlockEntity;
import com.godofthings.config.MachinesConfig;
import com.godofthings.dimension.GodFlatDimension;
import com.godofthings.energy.CreativeEnergyCubeBlock;
import com.godofthings.energy.CreativeEnergyCubeEntity;
import com.godofthings.energy.CreativeEnergyCubeMenu;
import com.godofthings.handler.AdAstraCompat;
import com.godofthings.handler.GodFavorWandAe2Helper;
import com.godofthings.item.GodAcceleratorItem;
import com.godofthings.item.GodArmorItem;
import com.godofthings.item.GodCannonItem;
import com.godofthings.item.GodChangeItem;
import com.godofthings.item.GodFavorWandItem;
import com.godofthings.item.GodInviteItem;
import com.godofthings.item.GodMinerItem;
import com.godofthings.item.GodSwordItem;
import com.godofthings.item.GodUnbreakableItem;
import com.godofthings.menu.GodChangeMenu;
import com.godofthings.menu.GodCraftConfigMenu;
import com.godofthings.menu.GodCraftMenu;
import com.godofthings.menu.GodCraftTemplateMenu;
import com.godofthings.menu.GodDropMenu;
import com.godofthings.menu.GodEnchantMenu;
import com.godofthings.menu.GodFurnaceConfigMenu;
import com.godofthings.menu.GodFurnaceMenu;
import com.godofthings.menu.GodMinerMenu;
import com.godofthings.menu.GodRecordMenu;
import com.godofthings.menu.GodResourceMenu;
import com.godofthings.menu.WaypointMenu;
import com.godofthings.recipe.GodUnbreakableRecipe;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ArmorItem;
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
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

@Mod(Godofthings.MODID)
public class Godofthings
{
    public static final String MODID = "godofthings";
    /** 天神附魔的附魔等级上限（突破原版，类似 give 指令） */
    public static final int HEAVENLY_ENCHANT_MAX_LEVEL = 255;
    private static final Logger LOGGER = LogUtils.getLogger();

    // ---- 维度 ----
    public static final ResourceKey<Level> SUPERFLAT_DIMENSION =
            ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(MODID, "superflat"));
    public static final ResourceKey<Level> VOID_DIMENSION =
            ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(MODID, "void"));

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, MODID);

    // ---- 方块 ----
    // 注意：不能用 BlockBehaviour.Properties.copy(Blocks.FURNACE) —— 会连带原版熔炉的
    // `lit` 状态属性，而本方块未定义该属性，注册时直接崩溃。因此手动构造属性。
    public static final DeferredBlock<GodFurnaceBlock> GOD_FURNACE = BLOCKS.registerBlock("god_furnace",
            GodFurnaceBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.5F, 3.5F)
                    .sound(SoundType.STONE));
    public static final DeferredItem<BlockItem> GOD_FURNACE_ITEM =
            ITEMS.registerSimpleBlockItem(GOD_FURNACE, new Item.Properties());

    // ---- 神之矿机 ----
    public static final DeferredBlock<GodMinerBlock> GOD_MINER = BLOCKS.registerBlock("god_miner",
            GodMinerBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(5.0F, 6.0F)
                    .sound(SoundType.METAL));
    public static final DeferredItem<GodMinerItem> GOD_MINER_ITEM =
            ITEMS.registerItem("god_miner", props -> new GodMinerItem(GOD_MINER.get(), props.stacksTo(1)));

    // ---- 神之资源 ----
    public static final DeferredBlock<GodResourceBlock> GOD_RESOURCE = BLOCKS.registerBlock("god_resource",
            GodResourceBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_YELLOW)
                    .strength(5.0F, 6.0F)
                    .sound(SoundType.METAL));
    public static final DeferredItem<BlockItem> GOD_RESOURCE_ITEM =
            ITEMS.registerSimpleBlockItem(GOD_RESOURCE, new Item.Properties());

    // ---- 神之掉落 ----
    public static final DeferredBlock<GodDropBlock> GOD_DROP = BLOCKS.registerBlock("god_drop",
            GodDropBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(5.0F, 6.0F)
                    .sound(SoundType.METAL));
    public static final DeferredItem<BlockItem> GOD_DROP_ITEM =
            ITEMS.registerSimpleBlockItem(GOD_DROP, new Item.Properties());

    // ---- 神之附魔 ----
    public static final DeferredBlock<GodEnchantBlock> GOD_ENCHANT = BLOCKS.registerBlock("god_enchant",
            GodEnchantBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(3.0F, 6.0F)
                    .sound(SoundType.STONE));
    public static final DeferredItem<BlockItem> GOD_ENCHANT_ITEM =
            ITEMS.registerSimpleBlockItem(GOD_ENCHANT, new Item.Properties());
    public static final DeferredBlock<GodHeavenEnchantBlock> GOD_HEAVEN_ENCHANT =
            BLOCKS.registerBlock("god_heaven_enchant", GodHeavenEnchantBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_LIGHT_BLUE)
                            .strength(3.0F, 6.0F)
                            .sound(SoundType.STONE));
    public static final DeferredItem<BlockItem> GOD_HEAVEN_ENCHANT_ITEM =
            ITEMS.registerSimpleBlockItem(GOD_HEAVEN_ENCHANT, new Item.Properties());

    // ---- 神装 ----
    // 1.21.1 ArmorItem 不再自动设置耐久，需显式 .durability(...)（下界合金系数 37）
    public static final DeferredItem<GodArmorItem> GOD_HELMET = ITEMS.registerItem("god_helmet",
            props -> new GodArmorItem(ArmorItem.Type.HELMET, props));
    public static final DeferredItem<GodArmorItem> GOD_CHESTPLATE = ITEMS.registerItem("god_chestplate",
            props -> new GodArmorItem(ArmorItem.Type.CHESTPLATE, props));
    public static final DeferredItem<GodArmorItem> GOD_LEGGINGS = ITEMS.registerItem("god_leggings",
            props -> new GodArmorItem(ArmorItem.Type.LEGGINGS, props));
    public static final DeferredItem<GodArmorItem> GOD_BOOTS = ITEMS.registerItem("god_boots",
            props -> new GodArmorItem(ArmorItem.Type.BOOTS, props));

    // ---- 神之不毁 ----
    public static final DeferredItem<GodUnbreakableItem> GOD_UNBREAKABLE =
            ITEMS.registerItem("god_unbreakable", GodUnbreakableItem::new);

    // ---- 神之加速（放入神之系列机器加速槽，提升并行数量）----
    public static final DeferredItem<GodAcceleratorItem> GOD_ACCELERATOR =
            ITEMS.registerItem("god_accelerator", GodAcceleratorItem::new);

    // ---- 神之剑（代码层面秒杀）----
    public static final DeferredItem<GodSwordItem> GOD_SWORD =
            ITEMS.registerItem("god_sword", GodSwordItem::new);

    // ---- 神之炮（电磁炮：左键贯穿光束 / 右键三层蓄力范围炮）----
    public static final DeferredItem<GodCannonItem> GOD_CANNON =
            ITEMS.registerItem("god_cannon", GodCannonItem::new);

    // ---- 请神（对生物右键使用，使其无限血量）----
    public static final DeferredItem<GodInviteItem> GOD_INVITE =
            ITEMS.registerItem("god_invite", GodInviteItem::new);

    // ---- 神之工具 ----
    public static final DeferredItem<GodFavorWandItem> GOD_FAVOR_WAND =
            ITEMS.registerItem("god_favor_wand", GodFavorWandItem::new);
    // GT 扳手模式子类（通过模式轮盘切换，不直接出现在创造标签）
    public static final DeferredItem<GodFavorWandItem> GOD_FAVOR_WAND_WRENCH =
            ITEMS.registerItem("god_favor_wand_wrench", GodFavorWandItem::new);
    public static final DeferredItem<GodFavorWandItem> GOD_FAVOR_WAND_SCREWDRIVER =
            ITEMS.registerItem("god_favor_wand_screwdriver", GodFavorWandItem::new);
    public static final DeferredItem<GodFavorWandItem> GOD_FAVOR_WAND_MALLET =
            ITEMS.registerItem("god_favor_wand_mallet", GodFavorWandItem::new);
    public static final DeferredItem<GodFavorWandItem> GOD_FAVOR_WAND_CROWBAR =
            ITEMS.registerItem("god_favor_wand_crowbar", GodFavorWandItem::new);
    public static final DeferredItem<GodFavorWandItem> GOD_FAVOR_WAND_HAMMER =
            ITEMS.registerItem("god_favor_wand_hammer", GodFavorWandItem::new);

    // ---- 维度传送器（方块）----
    public static final DeferredBlock<DimensionTeleporterBlock> SUPERFLAT_TELEPORTER =
            BLOCKS.registerBlock("superflat_teleporter",
                    props -> new DimensionTeleporterBlock(SUPERFLAT_DIMENSION, "message.godofthings.tp_superflat", props),
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .strength(2.0F, 65536.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.STONE));
    public static final DeferredItem<BlockItem> SUPERFLAT_TELEPORTER_ITEM =
            ITEMS.registerSimpleBlockItem(SUPERFLAT_TELEPORTER, new Item.Properties());

    public static final DeferredBlock<DimensionTeleporterBlock> VOID_TELEPORTER =
            BLOCKS.registerBlock("void_teleporter",
                    props -> new DimensionTeleporterBlock(VOID_DIMENSION, "message.godofthings.tp_void", props),
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .strength(2.0F, 65536.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.STONE));
    public static final DeferredItem<BlockItem> VOID_TELEPORTER_ITEM =
            ITEMS.registerSimpleBlockItem(VOID_TELEPORTER, new Item.Properties());

    // ---- 创造能量立方（无限 FE 输出 + 物品充能）----
    public static final DeferredBlock<CreativeEnergyCubeBlock> CREATIVE_ENERGY_CUBE =
            BLOCKS.registerBlock("creative_energy_cube", CreativeEnergyCubeBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_PURPLE)
                            .pushReaction(PushReaction.DESTROY)
                            .strength(5.0F, 1200.0F)
                            .lightLevel(state -> 15)
                            .sound(SoundType.AMETHYST));
    public static final DeferredItem<BlockItem> CREATIVE_ENERGY_CUBE_ITEM =
            ITEMS.registerSimpleBlockItem(CREATIVE_ENERGY_CUBE, new Item.Properties());
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CreativeEnergyCubeEntity>> CREATIVE_ENERGY_CUBE_BE =
            BLOCK_ENTITIES.register("creative_energy_cube",
                    () -> BlockEntityType.Builder.of(CreativeEnergyCubeEntity::new, CREATIVE_ENERGY_CUBE.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<CreativeEnergyCubeMenu>> CREATIVE_ENERGY_CUBE_MENU =
            MENUS.register("creative_energy_cube",
                    () -> IMenuTypeExtension.create((id, inv, buf) -> new CreativeEnergyCubeMenu(id, inv, buf.readBlockPos())));

    // ---- 神之更改 ----
    public static final DeferredItem<GodChangeItem> GOD_CHANGE =
            ITEMS.registerItem("god_change", props -> new GodChangeItem(props.stacksTo(1)));

    // ---- 神之合成 ----
    public static final DeferredBlock<GodCraftBlock> GOD_CRAFT = BLOCKS.registerBlock("god_craft",
            GodCraftBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(3.0F, 6.0F)
                    .sound(SoundType.WOOD));
    public static final DeferredItem<BlockItem> GOD_CRAFT_ITEM =
            ITEMS.registerSimpleBlockItem(GOD_CRAFT, new Item.Properties());
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GodCraftBlockEntity>> GOD_CRAFT_BE =
            BLOCK_ENTITIES.register("god_craft",
                    () -> BlockEntityType.Builder.of(GodCraftBlockEntity::new, GOD_CRAFT.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<GodCraftMenu>> GOD_CRAFT_MENU =
            MENUS.register("god_craft", () -> IMenuTypeExtension.create(GodCraftMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<GodCraftConfigMenu>> GOD_CRAFT_CONFIG_MENU =
            MENUS.register("god_craft_config", () -> IMenuTypeExtension.create(GodCraftConfigMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<GodCraftTemplateMenu>> GOD_CRAFT_TEMPLATE_MENU =
            MENUS.register("god_craft_templates", () -> IMenuTypeExtension.create(GodCraftTemplateMenu::new));

    // ---- 时空永恒（放下后世界时间与天气永久锁定当前状态）----
    public static final DeferredBlock<SpaceTimeEternityBlock> SPACE_TIME_ETERNITY =
            BLOCKS.registerBlock("space_time_eternity", SpaceTimeEternityBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_CYAN)
                            .pushReaction(PushReaction.DESTROY)
                            .strength(5.0F, 1200.0F)
                            .lightLevel(state -> 15)
                            .sound(SoundType.AMETHYST));
    public static final DeferredItem<BlockItem> SPACE_TIME_ETERNITY_ITEM =
            ITEMS.registerSimpleBlockItem(SPACE_TIME_ETERNITY, new Item.Properties());
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SpaceTimeEternityBlockEntity>> SPACE_TIME_ETERNITY_BE =
            BLOCK_ENTITIES.register("space_time_eternity",
                    () -> BlockEntityType.Builder.of(SpaceTimeEternityBlockEntity::new, SPACE_TIME_ETERNITY.get()).build(null));

    // ---- 生物覆灭（放下后半径 512 格内无生物自然生成）----
    public static final DeferredBlock<CreatureAnnihilationBlock> CREATURE_ANNIHILATION =
            BLOCKS.registerBlock("creature_annihilation", CreatureAnnihilationBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_RED)
                            .pushReaction(PushReaction.DESTROY)
                            .strength(5.0F, 1200.0F)
                            .lightLevel(state -> 15)
                            .sound(SoundType.METAL));
    public static final DeferredItem<BlockItem> CREATURE_ANNIHILATION_ITEM =
            ITEMS.registerSimpleBlockItem(CREATURE_ANNIHILATION, new Item.Properties());
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CreatureAnnihilationBlockEntity>> CREATURE_ANNIHILATION_BE =
            BLOCK_ENTITIES.register("creature_annihilation",
                    () -> BlockEntityType.Builder.of(CreatureAnnihilationBlockEntity::new, CREATURE_ANNIHILATION.get()).build(null));

    // ---- 神之记录（传送点管理 UI）----
    public static final DeferredBlock<GodRecordBlock> GOD_RECORD =
            BLOCKS.registerBlock("god_record", GodRecordBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_BROWN)
                            .strength(3.0F, 6.0F)
                            .sound(SoundType.WOOD));
    public static final DeferredItem<BlockItem> GOD_RECORD_ITEM =
            ITEMS.registerSimpleBlockItem(GOD_RECORD, new Item.Properties());
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GodRecordBlockEntity>> GOD_RECORD_BE =
            BLOCK_ENTITIES.register("god_record",
                    () -> BlockEntityType.Builder.of(GodRecordBlockEntity::new, GOD_RECORD.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<GodRecordMenu>> GOD_RECORD_MENU =
            MENUS.register("god_record", () -> IMenuTypeExtension.create(GodRecordMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<WaypointMenu>> WAYPOINT_MENU =
            MENUS.register("waypoint", () -> IMenuTypeExtension.create(WaypointMenu::new));

    // ---- 方块实体 ----
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GodFurnaceBlockEntity>> GOD_FURNACE_BE =
            BLOCK_ENTITIES.register("god_furnace",
                    () -> BlockEntityType.Builder.of(GodFurnaceBlockEntity::new, GOD_FURNACE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GodMinerBlockEntity>> GOD_MINER_BE =
            BLOCK_ENTITIES.register("god_miner",
                    () -> BlockEntityType.Builder.of(GodMinerBlockEntity::new, GOD_MINER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GodResourceBlockEntity>> GOD_RESOURCE_BE =
            BLOCK_ENTITIES.register("god_resource",
                    () -> BlockEntityType.Builder.of(GodResourceBlockEntity::new, GOD_RESOURCE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GodDropBlockEntity>> GOD_DROP_BE =
            BLOCK_ENTITIES.register("god_drop",
                    () -> BlockEntityType.Builder.of(GodDropBlockEntity::new, GOD_DROP.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GodEnchantBlockEntity>> GOD_ENCHANT_BE =
            BLOCK_ENTITIES.register("god_enchant",
                    () -> BlockEntityType.Builder.of(GodEnchantBlockEntity::new,
                            GOD_ENCHANT.get(), GOD_HEAVEN_ENCHANT.get()).build(null));

    // ---- 菜单 ----
    public static final DeferredHolder<MenuType<?>, MenuType<GodFurnaceMenu>> GOD_FURNACE_MENU =
            MENUS.register("god_furnace", () -> IMenuTypeExtension.create(GodFurnaceMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<GodFurnaceConfigMenu>> GOD_FURNACE_CONFIG_MENU =
            MENUS.register("god_furnace_config", () -> IMenuTypeExtension.create(GodFurnaceConfigMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<GodMinerMenu>> GOD_MINER_MENU =
            MENUS.register("god_miner", () -> IMenuTypeExtension.create(GodMinerMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<GodResourceMenu>> GOD_RESOURCE_MENU =
            MENUS.register("god_resource", () -> IMenuTypeExtension.create(GodResourceMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<GodDropMenu>> GOD_DROP_MENU =
            MENUS.register("god_drop", () -> IMenuTypeExtension.create(GodDropMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<GodEnchantMenu>> GOD_ENCHANT_MENU =
            MENUS.register("god_enchant", () -> IMenuTypeExtension.create(GodEnchantMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<GodChangeMenu>> GOD_CHANGE_MENU =
            MENUS.register("god_change", () -> IMenuTypeExtension.create(GodChangeMenu::new));

    // ---- 神之不毁配方序列化器（使用 DeferredHolder 常规注册）----
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<?>> GOD_UNBREAKABLE_SERIALIZER =
            RECIPE_SERIALIZERS.register("god_unbreakable", GodUnbreakableRecipe.Serializer::new);

    // ---- 创造标签 ----
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> GOD_OF_THINGS_TAB =
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
                        output.accept(GOD_ACCELERATOR.get());
                        output.accept(GOD_SWORD.get());
                        output.accept(GOD_CANNON.get());
                        output.accept(GOD_INVITE.get());
                        output.accept(GOD_CHANGE.get());
                        output.accept(GOD_CRAFT_ITEM.get());
                        output.accept(SUPERFLAT_TELEPORTER_ITEM.get());
                        output.accept(VOID_TELEPORTER_ITEM.get());
                        output.accept(CREATIVE_ENERGY_CUBE_ITEM.get());
                        output.accept(SPACE_TIME_ETERNITY_ITEM.get());
                        output.accept(CREATURE_ANNIHILATION_ITEM.get());
                        output.accept(GOD_RECORD_ITEM.get());
                    })
                    .build());

    public Godofthings(IEventBus modEventBus, ModContainer modContainer)
    {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        MENUS.register(modEventBus);
        RECIPE_SERIALIZERS.register(modEventBus);
        GodFlatDimension.CHUNK_GENERATORS.register(modEventBus);

        // 神之机器参数配置（矿机/资源机/掉落机，godofthings-machines.toml）
        // 显式指定文件名，避免依赖默认命名规则（默认 godofthings-server.toml）。
        modContainer.registerConfig(ModConfig.Type.SERVER, MachinesConfig.SPEC, "godofthings-machines.toml");

        modEventBus.addListener(this::commonSetup);

        NeoForge.EVENT_BUS.addListener(this::onServerStarting);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        event.enqueueWork(this::registerGridLinkables);
        AdAstraCompat.init(); // Ad Astra 未安装时自动跳过
        LOGGER.info("Godofthings loaded");
    }

    /** 注册神之工具到 AE2 无线访问点链接（AE2 未安装时跳过）。 */
    private void registerGridLinkables()
    {
        if (!ModList.get().isLoaded("ae2"))
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

    private void onServerStarting(ServerStartingEvent event)
    {
        LOGGER.info("Godofthings server starting");
    }
}
