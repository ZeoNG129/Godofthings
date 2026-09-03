package com.godofthings.block.entity;

import appeng.api.AECapabilities;
import appeng.api.config.Actionable;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageService;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.MEStorage;
import appeng.me.helpers.IGridConnectedBlockEntity;
import com.godofthings.Godofthings;
import com.godofthings.ae2.AeGridNode;
import com.godofthings.config.MachinesConfig;
import com.godofthings.item.GodAcceleratorItem;
import com.godofthings.menu.GodDropMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.BrushItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 神之掉落生物掉落物生产机方块实体。
 * - 无需能源，9 个输入槽（3×3）并行生产；每 20 tick 处理一轮所有非空刷怪蛋
 * - 放入刷怪蛋 → 按原版生物战利品表概率产出该生物的掉落物
 * - 不消耗刷怪蛋（生产模板，按时间持续产出）
 * - 向下自动输出，内置无限储存；打掉不掉落
 */
public class GodDropBlockEntity extends BlockEntity implements MenuProvider, IGridConnectedBlockEntity
{
    /** 工作间隔（tick），可经 godofthings-machines.toml 调整 */
    public static final int WORK_INTERVAL = MachinesConfig.DROP_WORK_INTERVAL.get();

    /** 可放置输入槽数量（3×3 共 9 个） */
    public static final int INPUT_SLOTS = 9;

