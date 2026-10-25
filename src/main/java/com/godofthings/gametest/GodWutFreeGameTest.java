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
    private static final String TEMPLATE = "gametest/note_data";

    @GameTest(template = TEMPLATE)
    public static void terminalRegistryLoaded(GameTestHelper helper)
    {
        // 三台终端定义已注册：crafting / pattern_encoding / pattern_access（全部由通用终端一台物品承载）
        helper.assertTrue(WtTerminalDefinition.wirelessTerminalList().size() == 3,
                "终端定义注册表应有 3 台终端");
        var wut = Godofthings.WIRELESS_UNIVERSAL_TERMINAL_FREE.get();
        helper.assertTrue(WtRegistration.CRAFTING().item() == wut,
                "crafting 终端定义的 item 应为通用终端");
        helper.assertTrue(WtRegistration.PATTERN_ENCODING().item() == wut,
                "pattern_encoding 终端定义的 item 应为通用终端");
        helper.assertTrue(WtRegistration.PATTERN_ACCESS().item() == wut,
                "pattern_access 终端定义的 item 应为通用终端");
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
        // upgrade 配方：AE2 ME样板编码终端（部件） + 通用终端 → 带样板编码终端的通用终端
        var level = helper.getLevel();
        var recipeManager = level.getRecipeManager();
        var input = net.minecraft.world.item.crafting.CraftingInput.of(1, 2, java.util.List.of(
                new ItemStack(appeng.core.definitions.AEParts.PATTERN_ENCODING_TERMINAL.get()),
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
        // combine 配方：AE2 样板管理终端 + 样板编码终端（部件） → 带两台终端的通用终端
        var level = helper.getLevel();
        var recipeManager = level.getRecipeManager();
        var input = net.minecraft.world.item.crafting.CraftingInput.of(1, 2, java.util.List.of(
                new ItemStack(appeng.core.definitions.AEParts.PATTERN_ACCESS_TERMINAL.get()),
                new ItemStack(appeng.core.definitions.AEParts.PATTERN_ENCODING_TERMINAL.get())));
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

    @GameTest(template = TEMPLATE)
    public static void serializersAreRegisteredSingletons(GameTestHelper helper)
    {
        // 防回归（7.15.1 进存档 update_recipes 断连）：注册表里登记的必须是配方类的
        // 静态单例——否则 JSON 配方持有的序列化器实例在注册表里查不到 id，
        // 客户端进服时配方同步包编码直接抛 IllegalArgumentException。
        helper.assertTrue(net.minecraft.core.registries.BuiltInRegistries.RECIPE_SERIALIZER
                .get(Godofthings.godofthingsId("wutfree_combine")) == WtWutCombine.SERIALIZER,
                "wutfree_combine 必须注册静态单例序列化器");
        helper.assertTrue(net.minecraft.core.registries.BuiltInRegistries.RECIPE_SERIALIZER
                .get(Godofthings.godofthingsId("wutfree_upgrade")) == WtWutUpgrade.SERIALIZER,
                "wutfree_upgrade 必须注册静态单例序列化器");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void configManagerNoRecursion(GameTestHelper helper)
    {
        // 防回归（7.15.3 打开终端 StackOverflow）：单物品架构下 getConfigManager 不许
        // 按定义转发回自己，且三台终端需要的设置都要注册（样板管理界面读
        // TERMINAL_SHOW_PATTERN_PROVIDERS，缺了抛 UnsupportedSettingException）。
        var stack = new ItemStack(Godofthings.WIRELESS_UNIVERSAL_TERMINAL_FREE.get());
        stack.set(WtRegistration.PATTERN_ACCESS().componentType().type(),
                com.mojang.datafixers.util.Unit.INSTANCE);
        var item = (WtUniversalTerminalItem) stack.getItem();
        var config = item.getConfigManager(() -> stack);
        helper.assertTrue(config.getSetting(appeng.api.config.Settings.SORT_BY)
                == appeng.api.config.SortOrder.NAME, "SORT_BY 设置应可读取");
        helper.assertTrue(config.getSetting(appeng.api.config.Settings.TERMINAL_SHOW_PATTERN_PROVIDERS)
                == appeng.api.config.ShowPatternProviders.VISIBLE, "样板供应器显示设置应可读取");
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
