package com.godofthings.gametest;

import com.godofthings.Godofthings;
import com.godofthings.block.entity.machine.DropLootRoller;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * 神之掉落机「按原版战利品表产出」的回归测试。
 *
 * <p>这条链路以前完全没有测试：鸡到底会不会同时掉羽毛和生鸡肉、装备有没有被正确过滤、
 * 非生物实体（船）会不会炸 —— 只能人工进游戏放机器试。把逻辑抽到
 * {@link DropLootRoller} 之后就能直接在这儿断言了。</p>
 */
@GameTestHolder(Godofthings.MODID)
@PrefixGameTestTemplate(false)
public class DropLootRollerGameTest
{
    private static final String TEMPLATE = "note_data";

    /** 鸡：原版必掉「生鸡肉」，羽毛是 0~2 随机（可能为 0，所以只断言鸡肉必在） */
    @GameTest(template = TEMPLATE)
    public static void chickenLootRoll(GameTestHelper helper)
    {
        List<ItemStack> drops = DropLootRoller.fromLootTable(helper.getLevel(), BlockPos.ZERO, EntityType.CHICKEN);
        helper.assertFalse(drops.isEmpty(), "鸡应该有掉落（生鸡肉是原版必掉项）");
        boolean hasChicken = false;
        for (ItemStack stack : drops)
        {
            helper.assertTrue(stack.getCount() == 64,
                    "每种产物应该是 64 个，实际 " + stack.getCount() + "（" + stack.getItem() + "）");
            helper.assertFalse(DropLootRoller.isBannedDrop(stack),
                    "产物里不该有被过滤的装备：" + stack.getItem());
            if (stack.is(Items.CHICKEN))
            {
                hasChicken = true;
            }
        }
        helper.assertTrue(hasChicken, "鸡应该产出「生鸡肉」（原版必掉项）");
        helper.succeed();
    }

    /** 刷怪蛋那条入口：鸡刷怪蛋要能产出、非刷怪蛋物品不能产出任何东西 */
    @GameTest(template = TEMPLATE)
    public static void spawnEggEntryPoint(GameTestHelper helper)
    {
        List<ItemStack> viaEgg = DropLootRoller.roll(helper.getLevel(), BlockPos.ZERO,
                new ItemStack(Items.CHICKEN_SPAWN_EGG));
        boolean hasChicken = false;
        for (ItemStack stack : viaEgg)
        {
            if (stack.is(Items.CHICKEN))
            {
                hasChicken = true;
            }
        }
        helper.assertTrue(hasChicken, "鸡刷怪蛋应该产出掉落（走 SpawnEggItem.getType 那条路）");

        List<ItemStack> viaStone = DropLootRoller.roll(helper.getLevel(), BlockPos.ZERO,
                new ItemStack(Blocks.STONE.asItem()));
        helper.assertTrue(viaStone.isEmpty(), "非刷怪蛋物品不该产出任何东西");
        helper.succeed();
    }

    /** 非生物实体（船）没有战利品表 → 必须安静地返回空表，而不是抛异常 */
    @GameTest(template = TEMPLATE)
    public static void nonLivingEntityYieldsNothing(GameTestHelper helper)
    {
        List<ItemStack> boat = DropLootRoller.fromLootTable(helper.getLevel(), BlockPos.ZERO, EntityType.BOAT);
        helper.assertTrue(boat.isEmpty(), "非生物实体（船）应该产出空表");
        helper.succeed();
    }

    /** 装备过滤：武器 / 工具 / 盔甲一律过滤，材料类放行（避免刷怪蛋变免费装备机） */
    @GameTest(template = TEMPLATE)
    public static void equipmentIsFiltered(GameTestHelper helper)
    {
        helper.assertTrue(DropLootRoller.isBannedDrop(new ItemStack(Items.DIAMOND_SWORD)), "剑应该被过滤");
        helper.assertTrue(DropLootRoller.isBannedDrop(new ItemStack(Items.DIAMOND_CHESTPLATE)), "盔甲应该被过滤");
        helper.assertTrue(DropLootRoller.isBannedDrop(new ItemStack(Items.BOW)), "弓应该被过滤");
        helper.assertTrue(DropLootRoller.isBannedDrop(new ItemStack(Items.SHEARS)), "剪刀应该被过滤");
        helper.assertTrue(DropLootRoller.isBannedDrop(new ItemStack(Items.FLINT_AND_STEEL)), "打火石应该被过滤");
        helper.assertTrue(DropLootRoller.isBannedDrop(ItemStack.EMPTY), "空物品按过滤处理");
        helper.assertFalse(DropLootRoller.isBannedDrop(new ItemStack(Items.FEATHER)), "羽毛不该被过滤");
        helper.assertFalse(DropLootRoller.isBannedDrop(new ItemStack(Items.CHICKEN)), "生鸡肉不该被过滤");
        helper.succeed();
    }
}
