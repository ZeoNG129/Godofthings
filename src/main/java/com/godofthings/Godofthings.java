package com.godofthings;

import com.godofthings.block.CreatureAnnihilationBlock;
import com.godofthings.block.DimensionTeleporterBlock;
import com.godofthings.block.GodCraftBlock;
import com.godofthings.block.GodDevourerBlock;
import com.godofthings.block.GodDropBlock;
import com.godofthings.block.GodEnchantBlock;
import com.godofthings.block.GodFurnaceBlock;
import com.godofthings.block.GodMinerBlock;
import com.godofthings.block.GodPeelerBlock;
import com.godofthings.block.GodResourceBlock;
import com.godofthings.block.GodSpawnEggBlock;
import com.godofthings.block.GodAbsorberBlock;
import com.godofthings.block.GodSlaughterBlock;
import com.godofthings.block.GodTransmitterBlock;
import com.godofthings.block.SpaceTimeEternityBlock;
import com.godofthings.block.entity.CreatureAnnihilationBlockEntity;
import com.godofthings.block.entity.GodCraftBlockEntity;
import com.godofthings.block.entity.GodDevourerBlockEntity;
import com.godofthings.block.entity.GodDropBlockEntity;
import com.godofthings.block.entity.GodEnchantBlockEntity;
import com.godofthings.block.entity.GodFurnaceBlockEntity;
import com.godofthings.block.entity.GodMinerBlockEntity;
import com.godofthings.block.entity.GodPeelerBlockEntity;
import com.godofthings.block.entity.GodResourceBlockEntity;
import com.godofthings.block.entity.GodSpawnEggBlockEntity;
import com.godofthings.block.entity.GodAbsorberBlockEntity;
import com.godofthings.block.entity.GodSlaughterBlockEntity;
import com.godofthings.block.entity.GodTransmitterBlockEntity;
import com.godofthings.block.entity.SpaceTimeEternityBlockEntity;
import com.godofthings.armor.GodArmorFeatures;
import com.godofthings.backpack.GodBackpackContents;
import com.godofthings.backpack.GodBackpackItem;
import com.godofthings.backpack.GodBackpackMenu;
import com.godofthings.backpack.GodBackpackSettings;
import com.godofthings.config.MachinesConfig;
import com.godofthings.construction.ConstructionTier;
import com.godofthings.construction.GodConstructionItem;
import com.godofthings.energy.CreativeEnergyCubeBlock;
import com.godofthings.energy.CreativeEnergyCubeEntity;
import com.godofthings.energy.CreativeEnergyCubeMenu;

import com.godofthings.belt.BeltRegistration;
import com.godofthings.belt.ConfigData;
import com.godofthings.belt.ToolBeltItem;
import com.godofthings.belt.common.BeltContainer;
import com.godofthings.belt.slot.BeltAttachment;
import com.godofthings.belt.slot.BeltSlotMenu;
import net.minecraft.world.flag.FeatureFlags;
import com.godofthings.handler.AdAstraCompat;
import com.godofthings.item.GodAcceleratorItem;
import com.godofthings.item.GodArmorItem;
import com.godofthings.item.GodBinderItem;
import com.godofthings.item.GodBlackBoxItem;
import com.godofthings.item.GodChangeItem;
import com.godofthings.item.GodConfigData;
import com.godofthings.item.GodConfigItem;
import com.godofthings.item.GodInviteItem;
import com.godofthings.item.GodMinerItem;
import com.godofthings.item.GodNoteItem;
import com.godofthings.item.GodRecipeItem;
import com.godofthings.item.GodUnbreakableItem;
import com.godofthings.infinitecell.UniversalCellInventory;
import com.godofthings.infinitecell.UniversalCellItem;
import com.godofthings.measurement.GodMeasureItem;
import appeng.api.client.StorageCellModels;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.StorageCells;
import appeng.api.util.AEColor;
import appeng.core.definitions.AEItems;
import appeng.core.definitions.AEParts;
import com.godofthings.menu.GodBlackBoxMenu;
import com.godofthings.menu.GodChangeMenu;
import com.godofthings.menu.GodCraftConfigMenu;
import com.godofthings.menu.GodCraftMenu;
import com.godofthings.menu.GodCraftTemplateMenu;
import com.godofthings.menu.GodDevourerMenu;
import com.godofthings.menu.GodDropMenu;
import com.godofthings.menu.GodDropConfigMenu;
import com.godofthings.menu.GodEnchantMenu;
import com.godofthings.menu.GodFurnaceConfigMenu;
import com.godofthings.menu.GodFurnaceMenu;
import com.godofthings.menu.GodMinerMenu;
import com.godofthings.menu.GodMinerConfigMenu;
import com.godofthings.menu.GodPeelerConfigMenu;
import com.godofthings.menu.GodPeelerMenu;
import com.godofthings.menu.GodResourceMenu;
import com.godofthings.menu.GodResourceConfigMenu;
import com.godofthings.menu.GodSpawnEggMenu;
import com.godofthings.menu.GodSpawnEggConfigMenu;
import com.godofthings.menu.GodSlaughterConfigMenu;
import com.godofthings.menu.GodAbsorberConfigMenu;
import com.godofthings.menu.GodAbsorberMenu;
import com.godofthings.menu.GodSlaughterMenu;
import com.godofthings.menu.GodTransmitterMenu;
import com.godofthings.menu.WaypointMenu;
import com.godofthings.recipe.GodUnbreakableRecipe;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.slf4j.Logger;

@Mod(Godofthings.MODID)
public class Godofthings
{
    public static final String MODID = "godofthings";
    /** 天神附魔的附魔等级上限（突破原版，类似 give 指令） */
    public static final int HEAVENLY_ENCHANT_MAX_LEVEL = 255;
    private static final Logger LOGGER = LogUtils.getLogger();