    /**
     * 刷怪蛋 -> 对应生物的代表性掉落物。
     * 覆盖常见生物，避免原版战利品表给出预期之外的物品（如凋灵骷髅只掉煤炭、烈焰人掉烈焰粉）。
     */
    private static final Map<EntityType<?>, Item> EGG_DROPS = Map.ofEntries(
            Map.entry(EntityType.BLAZE, Items.BLAZE_ROD),
            Map.entry(EntityType.CREEPER, Items.GUNPOWDER),
            Map.entry(EntityType.SKELETON, Items.BONE),
            Map.entry(EntityType.WITHER_SKELETON, Items.WITHER_SKELETON_SKULL),
            Map.entry(EntityType.ZOMBIE, Items.ROTTEN_FLESH),
            Map.entry(EntityType.ZOMBIE_VILLAGER, Items.ROTTEN_FLESH),
            Map.entry(EntityType.HUSK, Items.ROTTEN_FLESH),
            Map.entry(EntityType.DROWNED, Items.ROTTEN_FLESH),
            Map.entry(EntityType.SPIDER, Items.STRING),
            Map.entry(EntityType.CAVE_SPIDER, Items.STRING),
            Map.entry(EntityType.CHICKEN, Items.FEATHER),
            Map.entry(EntityType.PIG, Items.PORKCHOP),
            Map.entry(EntityType.COW, Items.BEEF),
            Map.entry(EntityType.MOOSHROOM, Items.BEEF),
            Map.entry(EntityType.SHEEP, Items.MUTTON),
            Map.entry(EntityType.RABBIT, Items.RABBIT),
            Map.entry(EntityType.GHAST, Items.GHAST_TEAR),
            Map.entry(EntityType.MAGMA_CUBE, Items.MAGMA_CREAM),
            Map.entry(EntityType.SLIME, Items.SLIME_BALL),
            Map.entry(EntityType.ENDERMAN, Items.ENDER_PEARL),
            Map.entry(EntityType.PHANTOM, Items.PHANTOM_MEMBRANE),
            Map.entry(EntityType.SHULKER, Items.SHULKER_SHELL),
            Map.entry(EntityType.WITCH, Items.GLOWSTONE_DUST),
            Map.entry(EntityType.HOGLIN, Items.PORKCHOP),
            Map.entry(EntityType.PIGLIN, Items.GOLD_NUGGET),
            Map.entry(EntityType.PIGLIN_BRUTE, Items.GOLD_NUGGET),
            Map.entry(EntityType.ZOMBIFIED_PIGLIN, Items.ROTTEN_FLESH),
            Map.entry(EntityType.STRIDER, Items.STRING),
            Map.entry(EntityType.BEE, Items.HONEYCOMB),
            Map.entry(EntityType.SILVERFISH, Items.AIR),
            Map.entry(EntityType.ENDERMITE, Items.AIR),
            Map.entry(EntityType.BAT, Items.AIR),
            Map.entry(EntityType.VEX, Items.AIR),
            Map.entry(EntityType.ALLAY, Items.AIR),
            Map.entry(EntityType.SQUID, Items.INK_SAC),
            Map.entry(EntityType.GLOW_SQUID, Items.GLOW_INK_SAC),
            // 1.21.1：原 minecraft:scute 已更名为 minecraft:turtle_scute
            Map.entry(EntityType.TURTLE, Items.TURTLE_SCUTE),
            Map.entry(EntityType.PUFFERFISH, Items.PUFFERFISH),
            Map.entry(EntityType.COD, Items.COD),
            Map.entry(EntityType.SALMON, Items.SALMON),
            Map.entry(EntityType.TROPICAL_FISH, Items.TROPICAL_FISH),
            Map.entry(EntityType.DOLPHIN, Items.COD),
            Map.entry(EntityType.GUARDIAN, Items.PRISMARINE_SHARD),
            Map.entry(EntityType.ELDER_GUARDIAN, Items.PRISMARINE_SHARD),
            Map.entry(EntityType.SNOW_GOLEM, Items.SNOWBALL),
            Map.entry(EntityType.IRON_GOLEM, Items.IRON_INGOT),
            Map.entry(EntityType.WARDEN, Items.SCULK_CATALYST),
            Map.entry(EntityType.CAMEL, Items.AIR),
            Map.entry(EntityType.SNIFFER, Items.AIR),
            Map.entry(EntityType.FROG, Items.AIR),
            Map.entry(EntityType.TADPOLE, Items.AIR),
            Map.entry(EntityType.AXOLOTL, Items.AIR),
            Map.entry(EntityType.GOAT, Items.AIR),
            Map.entry(EntityType.WOLF, Items.AIR),
            Map.entry(EntityType.CAT, Items.AIR),
            Map.entry(EntityType.OCELOT, Items.AIR),
            Map.entry(EntityType.PARROT, Items.FEATHER),
            Map.entry(EntityType.LLAMA, Items.LEATHER),
            Map.entry(EntityType.TRADER_LLAMA, Items.LEATHER),
            Map.entry(EntityType.HORSE, Items.LEATHER),
            Map.entry(EntityType.DONKEY, Items.LEATHER),
            Map.entry(EntityType.MULE, Items.LEATHER),
            Map.entry(EntityType.SKELETON_HORSE, Items.BONE),
            Map.entry(EntityType.ZOMBIE_HORSE, Items.ROTTEN_FLESH),
            Map.entry(EntityType.POLAR_BEAR, Items.COD),
            Map.entry(EntityType.PANDA, Items.BAMBOO),
            Map.entry(EntityType.FOX, Items.SWEET_BERRIES),
            Map.entry(EntityType.RAVAGER, Items.SADDLE),
            Map.entry(EntityType.EVOKER, Items.TOTEM_OF_UNDYING),
            Map.entry(EntityType.PILLAGER, Items.AIR),
            Map.entry(EntityType.VINDICATOR, Items.AIR),
            Map.entry(EntityType.WANDERING_TRADER, Items.AIR),
            Map.entry(EntityType.VILLAGER, Items.AIR),
            Map.entry(EntityType.PLAYER, Items.AIR)
    );

    /** 明确不允许产出的物品类别（武器/工具/装备/盔甲）。
     *  TieredItem 已兜底剑斧镐铲锄；以下额外覆盖不继承 TieredItem 的武器与装备。 */
    private static final Set<Class<?>> BANNED_ITEM_CLASSES = Set.of(
            SwordItem.class,
            AxeItem.class,
            PickaxeItem.class,
            ShovelItem.class,
            HoeItem.class,
            BowItem.class,
            CrossbowItem.class,
            TridentItem.class,
            MaceItem.class,        // 1.21 重锤（武器）
            ArmorItem.class,       // 含 AnimalArmorItem（马铠）等所有盔甲
            ElytraItem.class,      // 鞘翅（装备）
            ShieldItem.class,      // 盾（装备）
            FishingRodItem.class,  // 钓鱼竿（工具）
            ShearsItem.class,      // 剪刀（工具）
            FlintAndSteelItem.class, // 打火石（工具）
            BrushItem.class        // 考古刷子（工具）
    );

