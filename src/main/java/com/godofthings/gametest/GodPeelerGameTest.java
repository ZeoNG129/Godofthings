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
 * 神之去皮（v5.15.0）回归测试：原木 → 去皮原木映射与转化纯逻辑
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
        ItemStackHandler input = new ItemStackHandler(1);
        ItemStackHandler output = new ItemStackHandler(1);
        input.setStackInSlot(0, new ItemStack(Items.OAK_LOG, 64));

        helper.assertTrue(GodPeelerBlockEntity.convertOne(input, output), "应能去皮 1 个");
        helper.assertTrue(input.getStackInSlot(0).getCount() == 63, "输入应消耗 1 个原木");
        ItemStack out = output.getStackInSlot(0);
        helper.assertTrue(out.is(Items.STRIPPED_OAK_LOG) && out.getCount() == 1, "输出应得到 1 个去皮橡木");

        // 输出满：停
        output.setStackInSlot(0, new ItemStack(Items.STRIPPED_OAK_LOG, 64));
        helper.assertFalse(GodPeelerBlockEntity.convertOne(input, output), "输出满时应停止去皮");

        // 输出是别的物品：停
        output.setStackInSlot(0, new ItemStack(Items.BIRCH_LOG, 10));
        helper.assertFalse(GodPeelerBlockEntity.convertOne(input, output), "输出为其他物品时应停止去皮");

        // 非原木：不动
        input.setStackInSlot(0, new ItemStack(Items.DIRT));
        helper.assertFalse(GodPeelerBlockEntity.convertOne(input, output), "泥土不应被去皮");

        helper.succeed();
    }

    private static void checkStrip(GameTestHelper helper, Item log, Item stripped)
    {
        ItemStack out = LogStripper.strip(new ItemStack(log, 64));
        helper.assertTrue(!out.isEmpty() && out.is(stripped) && out.getCount() == 64,
                log + " 应去皮成 " + stripped + "（数量保留）");
    }
}
