package com.godofthings.block.entity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.CaveVinesBlock;
import net.minecraft.world.level.block.CaveVinesPlantBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.KelpBlock;
import net.minecraft.world.level.block.KelpPlantBlock;
import net.minecraft.world.level.block.MushroomBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.TallGrassBlock;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 神之资源三兄弟（v5.13.0 由单一「神之资源」拆分而来）的变体定义。
 *
 * <ul>
 *   <li>{@link #ORE} 神之矿物：只收矿物 —— 原矿 / 锭 / 宝石（{@link #ORE_ITEM_DROPS} 表）
 *       与矿石块（注册名含 "ore"，兼容格雷科技等模组）。</li>
 *   <li>{@link #CROP} 神之作物：只收作物 / 植物 —— 树苗、满熟作物、海带、仙人掌、
 *       竹子等（原「神之资源」的植物逻辑整体搬入）。</li>
 *   <li>{@link #DUPLICATE} 神之复制（v5.13.0 由「神之方块」改名并放开）：**收一切物品**
 *       （原版 / 模组任意物品，不再限于方块），复制 64 个。</li>
 * </ul>
 *
 * 其余行为与原神之资源一致：9 输入槽不消耗模板、快配方每 tick / 慢配方每 20 tick、
 * 神之加速并行倍率、向下自动输出、内置无限存储、AE2 并网。
 */
public enum GodResourceVariant
{
    ORE("god_ore_machine"),
    CROP("god_crop_machine"),
    DUPLICATE("god_duplicate_machine");

    /** 注册名（god_ore_machine / god_crop_machine / god_duplicate_machine），同时用作 GUI 贴图名 */
    public final String id;

    GodResourceVariant(String id)
    {
        this.id = id;
    }

    /** GUI 贴图路径名（textures/gui/<id>.png） */
    public String getGuiTexture()
    {
        return id;
    }

    // ==================== 矿物（ORE） ====================

    // 原矿石/材料 → 产出物（输入原矿石，产出对应的锭/成品，统一每 20 tick 64 个）
    private static final Map<Item, Item> ORE_ITEM_DROPS = Map.ofEntries(
            Map.entry(Items.RAW_IRON, Items.IRON_INGOT),             // 粗铁 → 铁锭
            Map.entry(Items.RAW_GOLD, Items.GOLD_INGOT),             // 粗金 → 金锭
            Map.entry(Items.RAW_COPPER, Items.COPPER_INGOT),         // 粗铜 → 铜锭
            Map.entry(Items.COAL, Items.COAL),                       // 煤 → 煤
            Map.entry(Items.IRON_INGOT, Items.IRON_INGOT),           // 铁锭 → 铁锭
            Map.entry(Items.GOLD_INGOT, Items.GOLD_INGOT),           // 金锭 → 金锭
            Map.entry(Items.COPPER_INGOT, Items.COPPER_INGOT),       // 铜锭 → 铜锭
            Map.entry(Items.DIAMOND, Items.DIAMOND),                 // 钻石 → 钻石
            Map.entry(Items.EMERALD, Items.EMERALD),                 // 绿宝石 → 绿宝石
            Map.entry(Items.LAPIS_LAZULI, Items.LAPIS_LAZULI),       // 青金石 → 青金石
            Map.entry(Items.REDSTONE, Items.REDSTONE),               // 红石 → 红石
            Map.entry(Items.QUARTZ, Items.QUARTZ),                   // 下界石英 → 下界石英
            Map.entry(Items.NETHERITE_SCRAP, Items.NETHERITE_INGOT), // 下界残骸碎片 → 下界合金锭
            Map.entry(Items.ANCIENT_DEBRIS, Items.NETHERITE_SCRAP),  // 远古残骸 → 下界残骸碎片
            Map.entry(Items.AMETHYST_SHARD, Items.AMETHYST_SHARD)    // 紫水晶碎片 → 紫水晶碎片
    );

    // AE2 复制兼容（软依赖：通过注册名识别，不依赖编译期 AE2）—— 矿物机与复制机都收
    private static final Set<String> AE2_DUPLICATE_KEYS = Set.of(
            "ae2:certus_quartz_crystal",   // 赛特斯石英水晶
            "ae2:fluix_crystal",           // 福鲁伊克斯水晶
            "ae2:sky_stone_block"          // 陨石（天陨石方块）
    );

    /** 是否为 AE2 可复制物品（复制 64 个） */
    static boolean isAe2Duplicate(Item item)
    {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
        return key != null && AE2_DUPLICATE_KEYS.contains(key.toString());
    }

    /** 是否为矿石类方块（按注册名含 "ore" 判断，兼容格雷科技等模组） */
    public static boolean isOreLike(Block block)
    {
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
        return key != null && key.getPath().contains("ore");
    }

    // ==================== 作物（CROP） ====================

    // 树苗 → 对应原木
    static final Map<Item, Item> SAPLING_LOG = Map.ofEntries(
            Map.entry(Items.OAK_SAPLING, Items.OAK_LOG),
            Map.entry(Items.SPRUCE_SAPLING, Items.SPRUCE_LOG),
            Map.entry(Items.BIRCH_SAPLING, Items.BIRCH_LOG),
            Map.entry(Items.JUNGLE_SAPLING, Items.JUNGLE_LOG),
            Map.entry(Items.ACACIA_SAPLING, Items.ACACIA_LOG),
            Map.entry(Items.DARK_OAK_SAPLING, Items.DARK_OAK_LOG),
            Map.entry(Items.CHERRY_SAPLING, Items.CHERRY_LOG),
            Map.entry(Items.MANGROVE_PROPAGULE, Items.MANGROVE_LOG),
            Map.entry(Items.AZALEA, Items.OAK_LOG),
            Map.entry(Items.FLOWERING_AZALEA, Items.OAK_LOG)
    );

    /** 是否为植物类方块（树苗/花/草/蘑菇/作物/甘蔗/海带/仙人掌/竹子等，兼容所有模组） */
    public static boolean isPlantLike(Block block)
    {
        if (block instanceof GrassBlock || block == Blocks.GRASS_BLOCK)
        {
            return false; // 草方块不算植物
        }
        if (block instanceof net.minecraft.world.level.block.SaplingBlock
                || block instanceof net.minecraft.world.level.block.FlowerBlock
                || block instanceof net.minecraft.world.level.block.DoublePlantBlock
                || block instanceof net.minecraft.world.level.block.TallGrassBlock
                || block instanceof net.minecraft.world.level.block.MushroomBlock
                || block instanceof net.minecraft.world.level.block.CropBlock
                || block instanceof net.minecraft.world.level.block.SugarCaneBlock
                || block instanceof net.minecraft.world.level.block.KelpBlock
                || block instanceof net.minecraft.world.level.block.KelpPlantBlock
                || block instanceof net.minecraft.world.level.block.CactusBlock
                || block instanceof net.minecraft.world.level.block.CocoaBlock
                || block instanceof net.minecraft.world.level.block.SeaPickleBlock
                || block instanceof net.minecraft.world.level.block.SweetBerryBushBlock
                || block instanceof net.minecraft.world.level.block.NetherWartBlock
                || block instanceof net.minecraft.world.level.block.CaveVinesBlock
                || block instanceof net.minecraft.world.level.block.CaveVinesPlantBlock)
        {
            return true;
        }
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
        if (key != null)
        {
            String p = key.getPath();
            // 兜底关键字：覆盖绝大多数模组植物（格雷科技树苗、植物魔法花、多彩世界植物等）
            if (p.contains("sapling") || p.contains("flower") || p.contains("plant")
                    || p.contains("grass") || p.contains("mushroom") || p.contains("sprout")
                    || p.contains("seedling") || p.contains("propagule") || p.contains("bush")
                    || p.contains("vine") || p.contains("kelp") || p.contains("cactus")
                    || p.contains("sugar_cane") || p.contains("sugarcane") || p.contains("bamboo")
                    || p.contains("cocoa") || p.contains("wart") || p.contains("berry")
                    || p.contains("leaves") || p.contains("growable") || p.contains("crop")
                    || p.contains("mystical") || p.contains("petal"))
            {
                return true;
            }
        }
        return false;
    }

    // ==================== 方块（BLOCK） ====================

    // 原「复制配方」名单：神之复制机现在收一切物品，这份名单用于快速判定
    // 「每 tick 产 64」的快配方资格（名单外方块走 20 tick 慢配方，节奏与原神之资源一致）
    static final Set<Item> FAST_DUPLICATES = Set.copyOf(List.of(
            Items.SAND, Items.GRAVEL, Items.CLAY, Items.ANDESITE, Items.DIORITE,
            Items.GRANITE, Items.DEEPSLATE, Items.BRICKS, Items.BLACKSTONE, Items.GRASS_BLOCK,
            Items.DIRT, Items.OBSIDIAN, Items.MAGMA_BLOCK, Items.NETHERRACK, Items.END_STONE,
            Items.SOUL_SAND, Items.SOUL_SOIL,
            // 石材/矿物
            Items.CALCITE, Items.TUFF, Items.MOSS_BLOCK, Items.PRISMARINE,
            Items.BASALT, Items.CRIMSON_FUNGUS, Items.WARPED_FUNGUS, Items.LILY_PAD,
            // 下界材料
            Items.GLOWSTONE, Items.GLOWSTONE_DUST,
            // 混凝土系列（16 色）
            Items.WHITE_CONCRETE, Items.ORANGE_CONCRETE, Items.MAGENTA_CONCRETE, Items.LIGHT_BLUE_CONCRETE,
            Items.YELLOW_CONCRETE, Items.LIME_CONCRETE, Items.PINK_CONCRETE, Items.GRAY_CONCRETE,
            Items.LIGHT_GRAY_CONCRETE, Items.CYAN_CONCRETE, Items.PURPLE_CONCRETE, Items.BLUE_CONCRETE,
            Items.BROWN_CONCRETE, Items.GREEN_CONCRETE, Items.RED_CONCRETE, Items.BLACK_CONCRETE,
            // 陶瓦系列（16 色）
            Items.TERRACOTTA, Items.WHITE_TERRACOTTA, Items.ORANGE_TERRACOTTA, Items.MAGENTA_TERRACOTTA,
            Items.LIGHT_BLUE_TERRACOTTA, Items.YELLOW_TERRACOTTA, Items.LIME_TERRACOTTA, Items.PINK_TERRACOTTA,
            Items.GRAY_TERRACOTTA, Items.LIGHT_GRAY_TERRACOTTA, Items.CYAN_TERRACOTTA, Items.PURPLE_TERRACOTTA,
            Items.BLUE_TERRACOTTA, Items.BROWN_TERRACOTTA, Items.GREEN_TERRACOTTA, Items.RED_TERRACOTTA,
            Items.BLACK_TERRACOTTA,
            // 羊毛系列（16 色）
            Items.WHITE_WOOL, Items.ORANGE_WOOL, Items.MAGENTA_WOOL, Items.LIGHT_BLUE_WOOL,
            Items.YELLOW_WOOL, Items.LIME_WOOL, Items.PINK_WOOL, Items.GRAY_WOOL,
            Items.LIGHT_GRAY_WOOL, Items.CYAN_WOOL, Items.PURPLE_WOOL, Items.BLUE_WOOL,
            Items.BROWN_WOOL, Items.GREEN_WOOL, Items.RED_WOOL, Items.BLACK_WOOL
    ));

    // ==================== 变体判定 ====================

    /** 该物品是否为本变体的合法输入（方块实体 isItemValid 与生产逻辑共用） */
    public boolean accepts(ItemStack stack)
    {
        if (stack.isEmpty())
        {
            return true; // 空堆叠没有「放错机器」的问题
        }
        Item item = stack.getItem();
        return switch (this)
        {
            case ORE -> ORE_ITEM_DROPS.containsKey(item)
                    || (item instanceof BlockItem bi && isOreLike(bi.getBlock()))
                    || isAe2Duplicate(item);
            case CROP -> isCropInput(item);
            // 复制机收一切物品（v5.13.0 由「只收纯方块」放开，按用户要求）：
            // 原版 / 模组任意物品都复制 64 个，不再与另两台的过滤互斥（同一物品可进多台）。
            case DUPLICATE -> true;
        };
    }

    /** 作物机输入：作物 / 种子 / 树苗 / 各类植物 */
    static boolean isCropInput(Item item)
    {
        if (SAPLING_LOG.containsKey(item))
        {
            return true;
        }
        if (cropStateFor(item) != null)
        {
            return true;
        }
        if (item instanceof BlockItem bi)
        {
            return isPlantLike(bi.getBlock());
        }
        return false;
    }

    /** 该输入在本变体下是否为「快配方」（每 tick 产出） */
    public boolean isFastRecipe(ItemStack stack)
    {
        Item item = stack.getItem();
        return switch (this)
        {
            // 矿物机：AE2 复制品（赛特斯石英/福鲁伊克斯/天陨石）走快配方（原神之资源同款）
            case ORE -> isAe2Duplicate(item);
            // 作物机：无快配方，全部 20 tick（原神之资源的植物侧行为）
            case CROP -> false;
            // 复制机：原 DUPLICATES 名单与 AE2 天陨石每 tick，其余一切物品 20 tick
            case DUPLICATE -> FAST_DUPLICATES.contains(item) || isAe2Duplicate(item);
        };
    }

    /** 作物物品 → 对应的满熟作物方块状态（用于按原版概率计算掉落） */
    static net.minecraft.world.level.block.state.BlockState cropStateFor(Item item)
    {
        if (item == Items.WHEAT_SEEDS || item == Items.WHEAT)
        {
            return Blocks.WHEAT.defaultBlockState().setValue(net.minecraft.world.level.block.CropBlock.AGE, 7);
        }
        if (item == Items.CARROT)
        {
            return Blocks.CARROTS.defaultBlockState().setValue(net.minecraft.world.level.block.CropBlock.AGE, 7);
        }
        if (item == Items.POTATO)
        {
            return Blocks.POTATOES.defaultBlockState().setValue(net.minecraft.world.level.block.CropBlock.AGE, 7);
        }
        if (item == Items.BEETROOT_SEEDS || item == Items.BEETROOT)
        {
            // 甜菜根使用自己的 AGE 属性（0-3），不是 CropBlock.AGE
            return Blocks.BEETROOTS.defaultBlockState().setValue(net.minecraft.world.level.block.BeetrootBlock.AGE, 3);
        }
        return null;
    }

    // ==================== 产出计算 ====================

    /**
     * 根据输入物品计算产物（统一 64 个主产物 ×并行倍率由方块实体处理）。
     * 返回空列表表示无效输入（各机器 isItemValid 已挡，这里只作兜底）。
     */
    public List<net.minecraft.world.item.ItemStack> produce(Item item, net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos)
    {
        return switch (this)
        {
            case ORE -> produceOre(item, level, pos);
            case CROP -> produceCrop(item, level, pos);
            case DUPLICATE -> produceDuplicate(item);
        };
    }

    private List<net.minecraft.world.item.ItemStack> produceOre(Item item, net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos)
    {
        Item material = ORE_ITEM_DROPS.get(item);
        if (material != null)
        {
            return List.of(new net.minecraft.world.item.ItemStack(material, 64));
        }
        // 通用矿石块兼容（格雷科技等模组的矿石）：按该矿石的时运 III 掉落产出 64 个
        if (item instanceof BlockItem blockItem && isOreLike(blockItem.getBlock()))
        {
            net.minecraft.world.item.ItemStack tool = new net.minecraft.world.item.ItemStack(Items.NETHERITE_PICKAXE);
            tool.enchant(level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                    .getHolderOrThrow(net.minecraft.world.item.enchantment.Enchantments.FORTUNE), 3);
            List<net.minecraft.world.item.ItemStack> drops = Block.getDrops(blockItem.getBlock().defaultBlockState(),
                    level, pos, null, null, tool);
            return multiplyTo64(drops);
        }
        // AE2 复制品（赛特斯石英等）：复制本身
        if (isAe2Duplicate(item))
        {
            return List.of(new net.minecraft.world.item.ItemStack(item, 64));
        }
        return List.of();
    }

    private List<net.minecraft.world.item.ItemStack> produceCrop(Item item, net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos)
    {
        Item log = SAPLING_LOG.get(item);
        if (log != null)
        {
            return List.of(new net.minecraft.world.item.ItemStack(log, 64)); // 树苗 → 原木 64 个
        }

        net.minecraft.world.level.block.state.BlockState crop = cropStateFor(item);
        if (crop != null)
        {
            // 与原版种植收获概率一致，但产量 ×64
            List<net.minecraft.world.item.ItemStack> drops = Block.getDrops(crop, level, pos, null, null,
                    net.minecraft.world.item.ItemStack.EMPTY);
            return multiplyTo64(drops);
        }

        if (item instanceof BlockItem plantItem && isPlantLike(plantItem.getBlock()))
        {
            // 特定植物的专属产物（海带/甘蔗/仙人掌/浆果/地狱疣/可可等）
            List<net.minecraft.world.item.ItemStack> specific = specificPlantDrop(plantItem.getBlock());
            if (specific != null)
            {
                return specific;
            }

            // GTCEu 橡胶树苗：产出橡胶原木 + 黏性树脂副产物
            List<net.minecraft.world.item.ItemStack> gtceuRubber = gtceuRubberSaplingDrop(item);
            if (gtceuRubber != null)
            {
                return gtceuRubber;
            }

            Item logFromSapling = findLogForSapling(item);
            if (logFromSapling != null)
            {
                return List.of(new net.minecraft.world.item.ItemStack(logFromSapling, 64)); // 树苗 → 原木 64 个
            }
            // 其他植物（花/草/蘑菇等）→ 复制 64 个
            return List.of(new net.minecraft.world.item.ItemStack(item, 64));
        }
        return List.of();
    }

    private List<net.minecraft.world.item.ItemStack> produceDuplicate(Item item)
    {
        // 一切物品：复制本身 64 个（产物是该物品的全新堆叠，输入的附加数据——
        // 耐久 / 附魔 / 容器内容等——不会带到产物上）
        return List.of(new net.minecraft.world.item.ItemStack(item, 64));
    }

    /** 把掉落列表放大到主产物共 64 个（按原版概率分布缩放） */
    private static List<net.minecraft.world.item.ItemStack> multiplyTo64(List<net.minecraft.world.item.ItemStack> drops)
    {
        if (drops.isEmpty())
        {
            return List.of();
        }
        net.minecraft.world.item.ItemStack main = drops.get(0);
        if (main.isEmpty())
        {
            return List.of();
        }
        return List.of(new net.minecraft.world.item.ItemStack(main.getItem(), 64));
    }

    /** 特定可种植物品的专属产物；返回 null 表示走通用植物逻辑 */
    private static List<net.minecraft.world.item.ItemStack> specificPlantDrop(Block block)
    {
        Item item = block.asItem();
        if (block == Blocks.KELP || item == Items.KELP || block instanceof KelpBlock
                || block instanceof net.minecraft.world.level.block.KelpPlantBlock)
        {
            return List.of(new net.minecraft.world.item.ItemStack(Items.KELP, 64)); // 复制海带本身
        }
        if (block instanceof SeaPickleBlock || item == Items.SEA_PICKLE)
        {
            return List.of(new net.minecraft.world.item.ItemStack(Items.SEA_PICKLE, 64)); // 复制海泡菜
        }
        if (block instanceof CactusBlock || item == Items.CACTUS)
        {
            return List.of(new net.minecraft.world.item.ItemStack(Items.CACTUS, 64));
        }
        if (block instanceof SugarCaneBlock || item == Items.SUGAR_CANE)
        {
            return List.of(new net.minecraft.world.item.ItemStack(Items.SUGAR_CANE, 64));
        }
        if (item == Items.BAMBOO)
        {
            return List.of(new net.minecraft.world.item.ItemStack(Items.BAMBOO, 64));
        }
        if (block instanceof CocoaBlock)
        {
            return List.of(new net.minecraft.world.item.ItemStack(Items.COCOA_BEANS, 64));
        }
        if (block instanceof SweetBerryBushBlock || item == Items.SWEET_BERRIES)
        {
            return List.of(new net.minecraft.world.item.ItemStack(Items.SWEET_BERRIES, 64));
        }
        if (block instanceof NetherWartBlock || item == Items.NETHER_WART)
        {
            return List.of(new net.minecraft.world.item.ItemStack(Items.NETHER_WART, 64));
        }
        return null;
    }

    /**
     * GTCEu 橡胶树苗专属产物：橡胶原木 + 黏性树脂。
     * 通过注册名识别（gtceu:rubber_sapling / gtceu:rubber_log / gtceu:sticky_resin），不依赖编译期模组。
     */
    private static List<net.minecraft.world.item.ItemStack> gtceuRubberSaplingDrop(Item item)
    {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
        if (key == null || !key.getNamespace().equals("gtceu") || !key.getPath().equals("rubber_sapling"))
        {
            return null;
        }
        Item log = BuiltInRegistries.ITEM.get(ResourceLocation.tryBuild("gtceu", "rubber_log"));
        Item resin = BuiltInRegistries.ITEM.get(ResourceLocation.tryBuild("gtceu", "sticky_resin"));
        if (log == null || log == Items.AIR)
        {
            return null;
        }
        java.util.ArrayList<net.minecraft.world.item.ItemStack> result = new java.util.ArrayList<>();
        result.add(new net.minecraft.world.item.ItemStack(log, 8));
        if (resin != null && resin != Items.AIR)
        {
            result.add(new net.minecraft.world.item.ItemStack(resin, 4)); // 每轮固定产出 4 个黏性树脂
        }
        return result;
    }

    /** 启发式找树苗对应的原木：名字 _sapling → _log（如 rubber_sapling → rubber_log） */
    private static Item findLogForSapling(Item sapling)
    {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(sapling);
        if (key == null)
        {
            return null;
        }
        String path = key.getPath();
        if (path.endsWith("_sapling"))
        {
            String base = path.substring(0, path.length() - "sapling".length());
            Item log = BuiltInRegistries.ITEM.get(ResourceLocation.tryBuild(key.getNamespace(), base + "log"));
            if (log != null && log != Items.AIR)
            {
                return log;
            }
        }
        return null;
    }
}