    /** 可放置输入槽：3×3 共 9 个（最多同时放置 9 种刷怪蛋并行生产） */
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
            return stack.getItem() instanceof SpawnEggItem;
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

    /** 是否接入 AE（并网后产物自动输出进 AE 网络，占一个频道）。 */
    private boolean aeEnabled = true;

    /** AE 网格节点（线缆直连并网）。 */
    private final AeGridNode aeNode = new AeGridNode(this);
    private int aeTick = 0;

    private int tickCounter = 0;

    public GodDropBlockEntity(BlockPos pos, BlockState state)
    {
        super(Godofthings.GOD_DROP_BE.get(), pos, state);
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
    public IManagedGridNode getMainNode() { return aeNode.getMainNode(); }

    @Override
    public void saveChanges() { setChanged(); }

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

    // ---- 生命周期：创建/销毁 AE 网格节点 ----

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

    public static void tick(Level level, BlockPos pos, BlockState state, GodDropBlockEntity be)
    {
        if (level.isClientSide)
        {
            return;
        }
        be.tickServer();
    }

    private void tickServer()
    {
        tickCounter++;
        if (tickCounter >= WORK_INTERVAL)
        {
            tickCounter = 0;
            for (int i = 0; i < INPUT_SLOTS; i++)
            {
                process(i);
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
        List<ItemStack> outputs = produce(input);
        if (outputs.isEmpty())
        {
            return;
        }
        int mult = getParallelMultiplier();
        // 不消耗刷怪蛋：生产模板，按时间持续产出（神之加速提升并行数量）
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

    /** 根据刷怪蛋，按本模组设定产出对应掉落物（每周期 64 个）。 */
    private List<ItemStack> produce(ItemStack input)
    {
        if (!(input.getItem() instanceof SpawnEggItem egg) || !(level instanceof ServerLevel))
        {
            return List.of();
        }
        // 1.21.1：SpawnEggItem.getType 直接收 ItemStack（实体数据存于 DataComponents.ENTITY_DATA，无 NBT 标签）
        EntityType<?> type = egg.getType(input);
        if (type == null)
        {
            return List.of();
        }
        return produceByMapping(type);
    }

    /** 优先使用手动映射；未映射的生物回退到安全 loot-table 解析。 */
    private List<ItemStack> produceByMapping(EntityType<?> type)
    {
        Item mapped = EGG_DROPS.get(type);
        if (mapped == Items.AIR)
        {
            return List.of();
        }
        if (mapped != null)
        {
            ItemStack stack = new ItemStack(mapped, 64);
            // 双保险：即便未来映射表误加工具/装备，也在此拦截，杜绝产出
            return isBannedDrop(stack) ? List.of() : List.of(stack);
        }
        return produceFromLootTable(type);
    }

    /**
     * 对未在 EGG_DROPS 中显式映射的生物，从原版战利品表取掉落物。
     * 过滤掉装备/武器/工具/盔甲，并优先返回第一个非空、非禁止掉落物；若全被过滤则空。
     */
    private List<ItemStack> produceFromLootTable(EntityType<?> type)
    {
        if (!(level instanceof ServerLevel serverLevel) || level.getServer() == null)
        {
            return List.of();
        }
        try
        {
            Entity entity = type.create(level);
            if (!(entity instanceof LivingEntity living))
            {
                return List.of();
            }
            living.moveTo(worldPosition.getX() + 0.5, worldPosition.getY(), worldPosition.getZ() + 0.5, 0.0F, 0.0F);

            // 1.21.1：getLootTable 返回 ResourceKey<LootTable>；经 MinecraftServer.reloadableRegistries() 取表
            ResourceKey<LootTable> lootId = living.getLootTable();
            LootTable lootTable = serverLevel.getServer().reloadableRegistries().getLootTable(lootId);
            if (lootTable == LootTable.EMPTY)
            {
                return List.of();
            }

            Player nearest = nearestPlayer();
            DamageSource damageSource = nearest != null
                    ? serverLevel.damageSources().playerAttack(nearest)
                    : serverLevel.damageSources().generic();
            LootParams.Builder builder = new LootParams.Builder(serverLevel)
                    .withParameter(LootContextParams.THIS_ENTITY, living)
                    .withParameter(LootContextParams.DAMAGE_SOURCE, damageSource)
                    .withParameter(LootContextParams.ORIGIN, worldPosition.getCenter());
            if (nearest != null)
            {
                builder.withParameter(LootContextParams.LAST_DAMAGE_PLAYER, nearest);
            }
            LootParams params = builder.withLuck(0).create(LootContextParamSets.ENTITY);
            List<ItemStack> drops = lootTable.getRandomItems(params);
            if (drops == null || drops.isEmpty())
            {
                return List.of();
            }
            ItemStack chosen = drops.stream()
                    .filter(stack -> !stack.isEmpty() && !isBannedDrop(stack))
                    .findFirst()
                    .orElse(ItemStack.EMPTY);
            if (chosen.isEmpty())
            {
                return List.of();
            }
            return List.of(new ItemStack(chosen.getItem(), 64));
        }
        catch (Exception e)
        {
            return List.of();
        }
    }

    private Player nearestPlayer()
    {
        Player nearest = null;
        double best = 64.0 * 64.0;
        for (Player p : level.players())
        {
            double d = p.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5);
            if (d < best)
            {
                best = d;
                nearest = p;
            }
        }
        return nearest;
    }

    /** 判断物品是否属于禁止产出的装备/武器/工具/盔甲类。 */
    private static boolean isBannedDrop(ItemStack stack)
    {
        if (stack.isEmpty())
        {
            return true;
        }
        Item item = stack.getItem();
        for (Class<?> banned : BANNED_ITEM_CLASSES)
        {
            if (banned.isAssignableFrom(item.getClass()))
            {
                return true;
            }
        }
        return item instanceof TieredItem;
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
        // 1.21.1：BE 不再覆盖 getCapability；Level.getCapability(BlockCapability, BlockPos, side) 直接返回能力对象（null = 无能力）
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

    // ---- capability 注册：任意面都能取走产物 ----
    // 1.21.1：BE 不再覆盖 getCapability/LazyOptional，能力经 RegisterCapabilitiesEvent（MOD 总线）集中注册

    @EventBusSubscriber(modid = Godofthings.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static class CapabilityRegistrar
    {
        @SubscribeEvent
        public static void onRegisterCapabilities(RegisterCapabilitiesEvent event)
        {
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, Godofthings.GOD_DROP_BE.get(),
                    (be, side) -> be.getItemHandler());
            event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, Godofthings.GOD_DROP_BE.get(),
                    (be, side) -> be);
        }
    }

    // ---- NBT（1.20.5+：save/load 需 HolderLookup.Provider） ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.saveAdditional(tag, registries);
        tag.put("InputSlot", inputSlot.serializeNBT(registries));
        tag.put("AccelSlot", accelSlot.serializeNBT(registries));
        tag.put("Inventory", itemHandler.serializeNBT(registries));
        tag.putInt("TickCounter", tickCounter);
        tag.putBoolean("AeEnabled", aeEnabled);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.loadAdditional(tag, registries);
        this.aeEnabled = tag.contains("AeEnabled") ? tag.getBoolean("AeEnabled") : true;
        if (tag.contains("InputSlot"))
        {
            inputSlot.deserializeNBT(registries, tag.getCompound("InputSlot"));
        }
        if (tag.contains("AccelSlot"))
        {
            accelSlot.deserializeNBT(registries, tag.getCompound("AccelSlot"));
        }
        if (tag.contains("Inventory"))
        {
            itemHandler.deserializeNBT(registries, tag.getCompound("Inventory"));
        }
        tickCounter = tag.getInt("TickCounter");
    }

    // ---- MenuProvider ----

    @Override
    public Component getDisplayName()
    {
        return Component.translatable("block.godofthings.god_drop");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player)
    {
        return new GodDropMenu(containerId, inventory, this);
    }
}
