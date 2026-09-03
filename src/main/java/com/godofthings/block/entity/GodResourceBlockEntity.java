package com.godofthings.block.entity;

import appeng.api.AECapabilities;
import appeng.api.config.Actionable;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageService;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.MEStorage;
import appeng.api.util.AECableType;
import com.godofthings.Godofthings;
import com.godofthings.ae2.AeGridNode;
import com.godofthings.config.MachinesConfig;
import com.godofthings.item.GodAcceleratorItem;
import com.godofthings.menu.GodResourceMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeetrootBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.CaveVinesBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.KelpBlock;
import net.minecraft.world.level.block.MushroomBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.TallGrassBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 神之资源自动生产机方块实体。
 * - 无需能源，9 个输入槽（3×3）并行生产；快配方每 tick、慢配方每 20 tick 处理一轮
 * - 树苗 → 对应原木 8 个 + 10% 概率额外 1 个树苗
 * - 作物/种子 → 按原版种植收获概率产出（使用原版满熟作物战利品表）
 * - 矿石块 → 对应的原矿 64 个
 * - 向下自动输出，内置无限储存；打掉不掉落
 */
public class GodResourceBlockEntity extends BlockEntity implements MenuProvider, IInWorldGridNodeHost, IActionHost
{
    /** 工作间隔（tick），可经 godofthings-machines.toml 调整 */
    public static final int WORK_INTERVAL = MachinesConfig.RESOURCE_WORK_INTERVAL.get();

    /** 可放置输入槽数量（3×3 共 9 个） */
    public static final int INPUT_SLOTS = 9;

