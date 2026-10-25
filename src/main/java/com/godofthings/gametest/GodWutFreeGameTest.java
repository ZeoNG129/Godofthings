package com.godofthings.gametest;

import com.godofthings.Godofthings;
import com.godofthings.wt.core.WtMagnetMode;
import com.godofthings.wt.core.WtTerminalDefinition;
import com.godofthings.wt.core.WtUniversalTerminalItem;
import com.godofthings.wt.WtRegistration;
import com.godofthings.wt.recipe.WtWutCombine;
import com.godofthings.wt.recipe.WtWutUpgrade;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 无耗能无线通用终端（v7.15.1 完全独立实现）回归测试：注册表 / 无耗能 / 磁卡模式 /
 * 合成产物 / 终端切换，全程不引用任何 ae2wtlib 类。
 */
@GameTestHolder(Godofthings.MODID)
@PrefixGameTestTemplate(false)
public class GodWutFreeGameTest
{
    private static final String TEMPLATE = "note_data";

    @GameTest(template = TEMPLATE)
    public static void terminalRegistryLoaded(GameTestHelper helper)
    {
        // 三台终端定义已注册：crafting（复用 AE2 物品）+ pattern_encoding / pattern_access
        helper.assertTrue(WtTerminalDefinition.wirelessTerminalList().size() == 3,
                "终端定义注册表应有 3 台终端");
        helper.assertTrue(WtRegistration.CRAFTING().item() == appeng.core.definitions.AEItems
                        .WIRELESS_CRAFTING_TERMINAL.get(),
                "crafting 终端应复用 AE2 原版无线合成终端物品");
        helper.assertTrue(WtRegistration.PATTERN_ENCODING().item() == Godofthings
                        .WIRELESS_PATTERN_ENCODING_TERMINAL.get(),
                "pattern_encoding 终端应为本模组物品");
        helper.assertTrue(WtRegistration.PATTERN_ACCESS().item() == Godofthings
                        .WIRELESS_PATTERN_ACCESS_TERMINAL.get(),
                "pattern_access 终端应为本模组物品");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void noPowerFive(GameTestHelper helper)
    {
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var stack = new ItemStack(Godofthings.WIRELESS_UNIVERSAL_TERMINAL_FREE.get());
        var item = (WtUniversalTerminalItem) stack.getItem();
        // 无耗能五连：耗电恒成功 / 有电恒 true / 电量恒满 / 充电速率 0 / 抽能恒返回请求数
        helper.assertTrue(item.usePower(player, 12345, stack), "无耗能终端 usePower 应恒成功");
        helper.assertTrue(item.hasPower(player, 12345, stack), "无耗能终端 hasPower 应恒 true");
        helper.assertTrue(item.getAECurrentPower(stack) == item.getAEMaxPower(stack),
                "无耗能终端电量应恒满");
        helper.assertTrue(item.getChargeRate(stack) == 0.0, "无耗能终端充电速率应为 0");
        helper.assertTrue(item.extractAEPower(stack, 999, appeng.api.config.Actionable.SIMULATE) == 999,
                "无耗能终端抽能应恒返回请求数");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void magnetModeRoundTrip(GameTestHelper helper)
    {
        var stack = new ItemStack(Godofthings.WIRELESS_UNIVERSAL_TERMINAL_FREE.get());
        // 无卡时不生效
        helper.assertTrue(com.godofthings.wt.core.WtMagnetHandler.getMagnetMode(stack) == WtMagnetMode.NO_CARD,
                "未装磁卡时磁卡模式应为 NO_CARD");
        // 先装一台终端（升级槽容量 = 已装终端数 × 2），再放磁卡
        stack.set(WtRegistration.PATTERN_ENCODING().componentType().type(),
                com.mojang.datafixers.util.Unit.INSTANCE);
        var upgrades = ((com.godofthings.wt.core.WtTerminalItem) stack.getItem()).getUpgrades(stack);
        upgrades.addItems(new ItemStack(Godofthings.MAGNET_CARD.get()));
        com.godofthings.wt.core.WtMagnetHandler.saveMagnetMode(stack, WtMagnetMode.PICKUP_ME);
        helper.assertTrue(com.godofthings.wt.core.WtMagnetHandler.getMagnetMode(stack) == WtMagnetMode.PICKUP_ME,
                "装磁卡后磁卡模式应可保存并读回");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void upgradeRecipeProducesWutWithTerminal(GameTestHelper helper)
    {
        // upgrade 配方：本模组样板编码终端 + 通用终端 → 带样板编码终端的通用终端
        var level = helper.getLevel();
        var recipeManager = level.getRecipeManager();
        var input = net.minecraft.world.item.crafting.CraftingInput.of(1, 2, java.util.List.of(
                new ItemStack(Godofthings.WIRELESS_PATTERN_ENCODING_TERMINAL.get()),
                new ItemStack(Godofthings.WIRELESS_UNIVERSAL_TERMINAL_FREE.get())));
        var holder = recipeManagerRecipes(recipeManager, WtWutUpgrade.SERIALIZER);
        boolean matched = false;
        for (var recipe : holder)
        {
            if (recipe.value() instanceof WtWutUpgrade upgrade && upgrade.matches(input, level))
            {
                var result = upgrade.assemble(input, level.registryAccess());
                helper.assertTrue(result.getItem() instanceof WtUniversalTerminalItem,
                        "合成产物应是本模组通用终端");
                helper.assertTrue(result.get(WtRegistration.PATTERN_ENCODING().componentType().type()) != null,
                        "产物应带 pattern_encoding 终端组件");
                matched = true;
            }
        }
        helper.assertTrue(matched, "upgrade 配方应能匹配");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void combineRecipeProducesWutWithBothTerminals(GameTestHelper helper)
    {
        var level = helper.getLevel();
        var recipeManager = level.getRecipeManager();
        var input = net.minecraft.world.item.crafting.CraftingInput.of(1, 2, java.util.List.of(
                new ItemStack(Godofthings.WIRELESS_PATTERN_ACCESS_TERMINAL.get()),
                new ItemStack(Godofthings.WIRELESS_PATTERN_ENCODING_TERMINAL.get())));
        var holder = recipeManagerRecipes(recipeManager, WtWutCombine.SERIALIZER);
        boolean matched = false;
        for (var recipe : holder)
        {
            if (recipe.value() instanceof WtWutCombine combine && combine.matches(input, level))
            {
                var result = combine.assemble(input, level.registryAccess());
                helper.assertTrue(result.getItem() instanceof WtUniversalTerminalItem,
                        "合成产物应是本模组通用终端");
                helper.assertTrue(result.get(WtRegistration.PATTERN_ENCODING().componentType().type()) != null,
                        "产物应带 pattern_encoding 终端组件");
                helper.assertTrue(result.get(WtRegistration.PATTERN_ACCESS().componentType().type()) != null,
                        "产物应带 pattern_access 终端组件");
                matched = true;
            }
        }
        helper.assertTrue(matched, "combine 配方应能匹配");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void cycleInstalledTerminals(GameTestHelper helper)
    {
        var stack = new ItemStack(Godofthings.WIRELESS_UNIVERSAL_TERMINAL_FREE.get());
        // 装两台终端
        stack.set(WtRegistration.PATTERN_ENCODING().componentType().type(),
                com.mojang.datafixers.util.Unit.INSTANCE);
        stack.set(WtRegistration.PATTERN_ACCESS().componentType().type(),
                com.mojang.datafixers.util.Unit.INSTANCE);
        // 首次解析应选出第一个已装终端并写回
        var first = WtTerminalDefinition.ofOrNull(stack);
        helper.assertTrue(first == WtRegistration.PATTERN_ENCODING(),
                "首次解析应选出注册表里第一个已装终端");
        // 循环切换（环形：正向第一台 → 第二台，反向第一台 → 绕回最后一台）
        var next = WtTerminalDefinition.nextTerminal(stack, false);
        helper.assertTrue(next == WtRegistration.PATTERN_ACCESS(),
                "正向循环应切到第二台已装终端");
        var back = WtTerminalDefinition.nextTerminal(stack, true);
        helper.assertTrue(back == WtRegistration.PATTERN_ACCESS(),
                "反向循环从第一台应绕回最后一台已装终端");
        helper.succeed();
    }

    private static java.util.List<net.minecraft.world.item.crafting.RecipeHolder<net.minecraft.world.item.crafting.Recipe<?>>>
            recipeManagerRecipes(net.minecraft.world.item.crafting.RecipeManager recipeManager,
                    net.minecraft.world.item.crafting.RecipeSerializer<?> serializer)
    {
        var result = new java.util.ArrayList<net.minecraft.world.item.crafting.RecipeHolder<net.minecraft.world.item.crafting.Recipe<?>>>();
        for (var holder : recipeManager.getRecipes())
        {
            if (holder.value().getSerializer() == serializer)
            {
                @SuppressWarnings({"unchecked", "rawtypes"})
                var cast = (net.minecraft.world.item.crafting.RecipeHolder<net.minecraft.world.item.crafting.Recipe<?>>)
                        (net.minecraft.world.item.crafting.RecipeHolder) holder;
                result.add(cast);
            }
        }
        return result;
    }
}
