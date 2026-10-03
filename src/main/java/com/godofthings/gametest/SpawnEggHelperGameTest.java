package com.godofthings.gametest;

import com.godofthings.Godofthings;
import com.godofthings.block.entity.machine.DropLootRoller;
import com.godofthings.block.entity.machine.SpawnEggHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * 刷怪蛋兼容性测试（v5.7.0）：
 * <ul>
 *   <li>原版刷怪蛋要认（{@link SpawnEggHelper#typeOf}）</li>
 *   <li><b>模组自建的刷怪蛋物品</b>（不是 {@code SpawnEggItem}，但按原版约定写了 {@code ENTITY_DATA} 组件）也要认
 *       —— 这正是用户反馈「神之掉落机只支持原版刷怪蛋」的修复点；</li>
 *   <li>非刷怪蛋不能被误认为刷怪蛋（否则石头也能当模板）；</li>
 *   <li>神之怪蛋的复制产物要保真（同种物品、整份组件一起带走）。</li>
 * </ul>
 */
@GameTestHolder(Godofthings.MODID)
@PrefixGameTestTemplate(false)
public class SpawnEggHelperGameTest
{
    private static final String TEMPLATE = "note_data";

    /** 造一个「模组刷怪蛋」：随便一个物品 + 按原版约定写 ENTITY_DATA（实体 id 在 "id" 字段） */
    private static ItemStack fakeModdedEgg(EntityType<?> type)
    {
        ItemStack stack = new ItemStack(Items.STICK);
        CompoundTag tag = new CompoundTag();
        tag.putString("id", EntityType.getKey(type).toString());
        stack.set(DataComponents.ENTITY_DATA, CustomData.of(tag));
        return stack;
    }

    /** 原版刷怪蛋：能解析出实体类型 */
    @GameTest(template = TEMPLATE)
    public static void vanillaEggIsRecognized(GameTestHelper helper)
    {
        ItemStack egg = new ItemStack(Items.CHICKEN_SPAWN_EGG);
        helper.assertTrue(SpawnEggHelper.isSpawnEgg(egg), "原版鸡刷怪蛋应该被认成刷怪蛋");
        helper.assertTrue(SpawnEggHelper.typeOf(egg) == EntityType.CHICKEN, "原版鸡刷怪蛋应该解析出 CHICKEN");
        helper.succeed();
    }

    /** 模组自建刷怪蛋（只有 ENTITY_DATA 组件）：也要认出来 */
    @GameTest(template = TEMPLATE)
    public static void moddedEggIsRecognized(GameTestHelper helper)
    {
        ItemStack modded = fakeModdedEgg(EntityType.PIG);
        helper.assertTrue(SpawnEggHelper.isSpawnEgg(modded),
                "带 ENTITY_DATA 的模组刷怪蛋应该被认出来（修复前只认 SpawnEggItem 实例）");
        helper.assertTrue(SpawnEggHelper.typeOf(modded) == EntityType.PIG, "模组刷怪蛋应该解析出 PIG");
        helper.succeed();
    }

    /** 非刷怪蛋 / 坏数据：不能误判 */
    @GameTest(template = TEMPLATE)
    public static void nonEggIsRejected(GameTestHelper helper)
    {
        helper.assertFalse(SpawnEggHelper.isSpawnEgg(new ItemStack(Items.STONE)), "石头不是刷怪蛋");
        helper.assertFalse(SpawnEggHelper.isSpawnEgg(ItemStack.EMPTY), "空物品不是刷怪蛋");

        ItemStack broken = new ItemStack(Items.STICK);
        CompoundTag tag = new CompoundTag();
        tag.putString("id", "not_a_mod:nonexistent_entity");
        broken.set(DataComponents.ENTITY_DATA, CustomData.of(tag));
        helper.assertFalse(SpawnEggHelper.isSpawnEgg(broken), "实体 id 不存在时不能当刷怪蛋");
        helper.succeed();
    }

    /** 神之怪蛋的产物：同种蛋 64 个，且组件一起带走 */
    @GameTest(template = TEMPLATE)
    public static void duplicateKeepsItemAndData(GameTestHelper helper)
    {
        ItemStack modded = fakeModdedEgg(EntityType.PIG);
        modded.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("我的猪蛋"));
        List<ItemStack> out = SpawnEggHelper.duplicate(modded);
        helper.assertTrue(out.size() == 1, "复制应该产出 1 种产物");
        helper.assertTrue(out.get(0).is(Items.STICK), "产物应该是同种物品");
        helper.assertTrue(out.get(0).getCount() == 64, "每种产物应该 64 个");
        helper.assertTrue(out.get(0).has(DataComponents.ENTITY_DATA), "产物应该保留实体数据组件");
        helper.assertTrue(out.get(0).has(DataComponents.CUSTOM_NAME), "产物应该保留自定义名");
        helper.assertTrue(SpawnEggHelper.typeOf(out.get(0)) == EntityType.PIG, "产物应该仍是猪蛋");

        helper.assertTrue(SpawnEggHelper.duplicate(new ItemStack(Items.STONE)).isEmpty(),
                "不是刷怪蛋时不该产出任何东西");
        helper.succeed();
    }

    /** 神之掉落机：模组刷怪蛋也要能掷出战利品表（用户反馈的修复点，端到端跑一遍） */
    @GameTest(template = TEMPLATE)
    public static void dropMachineAcceptsModdedEgg(GameTestHelper helper)
    {
        List<ItemStack> fromModded = DropLootRoller.roll(helper.getLevel(), BlockPos.ZERO,
                fakeModdedEgg(EntityType.CHICKEN));
        boolean hasChicken = false;
        for (ItemStack stack : fromModded)
        {
            if (stack.is(Items.CHICKEN))
            {
                hasChicken = true;
            }
        }
        helper.assertTrue(hasChicken, "模组刷怪蛋（ENTITY_DATA 路径）也该能产出原版掉落物");
        helper.assertTrue(DropLootRoller.roll(helper.getLevel(), BlockPos.ZERO,
                new ItemStack(Items.STONE)).isEmpty(), "非刷怪蛋不该产出");
        helper.succeed();
    }
}