    // 树苗 → 对应原木
    private static final Map<Item, Item> SAPLING_LOG = Map.ofEntries(
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

    // 复制配方：放入一个，每 tick 产 64 个相同物品
    private static final Set<Item> DUPLICATES = Set.copyOf(List.of(
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

    // 原矿石 → 产出物（输入原矿石，产出对应的锭/成品，统一每 20 tick 64 个）
    private static final Map<Item, Item> ORE_ITEM_DROPS = Map.ofEntries(
            Map.entry(Items.RAW_IRON, Items.IRON_INGOT),          // 粗铁 → 铁锭
            Map.entry(Items.RAW_GOLD, Items.GOLD_INGOT),          // 粗金 → 金锭
            Map.entry(Items.RAW_COPPER, Items.COPPER_INGOT),      // 粗铜 → 铜锭
            Map.entry(Items.COAL, Items.COAL),                    // 煤 → 煤
            Map.entry(Items.IRON_INGOT, Items.IRON_INGOT),        // 铁锭 → 铁锭
            Map.entry(Items.GOLD_INGOT, Items.GOLD_INGOT),        // 金锭 → 金锭
            Map.entry(Items.COPPER_INGOT, Items.COPPER_INGOT),    // 铜锭 → 铜锭
            Map.entry(Items.DIAMOND, Items.DIAMOND),              // 钻石 → 钻石
            Map.entry(Items.EMERALD, Items.EMERALD),              // 绿宝石 → 绿宝石
            Map.entry(Items.LAPIS_LAZULI, Items.LAPIS_LAZULI),    // 青金石 → 青金石
            Map.entry(Items.REDSTONE, Items.REDSTONE),            // 红石 → 红石
            Map.entry(Items.QUARTZ, Items.QUARTZ),                // 下界石英 → 下界石英
            Map.entry(Items.NETHERITE_SCRAP, Items.NETHERITE_INGOT), // 下界残骸碎片 → 下界合金锭
            Map.entry(Items.ANCIENT_DEBRIS, Items.NETHERITE_SCRAP),  // 远古残骸 → 下界残骸碎片
            Map.entry(Items.AMETHYST_SHARD, Items.AMETHYST_SHARD)    // 紫水晶碎片 → 紫水晶碎片
    );

    // AE2 复制兼容（软依赖：通过注册名识别，不依赖编译期 AE2）
    private static final Set<String> AE2_DUPLICATE_KEYS = Set.of(
            "ae2:certus_quartz_crystal",   // 赛特斯石英水晶
            "ae2:fluix_crystal",           // 福鲁伊克斯水晶
            "ae2:sky_stone_block"          // 陨石（天陨石方块）
    );

    /** 是否为 AE2 可复制物品（每 tick 复制 64 个，与 DUPLICATES 相同） */
    private static boolean isAe2Duplicate(Item item)
    {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
        return key != null && AE2_DUPLICATE_KEYS.contains(key.toString());
    }

    /** 可放置输入槽：3×3 共 9 个（最多同时放置 9 种模板并行生产） */
    private final ItemStackHandler inputSlot = new ItemStackHandler(INPUT_SLOTS)
    {
        @Override
        protected void onContentsChanged(int slot)
        {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack)
        {
            return isValidInput(stack);
        }

        @Override
        public void setSize(int size)
        {
            // 槽数固定为 INPUT_SLOTS：旧世界（2.0.5）保存的 NBT 里 Size=1，
            // deserializeNBT 会调 setSize(1) 缩槽，导致 tickServer 遍历 9 槽时越界崩溃
            // （Slot 1 not in valid range - [0,1)）。忽略非 INPUT_SLOTS 的 setSize，保持 9 槽。
            if (size != INPUT_SLOTS)
            {
                return;
            }
            super.setSize(size);
        }
    };
    private final InfiniteItemHandler itemHandler = new InfiniteItemHandler();

    /** 神之加速槽：放入神之加速提升并行数量（最多一组 64 个 = 1024 倍） */
    private final ItemStackHandler accelSlot = new ItemStackHandler(1)
    {
        @Override
        protected void onContentsChanged(int slot)
        {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack)
        {
            return stack.getItem() instanceof GodAcceleratorItem;
        }
    };

    private int tickCounter = 0;

    /** 是否接入 AE（并网后产物自动输出进 AE 网络，占一个频道）。 */
    private boolean aeEnabled = true;

    /** AE 网格节点（线缆直连并网）。 */
    private final AeGridNode aeNode = new AeGridNode(this);
    private int aeTick = 0;

    public GodResourceBlockEntity(BlockPos pos, BlockState state)
    {
        super(Godofthings.GOD_RESOURCE_BE.get(), pos, state);
        itemHandler.setOnChange(this::setChanged);
    }

    public ItemStackHandler getInputSlot()
    {
        return inputSlot;
    }

    public InfiniteItemHandler getItemHandler()
    {
        return itemHandler;
    }

    /** 神之加速槽（只接受神之加速，最多 64 个） */
    public ItemStackHandler getAccelSlot()
    {
        return accelSlot;
    }

    /** 并行倍率：每个神之加速 16 倍，最多一组（64 个）= 1024 倍。无加速时为 1。 */
    public int getParallelMultiplier()
    {
        int count = accelSlot.getStackInSlot(0).getCount();
        return count <= 0 ? 1 : count * 16;
    }

    public int getStorageCount()
    {
        return itemHandler.getStacks().size();
    }

    public boolean isAeEnabled()
    {
        return aeEnabled;
    }

    public void toggleAeEnabled()
    {
        this.aeEnabled = !this.aeEnabled;
        setChanged();
    }

    // ---- AE 网格节点（线缆直连并网，产物自动输出进 AE） ----

    @Override
    public IGridNode getGridNode(Direction side)
    {
        return aeNode.getGridNode(side);
    }

    @Override
    public AECableType getCableConnectionType(Direction side)
    {
        return aeNode.getCableConnectionType(side);
    }

    @Override
    public IGridNode getActionableNode()
    {
        return aeNode.getActionableNode();
    }

    /** 把产物推入 AE 网络（节流由 tick 控制）。 */
    private void pushOutputToAe()
    {
        if (!aeEnabled || !aeNode.isActive())
        {
            return;
        }
        IStorageService storage = aeNode.getStorage();
        if (storage == null)
        {
            return;
        }
        MEStorage inv = storage.getInventory();
        IActionSource source = aeNode.actionSource();
        for (int slot = 0; slot < getItemHandler().getSlots(); slot++)
        {
            ItemStack stack = getItemHandler().getStackInSlot(slot);
            if (stack.isEmpty())
            {
                continue;
            }
            long inserted = inv.insert(AEItemKey.of(stack), stack.getCount(), Actionable.MODULATE, source);
            if (inserted > 0)
            {
                getItemHandler().extractItem(slot, (int) inserted, false);
            }
        }
    }

    @Override
    public void onLoad()
    {
        super.onLoad();
        aeNode.create(level, worldPosition);
    }

    @Override
    public void setRemoved()
    {
        aeNode.destroy();
        super.setRemoved();
    }

    // ---- 每 tick 逻辑 ----

    public static void tick(Level level, BlockPos pos, BlockState state, GodResourceBlockEntity be)
    {
        if (level.isClientSide)
        {
            return;
        }
        be.tickServer();
    }

    private void tickServer()
    {
        // 快配方（复制类：矿石/混凝土/羊毛等）：每 tick 处理所有非空输入槽
        for (int i = 0; i < INPUT_SLOTS; i++)
        {
            ItemStack input = inputSlot.getStackInSlot(i);
            if (!input.isEmpty() && (DUPLICATES.contains(input.getItem()) || isAe2Duplicate(input.getItem())))
            {
                process(i); // 复制配方：每 tick 产 64 个
            }
        }
        // 慢配方（树苗/作物/原矿等）：每 WORK_INTERVAL tick 处理一轮所有非空输入槽
        tickCounter++;
        if (tickCounter >= WORK_INTERVAL)
        {
            tickCounter = 0;
            for (int i = 0; i < INPUT_SLOTS; i++)
            {
                ItemStack input = inputSlot.getStackInSlot(i);
                if (!input.isEmpty() && !(DUPLICATES.contains(input.getItem()) || isAe2Duplicate(input.getItem())))
                {
                    process(i);
                }
            }
        }
        pushDown();
        // AE 产物输出节流：每 20 tick（1 秒）推一次
        aeTick++;
        if (aeTick >= 20)
        {
            aeTick = 0;
            pushOutputToAe();
        }
    }

    private void process(int slot)
    {
        ItemStack input = inputSlot.getStackInSlot(slot);
        if (input.isEmpty())
        {
            return;
        }
        List<ItemStack> outputs = produce(input.getItem());
        if (outputs.isEmpty())
        {
            return; // 无效输入
        }
        int mult = getParallelMultiplier();
        // 不消耗原材料：输入只是生产模板，按时间持续产出（神之加速提升并行数量）
        for (ItemStack out : outputs)
        {
            if (!out.isEmpty())
            {
                ItemStack toInsert = out.copyWithCount(out.getCount() * mult);
                ItemStack leftover = itemHandler.insertItem(-1, toInsert, false);
                if (!leftover.isEmpty())
                {
                    InfiniteItemHandler.dropRemainder(level, worldPosition, leftover);
                }
            }
        }
    }

    /** 根据输入物品计算产物（统一每 20 tick 产 64 个主产物） */
    private List<ItemStack> produce(Item item)
    {
        Item log = SAPLING_LOG.get(item);
        if (log != null)
        {
            return List.of(new ItemStack(log, 64)); // 树苗 → 原木 64 个
        }

        BlockState crop = cropStateFor(item);
        if (crop != null)
        {
            // 与原版种植收获概率一致，但产量 ×64
            List<ItemStack> drops = Block.getDrops(crop, (ServerLevel) level, worldPosition, null, null, ItemStack.EMPTY);
            return multiplyTo64(drops);
        }

        Item material = ORE_ITEM_DROPS.get(item);
        if (material != null)
        {
            return List.of(new ItemStack(material, 64));
        }

        if (DUPLICATES.contains(item) || isAe2Duplicate(item))
        {
            return List.of(new ItemStack(item, 64)); // 复制：64 个相同物品
        }

        // 通用矿石块兼容（格雷科技等模组的矿石）：按该矿石的原版掉落产出 64 个
        if (item instanceof BlockItem blockItem && isOreLike(blockItem.getBlock()))
        {
            ItemStack tool = new ItemStack(Items.NETHERITE_PICKAXE);
            tool.enchant(level.registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                    .getHolderOrThrow(Enchantments.FORTUNE), 3);
            List<ItemStack> drops = Block.getDrops(blockItem.getBlock().defaultBlockState(),
                    (ServerLevel) level, worldPosition, null, null, tool);
            return multiplyTo64(drops);
        }

        // 通用植物兼容（格雷科技树苗、神秘花等所有植物）
        if (item instanceof BlockItem plantItem && isPlantLike(plantItem.getBlock()))
        {
            // 特定植物的专属产物
            List<ItemStack> specific = specificPlantDrop(plantItem.getBlock());
            if (specific != null)
            {
                return specific;
            }

            // GTCEu 橡胶树苗：产出橡胶原木 + 黏性树脂副产物
            List<ItemStack> gtceuRubber = gtceuRubberSaplingDrop(item);
            if (gtceuRubber != null)
            {
                return gtceuRubber;
            }

            Item logFromSapling = findLogForSapling(item);
            if (logFromSapling != null)
            {
                return List.of(new ItemStack(logFromSapling, 64)); // 树苗 → 原木 64 个
            }
            // 其他植物（花/草/蘑菇等）→ 复制 64 个
            return List.of(new ItemStack(item, 64));
        }

        return List.of();
    }

    /** 把掉落列表放大到主产物共 64 个（按原版概率分布缩放） */
    private List<ItemStack> multiplyTo64(List<ItemStack> drops)
    {
        if (drops.isEmpty())
        {
            return List.of();
        }
        ItemStack main = drops.get(0);
        if (main.isEmpty())
        {
            return List.of();
        }
        return List.of(new ItemStack(main.getItem(), 64));
    }

    /**
     * GTCEu 橡胶树苗专属产物：橡胶原木 + 黏性树脂。
     * 通过注册名识别（gtceu:rubber_sapling / gtceu:rubber_log / gtceu:sticky_resin），不依赖编译期模组。
     */
    private List<ItemStack> gtceuRubberSaplingDrop(Item item)
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
        List<ItemStack> result = new ArrayList<>();
        result.add(new ItemStack(log, 8));
        if (resin != null && resin != Items.AIR)
        {
            result.add(new ItemStack(resin, 4)); // 每轮固定产出 4 个黏性树脂
        }
        return result;
    }

    /** 特定可种植物品的专属产物；返回 null 表示走通用植物逻辑 */
    private List<ItemStack> specificPlantDrop(Block block)
    {
        Item item = block.asItem();
        if (block == Blocks.KELP || item == Items.KELP || block instanceof KelpBlock)
        {
            return List.of(new ItemStack(Items.KELP, 64)); // 复制海带本身
        }
        if (block == Blocks.KELP_PLANT)
        {
            return List.of(new ItemStack(Items.KELP, 64));
        }
        if (block instanceof SeaPickleBlock || item == Items.SEA_PICKLE)
        {
            return List.of(new ItemStack(Items.SEA_PICKLE, 64)); // 复制海泡菜
        }
        if (block instanceof CactusBlock || item == Items.CACTUS)
        {
            return List.of(new ItemStack(Items.CACTUS, 64));
        }
        if (block instanceof SugarCaneBlock || item == Items.SUGAR_CANE)
        {
            return List.of(new ItemStack(Items.SUGAR_CANE, 64));
        }
        if (item == Items.BAMBOO)
        {
            return List.of(new ItemStack(Items.BAMBOO, 64));
        }
        if (block instanceof CocoaBlock)
        {
            return List.of(new ItemStack(Items.COCOA_BEANS, 64));
        }
        if (block instanceof SweetBerryBushBlock || item == Items.SWEET_BERRIES)
        {
            return List.of(new ItemStack(Items.SWEET_BERRIES, 64));
        }
        if (block instanceof NetherWartBlock || item == Items.NETHER_WART)
        {
            return List.of(new ItemStack(Items.NETHER_WART, 64));
        }
        return null;
    }

    /** 是否为植物类方块（树苗/花/草/蘑菇/作物/甘蔗/海带/仙人掌/竹子等，兼容所有模组） */
    public static boolean isPlantLike(Block block)
    {
        if (block instanceof GrassBlock)
        {
            return false; // 草方块不算植物
        }
        if (block instanceof SaplingBlock || block instanceof FlowerBlock || block instanceof DoublePlantBlock
                || block instanceof TallGrassBlock || block instanceof MushroomBlock || block instanceof CropBlock
                || block instanceof SugarCaneBlock || block instanceof KelpBlock || block instanceof CactusBlock
                || block instanceof CocoaBlock || block instanceof SeaPickleBlock
                || block instanceof SweetBerryBushBlock || block instanceof NetherWartBlock || block instanceof CaveVinesBlock)
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

    /** 是否为矿石类方块（按注册名含 "ore" 判断，兼容格雷科技等模组） */
    public static boolean isOreLike(Block block)
    {
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
        return key != null && key.getPath().contains("ore");
    }

    /** 判断是否为有效输入（树苗/作物/原矿石/复制物品/矿石块/植物） */
    public static boolean isValidInput(ItemStack stack)
    {
        Item item = stack.getItem();
        return SAPLING_LOG.containsKey(item) || cropStateFor(item) != null
                || ORE_ITEM_DROPS.containsKey(item) || DUPLICATES.contains(item)
                || isAe2Duplicate(item)
                || (item instanceof BlockItem blockItem
                        && (isOreLike(blockItem.getBlock()) || isPlantLike(blockItem.getBlock())));
    }

    /** 作物物品 → 对应的满熟作物方块状态（用于按原版概率计算掉落） */
    private static BlockState cropStateFor(Item item)
    {
        if (item == Items.WHEAT_SEEDS || item == Items.WHEAT)
        {
            return Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7);
        }
        if (item == Items.CARROT)
        {
            return Blocks.CARROTS.defaultBlockState().setValue(CropBlock.AGE, 7);
        }
        if (item == Items.POTATO)
        {
            return Blocks.POTATOES.defaultBlockState().setValue(CropBlock.AGE, 7);
        }
        if (item == Items.BEETROOT_SEEDS || item == Items.BEETROOT)
        {
            // 甜菜根使用自己的 AGE 属性（0-3），不是 CropBlock.AGE
            return Blocks.BEETROOTS.defaultBlockState().setValue(BeetrootBlock.AGE, 3);
        }
        return null;
    }

    /** 向下自动输出到下方容器 */
    private void pushDown()
    {
        BlockPos below = worldPosition.below();
        if (!level.isLoaded(below))
        {
            return;
        }
        BlockEntity neighbor = level.getBlockEntity(below);
        if (neighbor == null)
        {
            return;
        }
        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, below, Direction.UP);
        if (handler == null)
        {
            return;
        }
        while (true)
        {
            ItemStack stack = itemHandler.getStackInSlot(0);
            if (stack.isEmpty())
            {
                break;
            }
            ItemStack toPush = stack.copy();
            boolean anyMoved = false;
            for (int s = 0; s < handler.getSlots(); s++)
            {
                ItemStack leftover = handler.insertItem(s, toPush, false);
                int moved = toPush.getCount() - leftover.getCount();
                if (moved > 0)
                {
                    itemHandler.extractItem(0, moved, false);
                    anyMoved = true;
                }
                toPush = leftover;
                if (toPush.isEmpty())
                {
                    break;
                }
            }
            if (!anyMoved)
            {
                break;
            }
        }
    }

    // ---- capability：任意面都能取走产物 ----

    @EventBusSubscriber(modid = Godofthings.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static class CapabilityRegistration
    {
        @SubscribeEvent
        public static void registerCapabilities(RegisterCapabilitiesEvent event)
        {
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, Godofthings.GOD_RESOURCE_BE.get(),
                    (be, side) -> be.itemHandler);
            event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, Godofthings.GOD_RESOURCE_BE.get(),
                    (be, side) -> be);
        }
    }