    // ---- 维度 ----
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
    /** 物品数据组件（神之背包的内容与设置；带持久化 + 网络同步 codec，掉地上 / 进箱子都不丢） */
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MODID);

    /** 神之套装功能开关：按玩家保存的位图（见 GodArmorFeatures / GodArmorState） */
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> ARMOR_FEATURES =
            ATTACHMENT_TYPES.register("armor_features", () -> AttachmentType.builder(() -> GodArmorFeatures.ALL)
                    .serialize(Codec.INT)
                    .copyOnDeath()
                    .build());

    /**
     * 神之套装功能开关（v2 格式）：v5.9.0 把 16 个开关合并成 8 个后启用的新键。
     *
     * <p>默认 {@code -1} = 「还没写过」，用来区分「老存档（要迁移）」与「玩家真的选了 0（全关）」——
     * 读的时候是 -1 就把旧的 {@link #ARMOR_FEATURES} 按合并规则迁移过来（见 {@link GodArmorState}）。</p>
     */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> ARMOR_FEATURES_V2 =
            ATTACHMENT_TYPES.register("armor_features_v2", () -> AttachmentType.builder(() -> -1)
                    .serialize(Codec.INT)
                    .copyOnDeath()
                    .build());

    /** 神之套装技能树：按玩家保存的「技能 id → 等级」表（见 ArmorSkills / ArmorSkillData） */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<java.util.Map<String, Integer>>> ARMOR_SKILLS =
            ATTACHMENT_TYPES.register("armor_skills", () -> AttachmentType.<java.util.Map<String, Integer>>builder(
                            () -> new java.util.HashMap<String, Integer>())
                    .serialize(Codec.unboundedMap(Codec.STRING, Codec.INT))
                    .copyOnDeath()
                    .build());

    // ---- 神之背包的数据组件 ----
    // 内容 120 格 + 设置（记忆格 / 忽略整理格 / 排序方式 / 开关 / 搜索词），都跟着物品走：
    // persistent = 存档 NBT，networkSynchronized = 物品栏同步时一起发给客户端。
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GodBackpackContents>> GOD_BACKPACK_CONTENTS =
            DATA_COMPONENTS.register("god_backpack_contents", () -> DataComponentType.<GodBackpackContents>builder()
                    .persistent(GodBackpackContents.CODEC)
                    .networkSynchronized(GodBackpackContents.STREAM_CODEC)
                    .build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GodBackpackSettings>> GOD_BACKPACK_SETTINGS =
            DATA_COMPONENTS.register("god_backpack_settings", () -> DataComponentType.<GodBackpackSettings>builder()
                    .persistent(GodBackpackSettings.CODEC)
                    .networkSynchronized(GodBackpackSettings.STREAM_CODEC)
                    .build());

    // ---- 神之配置的数据组件 ----
    // 一台机器的配置模板：方块注册名 + 六面 I/O 模式 + AE 并网开关 + 加速槽里的神之加速数量
    // （只记数量不记物品，粘贴时从玩家物品栏里扣真正的神之加速，见 GodConfigItem）。
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GodConfigData>> GOD_CONFIG_DATA =
            DATA_COMPONENTS.register("god_config", () -> DataComponentType.<GodConfigData>builder()
                    .persistent(GodConfigData.CODEC)
                    .networkSynchronized(GodConfigData.STREAM_CODEC)
                    .build());

    // ---- 机器的拆装状态（v5.20.6）----
    // 挖掉机器时把「六面 I/O + AE 开关 + 加速数量」写进掉落的方块物品，放下时还原
    // （对齐 Mekanism 的拆机手感；加速跟着机器走，不再单独掉成物品，见 MachineStateHandler）。
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GodConfigData>> MACHINE_STATE_DATA =
            DATA_COMPONENTS.register("machine_state", () -> DataComponentType.<GodConfigData>builder()
                    .persistent(GodConfigData.CODEC)
                    .networkSynchronized(GodConfigData.STREAM_CODEC)
                    .build());

    // ---- 神之建筑的数据组件 ----
    // 当前模式：0 = 建造 / 1 = 销毁 / 2 = 替换（潜行右键方块切换，见 construction 包的 GodConstructionItem）。
    // persistent = 跟着物品存盘，networkSynchronized = 物品栏同步时一起发给客户端。
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> GOD_CONSTRUCTION_MODE =
            DATA_COMPONENTS.register("god_construction_mode", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    // 预览开关（默认开）：手持时对着方块画出「将要铺下去的格子」线框（见 construction.client 包）。
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> GOD_CONSTRUCTION_PREVIEW =
            DATA_COMPONENTS.register("god_construction_preview", () -> DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .build());
    // 相似方块开关（默认关）：建造时「背后那一格」的判定从「同种方块」放宽到「同种或相似方块」
    // （按被点方块的物品标签收集，跳过 mineable* 标签，见 ConstructionActions#similarBlocks）。
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> GOD_CONSTRUCTION_SIMILAR =
            DATA_COMPONENTS.register("god_construction_similar", () -> DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .build());

    // ---- 自然罗盘数据组件（v5.23.0）----
    // 必须写在 Godofthings 自己的静态初始化里：DeferredRegister 一旦被延迟到
    // PermissionGatherEvent（服务端启动期）才首次加载，register() 会抛
    // 「Cannot register new entries to DeferredRegister after RegisterEvent has been fired」。
    // 所以这里只是「存放处」，NaturesCompassData 里用别名指向它们。
    /** 目标群系的资源位置字符串（如 {@code minecraft:plains}） */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> NATURES_COMPASS_BIOME_ID =
            DATA_COMPONENTS.register("natures_compass_biome_id", () -> DataComponentType.<String>builder()
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                    .build());
    /** 状态机（0 空闲 / 1 搜索中 / 2 已找到 / 3 未找到，见 CompassState） */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> NATURES_COMPASS_STATE =
            DATA_COMPONENTS.register("natures_compass_state", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());
    /** 找到的群系的 X 坐标 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> NATURES_COMPASS_FOUND_X =
            DATA_COMPONENTS.register("natures_compass_found_x", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());
    /** 找到的群系的 Z 坐标 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> NATURES_COMPASS_FOUND_Z =
            DATA_COMPONENTS.register("natures_compass_found_z", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());
    /** 搜索半径（搜索过程中不断刷新，用于 HUD 显示） */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> NATURES_COMPASS_SEARCH_RADIUS =
            DATA_COMPONENTS.register("natures_compass_search_radius", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());
    /** 已采样次数 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> NATURES_COMPASS_SAMPLES =
            DATA_COMPONENTS.register("natures_compass_samples", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());
    /** HUD 上是否显示精确坐标与距离（服务端按配置写，客户端只读） */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> NATURES_COMPASS_DISPLAY_COORDS =
            DATA_COMPONENTS.register("natures_compass_display_coords", () -> DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .build());
    /** 已经找到过的群系坐标（「搜索下一个」用来跳过已找到的、以及判断是否连通） */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<java.util.List<net.minecraft.core.BlockPos>>> NATURES_COMPASS_PREV_POS =
            DATA_COMPONENTS.register("natures_compass_prev_pos",
                    () -> DataComponentType.<java.util.List<net.minecraft.core.BlockPos>>builder()
                            .persistent(net.minecraft.core.BlockPos.CODEC.listOf().xmap(java.util.ArrayList::new, list -> list))
                            .networkSynchronized(ByteBufCodecs.collection(java.util.ArrayList::new, net.minecraft.core.BlockPos.STREAM_CODEC))
                            .build());
    /** 已用次数（配合配置项 compassDurability 做「用几次就坏，要拿指南针修」） */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> NATURES_COMPASS_DAMAGE =
            DATA_COMPONENTS.register("natures_compass_damage", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());
    // ---- 纬紬玩偶的数据组件（移植自 useless_stretcher 的 StretcherComponents 玩偶段）----
    // 注册必须发生在 Godofthings 的静态初始化里（仓库硬规则）：DollComponents 只是别名引用。
    // 若在这里之外首次加载该类，RegisterEvent 已结束，DeferredRegister.register 会直接抛
    // IllegalStateException（v5.23.0 与 v7.0.0 都在同一个坑里栽过）。
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> DOLL_TWO_I_ROTATION =
            DATA_COMPONENTS.register("two_i_rotation", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(StreamCodec.of(FriendlyByteBuf::writeVarInt, FriendlyByteBuf::readVarInt))
                    .build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> DOLL_TWO_I_GIFT_SCORE =
            DATA_COMPONENTS.register("two_i_gift_score", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(StreamCodec.of(FriendlyByteBuf::writeVarInt, FriendlyByteBuf::readVarInt))
                    .build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> DOLL_TWO_I_BOND =
            DATA_COMPONENTS.register("two_i_bond", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(StreamCodec.of(FriendlyByteBuf::writeVarInt, FriendlyByteBuf::readVarInt))
                    .build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<java.util.UUID>> DOLL_TWO_I_OWNER =
            DATA_COMPONENTS.register("two_i_owner", () -> DataComponentType.<java.util.UUID>builder()
                    .persistent(UUIDUtil.CODEC)
                    .networkSynchronized(StreamCodec.<FriendlyByteBuf, java.util.UUID>of((buf, value) -> buf.writeUUID(value), buf -> buf.readUUID()))
                    .build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<java.util.UUID>> DOLL_TWO_I_DOLL_ID =
            DATA_COMPONENTS.register("two_i_doll_id", () -> DataComponentType.<java.util.UUID>builder()
                    .persistent(UUIDUtil.CODEC)
                    .networkSynchronized(StreamCodec.<FriendlyByteBuf, java.util.UUID>of((buf, value) -> buf.writeUUID(value), buf -> buf.readUUID()))
                    .build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<com.godofthings.doll.entity.TwoIDollMemory>> DOLL_TWO_I_MEMORY =
            DATA_COMPONENTS.register("two_i_memory", () -> DataComponentType.<com.godofthings.doll.entity.TwoIDollMemory>builder()
                    .persistent(com.godofthings.doll.entity.TwoIDollMemory.CODEC)
                    .build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> DOLL_TWO_I_INVITATION_USED =
            DATA_COMPONENTS.register("two_i_invitation_used", () -> DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(StreamCodec.of(FriendlyByteBuf::writeBoolean, FriendlyByteBuf::readBoolean))
                    .build());

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

    // ---- 神之资源系列（v5.13.0：矿物 / 作物 / 复制三台，共用 BE 类型，输入过滤各不相同）----
    public static final DeferredBlock<GodResourceBlock> GOD_ORE_MACHINE = BLOCKS.registerBlock("god_ore_machine",
            props -> new GodResourceBlock(com.godofthings.block.entity.GodResourceVariant.ORE, props),
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_YELLOW)
                    .strength(5.0F, 6.0F)
                    .sound(SoundType.METAL));
    public static final DeferredItem<BlockItem> GOD_ORE_MACHINE_ITEM =
            ITEMS.registerSimpleBlockItem(GOD_ORE_MACHINE, new Item.Properties());

    public static final DeferredBlock<GodResourceBlock> GOD_CROP_MACHINE = BLOCKS.registerBlock("god_crop_machine",
            props -> new GodResourceBlock(com.godofthings.block.entity.GodResourceVariant.CROP, props),
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GREEN)
                    .strength(5.0F, 6.0F)
                    .sound(SoundType.METAL));
    public static final DeferredItem<BlockItem> GOD_CROP_MACHINE_ITEM =
            ITEMS.registerSimpleBlockItem(GOD_CROP_MACHINE, new Item.Properties());

    public static final DeferredBlock<GodResourceBlock> GOD_DUPLICATE_MACHINE = BLOCKS.registerBlock("god_duplicate_machine",
            props -> new GodResourceBlock(com.godofthings.block.entity.GodResourceVariant.DUPLICATE, props),
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(5.0F, 6.0F)
                    .sound(SoundType.METAL));
    public static final DeferredItem<BlockItem> GOD_DUPLICATE_MACHINE_ITEM =
            ITEMS.registerSimpleBlockItem(GOD_DUPLICATE_MACHINE, new Item.Properties());

    // ---- 神之去皮（原木进 → 对应去皮原木出，1 输入槽 + 1 输出槽）----
    public static final DeferredBlock<GodPeelerBlock> GOD_PEELER = BLOCKS.registerBlock("god_peeler",
            GodPeelerBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(5.0F, 6.0F)
                    .sound(SoundType.WOOD));
    public static final DeferredItem<BlockItem> GOD_PEELER_ITEM =
            ITEMS.registerSimpleBlockItem(GOD_PEELER, new Item.Properties());

    // ---- 神之掉落 ----
    public static final DeferredBlock<GodDropBlock> GOD_DROP = BLOCKS.registerBlock("god_drop",
            GodDropBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(5.0F, 6.0F)
                    .sound(SoundType.METAL));
    public static final DeferredItem<BlockItem> GOD_DROP_ITEM =
            ITEMS.registerSimpleBlockItem(GOD_DROP, new Item.Properties());

    // ---- 神之怪蛋（刷怪蛋复制机，任意模组刷怪蛋都能复制）----
    public static final DeferredBlock<GodSpawnEggBlock> GOD_SPAWN_EGG = BLOCKS.registerBlock("god_spawn_egg",
            GodSpawnEggBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(5.0F, 6.0F)
                    .sound(SoundType.METAL));
    public static final DeferredItem<BlockItem> GOD_SPAWN_EGG_ITEM =
            ITEMS.registerSimpleBlockItem(GOD_SPAWN_EGG, new Item.Properties());


    // ---- 神之附魔（v5.13.0：天神附魔并入，最高 255 级）----
    public static final DeferredBlock<GodEnchantBlock> GOD_ENCHANT = BLOCKS.registerBlock("god_enchant",
            GodEnchantBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(3.0F, 6.0F)
                    .sound(SoundType.STONE));
    public static final DeferredItem<BlockItem> GOD_ENCHANT_ITEM =
            ITEMS.registerSimpleBlockItem(GOD_ENCHANT, new Item.Properties());

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

    // ---- 神之炮（电磁炮：左键贯穿光束 / 右键三层蓄力范围炮）----

    // ---- 请神（对生物右键使用，使其无限血量）----
    public static final DeferredItem<GodInviteItem> GOD_INVITE =
            ITEMS.registerItem("god_invite", GodInviteItem::new);

    // ---- 神之黑盒（拾取过滤/无限储存）----
    public static final DeferredItem<GodBlackBoxItem> GOD_BLACK_BOX =
            ITEMS.registerItem("god_black_box", GodBlackBoxItem::new);
    public static final DeferredItem<GodBinderItem> GOD_BINDER =
            ITEMS.registerItem("god_binder", GodBinderItem::new);

    /**
     * 神之建筑：建筑棒「下界合金棒」的精简移植（无耐久，永久生效）。
     * 潜行右键方块切模式（建造 / 销毁 / 替换），建造是平面扩展铺同种方块，销毁不掉落，替换用副手方块顶替。
     * 材料先扣背包，不够再去绑定的 AE 网络取（见 construction 包）。
     * 上限按品阶取值：本物品是 {@link ConstructionTier#GOD}（1024 / 81 / 1024 / 天使 2）。
     */
    public static final DeferredItem<GodConstructionItem> GOD_CONSTRUCTION =
            ITEMS.registerItem("god_construction",
                    props -> new GodConstructionItem(props.stacksTo(1).fireResistant(), ConstructionTier.GOD));

    /**
     * 天神建筑：神之建筑的同族高阶物品，能力完全一致，只是四个上限全部拉到 {@code Integer.MAX_VALUE}
     * （{@link ConstructionTier#HEAVENLY}，即实际无限）。同样无耐久、不毁。
     * 超大操作由跨 tick 作业分摊（见 construction 包的 ConstructionJob），不会卡服务端。
     */
    public static final DeferredItem<GodConstructionItem> HEAVENLY_CONSTRUCTION =
            ITEMS.registerItem("heavenly_construction",
                    props -> new GodConstructionItem(props.stacksTo(1).fireResistant(), ConstructionTier.HEAVENLY));

    /** 神之配置：潜行右键机器保存六面 I/O / AE 开关 / 加速数量，普通右键同类机器一键粘贴（见 GodConfigItem） */
    public static final DeferredItem<GodConfigItem> GOD_CONFIG =
            ITEMS.registerItem("god_config", props -> new GodConfigItem(props.stacksTo(1)));
    /** 神之便签：记事本物品（内容按玩家存在存档里，见 GodNoteData） */
    public static final DeferredItem<GodNoteItem> GOD_NOTE =
            ITEMS.registerItem("god_note", props -> new GodNoteItem(props.stacksTo(1)));
    /** 神之配方：一切神之造物的合成底料（铁 / 铜 / 金 / 钻石 / 下界合金 / 绿宝石 / 红石 / 青金石 / 煤炭 各一，有序合成） */
    public static final DeferredItem<GodRecipeItem> GOD_RECIPE =
            ITEMS.registerItem("god_recipe", props -> new GodRecipeItem(props));

    /** 神之手册：游戏内查「这东西是干嘛的」（条目从注册表生成，见 manual 包） */
    public static final DeferredItem<com.godofthings.item.GodManualItem> GOD_MANUAL =
            ITEMS.registerItem("god_manual", props -> new com.godofthings.item.GodManualItem(props.stacksTo(1)));

    /** 神之背包：120 格随身仓库（右键打开；内容与设置存在物品数据组件里，见 backpack 包） */
    public static final DeferredItem<GodBackpackItem> GOD_BACKPACK_ITEM =
            ITEMS.registerItem("god_backpack", props -> new GodBackpackItem(props.stacksTo(1)));

    /**
     * 自然罗盘（移植自 Natures Compass 3.4.0，作者 ChaosTheDude，CC BY-NC-SA 4.0）：
     * 右键打开群系选择界面，选一个群系就能搜到它的坐标并指路；有权限还能直接传送过去。
     * 数据组件 / 网络包 / 客户端注册 / 配置见 {@code compass.natures} 包。
     */
    public static final DeferredItem<com.godofthings.compass.natures.items.NaturesCompassItem> NATURES_COMPASS =
            ITEMS.registerItem("natures_compass",
                    props -> new com.godofthings.compass.natures.items.NaturesCompassItem(props));

    /**
     * 探险者指南针（移植自 Explorer's Compass 1.20.x，作者 ChaosTheDude，CC BY-NC-SA 4.0）：
     * 右键打开结构选择界面，选一个结构就能搜到它的坐标并指路；有权限还能直接传送过去。
     * 网络包 / 客户端注册 / 配置见 {@code compass.explorers} 包。
     * <p>已按用户要求删掉上游「搜到后自动在小地图模组里建路径点」那段联动。</p>
     */
    public static final DeferredItem<com.godofthings.compass.explorers.items.ExplorersCompassItem> EXPLORERS_COMPASS =
            ITEMS.registerItem("explorers_compass",
                    com.godofthings.compass.explorers.items.ExplorersCompassItem::new);

    /** 神之皮带：9 格工具腰带（右键打开界面，R 键轮盘切换；移植自 ToolBelt 2.2.10，BSD-3-Clause） */
    public static final DeferredItem<ToolBeltItem> GOD_BELT =
            ITEMS.registerItem("god_belt", props -> new ToolBeltItem(props.stacksTo(1)));

    /** 神之皮带内容界面（皮带里的 9 个槽 + 玩家背包） */
    public static final DeferredHolder<MenuType<?>, MenuType<BeltContainer>> GOD_BELT_MENU =
            MENUS.register("god_belt", () -> IMenuTypeExtension.create(BeltContainer::new));
    /** 神之皮带槽位界面（装备皮带 + 2×2 合成 + 背包，V 键打开） */
    public static final DeferredHolder<MenuType<?>, MenuType<BeltSlotMenu>> GOD_BELT_SLOT_MENU =
            MENUS.register("god_belt_slot", () -> new MenuType<>(BeltSlotMenu::new, FeatureFlags.DEFAULT_FLAGS));
    /** 神之皮带槽（玩家附件：存档 / 死亡掉落 / 死亡复制都在 BeltAttachment 的事件里） */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<BeltAttachment>> GOD_BELT_ATTACHMENT =
            ATTACHMENT_TYPES.register("god_belt", () -> AttachmentType.serializable(BeltAttachment::new).build());

    // ---- 神之工具 ----
    // GT 扳手模式子类（通过模式轮盘切换，不直接出现在创造标签）

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

    // ---- 神之传输（能量传输器：范围内玩家/机器 FE 供能）----
    public static final DeferredBlock<GodTransmitterBlock> GOD_TRANSMITTER =
            BLOCKS.registerBlock("god_transmitter", GodTransmitterBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_BLUE)
                            .pushReaction(PushReaction.DESTROY)
                            .strength(5.0F, 1200.0F)
                            .lightLevel(state -> 15)
                            .sound(SoundType.METAL));
    public static final DeferredItem<BlockItem> GOD_TRANSMITTER_ITEM =
            ITEMS.registerSimpleBlockItem(GOD_TRANSMITTER, new Item.Properties());
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GodTransmitterBlockEntity>> GOD_TRANSMITTER_BE =
            BLOCK_ENTITIES.register("god_transmitter",
                    () -> BlockEntityType.Builder.of(GodTransmitterBlockEntity::new, GOD_TRANSMITTER.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<GodTransmitterMenu>> GOD_TRANSMITTER_MENU =
            MENUS.register("god_transmitter",
                    () -> IMenuTypeExtension.create(GodTransmitterMenu::new));

    // ---- 神之砍杀 ----
    public static final DeferredBlock<GodSlaughterBlock> GOD_SLAUGHTER =
            BLOCKS.registerBlock("god_slaughter", GodSlaughterBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_RED)
                            .pushReaction(PushReaction.DESTROY)
                            .strength(5.0F, 1200.0F)
                            .lightLevel(state -> 12)
                            .sound(SoundType.METAL));
    public static final DeferredItem<BlockItem> GOD_SLAUGHTER_ITEM =
            ITEMS.registerSimpleBlockItem(GOD_SLAUGHTER, new Item.Properties());
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GodSlaughterBlockEntity>> GOD_SLAUGHTER_BE =
            BLOCK_ENTITIES.register("god_slaughter",
                    () -> BlockEntityType.Builder.of(GodSlaughterBlockEntity::new, GOD_SLAUGHTER.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<GodSlaughterMenu>> GOD_SLAUGHTER_MENU =
            MENUS.register("god_slaughter",
                    () -> IMenuTypeExtension.create(GodSlaughterMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<GodSlaughterConfigMenu>> GOD_SLAUGHTER_CONFIG_MENU =
            MENUS.register("god_slaughter_config",
                    () -> IMenuTypeExtension.create(GodSlaughterConfigMenu::new));

    // ---- 神之吸收 ----
    public static final DeferredBlock<GodAbsorberBlock> GOD_ABSORBER =
            BLOCKS.registerBlock("god_absorber", GodAbsorberBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_PURPLE)
                            .pushReaction(PushReaction.DESTROY)
                            .strength(5.0F, 1200.0F)
                            .lightLevel(state -> 10)
                            .sound(SoundType.METAL));
    public static final DeferredItem<BlockItem> GOD_ABSORBER_ITEM =
            ITEMS.registerSimpleBlockItem(GOD_ABSORBER, new Item.Properties());
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GodAbsorberBlockEntity>> GOD_ABSORBER_BE =
            BLOCK_ENTITIES.register("god_absorber",
                    () -> BlockEntityType.Builder.of(GodAbsorberBlockEntity::new, GOD_ABSORBER.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<GodAbsorberMenu>> GOD_ABSORBER_MENU =
            MENUS.register("god_absorber",
                    () -> IMenuTypeExtension.create(GodAbsorberMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<GodAbsorberConfigMenu>> GOD_ABSORBER_CONFIG_MENU =
            MENUS.register("god_absorber_config",
                    () -> IMenuTypeExtension.create(GodAbsorberConfigMenu::new));

    // ---- 神之更改 ----
    public static final DeferredItem<GodChangeItem> GOD_CHANGE =
            ITEMS.registerItem("god_change", props -> new GodChangeItem(props.stacksTo(1)));

    // ---- 神之测量（卷尺：两定点画线框显三轴尺寸，纯客户端；自 Measurements 移植）----
    public static final DeferredItem<GodMeasureItem> GOD_MEASURE =
            ITEMS.registerItem("god_measure", GodMeasureItem::new);

    // ---- 「万物元件」（v5.25.0：合并原 4 个单资源无限元件 + 新增福禄伊克斯色玻璃线缆，共 5 种）----
    // 自 ExtendedAE（LGPL-3.0）的无限元件移植思路：固定资源无限存取、不写 NBT、幻影存储。
    // 容量口径从原来的 Integer.MAX_VALUE（2.1G）提升到 Long.MAX_VALUE（9.2E），
    // 多元件叠加时的溢出由 KeyCounterMixin + InfinityBigStorage 兜住（BigInteger 精确记账）。
    public static final DeferredItem<UniversalCellItem> UNIVERSAL_CELL =
            ITEMS.registerItem("universal_cell",
                    props -> new UniversalCellItem(() -> java.util.List.of(
                            AEFluidKey.of(Fluids.WATER),
                            AEFluidKey.of(Fluids.LAVA),
                            AEItemKey.of(Blocks.COBBLESTONE),
                            AEItemKey.of(AEItems.BLANK_PATTERN),
                            AEItemKey.of(AEParts.GLASS_CABLE.stack(AEColor.TRANSPARENT))),
                            props.stacksTo(1)));

    // ---- 「吞噬万籁的寂静」无限存储元件（自 ExtendedAE+ LGPL-3.0 移植：无限种类 + BigInteger 容量）----
    // 与上面 4 个固定资源的无限元件不同，这个是**通用**的：任意物品 / 流体都能存，
    // 真实数据放在世界存档 world/data/infinity_biginteger_cells.dat 里（见 InfinityStorageManager）。
    public static final DeferredItem<com.godofthings.infinitecell.big.InfinityCellItem> INFINITY_BIGINTEGER_CELL =
            ITEMS.registerItem("infinity_biginteger_cell",
                    props -> new com.godofthings.infinitecell.big.InfinityCellItem());

    // ---- 无限 FE 存储元件（软依赖 AppFlux：装了才有，没装不注册）----
    // 容量取 9.2E（Long.MAX_VALUE），与上面几个无限元件同档。不用普通 FE 容器是因为
    // NeoForge 的 IEnergyStorage 是 int，装不下 9.2E；AppFlux 的 FE 元件同样走 AE2 存储元件机制。
    public static final DeferredItem<com.godofthings.fluxcell.InfinityFluxCellItem> INFINITY_FLUX_CELL =
            ITEMS.registerItem("infinity_flux_cell",
                    props -> new com.godofthings.fluxcell.InfinityFluxCellItem());

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

    // ---- 神之吞噬（销毁物品/液体的大箱子，六面输入）----
    public static final DeferredBlock<GodDevourerBlock> GOD_DEVOURER =
            BLOCKS.registerBlock("god_devourer", GodDevourerBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_BLACK)
                            .strength(3.0F, 6.0F)
                            .sound(SoundType.METAL));
    public static final DeferredItem<BlockItem> GOD_DEVOURER_ITEM =
            ITEMS.registerSimpleBlockItem(GOD_DEVOURER, new Item.Properties());
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GodDevourerBlockEntity>> GOD_DEVOURER_BE =
            BLOCK_ENTITIES.register("god_devourer",
                    () -> BlockEntityType.Builder.of(GodDevourerBlockEntity::new, GOD_DEVOURER.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<GodDevourerMenu>> GOD_DEVOURER_MENU =
            MENUS.register("god_devourer", () -> IMenuTypeExtension.create(GodDevourerMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<GodDevourerMenu>> PORTABLE_DEVOURER_MENU =
            MENUS.register("portable_devourer", () -> IMenuTypeExtension.create(
                    (id, inv, buf) -> new GodDevourerMenu(id, inv)));

    public static final DeferredHolder<MenuType<?>, MenuType<WaypointMenu>> WAYPOINT_MENU =
            MENUS.register("waypoint", () -> IMenuTypeExtension.create(WaypointMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<GodBlackBoxMenu>> GOD_BLACK_BOX_MENU =
            MENUS.register("god_black_box", () -> IMenuTypeExtension.create(GodBlackBoxMenu::new));

    // ---- 方块实体 ----
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GodFurnaceBlockEntity>> GOD_FURNACE_BE =
            BLOCK_ENTITIES.register("god_furnace",
                    () -> BlockEntityType.Builder.of(GodFurnaceBlockEntity::new, GOD_FURNACE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GodMinerBlockEntity>> GOD_MINER_BE =
            BLOCK_ENTITIES.register("god_miner",
                    () -> BlockEntityType.Builder.of(GodMinerBlockEntity::new, GOD_MINER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GodResourceBlockEntity>> GOD_RESOURCE_BE =
            BLOCK_ENTITIES.register("god_resource",
                    () -> BlockEntityType.Builder.of(
                            // 工厂只能给 (pos, state)：按 state 归属的方块实例回填变体
                            // （newBlockEntity 已正确传入；旧存档读回走这条 lambda）
                            (pos, state) -> {
                                GodResourceBlockEntity be = new GodResourceBlockEntity(pos, state);
                                if (state.getBlock() instanceof GodResourceBlock resource)
                                {
                                    be.setVariant(resource.getVariant());
                                }
                                return be;
                            },
                            GOD_ORE_MACHINE.get(), GOD_CROP_MACHINE.get(), GOD_DUPLICATE_MACHINE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GodDropBlockEntity>> GOD_DROP_BE =
            BLOCK_ENTITIES.register("god_drop",
                    () -> BlockEntityType.Builder.of(GodDropBlockEntity::new, GOD_DROP.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GodSpawnEggBlockEntity>> GOD_SPAWN_EGG_BE =
            BLOCK_ENTITIES.register("god_spawn_egg",
                    () -> BlockEntityType.Builder.of(GodSpawnEggBlockEntity::new, GOD_SPAWN_EGG.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GodEnchantBlockEntity>> GOD_ENCHANT_BE =
            BLOCK_ENTITIES.register("god_enchant",
                    () -> BlockEntityType.Builder.of(GodEnchantBlockEntity::new,
                            GOD_ENCHANT.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GodPeelerBlockEntity>> GOD_PEELER_BE =
            BLOCK_ENTITIES.register("god_peeler",
                    () -> BlockEntityType.Builder.of(GodPeelerBlockEntity::new, GOD_PEELER.get()).build(null));

    // ---- 菜单 ----
    public static final DeferredHolder<MenuType<?>, MenuType<GodFurnaceMenu>> GOD_FURNACE_MENU =
            MENUS.register("god_furnace", () -> IMenuTypeExtension.create(GodFurnaceMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<GodFurnaceConfigMenu>> GOD_FURNACE_CONFIG_MENU =
            MENUS.register("god_furnace_config", () -> IMenuTypeExtension.create(GodFurnaceConfigMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<GodMinerMenu>> GOD_MINER_MENU =
            MENUS.register("god_miner", () -> IMenuTypeExtension.create(GodMinerMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<GodMinerConfigMenu>> GOD_MINER_CONFIG_MENU =
            MENUS.register("god_miner_config", () -> IMenuTypeExtension.create(GodMinerConfigMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<GodResourceMenu>> GOD_RESOURCE_MENU =
            MENUS.register("god_resource", () -> IMenuTypeExtension.create(GodResourceMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<GodResourceConfigMenu>> GOD_RESOURCE_CONFIG_MENU =
            MENUS.register("god_resource_config", () -> IMenuTypeExtension.create(GodResourceConfigMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<GodPeelerMenu>> GOD_PEELER_MENU =
            MENUS.register("god_peeler", () -> IMenuTypeExtension.create(GodPeelerMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<GodPeelerConfigMenu>> GOD_PEELER_CONFIG_MENU =
            MENUS.register("god_peeler_config", () -> IMenuTypeExtension.create(GodPeelerConfigMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<GodDropMenu>> GOD_DROP_MENU =
            MENUS.register("god_drop", () -> IMenuTypeExtension.create(GodDropMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<GodDropConfigMenu>> GOD_DROP_CONFIG_MENU =
            MENUS.register("god_drop_config", () -> IMenuTypeExtension.create(GodDropConfigMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<GodSpawnEggMenu>> GOD_SPAWN_EGG_MENU =
            MENUS.register("god_spawn_egg", () -> IMenuTypeExtension.create(GodSpawnEggMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<GodSpawnEggConfigMenu>> GOD_SPAWN_EGG_CONFIG_MENU =
            MENUS.register("god_spawn_egg_config", () -> IMenuTypeExtension.create(GodSpawnEggConfigMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<GodEnchantMenu>> GOD_ENCHANT_MENU =
            MENUS.register("god_enchant", () -> IMenuTypeExtension.create(GodEnchantMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<GodChangeMenu>> GOD_CHANGE_MENU =
            MENUS.register("god_change", () -> IMenuTypeExtension.create(GodChangeMenu::new));
    /** 神之背包界面：额外数据是「背包在哪个物品栏槽位」（主手 = 快捷栏下标，副手 = -1） */
    public static final DeferredHolder<MenuType<?>, MenuType<GodBackpackMenu>> GOD_BACKPACK_MENU =
            MENUS.register("god_backpack", () -> IMenuTypeExtension.create(
                    (id, inv, buf) -> new GodBackpackMenu(id, inv, buf.readVarInt())));

    // ---- 神之不毁配方序列化器（使用 DeferredHolder 常规注册）----
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<?>> GOD_UNBREAKABLE_SERIALIZER =
            RECIPE_SERIALIZERS.register("god_unbreakable", GodUnbreakableRecipe.Serializer::new);

    // ---- 创造标签 ----
    /**
     * 创造标签：众神之物（机器与方块合并页）。
     * 工作机器 + 无限样板供应器 + 创造/特殊方块 + 无线连接器都在这里；
     * 纯物品（装备、工具、卡片、书页、容器、升级件、ME 元件、玩偶）在
     * {@link #GOD_OF_THINGS_ITEMS_TAB}，众神建材的 66 个方块在
     * {@code BuildingRegistry.GOD_BUILDING_TAB}。
     */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> GOD_OF_THINGS_TAB =
            CREATIVE_MODE_TABS.register("god_of_things_tab", () -> CreativeModeTab.builder()
                    .icon(() -> GOD_FURNACE_ITEM.get().getDefaultInstance())
                    .title(Component.translatable("itemGroup.godofthings.god_of_things_tab"))
                    .displayItems((parameters, output) ->
                    {
                        // ---- 工作机器 ----
                        output.accept(GOD_FURNACE_ITEM.get());
                        output.accept(GOD_MINER_ITEM.get());
                        output.accept(GOD_ORE_MACHINE_ITEM.get());
                        output.accept(GOD_CROP_MACHINE_ITEM.get());
                        output.accept(GOD_DUPLICATE_MACHINE_ITEM.get());
                        output.accept(GOD_PEELER_ITEM.get());
                        output.accept(GOD_DROP_ITEM.get());
                        output.accept(GOD_SPAWN_EGG_ITEM.get());
                        output.accept(GOD_ENCHANT_ITEM.get());
                        output.accept(GOD_CRAFT_ITEM.get());
                        output.accept(GOD_TRANSMITTER_ITEM.get());
                        output.accept(GOD_SLAUGHTER_ITEM.get());
                        output.accept(GOD_ABSORBER_ITEM.get());
                        output.accept(GOD_DEVOURER_ITEM.get());
                        // ---- 无限样板供应器（方块 + part 两种形态 ×4 档）----
                        output.accept(com.godofthings.infinityprovider.InfProviderRegistry.P1_ITEM.get());
                        output.accept(com.godofthings.infinityprovider.InfProviderRegistry.P32_ITEM.get());
                        output.accept(com.godofthings.infinityprovider.InfProviderRegistry.P1024_ITEM.get());
                        output.accept(com.godofthings.infinityprovider.InfProviderRegistry.PINF_ITEM.get());
                        output.accept(com.godofthings.infinityprovider.InfProviderRegistry.P1_PART_ITEM.get());
                        output.accept(com.godofthings.infinityprovider.InfProviderRegistry.P32_PART_ITEM.get());
                        output.accept(com.godofthings.infinityprovider.InfProviderRegistry.P1024_PART_ITEM.get());
                        output.accept(com.godofthings.infinityprovider.InfProviderRegistry.PINF_PART_ITEM.get());
                        // ---- 创造 / 特殊方块 ----
                        output.accept(VOID_TELEPORTER_ITEM.get());
                        output.accept(CREATIVE_ENERGY_CUBE_ITEM.get());
                        output.accept(SPACE_TIME_ETERNITY_ITEM.get());
                        output.accept(CREATURE_ANNIHILATION_ITEM.get());
                        // 无用维度传送方块（奇数 / 偶数 / 三维度）
                        output.accept(com.godofthings.beef.init.ModItems.TELEPORT_BLOCK_ITEM.get());
                        output.accept(com.godofthings.beef.init.ModItems.TELEPORT_BLOCK_ITEM_2.get());
                        output.accept(com.godofthings.beef.init.ModItems.TELEPORT_BLOCK_ITEM_3.get());
                        // 无线连接器（移植自 AALink：把任意位置的 AE 设备无线并入中枢网络）
                        output.accept(com.godofthings.aalink.AaLinkRegistry.WIRELESS_CONNECTOR_ITEM.get());
                        // 过载接口（移植自 AE2LT：36 槽 + 无线 + 导出导入 + 弹出）
                        output.accept(
                                com.godofthings.overloadedinterface.OverloadedInterfaceRegistry
                                        .OVERLOADED_INTERFACE_ITEM.get());
                    })
                    .build());

    /**
     * 创造标签：众神之物·物品（装备、工具、卡片、书页、容器、升级件、ME 元件、
     * 玩偶摆件、AALink 连接工具）。
     */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> GOD_OF_THINGS_ITEMS_TAB =
            CREATIVE_MODE_TABS.register("god_of_things_items_tab", () -> CreativeModeTab.builder()
                    .icon(() -> GOD_HELMET.get().getDefaultInstance())
                    .title(Component.translatable("itemGroup.godofthings.god_of_things_items_tab"))
                    .displayItems((parameters, output) ->
                    {
                        // ---- 神之套装（四件套放一起）----
                        output.accept(GOD_HELMET.get());
                        output.accept(GOD_CHESTPLATE.get());
                        output.accept(GOD_LEGGINGS.get());
                        output.accept(GOD_BOOTS.get());
                        // ---- 手持神之工具（外形相近，按用途排）----
                        output.accept(com.godofthings.wondrous.init.WondrousItems.WONDROUS_STAFF.get());
                        output.accept(GOD_INVITE.get());
                        output.accept(GOD_CHANGE.get());
                        output.accept(GOD_MEASURE.get());
                        output.accept(GOD_BLACK_BOX.get());
                        output.accept(GOD_BINDER.get());
                        output.accept(GOD_CONSTRUCTION.get());
                        output.accept(HEAVENLY_CONSTRUCTION.get());
                        // 自然罗盘（移植自 Natures Compass，群系搜索 / 指路 / 传送）
                        output.accept(NATURES_COMPASS.get());
                        // 探险者指南针（移植自 Explorer's Compass，结构搜索 / 指路 / 传送）
                        output.accept(EXPLORERS_COMPASS.get());
                        // 无线连接工具（移植自 AALink）
                        output.accept(com.godofthings.aalink.AaLinkRegistry.WIRELESS_LINK_TOOL.get());
                        // ---- 卡片 / 石板类 ----
                        output.accept(GOD_RECIPE.get());
                        output.accept(GOD_CONFIG.get());
                        // ---- 书页类 ----
                        output.accept(GOD_MANUAL.get());
                        output.accept(GOD_NOTE.get());
                        // ---- 随身容器 ----
                        output.accept(GOD_BACKPACK_ITEM.get());
                        output.accept(GOD_BELT.get());
                        // ---- 升级件 ----
                        output.accept(GOD_ACCELERATOR.get());
                        output.accept(GOD_UNBREAKABLE.get());
                        // ---- ME 无限元件 ----
                        output.accept(UNIVERSAL_CELL.get());
                        output.accept(INFINITY_BIGINTEGER_CELL.get());
        // 无限 FE 元件：只在装了 AppFlux 时出现（软依赖）
        if (com.godofthings.fluxcell.FluxBridge.isAvailable())
        {
            output.accept(INFINITY_FLUX_CELL.get());
        }
                        // ---- 高级内存卡（移植自 Advanced Memory Card：AE2 内存卡增强版）----
                        output.accept(com.godofthings.advancedmemorycard.item.AmcItems.ADVANCED_MEMORY_CARD.get());
                        // ---- 紬紬玩偶（移植自 useless_stretcher 的 2i 玩偶）+ 摆件 ----
                        // 这一个物品同时是摆件的物品形态（TwoIDollItem extends BlockItem）
                        output.accept(com.godofthings.doll.init.DollRegistry.TWO_I_DOLL_ITEM.get());
                        // ---- 超级融合卡（移植自 Mekanism-Card，软依赖：只在装了通用机械时出现）----
                        if (com.godofthings.superfusion.SuperFusionRegistry.isEnabled())
                        {
                            output.accept(com.godofthings.superfusion.SuperFusionRegistry.itemOrNull());
                        }
                        // ---- ME 放置工具三件套（移植自 ME-Placement-Tool，不耗能源版；光谱钥匙功能已内置进线缆工具）----
                        output.accept(com.godofthings.mept.MeptItems.ME_PLACEMENT_TOOL.get());
                        output.accept(com.godofthings.mept.MeptItems.MULTIBLOCK_PLACEMENT_TOOL.get());
                        output.accept(com.godofthings.mept.MeptItems.ME_CABLE_PLACEMENT_TOOL.get());
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
        DATA_COMPONENTS.register(modEventBus);
        ATTACHMENT_TYPES.register(modEventBus);
        // 神之护甲材质（穿戴图层指向本模组贴图；漏注册会在玩家 tick 查询护甲属性时 unbound 崩溃）
        com.godofthings.item.GodArmorMaterials.register(modEventBus);

        // 神之机器参数配置（矿机/资源机/掉落机，godofthings-machines.toml）
        // 显式指定文件名，避免依赖默认命名规则（默认 godofthings-server.toml）。
        modContainer.registerConfig(ModConfig.Type.SERVER, MachinesConfig.SPEC, "godofthings-machines.toml");
        // 客户端配置：记住配置界面上次停留的标签页（见 ClientConfig）
        modContainer.registerConfig(ModConfig.Type.CLIENT, com.godofthings.config.ClientConfig.SPEC, "godofthings-client.toml");
        // 自然罗盘的配置（移植自 Natures Compass）：搜索参数是服务端权威值，HUD 显示是纯客户端项
        modContainer.registerConfig(ModConfig.Type.SERVER,
                com.godofthings.compass.natures.config.ConfigHandler.GENERAL_SPEC, "godofthings-natures-compass.toml");
        modContainer.registerConfig(ModConfig.Type.CLIENT,
                com.godofthings.compass.natures.config.ConfigHandler.CLIENT_SPEC,
                "godofthings-natures-compass-client.toml");
        // 探险者指南针的配置（移植自 Explorer's Compass）：搜索参数是服务端权威值，HUD 显示是纯客户端项
        modContainer.registerConfig(ModConfig.Type.SERVER,
                com.godofthings.compass.explorers.config.ConfigHandler.GENERAL_SPEC,
                "godofthings-explorers-compass.toml");
        modContainer.registerConfig(ModConfig.Type.CLIENT,
                com.godofthings.compass.explorers.config.ConfigHandler.CLIENT_SPEC,
                "godofthings-explorers-compass-client.toml");
        modEventBus.addListener(this::commonSetup);

        // ===== 超级融合卡（移植自 Mekanism-Card，软依赖 Mekanism）=====
        // 只在装了通用机械时才注册物品与数据组件；没装则整个 superfusion 包不会被触达，
        // 创造栏 / JEI / 手册里都不会出现这一件，也不会因为缺少 Mekanism 类而崩。
        if (com.godofthings.superfusion.SuperFusionEvents.isMekanismLoaded())
        {
            com.godofthings.superfusion.SuperFusionRegistry.init();
            com.godofthings.superfusion.SuperFusionRegistry.registerDataComponents(modEventBus);
            // 注意：网络载荷由 SuperFusionEvents 自身的 @EventBusSubscriber 自动注册，
            // 这里**不能**再 addListener 一次，否则会抛「payload 已注册」让模组加载失败。
        }

        // ===== 照抄 useless_mod 的 beef 子系统接线（牛排工具框架已整套删除，只剩仍活着的部分）=====
        // 造化杖数据组件（v7.13.0 恢复杖子系统后重新接线；必须在构造期注册，且要早于物品注册）
        com.godofthings.beef.core.component.UComponents.init(modEventBus);
        // 造化杖时间加速实体（v7.13.0 恢复）
        com.godofthings.beef.init.ModEntities.ENTITY_TYPES.register(modEventBus);
        com.godofthings.beef.init.ModItems.ITEMS.register(modEventBus);
        com.godofthings.beef.init.ModMenuType.register(modEventBus);
        // 无用维度（奇数 / 偶数 / 三维度）：传送方块 + 方块物品 + POI + 区块生成器
        com.godofthings.beef.init.ModBlocks.BLOCKS.register(modEventBus);
        com.godofthings.beef.init.ModPOIs.POI_TYPES.register(modEventBus);
        com.godofthings.beef.world.dimension.UselessDimensions.init(modEventBus);
        modEventBus.addListener(com.godofthings.beef.init.ModNetwork::registerPayloadHandlers);
        // beef 子系统配置已整体取消注册（v7.14.4）：COMMON/CLIENT/SERVER 三个 builder
        // 在多轮死代码清理后均已零 define，生成的三个 toml 是空文件（server 仅剩的 floor
        // 黑白名单两项也无调用方）。旧 config/godofthings-beef-*.toml 可手动删除。

        NeoForge.EVENT_BUS.addListener(this::onServerStarting);

        // ===== 玩偶系统（移植自 useless_stretcher 的 2i 玩偶；星光庭院/图片欣赏/尾巴已裁剪）=====
        // 实体 / 物品 / 方块 / 方块实体 / 音效各有独立注册表，见 DollRegistry
        modEventBus.addListener(com.godofthings.doll.network.DollNetwork::register);
        // 玩偶实体属性（移动速度等）——漏挂会导致客户端创建预览实体时
        // AttributeMap.supplier 为 null 而 NPE（对齐上游 ModEntities::registerAttributes）。
        modEventBus.addListener(com.godofthings.doll.init.DollRegistry::registerAttributes);
        com.godofthings.doll.init.DollRegistry.register(modEventBus);
        com.godofthings.doll.init.DollSounds.SOUNDS.register(modEventBus);
        // ===== 众神建材（移植自 AE2LT 猪咪建材：16 色素板/框板/台阶 + 建材方块 + 染料基底 + CTM）=====
        com.godofthings.building.BuildingRegistry.register(modEventBus);
         // 玩偶配置（树叶掉落开关与概率）
         modContainer.registerConfig(ModConfig.Type.SERVER,
                 com.godofthings.doll.config.DollConfig.SPEC, "godofthings-doll-server.toml");
         // 神之皮带（移植自 ToolBelt 2.2.10）：网络包 / 物品能力 / 皮带槽事件 / 配置刷新
         BeltRegistration.init(modEventBus);
        // ===== 无限样板供应器（移植自 InfinityPatternProvider：1/32/1024/无限槽 + part 形态）=====
        com.godofthings.infinityprovider.InfProviderRegistry.register(modEventBus);
        // ===== 全能升级（移植自 Omni Upgrades：批量安装插件 / 等级安装器 + 高亮预览）=====
        // 适配器内部用 ModList.isLoaded 守护；网络通道 + 客户端/服务端双配置
        modEventBus.addListener(com.godofthings.omni.OmniNetwork::onRegisterPayloads);
        // 同心契绑定供应器 / 机器的 C2S 包（Ctrl 批量状态随包走）
        modEventBus.addListener(
                com.godofthings.infinityprovider.BinderUsePayload::onRegisterPayloads);
        modContainer.registerConfig(ModConfig.Type.CLIENT,
                com.godofthings.omni.OmniConfig.SPEC, "godofthings-omni-client.toml");
        // 全能升级没有服务端配置（上游 ServerConfig 的升级上限规则只服务蜂巢模组，随环境裁剪）
         // 皮带自己的配置文件（服务器白黑名单 / 客户端轮盘显示选项），与上面两份文件名区分开
        modContainer.registerConfig(ModConfig.Type.SERVER, ConfigData.SERVER_SPEC, "godofthings-belt-server.toml");
        modContainer.registerConfig(ModConfig.Type.CLIENT, ConfigData.CLIENT_SPEC, "godofthings-belt-client.toml");
        // ===== 无线网格桥接（移植自 AALink）：无线连接器方块 + 连接工具 + 全局桥接事件 =====
        com.godofthings.aalink.AaLinkRegistry.register(modEventBus);
        // 方块放置 / 区块加载 / 服务端 tick 时把任意位置的 AE 设备接入中枢网络
        NeoForge.EVENT_BUS.register(com.godofthings.aalink.AaLinkBridge.class);
        // 无线连接器卡（v7.11.0 替换旧无线连接工具，功能移植自 AE2LT 过载频率卡）：
        // 三个网络包 + 放置方块后的自动连接
        modEventBus.addListener(com.godofthings.aalink.ConnectorCardNetwork::registerPayloads);
        NeoForge.EVENT_BUS.register(com.godofthings.aalink.ConnectorCardAutoConnectHandler.class);
        // 指令开关并入 /godofthings link on|off，不再注册独立的 /aalink
        // ===== 过载接口（移植自 AE2LT）：36 槽 / 无线 / 导出导入 / 弹出 / IO 速度 =====
        com.godofthings.overloadedinterface.OverloadedInterfaceRegistry.register(modEventBus);
        // ===== 高级内存卡（移植自 Advanced Memory Card：批量复制粘贴配置 + P2P 通道管理）=====
        com.godofthings.advancedmemorycard.item.AmcItems.register(modEventBus);
        com.godofthings.advancedmemorycard.menu.AmcMenus.register(modEventBus);
        modEventBus.addListener(com.godofthings.advancedmemorycard.network.NetworkHandler::register);
        // 客户端配置（界面颜色/布局）+ 服务端配置（复制体积上限、高亮聊天开关）
        com.godofthings.advancedmemorycard.config.ModConfigs.register(modContainer);
        // ME 放置工具（移植自 ME-Placement-Tool 2.1.5：三件套不耗能源 + 光谱钥匙 + 棱镜核心）=====
        // AE2 是本模组硬依赖，无需条件判断；fumo 摆件方块不移植
        com.godofthings.mept.MeptItems.register(modEventBus);
        com.godofthings.mept.MeptDataComponents.DATA_COMPONENTS.register(modEventBus);
        com.godofthings.mept.MeptMenus.register(modEventBus);
        // 网络载荷注册（RegisterPayloadHandlersEvent）
        modEventBus.addListener(com.godofthings.mept.network.MeptNetwork::registerPayloads);
        // 左键清空线缆选点 / 切手重置 / 玩家退出释放撤销历史（游戏总线事件）
        NeoForge.EVENT_BUS.register(com.godofthings.mept.MeptEvents.class);
        // 上游 Config 的放置数量上限等参数（本地沿用 MeptConfig 读取）

        // ===== 荒辰移晷之杖（v7.12.0 移植自 useless_stretcher）=====
        // 数据组件 / 物品 / 实体三类注册；配置文件独立成 godofthings-wondrous-server/client.toml
        com.godofthings.wondrous.init.StaffComponents.register(modEventBus);
        com.godofthings.wondrous.init.WondrousItems.register(modEventBus);
        com.godofthings.wondrous.init.WondrousEntities.register(modEventBus);
        // 配置文件独立成 godofthings-wondrous-server/client.toml（内部 registerConfig）
        com.godofthings.wondrous.config.WondrousConfig.register(modContainer);
        // 网络载荷（照抄上游 Network::register；内含 ApotheosisNetwork 段）
        modEventBus.addListener(com.godofthings.wondrous.network.WondrousNetwork::register);

        // 服务端 tick / 特性开关 / 相位交互 / 时停交互 / 树叶奖励 / 辐射防护（游戏总线事件类）
        NeoForge.EVENT_BUS.register(com.godofthings.wondrous.event.StaffServerTickHandler.class);
        NeoForge.EVENT_BUS.register(com.godofthings.wondrous.event.WondrousStaffFeatureEvents.class);
        NeoForge.EVENT_BUS.register(com.godofthings.wondrous.event.StaffPhaseInteractionEvents.class);
        NeoForge.EVENT_BUS.register(com.godofthings.wondrous.event.TimeStopInteractionEvents.class);
        NeoForge.EVENT_BUS.register(com.godofthings.wondrous.event.WondrousStaffLeafDropHandler.class);
        NeoForge.EVENT_BUS.register(com.godofthings.wondrous.compat.StaffRadiationProtection.class);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        // 注意：构造函数里只做注册；方块实体的绑定必须在 commonSetup 里做
        event.enqueueWork(com.godofthings.overloadedinterface.OverloadedInterfaceRegistry::bindBlockAndEntity);
        // 升级卡白名单：合成卡/模糊卡/appflux 感应卡（照抄上游）
        event.enqueueWork(com.godofthings.overloadedinterface.OverloadedInterfaceUpgrades::register);
        // ME 无限元件：存储处理器 + 驱动器槽位模型注册（enqueueWork 保证注册表就绪且主线程执行；
        // StorageCellModels 虽在 api.client 包里，但上游 ExtendedAE 同样在双侧通用初始化中调用）
        event.enqueueWork(() -> {
            com.godofthings.infinityprovider.InfProviderCapabilities.bindAll();
            StorageCells.addCellHandler(com.godofthings.infinitecell.big.InfinityCellHandler.INSTANCE);
            StorageCells.addCellHandler(UniversalCellInventory.HANDLER);
            StorageCellModels.registerModel(INFINITY_BIGINTEGER_CELL.get(), godofthingsId("block/drive/infinity_biginteger_cell"));
            StorageCellModels.registerModel(UNIVERSAL_CELL.get(), godofthingsId("block/drive/universal_cell"));
        if (com.godofthings.fluxcell.FluxBridge.isAvailable())
        {
            StorageCells.addCellHandler(com.godofthings.fluxcell.InfinityFluxCellHandler.INSTANCE);
            StorageCellModels.registerModel(INFINITY_FLUX_CELL.get(), godofthingsId("block/drive/infinity_flux_cell"));
        }
        });
        // 神之建筑：让 AE2 认「这个物品能绑到无线访问点」（绑上后材料可以从 ME 网络里取）
        event.enqueueWork(com.godofthings.construction.ConstructionAeLink::register);
        // 超级融合卡：同样注册成 AE2 可绑定物（放进 ME 无线访问点 Shift+右键绑定；
        // 必须在 enqueueWork 里做——构造阶段物品还没入表，取 DeferredHolder 会 NPE）
        event.enqueueWork(com.godofthings.superfusion.SuperFusionRegistry::registerAeLinkable);
        // ME 放置工具：三件套都注册成 AE2 可绑定物（同上，enqueueWork 里才安全）
        event.enqueueWork(com.godofthings.mept.MeptItems::registerAeLinkables);
        // 无线连接器卡 → AE2 升级卡注册：
        // 原版终端走四参 Upgrades.add（group 指定无线终端组，升级槽 UI 按组过滤显示）；
        // AE2 无线终端（ae2wtlib）的终端走它自己的 UpgradeHelper.addUpgradeToAllTerminals
        // （该 API 藏在 jarjar 嵌套 jar 里且是可选依赖，只能反射调用）。
        event.enqueueWork(() -> {
            var card = com.godofthings.aalink.AaLinkRegistry.WIRELESS_LINK_TOOL.get();
            String wtGroup = appeng.core.localization.GuiText.WirelessTerminals.getTranslationKey();
            appeng.api.upgrades.Upgrades.add(card, AEItems.WIRELESS_TERMINAL.get(), 1, wtGroup);
            appeng.api.upgrades.Upgrades.add(card, AEItems.WIRELESS_CRAFTING_TERMINAL.get(), 1, wtGroup);
            appeng.api.upgrades.Upgrades.add(card, AEItems.COLOR_APPLICATOR.get(), 1, wtGroup);
            com.godofthings.aalink.ConnectorCardTerminalCompat.registerAe2wtlibUpgrades(card);
        });
        AdAstraCompat.init(); // Ad Astra 未安装时自动跳过
        // 自然罗盘：注册传送权限节点 godofthings.natures_compass.teleport（无权限插件的服务器上
        // 默认解析为 false，也就是「只有 OP / 单人开作弊能传送」）
        NeoForge.EVENT_BUS.addListener(com.godofthings.compass.natures.NaturesCompassData::registerPermissionNodes);
        // 探险者指南针：读「结构 → 自定义模型数据」映射表（config/godofthings/config.json，
        // 不存在就自动生成一份默认的；配置项 customResource 关着时这张表也不会被用到）
        event.enqueueWork(() -> com.godofthings.compass.explorers.config.BetterUI.loadConfig(MODID));
        // 高级内存卡：注册复制/配置两种卡模式（上游在独立 mod 的 commonSetup 里做）
        com.godofthings.advancedmemorycard.item.custom.CardMode.initializeModes();
        LOGGER.info("Godofthings loaded");
    }

    public static net.minecraft.resources.ResourceLocation godofthingsId(String path)
    {
        return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    private void onServerStarting(ServerStartingEvent event)
    {
        LOGGER.info("Godofthings server starting");
    }
}
