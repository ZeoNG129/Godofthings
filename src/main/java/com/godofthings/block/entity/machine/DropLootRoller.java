package com.godofthings.block.entity.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 神之掉落机的「按原版战利品表产出掉落物」逻辑（原来长在 {@code GodDropBlockEntity} 里，约 130 行）。
 *
 * <p>拆出来是为了两件事：① 神之掉落机的方块实体本身就很长，这段是自成一体的纯算法；
 * ② 拆成静态方法后可以**直接用 GameTest 测**（见 {@code DropLootRollerGameTest}）——
 * 之前「鸡到底会不会同时掉羽毛和生鸡肉」这种事只能靠人工在游戏里试。</p>
 *
 * <p>做法：按刷怪蛋拿到实体类型 → 建一个临时实例、取它的战利品表，用与「被玩家击杀」一致的上下文掷表
 * （附近有玩家就把玩家作为 {@code LAST_DAMAGE_PLAYER} 传进去，抢夺等条件与原版一致）→
 * 按物品合并后**每种**产物给 64 个（沿用旧口径，神之加速再按并行倍率乘）。</p>
 *
 * <p>仍会过滤掉武器 / 工具 / 盔甲（见 {@link #isBannedDrop}）—— 避免刷怪蛋变成免费装备机；
 * 这是刻意的平衡取舍，不是原版同步的遗漏。</p>
 */
public final class DropLootRoller
{
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

    private DropLootRoller() {}

    /** 根据刷怪蛋产出对应掉落物（每周期每种 64 个）；不是刷怪蛋 / 不是服务端就返回空表 */
    public static List<ItemStack> roll(ServerLevel level, BlockPos pos, ItemStack input)
    {
        if (!(input.getItem() instanceof SpawnEggItem egg))
        {
            return List.of();
        }
        // 1.21.1：SpawnEggItem.getType 直接收 ItemStack（实体数据存于 DataComponents.ENTITY_DATA，无 NBT 标签）
        EntityType<?> type = egg.getType(input);
        if (type == null)
        {
            return List.of();
        }
        return fromLootTable(level, pos, type);
    }

    /**
     * 按<b>原版战利品表</b>产出：该生物被击杀时会掉什么，这里就产出什么，<b>而且全部产物都会产出</b>。
     *
     * <p>例：鸡 → 羽毛 + 生鸡肉；凋灵骷髅 → 煤炭 + 骨头（头颅按原版概率）；猪 → 生猪排 + 皮革；
     * 深海守卫者 → 海晶碎片 + 海晶砂粒 + 生鳕鱼…</p>
     */
    public static List<ItemStack> fromLootTable(ServerLevel level, BlockPos pos, EntityType<?> type)
    {
        if (level == null || level.getServer() == null)
        {
            return List.of();
        }
        try
        {
            Entity entity = type.create(level);
            if (!(entity instanceof LivingEntity living))
            {
                return List.of(); // 船 / 矿车这类没有战利品表的非生物实体
            }
            living.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);

            // 1.21.1：getLootTable 返回 ResourceKey<LootTable>；经 MinecraftServer.reloadableRegistries() 取表
            ResourceKey<LootTable> lootId = living.getLootTable();
            LootTable lootTable = level.getServer().reloadableRegistries().getLootTable(lootId);
            if (lootTable == LootTable.EMPTY)
            {
                return List.of();
            }

            Player nearest = nearestPlayer(level, pos);
            DamageSource damageSource = nearest != null
                    ? level.damageSources().playerAttack(nearest)
                    : level.damageSources().generic();
            LootParams.Builder builder = new LootParams.Builder(level)
                    .withParameter(LootContextParams.THIS_ENTITY, living)
                    .withParameter(LootContextParams.DAMAGE_SOURCE, damageSource)
                    .withParameter(LootContextParams.ORIGIN, pos.getCenter());
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

            // v5.1.6：原来这里只取 findFirst（整只生物只产一种物品），现改为**全部产物都产出**。
            // 战利品表可能把同种物品拆成多份（例如羽毛 0-2 根），先按物品合并，避免出现多份同名产物。
            // 每种产物按 64 个产出（沿用旧口径），并过滤掉武器/工具/盔甲。
            Map<Item, ItemStack> merged = new LinkedHashMap<>();
            for (ItemStack stack : drops)
            {
                if (stack.isEmpty() || isBannedDrop(stack))
                {
                    continue;
                }
                merged.computeIfAbsent(stack.getItem(), item -> new ItemStack(item, 64));
            }
            return merged.isEmpty() ? List.of() : List.copyOf(merged.values());
        }
        catch (Exception e)
        {
            return List.of();
        }
    }

    /** 64 格内最近的玩家（作为「被玩家击杀」的上下文；没有就退回 general 伤害来源） */
    private static Player nearestPlayer(ServerLevel level, BlockPos pos)
    {
        Player nearest = null;
        double best = 64.0 * 64.0;
        for (Player p : level.players())
        {
            double d = p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            if (d < best)
            {
                best = d;
                nearest = p;
            }
        }
        return nearest;
    }

    /** 判断物品是否属于禁止产出的装备/武器/工具/盔甲类。 */
    public static boolean isBannedDrop(ItemStack stack)
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
}