    // ---- NBT ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider)
    {
        super.saveAdditional(tag, provider);
        tag.put("InputSlot", inputSlot.serializeNBT(provider));
        tag.put("AccelSlot", accelSlot.serializeNBT(provider));
        tag.put("Inventory", itemHandler.serializeNBT(provider));
        tag.putInt("TickCounter", tickCounter);
        tag.putBoolean("AeEnabled", aeEnabled);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider)
    {
        super.loadAdditional(tag, provider);
        if (tag.contains("InputSlot"))
        {
            inputSlot.deserializeNBT(provider, tag.getCompound("InputSlot"));
        }
        if (tag.contains("AccelSlot"))
        {
            accelSlot.deserializeNBT(provider, tag.getCompound("AccelSlot"));
        }
        if (tag.contains("Inventory"))
        {
            itemHandler.deserializeNBT(provider, tag.getCompound("Inventory"));
        }
        tickCounter = tag.getInt("TickCounter");
        this.aeEnabled = tag.contains("AeEnabled") ? tag.getBoolean("AeEnabled") : true;
    }

    // ---- MenuProvider ----

    @Override
    public Component getDisplayName()
    {
        return Component.translatable("block.godofthings.god_resource");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player)
    {
        return new GodResourceMenu(containerId, inventory, this);
    }
}
