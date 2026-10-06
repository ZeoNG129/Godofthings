package com.godofthings.gametest;

import com.godofthings.Godofthings;
import com.godofthings.block.entity.GodPeelerBlockEntity;
import com.godofthings.block.entity.machine.LogStripper;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * 神之去皮（v5.15.0 / v5.15.1 改 9+9 布局）回归测试：原木 → 去皮原木映射与转化纯逻辑
 * （同 {@link GodResourceVariantGameTest} 的做法，静态测 {@link LogStripper} / {@code convertOne}）。
 */
@GameTestHolder(Godofthings.MODID)
@PrefixGameTestTemplate(false)
public class GodPeelerGameTest
{
    private static final String TEMPLATE = "note_data";

    @GameTest(template = TEMPLATE)
    public static void vanillaLogsStripToCounterpart(GameTestHelper helper)
    {
        checkStrip(helper, Items.OAK_LOG, Items.STRIPPED_OAK_LOG);
        checkStrip(helper, Items.SPRUCE_LOG, Items.STRIPPED_SPRUCE_LOG);
        checkStrip(helper, Items.BIRCH_LOG, Items.STRIPPED_BIRCH_LOG);
        checkStrip(helper, Items.JUNGLE_LOG, Items.STRIPPED_JUNGLE_LOG);
        checkStrip(helper, Items.ACACIA_LOG, Items.STRIPPED_ACACIA_LOG);
        checkStrip(helper, Items.DARK_OAK_LOG, Items.STRIPPED_DARK_OAK_LOG);
        checkStrip(helper, Items.MANGROVE_LOG, Items.STRIPPED_MANGROVE_LOG);
        checkStrip(helper, Items.CHERRY_LOG, Items.STRIPPED_CHERRY_LOG);
        checkStrip(helper, Items.CRIMSON_STEM, Items.STRIPPED_CRIMSON_STEM);
        checkStrip(helper, Items.WARPED_STEM, Items.STRIPPED_WARPED_STEM);
        checkStrip(helper, Items.BAMBOO_BLOCK, Items.STRIPPED_BAMBOO_BLOCK);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void woodAndHyphaeAlsoStrip(GameTestHelper helper)
    {
        checkStrip(helper, Items.OAK_WOOD, Items.STRIPPED_OAK_WOOD);
        checkStrip(helper, Items.SPRUCE_WOOD, Items.STRIPPED_SPRUCE_WOOD);
        checkStrip(helper, Items.CRIMSON_HYPHAE, Items.STRIPPED_CRIMSON_HYPHAE);
        checkStrip(helper, Items.WARPED_HYPHAE, Items.STRIPPED_WARPED_HYPHAE);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void alreadyStrippedAndNonLogsRejected(GameTestHelper helper)
    {
        helper.assertTrue(LogStripper.strip(new ItemStack(Items.STRIPPED_OAK_LOG)).isEmpty(),
                "已去皮的橡木不应再被去皮");
        helper.assertTrue(LogStripper.strip(new ItemStack(Items.DIRT)).isEmpty(), "泥土不是原木");
        helper.assertTrue(LogStripper.strip(new ItemStack(Items.STICK)).isEmpty(), "木棍不是原木");
        helper.assertTrue(LogStripper.strip(ItemStack.EMPTY).isEmpty(), "空堆叠不是原木");
        helper.assertTrue(LogStripper.isStrippable(new ItemStack(Items.OAK_LOG)), "橡木原木应可去皮");
        helper.assertFalse(LogStripper.isStrippable(new ItemStack(Items.STRIPPED_OAK_LOG)), "已去皮的橡木应被拒绝");
        helper.assertFalse(LogStripper.isStrippable(new ItemStack(Items.DIRT)), "泥土应被拒绝");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void convertOneMovesInputToOutput(GameTestHelper helper)
    {
        ItemStackHandler handler = new ItemStackHandler(GodPeelerBlockEntity.TOTAL_SLOTS);
        handler.setStackInSlot(0, new ItemStack(Items.OAK_LOG, 64));

        helper.assertTrue(GodPeelerBlockEntity.convertOne(handler, 0, GodPeelerBlockEntity.OUTPUT_SLOT_START, 1) == 1,
                "倍率 1 应去皮 1 个");
        helper.assertTrue(handler.getStackInSlot(0).getCount() == 63, "输入应消耗 1 个原木");
        ItemStack out = handler.getStackInSlot(GodPeelerBlockEntity.OUTPUT_SLOT_START);
        helper.assertTrue(out.is(Items.STRIPPED_OAK_LOG) && out.getCount() == 1, "对应输出槽应得到 1 个去皮橡木");

        // 输出满：停（原生 ItemStackHandler.getSlotLimit = 99，塞满 99 个才是真满）
        handler.setStackInSlot(GodPeelerBlockEntity.OUTPUT_SLOT_START, new ItemStack(Items.STRIPPED_OAK_LOG, 99));
        helper.assertTrue(GodPeelerBlockEntity.convertOne(handler, 0, GodPeelerBlockEntity.OUTPUT_SLOT_START, 1) == 0,
                "输出满时应停止去皮");

        // 输出是别的物品：停
        handler.setStackInSlot(GodPeelerBlockEntity.OUTPUT_SLOT_START, new ItemStack(Items.BIRCH_LOG, 10));
        helper.assertTrue(GodPeelerBlockEntity.convertOne(handler, 0, GodPeelerBlockEntity.OUTPUT_SLOT_START, 1) == 0,
                "输出为其他物品时应停止去皮");

        // 非原木：不动
        handler.setStackInSlot(0, new ItemStack(Items.DIRT));
        helper.assertTrue(GodPeelerBlockEntity.convertOne(handler, 0, GodPeelerBlockEntity.OUTPUT_SLOT_START, 1) == 0,
                "泥土不应被去皮");

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void parallelMultiplierConvertsMorePerSlot(GameTestHelper helper)
    {
        ItemStackHandler handler = new ItemStackHandler(GodPeelerBlockEntity.TOTAL_SLOTS);
        // 倍率 16：一个槽一次去皮 16 个
        handler.setStackInSlot(0, new ItemStack(Items.OAK_LOG, 64));
        helper.assertTrue(GodPeelerBlockEntity.convertOne(handler, 0, GodPeelerBlockEntity.OUTPUT_SLOT_START, 16) == 16,
                "倍率 16 应一次去皮 16 个");
        helper.assertTrue(handler.getStackInSlot(0).getCount() == 48, "输入应消耗 16 个原木");
        helper.assertTrue(handler.getStackInSlot(GodPeelerBlockEntity.OUTPUT_SLOT_START).getCount() == 16,
                "输出应得到 16 个去皮橡木");

        // 输入不足倍率时按剩余量去皮
        helper.assertTrue(GodPeelerBlockEntity.convertOne(handler, 0, GodPeelerBlockEntity.OUTPUT_SLOT_START, 16) == 16,
                "第二轮仍应去皮 16 个");
        helper.assertTrue(GodPeelerBlockEntity.convertOne(handler, 0, GodPeelerBlockEntity.OUTPUT_SLOT_START, 16) == 16,
                "第三轮仍应去皮 16 个");
        helper.assertTrue(GodPeelerBlockEntity.convertOne(handler, 0, GodPeelerBlockEntity.OUTPUT_SLOT_START, 16) == 16,
                "第四轮仍应去皮 16 个");
        helper.assertTrue(handler.getStackInSlot(0).isEmpty(), "64 个原木四轮（16×4）后应耗尽");
        helper.assertTrue(handler.getStackInSlot(GodPeelerBlockEntity.OUTPUT_SLOT_START).getCount() == 64,
                "输出应累计 64 个去皮橡木");

        // 倍率 0 / 负数：不动
        handler.setStackInSlot(1, new ItemStack(Items.BIRCH_LOG, 8));
        helper.assertTrue(GodPeelerBlockEntity.convertOne(handler, 1, GodPeelerBlockEntity.OUTPUT_SLOT_START + 1, 0) == 0,
                "倍率 0 不应去皮");
        helper.assertTrue(handler.getStackInSlot(1).getCount() == 8, "倍率 0 不应消耗输入");

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void nineInputSlotsStripIndependently(GameTestHelper helper)
    {
        ItemStackHandler handler = new ItemStackHandler(GodPeelerBlockEntity.TOTAL_SLOTS);
        // 不同输入槽放不同原木，各自转化到一一对应的输出槽（v5.15.1 的 9+9 布局）
        handler.setStackInSlot(0, new ItemStack(Items.OAK_LOG, 12));
        handler.setStackInSlot(4, new ItemStack(Items.BIRCH_LOG, 7));
        handler.setStackInSlot(8, new ItemStack(Items.CRIMSON_STEM, 3));

        helper.assertTrue(GodPeelerBlockEntity.convertOne(handler, 0, GodPeelerBlockEntity.OUTPUT_SLOT_START, 4) == 4,
                "槽 0 橡木应去皮 4 个");
        helper.assertTrue(GodPeelerBlockEntity.convertOne(handler, 4, GodPeelerBlockEntity.OUTPUT_SLOT_START + 4, 4) == 4,
                "槽 4 白桦木应去皮 4 个");
        helper.assertTrue(GodPeelerBlockEntity.convertOne(handler, 8, GodPeelerBlockEntity.OUTPUT_SLOT_START + 8, 4) == 3,
                "槽 8 绯红菌柄输入不足按剩余 3 个去皮");

        helper.assertTrue(handler.getStackInSlot(GodPeelerBlockEntity.OUTPUT_SLOT_START).is(Items.STRIPPED_OAK_LOG)
                && handler.getStackInSlot(GodPeelerBlockEntity.OUTPUT_SLOT_START).getCount() == 4, "槽 9 应是 4 个去皮橡木");
        helper.assertTrue(handler.getStackInSlot(GodPeelerBlockEntity.OUTPUT_SLOT_START + 4).is(Items.STRIPPED_BIRCH_LOG)
                && handler.getStackInSlot(GodPeelerBlockEntity.OUTPUT_SLOT_START + 4).getCount() == 4, "槽 13 应是 4 个去皮白桦木");
        helper.assertTrue(handler.getStackInSlot(GodPeelerBlockEntity.OUTPUT_SLOT_START + 8).is(Items.STRIPPED_CRIMSON_STEM)
                && handler.getStackInSlot(GodPeelerBlockEntity.OUTPUT_SLOT_START + 8).getCount() == 3, "槽 17 应是 3 个去皮绯红菌柄");
        helper.assertTrue(handler.getStackInSlot(4).getCount() == 3 && handler.getStackInSlot(0).getCount() == 8,
                "各输入槽消耗互不干扰");

        helper.succeed();
    }

    private static void checkStrip(GameTestHelper helper, Item log, Item stripped)
    {
        ItemStack out = LogStripper.strip(new ItemStack(log, 64));
        helper.assertTrue(!out.isEmpty() && out.is(stripped) && out.getCount() == 64,
                log + " 应去皮成 " + stripped + "（数量保留）");
    }
}
