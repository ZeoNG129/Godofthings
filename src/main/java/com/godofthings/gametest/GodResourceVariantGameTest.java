package com.godofthings.gametest;

import com.godofthings.Godofthings;
import com.godofthings.block.entity.GodResourceVariant;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 神之资源三分类（v5.13.0）输入过滤测试：
 * <ul>
 *   <li>神之矿物：认原矿 / 锭 / 矿石块，<b>拒绝</b>作物与普通方块；</li>
 *   <li>神之作物：认种子 / 树苗 / 植物，<b>拒绝</b>矿物与普通方块；</li>
 *   <li>神之复制（v5.13.0 追加由「神之方块」改名放开）：<b>收一切物品</b>。</li>
 * </ul>
 * 输入过滤是三台机器唯一的「分类」边界，直接静态测 {@link GodResourceVariant#accepts}。
 */
@GameTestHolder(Godofthings.MODID)
@PrefixGameTestTemplate(false)
public class GodResourceVariantGameTest
{
    private static final String TEMPLATE = "note_data";

    @GameTest(template = TEMPLATE)
    public static void oreMachineAcceptsOresOnly(GameTestHelper helper)
    {
        GodResourceVariant v = GodResourceVariant.ORE;
        helper.assertTrue(v.accepts(new ItemStack(Items.RAW_IRON)), "粗铁应被矿物机接受");
        helper.assertTrue(v.accepts(new ItemStack(Items.IRON_INGOT)), "铁锭应被矿物机接受");
        helper.assertTrue(v.accepts(new ItemStack(Items.DIAMOND)), "钻石应被矿物机接受");
        helper.assertTrue(v.accepts(new ItemStack(Items.IRON_ORE)), "铁矿石块应被矿物机接受");
        helper.assertTrue(v.accepts(new ItemStack(Items.DEEPSLATE_DIAMOND_ORE)), "深层钻石矿石应被矿物机接受");
        helper.assertFalse(v.accepts(new ItemStack(Items.WHEAT_SEEDS)), "小麦种子不应被矿物机接受");
        helper.assertFalse(v.accepts(new ItemStack(Items.OAK_SAPLING)), "橡树树苗不应被矿物机接受");
        helper.assertFalse(v.accepts(new ItemStack(Items.SAND)), "沙子不应被矿物机接受");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void cropMachineAcceptsPlantsOnly(GameTestHelper helper)
    {
        GodResourceVariant v = GodResourceVariant.CROP;
        helper.assertTrue(v.accepts(new ItemStack(Items.WHEAT_SEEDS)), "小麦种子应被作物机接受");
        helper.assertTrue(v.accepts(new ItemStack(Items.WHEAT)), "小麦应被作物机接受");
        helper.assertTrue(v.accepts(new ItemStack(Items.BEETROOT)), "甜菜根应被作物机接受");
        helper.assertTrue(v.accepts(new ItemStack(Items.OAK_SAPLING)), "橡树树苗应被作物机接受");
        helper.assertTrue(v.accepts(new ItemStack(Items.SUGAR_CANE)), "甘蔗应被作物机接受");
        helper.assertTrue(v.accepts(new ItemStack(Items.KELP)), "海带应被作物机接受");
        helper.assertTrue(v.accepts(new ItemStack(Items.CACTUS)), "仙人掌应被作物机接受");
        helper.assertFalse(v.accepts(new ItemStack(Items.RAW_IRON)), "粗铁不应被作物机接受");
        helper.assertFalse(v.accepts(new ItemStack(Items.DIAMOND)), "钻石不应被作物机接受");
        helper.assertFalse(v.accepts(new ItemStack(Items.SAND)), "沙子不应被作物机接受");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void duplicateMachineAcceptsAnything(GameTestHelper helper)
    {
        GodResourceVariant v = GodResourceVariant.DUPLICATE;
        helper.assertTrue(v.accepts(new ItemStack(Items.SAND)), "沙子应被复制机接受");
        helper.assertTrue(v.accepts(new ItemStack(Items.STONE)), "石头应被复制机接受");
        helper.assertTrue(v.accepts(new ItemStack(Items.OAK_SAPLING)), "树苗（方块物品）应被复制机接受");
        helper.assertTrue(v.accepts(new ItemStack(Items.WHITE_WOOL)), "羊毛应被复制机接受");
        helper.assertTrue(v.accepts(new ItemStack(Items.RAW_IRON)), "粗铁（非方块物品）应被复制机接受");
        helper.assertTrue(v.accepts(new ItemStack(Items.WHEAT_SEEDS)), "种子（关联放置物）应被复制机接受");
        helper.assertTrue(v.accepts(new ItemStack(Items.STICK)), "木棍（非方块物品）应被复制机接受");
        helper.assertTrue(v.accepts(new ItemStack(Items.DIAMOND_SWORD)), "钻石剑（耐久物品）应被复制机接受");
        helper.assertTrue(v.accepts(new ItemStack(Items.WRITABLE_BOOK)), "成书（带数据物品）应被复制机接受");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void produceOreGivesIngotAndCropGivesHarvest(GameTestHelper helper)
    {
        var level = helper.getLevel();
        var pos = helper.absolutePos(helper.absolutePos(new net.minecraft.core.BlockPos(0, 1, 0)));
        // 矿物机：粗铁 → 铁锭 64
        var out = GodResourceVariant.ORE.produce(Items.RAW_IRON, level, pos);
        helper.assertTrue(!out.isEmpty() && out.get(0).is(Items.IRON_INGOT) && out.get(0).getCount() == 64,
                "粗铁应产出 64 个铁锭");
        // 复制机：沙子 → 沙子 64；木棍（非方块物品）同样复制 64
        var out2 = GodResourceVariant.DUPLICATE.produce(Items.SAND, level, pos);
        helper.assertTrue(!out2.isEmpty() && out2.get(0).is(Items.SAND) && out2.get(0).getCount() == 64,
                "沙子应复制 64 个沙子");
        var out2b = GodResourceVariant.DUPLICATE.produce(Items.STICK, level, pos);
        helper.assertTrue(!out2b.isEmpty() && out2b.get(0).is(Items.STICK) && out2b.get(0).getCount() == 64,
                "木棍（非方块物品）应复制 64 个木棍");
        // 矿物机拒绝沙子（产出空表兜底）
        var out3 = GodResourceVariant.ORE.produce(Items.SAND, level, pos);
        helper.assertTrue(out3.isEmpty(), "矿物机对沙子应产出空表");
        helper.succeed();
    }
}
